package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ChestGenHooks;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;
import com.tuxprogrammer.lostcities.worldgen.terrain.TorchRule;

/**
 * The overlay's population phase (docs/design/0005-rwg-overlay.md §3): after RWG's {@code populate} returns, snow
 * and ice included, and before every {@code IWorldGenerator}, it runs the plan's {@link PopulationJob}s with world
 * access, inside the populated chunk only, so it never cascades: torch attachment, chests with loot, spawners and the
 * command-block tick.
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

    private boolean chest(PopulationJob job, int x, int z, Random random) {
        if (!world.setBlock(x, job.y(), z, Blocks.chest, 0, SEND_TO_CLIENTS)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, job.y(), z);
        if (!(tile instanceof IInventory) || job.argument()
            .isEmpty()) {
            return true;
        }
        ChestGenHooks info = ChestGenHooks.getInfo(job.argument());
        WeightedRandomChestContent
            .generateChestContents(random, info.getItems(random), (IInventory) tile, info.getCount(random));
        return true;
    }

    private boolean spawner(PopulationJob job, int x, int z) {
        if (!world.setBlock(x, job.y(), z, Blocks.mob_spawner, 0, SEND_TO_CLIENTS)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, job.y(), z);
        if (tile instanceof TileEntityMobSpawner spawner) {
            spawner.func_145881_a()
                .setEntityName(job.argument());
        }
        return true;
    }

    private boolean commandBlock(PopulationJob job, int x, int z) {
        if (world.getBlock(x, job.y(), z) != Blocks.command_block) {
            return false;
        }
        world.scheduleBlockUpdate(x, job.y(), z, Blocks.command_block, COMMAND_BLOCK_DELAY);
        return true;
    }
}
