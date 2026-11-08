package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.CityGrid;

/**
 * The geometric link choice and the lazy link index (D-perf2-links-1..3): links come from node positions alone
 * (relative neighbors plus the {@link CityLinkGraph#NEAREST} nearest), are symmetric, keep every city at two links,
 * answer the same in any query order, and drawing one chunk routes only a bounded number of links.
 */
class CityLinkGraphGeometryTest {

    /** The two grid profiles: {cell, jitter, minRadius, maxRadius} for the default and {@code rarecities} worlds. */
    private static final int[][] PROFILES = { { 64, 16, 100, 256 }, { 256, 64, 200, 512 } };

    /** Grid centers, with about one in five dropped as if the terrain filters refused it. */
    private static class Nodes implements CityLinkGraph.Nodes {

        private final CityGrid grid;
        private final long seed;

        Nodes(int[] profile, long seed) {
            this.grid = new CityGrid(seed, profile[0], profile[1], profile[2], profile[3]);
            this.seed = seed;
        }

        @Override
        public int cell() {
            return grid.cell();
        }

        @Override
        public int[] cityCenter(int cellX, int cellZ) {
            long h = (cellX * 341873128712L + cellZ * 132897987541L + seed) * 0x9E3779B97F4A7C15L;
            if ((h >>> 59) < 6) {
                return null;
            }
            CityGrid.Center c = grid.center(cellX, cellZ);
            return new int[] { c.chunkX(), c.chunkZ() };
        }
    }

    /** The same nodes with landmark discs on a lattice scaled to the cell, offset so some links cross them. */
    private static final class Landmarks extends Nodes {

        private final int pitch;
        private final int radius;

        Landmarks(int[] profile, long seed) {
            super(profile, seed);
            this.pitch = profile[0] * 5 / 8;
            this.radius = Math.max(6, profile[0] / 10);
        }

        @Override
        public boolean blocked(int chunkX, int chunkZ) {
            int dx = Math.floorMod(chunkX + pitch / 2, pitch) - pitch / 2;
            int dz = Math.floorMod(chunkZ + 7, pitch) - pitch / 2;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    /** Brute force: the nodes within the cap of a cell's node, nearest first, ties to the lower center. */
    private static List<int[]> inRange(CityLinkGraph graph, int cx, int cz) {
        int[] self = graph.node(cx, cz);
        List<int[]> all = new ArrayList<>();
        if (self == null) {
            return all;
        }
        int wide = graph.reach() + 2;
        long max = (long) graph.maxDistance() * graph.maxDistance();
        for (int ox = -wide; ox <= wide; ox++) {
            for (int oz = -wide; oz <= wide; oz++) {
                int[] o = graph.node(cx + ox, cz + oz);
                if (o == null || ox == 0 && oz == 0) {
                    continue;
                }
                if (d2(self, o) <= max) {
                    all.add(new int[] { o[0], o[1], cx + ox, cz + oz });
                }
            }
        }
        all.sort((p, q) -> {
            int c = Long.compare(d2(self, p), d2(self, q));
            return c != 0 ? c : p[0] != q[0] ? Integer.compare(p[0], q[0]) : Integer.compare(p[1], q[1]);
        });
        return all;
    }

    private static long d2(int[] a, int[] b) {
        long dx = a[0] - b[0];
        long dz = a[1] - b[1];
        return dx * dx + dz * dz;
    }

    /** Brute force: the far ends a cell's node links to by the geometric rule, nearest first. */
    private static List<int[]> partners(CityLinkGraph graph, int cx, int cz) {
        int[] self = graph.node(cx, cz);
        List<int[]> near = inRange(graph, cx, cz);
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < near.size(); i++) {
            int[] b = near.get(i);
            boolean rng = true;
            long ab = d2(self, b);
            for (int[] c : near) {
                if (d2(self, c) < ab && d2(b, c) < ab) {
                    rng = false;
                }
            }
            List<int[]> theirs = inRange(graph, b[2], b[3]);
            boolean mine = false;
            for (int k = 0; k < Math.min(CityLinkGraph.NEAREST, theirs.size()); k++) {
                mine |= theirs.get(k)[0] == self[0] && theirs.get(k)[1] == self[1];
            }
            if (i < CityLinkGraph.NEAREST || rng || mine) {
                out.add(b);
            }
        }
        return out;
    }

    private static int[] far(CityLinkGraph.Link link, int[] self) {
        return link.ax() == self[0] && link.az() == self[1] ? new int[] { link.bx(), link.bz() }
            : new int[] { link.ax(), link.az() };
    }

    private static Set<String> ends(List<int[]> nodes) {
        Set<String> out = new HashSet<>();
        for (int[] n : nodes) {
            out.add(n[0] + "," + n[1]);
        }
        return out;
    }

    @Test
    void partnersFollowTheGeometricRuleAndAreSymmetric() {
        for (int[] profile : PROFILES) {
            for (long seed : new long[] { 3L, -77L }) {
                CityLinkGraph graph = new CityLinkGraph(new Nodes(profile, seed), 1024);
                for (int cx = -6; cx <= 6; cx++) {
                    for (int cz = -6; cz <= 6; cz++) {
                        int[] self = graph.node(cx, cz);
                        List<CityLinkGraph.Link> links = graph.chosen(cx, cz);
                        if (self == null) {
                            assertTrue(links.isEmpty());
                            continue;
                        }
                        List<int[]> want = partners(graph, cx, cz);
                        assertEquals(want.size(), links.size(), "cell " + cx + "," + cz);
                        for (int i = 0; i < want.size(); i++) {
                            int[] f = far(links.get(i), self);
                            assertEquals(want.get(i)[0], f[0], "partner " + i + " of " + cx + "," + cz);
                            assertEquals(want.get(i)[1], f[1], "partner " + i + " of " + cx + "," + cz);
                            // Seen identically from the far end.
                            int fcx = want.get(i)[2];
                            int fcz = want.get(i)[3];
                            assertTrue(
                                ends(partners(graph, fcx, fcz)).contains(self[0] + "," + self[1]),
                                "symmetric by brute force");
                            assertTrue(
                                graph.links(fcx, fcz)
                                    .contains(links.get(i)),
                                links.get(i) + " not seen from its far end");
                        }
                    }
                }
            }
        }
    }

    /** The owner rule: every city keeps two links, over a few hundred cells of both profiles, landmarks or not. */
    @Test
    void everyCityKeepsTwoLinks() {
        for (int[] profile : PROFILES) {
            for (boolean landmarks : new boolean[] { false, true }) {
                long seed = 4242L + profile[0];
                CityLinkGraph.Nodes oracle = landmarks ? new Landmarks(profile, seed) : new Nodes(profile, seed);
                CityLinkGraph graph = new CityLinkGraph(oracle, 1024);
                int nodes = 0;
                int short2 = 0;
                for (int cx = -10; cx < 10; cx++) {
                    for (int cz = -10; cz < 10; cz++) {
                        if (graph.node(cx, cz) == null) {
                            continue;
                        }
                        nodes++;
                        int inRange = inRange(graph, cx, cz).size();
                        short2 += inRange < 2 ? 1 : 0;
                        assertTrue(
                            graph.links(cx, cz)
                                .size() >= Math.min(CityLinkGraph.NEAREST, inRange),
                            "profile " + profile[0] + " landmarks " + landmarks + " cell " + cx + "," + cz);
                    }
                }
                assertTrue(nodes > 250, "nodes " + nodes);
                assertTrue(short2 * 20 < nodes, "most sampled cities have two nodes in range: " + short2);
            }
        }
    }

    /**
     * D-perf2-links-2: a link whose route clears is kept on that route; one no path clears is dropped only while both
     * ends keep two routable links, else kept straight.
     */
    @Test
    void anUnroutableLinkIsDroppedOnlyWhenBothEndsKeepTwo() {
        for (int[] profile : PROFILES) {
            Landmarks oracle = new Landmarks(profile, 99L);
            CityLinkGraph graph = new CityLinkGraph(oracle, 1024);
            int detoured = 0;
            for (int cx = -6; cx <= 6; cx++) {
                for (int cz = -6; cz <= 6; cz++) {
                    int[] self = graph.node(cx, cz);
                    if (self == null) {
                        continue;
                    }
                    Set<String> kept = new HashSet<>();
                    for (CityLinkGraph.Link link : graph.chosen(cx, cz)) {
                        int[] f = far(link, self);
                        kept.add(f[0] + "," + f[1]);
                        CityLinkGraph.Link routed = graph.route(self[0], self[1], f[0], f[1]);
                        if (routed != null) {
                            assertEquals(steps(routed), steps(link), "a routed link keeps its route");
                            for (LinkRaster.Step s : link.path()) {
                                assertTrue(!oracle.blocked(s.chunkX(), s.chunkZ()), link + " crosses a landmark");
                            }
                            detoured += link.path()
                                .equals(LinkRaster.rasterize(link.ax(), link.az(), link.bx(), link.bz())) ? 0 : 1;
                        } else {
                            assertEquals(
                                steps(new CityLinkGraph.Link(link.ax(), link.az(), link.bx(), link.bz())),
                                steps(link),
                                "an unroutable kept link is straight");
                        }
                    }
                    for (int[] p : partners(graph, cx, cz)) {
                        if (kept.contains(p[0] + "," + p[1])) {
                            continue;
                        }
                        assertNull(graph.route(self[0], self[1], p[0], p[1]), "only an unroutable link is dropped");
                        assertTrue(routable(graph, cx, cz, p) >= 2, "this end keeps two without it");
                        assertTrue(
                            routable(graph, p[2], p[3], new int[] { self[0], self[1] }) >= 2,
                            "the far end keeps two without it");
                    }
                }
            }
            assertTrue(detoured > 0, "the fixture forces detours, so the test has teeth");
        }
    }

    private static int routable(CityLinkGraph graph, int cx, int cz, int[] without) {
        int[] self = graph.node(cx, cz);
        int n = 0;
        for (int[] p : partners(graph, cx, cz)) {
            if (!(p[0] == without[0] && p[1] == without[1]) && graph.route(self[0], self[1], p[0], p[1]) != null) {
                n++;
            }
        }
        return n;
    }

    private static String steps(CityLinkGraph.Link link) {
        StringBuilder out = new StringBuilder();
        for (LinkRaster.Step s : link.path()) {
            out.append(s.chunkX())
                .append(',')
                .append(s.chunkZ())
                .append(' ');
        }
        return out.toString();
    }

    /** The lazy index relies on every routed path staying in its ends' box widened by the route margin. */
    @Test
    void everyRouteStaysWithinTheRouteMargin() {
        for (int[] profile : PROFILES) {
            Landmarks oracle = new Landmarks(profile, 7L);
            CityLinkGraph graph = new CityLinkGraph(oracle, 1024);
            for (int cx = -5; cx <= 5; cx++) {
                for (int cz = -5; cz <= 5; cz++) {
                    for (CityLinkGraph.Link link : graph.links(cx, cz)) {
                        int m = CityLinkGraph.ROUTE_MARGIN;
                        for (LinkRaster.Step s : link.path()) {
                            assertTrue(
                                s.chunkX() >= Math.min(link.ax(), link.bx()) - m
                                    && s.chunkX() <= Math.max(link.ax(), link.bx()) + m
                                    && s.chunkZ() >= Math.min(link.az(), link.bz()) - m
                                    && s.chunkZ() <= Math.max(link.az(), link.bz()) + m,
                                link + " leaves its margin at " + s.chunkX() + "," + s.chunkZ());
                        }
                    }
                }
            }
        }
    }

    /**
     * The same links and hits whatever the query order, from cells or from chunks, with caches small enough to evict
     * (CONTRIBUTING § World generation rules).
     */
    @Test
    void sameAnswerFromAnyOrderAndAnyChunk() {
        for (int[] profile : PROFILES) {
            Landmarks oracle = new Landmarks(profile, 5150L);
            CityLinkGraph byCells = new CityLinkGraph(oracle, 16);
            CityLinkGraph byChunks = new CityLinkGraph(oracle, 16);
            int cell = profile[0];
            // Walk the second graph's chunks first, backwards, so its caches fill in a different order.
            List<String> chunkAnswers = new ArrayList<>();
            for (int i = 299; i >= 0; i--) {
                int x = i * cell / 37 - 4 * cell;
                int z = (i * 7919 % 300) * cell / 37 - 4 * cell;
                chunkAnswers.add(0, x + "," + z + ":" + describe(byChunks.hits(x, z)));
            }
            List<String> forwardLinks = new ArrayList<>();
            List<String> backwardLinks = new ArrayList<>();
            for (int cx = -3; cx <= 3; cx++) {
                for (int cz = -3; cz <= 3; cz++) {
                    forwardLinks.add(describeLinks(byCells.links(cx, cz)));
                }
            }
            for (int cx = 3; cx >= -3; cx--) {
                for (int cz = 3; cz >= -3; cz--) {
                    backwardLinks.add(0, describeLinks(byChunks.links(cx, cz)));
                }
            }
            assertEquals(forwardLinks, backwardLinks);
            List<String> cellAnswers = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                int x = i * cell / 37 - 4 * cell;
                int z = (i * 7919 % 300) * cell / 37 - 4 * cell;
                cellAnswers.add(x + "," + z + ":" + describe(byCells.hits(x, z)));
            }
            assertEquals(chunkAnswers, cellAnswers);
            // Every step of every kept link is reported at its chunk, by a third graph that saw nothing else first.
            CityLinkGraph fresh = new CityLinkGraph(oracle, 64);
            for (CityLinkGraph.Link link : byCells.links(0, 0)) {
                List<LinkRaster.Step> path = link.path();
                for (int i = 0; i < path.size(); i += 5) {
                    boolean found = false;
                    for (CityLinkGraph.Hit hit : fresh.hits(
                        path.get(i)
                            .chunkX(),
                        path.get(i)
                            .chunkZ())) {
                        found |= hit.link()
                            .equals(link) && hit.index() == i;
                    }
                    assertTrue(found, link + " step " + i);
                }
            }
        }
    }

    private static String describe(List<CityLinkGraph.Hit> hits) {
        StringBuilder out = new StringBuilder();
        for (CityLinkGraph.Hit hit : hits) {
            out.append(hit.link())
                .append('@')
                .append(hit.index())
                .append(';');
        }
        return out.toString();
    }

    private static String describeLinks(List<CityLinkGraph.Link> links) {
        StringBuilder out = new StringBuilder();
        for (CityLinkGraph.Link link : links) {
            out.append(link)
                .append('[')
                .append(steps(link))
                .append("];");
        }
        return out.toString();
    }

    /**
     * Drawing one chunk on a long link (its highway info, which plans its links' levels and crossings through
     * {@link LinkHighways}) routes a bounded number of links and builds a bounded number of cell candidate lists: it no
     * longer routes every candidate of every node near every cell the link crosses (handoff perf-next, chunk 251,3).
     */
    @Test
    void drawingOneChunkRoutesABoundedNumberOfLinks() {
        int[] profile = PROFILES[1];
        Landmarks oracle = new Landmarks(profile, 4242L);
        CityLinkGraph scout = new CityLinkGraph(oracle, 4096);
        // The longest kept link at the middle cell; draw the chunk halfway along it.
        CityLinkGraph.Link longest = null;
        for (CityLinkGraph.Link link : scout.links(0, 0)) {
            if (longest == null || link.path()
                .size() > longest.path()
                    .size()) {
                longest = link;
            }
        }
        assertTrue(longest != null && longest.path()
            .size() > profile[0], "a long link to draw");
        LinkRaster.Step middle = longest.path()
            .get(
                longest.path()
                    .size() / 2);
        CityLinkGraph graph = new CityLinkGraph(oracle, 4096);
        LinkHighways view = new LinkHighways(graph, new LinkHighways.Ends() {

            @Override
            public int level(int chunkX, int chunkZ) {
                return Math.floorMod(chunkX * 31 + chunkZ * 17, 4);
            }

            @Override
            public int radius(int chunkX, int chunkZ) {
                return 20;
            }
        });
        view.info(middle.chunkX(), middle.chunkZ());
        int routes = graph.cachedRoutes();
        int builds = graph.candidateBuilds();
        // What the old index routed for the same cell alone: every in-range candidate of every node within reach.
        int everyCandidate = 0;
        int reach = graph.reach();
        int cx = Math.floorDiv(middle.chunkX(), profile[0]);
        int cz = Math.floorDiv(middle.chunkZ(), profile[0]);
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                everyCandidate += inRange(scout, cx + ox, cz + oz).size();
            }
        }
        System.out.println(
            "drawOneChunk routes=" + routes + " candidateBuilds=" + builds + " oldCellCandidates=" + everyCandidate);
        assertTrue(routes > 0 && routes <= 64, "routes " + routes);
        assertTrue(builds > 0 && builds <= 40, "candidate builds " + builds);
        assertTrue(routes * 4 < everyCandidate, "routes " + routes + " vs " + everyCandidate);
    }
}
