package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.CityGrid;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;

/**
 * Links detour around landmarks (D390), so no city on the grid is a leaf; and a link's end runs on to the city as a
 * spur (D391).
 */
class CityLinkDetourTest {

    /**
     * The {@code rarecities} grid (64-chunk cells, ±16 jitter) with about one center in five refused, and landmarks
     * about as dense as the trace world's: a blocked disc (core plus margin) of radius 4 to 7 in about one 32-chunk
     * cell in three. A center inside a disc is no city, as a landmark's core forces non-city.
     */
    private static final class Nodes implements CityLinkGraph.Nodes {

        private final CityGrid grid;
        private final long seed;

        Nodes(long seed) {
            this.grid = new CityGrid(seed, 64, 16, 100, 256);
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
            return blocked(c.chunkX(), c.chunkZ()) ? null : new int[] { c.chunkX(), c.chunkZ() };
        }

        @Override
        public boolean blocked(int chunkX, int chunkZ) {
            int cx = Math.floorDiv(chunkX, 32);
            int cz = Math.floorDiv(chunkZ, 32);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    long h = mix(seed ^ (cx + ox) * 0x632BE59BD9B4E019L ^ (cz + oz) * 0x85157AF5L);
                    if (Long.remainderUnsigned(h, 3) != 0) {
                        continue;
                    }
                    int lx = (cx + ox) * 32 + (int) ((h >>> 8) & 31);
                    int lz = (cz + oz) * 32 + (int) ((h >>> 16) & 31);
                    int r = 4 + (int) ((h >>> 24) & 3);
                    long dx = chunkX - lx;
                    long dz = chunkZ - lz;
                    if (dx * dx + dz * dz <= (long) r * r) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static long mix(long h) {
            h ^= h >>> 33;
            h *= 0xFF51AFD7ED558CCDL;
            h ^= h >>> 33;
            h *= 0xC4CEB9FE1A85EC53L;
            return h ^ h >>> 33;
        }
    }

    /**
     * Over many seeds, every city in a 9 x 9-cell window has at least two links, every link's path is clear of
     * landmarks, steps between 8-neighbors and turns at most 45° per chunk, so it is built of a plain link's pieces.
     */
    @Test
    void noCityIsALeaf() {
        int nodes = 0;
        int detours = 0;
        for (long seed = 1; seed <= 60; seed++) {
            Nodes oracle = new Nodes(seed * 7919L);
            CityLinkGraph graph = new CityLinkGraph(oracle, 512);
            for (int cx = -4; cx <= 4; cx++) {
                for (int cz = -4; cz <= 4; cz++) {
                    int[] self = graph.node(cx, cz);
                    if (self == null) {
                        continue;
                    }
                    nodes++;
                    List<CityLinkGraph.Link> links = graph.links(cx, cz);
                    assertTrue(
                        links.size() >= 2,
                        "seed " + seed + " city " + self[0] + "," + self[1] + " has " + links.size() + " links");
                    for (CityLinkGraph.Link link : links) {
                        if (!link.path()
                            .equals(LinkRaster.rasterize(link.ax(), link.az(), link.bx(), link.bz()))) {
                            detours++;
                        }
                        checkPath(oracle, link);
                    }
                }
            }
        }
        assertTrue(nodes > 3000, "enough cities sampled: " + nodes);
        assertTrue(detours > 0, "the fixture's landmarks force detours");
    }

    private static void checkPath(Nodes oracle, CityLinkGraph.Link link) {
        List<LinkRaster.Step> path = link.path();
        assertEquals(link.ax(), path.get(0)
            .chunkX());
        assertEquals(link.az(), path.get(0)
            .chunkZ());
        LinkRaster.Step last = path.get(path.size() - 1);
        assertEquals(link.bx(), last.chunkX());
        assertEquals(link.bz(), last.chunkZ());
        for (int i = 0; i < path.size(); i++) {
            LinkRaster.Step s = path.get(i);
            assertTrue(!oracle.blocked(s.chunkX(), s.chunkZ()), link + " crosses a landmark at " + s);
            if (i > 0) {
                LinkRaster.Step p = path.get(i - 1);
                assertEquals(s.chunkX() - p.chunkX(), s.inDx());
                assertEquals(s.chunkZ() - p.chunkZ(), s.inDz());
            }
            if (i > 0 && i < path.size() - 1) {
                // Throws for any opening pair a link piece cannot join.
                assertNotNull(HighwayInfo.Piece.link(-s.inDx(), -s.inDz(), 1, s.outDx(), s.outDz(), 1));
                assertTrue(s.inDx() * s.outDx() + s.inDz() * s.outDz() > 0, link + " turns sharply at " + s);
            }
        }
    }

    /** The graph answers the same whichever cell asks first (CONTRIBUTING § World generation rules). */
    @Test
    void detoursDoNotDependOnQueryOrder() {
        for (long seed = 1; seed <= 8; seed++) {
            Nodes oracle = new Nodes(seed * 104729L);
            CityLinkGraph forward = new CityLinkGraph(oracle, 64);
            CityLinkGraph backward = new CityLinkGraph(oracle, 64);
            List<List<CityLinkGraph.Link>> a = new ArrayList<>();
            List<List<CityLinkGraph.Link>> b = new ArrayList<>();
            for (int cx = -3; cx <= 3; cx++) {
                for (int cz = -3; cz <= 3; cz++) {
                    a.add(forward.links(cx, cz));
                }
            }
            for (int cx = 3; cx >= -3; cx--) {
                for (int cz = 3; cz >= -3; cz--) {
                    b.add(0, backward.links(cx, cz));
                }
            }
            assertEquals(a, b);
            for (int i = 0; i < a.size(); i++) {
                for (int j = 0; j < a.get(i)
                    .size(); j++) {
                    assertEquals(
                        a.get(i)
                            .get(j)
                            .path()
                            .toString(),
                        b.get(i)
                            .get(j)
                            .path()
                            .toString());
                }
            }
            // The chunk index agrees with the paths, detours included.
            for (List<CityLinkGraph.Link> links : a) {
                for (CityLinkGraph.Link link : links) {
                    List<LinkRaster.Step> path = link.path();
                    for (int k = 0; k < path.size(); k++) {
                        boolean found = false;
                        for (CityLinkGraph.Hit hit : backward.hits(
                            path.get(k)
                                .chunkX(),
                            path.get(k)
                                .chunkZ())) {
                            found |= hit.link()
                                .equals(link) && hit.index() == k;
                        }
                        assertTrue(found, link + " step " + k + " is indexed");
                    }
                }
            }
        }
    }

    /** Smoothing removes every turn sharper than 45° and keeps the end. */
    @Test
    void smoothingKeepsTheEndAndTurnsGently() {
        List<int[]> moves = new ArrayList<>(
            Arrays.asList(
                new int[] { 1, 0 },
                new int[] { 0, 1 },
                new int[] { 1, 1 },
                new int[] { 1, -1 },
                new int[] { -1, 0 },
                new int[] { 1, 0 },
                new int[] { 0, -1 }));
        int sx = 0;
        int sz = 0;
        for (int[] m : moves) {
            sx += m[0];
            sz += m[1];
        }
        LinkRaster.smooth(moves);
        int tx = 0;
        int tz = 0;
        for (int i = 0; i < moves.size(); i++) {
            tx += moves.get(i)[0];
            tz += moves.get(i)[1];
            if (i > 0) {
                assertTrue(moves.get(i - 1)[0] * moves.get(i)[0] + moves.get(i - 1)[1] * moves.get(i)[1] > 0);
            }
        }
        assertEquals(sx, tx);
        assertEquals(sz, tz);
    }

    /** A detour through a waypoint ends at both ends and refuses a path that revisits a chunk. */
    @Test
    void rasterizeViaJoinsBothEnds() {
        List<LinkRaster.Step> path = LinkRaster.rasterizeVia(0, 0, 20, 15, 40, 0);
        assertNotNull(path);
        assertEquals(
            40,
            path.get(path.size() - 1)
                .chunkX());
        assertEquals(LinkRaster.Kind.END, path.get(0)
            .kind());
        // Doubling back cancels out: the path is the straight one.
        assertEquals(
            LinkRaster.rasterize(0, 0, 10, 0)
                .toString(),
            LinkRaster.rasterizeVia(0, 0, 30, 0, 10, 0)
                .toString());
    }

    /** The spur (D391): the written end moves in to the first city chunk, never past a landmark. */
    @Test
    void spurRunsToTheFirstCityChunk() {
        List<LinkRaster.Step> path = LinkRaster.rasterize(0, 0, 30, 0);
        // City from x = 0 to 5; the edge by radius is index 9.
        LinkHighways.ChunkTest city = (x, z) -> x <= 5;
        assertEquals(5, LinkHighways.spur(path, 9, -1, city, (x, z) -> false));
        // The chunk inward of the edge is city: the edge stays.
        assertEquals(6, LinkHighways.spur(path, 6, -1, city, (x, z) -> false));
        // A landmark between the edge and the city keeps the edge.
        assertEquals(9, LinkHighways.spur(path, 9, -1, city, (x, z) -> x == 7));
        // From the upper end: the city around x = 30 starts at x = 27.
        assertEquals(27, LinkHighways.spur(path, 22, 1, (x, z) -> x >= 27, (x, z) -> false));
    }
}
