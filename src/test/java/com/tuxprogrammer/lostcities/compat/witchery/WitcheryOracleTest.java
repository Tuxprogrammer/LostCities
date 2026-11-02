package com.tuxprogrammer.lostcities.compat.witchery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;

/** The Witchery oracle over fake biomes: Witchery may build east of block X 0 and nowhere west of it. */
class WitcheryOracleTest {

    private static final long SEED = 99L;
    private static final WitcheryBiomes EAST_ONLY = (x, z) -> x >= 0;

    private static WitcheryOracle oracle(WitcherySettings settings, int cacheSize) {
        return new WitcheryOracle(SEED, settings, EAST_ONLY, (x, z) -> 70, cacheSize);
    }

    @Test
    void lotsStandOnAcceptedCandidatesOnly() {
        WitcheryOracle oracle = oracle(WitcherySettings.pack(), WitcheryOracle.CACHE_SIZE);
        List<Landmark> found = oracle.landmarksTouching(-200, -200, 200, 200);
        assertFalse(found.isEmpty());
        for (Landmark landmark : found) {
            assertEquals(LandmarkKind.WITCHERY, landmark.kind());
            assertEquals(Certainty.UNION, landmark.certainty());
            assertEquals(70, landmark.groundEstimate());
            String[] chunk = landmark.id()
                .substring("witchery@".length())
                .split(",");
            int chunkX = Integer.parseInt(chunk[0]);
            int chunkZ = Integer.parseInt(chunk[1]);
            assertTrue(
                oracle.grid()
                    .isCandidate(chunkX, chunkZ),
                landmark.id());
            assertTrue(chunkX * 16 + WitcheryOracle.BIOME_OFFSET >= 0, "biome rejected west of X 0: " + landmark.id());
            BlockRect lot = landmark.footprint()
                .get(0);
            assertEquals(new BlockRect(chunkX * 16 - 9, chunkZ * 16 - 9, chunkX * 16 + 13, chunkZ * 16 + 13), lot);
        }
        // Every accepted candidate in the area is found.
        WitcheryGrid grid = oracle.grid();
        int expected = 0;
        for (int cellX = grid.cellOf(-200); cellX <= grid.cellOf(200); cellX++) {
            for (int cellZ = grid.cellOf(-200); cellZ <= grid.cellOf(200); cellZ++) {
                int[] candidate = grid.candidateChunk(cellX, cellZ);
                if (candidate[0] * 16 + 5 >= 0 && candidate[0] >= -200
                    && candidate[0] <= 200
                    && candidate[1] >= -200
                    && candidate[1] <= 200) {
                    expected++;
                    assertTrue(
                        found.stream()
                            .anyMatch(
                                l -> l.id()
                                    .equals(WitcheryOracle.id(candidate[0], candidate[1]))),
                        "missing " + candidate[0] + "," + candidate[1]);
                }
            }
        }
        assertTrue(expected > 0);
    }

    @Test
    void answersDoNotDependOnQueryOrderOrCacheSize() {
        WitcheryOracle whole = oracle(WitcherySettings.pack(), WitcheryOracle.CACHE_SIZE);
        TreeMap<String, Landmark> expected = new TreeMap<>();
        for (Landmark landmark : whole.landmarksTouching(-100, -100, 99, 99)) {
            expected.put(landmark.id(), landmark);
        }
        WitcheryOracle tiled = oracle(WitcherySettings.pack(), 1);
        TreeMap<String, Landmark> found = new TreeMap<>();
        for (int tileX = 96; tileX >= -100; tileX -= 4) {
            for (int tileZ = 96; tileZ >= -100; tileZ -= 4) {
                for (Landmark landmark : tiled.landmarksTouching(tileX, tileZ, tileX + 3, tileZ + 3)) {
                    found.put(landmark.id(), landmark);
                }
            }
        }
        assertEquals(expected.keySet(), found.keySet());
        for (String id : expected.keySet()) {
            assertEquals(
                expected.get(id)
                    .footprint(),
                found.get(id)
                    .footprint());
        }
    }

    @Test
    void lotReachesIntoTheWestAndNorthNeighbors() {
        WitcheryOracle oracle = oracle(WitcherySettings.pack(), WitcheryOracle.CACHE_SIZE);
        List<Landmark> found = oracle.landmarksTouching(0, 0, 400, 400);
        assertFalse(found.isEmpty());
        Landmark first = found.get(0);
        BlockRect lot = first.footprint()
            .get(0);
        // A query of only the north-west neighbor chunk still finds the lot.
        List<Landmark> corner = oracle
            .landmarksTouching((lot.minX() >> 4), (lot.minZ() >> 4), (lot.minX() >> 4), (lot.minZ() >> 4));
        assertTrue(
            corner.stream()
                .anyMatch(
                    l -> l.id()
                        .equals(first.id())));
    }

    @Test
    void nothingWhenEveryTypeIsOff() {
        WitcheryOracle off = oracle(new WitcherySettings(Collections.emptyList(), 20, true), 16);
        assertTrue(
            off.landmarksTouching(-300, -300, 300, 300)
                .isEmpty());
        assertNull(off.structureAt(0, 0));
    }

    @Test
    void nonCandidatesHoldNoLot() {
        WitcheryOracle oracle = oracle(WitcherySettings.pack(), 16);
        List<String> checked = new ArrayList<>();
        for (int x = 0; x < 60; x++) {
            if (!oracle.grid()
                .isCandidate(x, 7)) {
                assertNull(oracle.structureAt(x, 7));
                checked.add(x + ",7");
            }
        }
        assertFalse(checked.isEmpty());
    }
}
