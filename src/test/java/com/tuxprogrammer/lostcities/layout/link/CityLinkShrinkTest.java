package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.CityGrid;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph.Link;

/**
 * The link graph's shared and bounded work (D-linkgraph-shrink-2, -3) changes no answer: a route is the same whichever
 * end asks and is computed once, and the bounded scans of {@link CityLinkGraph#links} and {@link CityLinkGraph#hits}
 * find exactly what a scan of every cell within {@link CityLinkGraph#reach()} finds.
 */
class CityLinkShrinkTest {

    /** The {@code rarecities} scale: 256-chunk cells, with blocked discs dense enough that many links detour. */
    private static final class Nodes implements CityLinkGraph.Nodes {

        private final CityGrid grid = new CityGrid(77L, 256, 64, 100, 256);

        @Override
        public int cell() {
            return grid.cell();
        }

        @Override
        public int[] cityCenter(int cellX, int cellZ) {
            CityGrid.Center c = grid.center(cellX, cellZ);
            if (c == null || blocked(c.chunkX(), c.chunkZ()) || Math.floorMod(cellX * 7 + cellZ * 3, 5) == 0) {
                return null;
            }
            return new int[] { c.chunkX(), c.chunkZ() };
        }

        @Override
        public boolean blocked(int chunkX, int chunkZ) {
            // A disc of radius 6 at the middle of every 48-chunk square whose hash says so, about one in three.
            int sx = Math.floorDiv(chunkX, 48);
            int sz = Math.floorDiv(chunkZ, 48);
            long h = (sx * 0x632BE59BD9B4E019L ^ sz * 0x85157AF5L) * 0x9E3779B97F4A7C15L;
            if (Long.remainderUnsigned(h >>> 7, 3) != 0) {
                return false;
            }
            int dx = chunkX - (sx * 48 + 24);
            int dz = chunkZ - (sz * 48 + 24);
            return dx * dx + dz * dz <= 36;
        }
    }

    @Test
    public void aRouteIsTheSameWhicheverEndAsksAndIsComputedOnce() {
        // A long pair across blocked discs that has a route, found in a scratch graph.
        CityLinkGraph scratch = new CityLinkGraph(new Nodes(), 64);
        int bz = 90;
        while (scratch.route(10, 30, 560, bz) == null) {
            bz += 7;
        }
        CityLinkGraph graph = new CityLinkGraph(new Nodes(), 64);
        Link ab = graph.route(10, 30, 560, bz);
        Link ba = graph.route(560, bz, 10, 30);
        assertSame(ab, ba);
        assertEquals(1, graph.cachedRoutes());
        CityLinkGraph fresh = new CityLinkGraph(new Nodes(), 64);
        Link reverse = fresh.route(560, bz, 10, 30);
        assertEquals(ab, reverse);
        assertEquals(steps(ab), steps(reverse));
    }

    @Test
    public void boundedScansFindWhatEveryCellInReachFinds() {
        CityLinkGraph graph = new CityLinkGraph(new Nodes(), 1024);
        int reach = graph.reach();
        int cell = graph.cell();
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                int[] self = graph.node(cx, cz);
                List<Link> expected = new ArrayList<>();
                List<Link> crossing = new ArrayList<>();
                for (int ox = -reach; ox <= reach; ox++) {
                    for (int oz = -reach; oz <= reach; oz++) {
                        for (Link link : graph.chosen(cx + ox, cz + oz)) {
                            if (self != null
                                && (link.ax() == self[0] && link.az() == self[1]
                                    || link.bx() == self[0] && link.bz() == self[1])
                                && !expected.contains(link)) {
                                expected.add(link);
                            }
                            if (!crossing.contains(link)) {
                                crossing.add(link);
                            }
                        }
                    }
                }
                List<Link> actual = graph.links(cx, cz);
                assertEquals(expected.size(), actual.size(), "links at cell " + cx + "," + cz);
                assertTrue(actual.containsAll(expected), "links at cell " + cx + "," + cz);
                // Every hit a full scan would index in this cell, sampled every 4 chunks and on every path chunk.
                int hitsSeen = 0;
                for (Link link : crossing) {
                    List<LinkRaster.Step> path = link.path();
                    for (int i = 0; i < path.size(); i++) {
                        LinkRaster.Step step = path.get(i);
                        if (Math.floorDiv(step.chunkX(), cell) != cx || Math.floorDiv(step.chunkZ(), cell) != cz) {
                            continue;
                        }
                        boolean found = false;
                        for (CityLinkGraph.Hit hit : graph.hits(step.chunkX(), step.chunkZ())) {
                            found |= hit.link()
                                .equals(link) && hit.index() == i;
                        }
                        assertTrue(found, link + " step " + i + " missing from the index");
                        hitsSeen++;
                    }
                }
                int indexed = 0;
                for (int x = cx * cell; x < cx * cell + cell; x++) {
                    for (int z = cz * cell; z < cz * cell + cell; z++) {
                        indexed += graph.hits(x, z)
                            .size();
                    }
                }
                assertEquals(hitsSeen, indexed, "hits in cell " + cx + "," + cz);
            }
        }
    }

    private static List<String> steps(Link link) {
        List<String> out = new ArrayList<>();
        for (LinkRaster.Step step : link.path()) {
            out.add(step.toString() + step.inDx() + step.inDz() + step.outDx() + step.outDz());
        }
        return out;
    }
}
