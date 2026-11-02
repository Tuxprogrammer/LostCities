package com.tuxprogrammer.lostcities.worldgen.timing;

import java.util.Arrays;

/**
 * A fixed-bucket histogram of durations in nanoseconds, with no allocation per sample (docs/design/0005-rwg-overlay.md
 * §8). Buckets are log-linear: below 1024 ns one bucket, then eight buckets per power of two, so a quantile is
 * reported to within 12.5 % (the bucket's upper bound). Count, sum and maximum are exact.
 *
 * <p>
 * Timing is measurement only: nothing reads it to decide what is generated. Not thread-safe; the server thread
 * records. Side: both (plain Java).
 */
public final class Histogram {

    /** Sub-buckets per power of two. */
    static final int SUB_BUCKETS = 8;
    /** log2 of the first bucket's upper bound: 1024 ns. */
    static final int FIRST_POWER = 10;
    /** log2 of the largest value with its own bucket: about 18 minutes. */
    static final int LAST_POWER = 40;
    /** Number of buckets. */
    static final int BUCKETS = 1 + (LAST_POWER - FIRST_POWER + 1) * SUB_BUCKETS;

    private final long[] counts = new long[BUCKETS];
    private long count;
    private long sum;
    private long max;

    /**
     * Records one duration.
     *
     * @param nanos the duration in nanoseconds; negative values count as 0
     */
    public void record(long nanos) {
        long value = Math.max(0, nanos);
        counts[bucket(value)]++;
        count++;
        sum += value;
        max = Math.max(max, value);
    }

    /** Returns the number of samples. */
    public long count() {
        return count;
    }

    /** Returns the sum of all samples, in nanoseconds. */
    public long sum() {
        return sum;
    }

    /** Returns the largest sample, in nanoseconds. */
    public long max() {
        return max;
    }

    /**
     * Returns a quantile, as the upper bound of the bucket it falls in (never above the maximum).
     *
     * @param quantile 0 to 1, for example 0.95
     * @return nanoseconds; 0 without samples
     */
    public long quantile(double quantile) {
        if (count == 0) {
            return 0;
        }
        long rank = (long) Math.ceil(quantile * count);
        rank = Math.max(1, Math.min(count, rank));
        long seen = 0;
        for (int i = 0; i < BUCKETS; i++) {
            seen += counts[i];
            if (seen >= rank) {
                return Math.min(upperBound(i), max);
            }
        }
        return max;
    }

    /** Forgets every sample. */
    public void reset() {
        Arrays.fill(counts, 0);
        count = 0;
        sum = 0;
        max = 0;
    }

    static int bucket(long value) {
        if (value < (1L << FIRST_POWER)) {
            return 0;
        }
        int power = 63 - Long.numberOfLeadingZeros(value);
        if (power > LAST_POWER) {
            return BUCKETS - 1;
        }
        int sub = (int) ((value >>> (power - 3)) & (SUB_BUCKETS - 1));
        return 1 + (power - FIRST_POWER) * SUB_BUCKETS + sub;
    }

    static long upperBound(int bucket) {
        if (bucket == 0) {
            return (1L << FIRST_POWER) - 1;
        }
        int power = FIRST_POWER + (bucket - 1) / SUB_BUCKETS;
        int sub = (bucket - 1) % SUB_BUCKETS;
        long base = 1L << power;
        long step = base / SUB_BUCKETS;
        return base + step * (sub + 1) - 1;
    }
}
