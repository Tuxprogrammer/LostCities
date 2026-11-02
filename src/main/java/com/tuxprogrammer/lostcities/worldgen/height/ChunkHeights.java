package com.tuxprogrammer.lostcities.worldgen.height;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: TERR-10, TERR-34

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
 * <li>the cache is bounded and has no wall-clock expiry: a {@link #WAYS}-way set-associative table of
 * {@link #CAPACITY} groups owned by the world, freed on unload (0005 §8, B5). Eviction never changes a result. The
 * plan prefetcher's samples must outlive the few hundred samples taken before the generation front reaches them, and
 * a burst it fetches piece by piece must stay cached until its last piece arrives, which a direct-mapped table does
 * not promise;</li>
 * <li>while the world's {@link HeightProbe} is active, a sample the oracle would have to compute throws instead.</li>
 * </ul>
 *
 * <p>
 * Not thread-safe; world generation runs on the server thread. Side: server, and plain Java in tests.
 */
public final class ChunkHeights {

    /** Groups cached in all: 16,384, about 200 KB. */
    public static final int CAPACITY = 16384;

    /** Entries per set. */
    public static final int WAYS = 4;

    /** Bytes the cache holds at most: a key, a height and a flag per entry, and a cursor per set. */
    public static final long MAX_BYTES = CAPACITY * (long) (Long.BYTES + Integer.BYTES + 1) + CAPACITY / WAYS;

    private static final int SETS = CAPACITY / WAYS;
    private static final int SET_SHIFT = Integer.SIZE - Integer.numberOfTrailingZeros(SETS);

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
    private final HeightProbe probe;
    private final long[] keys = new long[CAPACITY];
    private final int[] heights = new int[CAPACITY];
    private final boolean[] filled = new boolean[CAPACITY];
    private final byte[] victims = new byte[SETS];
    private long samples;

    /**
     * Creates the sampler, with a probe that is never active.
     *
     * @param oracle     the height oracle
     * @param sampleSize chunks per side of a group ({@code heightSampleSize}), at least 1
     * @param listener   receives oracle timings
     */
    public ChunkHeights(TerrainHeightOracle oracle, int sampleSize, Listener listener) {
        this(oracle, sampleSize, listener, new HeightProbe());
    }

    /**
     * Creates the sampler with the world's probe.
     *
     * @param oracle     the height oracle
     * @param sampleSize chunks per side of a group ({@code heightSampleSize}), at least 1
     * @param listener   receives oracle timings
     * @param probe      the world's probe: while it is active, a sample the oracle would have to compute throws
     *                   {@link HeightProbe.NotCached}
     */
    public ChunkHeights(TerrainHeightOracle oracle, int sampleSize, Listener listener, HeightProbe probe) {
        if (sampleSize < 1) {
            throw new IllegalArgumentException("heightSampleSize must be at least 1, got " + sampleSize);
        }
        this.oracle = Objects.requireNonNull(oracle);
        this.sampleSize = sampleSize;
        this.listener = Objects.requireNonNull(listener);
        this.probe = Objects.requireNonNull(probe);
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
        long key = key(groupX, groupZ);
        int entry = find(key, groupX, groupZ);
        if (entry >= 0) {
            return heights[entry];
        }
        int blockX = (sampleCenter(chunkX) << 4) + 8;
        int blockZ = (sampleCenter(chunkZ) << 4) + 8;
        if (probe.active() && !oracle.isCached(blockX, blockZ)) {
            throw probe.miss(() -> height(chunkX, chunkZ));
        }
        long start = System.nanoTime();
        int height = oracle.height(blockX, blockZ);
        listener.sampled(System.nanoTime() - start);
        samples++;
        int set = set(groupX, groupZ);
        int slot = set * WAYS + victim(set);
        keys[slot] = key;
        heights[slot] = height;
        filled[slot] = true;
        return height;
    }

    /**
     * Returns whether a chunk's height is cached, so asking for it costs nothing.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} when its group's sample is held
     */
    public boolean isCached(int chunkX, int chunkZ) {
        int groupX = Math.floorDiv(chunkX, sampleSize);
        int groupZ = Math.floorDiv(chunkZ, sampleSize);
        return find(key(groupX, groupZ), groupX, groupZ) >= 0;
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

    private int find(long key, int groupX, int groupZ) {
        int base = set(groupX, groupZ) * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (filled[base + way] && keys[base + way] == key) {
                return base + way;
            }
        }
        return -1;
    }

    /** Picks the way a new entry of a set takes: an empty one, else the oldest (round robin). */
    private int victim(int set) {
        int base = set * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (!filled[base + way]) {
                return way;
            }
        }
        int way = victims[set];
        victims[set] = (byte) ((way + 1) % WAYS);
        return way;
    }

    private static long key(int groupX, int groupZ) {
        return ((long) groupX << 32) | (groupZ & 0xFFFFFFFFL);
    }

    private static int set(int groupX, int groupZ) {
        return ((groupX * 0x9E3779B1) ^ (groupZ * 0x85EBCA77)) >>> SET_SHIFT;
    }
}
