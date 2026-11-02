package com.tuxprogrammer.lostcities.layout.random;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/HierarchicalStreetPlanner.java

/**
 * The 64-bit hash primitives upstream's street planner, bridge planner and intercity highway planner share:
 * SplitMix64's finalizer ({@link #mix64}), a 64-bit FNV-1a string hash put through it ({@link #stableStringHash}), and
 * the two ways a hash becomes a number ({@link #unitDouble}, {@link #floorModHash}). Upstream copies these private
 * methods into each planner; here they live once.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md#hashing. Side: both.
 */
public final class StableHash {

    /** SplitMix64's first finalizer multiplier. */
    private static final long MIX_MULTIPLIER_1 = 0xbf58476d1ce4e5b9L;
    /** SplitMix64's second finalizer multiplier. */
    private static final long MIX_MULTIPLIER_2 = 0x94d049bb133111ebL;
    /** 64-bit FNV-1a offset basis. */
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    /** 64-bit FNV-1a prime. */
    private static final long FNV_PRIME = 0x100000001b3L;
    /** 2^-53, which turns the top 53 bits of a hash into a double in [0, 1). */
    private static final double UNIT_SCALE = 0x1.0p-53;

    private StableHash() {}

    /**
     * SplitMix64's finalizer: a bijective scramble of all 64 bits.
     *
     * @param value the value to mix
     * @return the mixed value
     */
    public static long mix64(long value) {
        long z = (value ^ (value >>> 30)) * MIX_MULTIPLIER_1;
        z = (z ^ (z >>> 27)) * MIX_MULTIPLIER_2;
        return z ^ (z >>> 31);
    }

    /**
     * FNV-1a over the string's UTF-16 code units, then {@link #mix64}: upstream's dimension-ID hash. It is stable
     * across JVMs, unlike {@link String#hashCode()}'s 32 bits, which it does not use.
     *
     * @param value the string, for example a dimension ID such as {@code minecraft:overworld}
     * @return the 64-bit hash
     */
    public static long stableStringHash(String value) {
        long hash = FNV_OFFSET_BASIS;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= FNV_PRIME;
        }
        return mix64(hash);
    }

    /**
     * The top 53 bits of a hash as a double in {@code [0, 1)}.
     *
     * @param hash the hash
     * @return a uniform double in {@code [0, 1)}
     */
    public static double unitDouble(long hash) {
        return (hash >>> 11) * UNIT_SCALE;
    }

    /**
     * The hash taken as a signed {@code long}, floor-modulo {@code modulus}.
     *
     * @param hash    the hash
     * @param modulus a positive modulus
     * @return a value in {@code [0, modulus)}
     */
    public static int floorModHash(long hash, int modulus) {
        return (int) Math.floorMod(hash, (long) modulus);
    }
}
