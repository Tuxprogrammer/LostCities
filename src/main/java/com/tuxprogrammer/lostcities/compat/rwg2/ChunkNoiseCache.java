package com.tuxprogrammer.lostcities.compat.rwg2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A bounded least-recently-used cache of per-chunk height-noise samples for {@link Rwg2HeightOracle}, safe for
 * concurrent callers (budget B5, docs/design/0005-rwg-overlay.md §8).
 *
 * <p>
 * Only the map is guarded by a lock; the {@link Sampler} runs outside it, so two threads that miss on the same chunk
 * both compute it and the later {@code put} replaces an equal value. A chunk's samples are a pure function of its
 * coordinates, so eviction and races never change an answer. Side: server.
 */
final class ChunkNoiseCache {

    /** Samples kept per chunk: the four stride-8 columns, slot {@code (localX >> 3) * 2 + (localZ >> 3)}. */
    static final int SAMPLES = 4;
    /** Estimated bytes of one cached chunk: the array, the boxed key, the map entry and its table slot. */
    static final long ENTRY_BYTES = 16 + SAMPLES * Float.BYTES + 16 + 56 + 8;

    /** Computes a chunk's {@link #SAMPLES} samples. */
    interface Sampler {

        /**
         * Computes a chunk's samples.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@link #SAMPLES} floats; the cache keeps the array, so the caller must not reuse it
         */
        float[] sample(int chunkX, int chunkZ);
    }

    private final Map<Long, float[]> entries;
    private final Sampler sampler;

    /**
     * Creates a cache.
     *
     * @param capacity the most chunks kept, at least 1
     * @param sampler  computes a chunk on a miss; called outside the lock
     */
    ChunkNoiseCache(int capacity, Sampler sampler) {
        int bound = Math.max(1, capacity);
        this.sampler = sampler;
        this.entries = new LinkedHashMap<Long, float[]>(Math.min(bound, 1024), 0.75F, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, float[]> eldest) {
                return size() > bound;
            }
        };
    }

    /**
     * Returns a chunk's samples, computing them on a miss. The returned array is shared and must not be modified.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the samples
     */
    float[] get(int chunkX, int chunkZ) {
        Long key = (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
        synchronized (entries) {
            float[] hit = entries.get(key);
            if (hit != null) {
                return hit;
            }
        }
        float[] computed = sampler.sample(chunkX, chunkZ);
        synchronized (entries) {
            entries.put(key, computed);
        }
        return computed;
    }

    /** Returns how many chunks are cached. */
    int size() {
        synchronized (entries) {
            return entries.size();
        }
    }
}
