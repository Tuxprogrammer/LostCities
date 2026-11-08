package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;

/**
 * Routes computed in bounded steps (D-perf2-worker-1) equal routes computed at once: a {@link DetourSearch} advanced a
 * few units at a time, or interrupted by a blocked test that throws the first time it meets new ground, ends with the
 * same path as {@link DetourSearch#find}; each step keeps its work bound; a graph whose gate defers routes to a
 * stand-in
 * worker publishes only finished routes, and the server path then finds them cached and routes nothing inline.
 */
public class RouteStepTest {

    private static final int CELL = 64;

    /** Thrown by the stand-in probe; carries nothing. */
    private static final class Miss extends RuntimeException {

        private static final long serialVersionUID = 1L;

        Miss() {
            super("miss", null, false, false);
        }
    }

    private static long hash(long seed, int x, int z) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        return h ^ h >>> 29;
    }

    /** Landmarks: a disc of radius 7 in about half of the 40 x 40-chunk blocks. */
    static boolean landmark(int x, int z) {
        int bx = Math.floorDiv(x, 40);
        int bz = Math.floorDiv(z, 40);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                long h = hash(7, bx + ox, bz + oz);
                if ((h & 1) == 0) {
                    continue;
                }
                int cx = (bx + ox) * 40 + 8 + (int) ((h >>> 8) & 23);
                int cz = (bz + oz) * 40 + 8 + (int) ((h >>> 16) & 23);
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= 49) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A node oracle over {@link #landmark}; its blocked test throws once per new 8 x 8-chunk region while armed. */
    private static final class Probed implements CityLinkGraph.Nodes {

        final Set<Long> fetched = new HashSet<>();
        boolean armed;
        int misses;

        @Override
        public int cell() {
            return CELL;
        }

        @Override
        public int[] cityCenter(int cellX, int cellZ) {
            long h = hash(3, cellX, cellZ);
            if ((h & 3) == 0) {
                return null;
            }
            return new int[] { cellX * CELL + 8 + (int) ((h >>> 8) % 48), cellZ * CELL + 8 + (int) ((h >>> 20) % 48) };
        }

        @Override
        public boolean blocked(int chunkX, int chunkZ) {
            long region = (long) (chunkX >> 3) << 32 | (chunkZ >> 3) & 0xffffffffL;
            if (armed && !fetched.contains(region)) {
                misses++;
                throw new Miss();
            }
            return landmark(chunkX, chunkZ);
        }

        void fetchAll() {
            armed = false;
        }
    }

    /** Pairs of nodes about a link apart, many of them crossing a landmark. */
    private static List<int[]> pairs(Probed nodes, int count) {
        Random random = new Random(42);
        List<int[]> out = new ArrayList<>();
        while (out.size() < count) {
            int cx = random.nextInt(20) - 10;
            int cz = random.nextInt(20) - 10;
            int[] a = nodes.cityCenter(cx, cz);
            int[] b = nodes.cityCenter(cx + random.nextInt(3) - 1, cz + random.nextInt(3) - 1);
            if (a != null && b != null && (a[0] != b[0] || a[1] != b[1])) {
                out.add(new int[] { a[0], a[1], b[0], b[1] });
            }
        }
        return out;
    }

    private static String describe(List<LinkRaster.Step> path) {
        if (path == null) {
            return "none";
        }
        StringBuilder out = new StringBuilder();
        for (LinkRaster.Step step : path) {
            out.append(step.chunkX())
                .append(',')
                .append(step.chunkZ())
                .append(',')
                .append(step.outDx())
                .append(',')
                .append(step.outDz())
                .append(' ');
        }
        return out.toString();
    }

    private static String describe(CityLinkGraph.Link link) {
        return link == null ? "none" : link.toString() + " " + describe(link.path());
    }

    private static List<LinkRaster.Step> straight(int[] p) {
        boolean lower = LinkRaster.lower(p[0], p[1], p[2], p[3]);
        return lower ? LinkRaster.rasterize(p[0], p[1], p[2], p[3]) : LinkRaster.rasterize(p[2], p[3], p[0], p[1]);
    }

    @Test
    public void aSearchAdvancedInStepsEndsWithTheOneShotPath() {
        Probed nodes = new Probed();
        Random random = new Random(7);
        int detours = 0;
        for (int[] p : pairs(nodes, 120)) {
            List<LinkRaster.Step> straight = straight(p);
            String once = describe(DetourSearch.detour(straight, nodes::blocked, 160));
            DetourSearch stepped = new DetourSearch(straight, nodes::blocked, 160);
            int calls = 0;
            while (true) {
                int budget = 1 + random.nextInt(40);
                int before = stepped.expansions();
                long workBefore = stepped.work();
                boolean done = stepped.advance(budget);
                assertTrue(stepped.expansions() - before <= budget, "a step expands at most its budget");
                assertTrue(stepped.work() - workBefore <= budget, "a step spends at most its budget");
                calls++;
                if (done) {
                    break;
                }
            }
            assertEquals(once, describe(stepped.result()), "pair " + p[0] + "," + p[1] + " " + p[2] + "," + p[3]);
            if (calls > 1 && stepped.expansions() > 0) {
                detours++;
            }
        }
        assertTrue(detours > 5, "the fixture has detours that take several steps: " + detours);
    }

    @Test
    public void aSearchInterruptedByMissesEndsWithTheOneShotPath() {
        Probed reference = new Probed();
        Probed nodes = new Probed();
        for (int[] p : pairs(reference, 80)) {
            List<LinkRaster.Step> straight = straight(p);
            String once = describe(DetourSearch.detour(straight, reference::blocked, 160));
            nodes.fetched.clear();
            nodes.armed = true;
            DetourSearch stepped = new DetourSearch(straight, (x, z) -> {
                try {
                    return nodes.blocked(x, z);
                } catch (Miss miss) {
                    // The fetch is the caller's; record the region and abandon this unit.
                    nodes.fetched.add((long) (x >> 3) << 32 | (z >> 3) & 0xffffffffL);
                    throw miss;
                }
            }, 160);
            boolean done = false;
            while (!done) {
                try {
                    done = stepped.advance(16);
                } catch (Miss retry) {
                    // the unit is repeated by the next call
                }
            }
            assertEquals(once, describe(stepped.result()));
        }
        assertTrue(nodes.misses > 20, "the searches met new ground: " + nodes.misses);
    }

    /** A gate that defers every route while on, keeping the step for a stand-in worker. */
    private static final class Deferring implements PlanningGate {

        boolean on = true;
        BooleanSupplier pending;

        @Override
        public void enter(Runnable compute) {}

        @Override
        public void enterSteps(BooleanSupplier step) {
            if (on) {
                pending = step;
                throw new Miss();
            }
        }
    }

    @Test
    public void routeTasksPublishOnlyFinishedRoutesEqualToInline() {
        Probed nodes = new Probed();
        CityLinkGraph inline = new CityLinkGraph(nodes, 256);
        CityLinkGraph graph = new CityLinkGraph(nodes, 256);
        for (int[] p : pairs(nodes, 60)) {
            CityLinkGraph.RouteTask task = graph.routeTask(p[0], p[1], p[2], p[3]);
            assertNotNull(task);
            while (!task.step()) {
                assertFalse(graph.routeCached(p[0], p[1], p[2], p[3]), "no route is published before it is finished");
            }
            assertTrue(graph.routeCached(p[0], p[1], p[2], p[3]));
            assertNull(graph.routeTask(p[0], p[1], p[2], p[3]), "a finished route needs no task");
            assertEquals(describe(inline.route(p[0], p[1], p[2], p[3])), describe(graph.route(p[2], p[3], p[0], p[1])));
        }
        assertEquals(0, graph.inlineRoutes(), "every route came from its steps");
    }

    @Test
    public void routesSteppedByTheWorkerAreCacheHitsOnTheServerPath() {
        Probed nodes = new Probed();
        Deferring gate = new Deferring();
        CityLinkGraph graph = new CityLinkGraph(nodes, 512, gate);
        CityLinkGraph inline = new CityLinkGraph(new Probed(), 512);
        List<String> expected = new ArrayList<>();
        List<int[]> chunks = new ArrayList<>();
        for (int x = -200; x <= 200; x += 23) {
            for (int z = -200; z <= 200; z += 29) {
                chunks.add(new int[] { x, z });
                expected.add(describeHits(inline.hits(x, z)));
            }
        }
        // The stand-in worker: plan each chunk, and on a deferral run the route's steps, then plan again.
        int steps = 0;
        for (int[] c : chunks) {
            while (true) {
                try {
                    graph.hits(c[0], c[1]);
                    break;
                } catch (Miss deferred) {
                    BooleanSupplier step = gate.pending;
                    gate.pending = null;
                    while (!step.getAsBoolean()) {
                        steps++;
                    }
                    steps++;
                }
            }
        }
        assertTrue(graph.steppedRoutes() > 0 && steps >= graph.steppedRoutes(), "routes ran in steps");
        // The server thread's turn: the gate lets everything through, and nothing is routed inline.
        gate.on = false;
        List<String> actual = new ArrayList<>();
        for (int[] c : chunks) {
            actual.add(describeHits(graph.hits(c[0], c[1])));
        }
        assertEquals(expected, actual);
        assertEquals(0, graph.inlineRoutes(), "the server path found every route cached");
    }

    private static String describeHits(List<CityLinkGraph.Hit> hits) {
        StringBuilder out = new StringBuilder();
        for (CityLinkGraph.Hit hit : hits) {
            out.append(hit.link())
                .append('@')
                .append(hit.index())
                .append(' ');
        }
        return out.toString();
    }
}
