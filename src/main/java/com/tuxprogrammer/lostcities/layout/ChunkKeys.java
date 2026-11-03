package com.tuxprogrammer.lostcities.layout;

/**
 * Keys of chunk or cell coordinate pairs for hash maps of boxed {@code Long}s (D-PF-3).
 *
 * <p>
 * {@code Long.hashCode} of the plain packing {@code x << 32 | z} is {@code x ^ z}: nearby chunks share a handful of
 * values, so a map of a few thousand of them puts dozens in one bucket and {@code HashMap} turns those buckets into
 * trees (the layout's caches spent about a twentieth of the plan prefetcher's time there). These keys pass the packing
 * through a multiplication by an odd constant, which is a bijection on {@code long}: two pairs never share a key, and
 * the high and low halves that {@code Long.hashCode} folds both depend on both coordinates. A key is only ever looked
 * up, never decoded; no map keyed by it is iterated where the order could matter.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ChunkKeys {

    /** The 64-bit golden-ratio constant; odd, so the multiplication is invertible. */
    private static final long MIX = 0x9E3779B97F4A7C15L;

    private ChunkKeys() {}

    /**
     * Returns the key of a pair.
     *
     * @param x chunk or cell X
     * @param z chunk or cell Z
     * @return a key unique to the pair, spread over {@code Long.hashCode}
     */
    public static long of(int x, int z) {
        return (((long) x << 32) ^ (z & 0xFFFFFFFFL)) * MIX;
    }
}
