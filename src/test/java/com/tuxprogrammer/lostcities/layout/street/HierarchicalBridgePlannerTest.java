package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ParityData;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;

/**
 * {@link HierarchicalBridgePlanner}: the span hashes and preferred intersection orientations are upstream's (recorded
 * values), and spans form over water gaps between level-0 primary roads, never across a landmark core.
 */
public class HierarchicalBridgePlannerTest {

    /** A world strip: city west of {@code westEdge} and east of {@code eastEdge}, water between. */
    private static final class Strip implements HierarchicalBridgePlanner.Context {

        final int westEdge;
        final int eastEdge;
        int coreX = Integer.MIN_VALUE;

        Strip(int westEdge, int eastEdge) {
            this.westEdge = westEdge;
            this.eastEdge = eastEdge;
        }

        @Override
        public boolean isCityRaw(int x, int z) {
            return x <= westEdge || x >= eastEdge;
        }

        @Override
        public boolean isWater(int x, int z) {
            return !isCityRaw(x, z);
        }

        @Override
        public boolean isLandmarkCore(int x, int z) {
            return x == coreX;
        }

        @Override
        public boolean isLevelZeroPrimaryRoad(int x, int z) {
            return isCityRaw(x, z);
        }
    }

    @Test
    public void spanHashesMatchUpstream() throws Exception {
        List<String[]> records = ParityData.records("bridge_hashes.txt");
        int spans = 0;
        for (String[] r : records) {
            long seed = Long.parseLong(r[1]);
            DimensionKey dimension = DimensionKey.parse(r[2]);
            if (r[0].equals("preferred")) {
                assertEquals(
                    Orientation.valueOf(r[3]),
                    HierarchicalBridgePlanner.preferredIntersectionOrientation(seed, dimension),
                    "seed " + seed);
            } else {
                HierarchicalBridgePlanner planner = new HierarchicalBridgePlanner(seed, dimension, null, 1f, 12);
                long hash = planner.spanHash(
                    Orientation.valueOf(r[3]),
                    Integer.parseInt(r[4]),
                    Integer.parseInt(r[5]),
                    Integer.parseInt(r[6]),
                    Integer.parseInt(r[7]));
                assertEquals(Long.parseLong(r[8]), hash, String.join(" ", r));
                spans++;
            }
        }
        assertTrue(spans > 50);
    }

    @Test
    public void bridgesAWaterGapOnAnEastWestPrimary() {
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            21L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        HierarchicalBridgePlanner planner = new HierarchicalBridgePlanner(21L, DimensionKey.OVERWORLD, streets, 1f, 12);
        int row = streets.primaryOffsetZ();
        int column = gapColumn(streets, 0, 6);
        Strip strip = new Strip(column - 1, column + 4);
        PlannedBridgeInfo span = planner.getBridgeInfo(strip, column + 1, row, false, Orientation.X);
        assertNotNull(span);
        assertEquals(column - 1, span.minimumX());
        assertEquals(column + 4, span.maximumX());
        assertEquals(4, span.gapLength());
        assertNull(planner.getBridgeInfo(strip, column + 1, row, true, Orientation.X), "a city chunk has no span");
        assertNull(planner.getBridgeInfo(strip, column + 1, row + 1, false, Orientation.X), "off the primary row");
    }

    @Test
    public void aSpanNeverCrossesALandmarkCore() {
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            21L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        HierarchicalBridgePlanner planner = new HierarchicalBridgePlanner(21L, DimensionKey.OVERWORLD, streets, 1f, 12);
        int row = streets.primaryOffsetZ();
        int column = gapColumn(streets, 0, 6);
        Strip strip = new Strip(column - 1, column + 4);
        strip.coreX = column + 2;
        assertNull(planner.getBridgeInfo(strip, column + 1, row, false, Orientation.X));
        assertNull(planner.getBridgeInfo(strip, column + 2, row, false, Orientation.X));
    }

    @Test
    public void tooLongAGapHasNoSpan() {
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            21L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        HierarchicalBridgePlanner planner = new HierarchicalBridgePlanner(21L, DimensionKey.OVERWORLD, streets, 1f, 3);
        int row = streets.primaryOffsetZ();
        int column = gapColumn(streets, 0, 6);
        Strip strip = new Strip(column - 1, column + 4);
        assertNull(planner.getBridgeInfo(strip, column + 1, row, false, Orientation.X));
    }

    /** A column from which the next {@code width} columns hold no north-south primary (so no intersections). */
    private static int gapColumn(HierarchicalStreetPlanner streets, int from, int width) {
        for (int x = from;; x++) {
            boolean clear = true;
            for (int i = -1; i <= width; i++) {
                clear &= !streets.isVerticalPrimary(x + i);
            }
            if (clear) {
                return x;
            }
        }
    }
}
