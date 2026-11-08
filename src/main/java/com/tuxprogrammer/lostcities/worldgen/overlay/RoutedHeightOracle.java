package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * A world's height oracle for whichever thread asks (D-plan-worker-2): the world's own oracle on the server thread, the
 * plan worker's own oracle, over its own copy of the biome provider, on the worker. Both compute the same heights from
 * the same seed; the oracles' samplers keep scratch arrays and memo maps, so each thread needs its own. The caches in
 * front of this oracle are shared and guarded by the world's {@link PlanLock}.
 *
 * <p>
 * Side: server.
 */
final class RoutedHeightOracle implements TerrainHeightOracle {

    private final World world;
    private final TerrainHeightOracle server;
    private final TerrainHeightOracle worker;

    /**
     * Routes a world's oracle.
     *
     * @param world  the world
     * @param server the oracle over the world's own provider
     * @param worker the plan worker's oracle
     */
    RoutedHeightOracle(World world, TerrainHeightOracle server, TerrainHeightOracle worker) {
        this.world = world;
        this.server = server;
        this.worker = worker;
    }

    private TerrainHeightOracle current() {
        return PlanThreads.onWorker(world) ? worker : server;
    }

    @Override
    public int height(int blockX, int blockZ) {
        return current().height(blockX, blockZ);
    }

    @Override
    public boolean computesWholeChunks() {
        return server.computesWholeChunks();
    }

    @Override
    public boolean isCached(int blockX, int blockZ) {
        return current().isCached(blockX, blockZ);
    }

    @Override
    public HeightProbe probe() {
        return server.probe();
    }

    @Override
    public long computations() {
        return server.computations() + worker.computations();
    }

    @Override
    public void chunkHeights(int chunkX, int chunkZ, int[] out) {
        current().chunkHeights(chunkX, chunkZ, out);
    }
}
