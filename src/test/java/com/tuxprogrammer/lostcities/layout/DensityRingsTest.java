package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.DensityRings.BuildingClass;
import com.tuxprogrammer.lostcities.layout.DensityRings.Ring;

/** The density rings (D306): the curves in isolation, and over a test city. */
public class DensityRingsTest {

    private static final long SEED = 4242L;
    private static final int R = 70;

    @Test
    public void ringsFollowTheNormalizedDistance() {
        assertEquals(Ring.CORE, Ring.of(0.1f));
        assertEquals(Ring.INNER, Ring.of(0.3f));
        assertEquals(Ring.OUTER, Ring.of(0.6f));
        assertEquals(Ring.EDGE, Ring.of(0.9f));
        assertEquals(0f, DensityRings.distance(1f, 0.2f), 1e-6f);
        assertEquals(1f, DensityRings.distance(0.2f, 0.2f), 1e-6f);
    }

    @Test
    public void theFloorCapFallsFromTheCoreToTheEdge() {
        int previous = Integer.MAX_VALUE;
        for (float d = 0f; d <= 1f; d += 0.05f) {
            int cap = DensityRings.floorCap(d, 8, 1f);
            assertTrue(cap <= previous, "cap at " + d);
            previous = cap;
        }
        assertEquals(8, DensityRings.floorCap(0.1f, 8, 1f));
        assertEquals(1, DensityRings.floorCap(0.9f, 8, 1f));
    }

    @Test
    public void classWeightsPutTowersInTheCoreAndHousesAtTheEdge() {
        assertTrue(
            DensityRings.acceptance(BuildingClass.TOWER, Ring.CORE, 1f)
                > DensityRings.acceptance(BuildingClass.TOWER, Ring.EDGE, 1f));
        assertEquals(0f, DensityRings.acceptance(BuildingClass.TOWER, Ring.EDGE, 1f), 0f);
        assertEquals(BuildingClass.HOUSE, DensityRings.classOf("nh_cottage"));
        assertEquals(BuildingClass.TOWER, DensityRings.classOf("building8"));
    }

    @Test
    public void constructionLotsAreHigherInTheInnerAndOuterRings() {
        assertTrue(DensityRings.lotChance(0.3f, 1f) > DensityRings.lotChance(0.1f, 1f));
        assertTrue(DensityRings.lotChance(0.6f, 1f) > DensityRings.lotChance(0.9f, 1f));
        assertTrue(DensityRings.lotChance(0.1f, 1f) > 0f);
    }

    @Test
    public void meanFloorsFallFromTheCoreToTheEdgeAndTheOriginalsAppear() {
        LayoutPlanner planner = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE, "densityRings", "1");
        double[] sum = new double[4];
        int[] count = new int[4];
        int edgeLow = 0;
        int edgeTotal = 0;
        Set<String> originals = new HashSet<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = planner.plan(x, z);
                if (!plan.isCity() || !plan.hasBuilding() || plan.floors() <= 0) {
                    continue;
                }
                Ring ring = Ring.of(planner.ringDistance(x, z));
                sum[ring.ordinal()] += plan.floors();
                count[ring.ordinal()]++;
                boolean original = plan.building() != null && plan.building()
                    .startsWith("nh_");
                if (original) {
                    originals.add(plan.building());
                }
                if (ring == Ring.EDGE) {
                    edgeTotal++;
                    if (plan.floors() <= 2 || original) {
                        edgeLow++;
                    }
                }
            }
        }
        double previous = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            assertTrue(count[i] > 10, "ring " + i + " has " + count[i] + " buildings");
            double mean = sum[i] / count[i];
            assertTrue(mean < previous, "ring " + i + " mean " + mean + " not below " + previous);
            previous = mean;
        }
        assertTrue(edgeLow >= 0.8 * edgeTotal, "edge low-rise " + edgeLow + " of " + edgeTotal);
        assertEquals(DensityRings.ORIGINALS.length, originals.size(), "originals seen: " + originals);
    }

    @Test
    public void ringsOffLeavesThePlanOfThePreviousLayout() {
        LayoutPlanner off = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE, "densityRings", "0");
        LayoutPlanner legacy = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                ChunkPlan a = off.plan(x, z);
                ChunkPlan b = legacy.plan(x, z);
                assertEquals(b.hasBuilding(), a.hasBuilding());
                assertEquals(b.floors(), a.floors());
                assertEquals(b.building(), a.building());
                assertEquals(b.lot(), a.lot());
            }
        }
    }

    @Test
    public void aCityWithRingsOnMatchesItsGolden() throws Exception {
        LayoutPlanner p = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE, "densityRings", "1");
        LayoutGoldens.assertMatches(
            "density_rings",
            "The character fixture world (seed 4242) with densityRings 1: floors, building classes and lots by ring.",
            LayoutText.maps(p, -30, -30, 30, 30),
            LayoutText.details(p, -30, -30, 30, 30));
    }
}
