package com.tuxprogrammer.lostcities.layout.perf;

import java.util.Arrays;
import java.util.Locale;

/**
 * Server tick durations of the {@code perf.walk} scenario (D-perf3-4): kept as a plain array of nanoseconds, so the
 * mean, the 95th and 99th percentiles and the longest tick are exact, not bucketed. Not thread-safe: the server thread
 * records and reports.
 */
public final class TickStats {

    private long[] nanos;
    private int size;

    /**
     * Creates an empty record.
     *
     * @param expected ticks expected, to size the array; it grows past this
     */
    public TickStats(int expected) {
        nanos = new long[Math.max(16, expected)];
    }

    /**
     * Records one tick.
     *
     * @param tickNanos its duration in nanoseconds
     */
    public void add(long tickNanos) {
        if (size == nanos.length) {
            nanos = Arrays.copyOf(nanos, size * 2);
        }
        nanos[size++] = tickNanos;
    }

    /**
     * Returns the ticks recorded.
     *
     * @return the count
     */
    public int size() {
        return size;
    }

    /**
     * Returns the mean.
     *
     * @return the mean tick in nanoseconds, 0 when empty
     */
    public double mean() {
        if (size == 0) {
            return 0.0;
        }
        long sum = 0L;
        for (int i = 0; i < size; i++) {
            sum += nanos[i];
        }
        return (double) sum / size;
    }

    /**
     * Returns a percentile by the nearest-rank rule: the smallest tick at least {@code quantile} of the ticks are no
     * longer than.
     *
     * @param quantile in (0, 1]
     * @return the tick in nanoseconds, 0 when empty
     */
    public long quantile(double quantile) {
        if (size == 0) {
            return 0L;
        }
        long[] sorted = Arrays.copyOf(nanos, size);
        Arrays.sort(sorted);
        int rank = (int) Math.ceil(quantile * size);
        return sorted[Math.min(size, Math.max(1, rank)) - 1];
    }

    /**
     * Returns the longest tick.
     *
     * @return nanoseconds, 0 when empty
     */
    public long max() {
        long max = 0L;
        for (int i = 0; i < size; i++) {
            max = Math.max(max, nanos[i]);
        }
        return max;
    }

    /**
     * Returns how many ticks took longer than a limit.
     *
     * @param limitNanos the limit, for example 50 ms
     * @return the count of ticks above it
     */
    public int over(long limitNanos) {
        int over = 0;
        for (int i = 0; i < size; i++) {
            if (nanos[i] > limitNanos) {
                over++;
            }
        }
        return over;
    }

    /**
     * Formats the summary.
     *
     * @return {@code "n=… mean=… p95=… p99=… max=… ms, over 50 ms …"}
     */
    public String summary() {
        return String.format(
            Locale.ROOT,
            "n=%d mean=%.2f p95=%.2f p99=%.2f max=%.2f ms, over 50 ms %d",
            size,
            mean() / 1e6,
            quantile(0.95) / 1e6,
            quantile(0.99) / 1e6,
            max() / 1e6,
            over(50_000_000L));
    }
}
