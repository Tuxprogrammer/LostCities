package com.tuxprogrammer.lostcities.network.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** A flood of rejections reaches the log only up to the limit per window, then says how many were dropped. */
public class RateLimitedSinkTest {

    @Test
    public void limitsReportsPerWindowAndCountsTheRest() {
        AtomicLong clock = new AtomicLong();
        RecordingSink log = new RecordingSink();
        RateLimitedSink sink = new RateLimitedSink(log, 2, 100, clock::get);
        Violation violation = new Violation(Rejection.OUT_OF_RANGE, "slot=9");
        for (int i = 0; i < 5; i++) {
            sink.rejected("P", "spammer", violation);
        }
        assertEquals(2, log.rejected.size());
        assertEquals(3, sink.droppedSinceLastReport());

        clock.set(100);
        sink.rejected("P", "spammer", violation);
        assertEquals(3, log.rejected.size());
        assertTrue(log.rejected.get(2).detail().contains("3 earlier reports dropped"), log.rejected.get(2).detail());
        assertEquals(0, sink.droppedSinceLastReport());
    }
}
