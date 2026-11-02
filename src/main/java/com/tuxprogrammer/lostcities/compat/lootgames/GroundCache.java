package com.tuxprogrammer.lostcities.compat.lootgames;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A least-recently-used cache of pure answers by site chunk, bounded and owned by the oracle that holds it, so it lives
 * and dies with the world (CONTRIBUTING § Architecture, "bounded caches that the world lifecycle owns"). The values
 * are pure, so an eviction only costs recomputing one; nothing iterates the map.
 *
 * <p>
 * Not thread-safe: the oracle synchronizes around it. Side: both (plain Java).
 */
final class GroundCache {

    private final LinkedHashMap<Long, Integer> entries;

    /**
     * Creates a cache.
     *
     * @param capacity the most entries kept, at least 1
     */
    GroundCache(int capacity) {
        int bound = Math.max(1, capacity);
        this.entries = new LinkedHashMap<Long, Integer>(16, 0.75F, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest) {
                return size() > bound;
            }
        };
    }

    /** Returns the key of a site chunk. */
    static long key(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    /** Returns a cached value, or {@code null}. */
    Integer get(long key) {
        return entries.get(key);
    }

    /** Caches a value and returns it. */
    int put(long key, int value) {
        entries.put(key, value);
        return value;
    }

    /** Returns the number of entries. */
    int size() {
        return entries.size();
    }
}
