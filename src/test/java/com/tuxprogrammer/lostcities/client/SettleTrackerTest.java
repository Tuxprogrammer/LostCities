package com.tuxprogrammer.lostcities.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.client.SettleTracker.Event;

/**
 * When a capture viewpoint counts as settled: still, every chunk in, the build queue empty, each long enough; and
 * when it stops counting.
 */
public class SettleTrackerTest {

    /** Feeds the same observation for a number of ticks and returns the tick (1-based) that settled, or -1. */
    private static int ticksUntilSettled(SettleTracker t, int ticks, double x, int missing, boolean built) {
        for (int i = 1; i <= ticks; i++) {
            if (t.tick(x, 100, 0, 45f, 30f, missing, built) == Event.SETTLED) {
                return i;
            }
        }
        return -1;
    }

    @Test
    public void settlesAfterStillAndCompleteLongEnough() {
        SettleTracker t = new SettleTracker();
        // The first tick only records the camera; the next STILL_TICKS ticks count.
        assertEquals(SettleTracker.STILL_TICKS + 1, ticksUntilSettled(t, 100, 0, 0, true));
        assertTrue(t.settled());
        assertEquals(Event.NONE, t.tick(0, 100, 0, 45f, 30f, 0, true), "settles once, not every tick");
    }

    @Test
    public void missingChunksOrABusyRendererHoldItBack() {
        SettleTracker t = new SettleTracker();
        assertEquals(-1, ticksUntilSettled(t, 200, 0, 3, true));
        assertEquals(-1, ticksUntilSettled(t, 200, 0, 0, false));
        assertFalse(t.settled());
        assertEquals(SettleTracker.COMPLETE_TICKS, ticksUntilSettled(t, 100, 0, 0, true));
    }

    @Test
    public void aOneTickLullInTheQueueDoesNotCount() {
        SettleTracker t = new SettleTracker();
        ticksUntilSettled(t, 40, 0, 0, false);
        for (int i = 0; i < SettleTracker.COMPLETE_TICKS - 1; i++) {
            assertEquals(Event.NONE, t.tick(0, 100, 0, 45f, 30f, 0, true));
        }
        assertEquals(Event.NONE, t.tick(0, 100, 0, 45f, 30f, 0, false), "the queue filled again");
        assertEquals(SettleTracker.COMPLETE_TICKS, ticksUntilSettled(t, 100, 0, 0, true));
    }

    @Test
    public void movingAwayUnsettlesAndRestartsTheCount() {
        SettleTracker t = new SettleTracker();
        ticksUntilSettled(t, 100, 0, 0, true);
        assertEquals(Event.MOVED, t.tick(500, 100, 0, 45f, 30f, 0, true));
        assertFalse(t.settled());
        assertEquals(0, t.ticksSinceMove());
        assertEquals(SettleTracker.STILL_TICKS, ticksUntilSettled(t, 100, 500, 0, true));
    }

    @Test
    public void turningCountsAsMovingButJitterDoesNot() {
        SettleTracker t = new SettleTracker();
        ticksUntilSettled(t, 100, 0, 0, true);
        assertEquals(Event.NONE, t.tick(0.01, 100, 0, 45.1f, 30f, 0, true), "below the epsilons");
        assertTrue(t.settled());
        assertEquals(Event.MOVED, t.tick(0.01, 100, 0, 90f, 30f, 0, true));
    }

    @Test
    public void resetForgetsTheViewpoint() {
        SettleTracker t = new SettleTracker();
        ticksUntilSettled(t, 100, 0, 0, true);
        t.reset();
        assertFalse(t.settled());
        assertEquals(SettleTracker.STILL_TICKS + 1, ticksUntilSettled(t, 100, 0, 0, true));
    }
}
