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
import com.tuxprogrammer.lostcities.layout.MultiPos;
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
                        if (!plan.lotFootprint()
                            .isMulti()) {
                            continue; // D-plaza-road-1: the 2 x 2 does not fit, a one-chunk plaza instead
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
     * in half by the highway row's street and border wall a city level higher. Now every 2 x 2 plaza chunk has its
     * three partners, all plaza with the same footprint origin and city level, and no plaza chunk carries a highway;
     * a big city whose 2 x 2 does not fit gets a one-chunk plaza (or none).
     */
    @Test
    public void everyPlazaIsWholeAndOffTheHighways() {
        int fallbacks = 0;
        for (long seed : new long[] { SEED, 11L, 12345L, 5253073417701929942L }) {
            LayoutPlanner p = planner(seed, "true");
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    ChunkPlan plan = p.plan(x, z);
                    if (plan.lot() != LotKind.PLAZA) {
                        continue;
                    }
                    String at = "seed " + seed + " chunk " + x + "," + z;
                    assertFalse(p.hasHighway(x, z), "plaza on a highway: " + at);
                    MultiPos pos = plan.lotFootprint();
                    if (!pos.isMulti()) {
                        if (!p.isCityCenter(x, z) || p.cityRadius(x, z) >= 112) {
                            fallbacks++;
                        }
                        continue;
                    }
                    int ox = x - pos.x();
                    int oz = z - pos.z();
                    for (int dx = 0; dx <= 1; dx++) {
                        for (int dz = 0; dz <= 1; dz++) {
                            ChunkPlan other = p.plan(ox + dx, oz + dz);
                            MultiPos op = other.lotFootprint();
                            assertTrue(
                                other.lot() == LotKind.PLAZA && op.isMulti()
                                    && ox + dx - op.x() == ox
                                    && oz + dz - op.z() == oz,
                                "a cut 2 x 2 plaza: " + at + " partner " + dx + "," + dz);
                            assertEquals(plan.cityLevel(), other.cityLevel(), "2 x 2 plaza across a level step: " + at);
                        }
                    }
                }
            }
        }
        System.out.println("districts: one-chunk fallbacks for big cities=" + fallbacks);
        assertTrue(fallbacks > 0, "the test worlds hold a big city whose 2 x 2 a highway or level step cuts");
    }

    /**
     * D-plaza-station-1: every link-network city center is a station, and the plaza is drawn at the center. On a plaza
     * chunk the station stays under the plaza without its staircase, whose surface part would break the basin.
     */
    @Test
    public void aStationUnderAPlazaHasNoStaircase() {
        LayoutPlanner p = planner(SEED, "true");
        RailChunk station = RailChunk.link(RailChunkType.STATION_UNDERGROUND, 0, true, 0);
        int plazas = 0;
        int streets = 0;
        for (int[] c : centers(p)) {
            ChunkPlan plan = p.plan(c[0], c[1]);
            if (plan.lot() == LotKind.PLAZA) {
                plazas++;
                assertFalse(NetworkWriter.stationStairs(station, plan), "stairs on the plaza at " + c[0] + "," + c[1]);
            } else if (plan.lot() == LotKind.NONE && !plan.hasBuilding()) {
                streets++;
                assertTrue(
                    NetworkWriter.stationStairs(station, plan),
                    "no stairs off the plaza at " + c[0] + "," + c[1]);
            }
        }
        assertTrue(plazas > 0, "some center is a plaza");
        System.out.println("districts: plaza centers=" + plazas + " street centers=" + streets);
    }

    private static NetworkWriter.StairSite site(RailChunk rail, boolean plaza, boolean building, boolean highway,
        int level) {
        return new NetworkWriter.StairSite(rail, plaza, building, highway, true, level);
    }

    /** D-plaza-station-1: the staircase of a plaza station moves to exactly one qualifying neighbor, else none. */
    @Test
    public void aPlazaStationsStaircaseMovesToOneQualifyingNeighbor() {
        int both = com.tuxprogrammer.lostcities.layout.link.LinkRails.EAST
            | com.tuxprogrammer.lostcities.layout.link.LinkRails.WEST;
        RailChunk station = RailChunk.link(RailChunkType.STATION_UNDERGROUND, 0, true, both);
        NetworkWriter.StairSite self = site(station, true, false, false, 2);
        RailChunk tunnel = RailChunk.link(RailChunkType.HORIZONTAL, 0, false, both);
        RailChunk bend = RailChunk.link(
            RailChunkType.DOUBLE_BEND,
            0,
            false,
            com.tuxprogrammer.lostcities.layout.link.LinkRails.WEST
                | com.tuxprogrammer.lostcities.layout.link.LinkRails.SOUTH);
        NetworkWriter.StairSite good = site(tunnel, false, false, false, 2);
        // A qualifying neighbor on both sides: exactly one carries it (east first).
        assertEquals(1, NetworkWriter.stairCarrier(station, self, dx -> good));
        // East has a building, west qualifies.
        assertEquals(
            -1,
            NetworkWriter.stairCarrier(station, self, dx -> dx > 0 ? site(tunnel, false, true, false, 2) : good));
        // Neither qualifies: a building, a highway, a plaza, another city level, a bend, other rail level.
        assertEquals(0, NetworkWriter.stairCarrier(station, self, dx -> site(tunnel, false, true, false, 2)));
        assertEquals(0, NetworkWriter.stairCarrier(station, self, dx -> site(tunnel, false, false, true, 2)));
        assertEquals(0, NetworkWriter.stairCarrier(station, self, dx -> site(tunnel, true, false, false, 2)));
        assertEquals(0, NetworkWriter.stairCarrier(station, self, dx -> site(tunnel, false, false, false, 3)));
        assertEquals(0, NetworkWriter.stairCarrier(station, self, dx -> site(bend, false, false, false, 2)));
        assertEquals(
            0,
            NetworkWriter.stairCarrier(
                station,
                self,
                dx -> site(RailChunk.link(RailChunkType.HORIZONTAL, 1, false, both), false, false, false, 2)));
        // A station off the plaza never moves its staircase.
        assertEquals(0, NetworkWriter.stairCarrier(station, site(station, false, false, false, 2), dx -> good));
        // An underground extension is preferred to a tunnel, on either side.
        RailChunk extension = new RailChunk(RailChunkType.STATION_EXTENSION_UNDERGROUND, RailChunk.Direction.BI, 0, 1);
        RailChunk legacy = new RailChunk(RailChunkType.STATION_UNDERGROUND, RailChunk.Direction.BI, 0, 1);
        NetworkWriter.StairSite ext = site(extension, false, false, false, 2);
        assertEquals(
            -1,
            NetworkWriter.stairCarrier(legacy, site(legacy, true, false, false, 2), dx -> dx > 0 ? good : ext));
        assertEquals(-1, NetworkWriter.stairCarrier(station, self, dx -> dx > 0 ? good : ext));
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
