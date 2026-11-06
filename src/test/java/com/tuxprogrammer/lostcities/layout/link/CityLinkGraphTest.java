package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.CityGrid;

/** The link graph (0015 §1) and the network walk over it (0015 §4): every network chunk lies on a city-to-city link. */
class CityLinkGraphTest {

    /** The {@code rarecities} grid: 64-chunk cells, ±16 jitter. */
    private static Nodes grid(long seed) {
        CityGrid grid = new CityGrid(seed, 64, 16, 100, 256);
        return new Nodes(grid, seed);
    }

    /** Grid centers, with about one in five dropped as if the terrain filters refused it. */
    private static final class Nodes implements CityLinkGraph.Nodes {

        private final CityGrid grid;
        private final long seed;
        int calls;

        Nodes(CityGrid grid, long seed) {
            this.grid = grid;
            this.seed = seed;
        }

        @Override
        public int cell() {
            return grid.cell();
        }

        @Override
        public int[] cityCenter(int cellX, int cellZ) {
            calls++;
            long h = (cellX * 341873128712L + cellZ * 132897987541L + seed) * 0x9E3779B97F4A7C15L;
            if ((h >>> 59) < 6) {
                return null;
            }
            CityGrid.Center c = grid.center(cellX, cellZ);
            return new int[] { c.chunkX(), c.chunkZ() };
        }
    }

    @Test
    void choosesThreeNearestWithinCap() {
        Nodes nodes = grid(1234L);
        CityLinkGraph graph = new CityLinkGraph(nodes, 512);
        for (int cx = -6; cx <= 6; cx++) {
            for (int cz = -6; cz <= 6; cz++) {
                int[] self = graph.node(cx, cz);
                List<CityLinkGraph.Link> chosen = graph.chosen(cx, cz);
                if (self == null) {
                    assertTrue(chosen.isEmpty());
                    continue;
                }
                // Brute force over a wider square than the graph's reach.
                List<long[]> all = new ArrayList<>();
                for (int ox = -8; ox <= 8; ox++) {
                    for (int oz = -8; oz <= 8; oz++) {
                        int[] o = graph.node(cx + ox, cz + oz);
                        if (o == null || ox == 0 && oz == 0) {
                            continue;
                        }
                        long d = (long) (o[0] - self[0]) * (o[0] - self[0]) + (long) (o[1] - self[1]) * (o[1] - self[1]);
                        if (d <= 160L * 160L) {
                            all.add(new long[] { d, o[0], o[1] });
                        }
                    }
                }
                all.sort((p, q) -> p[0] != q[0] ? Long.compare(p[0], q[0])
                    : p[1] != q[1] ? Long.compare(p[1], q[1]) : Long.compare(p[2], q[2]));
                assertEquals(Math.min(3, all.size()), chosen.size());
                for (int i = 0; i < chosen.size(); i++) {
                    CityLinkGraph.Link l = chosen.get(i);
                    int fx = l.ax() == self[0] && l.az() == self[1] ? l.bx() : l.ax();
                    int fz = l.ax() == self[0] && l.az() == self[1] ? l.bz() : l.az();
                    assertEquals(all.get(i)[1], fx);
                    assertEquals(all.get(i)[2], fz);
                    long dx = fx - self[0];
                    long dz = fz - self[1];
                    assertTrue(dx * dx + dz * dz <= 160L * 160L);
                }
            }
        }
    }

    @Test
    void linksAreSymmetric() {
        CityLinkGraph graph = new CityLinkGraph(grid(99L), 512);
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                for (CityLinkGraph.Link link : graph.links(cx, cz)) {
                    int ocx = Math.floorDiv(link.ax(), 64);
                    int ocz = Math.floorDiv(link.az(), 64);
                    int[] self = graph.node(cx, cz);
                    if (link.ax() == self[0] && link.az() == self[1]) {
                        ocx = Math.floorDiv(link.bx(), 64);
                        ocz = Math.floorDiv(link.bz(), 64);
                    }
                    assertTrue(graph.links(ocx, ocz).contains(link), link + " not seen from its far end");
                }
            }
        }
    }

    @Test
    void sameAnswerInAnyQueryOrder() {
        CityLinkGraph forward = new CityLinkGraph(grid(7L), 16);
        CityLinkGraph backward = new CityLinkGraph(grid(7L), 16);
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            a.add(i + ":" + forward.hits(i * 3 - 600, i * 2 - 400).size());
        }
        for (int i = 399; i >= 0; i--) {
            b.add(0, i + ":" + backward.hits(i * 3 - 600, i * 2 - 400).size());
        }
        assertEquals(a, b);
    }

    @Test
    void everyNetworkChunkLiesOnACityToCityLink() {
        for (long seed : new long[] { 1L, -4242424242L }) {
            Nodes nodes = grid(seed);
            CityLinkGraph graph = new CityLinkGraph(nodes, 4096);
            Set<CityLinkGraph.Link> links = new HashSet<>();
            int networkChunks = 0;
            for (int x = -512; x < 512; x++) {
                for (int z = -512; z < 512; z++) {
                    List<CityLinkGraph.Hit> hits = graph.hits(x, z);
                    if (!hits.isEmpty()) {
                        networkChunks++;
                    }
                    for (CityLinkGraph.Hit hit : hits) {
                        assertEquals(x, hit.step().chunkX());
                        assertEquals(z, hit.step().chunkZ());
                        links.add(hit.link());
                    }
                }
            }
            assertTrue(networkChunks > 1000, "too few network chunks: " + networkChunks);
            for (CityLinkGraph.Link link : links) {
                // Both ends are cities.
                assertNotNull(graph.node(Math.floorDiv(link.ax(), 64), Math.floorDiv(link.az(), 64)));
                assertNotNull(graph.node(Math.floorDiv(link.bx(), 64), Math.floorDiv(link.bz(), 64)));
                assertTrue(link.bothEndsCities());
                // Walking the link from any chunk reaches both ends through chunks that report the link.
                List<LinkRaster.Step> path = link.path();
                assertEquals(link.ax(), path.get(0).chunkX());
                assertEquals(link.az(), path.get(0).chunkZ());
                assertEquals(link.bx(), path.get(path.size() - 1).chunkX());
                assertEquals(link.bz(), path.get(path.size() - 1).chunkZ());
                for (int i = 0; i < path.size(); i++) {
                    LinkRaster.Step s = path.get(i);
                    boolean found = false;
                    for (CityLinkGraph.Hit hit : graph.hits(s.chunkX(), s.chunkZ())) {
                        found |= hit.link()
                            .equals(link) && hit.index() == i;
                    }
                    assertTrue(found, link + " missing at step " + i);
                    if (i > 0) {
                        LinkRaster.Step p = path.get(i - 1);
                        assertEquals(
                            1,
                            Math.max(Math.abs(s.chunkX() - p.chunkX()), Math.abs(s.chunkZ() - p.chunkZ())),
                            "dead end on " + link);
                    }
                }
            }
            // The graph is connected enough to matter: most nodes have a link.
            int nodesSeen = 0;
            int linked = 0;
            for (int cx = -8; cx < 8; cx++) {
                for (int cz = -8; cz < 8; cz++) {
                    if (graph.node(cx, cz) != null) {
                        nodesSeen++;
                        linked += graph.links(cx, cz)
                            .isEmpty() ? 0 : 1;
                    }
                }
            }
            assertEquals(nodesSeen, linked, "an isolated city");
            assertFalse(links.isEmpty());
        }
    }
}
