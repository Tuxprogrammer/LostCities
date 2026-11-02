package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link PlannedRoadType}: upstream's order and its {@code strongest}. */
public class PlannedRoadTypeTest {

    @Test
    public void strongestPicksTheHigherClass() {
        assertEquals(
            PlannedRoadType.PRIMARY,
            PlannedRoadType.strongest(PlannedRoadType.TERTIARY, PlannedRoadType.PRIMARY));
        assertEquals(
            PlannedRoadType.SECONDARY,
            PlannedRoadType.strongest(PlannedRoadType.SECONDARY, PlannedRoadType.NONE));
        assertEquals(
            PlannedRoadType.TERTIARY,
            PlannedRoadType.strongest(PlannedRoadType.TERTIARY, PlannedRoadType.TERTIARY));
    }

    @Test
    public void onlyNoneIsNotARoad() {
        assertFalse(PlannedRoadType.NONE.isRoad());
        assertTrue(PlannedRoadType.TERTIARY.isRoad());
        assertTrue(PlannedRoadType.PRIMARY.isRoad());
    }
}
