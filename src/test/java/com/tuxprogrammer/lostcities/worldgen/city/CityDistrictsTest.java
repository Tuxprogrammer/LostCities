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
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * City districts (phase 5c, D350): the central plaza, the industrial yards and the park belt, on grid-placed
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
                            plan.lotFootprint()
                                .width(),
                            "center " + c[0]
                                + ","
                                + c[1]
                                + " r="
                                + p.cityRadius(c[0], c[1])
                                + " chunk "
                                + dx
                                + ","
                                + dz);
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

    /**
     * D-plaza-road-1, the owner's report on seed 5253073417701929942: a highway along the center row of a big city
     * left only the south half of its 2 x 2 plaza, so the monument (centered on the footprint's middle corner) was cut
     * in half by the highway row's street and border wall a city level higher. A plaza is now whole or one chunk: all
     * four chunks of a 2 x 2 are plaza at one city level, never a highway chunk; otherwise at most one chunk of the
     * footprint is a one-chunk plaza.
     */
    @Test
    public void everyPlazaIsWholeAndOffTheHighways() {
        int[] seen = new int[3];
        for (long seed : new long[] { SEED, 11L, 12345L, 5253073417701929942L }) {
            LayoutPlanner p = planner(seed, "true");
            for (int[] c : centers(p)) {
                if (!p.centerYieldsCity(c[0], c[1])) {
                    continue;
                }
                boolean big = p.cityRadius(c[0], c[1]) >= 112;
                int span = big ? 1 : 0;
                int whole = 0;
                int single = 0;
                int level = p.plan(c[0], c[1])
                    .cityLevel();
                boolean blocked = false;
                for (int dx = 0; dx <= span; dx++) {
                    for (int dz = 0; dz <= span; dz++) {
                        int x = c[0] + dx;
                        int z = c[1] + dz;
                        if ((dx != 0 || dz != 0) && p.isCityCenter(x, z)) {
                            continue; // an adjacent city's own plaza
                        }
                        ChunkPlan plan = p.plan(x, z);
                        blocked |= p.hasHighway(x, z) || plan.cityLevel() != level;
                        if (plan.lot() != LotKind.PLAZA) {
                            continue;
                        }
                        String at = "seed " + seed + " center " + c[0] + "," + c[1] + " chunk " + dx + "," + dz;
                        assertFalse(p.hasHighway(x, z), "plaza on a highway: " + at);
                        if (plan.lotFootprint()
                            .isMulti()) {
                            assertEquals(level, plan.cityLevel(), "2 x 2 plaza across a level step: " + at);
                            whole++;
                        } else {
                            single++;
                        }
                    }
                }
                if (big) {
                    String at = "seed " + seed + " center " + c[0] + "," + c[1];
                    assertTrue(whole == 0 || whole >= 3 && single == 0, "a cut 2 x 2 plaza: " + at);
                    assertTrue(single <= 1, "two one-chunk plazas for one city: " + at);
                    if (blocked && whole == 0) {
                        seen[single]++;
                    }
                }
            }
        }
        System.out.println("districts: blocked 2x2 -> none=" + seen[0] + " single=" + seen[1]);
        assertTrue(seen[0] + seen[1] > 0, "the test worlds hold a big city whose 2 x 2 a highway or level step cuts");
    }

    /**
     * D-plaza-station-1: every link-network city center is a station, and the plaza is drawn at the center. On a plaza
     * chunk the station stays under the plaza without its staircase, whose surface part would break the basin.
     */
    @Test
    public void aStationUnderAPlazaHasNoStaircase() {
        int stations = 0;
        for (long seed : new long[] { SEED, 11L }) {
            LayoutPlanner p = planner(seed, "true");
            for (int[] c : centers(p)) {
                ChunkPlan plan = p.plan(c[0], c[1]);
                RailChunk rail = p.networks()
                    .railChunk(c[0], c[1]);
                if (plan.lot() != LotKind.PLAZA || rail.type() != RailChunkType.STATION_UNDERGROUND) {
                    continue;
                }
                stations++;
                assertFalse(NetworkWriter.stationStairs(rail, plan), "stairs on the plaza at " + c[0] + "," + c[1]);
            }
        }
        System.out.println("districts: plaza stations=" + stations);
        assertTrue(stations > 0, "some plaza sits over its city's station");
    }

    @Test
    public void withTheOptionOffNothingIsADistrict() {
        LayoutPlanner p = planner(SEED, "false");
        for (int x = -90; x <= 90; x++) {
            for (int z = -90; z <= 90; z++) {
                LotKind lot = p.plan(x, z)
                    .lot();
                assertTrue(lot != LotKind.PLAZA && lot != LotKind.INDUSTRY, x + "," + z);
            }
        }
    }
}
