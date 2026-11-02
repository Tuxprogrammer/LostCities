package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A least-recently-used cache of pure answers by chunk key, bounded and owned by the oracle that holds it, so it lives
 * and dies with the world (CONTRIBUTING § Architecture, "bounded caches that the world lifecycle owns"; decision log
 * D57). The values are pure, so an eviction only costs recomputing one; nothing ever iterates the map.
 *
 * <p>
 * Not thread-safe: the oracles synchronize around it. Side: both (plain Java).
 *
 * @param <V> the cached value
 */
final class BoundedCache<V> {

    private final LinkedHashMap<Long, V> entries;

    /**
     * Creates a cache.
     *
     * @param capacity the most entries kept, at least 1
     */
    BoundedCache(int capacity) {
        int bound = Math.max(1, capacity);
        this.entries = new LinkedHashMap<Long, V>(16, 0.75F, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > bound;
            }
        };
    }

    /**
     * Returns the key of a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return a key unique to the pair
     */
    static long key(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    /** Returns whether a key is cached (a cached {@code null} counts). */
    boolean contains(long key) {
        return entries.containsKey(key);
    }

    /** Returns a cached value, or {@code null}. */
    V get(long key) {
        return entries.get(key);
    }

    /** Caches a value, which may be {@code null}, and returns it. */
    V put(long key, V value) {
        entries.put(key, value);
        return value;
    }

    /** Drops a key, if cached. */
    void remove(long key) {
        entries.remove(key);
    }

    /** Returns the number of entries. */
    int size() {
        return entries.size();
    }
}
