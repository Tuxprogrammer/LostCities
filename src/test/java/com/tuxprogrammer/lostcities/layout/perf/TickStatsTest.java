package com.tuxprogrammer.lostcities.layout.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Exact tick statistics of {@link TickStats} (D-perf3-4). */
public class TickStatsTest {

    @Test
    void percentilesUseTheNearestRank() {
        TickStats stats = new TickStats(4);
        for (int i = 100; i >= 1; i--) {
            stats.add(i * 1_000_000L);
        }
        assertEquals(100, stats.size());
        assertEquals(50.5e6, stats.mean(), 1e-6);
        assertEquals(95_000_000L, stats.quantile(0.95));
        assertEquals(99_000_000L, stats.quantile(0.99));
        assertEquals(100_000_000L, stats.max());
        assertEquals(50, stats.over(50_000_000L));
        assertEquals(
            "n=100 mean=50.50 p95=95.00 p99=99.00 max=100.00 ms, over 50 ms 50",
            stats.summary());
    }

    @Test
    void emptyIsZero() {
        TickStats stats = new TickStats(0);
        assertEquals(0.0, stats.mean());
        assertEquals(0L, stats.quantile(0.99));
        assertEquals(0L, stats.max());
    }
}
