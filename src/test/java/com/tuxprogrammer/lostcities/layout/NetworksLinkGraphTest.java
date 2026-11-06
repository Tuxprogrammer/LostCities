package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;

/**
 * The link graph wired to the real planner (0015 §1): built only for {@code networkLayout=links} over grid-placed
 * cities, with nodes that are exactly the centers the terrain filters let through.
 */
public class NetworksLinkGraphTest {

    private static final long SEED = 7919L;

    private static LayoutPlanner planner(boolean grid, boolean links, LayoutFixtures.FakeTerrain terrain) {
        LayoutProfile.Builder profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(100, 256);
        if (grid) {
            profile.cityGrid(64, 16);
        }
        return new LayoutPlanner(
            LayoutInputs.builder(SEED, profile.build(), LayoutFixtures.standardAssets(), terrain)
                .networks(NetworkSettings.DEFAULTS.withLinks(links))
                .build());
    }

    @Test
    public void legacyLayoutBuildsNoGraph() {
        assertNull(
            planner(true, false, LayoutFixtures.flatTerrain()).networks()
                .links());
        assertNull(
            planner(false, true, LayoutFixtures.flatTerrain()).networks()
                .links(),
            "chance placement");
    }

    @Test
    public void linkEndsAreCentersThatYieldCities() {
        LayoutPlanner planner = planner(true, true, LayoutFixtures.flatTerrain());
        CityLinkGraph graph = planner.networks()
            .links();
        assertNotNull(graph);
        int links = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                for (CityLinkGraph.Link l : graph.links(cx, cz)) {
                    links++;
                    assertTrue(planner.centerYieldsCity(l.ax(), l.az()), l.toString());
                    assertTrue(planner.centerYieldsCity(l.bx(), l.bz()), l.toString());
                }
            }
        }
        assertTrue(links > 0, "flat land links its cities");
    }

    @Test
    public void seaYieldsNoNodes() {
        LayoutPlanner sea = planner(true, true, new LayoutFixtures.FakeTerrain((x, z) -> 40));
        CityLinkGraph graph = sea.networks()
            .links();
        assertNotNull(graph);
        assertFalse(
            graph.links(0, 0)
                .iterator()
                .hasNext(),
            "no city, no link");
    }
}
