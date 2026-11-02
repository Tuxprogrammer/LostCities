package com.tuxprogrammer.lostcities.compat.lootgames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

/** The LootGames oracle: its footprint, completeness, and in a real layout, rooms the city never covers. */
class LootGamesOracleTest {

    private static final LootGamesSettings PACK = LootGamesSettings.of(false, 0, 20);

    private static LootGamesOracle oracle(int cacheSize) {
        return new LootGamesOracle(4242L, PACK, (x, z) -> 72, cacheSize);
    }

    @Test
    void roomAndTunnel() {
        Landmark landmark = oracle(LootGamesOracle.CACHE_SIZE).dungeonAt(-37, 11);
        assertNotNull(landmark);
        assertEquals("lootgames@-37,11", landmark.id());
        assertEquals(LandmarkKind.LOOTGAMES, landmark.kind());
        assertEquals(Certainty.EXACT, landmark.certainty());
        assertEquals(72, landmark.groundEstimate());
        BlockRect room = landmark.footprint()
            .get(0);
        BlockRect tunnel = landmark.footprint()
            .get(1);
        // The room centered on block (16 cx + 8, 16 cz + 8), 21 x 21.
        assertEquals(new BlockRect(-37 * 16 - 2, 11 * 16 - 2, -37 * 16 + 18, 11 * 16 + 18), room);
        // The tunnel from the +Z wall, 15 blocks at most.
        assertEquals(new BlockRect(-37 * 16 + 8, 11 * 16 + 19, -37 * 16 + 8, 11 * 16 + 33), tunnel);
        assertNull(oracle(1).dungeonAt(-37, 12));
    }

    @Test
    void completeWhateverRectangleIsAsked() {
        TreeSet<String> all = ids(oracle(LootGamesOracle.CACHE_SIZE).landmarksTouching(-60, -60, 59, 59));
        assertTrue(all.size() >= 22, all.toString());
        LootGamesOracle tiled = oracle(1);
        TreeSet<String> tiles = new TreeSet<>();
        for (int x = 59; x >= -60; x -= 7) {
            for (int z = 59; z >= -60; z -= 5) {
                tiles.addAll(ids(tiled.landmarksTouching(Math.max(-60, x - 6), Math.max(-60, z - 4), x, z)));
            }
        }
        assertEquals(all, tiles);
    }

    @Test
    void aTunnelReachesTwoChunksSouth() {
        LootGamesOracle oracle = oracle(LootGamesOracle.CACHE_SIZE);
        // The tunnel of the site at chunk (-37, 11) ends at block Z 16 * 11 + 33, in chunk 13.
        List<Landmark> found = oracle.landmarksTouching(-37, 13, -37, 13);
        assertEquals(1, found.size());
        assertEquals(
            "lootgames@-37,11",
            found.get(0)
                .id());
    }

    @Test
    void offSettings() {
        assertSame(LootGamesSettings.OFF, LootGamesSettings.of(true, 0, 20));
        assertSame(LootGamesSettings.OFF, LootGamesSettings.of(false, 7, -1));
        assertEquals("on rhomb=20", PACK.summary());
        assertThrows(IllegalArgumentException.class, () -> new LootGamesOracle(1L, LootGamesSettings.OFF, null, 1));
    }

    /** In a city: every chunk a room or its tunnel touches is core, never city. */
    @Test
    void theCityLeavesEveryRoomNatural() {
        LootGamesOracle oracle = oracle(LootGamesOracle.CACHE_SIZE);
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    106L,
                    LayoutFixtures.predefinedOnly(),
                    LayoutFixtures.standardAssets(),
                    LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(-37, 11, 480, "citystyle_common"))
                .structures(oracle)
                .build());
        int checked = 0;
        for (Landmark landmark : oracle.landmarksTouching(-60, -20, -10, 40)) {
            for (BlockRect rect : landmark.footprint()) {
                for (int chunkX = rect.minChunkX(); chunkX <= rect.maxChunkX(); chunkX++) {
                    for (int chunkZ = rect.minChunkZ(); chunkZ <= rect.maxChunkZ(); chunkZ++) {
                        ChunkPlan plan = planner.plan(chunkX, chunkZ);
                        assertEquals(
                            Zone.CORE,
                            plan.zone()
                                .zone(),
                            landmark.id() + " at chunk " + chunkX + "," + chunkZ);
                        assertTrue(!plan.isCity(), landmark.id() + " at chunk " + chunkX + "," + chunkZ);
                        checked++;
                    }
                }
            }
        }
        assertTrue(checked > 0);
    }

    private static TreeSet<String> ids(List<Landmark> landmarks) {
        List<String> out = new ArrayList<>();
        for (Landmark landmark : landmarks) {
            out.add(landmark.id());
        }
        return new TreeSet<>(out);
    }
}
