package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;

/** {@link MultiBuildingStreetConflict}: which roads block a multibuilding, and locale-independent parsing. */
public class MultiBuildingStreetConflictTest {

    @Test
    public void policies() {
        assertTrue(MultiBuildingStreetConflict.BLOCK_ALL.roadBlocks(PlannedRoadType.TERTIARY));
        assertFalse(MultiBuildingStreetConflict.BLOCK_ALL.roadBlocks(PlannedRoadType.NONE));
        assertTrue(MultiBuildingStreetConflict.OVERRIDE_MINOR.roadBlocks(PlannedRoadType.PRIMARY));
        assertFalse(MultiBuildingStreetConflict.OVERRIDE_MINOR.roadBlocks(PlannedRoadType.SECONDARY));
        assertFalse(MultiBuildingStreetConflict.OVERRIDE_ALL.roadBlocks(PlannedRoadType.PRIMARY));
    }

    @Test
    public void parsesIgnoringCase() {
        assertEquals(MultiBuildingStreetConflict.OVERRIDE_MINOR, MultiBuildingStreetConflict.byName("override_minor"));
        assertThrows(IllegalArgumentException.class, () -> MultiBuildingStreetConflict.byName("none"));
    }
}
