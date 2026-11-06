package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ChestGenHooks;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPass;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.MultiblockShapes;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;
import com.tuxprogrammer.lostcities.worldgen.terrain.TorchRule;

/**
 * The overlay's population phase (docs/design/0005-rwg-overlay.md §3): after RWG's {@code populate} returns, snow
 * and ice included, and before every {@code IWorldGenerator}, it runs the plan's {@link PopulationJob}s with world
 * access, inside the populated chunk only, so it never cascades: torch attachment, chests with loot, spawners and the
 * command-block tick. Blocks are written by the terrain phase; a job only finishes the block it finds there, so a block
 * a ruin, an explosion or a door replaced is left alone.
 *
 * <p>
 * Rules: it writes only into the chunk being populated, which is loaded; it draws only from its own random, seeded
 * from the world seed and the chunk coordinates, never from RWG's or an event's; it writes no light updates, because
 * ArchaicFix's Phosphor lights every source when the chunk's light is first populated (P03-8, D31); saplings are never
 * grown (P03-10, TERR-20, TERR-21: there is no tick-time job queue at all).
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class PopulationPhase {

    /** Block flag: send the change to clients, no neighbor updates (vanilla's flag 2). */
    static final int SEND_TO_CLIENTS = 2;
    /** Delay of a command block's first tick (upstream's post-todo job). */
    static final int COMMAND_BLOCK_DELAY = 1;

    private static final long POPULATION_X = 341873128712L;
    private static final long POPULATION_Z = 104729L;

    private final World world;
    private final WorldChunkBuffer reader;
    /** Whether the first completed GT machine has been logged, the real-pack check of design record 0016. */
    private boolean machineLogged;
    private boolean multiblockLogged;

    /**
     * Creates the population phase of one world.
     *
     * @param world the world
     * @param refs  the world's block translations
     */
    public PopulationPhase(World world, BlockRefs refs) {
        this.world = world;
        this.reader = new WorldChunkBuffer(refs);
    }

    /**
     * Runs a chunk's jobs.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param plan   the chunk's plan
     * @return the number of jobs that changed the world
     */
    public int run(int chunkX, int chunkZ, ChunkPlan plan) {
        if (plan.jobs()
            .isEmpty()) {
            return 0;
        }
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        reader.attach(chunk);
        Random random = new Random(SeedMix.chunk(world.getSeed(), chunkX, chunkZ, POPULATION_X, POPULATION_Z));
        int done = 0;
        for (PopulationJob job : plan.jobs()) {
            int x = (chunkX << 4) + job.x();
            int z = (chunkZ << 4) + job.z();
            switch (job.kind()) {
                case TORCH -> done += torch(job, x, z) ? 1 : 0;
                case CHEST -> done += chest(job, x, z, random) ? 1 : 0;
                case SPAWNER -> done += spawner(job, x, z) ? 1 : 0;
                case COMMAND_BLOCK -> done += commandBlock(job, x, z) ? 1 : 0;
                case TILE_ENTITY -> done += tileEntity(job, x, z) ? 1 : 0;
                case FURNISH -> done += furnish(job, chunkX, chunkZ);
                default -> LostCities.LOG.warn("Unknown population job {}", job);
            }
        }
        return done;
    }

    private boolean torch(PopulationJob job, int x, int z) {
        if (world.getBlock(x, job.y(), z) != Blocks.torch) {
            return false;
        }
        int meta = TorchRule.metaFor(reader, job.x(), job.y(), job.z());
        if (meta == TorchRule.KEEP || meta == world.getBlockMetadata(x, job.y(), z)) {
            return false;
        }
        return world.setBlockMetadataWithNotify(x, job.y(), z, meta, SEND_TO_CLIENTS);
    }

    /**
     * Fills the container the terrain phase wrote. Nothing happens when a ruin or an explosion took it: getting the
     * tile entity creates it only for a block that has one (upstream's "still not air" check).
     */
    private boolean chest(PopulationJob job, int x, int z, Random random) {
        if (world.isAirBlock(x, job.y(), z)) {
            return false;
        }
        TileEntity tile = tileAt(x, job.y(), z);
        if (!(tile instanceof IInventory)) {
            return false;
        }
        TemplateNbt.apply(tile, job.template());
        if (job.argument()
            .isEmpty()) {
            return true;
        }
        fill(tile, job.argument(), random, x, z);
        return true;
    }

    /** Fills a chest with a loot category's contents (see the comment on the scratch inventory). */
    private void fill(TileEntity tile, String category, Random random, int x, int z) {
        // Filled through a scratch inventory and written as NBT: a container's setInventorySlotContents calls
        // markDirty, whose comparator update reads the neighbors and, on a chunk's edge, would load the next chunk.
        IInventory container = (IInventory) tile;
        InventoryBasic scratch = new InventoryBasic("", false, container.getSizeInventory());
        ChestGenHooks info = ChestGenHooks.getInfo(category);
        WeightedRandomChestContent.generateChestContents(random, info.getItems(random), scratch, info.getCount(random));
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < scratch.getSizeInventory(); slot++) {
            ItemStack stack = scratch.getStackInSlot(slot);
            if (stack != null) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte("Slot", (byte) slot);
                stack.writeToNBT(entry);
                items.appendTag(entry);
            }
        }
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        tag.setTag("Items", items);
        tile.readFromNBT(tag);
        world.getChunkFromChunkCoords(x >> 4, z >> 4)
            .setChunkModified();
    }

    /**
     * Sets the mob of the spawner the terrain phase wrote, checked against the entity registry ({@link SpawnerMobs});
     * a spawner whose condition picked no mob, or a mob this pack lacks with no fallback, becomes air
     * (ASSET-29
     * is fixed by the condition returning nothing, and nothing must not mean a pig spawner).
     */
    private boolean spawner(PopulationJob job, int x, int z) {
        if (world.getBlock(x, job.y(), z) != Blocks.mob_spawner) {
            return false;
        }
        String mob = SpawnerMobs.resolve(job.argument());
        if (mob.isEmpty()) {
            return world.setBlock(x, job.y(), z, Blocks.air, 0, SEND_TO_CLIENTS);
        }
        TileEntity tile = tileAt(x, job.y(), z);
        if (tile instanceof TileEntityMobSpawner spawner) {
            spawner.func_145881_a()
                .setEntityName(mob);
            world.getChunkFromChunkCoords(x >> 4, z >> 4)
                .setChunkModified();
            return true;
        }
        return false;
    }

    /**
     * Creates the tile entity of a block the terrain phase wrote, which a write into the chunk's sections does not do,
     * and applies its template.
     */
    private boolean tileEntity(PopulationJob job, int x, int z) {
        if (world.isAirBlock(x, job.y(), z)) {
            return false;
        }
        TileEntity tile = tileAt(x, job.y(), z);
        if (tile == null) {
            return false;
        }
        if (TemplateNbt.apply(tile, job.template())) {
            world.getChunkFromChunkCoords(x >> 4, z >> 4)
                .setChunkModified();
        }
        return true;
    }

    /**
     * Completes one furnished floor (design record 0016): creates the tile entities of the furniture the terrain phase
     * wrote and gives each GT machine its MetaTileEntity and facing through its NBT, the chunk-load path
     * ({@code BaseMetaTileEntity.readFromNBT} builds the machine from {@code mID}, research-gt-machines.md §1). A
     * block that already has a tile entity is left as it is. Reads and writes only this chunk.
     *
     * @return the number of tile entities completed
     */
    private int furnish(PopulationJob job, int chunkX, int chunkZ) {
        int done = 0;
        // The terrain phase wrote the floor's GT machines as stand-ins (D-gtl-1): loose machines stand on the floor,
        // multiblock controllers within its placed layers. Swapped back first, so each gets its tile entity below.
        reader.restoreStandIns(job.y(), job.y() + MultiblockShapes.MAX_PLACED_LAYERS - 1);
        for (FurnishPass.Completion completion : FurnishPass
            .completions(reader::get, job.y(), job.argument(), FurnishPalettes.bundled())) {
            int x = (chunkX << 4) + completion.x();
            int z = (chunkZ << 4) + completion.z();
            int level = completion.y() >= 0 ? completion.y() : job.y();
            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            if (chunk.getTileEntityUnsafe(completion.x(), level, completion.z()) != null) {
                continue;
            }
            TileEntity tile = tileAt(x, level, z);
            if (tile == null) {
                continue;
            }
            if (completion.loot() != null && tile instanceof IInventory) {
                fill(
                    tile,
                    completion.loot(),
                    new Random(SeedMix.mix(world.getSeed(), x * 31L + z * 17L + level)),
                    x,
                    z);
            }
            if (completion.mob() != null && tile instanceof TileEntityMobSpawner spawner) {
                String mob = SpawnerMobs.resolve(completion.mob());
                if (mob.isEmpty()) {
                    world.setBlock(x, level, z, Blocks.air, 0, SEND_TO_CLIENTS);
                    continue;
                }
                spawner.func_145881_a()
                    .setEntityName(mob);
                chunk.setChunkModified();
            }
            if (!completion.template()
                .isEmpty() && TemplateNbt.apply(tile, completion.template())) {
                chunk.setChunkModified();
                if (completion.y() >= 0 && !multiblockLogged) {
                    multiblockLogged = true;
                    LostCities.LOG.info(
                        "Furnish: partial multiblock controller {} at {} {} {} is {} with {}",
                        completion.gtId(),
                        x,
                        level,
                        z,
                        machineName(tile),
                        completion.template());
                }
                if (!machineLogged) {
                    machineLogged = true;
                    LostCities.LOG.info(
                        "Furnish: GT machine {} at {} {} {} is {} with {}",
                        completion.gtId(),
                        x,
                        level,
                        z,
                        machineName(tile),
                        completion.template());
                }
            }
            done++;
        }
        return done;
    }

    /** Names a GT tile entity's MetaTileEntity class for the first machine's log line, by reflection (no GT API). */
    private static String machineName(TileEntity tile) {
        try {
            Object mte = tile.getClass()
                .getMethod("getMetaTileEntity")
                .invoke(tile);
            return mte == null ? "no MetaTileEntity"
                : mte.getClass()
                    .getSimpleName();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return tile.getClass()
                .getSimpleName();
        }
    }

    /**
     * Returns the tile entity at a position of the chunk being populated, creating it through the chunk alone.
     *
     * <p>
     * {@code World.getTileEntity} creates a missing tile entity with {@code World.setTileEntity}, which then tells the
     * neighbors' comparators ({@code World.func_147453_f}) and so reads the blocks beside it: for a door on a chunk's
     * edge that loads, or generates, the next chunk, a cascade the real pack logged. The chunk's own
     * {@code getTileEntityUnsafe} and {@code addTileEntity} touch nothing outside the chunk.
     *
     * @return the tile entity, or {@code null} when the block has none
     */
    private TileEntity tileAt(int x, int y, int z) {
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        TileEntity tile = chunk.getTileEntityUnsafe(x & 15, y, z & 15);
        if (tile != null) {
            return tile;
        }
        Block block = chunk.getBlock(x & 15, y, z & 15);
        int meta = chunk.getBlockMetadata(x & 15, y, z & 15);
        if (!block.hasTileEntity(meta)) {
            return null;
        }
        tile = block.createTileEntity(world, meta);
        if (tile == null) {
            return null;
        }
        tile.xCoord = x;
        tile.yCoord = y;
        tile.zCoord = z;
        chunk.addTileEntity(tile);
        return tile;
    }

    private boolean commandBlock(PopulationJob job, int x, int z) {
        if (world.getBlock(x, job.y(), z) != Blocks.command_block) {
            return false;
        }
        world.scheduleBlockUpdate(x, job.y(), z, Blocks.command_block, COMMAND_BLOCK_DELAY);
        return true;
    }
}
