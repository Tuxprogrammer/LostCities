package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * The plan prefetcher (0005 §8, B4): it plans the ring {@link PlanPrefetcher#LEAD} chunks around each generated chunk
 * a sample at a time, the plans it leaves are the plans computed without it, and the terrain phase then finds every
 * sample cached.
 */
public class PlanPrefetcherTest {

    /** A world whose plans read a wide square of height groups, as a layout cell does when it meets new ground. */
    private static final class World {

        final Set<Long> sampled = new HashSet<>();
        final HeightProbe probe = new HeightProbe();
        final ChunkHeights heights;
        final CachedCityPlan plan;
        final GenerationTimers timers = new GenerationTimers();

        World() {
            heights = new ChunkHeights((x, z) -> {
                sampled.add(((long) x << 32) | (z & 0xFFFFFFFFL));
                return 50 + Math.floorMod(x * 31 + z * 17, 60);
            }, 3, ChunkHeights.Listener.NONE, probe);
            CityPlan source = (chunkX, chunkZ) -> {
                int sum = 0;
                for (int dx = -9; dx <= 9; dx += 3) {
                    for (int dz = -9; dz <= 9; dz += 3) {
                        sum += heights.height(chunkX + dx, chunkZ + dz);
                    }
                }
                return ChunkPlan.none(chunkX, chunkZ, 0, 71, sum % 256, false);
            };
            plan = new CachedCityPlan(source);
        }

        PlanPrefetcher prefetcher() {
            return new PlanPrefetcher(plan, probe, timers, (x, z) -> false);
        }
    }

    private static void drain(PlanPrefetcher prefetcher) {
        for (int tick = 0; tick < 100_000 && prefetcher.pending() > 0; tick++) {
            prefetcher.tick();
        }
        assertEquals(0, prefetcher.pending(), "the prefetcher finished");
    }

    @Test
    public void plansTheRingAheadAndLeavesTheSamePlans() {
        World world = new World();
        PlanPrefetcher prefetcher = world.prefetcher();
        prefetcher.generated(5, -3);
        drain(prefetcher);
        World plain = new World();
        for (int i = 0; i < 8 * PlanPrefetcher.LEAD; i++) {
            int[] offset = PlanPrefetcher.perimeterOffset(i);
            int x = 5 + offset[0];
            int z = -3 + offset[1];
            assertEquals(
                plain.plan.chunk(x, z)
                    .naturalHeight(),
                world.plan.chunk(x, z)
                    .naturalHeight(),
                "chunk " + x + "," + z);
        }
        assertEquals(plain.sampled, world.sampled, "the same samples, each taken once");
        assertFalse(world.probe.active(), "the probe is off between ticks");
        assertTrue(
            world.timers.histogram(GenerationTimers.Series.PREFETCH, null)
                .count() > 0,
            "the prefetcher's ticks are timed");
    }

    @Test
    public void theTerrainPhaseFindsEverySampleCached() {
        World world = new World();
        PlanPrefetcher prefetcher = world.prefetcher();
        prefetcher.generated(0, 0);
        drain(prefetcher);
        int before = world.sampled.size();
        long samples = world.heights.samples();
        // The front reaches the ring: the plans there and the samples they read are all held.
        int[] offset = PlanPrefetcher.perimeterOffset(3);
        world.plan.chunk(offset[0], offset[1]);
        assertEquals(before, world.sampled.size());
        assertEquals(samples, world.heights.samples());
    }

    @Test
    public void aHeavyFetchEndsItsTick() {
        World world = new World();
        boolean[] started = new boolean[1];
        CityPlan source = (chunkX, chunkZ) -> {
            if (!started[0] && world.probe.active()) {
                // A village's start, which the prefetcher builds alone.
                throw world.probe.missHeavy(() -> started[0] = true);
            }
            return ChunkPlan.none(chunkX, chunkZ, 0, 71, 64, false);
        };
        CachedCityPlan plan = new CachedCityPlan(source);
        PlanPrefetcher prefetcher = new PlanPrefetcher(plan, world.probe, world.timers, (x, z) -> false);
        prefetcher.generated(0, 0);
        assertEquals(1, prefetcher.tick(), "the heavy fetch, and nothing after it");
        assertTrue(started[0]);
        assertTrue(prefetcher.pending() > 0);
        drain(prefetcher);
        assertTrue(plan.isCached(-PlanPrefetcher.LEAD, -PlanPrefetcher.LEAD));
    }

    @Test
    public void loadedChunksAreSkipped() {
        World world = new World();
        PlanPrefetcher prefetcher = new PlanPrefetcher(world.plan, world.probe, world.timers, (x, z) -> true);
        prefetcher.generated(0, 0);
        drain(prefetcher);
        assertTrue(world.sampled.isEmpty(), "a loaded chunk was generated, so its plan needs no prefetch");
    }

    @Test
    public void rememberedChunksAreBounded() {
        World world = new World();
        PlanPrefetcher prefetcher = world.prefetcher();
        for (int i = 0; i < PlanPrefetcher.RECENT + 10; i++) {
            prefetcher.generated(i * 100, 0);
        }
        assertEquals(PlanPrefetcher.RECENT, prefetcher.pending(), "bounded");
    }

    @Test
    public void theRingIsEveryCellAtTheLead() {
        Set<Long> cells = new HashSet<>();
        for (int i = 0; i < 8 * PlanPrefetcher.LEAD; i++) {
            int[] offset = PlanPrefetcher.perimeterOffset(i);
            assertEquals(PlanPrefetcher.LEAD, Math.max(Math.abs(offset[0]), Math.abs(offset[1])));
            cells.add(((long) offset[0] << 32) | (offset[1] & 0xFFFFFFFFL));
        }
        assertEquals(8 * PlanPrefetcher.LEAD, cells.size());
    }
}
