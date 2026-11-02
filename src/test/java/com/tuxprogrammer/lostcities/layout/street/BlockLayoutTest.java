package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.random.DimensionKey;

/**
 * {@link BlockLayout}: a block lies between two active primaries on each axis, its secondary roads keep their edge
 * distance and separation, and its lists are read-only. The values themselves are checked against upstream in
 * {@link HierarchicalStreetPlannerTest}.
 */
public class BlockLayoutTest {

    @Test
    public void secondaryRoadsKeepTheirDistances() {
        HierarchicalStreetPlanner planner = new HierarchicalStreetPlanner(
            11L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        for (int x = -200; x < 200; x += 13) {
            BlockLayout b = planner.getBlockLayout(x, 5);
            assertTrue(planner.isVerticalPrimary(b.westX()));
            assertTrue(planner.isVerticalPrimary(b.eastX()));
            assertTrue(b.westX() <= x && x < b.eastX());
            int previous = Integer.MIN_VALUE / 2;
            for (int s : b.secondaryX()) {
                assertTrue(s - b.westX() >= 3 && b.eastX() - s >= 3, "edge distance");
                assertTrue(s - previous >= 4, "separation");
                previous = s;
            }
        }
    }

    @Test
    public void listsAreReadOnly() {
        BlockLayout b = new HierarchicalStreetPlanner(1L, DimensionKey.OVERWORLD, StreetPlannerSettings.DEFAULTS)
            .getBlockLayout(0, 0);
        assertThrows(
            UnsupportedOperationException.class,
            () -> b.secondaryX()
                .add(1));
        assertEquals(b.eastX() - b.westX() > 0, true);
    }
}
