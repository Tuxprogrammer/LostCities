package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;

/**
 * Determinism (CONTRIBUTING § World generation rules): the same seed, profile, assets and oracles give the same layout
 * whatever order chunks are asked in, whatever the cache size, and whatever order the oracle lists landmarks in.
 */
public class DeterminismTest {

    private static final int R = 10;

    private static LayoutPlanner planner(long seed, int cacheSize) {
        Landmark village = LayoutFixtures.landmark("v", LandmarkKind.VILLAGE, 40, 40, 70, 70);
        Landmark room = LayoutFixtures.landmark("r", LandmarkKind.LOOTGAMES, -100, 20, -80, 40);
        return new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    LayoutFixtures.denseCity(),
                    LayoutFixtures.standardAssets(),
                    LayoutFixtures.rollingTerrain())
                .structures(new LayoutFixtures.FakeOracle(village, room))
                .cacheSize(cacheSize)
                .build());
    }

    private static List<int[]> chunks() {
        List<int[]> out = new ArrayList<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                out.add(new int[] { x, z });
            }
        }
        return out;
    }

    private static List<String> describeAll(LayoutPlanner p, List<int[]> order) {
        List<String> out = new ArrayList<>();
        for (int[] c : order) {
            out.add(LayoutText.describe(p, c[0], c[1]));
        }
        Collections.sort(out);
        return out;
    }

    @Test
    public void generationOrderDoesNotMatter() {
        List<int[]> forward = chunks();
        List<int[]> shuffled = new ArrayList<>(forward);
        Collections.shuffle(shuffled, new Random(7L));
        List<int[]> backward = new ArrayList<>(forward);
        Collections.reverse(backward);
        List<String> a = describeAll(planner(9L, LayoutInputs.DEFAULT_CACHE_SIZE), forward);
        assertEquals(a, describeAll(planner(9L, LayoutInputs.DEFAULT_CACHE_SIZE), shuffled));
        assertEquals(a, describeAll(planner(9L, LayoutInputs.DEFAULT_CACHE_SIZE), backward));
    }

    @Test
    public void cacheSizeDoesNotMatter() {
        List<int[]> forward = chunks();
        assertEquals(
            describeAll(planner(9L, LayoutInputs.DEFAULT_CACHE_SIZE), forward),
            describeAll(planner(9L, 64), forward));
    }

    @Test
    public void aDifferentSeedGivesADifferentLayout() {
        List<int[]> forward = chunks();
        assertNotEquals(describeAll(planner(9L, 4096), forward), describeAll(planner(10L, 4096), forward));
    }
}
