package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link RoadDirection}: upstream's order (the tertiary side walk depends on it) and steps. */
public class RoadDirectionTest {

    @Test
    public void upstreamOrderAndSteps() {
        assertArrayEquals(
            new RoadDirection[] { RoadDirection.NORTH, RoadDirection.SOUTH, RoadDirection.WEST, RoadDirection.EAST },
            RoadDirection.values());
        assertEquals(-1, RoadDirection.NORTH.stepZ());
        assertEquals(-1, RoadDirection.WEST.stepX());
        assertTrue(RoadDirection.SOUTH.isNorthSouth());
    }

    @Test
    public void oppositesPair() {
        for (RoadDirection d : RoadDirection.values()) {
            assertEquals(
                d,
                d.opposite()
                    .opposite());
            assertEquals(
                -d.stepX(),
                d.opposite()
                    .stepX());
        }
    }
}
