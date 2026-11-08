package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The bounded detour search (D-perf2-route-2): it replaces only the blocked stretch of the straight raster, never
 * crosses a blocked chunk, keeps every turn at 45° or less, gives the same path whichever end asks, and fails
 * deterministically once it has spent its expansion cap.
 */
class DetourSearchTest {

    /** Blocked discs of radius 6 on the line every 80 chunks, the first at x = 120. */
    private static boolean discs(int x, int z) {
        for (int cx = 120; cx <= 520; cx += 80) {
            long dx = x - cx;
            long dz = z - (cx % 160 == 0 ? 3 : -2);
            if (dx * dx + dz * dz <= 36) {
                return true;
            }
        }
        return false;
    }

    private static void checkPath(List<LinkRaster.Step> path, DetourSearch.Blocked blocked, int ax, int az, int bx,
        int bz, int maxDistance) {
        assertEquals(
            ax,
            path.get(0)
                .chunkX());
        assertEquals(
            az,
            path.get(0)
                .chunkZ());
        LinkRaster.Step last = path.get(path.size() - 1);
        assertEquals(bx, last.chunkX());
        assertEquals(bz, last.chunkZ());
        Set<Long> seen = new HashSet<>();
        long max = (long) maxDistance * maxDistance;
        for (int i = 0; i < path.size(); i++) {
            LinkRaster.Step s = path.get(i);
            assertTrue(!blocked.test(s.chunkX(), s.chunkZ()), "crosses a blocked chunk at " + s);
            assertTrue(seen.add((long) s.chunkX() << 32 | s.chunkZ() & 0xffffffffL), "revisits " + s);
            long da = (long) (s.chunkX() - ax) * (s.chunkX() - ax) + (long) (s.chunkZ() - az) * (s.chunkZ() - az);
            long db = (long) (s.chunkX() - bx) * (s.chunkX() - bx) + (long) (s.chunkZ() - bz) * (s.chunkZ() - bz);
            assertTrue(da <= max && db <= max, "strays out of reach at " + s);
            if (i > 0) {
                LinkRaster.Step p = path.get(i - 1);
                assertEquals(s.chunkX() - p.chunkX(), s.inDx());
                assertEquals(s.chunkZ() - p.chunkZ(), s.inDz());
            }
            if (i > 0 && i < path.size() - 1) {
                assertTrue(s.inDx() * s.outDx() + s.inDz() * s.outDz() > 0, "turns sharply at " + s);
            }
        }
    }

    @Test
    void detoursOnlyTheBlockedStretch() {
        List<LinkRaster.Step> straight = LinkRaster.rasterize(0, 0, 600, 0);
        DetourSearch.Blocked blocked = DetourSearchTest::discs;
        List<LinkRaster.Step> path = DetourSearch.detour(straight, blocked, 640);
        assertNotNull(path);
        checkPath(path, blocked, 0, 0, 600, 0, 640);
        // The first blocked chunk is x = 114; the raster before 114 - MARGIN, and after the last disc's far edge plus
        // MARGIN, is kept as it was.
        int keep = 114 - DetourSearch.MARGIN;
        for (int i = 0; i <= keep; i++) {
            assertEquals(
                straight.get(i)
                    .toString(),
                path.get(i)
                    .toString(),
                "prefix step " + i);
        }
        int tail = 600 - (526 + DetourSearch.MARGIN);
        for (int k = 1; k < tail; k++) {
            assertEquals(
                straight.get(straight.size() - k)
                    .toString(),
                path.get(path.size() - k)
                    .toString(),
                "suffix step " + k);
        }
    }

    @Test
    void theSameRouteFromEitherEndAndInAnyOrder() {
        CityLinkGraph.Nodes nodes = new CityLinkGraph.Nodes() {

            @Override
            public int cell() {
                return 256;
            }

            @Override
            public int[] cityCenter(int cellX, int cellZ) {
                return null;
            }

            @Override
            public boolean blocked(int chunkX, int chunkZ) {
                return discs(chunkX, chunkZ - 7) || discs(chunkX + 40, chunkZ + 11);
            }
        };
        int[][] pairs = { { 0, 0, 600, 0 }, { 10, 7, 590, 20 }, { 40, -11, 400, 30 }, { 560, 5, 100, -4 } };
        CityLinkGraph forward = new CityLinkGraph(nodes, 64);
        CityLinkGraph backward = new CityLinkGraph(nodes, 64);
        int detours = 0;
        for (int i = 0; i < pairs.length; i++) {
            int[] p = pairs[i];
            int[] q = pairs[pairs.length - 1 - i];
            CityLinkGraph.Link a = forward.route(p[0], p[1], p[2], p[3]);
            CityLinkGraph.Link b = backward.route(q[2], q[3], q[0], q[1]);
            assertNotNull(a);
            assertNotNull(b);
        }
        for (int[] p : pairs) {
            CityLinkGraph.Link a = forward.route(p[0], p[1], p[2], p[3]);
            CityLinkGraph.Link b = backward.route(p[2], p[3], p[0], p[1]);
            assertEquals(a, b);
            assertEquals(
                a.path()
                    .toString(),
                b.path()
                    .toString());
            checkPath(a.path(), nodes::blocked, a.ax(), a.az(), a.bx(), a.bz(), forward.maxDistance());
            if (!a.path()
                .equals(LinkRaster.rasterize(a.ax(), a.az(), a.bx(), a.bz()))) {
                detours++;
            }
        }
        assertTrue(detours > 0, "the fixture forces detours");
    }

    @Test
    void aWallAcrossTheCorridorFailsAtTheCap() {
        List<LinkRaster.Step> straight = LinkRaster.rasterize(0, 0, 600, 0);
        DetourSearch search = new DetourSearch(straight, (x, z) -> x == 300, 640);
        assertNull(search.find());
        assertEquals(DetourSearch.MAX_EXPANSIONS, search.expansions());
        // Again, the same answer: the cap is deterministic.
        DetourSearch again = new DetourSearch(straight, (x, z) -> x == 300, 640);
        assertNull(again.find());
        assertEquals(search.expansions(), again.expansions());
    }

    @Test
    void anEnclosedEndFailsWithinTheCap() {
        List<LinkRaster.Step> straight = LinkRaster.rasterize(0, 0, 300, 0);
        DetourSearch search = new DetourSearch(straight, (x, z) -> {
            int r = Math.max(Math.abs(x - 300), Math.abs(z));
            return r == 12 || r == 13;
        }, 640);
        assertNull(search.find());
        assertTrue(search.expansions() <= DetourSearch.MAX_EXPANSIONS);
        // A blocked end needs no search at all.
        DetourSearch blockedEnd = new DetourSearch(straight, (x, z) -> x == 0 && z == 0, 640);
        assertNull(blockedEnd.find());
        assertEquals(0, blockedEnd.expansions());
    }
}
