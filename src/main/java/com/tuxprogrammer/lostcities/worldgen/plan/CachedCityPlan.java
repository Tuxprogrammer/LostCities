package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.Arrays;
import java.util.Objects;

/**
 * A bounded cache in front of a {@link CityPlan}: a direct-mapped table of {@link #CAPACITY} slots keyed by chunk
 * coordinates. A miss computes the plan and overwrites the slot. Because plans are pure, eviction never changes a
 * result, only what it costs (0005 §8, budget B5: caches bounded and owned by the world). Not thread-safe; world
 * generation runs on the server thread.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class CachedCityPlan implements CityPlan {

    /** Number of slots: 4096 plans, about 4 MB with their footprints. */
    public static final int CAPACITY = 4096;

    /** Rough size of one cached plan with its footprint, for the B5 estimate, in bytes. */
    public static final int ESTIMATED_PLAN_BYTES = 1024;

    private static final int MASK = CAPACITY - 1;

    private final CityPlan source;
    private final long[] keys = new long[CAPACITY];
    private final ChunkPlan[] plans = new ChunkPlan[CAPACITY];
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
    public ChunkPlan chunk(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        int slot = slot(chunkX, chunkZ);
        ChunkPlan plan = plans[slot];
        if (plan != null && keys[slot] == key) {
            hits++;
            return plan;
        }
        misses++;
        plan = source.chunk(chunkX, chunkZ);
        keys[slot] = key;
        plans[slot] = plan;
        return plan;
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
        return (long) CAPACITY * (ESTIMATED_PLAN_BYTES + Long.BYTES + 8);
    }

    /** Empties the cache; the world calls it on unload. */
    public void clear() {
        Arrays.fill(plans, null);
    }

    private static int slot(int chunkX, int chunkZ) {
        // A 4 × 4-chunk tile per 16 slots keeps neighbors in different slots; the multiplier spreads tiles.
        int hash = (chunkX * 0x9E3779B1) ^ (chunkZ * 0x85EBCA77);
        hash ^= hash >>> 15;
        return hash & MASK;
    }
}
