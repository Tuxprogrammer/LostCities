package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;

/** The ground estimate: the median oracle height at each box's center and corners. */
class GroundEstimatesTest {

    @Test
    void medianOfCentersAndCorners() {
        // Height = X: one box at X 0..10 samples 5, 0, 10, 0, 10; another at X 20..30 samples 25, 20, 30, 20, 30.
        int y = GroundEstimates.median(
            (x, z) -> x,
            Arrays.asList(new BlockRect(0, 0, 10, 10), new BlockRect(20, 0, 30, 10)));
        assertEquals(20, y);
    }

    @Test
    void oneOutlierDoesNotMoveIt() {
        int y = GroundEstimates.median((x, z) -> x == 5 && z == 5 ? 200 : 70, Collections.singletonList(new BlockRect(0, 0, 10, 10)));
        assertEquals(70, y);
    }

    @Test
    void fallsBackWithoutAnOracleOrBoxes() {
        assertEquals(GroundEstimates.FALLBACK_Y, GroundEstimates.median(null, Collections.singletonList(new BlockRect(0, 0, 1, 1))));
        assertEquals(GroundEstimates.FALLBACK_Y, GroundEstimates.median((x, z) -> 90, Collections.<BlockRect>emptyList()));
    }
}
