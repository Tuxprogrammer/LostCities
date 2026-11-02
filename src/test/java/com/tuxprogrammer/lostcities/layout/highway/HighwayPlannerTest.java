package com.tuxprogrammer.lostcities.layout.highway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.random.StableHash;

/**
 * The intercity highway planner (intercity-planner.md; new-spec 06, design record 0011): hashing, hubs, connections,
 * routes around obstacles, level sharing, ramps and the chunk query.
 */
public class HighwayPlannerTest {

    static final long SEED = 4242L;
    static final long SALT = StableHash.stableStringHash("minecraft:overworld");

    /** Potential 1 at the given hub chunks, falling off over 6 chunks, 0 elsewhere. */
    static CityPotential peaks(int[]... hubs) {
        return new CityPotential() {

            @Override
            public float potential(int chunkX, int chunkZ) {
                float best = 0;
                for (int[] hub : hubs) {
                    int d = Math.abs(hub[0] - chunkX) + Math.abs(hub[1] - chunkZ);
                    best = Math.max(best, 1f - d / 6f);
                }
                return best;
            }

            @Override
            public int scoreWithUpperBound(int chunkX, int chunkZ, int required) {
                return CityPotential.score(potential(chunkX, chunkZ));
            }
        };
    }

    /** Small cells with a sample on every chunk, so hubs land exactly on the peaks. */
    static HighwaySettings.Builder small() {
        return HighwaySettings.builder()
            .hubs(32, 1, 0.2f, 2)
            .connections(10, 200, 2, 10, 1.0f);
    }

    /** Blocked chunks. */
    static HighwayObstacles blocked(Set<Long> chunks) {
        return new HighwayObstacles() {

            @Override
            public boolean blocksHub(int chunkX, int chunkZ) {
                return chunks.contains(key(chunkX, chunkZ));
            }

            @Override
            public Area area(int minX, int minZ, int maxX, int maxZ) {
                return (x, z) -> chunks.contains(key(x, z));
            }
        };
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    static Set<Long> box(int minX, int minZ, int maxX, int maxZ) {
        Set<Long> out = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                out.add(key(x, z));
            }
        }
        return out;
    }

    static HighwayPlanner planner(HighwaySettings settings, CityPotential potential, HighwayTerrain terrain,
        HighwayObstacles obstacles) {
        return new HighwayPlanner(SEED, SALT, settings, potential, terrain, obstacles, PlanningGate.NONE);
    }

    /** Three hubs on row 5: A (2, 5) at level 0, B (37, 5) at level 2, C (72, 5) at level 4. */
    static final int[] A = { 2, 5 };
    static final int[] B = { 37, 5 };
    static final int[] C = { 72, 5 };

    static HighwayTerrain levels() {
        return (x, z) -> x < 20 ? 0 : x < 55 ? 2 : 4;
    }

    @Test
    public void theHashIsUpstreamsSaltedSplitMix() {
        HighwayPlanner planner = planner(HighwaySettings.DEFAULTS, peaks(), levels(), HighwayObstacles.NONE);
        long value = SEED ^ SALT ^ 0x494e544552563101L ^ 0x5d39a74e10c26bf1L;
        value ^= StableHash.mix64((3L & 0xffffffffL) + 0x9e3779b97f4a7c15L);
        value ^= StableHash.mix64((-7L & 0xffffffffL) + 0xc2b2ae3d27d4eb4fL);
        value ^= StableHash.mix64((11L & 0xffffffffL) + 0x165667b19e3779f9L);
        assertEquals(StableHash.mix64(value), planner.hash(HighwayPlanner.HUB_SAMPLE_SALT, 3, -7, 11));
        long mixed = StableHash.mix64(5L ^ Long.rotateLeft(-2L & 0xffffffffL, 32));
        assertEquals((int) (mixed ^ mixed >>> 32), HighwayPlanner.pairExtra(new HubKey(5, -2)));
    }

    @Test
    public void aHubIsTheBestSampleAndNeverOnARailwayCorridor() {
        HighwayPlanner planner = planner(small().build(), peaks(A), levels(), HighwayObstacles.NONE);
        Hub hub = planner.hub(new HubKey(0, 0));
        assertNotNull(hub);
        assertEquals(A[0], hub.chunkX());
        assertEquals(A[1], hub.chunkZ());
        assertEquals(CityPotential.SCORE_SCALE, hub.score());
        assertEquals(0, hub.cityLevel());
        // A peak on a railway row (z = 9) and column (x = 4) moves to the chunk past it.
        HighwayPlanner railway = planner(small().build(), peaks(new int[] { 4, 9 }), levels(), HighwayObstacles.NONE);
        Hub moved = railway.hub(new HubKey(0, 0));
        assertFalse(railway.isVerticalRailwayOrStationCorridor(moved.chunkX()));
        assertFalse(railway.isHorizontalRailwayCorridor(moved.chunkZ()));
    }

    @Test
    public void aCellBelowTheMinimumPotentialHasNoHub() {
        CityPotential weak = new CityPotential() {

            @Override
            public float potential(int chunkX, int chunkZ) {
                return 0.1f;
            }

            @Override
            public int scoreWithUpperBound(int chunkX, int chunkZ, int required) {
                return CityPotential.score(0.1f);
            }
        };
        assertNull(planner(small().build(), weak, levels(), HighwayObstacles.NONE).hub(new HubKey(0, 0)));
    }

    @Test
    public void aHubIsNeverInALandmarksCoreOrMargin() {
        // P06-2 and AR-10: the peak itself is an obstacle; the hub moves to the best sample outside it.
        HighwayPlanner planner = planner(small().build(), peaks(A), levels(), blocked(box(1, 4, 3, 6)));
        Hub hub = planner.hub(new HubKey(0, 0));
        assertNotNull(hub);
        assertFalse(hub.chunkX() >= 1 && hub.chunkX() <= 3 && hub.chunkZ() >= 4 && hub.chunkZ() <= 6);
    }

    @Test
    public void hubsPickOneSectorFirstAndOnlyMutualPicksConnect() {
        HighwayPlanner planner = planner(small().build(), peaks(A, B, C), levels(), HighwayObstacles.NONE);
        HubKey a = new HubKey(0, 0);
        HubKey b = new HubKey(1, 0);
        HubKey c = new HubKey(2, 0);
        List<Candidate> candidates = planner.candidates(a);
        assertEquals(2, candidates.size());
        assertEquals(
            b,
            candidates.get(0)
                .target()
                .key(),
            "nearest first");
        assertEquals(
            Sector.EAST,
            candidates.get(0)
                .sector());
        // Both are east: the first pass takes B, the second C.
        assertEquals(java.util.Arrays.asList(b, c), planner.selected(a));
        assertEquals(java.util.Arrays.asList(ConnectionKey.of(a, b), ConnectionKey.of(a, c)), planner.accepted(a));
        // The smaller cell owns the route.
        assertEquals(
            2,
            planner.ownedRoutes(a)
                .size());
        assertEquals(
            1,
            planner.ownedRoutes(b)
                .size());
        assertTrue(
            planner.ownedRoutes(c)
                .isEmpty());
    }

    @Test
    public void aStraightRouteRunsFromHubToHubAtTheAverageLevel() {
        HighwayPlanner planner = planner(small().build(), peaks(A, B), levels(), HighwayObstacles.NONE);
        Route route = planner.ownedRoutes(new HubKey(0, 0))
            .get(0);
        assertEquals(Route.Shape.STRAIGHT, route.shape());
        assertEquals(1, route.level(), "(0 + 2) / 2");
        assertEquals(
            1,
            route.runs()
                .size());
        Run run = route.runs()
            .get(0);
        assertEquals(
            Run.EndKind.HUB,
            run.start()
                .kind());
        assertEquals(B[0] - A[0], run.length());
        assertEquals(0, route.detours());
    }

    @Test
    public void aRouteDetoursAroundAnObstacleOnTheNearerSide() {
        // An obstacle across row 5 from x 15 to 20, z 3 to 7: north and south are 3 rows away, so north wins.
        HighwayPlanner planner = planner(small().build(), peaks(A, B), levels(), blocked(box(15, 3, 20, 7)));
        Route route = planner.ownedRoutes(new HubKey(0, 0))
            .get(0);
        assertEquals(1, route.detours());
        assertEquals(
            1,
            route.runs()
                .size());
        Run run = route.runs()
            .get(0);
        for (int x = A[0]; x <= B[0]; x++) {
            for (int z = 0; z <= 10; z++) {
                if (run.contains(x, z)) {
                    assertFalse(
                        x >= 15 && x <= 20 && z >= 3 && z <= 7,
                        "the run enters the obstacle at " + x + "," + z);
                }
            }
        }
        assertTrue(run.contains(14, 2), "the leg leaves the line on the chunk before the box");
        assertTrue(run.contains(21, 2), "and rejoins on the chunk after it");
        assertTrue(run.contains(17, 2), "running parallel one chunk past the box");
        HighwayInfo corner = planner.info(14, 5);
        assertEquals(
            HighwayInfo.Shape.BEND,
            corner.pieces(14, 5)
                .get(0)
                .shape(),
            "a bend where the detour leaves the line");
    }

    @Test
    public void aRouteThatCannotDetourEndsBeforeTheObstacleOnBothSides() {
        // A wall of obstacle from z 0 to 15 across the row: the offset would exceed 8 chunks.
        HighwayPlanner planner = planner(small().build(), peaks(A, B), levels(), blocked(box(15, -8, 20, 18)));
        Route route = planner.ownedRoutes(new HubKey(0, 0))
            .get(0);
        assertEquals(
            2,
            route.runs()
                .size());
        assertEquals(1, route.deadEnds());
        Run west = route.runs()
            .get(0);
        Run east = route.runs()
            .get(1);
        assertEquals(
            Run.EndKind.DEAD_END,
            west.end()
                .kind());
        assertEquals(
            14,
            west.end()
                .chunkX(),
            "one chunk before the obstacle");
        assertEquals(
            Run.EndKind.DEAD_END,
            east.start()
                .kind());
        assertEquals(
            21,
            east.start()
                .chunkX());
        for (int x = 15; x <= 20; x++) {
            assertFalse(
                planner.info(x, 5)
                    .hasHighway(),
                "no highway in the obstacle at " + x);
        }
        // AR-10: the end is deliberate, a plaza at the ground's level.
        HighwayInfo end = planner.info(14, 5);
        List<HighwayInfo.Piece> pieces = end.pieces(14, 5);
        assertEquals(1, pieces.size());
        assertEquals(
            HighwayInfo.Shape.CROSSING,
            pieces.get(0)
                .shape());
        assertEquals(
            0,
            pieces.get(0)
                .level(),
            "the dead end's ground level");
    }

    @Test
    public void aRouteReachesEachHubAtTheHubsLevelByRampsOfOneLevelPerChunk() {
        HighwayPlanner planner = planner(small().build(), peaks(B, C), levels(), HighwayObstacles.NONE);
        Route route = planner.ownedRoutes(new HubKey(1, 0))
            .get(0);
        assertEquals(3, route.level(), "(2 + 4) / 2");
        int previous = -1;
        for (int x = B[0]; x <= C[0]; x++) {
            Deck deck = planner.info(x, 5)
                .xDeck();
            assertNotNull(deck);
            assertTrue(Math.abs(deck.high() - deck.low()) <= 1, "at most one level across a chunk");
            if (previous >= 0) {
                assertEquals(previous, deck.low(), "each chunk starts where the one before ended, at " + x);
            }
            previous = deck.high();
        }
        assertEquals(
            2,
            planner.info(B[0], 5)
                .xDeck()
                .low(),
            "B's level at B");
        assertEquals(
            4,
            planner.info(C[0], 5)
                .xDeck()
                .high(),
            "C's level at C");
        assertEquals(
            HighwayInfo.Shape.RAMP,
            planner.info(B[0] + 1, 5)
                .pieces(B[0] + 1, 5)
                .get(0)
                .shape());
    }

    @Test
    public void doesNotReproduceHWY09_overlappingRoutesShareTheirLowestLevelAndNeverBreak() {
        HighwayPlanner planner = planner(small().build(), peaks(A, B, C), levels(), HighwayObstacles.NONE);
        Route ab = null;
        Route ac = null;
        for (Route route : planner.ownedRoutes(new HubKey(0, 0))) {
            if (route.second()
                .chunkX() == B[0]) {
                ab = route;
            } else {
                ac = route;
            }
        }
        Route bc = planner.ownedRoutes(new HubKey(1, 0))
            .get(0);
        assertEquals(1, ab.level());
        assertEquals(2, ac.level());
        assertEquals(3, bc.level());
        assertEquals(1, planner.sharedLevel(ac), "A-C shares A-B's level 1");
        assertEquals(2, planner.sharedLevel(bc), "B-C shares A-C's level 2");
        int previous = -1;
        for (int x = A[0]; x <= C[0]; x++) {
            Deck deck = planner.info(x, 5)
                .xDeck();
            if (previous >= 0) {
                assertEquals(previous, deck.low(), "the deck breaks off at " + x);
            }
            previous = deck.high();
        }
    }

    @Test
    public void doesNotReproduceHWY06_highwaysSwitchedOffGiveNoHighwayAnywhere() {
        HighwayPlanner planner = planner(
            small().enabled(false)
                .build(),
            peaks(A, B),
            levels(),
            HighwayObstacles.NONE);
        for (int x = A[0]; x <= B[0]; x++) {
            assertSame(HighwayInfo.NONE, planner.info(x, 5));
        }
    }

    @Test
    public void doesNotReproduceHWY10_theCachesEvictTheLeastRecentlyUsedEntry() {
        LruCache<Integer, String> cache = new LruCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1);
        cache.put(3, "three");
        assertNotNull(cache.get(1), "the entry just used stays");
        assertNull(cache.get(2), "the least recently used goes");
        assertNotNull(cache.get(3));
    }

    @Test
    public void doesNotReproduceHWY11_noHubIsStoredSoHistoryNeverChangesAnAnswer() {
        HighwaySettings settings = small().build();
        HighwayPlanner first = planner(settings, peaks(A, B, C), levels(), HighwayObstacles.NONE);
        HighwayPlanner second = planner(settings, peaks(A, B, C), levels(), HighwayObstacles.NONE);
        // Different histories: one asks east to west after its hubs, the other west to east cold.
        for (int cell = 2; cell >= 0; cell--) {
            first.hub(new HubKey(cell, 0));
        }
        for (int x = C[0]; x >= A[0]; x--) {
            first.info(x, 5);
        }
        for (int x = A[0]; x <= C[0]; x++) {
            assertEquals(
                String.valueOf(
                    first.info(x, 5)
                        .xDeck()),
                String.valueOf(
                    second.info(x, 5)
                        .xDeck()),
                "at " + x);
        }
    }

    @Test
    public void doesNotReproduceHWY14_planningRunsOnTheCallersThreadWithNoPool() {
        int before = Thread.activeCount();
        HighwayPlanner planner = planner(small().build(), peaks(A, B, C), levels(), HighwayObstacles.NONE);
        for (int x = A[0]; x <= C[0]; x++) {
            planner.info(x, 5);
        }
        assertTrue(Thread.activeCount() <= before, "no thread started");
    }

    @Test
    public void doesNotReproduceHWY15_theChunkCacheIsBounded() {
        HighwayPlanner planner = planner(small().build(), peaks(A, B), levels(), HighwayObstacles.NONE);
        for (int z = 0; z < 40; z++) {
            for (int x = 0; x < 40; x++) {
                planner.info(x, z);
            }
        }
        assertTrue(
            planner.cacheStats()
                .contains("chunks " + HighwayPlanner.CHUNK_CACHE),
            planner.cacheStats());
        assertTrue(planner.estimatedCacheBytes() < 4L * 1024 * 1024, "well within B5's 16 MB");
    }

    @Test
    public void theGateIsAskedBeforeEachMissingHubAndChangesNothing() {
        List<Runnable> asked = new ArrayList<>();
        HighwayPlanner gated = new HighwayPlanner(
            SEED,
            SALT,
            small().build(),
            peaks(A, B),
            levels(),
            HighwayObstacles.NONE,
            asked::add);
        HighwayPlanner plain = planner(small().build(), peaks(A, B), levels(), HighwayObstacles.NONE);
        assertEquals(
            String.valueOf(
                plain.info(10, 5)
                    .xDeck()),
            String.valueOf(
                gated.info(10, 5)
                    .xDeck()));
        assertFalse(asked.isEmpty());
        int count = asked.size();
        gated.info(11, 5);
        assertEquals(count, asked.size(), "a cached hub is not asked for again");
    }

    @Test
    public void theAnswerDoesNotDependOnQueryOrderOrCacheSize() {
        Map<Long, String> forward = new HashMap<>();
        HighwayPlanner one = planner(small().build(), peaks(A, B, C), levels(), blocked(box(45, 3, 48, 7)));
        for (int z = 0; z < 12; z++) {
            for (int x = 0; x < 80; x++) {
                forward.put(key(x, z), describe(one.info(x, z)));
            }
        }
        HighwayPlanner two = planner(small().build(), peaks(A, B, C), levels(), blocked(box(45, 3, 48, 7)));
        for (int z = 11; z >= 0; z--) {
            for (int x = 79; x >= 0; x--) {
                assertEquals(forward.get(key(x, z)), describe(two.info(x, z)), "at " + x + "," + z);
            }
        }
    }

    static String describe(HighwayInfo info) {
        return info.xDeck() + "/" + info.zDeck() + "/" + info.classification();
    }
}
