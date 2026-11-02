package com.tuxprogrammer.lostcities.worldgen.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link GroundFit}: distance to the pads, the fade's weight, the target height and the chunks it reaches. */
public class GroundFitTest {

    /** One pad over blocks 3..12 of chunk (0, 0), ground at Y 70. */
    private static final GroundFit FIT = new GroundFit(new int[] { 3, 3, 12, 12 }, 70, GroundFit.Mode.LEVEL);

    @Test
    public void distanceIsZeroOnThePadAndEuclideanOutside() {
        assertEquals(0.0, FIT.distance(5, 5), 1e-9);
        assertEquals(0.0, FIT.distance(12, 3), 1e-9);
        assertEquals(2.0, FIT.distance(14, 8), 1e-9);
        assertEquals(5.0, FIT.distance(15, 16), 1e-9);
    }

    @Test
    public void weightIsOneOnTheWalkwayAndFadesSmoothlyToZero() {
        assertEquals(1.0, GroundFit.weight(0), 1e-9);
        assertEquals(1.0, GroundFit.weight(GroundFit.WALKWAY), 1e-9);
        assertEquals(0.0, GroundFit.weight(GroundFit.WALKWAY + GroundFit.RADIUS + 1), 1e-9);
        double previous = 1.0;
        for (double d = GroundFit.WALKWAY; d <= GroundFit.WALKWAY + GroundFit.RADIUS + 1; d += 0.25) {
            double w = GroundFit.weight(d);
            assertTrue(w <= previous + 1e-12, "monotonic at " + d);
            previous = w;
        }
        // The smooth step's steepest point: 1.5 / 13 of the height difference per block.
        assertTrue(GroundFit.weight(7.5) - GroundFit.weight(8.5) < 1.5 / 13 + 1e-9);
    }

    @Test
    public void targetLevelsThePadAndLeavesFarGroundAlone() {
        assertEquals(70, FIT.target(75, 6, 6));
        assertEquals(70, FIT.target(62, 6, 6));
        assertEquals(90, FIT.target(90, 60, 60));
        int halfway = FIT.target(80, 12 + 1 + 7, 8);
        assertTrue(halfway > 70 && halfway < 80, "halfway " + halfway);
    }

    @Test
    public void cutOnlyNeverRaisesGround() {
        GroundFit cut = new GroundFit(new int[] { 0, 0, 15, 15 }, 70, GroundFit.Mode.CUT);
        assertEquals(60, cut.target(60, 5, 5));
        assertEquals(70, cut.target(80, 5, 5));
    }

    @Test
    public void theFadeLengthensWhereTheGroundLiesFarFromThePad() {
        assertEquals(GroundFit.RADIUS, FIT.radiusFor(70), 1e-9);
        assertEquals(GroundFit.RADIUS, FIT.radiusFor(64), 1e-9);
        assertEquals(15.0, FIT.radiusFor(80), 1e-9);
        assertEquals(GroundFit.MAX_RADIUS, FIT.radiusFor(40), 1e-9);
        // 20 blocks below the pad, 20 blocks out, the column still moves; over the shortest fade it would not.
        assertTrue(FIT.weightAt(50, 12 + 20, 8) > 0);
        assertEquals(0.0, GroundFit.weight(20), 1e-9);
        assertTrue(FIT.beyond(12 + GroundFit.WALKWAY + GroundFit.MAX_RADIUS + 1, 8));
        assertFalse(FIT.beyond(12 + GroundFit.WALKWAY + GroundFit.MAX_RADIUS, 8));
    }

    @Test
    public void reachesTheChunksWithinTheFade() {
        assertTrue(FIT.reaches(0, 0));
        assertTrue(FIT.reaches(1, 1));
        assertTrue(FIT.reaches(-1, -1));
        assertTrue(FIT.reaches(2, 0));
        assertFalse(FIT.reaches(3, 0));
        assertFalse(FIT.reaches(0, -3));
    }

    @Test
    public void rejectsPartialPads() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new GroundFit(new int[] { 1, 2, 3 }, 70, GroundFit.Mode.LEVEL));
        assertThrows(IllegalArgumentException.class, () -> new GroundFit(new int[0], 70, GroundFit.Mode.LEVEL));
    }
}
