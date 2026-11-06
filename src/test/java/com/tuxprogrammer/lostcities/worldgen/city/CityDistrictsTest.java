package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.DensityRings;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * City districts (phase 5c, D-p5c-1): the central plaza, the industrial yards and the park belt, on grid-placed
 * cities with density rings; deterministic, and off for worlds pinned before the option.
 */
public class CityDistrictsTest {

    private static final long SEED = 7L;
    private static final int R = 220;

    private static LayoutPlanner planner(long seed, String districts, String... extra) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < extra.length; i += 2) {
            map.put(extra[i], extra[i + 1]);
        }
        map.put("cityPlacement", "grid");
        map.put("networkLayout", "links");
        map.put("densityRings", "1");
        map.put("cityDistricts", districts);
        return CityFixtures.plan(seed, TerrainSettings.fromValues(map), StructureOracle.NONE, LayoutLog.SILENT)
            .planner();
    }

    /** City centers in the window, as {chunkX, chunkZ}. */
    private static List<int[]> centers(LayoutPlanner p) {
        List<int[]> list = new ArrayList<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (p.isCityCenter(x, z)) {
                    list.add(new int[] { x, z });
                }
            }
        }
        assertTrue(!list.isEmpty(), "the test world has city centers");
        return list;
    }

    private static float ring(LayoutPlanner p, int x, int z) {
        return DensityRings.distance(p.cityFactor(x, z), p.profile().cityThreshold);
    }

    @Test
    public void theCenterChunkIsAPlazaAndNeverABuilding() {
        LayoutPlanner p = planner(SEED, "true");
        int plazas = 0;
        int cities = 0;
        for (int[] c : centers(p)) {
            if (!p.centerYieldsCity(c[0], c[1])) {
                continue;
            }
            cities++;
            ChunkPlan plan = p.plan(c[0], c[1]);
            assertFalse(plan.hasBuilding(), "no building at the center " + c[0] + "," + c[1]);
            if (plan.lot() == LotKind.PLAZA) {
                plazas++;
            }
        }
        System.out.println("districts: cities=" + cities + " plazas=" + plazas);
        assertTrue(cities > 0);
        assertTrue(plazas * 2 >= cities, "most centers are plazas: " + plazas + " of " + cities);
    }

    @Test
    public void bigCitiesGetATwoByTwoPlaza() {
        LayoutPlanner p = planner(SEED, "true");
        int cities = 0;
        int full = 0;
        for (int[] c : centers(p)) {
            if (!p.centerYieldsCity(c[0], c[1]) || p.cityRadius(c[0], c[1]) < 112) {
                continue;
            }
            cities++;
            int plazas = 0;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    ChunkPlan plan = p.plan(c[0] + dx, c[1] + dz);
                    if (plan.lot() == LotKind.PLAZA) {
                        if ((dx != 0 || dz != 0) && p.isCityCenter(c[0] + dx, c[1] + dz)) {
                            continue; // an adjacent city's own one-chunk plaza
                        }
                        assertEquals(
                            2,
                            plan.lotFootprint().width(),
                            "center " + c[0] + "," + c[1] + " r=" + p.cityRadius(c[0], c[1]) + " chunk " + dx + "," + dz);
                        assertFalse(plan.hasBuilding());
                        plazas++;
                    }
                }
            }
            full += plazas >= 3 ? 1 : 0;
        }
        assertTrue(cities > 0 && full * 2 >= cities, "2 x 2 plazas: " + full + " of " + cities);
    }

    @Test
    public void yardsLieInTheOuterOrEdgeRing() {
        LayoutPlanner p = planner(SEED, "true");
        int yards = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = p.plan(x, z);
                if (plan.lot() != LotKind.INDUSTRY) {
                    continue;
                }
                yards++;
                assertFalse(plan.hasBuilding());
                assertTrue(ring(p, x, z) >= DensityRings.INNER_END, "yard ring at " + x + "," + z);
            }
        }
        System.out.println("districts: yards=" + yards);
        assertTrue(yards > 0, "some city has an industrial yard");
    }

    @Test
    public void theBeltIsGreenAndOnlyInItsBand() {
        LayoutPlanner p = planner(SEED, "true");
        int band = 0;
        int parks = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (!p.isCityRaw(x, z)) {
                    continue;
                }
                float d = ring(p, x, z);
                if (d >= 0.38f && d <= 0.52f) {
                    band++;
                    ChunkPlan plan = p.plan(x, z);
                    if (!plan.hasBuilding() && plan.lot() == LotKind.NONE
                        && plan.streetType() == StreetType.PARK
                        && plan.parkPart() != null) {
                        parks++;
                    }
                }
            }
        }
        System.out.println("districts: band=" + band + " parks=" + parks);
        assertTrue(band > 0);
        double fraction = parks / (double) band;
        assertTrue(fraction > 0.2 && fraction < 0.8, "belt fraction " + fraction);
    }

    @Test
    public void districtsAreDeterministicWhateverTheQueryOrder() {
        LayoutPlanner a = planner(SEED, "true");
        LayoutPlanner b = planner(SEED, "true");
        int span = 80;
        String[][] seen = new String[2 * span + 1][2 * span + 1];
        for (int x = -span; x <= span; x++) {
            for (int z = -span; z <= span; z++) {
                ChunkPlan plan = a.plan(x, z);
                seen[x + span][z + span] = plan.lot() + "/" + plan.hasBuilding() + "/" + plan.streetType();
            }
        }
        for (int x = span; x >= -span; x--) {
            for (int z = span; z >= -span; z--) {
                ChunkPlan plan = b.plan(x, z);
                assertEquals(
                    seen[x + span][z + span],
                    plan.lot() + "/" + plan.hasBuilding() + "/" + plan.streetType(),
                    x + "," + z);
            }
        }
    }

    @Test
    public void withTheOptionOffNothingIsADistrict() {
        LayoutPlanner p = planner(SEED, "false");
        for (int x = -90; x <= 90; x++) {
            for (int z = -90; z <= 90; z++) {
                LotKind lot = p.plan(x, z).lot();
                assertTrue(lot != LotKind.PLAZA && lot != LotKind.INDUSTRY, x + "," + z);
            }
        }
    }
}
