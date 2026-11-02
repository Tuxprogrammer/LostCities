package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Urban intensity, tapered heights and edge lots (alpha review AR-5 and AR-6,
 * docs/design/0008-city-edges-and-character.md §1 to §3).
 */
public class UrbanityTest {

    private static final long SEED = 4242L;
    private static final int R = 40;

    @Test
    public void theFloorCapRisesFromTheEdgeToTheCore() {
        assertEquals(2, Urbanity.floorCap(0f, 8, 1, 1f));
        assertEquals(4, Urbanity.floorCap(0.3f, 8, 1, 1f));
        assertEquals(8, Urbanity.floorCap(1f, 8, 1, 1f));
        assertEquals(8, Urbanity.floorCap(0f, 8, 1, 0f), "no taper: upstream's cap");
        assertEquals(5, Urbanity.floorCap(0f, 19, 5, 1f), "a profile's minimum floors stay");
    }

    @Test
    public void theCoreChanceIsZeroOutsideTheCore() {
        assertEquals(0f, Urbanity.coreChance(0.6f, 1f), 0f);
        assertEquals(Urbanity.CORE_CHANCE, Urbanity.coreChance(1f, 1f), 1e-6f);
        assertEquals(0f, Urbanity.coreChance(1f, 0f), 0f);
    }

    @Test
    public void theCoreTermIsTheRootOfTheFactorAboveTheThreshold() {
        assertEquals(0f, Urbanity.core(0.2f, 0.2f), 1e-6f);
        assertEquals(0.5f, Urbanity.core(0.4f, 0.2f), 1e-6f);
        assertEquals(1f, Urbanity.core(1f, 0.2f), 1e-6f);
    }

    @Test
    public void buildingsBesideTheEdgeStayLowAndTheCoreRises() {
        LayoutPlanner planner = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        int edgeMax = 0;
        double lowSum = 0;
        int lowCount = 0;
        double highSum = 0;
        int highCount = 0;
        int coreMax = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = planner.plan(x, z);
                if (!plan.hasBuilding() || !plan.multiPos()
                    .isSingle()) {
                    continue;
                }
                float intensity = planner.intensity(x, z);
                if (planner.urbanity.edgeDistance(x, z) <= 1f) {
                    edgeMax = Math.max(edgeMax, plan.floors());
                }
                if (intensity < 0.35f) {
                    lowSum += plan.floors();
                    lowCount++;
                } else if (intensity > 0.7f) {
                    highSum += plan.floors();
                    highCount++;
                    coreMax = Math.max(coreMax, plan.floors());
                }
            }
        }
        assertTrue(lowCount > 0 && highCount > 0, "low " + lowCount + ", high " + highCount);
        assertTrue(edgeMax <= 4, "beside the edge at most 4 floors: " + edgeMax);
        assertTrue(
            highSum / highCount > lowSum / lowCount + 1.5,
            "mean floors: core " + highSum / highCount + ", edge " + lowSum / lowCount);
        assertTrue(coreMax >= 7, "the core has high-rises: " + coreMax);
    }

    @Test
    public void heightsAreNotRings() {
        LayoutPlanner planner = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        // Chunks deep inside a city, with the same edge distance and city factor, still differ by the noise.
        List<Float> deep = new ArrayList<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (planner.urbanity.edgeDistance(x, z) > Urbanity.EDGE_WINDOW) {
                    deep.add(planner.intensity(x, z) - planner.urbanity.noise(x, z) * Urbanity.NOISE_AMPLITUDE);
                }
            }
        }
        assertTrue(deep.size() > 20, "deep chunks: " + deep.size());
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                float noise = planner.urbanity.noise(x, z);
                min = Math.min(min, noise);
                max = Math.max(max, noise);
            }
        }
        assertTrue(max - min > 0.8f, "the noise spans districts: " + min + " to " + max);
    }

    @Test
    public void edgeLotsAppearOnlyWhereTheIntensityIsLow() {
        LayoutPlanner planner = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        int lots = 0;
        int dug = 0;
        int foundations = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = planner.plan(x, z);
                if (plan.lot() == LotKind.NONE) {
                    continue;
                }
                lots++;
                assertTrue(plan.isCity() && !plan.hasBuilding(), "a lot is an open city chunk at " + x + "," + z);
                assertTrue(plan.intensity() < LayoutPlanner.LOT_INTENSITY, "intensity " + plan.intensity());
                assertTrue(!plan.isPlannedRoad(), "a lot is not a road");
                if (plan.lot()
                    .isDug()) {
                    dug++;
                    assertTrue(
                        plan.lotDepth() == 1 || plan.lotDepth() == 2 || plan.lotDepth() == 4,
                        "depth " + plan.lotDepth());
                }
                if (plan.lot() == LotKind.FOUNDATION) {
                    foundations++;
                    assertTrue(plan.lotColumnFloors() >= 1 && plan.lotColumnFloors() <= 3);
                }
            }
        }
        assertTrue(lots >= 5, "lots: " + lots);
        assertTrue(dug > 0 && foundations > 0, "dug " + dug + ", foundations " + foundations);
    }

    @Test
    public void noLotsWhenTheChanceIsZero() {
        LayoutPlanner planner = CharacterFixtures
            .planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE, "edgeLotChance", "0");
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                assertEquals(
                    LotKind.NONE,
                    planner.plan(x, z)
                        .lot());
            }
        }
    }

    @Test
    public void aDugLotsDepthFollowsItsRoll() {
        assertEquals(1, LayoutPlanner.lotDepth(0.1f));
        assertEquals(2, LayoutPlanner.lotDepth(0.5f));
        assertEquals(4, LayoutPlanner.lotDepth(0.9f));
    }
}
