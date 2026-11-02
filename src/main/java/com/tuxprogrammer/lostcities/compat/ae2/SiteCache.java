package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A least-recently-used cache of pure answers by grid cell, bounded and owned by the oracle that holds it, so it lives
 * and dies with the world (CONTRIBUTING § Architecture, "bounded caches that the world lifecycle owns"). The values
 * are pure, so an eviction only costs recomputing one; nothing iterates the map.
 *
 * <p>
 * Not thread-safe: the oracle synchronizes around it. Side: both (plain Java).
 *
 * @param <V> the cached value
 */
final class SiteCache<V> {

    private final LinkedHashMap<Long, V> entries;

    /**
     * Creates a cache.
     *
     * @param capacity the most entries kept, at least 1
     */
    SiteCache(int capacity) {
        int bound = Math.max(1, capacity);
        this.entries = new LinkedHashMap<Long, V>(16, 0.75F, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > bound;
            }
        };
    }

    /** Returns the key of a cell. */
    static long key(int cellX, int cellZ) {
        return (long) cellX << 32 | cellZ & 0xFFFFFFFFL;
    }

    /** Returns a cached value, or {@code null}. */
    V get(long key) {
        return entries.get(key);
    }

    /** Caches a value and returns it. */
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
