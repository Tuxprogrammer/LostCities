package com.tuxprogrammer.lostcities.worldgen.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/** The synthetic test city's plan. */
public class SyntheticCityPlanTest {

    static SyntheticCityPlan plan(long seed) {
        TerrainSettings settings = TerrainSettings.defaults();
        return new SyntheticCityPlan(
            seed,
            settings,
            new ChunkHeights(TestTerrain.oracle(), settings.heightSampleSize(), ChunkHeights.Listener.NONE));
    }

    @Test
    public void citiesRepeatEverySixtyFourChunks() {
        SyntheticCityPlan plan = plan(3L);
        assertTrue(
            plan.chunk(0, 1)
                .isCity());
        assertTrue(
            plan.chunk(64, 1)
                .isCity());
        assertFalse(
            plan.chunk(32, 1)
                .isCity(),
            "between cities");
        assertEquals(0, SyntheticCityPlan.centerOf(31));
        assertEquals(64, SyntheticCityPlan.centerOf(32));
        assertEquals(2048, SyntheticCityPlan.centerOf(2050));
    }

    @Test
    public void streetsEveryThirdRowWithBuildingsBetween() {
        SyntheticCityPlan plan = plan(3L);
        ChunkPlan street = plan.chunk(0, 1);
        assertFalse(street.hasBuilding());
        assertTrue(street.hasFootprint(), "a street is in the footprint");
        int buildings = 0;
        for (int dx = 1; dx <= 5; dx++) {
            for (int dz = 1; dz <= 2; dz++) {
                ChunkPlan lot = plan.chunk(dx == 3 ? 4 : dx, dz);
                if (lot.hasBuilding()) {
                    buildings++;
                    assertTrue(lot.floors() >= 1 && lot.floors() <= SyntheticCityPlan.MAX_FLOORS);
                    assertTrue(lot.buildingBottom() >= WorldLimits.BUILDING_BOTTOM_MIN);
                    assertEquals(lot.buildingBottom() - 1, lot.footprintBottom(8, 8));
                }
            }
        }
        assertTrue(buildings > 0);
    }

    @Test
    public void fakeLandmarksHaveCoresEdgeRingsAndALane() {
        SyntheticCityPlan plan = plan(3L);
        ChunkPlan core = plan.chunk(4, -5);
        assertEquals(ChunkKind.LANDMARK_CORE, core.kind());
        assertEquals(core.naturalHeight(), core.groundY(), "a core's ground is its natural height");
        assertFalse(core.hasFootprint(), "cores are outside the footprint");
        ChunkPlan ring = plan.chunk(6, -4);
        assertTrue(ring.isCity());
        assertFalse(ring.hasBuilding(), "no building in the edge ring");
        assertNotNull(
            plan.chunk(4, -4)
                .lane(),
            "the lane crosses the core");
        assertNotNull(
            plan.chunk(3, -4)
                .lane(),
            "and the strip of the street facing it");
        assertNull(
            plan.chunk(5, -4)
                .lane(),
            "only to the core's center");
        assertEquals(
            ChunkKind.LANDMARK_CORE,
            plan.chunk(-7, 0)
                .kind());
        assertEquals(
            ChunkKind.LANDMARK_CORE,
            plan.chunk(-6, -1)
                .kind());
    }

    @Test
    public void thePlanIsPure() {
        SyntheticCityPlan first = plan(9L);
        SyntheticCityPlan second = plan(9L);
        for (int cx = -8; cx <= 8; cx++) {
            for (int cz = 8; cz >= -8; cz--) {
                assertEquals(
                    first.chunk(cx, cz)
                        .toString(),
                    second.chunk(cx, cz)
                        .toString());
            }
        }
    }
}
