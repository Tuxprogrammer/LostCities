package com.tuxprogrammer.lostcities.layout.random;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/varia/QualityRandom.java

import java.util.Random;

/**
 * Upstream's per-chunk "building random": a combined generator (a 64-bit LCG, an xorshift and a multiply-with-carry)
 * behind the {@link Random} API, so every {@code nextInt}, {@code nextFloat} and {@code nextDouble} draws from
 * {@link #nextLong()}. It decides most per-chunk layout choices, so it is ported bit for bit; the recorded upstream
 * values in {@code src/test/resources/parity/quality_random.txt} prove it.
 *
 * <p>
 * Only the seeded constructor is ported: an unseeded one would break the determinism rule (CONTRIBUTING § World
 * generation rules).
 *
 * <p>
 * Spec: docs/original-spec/01-core/noise-and-random.md#qualityrandom. Side: both.
 */
public final class QualityRandom extends Random {

    private static final long serialVersionUID = 1L;

    /** Initial value of the xorshift state before seeding. */
    private static final long XORSHIFT_START = 4101842887655102017L;
    private static final long LCG_MULTIPLIER = 2862933555777941757L;
    private static final long LCG_INCREMENT = 7046029254386353087L;
    private static final long MWC_MULTIPLIER = 4294957665L;
    private static final long LOW_32_BITS = 0xffffffffL;

    private long u;
    private long v = XORSHIFT_START;
    private long w = 1;
    /** How many 64-bit values have been drawn since seeding; the seeding stir does not count. */
    private long draws;

    /**
     * Seeds the generator the way upstream does: the three states are stirred by three {@link #nextLong()} calls.
     *
     * @param seed the seed
     */
    public QualityRandom(long seed) {
        u = seed ^ v;
        nextLong();
        v = u;
        nextLong();
        w = v;
        nextLong();
        draws = 0;
    }

    /**
     * Returns how many 64-bit values have been drawn since seeding. Every {@code next*} method draws whole values, so
     * a fresh generator with the same seed that {@link #skip skips} this many is in the same state.
     *
     * @return the number of {@link #nextLong()} steps taken
     */
    public long drawCount() {
        return draws;
    }

    /**
     * Advances the generator as if {@code count} values had been drawn and discarded.
     *
     * @param count how many {@link #nextLong()} steps to take
     */
    public void skip(long count) {
        for (long i = 0; i < count; i++) {
            nextLong();
        }
    }

    @Override
    public long nextLong() {
        draws++;
        u = u * LCG_MULTIPLIER + LCG_INCREMENT;
        v ^= v >>> 17;
        v ^= v << 31;
        v ^= v >>> 8;
        w = MWC_MULTIPLIER * (w & LOW_32_BITS) + (w >>> 32);
        long x = u ^ (u << 21);
        x ^= x >>> 35;
        x ^= x << 4;
        return (x + v) ^ w;
    }

    @Override
    protected int next(int bits) {
        return (int) (nextLong() >>> (64 - bits));
    }
}
