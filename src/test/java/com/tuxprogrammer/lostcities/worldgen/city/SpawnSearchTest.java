package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.ProfileValidator;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;

/** Spawn selection over the layout (docs/original-spec/01-core/spawn-selection.md; D21). */
public class SpawnSearchTest {

    private static final LayoutPlanner PLANNER = CityFixtures.planner(CityFixtures.SEED, ProfileValues.defaults());

    /** The stand-in terrain; counts how many columns the search asked about. */
    static final class CountingTerrain implements SpawnSearch.Terrain {

        int heights;

        @Override
        public String biomeName(int x, int z) {
            return x > 200 ? "Desert" : "Plains";
        }

        @Override
        public boolean biomeExists(String name) {
            return "Desert".equalsIgnoreCase(name) || "Plains".equalsIgnoreCase(name);
        }

        @Override
        public int height(int x, int z) {
            heights++;
            return TestTerrain.surface(x, z) + 1;
        }
    }

    private static SpawnSearch.Options options(String... pairs) {
        Map<String, String> given = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            given.put(pairs[i], pairs[i + 1]);
        }
        ProfileValidator.Result result = ProfileValidator.validate(given);
        assertTrue(result.ok(), result.errors()::toString);
        return SpawnSearch.Options.of(ProfileValues.of(result.values()));
    }

    private static SpawnSearch search(CountingTerrain terrain) {
        return new SpawnSearch(PLANNER, terrain, 63, Collections.<SpawnSearch.City>emptyList());
    }

    @Test
    public void theDefaultProfileKeepsThePacksSpawn() {
        SpawnSearch.Options none = options();
        assertFalse(none.needsSearch());
        assertNull(search(new CountingTerrain()).find(none, new ArrayList<String>()));
    }

    @Test
    public void forceSpawnInBuildingPicksABuildingsGroundFloor() {
        SpawnSearch.Spot spot = search(new CountingTerrain())
            .find(options("forceSpawnInBuilding", "true"), new ArrayList<String>());
        assertNotNull(spot);
        ChunkPlan plan = PLANNER.plan(spot.x() >> 4, spot.z() >> 4);
        assertTrue(plan.isCity() && plan.hasBuilding(), plan::toString);
        assertTrue(spot.inBuilding());
        assertEquals(SpawnSearch.CENTER, spot.x() & 15, "the chunk's center column first");
    }

    @Test
    public void spawnNotInBuildingAvoidsBuildingChunks() {
        SpawnSearch.Spot spot = search(new CountingTerrain())
            .find(options("spawnNotInBuilding", "true"), new ArrayList<String>());
        assertNotNull(spot);
        ChunkPlan plan = PLANNER.plan(spot.x() >> 4, spot.z() >> 4);
        assertFalse(plan.isCity() && plan.hasBuilding());
    }

    @Test
    public void spawnBiomeFindsThatBiome() {
        CountingTerrain terrain = new CountingTerrain();
        SpawnSearch.Spot spot = search(terrain).find(options("spawnBiome", "desert"), new ArrayList<String>());
        assertNotNull(spot);
        assertEquals("Desert", terrain.biomeName(spot.x(), spot.z()));
        List<String> problems = new ArrayList<>();
        assertNull(search(terrain).find(options("spawnBiome", "Nowhere"), problems), "an unknown biome sets no rule");
        assertTrue(
            problems.get(0)
                .contains("Nowhere"));
    }

    @Test
    public void theSpiralWalksUpstreamsRings() {
        List<int[]> ring = SpawnSearch.ring(1);
        assertEquals(8, ring.size());
        assertEquals(-1, ring.get(0)[0]);
        assertEquals(-1, ring.get(0)[1]);
        assertEquals(1, ring.get(3)[0], "the right edge follows the top edge");
        assertEquals(
            24,
            SpawnSearch.ring(3)
                .size());
    }

    @Test
    public void doesNotReproduceCORE13_duplicateForcedEntriesDoNotCrash() {
        SpawnSearch.Options forced = options(
            "forceSpawnBuildings",
            "[\"nosuch\",\"nosuch\"]",
            "spawnCheckAttempts",
            "50");
        assertTrue(forced.placesExactly());
        assertNull(search(new CountingTerrain()).find(forced, new ArrayList<String>()));
    }

    @Test
    public void doesNotReproduceCORE14_aFailedSearchReturnsNothingInsteadOfThrowing() {
        List<String> problems = new ArrayList<>();
        SpawnSearch.Spot spot = search(new CountingTerrain())
            .find(options("forceSpawnParts", "[\"no_such_part\"]", "spawnCheckAttempts", "200"), problems);
        assertNull(spot);
        assertTrue(
            problems.get(0)
                .contains("200"),
            problems::toString);
    }

    @Test
    public void doesNotReproduceCORE15_theSearchReadsTheLayoutNotTheWorld() {
        CountingTerrain terrain = new CountingTerrain();
        SpawnSearch.Spot spot = search(terrain).find(options("forceSpawnInBuilding", "true"), new ArrayList<String>());
        assertNotNull(spot);
        assertEquals(0, terrain.heights, "a building spawn needs no natural height, let alone a generated chunk");
    }

    @Test
    public void doesNotReproduceCORE16_aBuildingSpawnStandsOnTheGroundFloorNotInACellar() {
        SpawnSearch.Spot spot = search(new CountingTerrain())
            .find(options("forceSpawnInBuilding", "true"), new ArrayList<String>());
        ChunkPlan plan = PLANNER.plan(spot.x() >> 4, spot.z() >> 4);
        assertEquals(plan.cityGroundLevel() + 1, spot.y());
    }
}
