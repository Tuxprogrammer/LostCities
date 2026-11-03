package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The village oracle over fake biomes and plans: VillageNames' tests in its order, complete answers, identities, and
 * answers that depend on neither the query order nor the cache (CONTRIBUTING § World generation rules,
 * "Deterministic").
 */
class VillageOracleTest {

    private static final long SEED = 4242L;
    private static final TerrainHeightOracle HEIGHTS = (x, z) -> 64 + Math.floorMod(x * 7 + z * 3, 11);

    /** "Plains" on two thirds of the chunks, "Hot Plains" on the rest. */
    static final BiomeNames BIOMES = (x, z) -> Math.floorMod((x >> 4) * 31 + (z >> 4) * 17, 3) == 0 ? "Hot Plains"
        : "Plains";

    /** A plan that grows with the chunk: a town center and four paths, one of them up to 140 blocks long. */
    static final class FakePlans implements VillagePlans {

        final Set<Long> asked = new HashSet<>();
        int calls;
        boolean sizeable = true;

        @Override
        public VillagePlan plan(int chunkX, int chunkZ) {
            calls++;
            asked.add(BoundedCache.key(chunkX, chunkZ));
            int x = chunkX * 16 + 2;
            int z = chunkZ * 16 + 2;
            int far = 21 + Math.floorMod(chunkX * 13 + chunkZ, 120);
            List<VillagePlan.Piece> pieces = Arrays.asList(
                new VillagePlan.Piece(new BlockRect(x, z, x + 9, z + 9), false),
                new VillagePlan.Piece(new BlockRect(x + 3, z - 21, x + 5, z - 1), true),
                new VillagePlan.Piece(new BlockRect(x + 3, z + 10, x + 5, z + 9 + far), true),
                new VillagePlan.Piece(new BlockRect(x - 21, z + 3, x - 1, z + 5), true),
                new VillagePlan.Piece(new BlockRect(x + 10, z + 3, x + 30, z + 5), true),
                new VillagePlan.Piece(new BlockRect(x + 12, z + 7, x + 20, z + 14), false),
                new VillagePlan.Piece(new BlockRect(x - 12, z - 9, x - 4, z - 2), false));
            return new VillagePlan(pieces, sizeable);
        }
    }

    private static VillageNamesSettings settings(boolean on, int radius) {
        return new VillageNamesSettings(on, true, 40, 10, radius, Collections.singleton("Plains"));
    }

    private static VillageOracle oracle(FakePlans plans, int cacheSize) {
        return new VillageOracle(SEED, settings(true, 0), BIOMES, plans, HEIGHTS, 2, cacheSize);
    }

    static String describe(Landmark landmark) {
        return landmark.id() + " "
            + landmark.kind()
            + " "
            + landmark.certainty()
            + " y"
            + landmark.groundEstimate()
            + " "
            + landmark.footprint()
            + " "
            + landmark.gates();
    }

    private static List<String> describeAll(List<Landmark> landmarks) {
        List<String> out = new ArrayList<>();
        for (Landmark landmark : landmarks) {
            out.add(describe(landmark));
        }
        return out;
    }

    @Test
    void findsTheVillagesOfAWindowSortedById() {
        VillageOracle oracle = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        List<Landmark> found = oracle.landmarksTouching(-150, -150, 150, 150);
        assertFalse(found.isEmpty());
        List<String> ids = new ArrayList<>();
        for (Landmark landmark : found) {
            ids.add(landmark.id());
            assertEquals(LandmarkKind.VILLAGE, landmark.kind());
            assertEquals(Certainty.EXACT, landmark.certainty());
            assertEquals(
                7,
                landmark.footprint()
                    .size());
            assertEquals(
                4,
                landmark.gates()
                    .size(),
                describe(landmark));
        }
        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        assertEquals(sorted, ids);
    }

    @Test
    void everyVillageStartsOnTheGridInASpawnBiome() {
        VillageOracle oracle = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        for (Landmark landmark : oracle.landmarksTouching(-150, -150, 150, 150)) {
            String[] coords = landmark.id()
                .substring("village@".length())
                .split(",");
            int cx = Integer.parseInt(coords[0]);
            int cz = Integer.parseInt(coords[1]);
            assertTrue(
                oracle.grid()
                    .isCandidate(cx, cz));
            assertEquals("Plains", BIOMES.nameAt(cx * 16 + 8, cz * 16 + 8));
            assertEquals(
                VillageOracle.Status.VILLAGE,
                oracle.predict(cx, cz)
                    .status());
        }
    }

    @Test
    void rejectedBiomesAreNotPlanned() {
        FakePlans plans = new FakePlans();
        VillageOracle oracle = oracle(plans, VillageOracle.CACHE_SIZE);
        VillageGrid grid = oracle.grid();
        int rejected = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                int[] start = grid.startChunk(cx, cz);
                VillageOracle.Prediction prediction = oracle.predict(start[0], start[1]);
                if (!"Plains".equals(BIOMES.nameAt(start[0] * 16 + 8, start[1] * 16 + 8))) {
                    rejected++;
                    assertEquals(VillageOracle.Status.BIOME_REJECTED, prediction.status());
                    assertNull(prediction.plan());
                    assertFalse(plans.asked.contains(BoundedCache.key(start[0], start[1])));
                }
            }
        }
        assertTrue(rejected > 0, "the fake biomes reject some starts");
        assertEquals(
            VillageOracle.Status.NOT_CANDIDATE,
            oracle.predict(grid.startChunk(0, 0)[0] + 1, 0)
                .status());
    }

    @Test
    void unsizeablePlansAreNoLandmark() {
        FakePlans plans = new FakePlans();
        plans.sizeable = false;
        VillageOracle oracle = oracle(plans, VillageOracle.CACHE_SIZE);
        assertTrue(
            oracle.landmarksTouching(-150, -150, 150, 150)
                .isEmpty());
        assertTrue(plans.calls > 0);
    }

    @Test
    void offMeansEmptyAndNothingPlanned() {
        FakePlans plans = new FakePlans();
        VillageOracle oracle = new VillageOracle(
            SEED,
            settings(false, 0),
            BIOMES,
            plans,
            HEIGHTS,
            2,
            VillageOracle.CACHE_SIZE);
        assertTrue(
            oracle.landmarksTouching(-150, -150, 150, 150)
                .isEmpty());
        assertEquals(0, plans.calls);
    }

    @Test
    void villageFreeRadiusRemovesStarts() {
        VillageOracle free = new VillageOracle(
            SEED,
            settings(true, 1000),
            BIOMES,
            new FakePlans(),
            HEIGHTS,
            2,
            VillageOracle.CACHE_SIZE);
        assertTrue(
            free.landmarksTouching(-150, -150, 150, 150)
                .isEmpty());
        int[] start = free.grid()
            .startChunk(0, 0);
        assertEquals(
            VillageOracle.Status.VILLAGE_FREE,
            free.predict(start[0], start[1])
                .status());
    }

    @Test
    void completeForEveryChunkAFootprintTouches() {
        VillageOracle oracle = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        List<Landmark> all = oracle.landmarksTouching(-120, -120, 120, 120);
        assertFalse(all.isEmpty());
        for (Landmark landmark : all) {
            for (BlockRect rect : landmark.footprint()) {
                for (int cx = rect.minChunkX(); cx <= rect.maxChunkX(); cx++) {
                    for (int cz = rect.minChunkZ(); cz <= rect.maxChunkZ(); cz++) {
                        List<Landmark> one = oracle(new FakePlans(), 4).landmarksTouching(cx, cz, cx, cz);
                        assertTrue(
                            one.stream()
                                .anyMatch(
                                    l -> l.id()
                                        .equals(landmark.id())),
                            landmark.id() + " missing from chunk " + cx + "," + cz);
                    }
                }
            }
        }
    }

    @Test
    void reachCoversTheFarthestPiece() {
        VillageOracle oracle = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        for (Landmark landmark : oracle.landmarksTouching(-200, -200, 200, 200)) {
            String[] coords = landmark.id()
                .substring("village@".length())
                .split(",");
            int cx = Integer.parseInt(coords[0]);
            int cz = Integer.parseInt(coords[1]);
            int[] bounds = landmark.coreChunkBounds(0);
            assertTrue(cx - bounds[0] <= VillageOracle.REACH_CHUNKS && bounds[2] - cx <= VillageOracle.REACH_CHUNKS);
            assertTrue(cz - bounds[1] <= VillageOracle.REACH_CHUNKS && bounds[3] - cz <= VillageOracle.REACH_CHUNKS);
        }
    }

    @Test
    void answersDependOnNeitherQueryOrderNorCache() {
        List<int[]> rects = new ArrayList<>();
        for (int x = -96; x < 96; x += 24) {
            for (int z = -96; z < 96; z += 24) {
                rects.add(new int[] { x, z, x + 35, z + 35 });
            }
        }
        VillageOracle forward = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        List<List<String>> first = new ArrayList<>();
        for (int[] r : rects) {
            first.add(describeAll(forward.landmarksTouching(r[0], r[1], r[2], r[3])));
        }
        VillageOracle backward = oracle(new FakePlans(), 1);
        List<List<String>> second = new ArrayList<>();
        for (int i = rects.size() - 1; i >= 0; i--) {
            int[] r = rects.get(i);
            second.add(0, describeAll(backward.landmarksTouching(r[0], r[1], r[2], r[3])));
        }
        assertEquals(first, second);
        // And again on the first oracle, now warm.
        for (int i = 0; i < rects.size(); i++) {
            int[] r = rects.get(i);
            assertEquals(first.get(i), describeAll(forward.landmarksTouching(r[0], r[1], r[2], r[3])));
        }
    }

    @Test
    void tilesAddUpToTheWhole() {
        VillageOracle whole = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        TreeSet<String> expected = new TreeSet<>(describeAll(whole.landmarksTouching(-64, -64, 63, 63)));
        VillageOracle tiled = oracle(new FakePlans(), 2);
        TreeSet<String> actual = new TreeSet<>();
        for (int x = 56; x >= -64; x -= 8) {
            for (int z = 56; z >= -64; z -= 8) {
                actual.addAll(describeAll(tiled.landmarksTouching(x, z, x + 7, z + 7)));
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    void cacheIsBounded() {
        FakePlans plans = new FakePlans();
        VillageOracle oracle = oracle(plans, 3);
        oracle.landmarksTouching(-300, -300, 300, 300);
        int first = plans.calls;
        oracle.landmarksTouching(-300, -300, 300, 300);
        assertTrue(plans.calls > first, "a 3-entry cache recomputes a large window");
        FakePlans warm = new FakePlans();
        VillageOracle big = oracle(warm, VillageOracle.CACHE_SIZE);
        big.landmarksTouching(-300, -300, 300, 300);
        int once = warm.calls;
        big.landmarksTouching(-300, -300, 300, 300);
        assertEquals(once, warm.calls, "a large cache answers again without planning");
    }

    /** Thrown by {@link #retryingHeights} the first time a column is asked, as the prefetcher's probe does. */
    private static final class NotYet extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    @Test
    void theGroundEstimateWaitsForItsFirstReadAndSurvivesAnAbandonedRead() {
        Set<Long> known = new HashSet<>();
        TerrainHeightOracle retryingHeights = (x, z) -> {
            if (known.add(BoundedCache.key(x, z))) {
                throw new NotYet();
            }
            return HEIGHTS.height(x, z);
        };
        FakePlans plans = new FakePlans();
        VillageOracle oracle = new VillageOracle(
            SEED,
            settings(true, 0),
            BIOMES,
            plans,
            retryingHeights,
            2,
            VillageOracle.CACHE_SIZE);
        List<Landmark> answer = oracle.landmarksTouching(-150, -150, 150, 150);
        assertTrue(known.isEmpty(), "no height is read before a ground estimate is (D263)");
        assertEquals(plans.asked.size(), plans.calls, "each village planned once");
        List<String> described = null;
        for (int attempt = 0; attempt < 10_000 && described == null; attempt++) {
            try {
                described = describeAll(answer);
            } catch (NotYet abandoned) {
                // The height is fetched and the estimate asked again.
            }
        }
        assertNotNull(described);
        assertEquals(
            describeAll(oracle(new FakePlans(), VillageOracle.CACHE_SIZE).landmarksTouching(-150, -150, 150, 150)),
            described,
            "the same landmarks and grounds as an oracle that never abandons");
    }

    @Test
    void groundAndIdentityComeFromThePlan() {
        VillageOracle oracle = oracle(new FakePlans(), VillageOracle.CACHE_SIZE);
        Landmark landmark = oracle.landmarksTouching(-150, -150, 150, 150)
            .get(0);
        assertNotNull(landmark);
        assertTrue(landmark.groundEstimate() >= 64 && landmark.groundEstimate() <= 74);
        assertTrue(
            landmark.id()
                .matches("village@-?\\d+,-?\\d+"));
    }
}
