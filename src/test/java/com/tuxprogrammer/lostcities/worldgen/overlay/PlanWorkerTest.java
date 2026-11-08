package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.NetworkSettings;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.CityPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.city.CityWriteContext;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The plan worker leaves the same plans as the server thread alone (D-plan-worker-1..3): a 32 x 32 region planned
 * inline on one stack and through a {@link PlanWorker} on an identical one, with the test thread in the server's role
 * (holding the {@link PlanLock} outside windows, running deferred fetches through the prefetcher, planning a chunk
 * inline now and then), gives equal descriptions chunk by chunk.
 */
public class PlanWorkerTest {

    private static final int SIZE = 32;

    /** One world's planning objects: probed heights, a structure oracle whose fetches are deferred, the network gate. */
    private static final class Stack {

        final HeightProbe probe = new HeightProbe();
        final Set<Long> structureRegions = ConcurrentHashMap.newKeySet();
        final CachedCityPlan plan;

        Stack() {
            ChunkHeights heights = new ChunkHeights(
                (x, z) -> TestTerrain.surface(x, z) + 1,
                TerrainSettings.defaults()
                    .heightSampleSize(),
                ChunkHeights.Listener.NONE,
                probe);
            TerrainView terrain = new TerrainView() {

                @Override
                public int height(int chunkX, int chunkZ) {
                    return heights.height(chunkX, chunkZ);
                }

                @Override
                public boolean isWaterBiome(int chunkX, int chunkZ) {
                    return height(chunkX, chunkZ) < 63;
                }

                @Override
                public boolean isOcean(int chunkX, int chunkZ) {
                    return false;
                }
            };
            // A stand-in for an oracle not proven pure (VillageOracle): its fetch is this test's, so the worker
            // defers it to the server thread.
            StructureOracle structures = (minX, minZ, maxX, maxZ) -> {
                long key = ((long) (minX >> 4) << 32) | ((minZ >> 4) & 0xFFFFFFFFL);
                if (!structureRegions.contains(key)) {
                    if (probe.active()) {
                        throw probe.miss(() -> structureRegions.add(key));
                    }
                    structureRegions.add(key);
                }
                return Collections.emptyList();
            };
            TerrainSettings settings = TerrainSettings.defaults();
            WorldStyle worldStyle = CityFixtures.ASSETS.worldStyle(CityProfiles.WORLD_STYLE);
            LayoutPlanner planner = new LayoutPlanner(
                LayoutInputs
                    .builder(
                        CityFixtures.SEED,
                        CityProfiles.layoutProfile(settings),
                        new LayoutAssets(CityFixtures.ASSETS, worldStyle, CityFixtures.plains()),
                        terrain)
                    .structures(structures)
                    .networks(
                        new NetworkSettings(
                            NetworkSettings.DEFAULTS.highways(),
                            NetworkSettings.DEFAULTS.railways(),
                            new NetworkGate(probe)))
                    .build());
            CityWriteContext context = new CityWriteContext(
                CityFixtures.SEED,
                planner,
                CityFixtures.ASSETS,
                worldStyle,
                new CityPalettes(CityFixtures.ASSETS, MetalFlags.ALL_ON),
                settings,
                CityFixtures.plains(),
                new NameBlockClassifier(),
                message -> {});
            plan = new CachedCityPlan(new LayoutCityPlan(context, structures, terrain));
        }
    }

    private static String describe(ChunkPlan plan) {
        StringBuilder out = new StringBuilder(plan.toString()).append(' ')
            .append(plan.label())
            .append(' ')
            .append(plan.jobs())
            .append(' ')
            .append(plan.hasHighway());
        for (int i = 0; i < 256; i += 17) {
            out.append(',')
                .append(plan.footprintBottom(i & 15, i >> 4));
        }
        return out.toString();
    }

    @Test
    public void theWorkerLeavesTheSamePlansAsTheServerThread() throws InterruptedException {
        // The region: the first 32 x 32 block, nearest the origin, with a city chunk and a highway in it.
        Stack inline = new Stack();
        int minX = 0;
        int minZ = 0;
        List<String> expected = null;
        search: for (int ring = 0; ring <= 8; ring++) {
            for (int rx = -ring; rx <= ring; rx++) {
                for (int rz = -ring; rz <= ring; rz++) {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) != ring) {
                        continue;
                    }
                    List<String> region = regionWithCityAndHighway(inline.plan, rx * SIZE, rz * SIZE);
                    if (region != null) {
                        minX = rx * SIZE;
                        minZ = rz * SIZE;
                        expected = region;
                        break search;
                    }
                }
            }
        }
        assertTrue(expected != null, "a region with a city chunk and a highway near the origin");

        Stack threaded = new Stack();
        PlanLock lock = new PlanLock();
        PlanWorker worker = new PlanWorker("test plan worker", threaded.plan, threaded.probe, lock, () -> {});
        PlanPrefetcher prefetcher = new PlanPrefetcher(
            threaded.plan,
            threaded.probe,
            new GenerationTimers(),
            (x, z) -> false);
        prefetcher.worker(worker);
        try {
            for (int x = minX; x < minX + SIZE; x++) {
                for (int z = minZ; z < minZ + SIZE; z++) {
                    worker.submit(x, z, false);
                }
            }
            long deadline = System.nanoTime() + 120_000_000_000L;
            int round = 0;
            while (!allCached(threaded.plan, minX, minZ) && System.nanoTime() < deadline) {
                assertTrue(lock.openWindow(), "a window opens");
                Thread.sleep(2);
                lock.closeWindow();
                // The server thread's turn: the deferred fetches, and now and then a plan of its own.
                prefetcher.tick();
                if (round++ % 7 == 0) {
                    threaded.plan.chunk(minX + round % SIZE, minZ + (round * 5) % SIZE);
                }
                if (worker.queued() == 0 && worker.peekDeferred() == null) {
                    // Targets the worker dropped on a deferral are planned inline, as a chunk's own phase would.
                    for (int x = minX; x < minX + SIZE; x++) {
                        for (int z = minZ; z < minZ + SIZE; z++) {
                            if (!threaded.plan.isCached(x, z)) {
                                worker.submit(x, z, false);
                            }
                        }
                    }
                }
            }
            assertFalse(threaded.probe.active(), "the probe is off while the server thread holds the lock");
            String summary = worker.summary();
            assertTrue(summary.matches("plan worker: [1-9][0-9]* plans.*"), summary);
            assertTrue(summary.matches(".* [1-9][0-9]* deferred to the server thread.*"), summary);
        } finally {
            worker.stop();
        }
        List<String> actual = new ArrayList<>();
        for (int x = minX; x < minX + SIZE; x++) {
            for (int z = minZ; z < minZ + SIZE; z++) {
                actual.add(describe(threaded.plan.chunk(x, z)));
            }
        }
        assertEquals(expected, actual);
    }

    /** The region's descriptions, or {@code null} unless it holds a city chunk and a highway. */
    private static List<String> regionWithCityAndHighway(CachedCityPlan plan, int minX, int minZ) {
        List<String> out = new ArrayList<>();
        boolean city = false;
        boolean highway = false;
        for (int x = minX; x < minX + SIZE; x++) {
            for (int z = minZ; z < minZ + SIZE; z++) {
                ChunkPlan chunk = plan.chunk(x, z);
                city |= chunk.isCity();
                highway |= chunk.hasHighway();
                out.add(describe(chunk));
            }
        }
        return city && highway ? out : null;
    }

    private static boolean allCached(CachedCityPlan plan, int minX, int minZ) {
        for (int x = minX; x < minX + SIZE; x++) {
            for (int z = minZ; z < minZ + SIZE; z++) {
                if (!plan.isCached(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }
}
