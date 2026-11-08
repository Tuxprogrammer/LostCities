package com.tuxprogrammer.lostcities.compat.rwg2;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.compat.HeightOracles;

import rwg2.world.ChunkManagerRealistic;

/**
 * Builds a plan worker's own RWG2 inputs (D-plan-worker-2): a {@code ChunkManagerRealistic(seed, continental)}, built
 * exactly as RWG2's world type builds the world's own (RWG2 2.0.6,
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/WorldTypeRealistic.java#L18
 * and ChunkManagerRealistic.java#L124-L130), and a height oracle over it. The manager's memo maps are unsynchronized,
 * so the worker never touches the world's.
 *
 * <p>
 * Built on the server thread: the manager's constructor reads RWG2's per-world noise choice and writes a static of
 * RWG2's ({@code NoiseSelector}), which must not race the server. Load only when {@code Loader.isModLoaded("rwg2")}.
 * Side: server.
 */
public final class Rwg2WorkerInputs {

    private Rwg2WorkerInputs() {}

    /**
     * Builds the inputs of a world's plan worker.
     *
     * @param world       the world, whose chunk manager must be RWG2's
     * @param continental whether the world type is RWG2 Continents
     * @return the inputs, or {@code null} when the world's chunk manager is not RWG2's
     */
    public static HeightOracles.WorkerInputs create(World world, boolean continental) {
        if (!(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) {
            return null;
        }
        ChunkManagerRealistic manager = new ChunkManagerRealistic(world.getSeed(), continental);
        return new HeightOracles.WorkerInputs(
            manager,
            Rwg2HeightOracle.forWorker(manager, world.getSeed(), continental));
    }
}
