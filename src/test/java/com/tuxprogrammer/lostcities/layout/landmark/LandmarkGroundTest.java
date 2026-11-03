package com.tuxprogrammer.lostcities.layout.landmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** A landmark's ground estimate, given as a value or computed on its first read (D263). */
public class LandmarkGroundTest {

    private static Landmark.Builder lot() {
        return Landmark.builder("test@0,0", LandmarkKind.WITCHERY)
            .footprint(new BlockRect(0, 0, 15, 15));
    }

    @Test
    public void aGivenValueIsReturned() {
        assertEquals(
            71,
            lot().groundEstimate(71)
                .build()
                .groundEstimate());
    }

    @Test
    public void aLazyEstimateRunsOnTheFirstReadOnlyAndIsKept() {
        int[] runs = { 0 };
        Landmark landmark = lot().groundEstimate(() -> {
            runs[0]++;
            return 68;
        })
            .build();
        assertEquals(0, runs[0], "building the landmark reads no height");
        assertEquals(68, landmark.groundEstimate());
        assertEquals(68, landmark.groundEstimate());
        assertEquals(1, runs[0], "computed once");
    }

    @Test
    public void anAbandonedReadIsAskedAgain() {
        int[] runs = { 0 };
        Landmark landmark = lot().groundEstimate(() -> {
            if (runs[0]++ == 0) {
                throw new IllegalStateException("not cached yet");
            }
            return 64;
        })
            .build();
        assertThrows(IllegalStateException.class, landmark::groundEstimate);
        assertEquals(64, landmark.groundEstimate(), "a read that threw leaves the estimate to the next read");
    }

    @Test
    public void theLastSetterWins() {
        assertEquals(
            70,
            lot().groundEstimate(() -> 50)
                .groundEstimate(70)
                .build()
                .groundEstimate());
        assertEquals(
            50,
            lot().groundEstimate(70)
                .groundEstimate(() -> 50)
                .build()
                .groundEstimate());
    }
}
