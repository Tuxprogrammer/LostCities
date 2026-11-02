package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;

/** The monument oracle over fake biomes: deep ocean west of block X 0, a Kelp Forest strip, land elsewhere. */
class MonumentOracleTest {

    private static final long SEED = 777L;

    /** Deep ocean for X below −160, except a Kelp Forest band at Z 1000..1100; plains elsewhere. */
    private static final BiomeNames BIOMES = (x, z) -> {
        if (x >= -160) {
            return "Plains";
        }
        return z >= 1000 && z <= 1100 ? "Kelp Forest" : "Deep Ocean";
    };

    private static MonumentOracle oracle(boolean enabled, int cacheSize) {
        return new MonumentOracle(SEED, enabled, BIOMES, (x, z) -> 40, cacheSize);
    }

    @Test
    void monumentsOnlyInDeepOcean() {
        List<Landmark> found = oracle(true, MonumentOracle.CACHE_SIZE).landmarksTouching(-400, -400, 400, 400);
        assertFalse(found.isEmpty());
        for (Landmark landmark : found) {
            assertEquals(LandmarkKind.OCEAN_MONUMENT, landmark.kind());
            assertEquals(Certainty.EXACT, landmark.certainty());
            assertEquals(40, landmark.groundEstimate());
            assertEquals(
                1,
                landmark.footprint()
                    .size());
            BlockRect box = landmark.footprint()
                .get(0);
            // The 15 x 15 test square around the center lies wholly in the ocean.
            assertTrue(box.minX() + MonumentGrid.HALF + MonumentGrid.WATER_RADIUS < -160, landmark.id());
            assertTrue(
                landmark.gates()
                    .isEmpty());
        }
    }

    @Test
    void kelpForestHostsNone() {
        MonumentOracle oracle = oracle(true, MonumentOracle.CACHE_SIZE);
        for (Landmark landmark : oracle.landmarksTouching(-2000, 0, -11, 120)) {
            BlockRect box = landmark.footprint()
                .get(0);
            int centerZ = box.minZ() + MonumentGrid.HALF;
            assertTrue(centerZ + 7 < 1000 || centerZ - 7 > 1100, landmark.id() + " in the kelp band");
        }
    }

    @Test
    void onlyStartChunksAreMonuments() {
        MonumentOracle oracle = oracle(true, MonumentOracle.CACHE_SIZE);
        MonumentGrid grid = oracle.grid();
        int[] start = grid.startChunk(-5, 0);
        assertNull(oracle.monumentAt(start[0] + 1, start[1]));
        assertEquals(
            MonumentOracle.id(start[0], start[1]),
            oracle.monumentAt(start[0], start[1])
                .id());
    }

    @Test
    void disabledMeansEmpty() {
        MonumentOracle oracle = oracle(false, MonumentOracle.CACHE_SIZE);
        assertTrue(
            oracle.landmarksTouching(-400, -400, 400, 400)
                .isEmpty());
        int[] start = oracle.grid()
            .startChunk(-5, 0);
        assertNull(oracle.monumentAt(start[0], start[1]));
    }

    @Test
    void answersDependOnNeitherQueryOrderNorCache() {
        TreeSet<String> whole = new TreeSet<>();
        for (Landmark landmark : oracle(true, MonumentOracle.CACHE_SIZE).landmarksTouching(-300, -100, -1, 99)) {
            whole.add(VillageOracleTest.describe(landmark));
        }
        TreeSet<String> tiled = new TreeSet<>();
        MonumentOracle small = oracle(true, 1);
        List<int[]> tiles = new ArrayList<>();
        for (int x = -300; x < 0; x += 10) {
            for (int z = -100; z < 100; z += 10) {
                tiles.add(new int[] { x, z });
            }
        }
        Collections.reverse(tiles);
        for (int[] t : tiles) {
            for (Landmark landmark : small.landmarksTouching(t[0], t[1], t[0] + 9, t[1] + 9)) {
                tiled.add(VillageOracleTest.describe(landmark));
            }
        }
        assertFalse(whole.isEmpty());
        assertEquals(whole, tiled);
    }
}
