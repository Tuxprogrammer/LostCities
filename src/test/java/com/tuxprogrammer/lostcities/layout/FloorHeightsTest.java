package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Per-building floor heights (decision log D-floor-heights-1): the arithmetic of slabs, tops and floor lookups. */
public class FloorHeightsTest {

    @Test
    public void uniformHeightsMatchTheSinglePitch() {
        for (int pitch = 6; pitch <= 10; pitch++) {
            FloorHeights h = FloorHeights.uniform(pitch);
            assertTrue(h.isUniform());
            assertEquals(h, FloorHeights.of(pitch, -1, -1), "unset fields take the pitch");
            for (int f = -4; f <= 30; f++) {
                assertEquals(f * pitch, h.offset(f), "offset " + f);
                assertEquals(pitch, h.height(f));
            }
            for (int dy = -40; dy <= 250; dy++) {
                assertEquals(Math.floorDiv(dy, pitch), h.floorOf(dy), "floorOf " + dy + " at " + pitch);
            }
        }
    }

    @Test
    public void houseFloorsAreFiveBlocksSlabToSlab() {
        FloorHeights h = FloorHeights.of(8, 5, -1);
        assertTrue(h.isUniform());
        assertEquals(0, h.offset(0), "the ground floor meets the street where it always did");
        assertEquals(10, h.offset(2));
        assertEquals(-5, h.offset(-1));
        assertEquals("5", h.toString());
    }

    @Test
    public void workshopHasAHighBayUnderLowFloors() {
        FloorHeights h = FloorHeights.of(8, 5, 7);
        assertFalse(h.isUniform());
        assertEquals(7, h.height(0));
        assertEquals(5, h.height(1));
        assertEquals(5, h.height(-1), "cellars take the upper height");
        assertEquals(0, h.offset(0));
        assertEquals(7, h.offset(1));
        assertEquals(12, h.offset(2), "two floors: the roof part stands 12 above the ground");
        assertEquals(-10, h.offset(-2));
        assertEquals(0, h.floorOf(0));
        assertEquals(0, h.floorOf(6));
        assertEquals(1, h.floorOf(7));
        assertEquals(1, h.floorOf(11));
        assertEquals(2, h.floorOf(12));
        assertEquals(-1, h.floorOf(-1));
        assertEquals(-1, h.floorOf(-5));
        assertEquals(-2, h.floorOf(-6));
        // floorOf inverts offset: every floor's slab lies in that floor.
        for (int f = -3; f <= 10; f++) {
            assertEquals(f, h.floorOf(h.offset(f)));
            assertEquals(f, h.floorOf(h.offset(f) + h.height(f) - 1));
        }
        assertEquals("7/5", h.toString());
    }

    @Test
    public void capFloorsCountsTheBuildingsOwnHeights() {
        // MAX_ROOF_FLOOR_Y is 248: at ground 200 the uniform pitch 8 fits 5 floors, 5-block floors fit 9.
        assertEquals(5, LayoutPlanner.capFloors(10, 200, 8));
        assertEquals(LayoutPlanner.capFloors(10, 200, 8), LayoutPlanner.capFloors(10, 200, FloorHeights.uniform(8)));
        assertEquals(9, LayoutPlanner.capFloors(10, 200, FloorHeights.of(8, 5, -1)));
        // The high bay and eight 5-block floors make 47, under 48; a ninth upper floor would make 52.
        assertEquals(9, LayoutPlanner.capFloors(10, 200, FloorHeights.of(8, 5, 7)));
        assertEquals(10, LayoutPlanner.capFloors(10, 195, FloorHeights.of(8, 5, 7)));
    }
}
