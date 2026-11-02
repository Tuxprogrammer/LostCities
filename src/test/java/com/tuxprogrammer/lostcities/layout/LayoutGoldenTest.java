package com.tuxprogrammer.lostcities.layout;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.street.HierarchicalStreetPlanner;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/**
 * Golden layouts with fixed fake structure oracles, the cases 0006's consequences name for M03: a village inside a
 * city, one at its edge, one three chunks out, a severed primary road and a meteorite basin; plus a plain city and a
 * village under upstream's {@code AVOID} treatment. The maps in each golden are the review artifact; their details are
 * pinned by hash (decision log D-M03-3).
 */
public class LayoutGoldenTest {

    /** An irregular village of two pieces with gates on every side, two of them north. */
    private static Landmark village(String id, int offsetX, int offsetZ) {
        return Landmark.builder(id, LandmarkKind.VILLAGE)
            .footprint(new BlockRect(offsetX + 48, offsetZ + 40, offsetX + 90, offsetZ + 70))
            .footprint(new BlockRect(offsetX + 60, offsetZ + 70, offsetX + 75, offsetZ + 95))
            .groundEstimate(70)
            .gate(new Gate(offsetX + 70, offsetZ + 40, RoadDirection.NORTH))
            .gate(new Gate(offsetX + 55, offsetZ + 40, RoadDirection.NORTH))
            .gate(new Gate(offsetX + 90, offsetZ + 55, RoadDirection.EAST))
            .gate(new Gate(offsetX + 68, offsetZ + 95, RoadDirection.SOUTH))
            .gate(new Gate(offsetX + 48, offsetZ + 55, RoadDirection.WEST))
            .build();
    }

    private static LayoutPlanner planner(long seed, LayoutProfile profile, int centerX, int centerZ, int radius,
        Landmark... landmarks) {
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, LayoutFixtures.standardAssets(), LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(centerX, centerZ, radius, "citystyle_common"))
                .structures(new LayoutFixtures.FakeOracle(landmarks))
                .build());
    }

    private static void check(String name, String description, LayoutPlanner p, int minX, int minZ, int maxX, int maxZ)
        throws Exception {
        LayoutGoldens.assertMatches(
            name,
            description,
            LayoutText.maps(p, minX, minZ, maxX, maxZ),
            LayoutText.details(p, minX, minZ, maxX, maxZ));
    }

    @Test
    public void villageInsideACity() throws Exception {
        LayoutPlanner p = planner(101L, LayoutFixtures.predefinedOnly(), 0, 0, 480, village("village", 0, 0));
        check(
            "village_inside_city",
            "A village surrounded by a city (seed 101, predefined city radius 480 at chunk 0,0, flat ground):\n"
                + "core, edge ring, two transition rings, one lane per side with a gate (two north gates; the one\n"
                + "nearer the city center is used), severed roads detouring or ending in plazas.",
            p,
            -4,
            -4,
            16,
            16);
    }

    @Test
    public void villageAtACitysEdge() throws Exception {
        LayoutPlanner p = planner(
            102L,
            LayoutFixtures.predefinedOnly(),
            0,
            0,
            160,
            Landmark.builder("edge_village", LandmarkKind.VILLAGE)
                .footprint(new BlockRect(110, -10, 140, 20))
                .groundEstimate(70)
                .gate(new Gate(110, 5, RoadDirection.WEST))
                .gate(new Gate(140, 5, RoadDirection.EAST))
                .build());
        check(
            "village_at_city_edge",
            "A village on the edge of a city of radius 160 (city out to about chunk 7): the city side is sewn in\n"
                + "with a lane, the far side's gate opens on non-city land.",
            p,
            -4,
            -8,
            14,
            8);
    }

    @Test
    public void villageThreeChunksOutside() throws Exception {
        LayoutPlanner p = planner(
            103L,
            LayoutFixtures.predefinedOnly(),
            0,
            0,
            160,
            Landmark.builder("far_village", LandmarkKind.VILLAGE)
                .footprint(new BlockRect(182, 2, 200, 14))
                .groundEstimate(70)
                .gate(new Gate(182, 8, RoadDirection.WEST))
                .build());
        check(
            "village_outside_city",
            "A village whose gate chunk is three chunks beyond the city edge, within villageLinkDistance (4):\n"
                + "a surface lane (L) crosses the land between to the nearest city road.",
            p,
            -2,
            -6,
            16,
            6);
    }

    @Test
    public void severedPrimaryRoad() throws Exception {
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            104L,
            DimensionKey.OVERWORLD,
            LayoutProfile.defaults().streets);
        int px = streets.primaryOffsetX();
        LayoutPlanner p = planner(
            104L,
            LayoutFixtures.predefinedOnly(),
            0,
            0,
            480,
            Landmark.builder("roguelike_tower", LandmarkKind.ROGUELIKE)
                .footprint(new BlockRect(px * 16 + 2, 36, px * 16 + 13, 60))
                .groundEstimate(70)
                .build());
        check(
            "severed_primary",
            "A Roguelike tower's core on a north-south primary road (chunk X " + px
                + ", Z 2 to 3): the road\n"
                + "detours through the edge ring on the shorter side (ties west).",
            p,
            px - 8,
            -6,
            px + 8,
            10);
    }

    @Test
    public void meteoriteBasin() throws Exception {
        Landmark meteor = Landmark.builder("meteor", LandmarkKind.METEORITE)
            .footprint(new BlockRect(150, 150, 170, 170))
            .groundEstimate(70)
            .heightCap(HeightCap.ae2(160, 160, 70 - 14 - 8 + 1))
            .build();
        LayoutPlanner p = planner(105L, LayoutFixtures.predefinedOnly(), 0, 0, 480, meteor);
        check(
            "meteorite_basin",
            "An AE2 meteorite in a city (impact at block 160,160, cap center Y 49): buildings step down toward\n"
                + "the impact so every roof stays under the crater paraboloid.",
            p,
            0,
            0,
            20,
            20);
    }

    @Test
    public void plainCity() throws Exception {
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs
                .builder(
                    42L,
                    LayoutFixtures.denseCity(),
                    LayoutFixtures.standardAssets(),
                    LayoutFixtures.rollingTerrain())
                .build());
        check(
            "plain_city",
            "Random cities (cityChance 0.02, radius 150 to 200) on rolling ground, no landmarks: upstream's layout\n"
                + "with the D10 seeds.",
            p,
            -12,
            -12,
            12,
            12);
    }

    @Test
    public void villageAvoidedAsUpstream() throws Exception {
        LayoutProfile avoid = LayoutProfile.builder()
            .cityChance(0)
            .villageTreatment(LandmarkTreatment.AVOID)
            .build();
        LayoutPlanner p = planner(101L, avoid, 0, 0, 480, village("village", 0, 0));
        check(
            "village_avoided",
            "The village of village_inside_city under villageTreatment AVOID: upstream's hole and non-city ring,\n"
                + "nothing sewn in.",
            p,
            -4,
            -4,
            16,
            16);
    }
}
