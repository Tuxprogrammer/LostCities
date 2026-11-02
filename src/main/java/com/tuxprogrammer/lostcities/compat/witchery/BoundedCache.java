package com.tuxprogrammer.lostcities.compat.witchery;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A least-recently-used cache of pure answers by chunk key, bounded and owned by the oracle that holds it, so it lives
 * and dies with the world (CONTRIBUTING § Architecture, "bounded caches that the world lifecycle owns"). The values are
 * pure, so an eviction only costs recomputing one; nothing ever iterates the map.
 *
 * <p>
 * Not thread-safe: the oracle synchronizes around it. Side: both (plain Java).
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
     * Returns the key of a chunk or cell.
     *
     * @param x chunk or cell X
     * @param z chunk or cell Z
     * @return a key unique to the pair
     */
    static long key(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
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

    /** Returns the number of entries. */
    int size() {
        return entries.size();
    }
}
