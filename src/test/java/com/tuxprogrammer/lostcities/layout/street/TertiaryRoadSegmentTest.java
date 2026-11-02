package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link TertiaryRoadSegment#contains}: the chunks 1 to length along the direction, on the origin's line. */
public class TertiaryRoadSegmentTest {

    @Test
    public void coversOneToLengthAlongItsLine() {
        TertiaryRoadSegment s = new TertiaryRoadSegment(0L, 10, 20, RoadDirection.SOUTH, 3);
        assertFalse(s.contains(10, 20), "the origin lies on the boundary road");
        assertTrue(s.contains(10, 21));
        assertTrue(s.contains(10, 23));
        assertFalse(s.contains(10, 24));
        assertFalse(s.contains(11, 21), "off the line");
        assertFalse(s.contains(10, 19), "behind the origin");
    }

    @Test
    public void westwardSegment() {
        TertiaryRoadSegment s = new TertiaryRoadSegment(0L, 0, 0, RoadDirection.WEST, 2);
        assertTrue(s.contains(-1, 0));
        assertTrue(s.contains(-2, 0));
        assertFalse(s.contains(1, 0));
    }
}
