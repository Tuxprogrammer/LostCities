package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * The plan worker routes city links in steps (D-perf2-worker-1): targets whose plans read a link graph, with a blocked
 * test that reads probed heights, are planned through a {@link PlanWorker} and its {@link NetworkGate}; the routes run
 * on the worker in bounded steps, and every chunk's link pieces then equal those of a graph routed inline with no
 * worker at all, while the server path routes nothing more.
 */
public class PlanWorkerRouteTest {

    private static final int CELL = 64;

    private static long hash(long seed, int x, int z) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        return h ^ h >>> 29;
    }

    /** Landmarks: a disc of radius 7 in about half of the 40 x 40-chunk blocks. */
    private static boolean landmark(int x, int z) {
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

    /** One world's link graph over probed heights, and a plan that reads its pieces. */
    private static final class Stack {

        final HeightProbe probe = new HeightProbe();
        final CityLinkGraph graph;
        final CachedCityPlan plan;

        Stack() {
            ChunkHeights heights = new ChunkHeights(
                (x, z) -> TestTerrain.surface(x, z) + 1,
                TerrainSettings.defaults()
                    .heightSampleSize(),
                ChunkHeights.Listener.NONE,
                probe);
            graph = new CityLinkGraph(new CityLinkGraph.Nodes() {

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
                    return new int[] { cellX * CELL + 8 + (int) ((h >>> 8) % 48),
                        cellZ * CELL + 8 + (int) ((h >>> 20) % 48) };
                }

                @Override
                public boolean blocked(int chunkX, int chunkZ) {
                    // A probed input, as the landmark masks are: the worker fetches it, one per unit.
                    heights.height(chunkX, chunkZ);
                    return landmark(chunkX, chunkZ);
                }
            }, 512, new NetworkGate(probe));
            plan = new CachedCityPlan(
                (x, z) -> ChunkPlan.none(
                    x,
                    z,
                    graph.hits(x, z)
                        .size(),
                    71,
                    71,
                    false));
        }
    }

    private static List<int[]> targets() {
        List<int[]> out = new ArrayList<>();
        for (int x = -160; x <= 160; x += 16) {
            for (int z = -160; z <= 160; z += 16) {
                out.add(new int[] { x, z });
            }
        }
        return out;
    }

    private static String describe(List<CityLinkGraph.Hit> hits) {
        StringBuilder out = new StringBuilder();
        for (CityLinkGraph.Hit hit : hits) {
            out.append(hit.link())
                .append('@')
                .append(hit.index())
                .append(' ');
        }
        return out.toString();
    }

    @Test
    public void routesSteppedOnTheWorkerEqualInlineRoutes() throws InterruptedException {
        Stack inline = new Stack();
        List<String> expected = new ArrayList<>();
        for (int[] t : targets()) {
            expected.add(describe(inline.graph.hits(t[0], t[1])));
        }
        assertEquals(0, inline.graph.steppedRoutes(), "no probe, no steps: the worker-off path");

        Stack threaded = new Stack();
        PlanLock lock = new PlanLock();
        PlanWorker worker = new PlanWorker("test route worker", threaded.plan, threaded.probe, lock, () -> {});
        PlanPrefetcher prefetcher = new PlanPrefetcher(
            threaded.plan,
            threaded.probe,
            new GenerationTimers(),
            (x, z) -> false);
        prefetcher.worker(worker);
        try {
            for (int[] t : targets()) {
                worker.submit(t[0], t[1], false);
            }
            long deadline = System.nanoTime() + 20_000_000_000L;
            while (!allCached(threaded.plan) && System.nanoTime() < deadline) {
                assertTrue(lock.openWindow(), "a window opens");
                Thread.sleep(2);
                lock.closeWindow();
                prefetcher.tick();
                if (worker.queued() == 0 && worker.peekDeferred() == null) {
                    // The worker's queue is bounded: targets it dropped are submitted again.
                    for (int[] t : targets()) {
                        if (!threaded.plan.isCached(t[0], t[1])) {
                            worker.submit(t[0], t[1], false);
                        }
                    }
                }
            }
            assertTrue(allCached(threaded.plan), "the worker planned every target: " + worker.summary() + " uncached " + uncached(threaded.plan));
        } finally {
            worker.stop();
        }
        int inlineBefore = threaded.graph.inlineRoutes();
        List<String> actual = new ArrayList<>();
        for (int[] t : targets()) {
            actual.add(describe(threaded.graph.hits(t[0], t[1])));
        }
        assertEquals(expected, actual);
        assertTrue(threaded.graph.steppedRoutes() > 0, "routes ran in steps on the worker");
        assertEquals(inlineBefore, threaded.graph.inlineRoutes(), "the server path found the worker's routes cached");
    }

    private static String uncached(CachedCityPlan plan) {
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (int[] t : targets()) {
            if (!plan.isCached(t[0], t[1])) {
                if (n++ < 12) {
                    out.append(t[0]).append(',').append(t[1]).append(' ');
                }
            }
        }
        return n + ": " + out;
    }

    private static boolean allCached(CachedCityPlan plan) {
        for (int[] t : targets()) {
            if (!plan.isCached(t[0], t[1])) {
                return false;
            }
        }
        return true;
    }
}
