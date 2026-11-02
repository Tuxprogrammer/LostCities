package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.street.HierarchicalStreetPlanner;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/** {@link Sewing}: detours of severed primaries, village lanes and gates, and surface links (0006 §3). */
public class SewingTest {

    @Test
    public void severedPrimaryDetoursWestOnATie() {
        long seed = 31L;
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            seed,
            DimensionKey.OVERWORLD,
            LayoutProfile.defaults().streets);
        int px = streets.primaryOffsetX();
        int z = 3;
        while (streets.isHorizontalPrimary(z)) {
            z++;
        }
        LayoutPlanner p = LayoutFixtures.cityWith(
            seed,
            480,
            LayoutFixtures
                .landmark("room", LandmarkKind.LOOTGAMES, px * 16 + 4, z * 16 + 4, px * 16 + 11, z * 16 + 11));
        assertFalse(
            p.plan(px, z)
                .isCity());
        int[][] path = { { px, z - 1 }, { px - 1, z - 1 }, { px - 1, z }, { px - 1, z + 1 }, { px, z + 1 } };
        for (int[] c : path) {
            ChunkPlan plan = p.plan(c[0], c[1]);
            assertEquals(
                LaneRole.DETOUR,
                plan.lane()
                    .role(),
                c[0] + "," + c[1]);
            assertEquals(PlannedRoadType.PRIMARY, plan.plannedRoad(), c[0] + "," + c[1]);
            assertFalse(plan.hasBuilding());
        }
        LaneCell corner = p.plan(px - 1, z - 1)
            .lane();
        assertTrue(corner.connects(RoadDirection.EAST) && corner.connects(RoadDirection.SOUTH));
        assertEquals(
            LaneRole.NONE,
            p.plan(px + 1, z)
                .lane()
                .role(),
            "one side only");
    }

    @Test
    public void aGateGetsALaneToTheNearestRoad() {
        Landmark village = Landmark.builder("v", LandmarkKind.VILLAGE)
            .footprint(new BlockRect(84, 84, 91, 91))
            .gate(new Gate(91, 88, RoadDirection.EAST))
            .build();
        LayoutPlanner p = LayoutFixtures.cityWith(33L, 480, village);
        ChunkPlan gate = p.plan(6, 5);
        assertEquals(
            LaneRole.GATE,
            gate.lane()
                .role());
        assertNotNull(
            gate.lane()
                .gate());
        assertTrue(
            gate.lane()
                .connects(RoadDirection.WEST),
            "the gate chunk leads into the core");
        assertTrue(
            gate.plannedRoad()
                .isRoad());
        assertFalse(gate.hasBuilding());
        // Follow the lane away from the core until it meets a road chunk that is not part of it.
        int x = 6;
        int z = 5;
        RoadDirection from = RoadDirection.WEST;
        for (int steps = 0; steps < 10; steps++) {
            LaneCell cell = p.plan(x, z)
                .lane();
            RoadDirection next = null;
            for (RoadDirection d : RoadDirection.values()) {
                if (d != from && cell.connects(d)) {
                    next = d;
                }
            }
            if (next == null) {
                // A gate chunk on a road needs no lane.
                assertTrue(
                    p.plan(x, z)
                        .rawRoad()
                        .isRoad(),
                    "lane end " + x + "," + z);
                return;
            }
            x += next.stepX();
            z += next.stepZ();
            from = next.opposite();
            ChunkPlan step = p.plan(x, z);
            if (step.lane()
                .role() == LaneRole.NONE) {
                // The lane has reached the road it was laid to.
                assertTrue(step.isPlannedRoad(), "lane target " + x + "," + z);
                return;
            }
            assertEquals(
                LaneRole.LANE,
                step.lane()
                    .role(),
                x + "," + z);
            assertTrue(
                step.plannedRoad()
                    .isRoad());
        }
        org.junit.jupiter.api.Assertions.fail("the lane never ended");
    }

    @Test
    public void aVillageJustOutsideTheCityGetsASurfaceLink() {
        long seed = 35L;
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            seed,
            DimensionKey.OVERWORLD,
            LayoutProfile.defaults().streets);
        int row = streets.primaryOffsetZ();
        Landmark village = Landmark.builder("far", LandmarkKind.VILLAGE)
            .footprint(new BlockRect(182, row * 16 + 2, 200, row * 16 + 13))
            .gate(new Gate(182, row * 16 + 8, RoadDirection.WEST))
            .build();
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    LayoutFixtures.predefinedOnly(),
                    LayoutFixtures.standardAssets(),
                    LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, row, 160, "citystyle_common"))
                .structures(new LayoutFixtures.FakeOracle(village))
                .build());
        assertTrue(
            p.plan(7, row)
                .isPrimaryRoad(),
            "the city's primary road ends at chunk 7");
        assertFalse(
            p.plan(8, row)
                .isCity());
        assertEquals(
            LaneRole.GATE,
            p.plan(10, row)
                .lane()
                .role());
        assertEquals(
            LaneRole.LINK,
            p.plan(9, row)
                .lane()
                .role());
        assertEquals(
            LaneRole.LINK,
            p.plan(8, row)
                .lane()
                .role());
        assertTrue(
            p.plan(8, row)
                .lane()
                .connects(RoadDirection.WEST));
    }

    @Test
    public void villagesBeyondTheLinkDistanceAreLeftAlone() {
        Landmark village = Landmark.builder("remote", LandmarkKind.VILLAGE)
            .footprint(new BlockRect(400, 2, 420, 14))
            .gate(new Gate(400, 8, RoadDirection.WEST))
            .build();
        LayoutPlanner p = LayoutFixtures.cityWith(36L, 160, village);
        for (int x = 8; x < 25; x++) {
            LaneRole role = p.plan(x, 0)
                .lane()
                .role();
            assertTrue(role == LaneRole.NONE || role == LaneRole.GATE, x + " " + role);
        }
    }
}
