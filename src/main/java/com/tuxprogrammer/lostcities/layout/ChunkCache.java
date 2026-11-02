package com.tuxprogrammer.lostcities.layout;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A bounded least-recently-used cache keyed by a chunk (or cell) coordinate pair, owned by one planner instance and so
 * by one world, never static (CONTRIBUTING § World generation rules). Every value it holds is a pure function of its
 * key and the planner's inputs, so eviction only costs a recomputation; nothing iterates it, so its order never reaches
 * the output.
 *
 * <p>
 * Values are computed outside the map ({@link #get}, then {@link #put}), because computing one value may look up and
 * insert others, which a {@code computeIfAbsent} on the same map does not allow.
 *
 * <p>
 * Side: server (one generation thread).
 */
final class ChunkCache<V> {

    private final Map<Long, V> entries;

    /**
     * Creates a cache.
     *
     * @param capacity the most entries kept
     */
    ChunkCache(int capacity) {
        entries = new LinkedHashMap<Long, V>(Math.min(capacity, 1024), 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > capacity;
            }
        };
    }

    /** Returns the value for a coordinate pair, or {@code null}. */
    V get(int x, int z) {
        return entries.get(key(x, z));
    }

    /** Stores the value for a coordinate pair and returns it. */
    V put(int x, int z, V value) {
        entries.put(key(x, z), value);
        return value;
    }

    /** Returns the number of entries held. */
    int size() {
        return entries.size();
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
