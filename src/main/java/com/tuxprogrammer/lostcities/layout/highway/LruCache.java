package com.tuxprogrammer.lostcities.layout.highway;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A bounded cache that evicts the least recently used entry, owned by one planner and so by one world (avoids
 * HWY-10: upstream trimmed its "LRU" caches in hash-map iteration order). Values are pure functions of their keys, so
 * eviction only costs a recomputation, and nothing iterates the map, so its order never reaches the output.
 *
 * <p>
 * Values are computed outside the map ({@link #get}, then {@link #put}), because computing one may look up others.
 * Not thread-safe; world generation runs on the server thread. Side: both.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
final class LruCache<K, V> {

    private final Map<K, V> entries;
    private long hits;
    private long misses;

    /**
     * Creates a cache.
     *
     * @param capacity the most entries kept
     */
    LruCache(int capacity) {
        entries = new LinkedHashMap<K, V>(Math.min(capacity, 1024), 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    /** Returns the value for a key, or {@code null}, and counts the hit or miss. */
    V get(K key) {
        V value = entries.get(key);
        if (value == null) {
            misses++;
        } else {
            hits++;
        }
        return value;
    }

    /** Returns whether a key is held, without touching its recency or the counts. */
    boolean holds(K key) {
        return entries.containsKey(key);
    }

    /** Stores a value and returns it. */
    V put(K key, V value) {
        entries.put(key, value);
        return value;
    }

    /** Returns the number of entries held. */
    int size() {
        return entries.size();
    }

    /** Returns the lookups that found a value. */
    long hits() {
        return hits;
    }

    /** Returns the lookups that found none. */
    long misses() {
        return misses;
    }
}
