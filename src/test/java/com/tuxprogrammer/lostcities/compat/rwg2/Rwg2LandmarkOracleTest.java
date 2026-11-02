package com.tuxprogrammer.lostcities.compat.rwg2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The RWG2 terrain-landmark oracle over fake landforms: a volcano at (1000, 2000), a lava cave at (−3000, 500), a
 * junction peaking at (600, −600) beside a mountain chain, and one at (−1000, −1000) with no chain near it.
 */
class Rwg2LandmarkOracleTest {

    private static final int VOLCANO_X = 1000;
    private static final int VOLCANO_Z = 2000;
    private static final int CAVE_X = -3000;
    private static final int CAVE_Z = 500;
    private static final int JUNCTION_X = 600;
    private static final int JUNCTION_Z = -600;
    private static final int LONE_JUNCTION_X = -1000;
    private static final int LONE_JUNCTION_Z = -1000;
    /** Distance at which the fake junction strength reaches 0, blocks. */
    private static final double JUNCTION_FADE = 40D;

    /** The fake world. Strength falls 1/40 per block from each junction's peak, slower than RWG2's worst case. */
    static final class FakeLandforms implements Rwg2Landforms {

        int junctionCalls;

        @Override
        public long volcanoCenter(int x, int z) {
            return within(x, z, VOLCANO_X, VOLCANO_Z, Rwg2LandmarkOracle.VOLCANO_RADIUS)
                ? Rwg2Landforms.pack(VOLCANO_X, VOLCANO_Z)
                : NONE;
        }

        @Override
        public long lavaCaveCenter(int x, int z) {
            return within(x, z, CAVE_X, CAVE_Z, 320) ? Rwg2Landforms.pack(CAVE_X, CAVE_Z) : NONE;
        }

        @Override
        public float junctionStrength(int x, int z) {
            junctionCalls++;
            double best = Math.max(
                1D - distance(x, z, JUNCTION_X, JUNCTION_Z) / JUNCTION_FADE,
                1D - distance(x, z, LONE_JUNCTION_X, LONE_JUNCTION_Z) / JUNCTION_FADE);
            return (float) Math.max(0D, best);
        }

        @Override
        public boolean mountainChain(int x, int z) {
            return x >= 520 && x <= 700 && z >= -700 && z <= -500;
        }

        /** RWG2's own rule, without the margin. */
        boolean protects(int x, int z) {
            if (volcanoCenter(x, z) != NONE || within(x, z, CAVE_X, CAVE_Z, Rwg2LandmarkOracle.LAVA_CAVE_OPENING)) {
                return true;
            }
            if (junctionStrength(x, z) < Rwg2LandmarkOracle.JUNCTION_THRESHOLD) {
                return false;
            }
            for (int dx = -32; dx <= 32; dx += 16) {
                for (int dz = -32; dz <= 32; dz += 16) {
                    if (mountainChain(x + dx, z + dz)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static boolean within(int x, int z, int centerX, int centerZ, int radius) {
        return distance(x, z, centerX, centerZ) <= radius;
    }

    private static double distance(int x, int z, int centerX, int centerZ) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Rwg2LandmarkOracle oracle(FakeLandforms landforms, int cacheSize) {
        return new Rwg2LandmarkOracle(landforms, (x, z) -> 80, cacheSize, cacheSize);
    }

    private static TreeMap<String, Landmark> query(Rwg2LandmarkOracle oracle, int minX, int minZ, int maxX, int maxZ) {
        TreeMap<String, Landmark> out = new TreeMap<>();
        for (Landmark landmark : oracle.landmarksTouching(minX, minZ, maxX, maxZ)) {
            out.put(landmark.id(), landmark);
        }
        return out;
    }

    @Test
    void findsEachLandmarkByItsCenter() {
        Rwg2LandmarkOracle oracle = oracle(new FakeLandforms(), Rwg2LandmarkOracle.REGION_CACHE_SIZE);
        assertTrue(query(oracle, 60, 122, 64, 128).containsKey("volcano@1000,2000"));
        assertTrue(query(oracle, -190, 28, -185, 34).containsKey("lavacave@-3000,500"));
        TreeMap<String, Landmark> junction = query(oracle, 36, -40, 39, -36);
        assertEquals(1, junction.size(), junction.toString());
        Landmark found = junction.firstEntry()
            .getValue();
        assertEquals("junction@600,-600", found.id(), "the climb reaches the peak");
        assertEquals(LandmarkKind.TERRAIN, found.kind());
        assertEquals(Certainty.EXACT, found.certainty());
        assertEquals(80, found.groundEstimate());
        assertTrue(
            query(oracle, -66, -66, -60, -60).isEmpty(),
            "a junction with no mountain chain near it is not RWG2's landmark");
        assertTrue(query(oracle, 300, 300, 310, 310).isEmpty(), "open country");
    }

    @Test
    void footprintsCoverEveryProtectedColumnAndItsMargin() {
        FakeLandforms landforms = new FakeLandforms();
        Rwg2LandmarkOracle oracle = oracle(landforms, Rwg2LandmarkOracle.REGION_CACHE_SIZE);
        int[][] areas = { { VOLCANO_X, VOLCANO_Z, 180 }, { CAVE_X, CAVE_Z, 90 }, { JUNCTION_X, JUNCTION_Z, 50 } };
        int protectedColumns = 0;
        for (int[] area : areas) {
            for (int x = area[0] - area[2]; x <= area[0] + area[2]; x++) {
                for (int z = area[1] - area[2]; z <= area[1] + area[2]; z++) {
                    if (!landforms.protects(x, z)) {
                        continue;
                    }
                    protectedColumns++;
                    int m = Rwg2LandmarkOracle.RWG2_MARGIN;
                    assertTrue(oracle.covers(x, z), "uncovered " + x + "," + z);
                    assertTrue(oracle.covers(x - m, z - m) && oracle.covers(x + m, z + m), "margin at " + x + "," + z);
                    assertTrue(oracle.covers(x - m, z + m) && oracle.covers(x + m, z - m), "margin at " + x + "," + z);
                }
            }
        }
        assertTrue(protectedColumns > 1000);
    }

    @Test
    void answersDoNotDependOnQueryOrderOrCacheSize() {
        TreeMap<String, Landmark> whole = query(
            oracle(new FakeLandforms(), Rwg2LandmarkOracle.REGION_CACHE_SIZE),
            -200,
            -60,
            80,
            140);
        assertEquals(3, whole.size(), whole.keySet()
            .toString());
        Rwg2LandmarkOracle tiled = oracle(new FakeLandforms(), 1);
        TreeMap<String, Landmark> found = new TreeMap<>();
        for (int x = 72; x >= -200; x -= 8) {
            for (int z = 132; z >= -60; z -= 8) {
                found.putAll(query(tiled, x, z, x + 7, z + 7));
            }
        }
        assertEquals(whole.keySet(), found.keySet());
        for (String id : whole.keySet()) {
            assertEquals(
                whole.get(id)
                    .footprint(),
                found.get(id)
                    .footprint(),
                id);
        }
    }

    @Test
    void discRowsSpanTheDiscBandByBand() {
        List<BlockRect> rows = Rwg2LandmarkOracle.discRows(0, 0, 20);
        // The band Z −32..−17 holds Z −20..−17; at Z −17 the disc is 2 × 10 + 1 wide.
        assertEquals(new BlockRect(-10, -20, 10, -17), rows.get(0));
        int covered = 0;
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                if (x * x + z * z <= 400) {
                    covered++;
                    final int cx = x;
                    final int cz = z;
                    assertTrue(
                        rows.stream()
                            .anyMatch(r -> r.contains(cx, cz)),
                        cx + "," + cz);
                }
            }
        }
        assertTrue(covered > 1000);
    }

    @Test
    void probeDefersUncachedSampling() {
        FakeLandforms landforms = new FakeLandforms();
        HeightProbe probe = new HeightProbe();
        TerrainHeightOracle heights = new TerrainHeightOracle() {

            @Override
            public int height(int x, int z) {
                return 80;
            }

            @Override
            public HeightProbe probe() {
                return probe;
            }
        };
        Rwg2LandmarkOracle oracle = new Rwg2LandmarkOracle(landforms, heights, 4096, 512);
        probe.setActive(true);
        int fetches = 0;
        while (true) {
            try {
                oracle.landmarksTouching(36, -40, 39, -36);
                break;
            } catch (HeightProbe.NotCached e) {
                assertTrue(HeightProbe.isProbe(e));
                assertTrue(probe.fetchMissing());
                fetches++;
            }
        }
        assertTrue(fetches > 0, "the first probe misses");
        int calls = landforms.junctionCalls;
        assertFalse(
            oracle.landmarksTouching(36, -40, 39, -36)
                .isEmpty());
        assertEquals(calls, landforms.junctionCalls, "everything cached after the fetches");
        probe.setActive(false);
    }
}
