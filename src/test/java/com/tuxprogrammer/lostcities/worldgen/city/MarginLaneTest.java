package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.LayoutFixtures;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/**
 * A village sewn into a city across its margin (alpha review AR-7, AR-10): the gate chunk is the margin, its lane is
 * graded from the city's ground, and the street it arrives at opens its border for it.
 */
public class MarginLaneTest {

    /** A 2 × 2-chunk village with a gate on its west side, in the middle of a block of city chunks. */
    private static Landmark village(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        return Landmark.builder("village@" + minX + "," + minZ, LandmarkKind.VILLAGE)
            .footprint(new BlockRect(minX, minZ, minX + 31, minZ + 31))
            .groundEstimate(70)
            .gate(new Gate(minX, minZ + 16, RoadDirection.WEST))
            .build();
    }

    @Test
    public void theGatesMarginIsGradedFromTheStreetWhoseBorderOpensForIt() {
        LayoutCityPlan base = CityFixtures.plan();
        int[] at = CityFixtures.first(12, (x, z) -> {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -3; dz <= 4; dz++) {
                    if (!base.planner()
                        .plan(x + dx, z + dz)
                        .isCity()) {
                        return false;
                    }
                }
            }
            return true;
        });
        assertNotNull(at, "a block of city chunks near the origin");
        Landmark village = village(at[0], at[1]);
        LayoutCityPlan plan = CityFixtures
            .plan(CityFixtures.SEED, new LayoutFixtures.FakeOracle(village), LayoutLog.SILENT);
        // The core is the footprint grown by 2 blocks, so it reaches chunk at[0] - 1; the gate's chunk is the margin
        // just west of it.
        int gateX = at[0] - 2;
        int gateZ = at[1] + 1;
        LaneCell cell = plan.planner()
            .plan(gateX, gateZ)
            .lane();
        assertEquals(LaneRole.GATE, cell.role());
        assertTrue(cell.connects(RoadDirection.WEST), "the lane crosses the margin toward the city");
        ChunkPlan gate = plan.chunk(gateX, gateZ);
        assertFalse(gate.isCity(), "the gate chunk is the margin");
        LanePath lane = gate.lane();
        assertNotNull(lane);
        assertEquals(ChunkPlan.Side.WEST, lane.gradeSide(), "graded from the city side");
        ChunkPlan street = plan.chunk(gateX - 1, gateZ);
        assertTrue(street.isCity());
        assertEquals(street.groundY(), lane.gradeFrom(), "from the city street's ground");
        int openings = plan.borderOpenings(gateX - 1, gateZ, ChunkPlan.Side.EAST);
        for (int along = LaneShapes.MIDDLE - LaneShapes.WIDTH / 2; along
            <= LaneShapes.MIDDLE + LaneShapes.WIDTH / 2; along++) {
            assertTrue((openings & (1 << along)) != 0, "the street's border opens for the lane at " + along);
        }
    }
}
