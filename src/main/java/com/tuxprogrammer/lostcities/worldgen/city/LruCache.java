package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A bounded least-recently-used cache of pure values: what the city writer keeps per world (compiled palettes, part
 * palettes, style blocks). The bound keeps memory owned and capped (CONTRIBUTING § World generation rules, budget B5);
 * since every value is a pure function of its key, eviction only costs recomputation, never a different world.
 *
 * <p>
 * Not thread-safe; world generation runs on the server thread. Side: both (plain Java).
 *
 * @param <K> the key
 * @param <V> the value
 */
public final class LruCache<K, V> {

    private final int capacity;
    private final Map<K, V> entries;
    private long hits;
    private long misses;

    /**
     * Creates an empty cache.
     *
     * @param capacity the most entries it holds, at least 1
     */
    public LruCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1, got " + capacity);
        }
        this.capacity = capacity;
        this.entries = new LinkedHashMap<K, V>(16, 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.capacity;
            }
        };
    }

    /**
     * Returns the value of a key, computing and keeping it on a miss.
     *
     * @param key     the key
     * @param compute computes the value; must be pure
     * @return the value
     */
    public V get(K key, Function<K, V> compute) {
        V value = entries.get(key);
        if (value != null) {
            hits++;
            return value;
        }
        misses++;
        value = compute.apply(key);
        entries.put(key, value);
        return value;
    }

    /** Returns how many entries the cache holds now. */
    public int size() {
        return entries.size();
    }

    /** Returns the bound. */
    public int capacity() {
        return capacity;
    }

    /** Returns the lookups answered from the cache. */
    public long hits() {
        return hits;
    }

    /** Returns the lookups that computed their value. */
    public long misses() {
        return misses;
    }

    /** Empties the cache. */
    public void clear() {
        entries.clear();
    }
}
