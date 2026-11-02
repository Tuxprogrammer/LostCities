package com.tuxprogrammer.lostcities.compat.ae2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutFixtures;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;

/**
 * The meteorite oracle: one landmark per cell whose candidate falls, complete whatever rectangle is asked, and, in a
 * real layout, a crater basin no city block reaches into.
 */
class MeteoriteOracleTest {

    private static final long SEED = 4242L;
    private static final MeteoriteSettings PACK = MeteoriteSettings
        .of(true, 0, new String[] { "0=707" }, new String[] { "0=0.3" });

    private static MeteoriteOracle oracle(int cacheSize) {
        return new MeteoriteOracle(SEED, PACK, (x, z) -> 70, cacheSize);
    }

    @Test
    void aProbeReadsABasinInStepsAndGetsTheOnePassLandmark() {
        com.tuxprogrammer.lostcities.worldgen.height.HeightProbe probe = new com.tuxprogrammer.lostcities.worldgen.height.HeightProbe();
        com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle rough = (x, z) -> 64
            + Math.floorMod(x * 13 + z * 7, 11);
        int[] calls = { 0 };
        com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle heights = new com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle() {

            @Override
            public int height(int blockX, int blockZ) {
                calls[0]++;
                return rough.height(blockX, blockZ);
            }

            @Override
            public com.tuxprogrammer.lostcities.worldgen.height.HeightProbe probe() {
                return probe;
            }
        };
        Landmark whole = new MeteoriteOracle(SEED, PACK, rough, 16).meteoriteAt(0, 0);
        MeteoriteOracle stepped = new MeteoriteOracle(SEED, PACK, heights, 16);
        probe.setActive(true);
        Landmark found = null;
        int steps = 0;
        while (found == null && steps < 100) {
            try {
                found = stepped.meteoriteAt(0, 0);
            } catch (com.tuxprogrammer.lostcities.worldgen.height.HeightProbe.NotCached missing) {
                int before = calls[0];
                assertTrue(probe.fetchMissing());
                steps++;
                assertTrue(calls[0] > before, "a step reads samples");
            }
        }
        probe.setActive(false);
        assertTrue(steps >= 1, "nothing is read inside a probe");
        assertEquals(whole.footprint(), found.footprint());
        assertEquals(whole.groundEstimate(), found.groundEstimate());
        assertEquals(
            whole.heightCap()
                .centerY(),
            found.heightCap()
                .centerY());
    }

    @Test
    void oneLandmarkPerCell() {
        MeteoriteOracle oracle = oracle(MeteoriteOracle.CACHE_SIZE);
        Landmark landmark = oracle.meteoriteAt(0, 0);
        assertNotNull(landmark);
        assertEquals("meteorite@286,473", landmark.id());
        assertEquals(LandmarkKind.METEORITE, landmark.kind());
        assertEquals(Certainty.EXACT, landmark.certainty());
        assertEquals(70, landmark.groundEstimate());
        BlockRect box = landmark.footprint()
            .get(0);
        assertTrue(box.contains(286, 473));
        assertEquals(286 - box.minX(), box.maxX() - 286, "the footprint is centered on the impact");
        assertTrue(box.maxX() - 286 >= CraterBasin.DECAY_HALF_WIDTH);
        assertNotNull(landmark.heightCap());
        assertEquals(
            286,
            landmark.heightCap()
                .centerX());
        assertEquals(
            473,
            landmark.heightCap()
                .centerZ());
        // Flat ground at 70, size 5.91: the lowest paraboloid is at Y 54.
        assertEquals(
            54,
            landmark.heightCap()
                .centerY());
    }

    @Test
    void completeWhateverRectangleIsAsked() {
        MeteoriteOracle whole = oracle(MeteoriteOracle.CACHE_SIZE);
        TreeSet<String> all = ids(whole.landmarksTouching(-200, -200, 200, 200));
        // 6,400 blocks a side over 707-block cells: about 82 cells, each with a meteorite that falls.
        assertTrue(all.size() >= 64, "meteorites in a 401-chunk square: " + all.size());
        MeteoriteOracle tiled = oracle(1);
        TreeSet<String> tiles = new TreeSet<>();
        // Tiles in reverse order, with a one-cell cache: the answer must not depend on what was asked before.
        for (int x = 200; x >= -200; x -= 37) {
            for (int z = 200; z >= -200; z -= 41) {
                tiles.addAll(ids(tiled.landmarksTouching(Math.max(-200, x - 36), Math.max(-200, z - 40), x, z)));
            }
        }
        assertEquals(all, tiles);
    }

    @Test
    void touchingMeansTheFootprintOverlaps() {
        MeteoriteOracle oracle = oracle(MeteoriteOracle.CACHE_SIZE);
        Landmark landmark = oracle.meteoriteAt(0, 0);
        BlockRect box = landmark.footprint()
            .get(0);
        int chunkX = box.maxChunkX();
        int chunkZ = box.maxChunkZ();
        assertEquals(
            1,
            oracle.landmarksTouching(chunkX, chunkZ, chunkX, chunkZ)
                .size());
        assertTrue(
            oracle.landmarksTouching(chunkX + 1, chunkZ + 1, chunkX + 2, chunkZ + 2)
                .isEmpty());
    }

    /**
     * The crater basin end to end: a meteorite in the middle of a big city on flat ground. No city chunk lies in the
     * decay square, no building's top block and no street's headroom rises above the paraboloid over its chunk, and
     * the chunks right around the impact are core.
     */
    @Test
    void theCityStandsUnderTheCrater() {
        MeteoriteOracle oracle = oracle(MeteoriteOracle.CACHE_SIZE);
        Landmark meteor = oracle.meteoriteAt(0, 0);
        int impactX = meteor.heightCap()
            .centerX();
        int impactZ = meteor.heightCap()
            .centerZ();
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    105L,
                    LayoutFixtures.predefinedOnly(),
                    LayoutFixtures.standardAssets(),
                    LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(impactX >> 4, impactZ >> 4, 480, "citystyle_common"))
                .structures(oracle)
                .build());
        int buildings = 0;
        int capped = 0;
        for (int chunkX = (impactX >> 4) - 14; chunkX <= (impactX >> 4) + 14; chunkX++) {
            for (int chunkZ = (impactZ >> 4) - 14; chunkZ <= (impactZ >> 4) + 14; chunkZ++) {
                ChunkPlan plan = planner.plan(chunkX, chunkZ);
                ZoneInfo zone = plan.zone();
                if (contains(chunkX, chunkZ, impactX, impactZ)) {
                    assertEquals(Zone.CORE, zone.zone(), "the impact's chunk");
                }
                if (!plan.isCity()) {
                    continue;
                }
                String where = "chunk " + chunkX + "," + chunkZ + " " + zone;
                if (!zone.inCraterBasin()) {
                    // Beyond the crater pass's 200 blocks.
                    continue;
                }
                assertTrue(
                    plan.cityGroundLevel() + CraterBasin.CITY_HEADROOM - 1 <= zone.heightCap(),
                    "a street's headroom under the crater: " + where);
                if (plan.hasBuilding()) {
                    buildings++;
                    int top = plan.cityGroundLevel() + plan.floors() * ChunkPlan.FLOOR_HEIGHT
                        + ChunkPlan.FLOOR_HEIGHT
                        - 1;
                    assertTrue(top <= zone.heightCap(), "a roof under the crater: " + where + " top " + top);
                    assertFalse(
                        overlapsDecay(chunkX, chunkZ, impactX, impactZ),
                        "a building in the decay square: " + where);
                    capped += plan.floors() < 3 ? 1 : 0;
                }
            }
        }
        assertTrue(buildings > 0, "the basin keeps buildings farther out");
        assertTrue(capped > 0, "some buildings step down toward the impact");
    }

    private static boolean contains(int chunkX, int chunkZ, int x, int z) {
        return x >> 4 == chunkX && z >> 4 == chunkZ;
    }

    private static boolean overlapsDecay(int chunkX, int chunkZ, int x, int z) {
        int half = CraterBasin.DECAY_HALF_WIDTH;
        return (chunkX << 4) <= x + half - 1 && (chunkX << 4) + 15 >= x - half
            && (chunkZ << 4) <= z + half - 1
            && (chunkZ << 4) + 15 >= z - half;
    }

    private static TreeSet<String> ids(List<Landmark> landmarks) {
        List<String> out = new ArrayList<>();
        for (Landmark landmark : landmarks) {
            out.add(landmark.id());
        }
        return new TreeSet<>(out);
    }
}
