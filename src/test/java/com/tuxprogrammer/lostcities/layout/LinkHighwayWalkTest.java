package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkHighways;
import com.tuxprogrammer.lostcities.layout.link.LinkRaster;

/**
 * The network walk of design record 0015 §4 through the real planner: in {@code networkLayout=links}, every link's
 * highway is continuous chunk to chunk as {@code planner.networks().highway(x, z)} reports it, with matching deck
 * levels at every seam, and ends only on a plaza whose next chunk inward lies inside the city.
 */
public class LinkHighwayWalkTest {

    private static LayoutPlanner planner(long seed, LayoutFixtures.FakeTerrain terrain) {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(100, 256)
            .cityGrid(64, 16)
            .build();
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, LayoutFixtures.standardAssets(), terrain)
                .networks(NetworkSettings.DEFAULTS.withLinks(true))
                .build());
    }

    @Test
    public void everyLinkHighwayIsContinuousOnFlatLand() {
        assertTrue(walk(planner(7919L, LayoutFixtures.flatTerrain()))[0] > 0, "flat land links its cities");
        assertTrue(walk(planner(31337L, LayoutFixtures.flatTerrain()))[0] > 0, "second seed");
    }

    @Test
    public void everyLinkHighwayIsContinuousOverTerraces() {
        // Terraces one city level apart, so links ramp between cities at different levels.
        int[] counts = walk(
            planner(7919L, new LayoutFixtures.FakeTerrain((x, z) -> 70 + 6 * Math.floorMod(Math.floorDiv(x, 50), 3))));
        assertTrue(counts[1] > 0, "some links ramp between levels");
    }

    @Test
    public void linksTakeTheStaircaseOverARiverAndThroughAHill() {
        // A river four chunks wide across the whole map, and a hill ridge 30 blocks above the land.
        LayoutFixtures.FakeTerrain terrain = new LayoutFixtures.FakeTerrain(
            (x, z) -> x >= 20 && x <= 23 ? 50 : z >= -40 && z <= -30 ? 100 : 70).water(20, -100000, 23, 100000);
        int[] counts = walk(planner(7919L, terrain));
        assertTrue(counts[0] > 0, "the land links its cities");
        assertTrue(counts[2] > 0, "some links take the staircase");
    }

    @Test
    public void legacyLayoutStillAnswersFromThePlanner() {
        LayoutPlanner legacy = new LayoutPlanner(
            LayoutInputs.builder(
                7919L,
                LayoutProfile.builder()
                    .cityChance(0.00025)
                    .cityRadius(100, 256)
                    .cityGrid(64, 16)
                    .build(),
                LayoutFixtures.standardAssets(),
                LayoutFixtures.flatTerrain())
                .networks(NetworkSettings.DEFAULTS.withLinks(false))
                .build());
        assertEquals(
            null,
            legacy.networks()
                .linkHighways());
    }

    /**
     * Walks every link with an end in the 7 × 7 cells around the origin; returns how many were written and how many of
     * those change level and take the staircase.
     */
    private static int[] walk(LayoutPlanner planner) {
        Networks networks = planner.networks();
        CityLinkGraph graph = networks.links();
        LinkHighways view = networks.linkHighways();
        assertNotNull(graph);
        assertNotNull(view);
        Set<CityLinkGraph.Link> seen = new HashSet<>();
        int written = 0;
        int ramped = 0;
        int staircase = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                for (CityLinkGraph.Link link : graph.links(cx, cz)) {
                    if (!seen.add(link)) {
                        continue;
                    }
                    LinkHighways.Profile p = view.profile(link);
                    if (!p.written()) {
                        continue;
                    }
                    written++;
                    ramped += p.inLevel(p.start()) != p.outLevel(p.end()) ? 1 : 0;
                    staircase += p.staircase() ? 1 : 0;
                    walkOne(planner, networks, link, p);
                }
            }
        }
        return new int[] { written, ramped, staircase };
    }

    private static void walkOne(LayoutPlanner planner, Networks networks, CityLinkGraph.Link link,
        LinkHighways.Profile p) {
        List<LinkRaster.Step> path = link.path();
        for (int i = p.start(); i <= p.end(); i++) {
            LinkRaster.Step s = path.get(i);
            HighwayInfo info = networks.highway(s.chunkX(), s.chunkZ());
            assertTrue(info.hasHighway(), link + " chunk " + i);
            List<HighwayInfo.Piece> pieces = info.pieces(s.chunkX(), s.chunkZ());
            assertTrue(!pieces.isEmpty(), link + " has a piece at " + i);
            if (i == p.start() || i == p.end()) {
                assertTrue(has(pieces, HighwayInfo.Shape.CROSSING), link + " ends on a plaza at " + i);
                // The chunk beyond a plaza is the city: its center, or inside its radius.
                LinkRaster.Step inward = i == p.start() ? (i > 0 ? path.get(i - 1) : null)
                    : (i < path.size() - 1 ? path.get(i + 1) : null);
                int ex = i == p.start() ? link.ax() : link.bx();
                int ez = i == p.start() ? link.az() : link.bz();
                if (inward != null) {
                    long dx = inward.chunkX() - ex;
                    long dz = inward.chunkZ() - ez;
                    float radius = planner.cityRadius(ex, ez);
                    assertTrue(256 * (dx * dx + dz * dz) <= radius * radius, link + " plaza meets its city");
                }
            }
            if (i < p.end()) {
                LinkRaster.Step next = path.get(i + 1);
                int dx = next.chunkX() - s.chunkX();
                int dz = next.chunkZ() - s.chunkZ();
                int level = p.outLevel(i);
                assertEquals(level, p.inLevel(i + 1), link + " profile seam " + i);
                if (dx != 0 && dz != 0) {
                    if (p.staircase()) {
                        // Two straight steps, along X first, through a bend in the side chunk between (0015 §3).
                        int bx = s.chunkX() + dx;
                        List<HighwayInfo.Piece> bend = networks.highway(bx, s.chunkZ())
                            .pieces(bx, s.chunkZ());
                        assertTrue(opens(pieces, dx, 0, level), link + " steps along X at " + i);
                        assertTrue(opens(bend, -dx, 0, level) && opens(bend, 0, dz, level), link + " bends at " + i);
                        assertTrue(
                            opens(
                                networks.highway(next.chunkX(), next.chunkZ())
                                    .pieces(next.chunkX(), next.chunkZ()),
                                0,
                                -dz,
                                level),
                            link + " steps along Z into " + (i + 1));
                        continue;
                    }
                    // No 45° deck over a bridge or through a tunnel: such a link takes the staircase.
                    LinkHighways.Ends ends = networks.linkHighways()
                        .ends();
                    assertTrue(
                        !ends.bridgeOrTunnel(s.chunkX(), s.chunkZ(), level, level)
                            && !ends.bridgeOrTunnel(next.chunkX(), next.chunkZ(), level, level)
                            && !ends.bridgeOrTunnel(s.chunkX() + dx, s.chunkZ(), level, level)
                            && !ends.bridgeOrTunnel(s.chunkX(), s.chunkZ() + dz, level, level),
                        link + " runs 45° over a bridge or tunnel at " + i);
                }
                assertTrue(opens(pieces, dx, dz, level), link + " leaves chunk " + i + " at level " + level);
                assertTrue(
                    opens(
                        networks.highway(next.chunkX(), next.chunkZ())
                            .pieces(next.chunkX(), next.chunkZ()),
                        -dx,
                        -dz,
                        level),
                    link + " enters chunk " + (i + 1) + " at level " + level);
            }
        }
    }

    private static boolean has(List<HighwayInfo.Piece> pieces, HighwayInfo.Shape shape) {
        for (HighwayInfo.Piece piece : pieces) {
            if (piece.shape() == shape) {
                return true;
            }
        }
        return false;
    }

    private static boolean opens(List<HighwayInfo.Piece> pieces, int dx, int dz, int level) {
        for (HighwayInfo.Piece piece : pieces) {
            if (piece.levelAt(dx, dz) == level) {
                return true;
            }
        }
        return false;
    }
}
