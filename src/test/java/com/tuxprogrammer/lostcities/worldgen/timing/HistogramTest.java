package com.tuxprogrammer.lostcities.worldgen.timing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The timing histograms behind {@code /lostcities timers}, which replace upstream's {@code Statistics}
 * (docs/new-spec/01-core/index.md, "Utilities"; avoids CORE-24).
 */
public class HistogramTest {

    @Test
    public void doesNotReproduceCORE24_onlyRecordedSamplesCountAndTheMaximumIsReset() {
        Histogram histogram = new Histogram();
        histogram.record(1500);
        histogram.record(1500);
        histogram.record(3001);
        // Upstream averaged a fixed array of 100 slots with integer division, empty slots included.
        assertEquals(3, histogram.count(), "only recorded samples count");
        assertEquals(6001, histogram.sum(), "the sum is exact, so the mean is sum / count in any precision");
        long median = histogram.quantile(0.5);
        assertTrue(median >= 1500 && median <= 1500 * 9 / 8, "p50 within one bucket of 1500 ns, got " + median);
        assertEquals(3001, histogram.quantile(1.0), "the top quantile never passes the largest sample");
        assertEquals(3001, histogram.max());
        // Upstream's minimum and maximum were all-time; here a reset starts a new window.
        histogram.reset();
        assertEquals(0, histogram.count());
        assertEquals(0, histogram.quantile(0.5), "no samples, no made-up time");
        histogram.record(2000);
        assertEquals(2000, histogram.max(), "the maximum is of the samples since the reset");
    }
}
