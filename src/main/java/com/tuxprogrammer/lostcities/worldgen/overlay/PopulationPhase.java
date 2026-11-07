package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Arrays;
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
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.ChestGenHooks;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalette;
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

    /** The mob a spawner's tile entity starts with, vanilla's {@code MobSpawnerBaseLogic} default. */
    static final String DEFAULT_MOB = "Pig";

    private final World world;
    private final WorldChunkBuffer reader;
    /** Positions of the chunk being populated that a job completed this run, by {@link OrphanTiles#index}. */
    private final boolean[] handled = new boolean[16 * 16 * 256];
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
        Arrays.fill(handled, false);
        int done = 0;
        int top = -1;
        for (PopulationJob job : plan.jobs()) {
            int x = (chunkX << 4) + job.x();
            int z = (chunkZ << 4) + job.z();
            top = Math.max(top, job.y());
            switch (job.kind()) {
                case TORCH -> done += torch(job, x, z) ? 1 : 0;
                case CHEST -> done += chest(job, x, z, random) ? 1 : 0;
                case CLEAR_CHEST -> done += clearChest(job, x, z) ? 1 : 0;
                case SPAWNER -> done += spawner(job, x, z) ? 1 : 0;
                case COMMAND_BLOCK -> done += commandBlock(job, x, z) ? 1 : 0;
                case TILE_ENTITY -> done += tileEntity(job, x, z) ? 1 : 0;
                case FURNISH -> done += furnish(job, chunkX, chunkZ);
                default -> LostCities.LOG.warn("Unknown population job {}", job);
            }
        }
        if (OrphanTiles.worthSweeping(plan.jobs())) {
            done += orphans(plan, chunkX, chunkZ, random, top + MultiblockShapes.MAX_PLACED_LAYERS);
        }
        return done;
    }

    /** Marks a position of the chunk being populated as completed by a job of this run. */
    private void handle(int x, int y, int z) {
        if (y >= 0 && y <= 255) {
            handled[OrphanTiles.index(x & 15, y, z & 15)] = true;
        }
    }

    /**
     * Sends a block and its tile entity to the players watching the chunk (bug 2 of D-popsync-1). A tile entity
     * changed through its NBT, as a GT machine is given its MetaTileEntity, tells no client; GT's
     * {@code CommonBaseMetaTileEntity.getDescriptionPacket} (GT5-Unofficial 5.09.54.265) carries the machine's ID
     * and state, and {@code readDescriptionBuffer} builds the MetaTileEntity on the client from it, so one resend is
     * enough. {@code World.markBlockForUpdate} goes to the player manager's watcher of this chunk only, which it
     * looks up without creating ({@code getOrCreateChunkWatcher(..., false)}): it reads and loads no other chunk.
     */
    private void sendToClients(int x, int y, int z) {
        world.markBlockForUpdate(x, y, z);
    }

    /**
     * Completes the spawners, chests and GT machines of the chunk that no job found at its position, or clears them
     * (decision log D-popsync-1, {@link OrphanTiles}).
     *
     * @return the number of blocks completed or cleared
     */
    private int orphans(ChunkPlan plan, int chunkX, int chunkZ, Random random, int maxY) {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        int done = 0;
        for (OrphanTiles.Orphan orphan : OrphanTiles
            .find(reader::get, plan.jobs(), i -> handled[i], FurnishPalettes.bundled(), 0, maxY)) {
            int x = (chunkX << 4) + orphan.x();
            int y = orphan.y();
            int z = (chunkZ << 4) + orphan.z();
            TileEntity existing = chunk.getTileEntityUnsafe(orphan.x(), y, orphan.z());
            // Trace: every orphan the sweep finds and whether its tile entity was still default, so a miss shows why.
            CityTraceLog.log(
                "orphan chunk=%d,%d %s tile=%s default=%s",
                chunkX,
                chunkZ,
                orphan,
                existing == null ? "none" : existing.getClass()
                    .getSimpleName(),
                existing == null || isDefault(existing));
            if (existing != null && !isDefault(existing)) {
                continue; // a dungeon's spawner or chest, or a machine something else completed
            }
            if (FurnishItem.GT_MACHINES.equals(orphan.block())) {
                reader.restoreStandIn(orphan.x(), y, orphan.z());
            }
            String mob = FurnishPalette.SPAWNER.equals(orphan.block()) ? SpawnerMobs.resolve(orphan.mob()) : null;
            if (orphan.action() == OrphanTiles.Action.CLEAR || mob != null && mob.isEmpty()) {
                done += clearQuietly(x, y, z) ? 1 : 0;
                continue;
            }
            TileEntity tile = tileAt(x, y, z);
            if (tile == null) {
                continue;
            }
            if (!orphan.template()
                .isEmpty()) {
                TemplateNbt.apply(tile, orphan.template());
            }
            if (mob != null && tile instanceof TileEntityMobSpawner spawner) {
                spawner.func_145881_a()
                    .setEntityName(mob);
            }
            if (orphan.loot() != null && !orphan.loot()
                .isEmpty() && tile instanceof IInventory) {
                fill(tile, orphan.loot(), random, x, z);
            }
            chunk.setChunkModified();
            sendToClients(x, y, z);
            done++;
        }
        CityTraceLog.log("orphans chunk=%d,%d swept=0..%d done=%d", chunkX, chunkZ, maxY, done);
        if (done > 0) {
            LostCities.LOG
                .debug("Population: {} moved spawners, chests or machines completed in {} {}", done, chunkX, chunkZ);
        }
        return done;
    }

    /**
     * Whether a tile entity is still as its block made it: a spawner with vanilla's default mob, a container with no
     * items, a GT machine with no MetaTileEntity. Only such a one is completed by a job or the orphan sweep.
     */
    private static boolean isDefault(TileEntity tile) {
        if (tile instanceof TileEntityMobSpawner spawner) {
            return DEFAULT_MOB.equals(
                spawner.func_145881_a()
                    .getEntityNameToSpawn());
        }
        if (tile instanceof IInventory inventory) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                if (inventory.getStackInSlot(slot) != null) {
                    return false;
                }
            }
            return true;
        }
        return "no MetaTileEntity".equals(machineName(tile));
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
        handle(x, job.y(), z);
        if (!isDefault(tile)) {
            return false;
        }
        TemplateNbt.apply(tile, job.template());
        if (!job.argument()
            .isEmpty()) {
            fill(tile, job.argument(), random, x, z);
        }
        sendToClients(x, job.y(), z);
        return true;
    }

    /** Removes a chest the room rules did not keep; a block that is no container any more (a ruin) is left alone. */
    private boolean clearChest(PopulationJob job, int x, int z) {
        if (world.isAirBlock(x, job.y(), z) || !(tileAt(x, job.y(), z) instanceof IInventory)) {
            return false;
        }
        return clearQuietly(x, job.y(), z);
    }

    /**
     * Turns a block of this chunk into air without its block hooks: the tile entity is dropped and the section written
     * straight, then clients are told. {@code World.setBlock} would run the block's {@code breakBlock}, and a chest's
     * comparator update there reads its neighbors, loading the next chunk when the chest stands on the edge. Only
     * blocks that neither give nor block light are cleared this way (chests, spawners), so light and height map hold.
     */
    private boolean clearQuietly(int x, int y, int z) {
        if (y < 0 || y > 255) {
            return false;
        }
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        if (section == null) {
            return false;
        }
        handle(x, y, z);
        world.removeTileEntity(x, y, z);
        section.func_150818_a(x & 15, y & 15, z & 15, Blocks.air);
        section.setExtBlockMetadata(x & 15, y & 15, z & 15, 0);
        chunk.setChunkModified();
        world.markBlockForUpdate(x, y, z);
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
        handle(x, job.y(), z);
        String mob = SpawnerMobs.resolve(job.argument());
        if (mob.isEmpty()) {
            return clearQuietly(x, job.y(), z);
        }
        TileEntity tile = tileAt(x, job.y(), z);
        if (tile instanceof TileEntityMobSpawner spawner) {
            spawner.func_145881_a()
                .setEntityName(mob);
            world.getChunkFromChunkCoords(x >> 4, z >> 4)
                .setChunkModified();
            sendToClients(x, job.y(), z);
            return true;
        }
        // A spawner that cannot take a mob is air, never a pig spawner.
        return clearQuietly(x, job.y(), z);
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
        handle(x, job.y(), z);
        if (TemplateNbt.apply(tile, job.template())) {
            world.getChunkFromChunkCoords(x >> 4, z >> 4)
                .setChunkModified();
        }
        sendToClients(x, job.y(), z);
        return true;
    }

    /**
     * Completes one furnished floor (design record 0016): creates the tile entities of the furniture the terrain phase
     * wrote and gives each GT machine its MetaTileEntity and facing through its NBT, the chunk-load path
     * ({@code BaseMetaTileEntity.readFromNBT} builds the machine from {@code mID}, research-gt-machines.md §1). A
     * block whose tile entity already holds anything but its defaults is left as it is; a spawner no mob can be given
     * becomes air. Each completed tile entity is sent to watching clients. Reads and writes only this chunk.
     *
     * @return the number of tile entities completed
     */
    private int furnish(PopulationJob job, int chunkX, int chunkZ) {
        int done = 0;
        // The terrain phase wrote the floor's GT machines as stand-ins (D378): loose machines stand on the floor,
        // multiblock controllers within its placed layers. Swapped back first, so each gets its tile entity below.
        reader.restoreStandIns(job.y(), job.y() + MultiblockShapes.MAX_PLACED_LAYERS - 1);
        for (FurnishPass.Completion completion : FurnishPass
            .completions(reader::get, job.y(), job.argument(), FurnishPalettes.bundled())) {
            int x = (chunkX << 4) + completion.x();
            int z = (chunkZ << 4) + completion.z();
            int level = completion.y() >= 0 ? completion.y() : job.y();
            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            TileEntity existing = chunk.getTileEntityUnsafe(completion.x(), level, completion.z());
            // A tile entity made before population (anything that asked the world for it) still has its block's
            // defaults, a pig spawner among them; one that holds anything else is left as it is.
            if (existing != null && !isDefault(existing)) {
                handle(x, level, z);
                continue;
            }
            TileEntity tile = tileAt(x, level, z);
            if (tile == null) {
                continue;
            }
            handle(x, level, z);
            if (tile instanceof TileEntityMobSpawner spawner) {
                // A spawner the palette names no mob for, or one the pack lacks, is air (SpawnerMobs).
                String mob = SpawnerMobs.resolve(completion.mob());
                if (mob.isEmpty()) {
                    clearQuietly(x, level, z);
                    continue;
                }
                spawner.func_145881_a()
                    .setEntityName(mob);
                chunk.setChunkModified();
            }
            if (completion.loot() != null && tile instanceof IInventory) {
                fill(
                    tile,
                    completion.loot(),
                    new Random(SeedMix.mix(world.getSeed(), x * 31L + z * 17L + level)),
                    x,
                    z);
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
            sendToClients(x, level, z);
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
