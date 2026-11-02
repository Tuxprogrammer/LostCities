package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.random.DimensionKey;

/** {@link PlannedStreetInfo#connects}: both this chunk and the neighbor have a raw road. */
public class PlannedStreetInfoTest {

    @Test
    public void connectionsFollowNeighborRoads() {
        HierarchicalStreetPlanner planner = new HierarchicalStreetPlanner(
            5L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        int checked = 0;
        for (int x = -30; x < 30; x++) {
            for (int z = -30; z < 30; z++) {
                PlannedStreetInfo info = planner.getStreetInfo(x, z);
                for (RoadDirection d : RoadDirection.values()) {
                    boolean expected = info.isRoad() && planner.getRoadType(x + d.stepX(), z + d.stepZ())
                        .isRoad();
                    assertEquals(expected, info.connects(d));
                    checked++;
                }
            }
        }
        assertTrue(checked > 0);
    }
}
