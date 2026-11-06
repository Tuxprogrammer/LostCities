package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkHighways;
import com.tuxprogrammer.lostcities.layout.link.LinkRailGeometry;
import com.tuxprogrammer.lostcities.layout.link.LinkRails;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;

/**
 * The network scenarios of design record 0015 §4 through the real planner in {@code networkLayout=links}: every
 * railway chunk in a large window continues on each side it opens on and stops only at a city's station, every line
 * runs from one city center to another; every highway chunk lies on a written link whose both ends are cities; and no
 * ramp shares its chunk with another piece (the ramp/flat overlap gap, D323).
 */
public class LinkNetworkWalkTest {

    private static final int WINDOW = 256;

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

    private static LayoutFixtures.FakeTerrain terraces() {
        return new LayoutFixtures.FakeTerrain((x, z) -> 70 + 6 * Math.floorMod(Math.floorDiv(x, 50), 3));
    }

    @Test
    public void everyRailChunkContinuesAndStopsOnlyAtAStation() {
        for (long seed : new long[] { 7919L, 31337L }) {
            Networks networks = planner(seed, LayoutFixtures.flatTerrain()).networks();
            int rails = 0;
            int stations = 0;
            for (int x = -WINDOW; x <= WINDOW; x++) {
                for (int z = -WINDOW; z <= WINDOW; z++) {
                    RailChunk rail = networks.railChunk(x, z);
                    if (!rail.isPresent()) {
                        continue;
                    }
                    rails++;
                    int sides = rail.openings();
                    assertTrue(sides != 0, "a links-mode rail chunk opens somewhere at " + x + "," + z);
                    if (rail.type()
                        .isStation()) {
                        stations++;
                        assertEquals(0, sides & (LinkRails.NORTH | LinkRails.SOUTH), "a station runs along X");
                    } else {
                        assertTrue(Integer.bitCount(sides) >= 2, "a line chunk never dead-ends at " + x + "," + z);
                        assertTrue(rail.coded(), "a line chunk is written in code");
                    }
                    for (int[] d : new int[][] { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } }) {
                        if ((sides & LinkRails.side(d[0], d[1])) == 0) {
                            continue;
                        }
                        RailChunk next = networks.railChunk(x + d[0], z + d[1]);
                        assertTrue(
                            (next.openings() & LinkRails.side(-d[0], -d[1])) != 0,
                            "the track continues from " + x + "," + z + " to " + (x + d[0]) + "," + (z + d[1]));
                        assertEquals(rail.level(), next.level(), "one level along a line");
                    }
                }
            }
            assertTrue(rails > 0 && stations > 0, "the window has lines and stations, seed " + seed);
        }
    }

    @Test
    public void everyLineRunsFromCityCenterToCityCenter() {
        Networks networks = planner(7919L, LayoutFixtures.flatTerrain()).networks();
        CityLinkGraph graph = networks.links();
        assertNotNull(
            networks.railways()
                .links());
        Set<CityLinkGraph.Link> seen = new HashSet<>();
        int lines = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                for (CityLinkGraph.Link link : graph.links(cx, cz)) {
                    if (!seen.add(link)) {
                        continue;
                    }
                    lines++;
                    List<int[]> path = LinkRails.path(link);
                    assertEquals(link.ax(), path.get(0)[0]);
                    assertEquals(link.az(), path.get(0)[1]);
                    assertEquals(link.bx(), path.get(path.size() - 1)[0]);
                    assertEquals(link.bz(), path.get(path.size() - 1)[1]);
                    Set<Long> visited = new HashSet<>();
                    for (int i = 0; i < path.size(); i++) {
                        int[] c = path.get(i);
                        assertTrue(visited.add((long) c[0] << 32 | (c[1] & 0xffffffffL)), "no chunk twice");
                        if (i > 0) {
                            int[] p = path.get(i - 1);
                            assertTrue(LinkRails.side(c[0] - p[0], c[1] - p[1]) != 0, "side neighbors");
                        }
                    }
                    // Both centers are entered along X, where the station stands.
                    assertEquals(path.get(0)[1], path.get(1)[1]);
                    assertEquals(path.get(path.size() - 1)[1], path.get(path.size() - 2)[1]);
                    RailChunk a = networks.railChunk(link.ax(), link.az());
                    RailChunk b = networks.railChunk(link.bx(), link.bz());
                    assertTrue(a.isPresent() && b.isPresent(), "both ends carry the line");
                }
            }
        }
        assertTrue(lines > 0);
    }

    @Test
    public void everyHighwayChunkLiesOnAWrittenCityToCityLink() {
        Networks networks = planner(7919L, terraces()).networks();
        LinkHighways view = networks.linkHighways();
        int chunks = 0;
        for (int x = -WINDOW; x <= WINDOW; x++) {
            for (int z = -WINDOW; z <= WINDOW; z++) {
                if (view.pieces(x, z)
                    .isEmpty()) {
                    continue;
                }
                chunks++;
                // A piece lies on a written city-to-city link's path, or beside it (a staircase bend).
                boolean onLink = false;
                for (int dx = -1; dx <= 1 && !onLink; dx++) {
                    for (int dz = -1; dz <= 1 && !onLink; dz++) {
                        for (CityLinkGraph.Hit hit : networks.links()
                            .hits(x + dx, z + dz)) {
                            onLink |= hit.link()
                                .bothEndsCities()
                                && view.profile(hit.link())
                                    .written();
                        }
                    }
                }
                assertTrue(onLink, "highway chunk " + x + "," + z + " lies on a written link");
            }
        }
        assertTrue(chunks > 0);
    }

    @Test
    public void noRampSharesItsChunkWithAPieceInsideItsClimb() {
        Networks networks = planner(7919L, terraces()).networks();
        LinkHighways view = networks.linkHighways();
        int ramps = 0;
        for (int x = -WINDOW; x <= WINDOW; x++) {
            for (int z = -WINDOW; z <= WINDOW; z++) {
                List<HighwayInfo.Piece> pieces = view.pieces(x, z);
                for (HighwayInfo.Piece ramp : pieces) {
                    if (ramp.level() == ramp.topLevel()) {
                        continue;
                    }
                    ramps++;
                    for (HighwayInfo.Piece other : pieces) {
                        assertFalse(
                            other != ramp && other.topLevel() >= ramp.level() && other.level() <= ramp.topLevel(),
                            "ramp " + ramp + " overlaps " + other + " at " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(ramps > 0, "the terraces have ramps");
    }

    @Test
    public void codedTrackMeetsAtEverySideAndJoinsAtTheCenter() {
        for (int sides = 1; sides <= LinkRails.SIDES; sides++) {
            assertEquals((sides & LinkRails.WEST) != 0, LinkRailGeometry.shape(sides, 0, LinkRailGeometry.TRACK) >= 0);
            assertEquals((sides & LinkRails.EAST) != 0, LinkRailGeometry.shape(sides, 15, LinkRailGeometry.TRACK) >= 0);
            assertEquals((sides & LinkRails.NORTH) != 0, LinkRailGeometry.shape(sides, LinkRailGeometry.TRACK, 0) >= 0);
            assertEquals(
                (sides & LinkRails.SOUTH) != 0,
                LinkRailGeometry.shape(sides, LinkRailGeometry.TRACK, 15) >= 0);
            // The tunnel's edge columns match the neighbor's across each side: inside at 4..10, wall at 3 and 11.
            for (int a = 0; a < 16; a++) {
                int role = LinkRailGeometry.role(sides, 0, a);
                int expected = (sides & LinkRails.WEST) == 0 ? -1
                    : a >= 4 && a <= 10 ? LinkRailGeometry.INSIDE : a == 3 || a == 11 ? LinkRailGeometry.WALL : -1;
                if (expected >= 0) {
                    assertEquals(expected, role, "west edge row " + a + " sides " + sides);
                }
            }
            int center = LinkRailGeometry.shape(sides, LinkRailGeometry.TRACK, LinkRailGeometry.TRACK);
            assertTrue(center >= 0);
        }
        int bend = LinkRails.NORTH | LinkRails.EAST;
        assertEquals(LinkRailGeometry.NORTH_EAST, LinkRailGeometry.shape(bend, 7, 7));
        assertEquals(LinkRailGeometry.WALL, LinkRailGeometry.role(bend, 3, 7));
        assertEquals(LinkRailGeometry.INSIDE, LinkRailGeometry.role(bend, 7, 0));
        assertEquals(LinkRailGeometry.NONE, LinkRailGeometry.role(bend, 0, 0));
        assertEquals(
            RailChunkType.DOUBLE_BEND,
            RailChunk.link(RailChunkType.DOUBLE_BEND, -3, false, bend)
                .type());
    }

    /**
     * D357: a line never passes through a third city's center; it runs to it and on, so every center a line
     * touches is entered and left along X and takes its station, and the route is still one unbroken line.
     */
    @Test
    public void noLinePassesThroughAnotherCityCenterAndEveryCenterIsAStation() {
        int splits = 0;
        int centers = 0;
        int tees = 0;
        int crossings = 0;
        for (long seed : new long[] { 7919L, 31337L, 1L, 777L }) {
            Networks networks = planner(seed, LayoutFixtures.flatTerrain()).networks();
            CityLinkGraph graph = networks.links();
            LinkRails rails = networks.railways()
                .links();
            Set<Long> nodes = new HashSet<>();
            for (int cx = -6; cx <= 6; cx++) {
                for (int cz = -6; cz <= 6; cz++) {
                    int[] n = graph.node(cx, cz);
                    if (n != null) {
                        nodes.add(key(n[0], n[1]));
                    }
                }
            }
            Set<CityLinkGraph.Link> seen = new HashSet<>();
            for (int cx = -3; cx <= 3; cx++) {
                for (int cz = -3; cz <= 3; cz++) {
                    for (CityLinkGraph.Link link : graph.links(cx, cz)) {
                        if (!seen.add(link) || !rails.live(link)) {
                            continue;
                        }
                        List<int[]> route = rails.route(link);
                        assertEquals(link.ax(), route.get(0)[0]);
                        assertEquals(link.az(), route.get(0)[1]);
                        assertEquals(link.bx(), route.get(route.size() - 1)[0]);
                        assertEquals(link.bz(), route.get(route.size() - 1)[1]);
                        for (int i = 1; i < route.size(); i++) {
                            int[] c = route.get(i);
                            int[] p = route.get(i - 1);
                            assertTrue(LinkRails.side(c[0] - p[0], c[1] - p[1]) != 0, "side neighbors " + link);
                            if (i < route.size() - 1 && nodes.contains(key(c[0], c[1]))) {
                                // A center inside the route: reached and left along X.
                                splits++;
                                assertEquals(p[1], c[1], "center entered along X " + link);
                                assertEquals(route.get(i + 1)[1], c[1], "center left along X " + link);
                            }
                        }
                    }
                }
            }
            for (int x = -WINDOW; x <= WINDOW; x++) {
                for (int z = -WINDOW; z <= WINDOW; z++) {
                    RailChunk rail = networks.railChunk(x, z);
                    int sides = rail.openings() & LinkRails.SIDES;
                    tees += Integer.bitCount(sides) == 3 ? 1 : 0;
                    crossings += Integer.bitCount(sides) == 4 ? 1 : 0;
                    if (rail.isPresent() && nodes.contains(key(x, z))) {
                        centers++;
                        assertTrue(
                            rail.type()
                                .isStation(),
                            "a city center with track is a station at " + x + "," + z + ": " + rail);
                    }
                }
            }
        }
        System.out.println(
            "[link rails] centers " + centers
                + ", route splits "
                + splits
                + ", T "
                + tees
                + ", crossings "
                + crossings);
        assertTrue(centers > 0);
    }

    /** D356: a T's center is vanilla's unpowered curve with a lever beside it, off the track, inside the tunnel. */
    @Test
    public void aTeeCenterIsAVanillaSwitchWithALever() {
        int n = LinkRails.NORTH;
        int e = LinkRails.EAST;
        int s = LinkRails.SOUTH;
        int w = LinkRails.WEST;
        int t = LinkRailGeometry.TRACK;
        assertEquals(LinkRailGeometry.SOUTH_EAST, LinkRailGeometry.shape(e | s | w, t, t));
        assertEquals(LinkRailGeometry.SOUTH_EAST, LinkRailGeometry.shape(n | e | s, t, t));
        assertEquals(LinkRailGeometry.SOUTH_WEST, LinkRailGeometry.shape(n | s | w, t, t));
        assertEquals(LinkRailGeometry.NORTH_EAST, LinkRailGeometry.shape(n | e | w, t, t));
        assertEquals(LinkRailGeometry.EAST_WEST, LinkRailGeometry.shape(n | e | s | w, t, t));
        assertNull(LinkRailGeometry.lever(e | w));
        assertNull(LinkRailGeometry.lever(n | e | s | w));
        for (int sides : new int[] { e | s | w, n | e | s, n | s | w, n | e | w }) {
            int[] lever = LinkRailGeometry.lever(sides);
            assertNotNull(lever);
            int dx = lever[0] - t;
            int dz = lever[1] - t;
            assertEquals(1, Math.abs(dx) + Math.abs(dz), "beside the center");
            assertEquals(-1, LinkRailGeometry.shape(sides, lever[0], lever[1]), "off the track");
            assertEquals(LinkRailGeometry.INSIDE, LinkRailGeometry.role(sides, lever[0], lever[1]));
            assertEquals(0, sides & LinkRails.side(dx, dz), "on the side no arm leaves by");
        }
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }
}
