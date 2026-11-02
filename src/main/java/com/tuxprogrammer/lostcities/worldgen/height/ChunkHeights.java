// @formatter:off
package com.tuxprogrammer.lostcities.worldgen.height;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: TERR-10, TERR-34
// @formatter:on

import java.util.Arrays;
import java.util.Objects;

/**
 * Chunk heights for the layout: one oracle sample per {@code heightSampleSize} × {@code heightSampleSize} group of
 * chunks, at the group's center chunk, block (8, 8), as upstream's {@code getHeightmap} samples
 * (docs/original-spec/03-terrain/heightmap-sampling.md). Every chunk of a group shares the sample.
 *
 * <p>
 * Changes from upstream (docs/new-spec/03-terrain/index.md, "Heights"):
 * <ul>
 * <li>groups come from floor division ({@code Math.floorDiv}), so every group is N × N chunks, also next to the X = 0
 * and Z = 0 axes, where upstream's truncating division made narrower groups (P03-3, D31);</li>
 * <li>TERR-10: a height is never written back; the blend reads these values but never changes them;</li>
 * <li>TERR-34: there is one sampling path, so no option changes the world;</li>
 * <li>the cache is bounded and has no wall-clock expiry: a direct-mapped table of {@link #CAPACITY} groups owned by
 * the world, freed on unload (0005 §8, B5). Eviction never changes a result.</li>
 * </ul>
 *
 * <p>
 * Not thread-safe; world generation runs on the server thread. Side: server, and plain Java in tests.
 */
public final class ChunkHeights {

    /** Number of cached groups. */
    public static final int CAPACITY = 4096;

    /** Bytes the cache holds at most: a key and a height per slot. */
    public static final long MAX_BYTES = CAPACITY * (long) (Long.BYTES + Integer.BYTES);

    /** Receives the duration of each oracle call, for the timers (0005 §8). */
    @FunctionalInterface
    public interface Listener {

        /** Ignores samples. */
        Listener NONE = nanos -> {};

        /**
         * Called after each oracle sample.
         *
         * @param nanos how long the oracle took, in nanoseconds
         */
        void sampled(long nanos);
    }

    private final TerrainHeightOracle oracle;
    private final int sampleSize;
    private final Listener listener;
    private final long[] keys = new long[CAPACITY];
    private final int[] heights = new int[CAPACITY];
    private final boolean[] filled = new boolean[CAPACITY];
    private long samples;

    /**
     * Creates the sampler.
     *
     * @param oracle     the height oracle
     * @param sampleSize chunks per side of a group ({@code heightSampleSize}), at least 1
     * @param listener   receives oracle timings
     */
    public ChunkHeights(TerrainHeightOracle oracle, int sampleSize, Listener listener) {
        if (sampleSize < 1) {
            throw new IllegalArgumentException("heightSampleSize must be at least 1, got " + sampleSize);
        }
        this.oracle = Objects.requireNonNull(oracle);
        this.sampleSize = sampleSize;
        this.listener = Objects.requireNonNull(listener);
    }

    /**
     * Returns a chunk's height: the oracle's height at block (8, 8) of its group's center chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return block Y
     */
    public int height(int chunkX, int chunkZ) {
        int groupX = Math.floorDiv(chunkX, sampleSize);
        int groupZ = Math.floorDiv(chunkZ, sampleSize);
        long key = ((long) groupX << 32) | (groupZ & 0xFFFFFFFFL);
        int slot = ((groupX * 0x9E3779B1) ^ (groupZ * 0x85EBCA77)) >>> 20;
        if (filled[slot] && keys[slot] == key) {
            return heights[slot];
        }
        int centerX = sampleCenter(chunkX);
        int centerZ = sampleCenter(chunkZ);
        long start = System.nanoTime();
        int height = oracle.height((centerX << 4) + 8, (centerZ << 4) + 8);
        listener.sampled(System.nanoTime() - start);
        samples++;
        keys[slot] = key;
        heights[slot] = height;
        filled[slot] = true;
        return height;
    }

    /**
     * Returns the chunk coordinate of the sample center of the group a chunk coordinate is in.
     *
     * @param chunk a chunk X or Z
     * @return {@code floorDiv(chunk, N) × N + N / 2}
     */
    public int sampleCenter(int chunk) {
        return Math.floorDiv(chunk, sampleSize) * sampleSize + sampleSize / 2;
    }

    /** Returns how many times the oracle was asked. */
    public long samples() {
        return samples;
    }

    /** Empties the cache; the world calls it on unload. */
    public void clear() {
        Arrays.fill(filled, false);
    }
}
