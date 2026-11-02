package com.tuxprogrammer.lostcities.scenario;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * A scenario's share of world generation, spread over server ticks: loads a rectangle of chunks in row order, then
 * populates an inner rectangle wherever a chunk is not populated yet, within a time budget per tick. The outer
 * rectangle must reach one chunk beyond the inner one on every side, so population never generates a chunk of its
 * own.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class RegionJob {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int popMinX;
    private final int popMinZ;
    private final int popMaxX;
    private final int popMaxZ;
    private int loadCursor;
    private int populateCursor;

    /**
     * Creates a job.
     *
     * @param minX    smallest chunk X to load
     * @param minZ    smallest chunk Z to load
     * @param maxX    largest chunk X to load
     * @param maxZ    largest chunk Z to load
     * @param popMinX smallest chunk X to populate
     * @param popMinZ smallest chunk Z to populate
     * @param popMaxX largest chunk X to populate
     * @param popMaxZ largest chunk Z to populate
     */
    public RegionJob(int minX, int minZ, int maxX, int maxZ, int popMinX, int popMinZ, int popMaxX, int popMaxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.popMinX = popMinX;
        this.popMinZ = popMinZ;
        this.popMaxX = popMaxX;
        this.popMaxZ = popMaxZ;
    }

    /**
     * A square around a chunk: loads {@code radius + 1} chunks out and populates {@code radius} chunks out.
     *
     * @param chunkX the center chunk X
     * @param chunkZ the center chunk Z
     * @param radius the populated radius, chunks
     * @return the job
     */
    public static RegionJob around(int chunkX, int chunkZ, int radius) {
        return new RegionJob(
            chunkX - radius - 1,
            chunkZ - radius - 1,
            chunkX + radius + 1,
            chunkZ + radius + 1,
            chunkX - radius,
            chunkZ - radius,
            chunkX + radius,
            chunkZ + radius);
    }

    /** Returns whether a chunk is in the populated rectangle. */
    public boolean populates(int chunkX, int chunkZ) {
        return chunkX >= popMinX && chunkX <= popMaxX && chunkZ >= popMinZ && chunkZ <= popMaxZ;
    }

    /**
     * Works until the tick's budget is spent.
     *
     * @param server      the world's chunk provider
     * @param tickStart   {@code System.nanoTime()} at the tick's start
     * @param budgetNanos the tick's budget, nanoseconds
     * @return {@code true} when the job is complete
     */
    public boolean step(ChunkProviderServer server, long tickStart, long budgetNanos) {
        int width = maxX - minX + 1;
        int size = width * (maxZ - minZ + 1);
        while (loadCursor < size && System.nanoTime() - tickStart < budgetNanos) {
            server.loadChunk(minX + loadCursor % width, minZ + loadCursor / width);
            loadCursor++;
        }
        if (loadCursor < size) {
            return false;
        }
        int popWidth = Math.max(0, popMaxX - popMinX + 1);
        int popSize = popWidth * Math.max(0, popMaxZ - popMinZ + 1);
        while (populateCursor < popSize && System.nanoTime() - tickStart < budgetNanos) {
            int x = popMinX + populateCursor % popWidth;
            int z = popMinZ + populateCursor / popWidth;
            populateCursor++;
            Chunk chunk = server.provideChunk(x, z);
            if (!chunk.isTerrainPopulated) {
                server.populate(server, x, z);
            }
        }
        return populateCursor >= popSize;
    }
}
