package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.Arrays;
import java.util.Objects;

import com.tuxprogrammer.lostcities.worldgen.damage.Repairs;

/**
 * A bounded cache in front of a {@link CityPlan}: a {@link #WAYS}-way set-associative table of {@link #CAPACITY}
 * plans keyed by chunk coordinates. A miss computes the plan and stores it in its set, over the set's oldest entry
 * when the set is full, and only once the plan is complete, so a computation the plan prefetcher's probe abandons
 * leaves nothing behind. Because plans are pure, eviction never changes a result, only what it costs (0005 §8, budget
 * B5: caches bounded and owned by the world). Associativity keeps the plans the prefetcher makes a few chunks ahead
 * of generation until the generation front reads them. Not thread-safe; world generation runs on the server thread.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class CachedCityPlan implements CityPlan {

    /** Number of plans held: 4096, about 4 MB with their footprints. */
    public static final int CAPACITY = 4096;

    /** Entries per set. */
    public static final int WAYS = 4;

    /** Rough size of one cached plan with its footprint, for the B5 estimate, in bytes. */
    public static final int ESTIMATED_PLAN_BYTES = 1024;

    private static final int SETS = CAPACITY / WAYS;
    private static final int SET_MASK = SETS - 1;

    private final CityPlan source;
    private final long[] keys = new long[CAPACITY];
    private final ChunkPlan[] plans = new ChunkPlan[CAPACITY];
    private final byte[] victims = new byte[SETS];
    private long hits;
    private long misses;

    /**
     * Wraps a plan source.
     *
     * @param source the source; called only on a miss
     */
    public CachedCityPlan(CityPlan source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public Repairs repairs() {
        return source.repairs();
    }

    @Override
    public ChunkPlan chunk(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        int set = set(chunkX, chunkZ);
        int entry = find(key, set);
        if (entry >= 0) {
            hits++;
            return plans[entry];
        }
        misses++;
        ChunkPlan plan = source.chunk(chunkX, chunkZ);
        int slot = set * WAYS + victim(set);
        keys[slot] = key;
        plans[slot] = plan;
        return plan;
    }

    /**
     * Returns whether a chunk's plan is cached, without asking the source.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} when {@link #chunk} would answer from the cache
     */
    public boolean isCached(int chunkX, int chunkZ) {
        return find(key(chunkX, chunkZ), set(chunkX, chunkZ)) >= 0;
    }

    /** Returns the number of lookups answered from the cache. */
    public long hits() {
        return hits;
    }

    /** Returns the number of lookups that asked the source. */
    public long misses() {
        return misses;
    }

    /** Returns the most memory the cache can hold, estimated, in bytes. */
    public long estimatedMaxBytes() {
        return (long) CAPACITY * (ESTIMATED_PLAN_BYTES + Long.BYTES + 8) + SETS;
    }

    /** Empties the cache; the world calls it on unload. */
    public void clear() {
        Arrays.fill(plans, null);
    }

    private int find(long key, int set) {
        int base = set * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (plans[base + way] != null && keys[base + way] == key) {
                return base + way;
            }
        }
        return -1;
    }

    /** Picks the way a new plan of a set takes: an empty one, else the oldest (round robin). */
    private int victim(int set) {
        int base = set * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (plans[base + way] == null) {
                return way;
            }
        }
        int way = victims[set];
        victims[set] = (byte) ((way + 1) % WAYS);
        return way;
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static int set(int chunkX, int chunkZ) {
        int hash = (chunkX * 0x9E3779B1) ^ (chunkZ * 0x85EBCA77);
        hash ^= hash >>> 15;
        return hash & SET_MASK;
    }
}
