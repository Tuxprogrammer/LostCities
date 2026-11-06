package com.tuxprogrammer.lostcities.layout.highway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** A run's level profile: ramps of one level a chunk at each end, never on a corner (design record 0013). */
public class RunTest {

    static Run run(int startLevel, int endLevel, Segment... segments) {
        Segment first = segments[0];
        Segment last = segments[segments.length - 1];
        return new Run(
            Arrays.asList(segments),
            new Run.End(Run.EndKind.HUB, first.startX(), first.startZ(), startLevel),
            new Run.End(Run.EndKind.HUB, last.endX(), last.endZ(), endLevel));
    }

    @Test
    public void theDeckClimbsFromEachEndToTheCruiseLevelOneLevelPerChunk() {
        Run run = run(0, 1, new Segment(0, 0, 10, 0, Axis.X));
        int cruise = 3;
        assertEquals(0, run.entryLevel(0, cruise));
        assertEquals(0, run.exitLevel(0, cruise), "the end chunk is flat");
        assertEquals(1, run.exitLevel(1, cruise));
        assertEquals(3, run.exitLevel(3, cruise));
        assertEquals(3, run.entryLevel(6, cruise));
        assertEquals(1, run.entryLevel(10, cruise));
        assertEquals(1, run.exitLevel(10, cruise));
        for (int i = 0; i <= 10; i++) {
            assertTrue(Math.abs(run.exitLevel(i, cruise) - run.entryLevel(i, cruise)) <= 1);
            if (i > 0) {
                assertEquals(run.exitLevel(i - 1, cruise), run.entryLevel(i, cruise), "continuous at " + i);
            }
        }
    }

    @Test
    public void aCornerStaysFlat() {
        // Two segments: x 0 to 2, then z 0 to 4; the corner at index 2.
        Run run = run(0, 0, new Segment(0, 0, 2, 0, Axis.X), new Segment(2, 0, 2, 4, Axis.Z));
        assertTrue(run.isCorner(2));
        int cruise = 5;
        assertEquals(run.entryLevel(2, cruise), run.exitLevel(2, cruise));
        assertEquals(2, run.indexOf(2, 0));
        assertEquals(6, run.length());
    }

    @Test
    public void aRunTooShortToRampAllTheWayArrivesAsNearAsItCan() {
        Run run = run(0, 5, new Segment(0, 0, 3, 0, Axis.X));
        // Two ramp chunks (indices 1 and 2): the end is reached at level 2.
        assertEquals(2, run.reachableEndLevel());
        assertEquals(2, run.exitLevel(3, 9));
    }

    /**
     * KI-5 (RWG2 seed 1): two routes leave one hub at level 7 eastward; one turns south at index 3, inside the other's
     * descent. The other run holds index 3 flat, so both reach it at the same level and the turn gets its junction.
     */
    @Test
    public void aHeldChunkStaysFlatAndTheRampMovesPastIt() {
        Run run = run(7, 0, new Segment(0, 0, 10, 0, Axis.X));
        int cruise = 3;
        int[] held = run.holds(new int[] { 3, 0, 10, 42, 3 });
        assertTrue(Arrays.equals(new int[] { 3 }, held), "only an inner index, once");
        assertEquals(5, run.entryLevel(3, cruise, held));
        assertEquals(5, run.exitLevel(3, cruise, held), "the held chunk is flat");
        assertEquals(4, run.exitLevel(4, cruise, held), "the descent goes on after it");
        assertEquals(4, run.exitLevel(3, cruise), "without holds it ramps there");
        for (int i = 1; i <= run.length(); i++) {
            assertEquals(run.exitLevel(i - 1, cruise, held), run.entryLevel(i, cruise, held), "continuous at " + i);
        }
    }

    @Test
    public void aRunsOwnCornerIsNeverHeldTwice() {
        Run run = run(0, 0, new Segment(0, 0, 4, 0, Axis.X), new Segment(4, 0, 4, 4, Axis.Z));
        assertEquals(0, run.holds(new int[] { 4 }).length);
    }
}
