package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * Decision log D30 (3) asks M03 to measure how far the nearest city is from spawn over many seeds, to decide whether
 * a near-spawn city option is needed. With the default profile and land within the height window, the nearest city
 * chunk lies within a short walk of block (0, 0) for every seed tried (decision log D-M03-9), so no option is added.
 * This test keeps that true.
 */
public class NearSpawnCityTest {

    /** Seeds measured. */
    private static final int SEEDS = 100;
    /** Chunks searched outward from the origin. */
    private static final int SEARCH = 24;

    @Test
    public void aCityIsAShortWalkFromSpawnOnEverySeed() {
        List<Double> distances = new ArrayList<>();
        for (long seed = 0; seed < SEEDS; seed++) {
            LayoutPlanner p = LayoutFixtures
                .planner(seed * 7919L, LayoutProfile.defaults(), LayoutFixtures.flatTerrain(), StructureOracle.NONE);
            distances.add(nearestCity(p));
        }
        Collections.sort(distances);
        double median = distances.get(SEEDS / 2);
        double worst = distances.get(SEEDS - 1);
        assertTrue(median < 128, "median distance to the nearest city chunk " + median + " blocks");
        assertTrue(worst < 384, "worst distance " + worst + " blocks");
    }

    /** The distance in blocks from (0, 0) to the center of the nearest raw-city chunk, searched ring by ring. */
    private static double nearestCity(LayoutPlanner p) {
        double best = Double.MAX_VALUE;
        for (int ring = 0; ring <= SEARCH; ring++) {
            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) == ring && p.isCityRaw(x, z)) {
                        best = Math.min(best, Math.hypot(x * 16 + 8, z * 16 + 8));
                    }
                }
            }
            if (best <= ring * 16) {
                return best;
            }
        }
        return best;
    }
}
