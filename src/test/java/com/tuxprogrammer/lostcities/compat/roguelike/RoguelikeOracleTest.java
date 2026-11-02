package com.tuxprogrammer.lostcities.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/** The Roguelike oracle over fake terrain: which candidate wins, the lot, completeness, caching and the probe. */
class RoguelikeOracleTest {

    private static final long SEED = 99L;

    /** Plain grass land; ocean (a rejected biome) west of block X −2000. */
    private static final RoguelikeTerrain LAND = new RoguelikeTerrain() {

        @Override
        public boolean biomeRejects(int blockX, int blockZ) {
            return blockX < -2000;
        }

        @Override
        public boolean grassTop(int blockX, int blockZ) {
            return true;
        }
    };

    private static RoguelikeOracle oracle(TerrainHeightOracle heights, int cacheSize) {
        return new RoguelikeOracle(
            0,
            new RoguelikeGrid(SEED, RoguelikeSettings.pack(), new Random()),
            heights,
            LAND,
            cacheSize);
    }

    @Test
    void flatLandTakesTheFirstCandidate() {
        RoguelikeOracle oracle = oracle((x, z) -> 72, RoguelikeOracle.CACHE_SIZE);
        RoguelikeOracle.Prediction prediction = oracle.predict(1, -2);
        assertTrue(prediction.hasSite());
        assertEquals(0, prediction.candidate());
        int[] first = oracle.grid()
            .candidates(prediction.chunkX(), prediction.chunkZ())[0];
        assertEquals(first[0], prediction.x());
        assertEquals(first[1], prediction.z());
        Landmark landmark = prediction.landmark();
        assertEquals("roguelike@" + first[0] + "," + first[1], landmark.id());
        assertEquals(LandmarkKind.ROGUELIKE, landmark.kind());
        assertEquals(Certainty.CANDIDATE, landmark.certainty());
        assertEquals(72, landmark.groundEstimate());
        assertEquals(
            new BlockRect(first[0] - 10, first[1] - 10, first[0] + 10, first[1] + 10),
            landmark.footprint()
                .get(0));
    }

    @Test
    void laterCandidateWinsWhenEarlierOnesFail() {
        RoguelikeGrid grid = new RoguelikeGrid(SEED, RoguelikeSettings.pack(), new Random());
        int[] chunk = grid.dungeonChunk(0, 0);
        int[][] candidates = grid.candidates(chunk[0], chunk[1]);
        int[] third = candidates[2];
        // Mountains (no air at Y 100) everywhere but a flat 30-block plateau around the third candidate.
        TerrainHeightOracle heights = (x, z) -> Math.abs(x - third[0]) <= 15 && Math.abs(z - third[1]) <= 15 ? 72 : 120;
        RoguelikeOracle.Prediction prediction = oracle(heights, 16).predict(0, 0);
        int expected = -1;
        for (int i = 0; i < candidates.length; i++) {
            if (Math.abs(candidates[i][0] - third[0]) <= 11 && Math.abs(candidates[i][1] - third[1]) <= 11) {
                expected = i;
                break;
            }
        }
        assertEquals(expected, prediction.candidate());
        assertTrue(prediction.candidate() <= 2);
        int rejected = 0;
        for (int count : prediction.rejected()
            .values()) {
            rejected += count;
        }
        assertEquals(prediction.candidate(), rejected, "every earlier candidate is counted once");
    }

    @Test
    void noPassingCandidateMeansNoLandmark() {
        RoguelikeOracle.Prediction prediction = oracle((x, z) -> 50, 16).predict(2, 2);
        assertFalse(prediction.hasSite());
        assertNull(prediction.landmark());
        assertEquals(-1, prediction.candidate());
        assertEquals(
            Integer.valueOf(RoguelikeGrid.CANDIDATES),
            prediction.rejected()
                .get(SiteRules.Verdict.WATER));
    }

    @Test
    void everyLotTouchingARectangleIsReturned() {
        RoguelikeOracle oracle = oracle((x, z) -> 72, RoguelikeOracle.CACHE_SIZE);
        for (int cellX = -2; cellX <= 2; cellX++) {
            for (int cellZ = -2; cellZ <= 2; cellZ++) {
                RoguelikeOracle.Prediction prediction = oracle.predict(cellX, cellZ);
                BlockRect lot = prediction.landmark()
                    .footprint()
                    .get(0);
                // The corner chunks of the lot each find it alone.
                assertTrue(
                    ids(oracle.landmarksTouching(lot.minChunkX(), lot.minChunkZ(), lot.minChunkX(), lot.minChunkZ()))
                        .contains(
                            prediction.landmark()
                                .id()));
                assertTrue(
                    ids(oracle.landmarksTouching(lot.maxChunkX(), lot.maxChunkZ(), lot.maxChunkX(), lot.maxChunkZ()))
                        .contains(
                            prediction.landmark()
                                .id()));
                // And a chunk just past the lot does not.
                assertFalse(
                    ids(
                        oracle.landmarksTouching(
                            lot.maxChunkX() + 1,
                            lot.maxChunkZ() + 1,
                            lot.maxChunkX() + 1,
                            lot.maxChunkZ() + 1)).contains(
                                prediction.landmark()
                                    .id()));
                // The lot lies within the reach the query uses.
                assertTrue(Math.abs(lot.minChunkX() - prediction.chunkX()) <= RoguelikeOracle.REACH_CHUNKS);
                assertTrue(Math.abs(lot.maxChunkZ() - prediction.chunkZ()) <= RoguelikeOracle.REACH_CHUNKS);
            }
        }
    }

    @Test
    void answersDoNotDependOnQueryOrderOrCacheSize() {
        TerrainHeightOracle hilly = (x, z) -> 66 + Math.floorMod(x * 7 + z * 13, 11);
        RoguelikeOracle big = oracle(hilly, RoguelikeOracle.CACHE_SIZE);
        RoguelikeOracle tiny = oracle(hilly, 1);
        List<String> forward = new ArrayList<>();
        List<String> backward = new ArrayList<>();
        for (int chunk = -150; chunk <= 150; chunk += 10) {
            forward.add(ids(big.landmarksTouching(chunk, -chunk, chunk + 9, -chunk + 9)).toString());
        }
        for (int chunk = 150; chunk >= -150; chunk -= 10) {
            backward.add(0, ids(tiny.landmarksTouching(chunk, -chunk, chunk + 9, -chunk + 9)).toString());
        }
        assertEquals(forward, backward);
    }

    @Test
    void rejectedBiomeRegionHasNoLots() {
        RoguelikeOracle oracle = oracle((x, z) -> 72, RoguelikeOracle.CACHE_SIZE);
        assertTrue(
            oracle.landmarksTouching(-400, -50, -200, 50)
                .isEmpty());
        assertFalse(
            oracle.landmarksTouching(200, -50, 400, 50)
                .isEmpty());
    }

    @Test
    void otherDimensionsKnowNothing() {
        RoguelikeOracle nether = new RoguelikeOracle(
            -1,
            new RoguelikeGrid(SEED, RoguelikeSettings.pack(), new Random()),
            (x, z) -> 72,
            LAND,
            16);
        assertFalse(nether.enabled());
        assertTrue(
            nether.landmarksTouching(-200, -200, 200, 200)
                .isEmpty());
    }

    @Test
    void aProbeFetchesACellWholeAndNeverHalfway() {
        HeightProbe probe = new HeightProbe();
        int[] calls = { 0 };
        TerrainHeightOracle heights = new TerrainHeightOracle() {

            @Override
            public int height(int blockX, int blockZ) {
                calls[0]++;
                return 72;
            }

            @Override
            public HeightProbe probe() {
                return probe;
            }
        };
        RoguelikeOracle oracle = oracle(heights, 16);
        probe.setActive(true);
        assertThrows(HeightProbe.NotCached.class, () -> oracle.predict(0, 0));
        assertEquals(0, calls[0], "nothing computed while probing");
        assertTrue(probe.fetchMissing());
        assertTrue(calls[0] > 0);
        int after = calls[0];
        RoguelikeOracle.Prediction prediction = oracle.predict(0, 0);
        assertTrue(prediction.hasSite());
        assertEquals(after, calls[0], "the fetched cell is cached");
        probe.setActive(false);
    }

    @Test
    void predictionsAreStable() {
        RoguelikeOracle a = oracle((x, z) -> 72, 16);
        RoguelikeOracle b = oracle((x, z) -> 72, 16);
        assertEquals(
            a.predict(3, 4)
                .toString(),
            b.predict(3, 4)
                .toString());
        assertEquals(
            Collections.singletonList(
                a.predict(3, 4)
                    .landmark()
                    .id()),
            Collections.singletonList(
                b.predictForChunk(
                    b.predict(3, 4)
                        .chunkX(),
                    b.predict(3, 4)
                        .chunkZ())
                    .landmark()
                    .id()));
    }

    private static List<String> ids(List<Landmark> landmarks) {
        List<String> out = new ArrayList<>();
        for (Landmark landmark : landmarks) {
            out.add(landmark.id());
        }
        return out;
    }
}
