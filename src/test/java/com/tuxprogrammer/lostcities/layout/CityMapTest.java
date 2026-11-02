package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedBuilding;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedCity;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedIndex;

/** {@link CityMap}: centers, radii, the city factor and city styles, with upstream's numbers and the D10 fixes. */
public class CityMapTest {

    private static LayoutPlanner planner(long seed, LayoutProfile profile) {
        return LayoutFixtures.planner(seed, profile, LayoutFixtures.flatTerrain(), StructureOracle.NONE);
    }

    @Test
    public void centerAndRadiusUseUpstreamsSeedsPlusTheWorldSeed() {
        long seed = 12345L;
        LayoutPlanner p = planner(seed, LayoutProfile.defaults());
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                boolean expected = new Random(seed + z * 797003437L + x * 295075153L).nextDouble() < .01;
                assertEquals(expected, p.isCityCenter(x, z));
                if (expected) {
                    int radius = 50 + new Random(seed + z * 100001653L + x * 295075153L).nextInt(78);
                    assertEquals(radius, (int) p.cityRadius(x, z));
                }
            }
        }
    }

    @Test
    public void doesNotReproducePROF11_citiesMoveWithTheWorldSeed() {
        LayoutPlanner a = planner(1L, LayoutProfile.defaults());
        LayoutPlanner b = planner(2L, LayoutProfile.defaults());
        Set<String> centersA = new HashSet<>();
        Set<String> centersB = new HashSet<>();
        for (int x = -60; x < 60; x++) {
            for (int z = -60; z < 60; z++) {
                if (a.isCityCenter(x, z)) {
                    centersA.add(x + "," + z);
                }
                if (b.isCityCenter(x, z)) {
                    centersB.add(x + "," + z);
                }
            }
        }
        assertFalse(centersA.isEmpty());
        assertNotEquals(centersA, centersB);
    }

    @Test
    public void factorIsTheSumOfContributionsClampedToOne() {
        LayoutPlanner p = planner(7L, LayoutProfile.defaults());
        for (int x = -30; x < 30; x += 3) {
            for (int z = -30; z < 30; z += 3) {
                float sum = 0;
                for (int cx = x - 8; cx <= x + 8; cx++) {
                    for (int cz = z - 8; cz <= z + 8; cz++) {
                        if (p.isCityCenter(cx, cz)) {
                            float r = p.cityRadius(cx, cz);
                            float d2 = (cx - x) * 16 * (cx - x) * 16 + (cz - z) * 16 * (cz - z) * 16;
                            if (d2 < r * r) {
                                sum += (r - (float) Math.sqrt(d2)) / r;
                            }
                        }
                    }
                }
                assertEquals(Math.min(sum, 1f), p.cityFactor(x, z), 1e-6f);
            }
        }
    }

    @Test
    public void heightWindowExcludesLowAndHighTerrain() {
        LayoutProfile profile = LayoutFixtures.denseCity();
        LayoutPlanner low = LayoutFixtures
            .planner(3L, profile, new LayoutFixtures.FakeTerrain((x, z) -> 40), StructureOracle.NONE);
        LayoutPlanner high = LayoutFixtures
            .planner(3L, profile, new LayoutFixtures.FakeTerrain((x, z) -> 160), StructureOracle.NONE);
        for (int x = -10; x < 10; x++) {
            assertEquals(0f, low.cityFactor(x, 0));
            assertEquals(0f, high.cityFactor(x, 0));
        }
    }

    @Test
    public void doesNotReproducePROF10_aCityHasItsCentersStyle() {
        // One isolated city, from a predefined center; the profile's own chance is 0, so no other city overlaps it.
        PredefinedIndex index = PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                new PredefinedCity(
                    "test",
                    "minecraft:overworld",
                    0,
                    0,
                    120,
                    "citystyle_desert",
                    Collections.<PredefinedBuilding>emptyList(),
                    Collections.<int[]>emptyList())),
            name -> null);
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(
                5L,
                LayoutProfile.builder()
                    .cityChance(0)
                    .build(),
                LayoutFixtures.standardAssets(),
                LayoutFixtures.flatTerrain())
                .predefined(index)
                .build());
        int city = 0;
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (p.isCityRaw(x, z)) {
                    assertEquals("citystyle_desert", p.cities.cityStyleName(x, z), x + "," + z);
                    city++;
                }
            }
        }
        assertTrue(city > 50);
    }

    @Test
    public void doesNotReproduceCITY05_rarityMapFollowsThisProfile() {
        LayoutProfile coarse = LayoutProfile.builder()
            .cityChance(-1)
            .cityPerlin(3, .1, .1)
            .build();
        LayoutProfile fine = LayoutProfile.builder()
            .cityChance(-1)
            .cityPerlin(9, 1, 0)
            .build();
        LayoutPlanner a = planner(4L, coarse);
        LayoutPlanner b = planner(4L, fine);
        CityRarityMap expectedA = new CityRarityMap(4L, 3, .1, .1);
        CityRarityMap expectedB = new CityRarityMap(4L, 9, 0, 1);
        for (int x = -10; x < 10; x++) {
            assertEquals(Math.min(1f, expectedA.getCityFactor(x, 2)), a.cityFactor(x, 2));
            assertEquals(Math.min(1f, expectedB.getCityFactor(x, 2)), b.cityFactor(x, 2));
        }
    }

    @Test
    public void doesNotReproduceCITY06_styleCacheBelongsToThePlannersProfile() {
        // Same seed and assets, but every contribution falls below the threshold in the second profile, so its
        // chunks take the alternative style; a shared, profile-blind cache would hand one profile's answer to the
        // other.
        LayoutPlanner normal = planner(8L, LayoutFixtures.denseCity());
        LayoutPlanner alternative = planner(
            8L,
            LayoutProfile.builder()
                .cityChance(.02)
                .cityRadius(150, 200)
                .cityStyleAlternative(1.1f, "citystyle_alt")
                .build());
        int checked = 0;
        for (int x = -10; x < 10; x++) {
            String a = normal.cities.cityStyleName(x, 0);
            String b = alternative.cities.cityStyleName(x, 0);
            if (normal.cityFactor(x, 0) > 0) {
                assertNotEquals("citystyle_alt", a);
                assertEquals("citystyle_alt", b);
                checked++;
            }
        }
        assertTrue(checked > 0);
    }

    @Test
    public void doesNotReproduceCITY07_everyChunkOfAPredefinedMultibuildingIsCity() {
        PredefinedIndex index = PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                new PredefinedCity(
                    "test",
                    "minecraft:overworld",
                    500,
                    500,
                    10,
                    "citystyle_common",
                    Arrays.asList(new PredefinedBuilding("multi3x3", 0, 0, true, false)),
                    Collections.<int[]>emptyList())),
            name -> new LayoutFixtures.FakeMulti(name, 3, 3, "building1"));
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(
                5L,
                LayoutProfile.builder()
                    .cityChance(0)
                    .build(),
                LayoutFixtures.standardAssets(),
                LayoutFixtures.flatTerrain())
                .predefined(index)
                .build());
        for (int x = 500; x < 503; x++) {
            for (int z = 500; z < 503; z++) {
                assertEquals(1f, p.cityFactor(x, z), x + "," + z);
            }
        }
        assertEquals(0f, p.cityFactor(503, 500));
    }

    @Test
    public void doesNotReproduceCITY39_spawnMultiplierFarFromTheOrigin() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(-1)
            .cityPerlin(3, 0, -1)
            .citySpawn(1000, 1.0, 2000, .5)
            .build();
        LayoutPlanner p = planner(1L, profile);
        // 200,000 blocks out: upstream squares the block coordinate in int, which overflows to a negative distance.
        int far = 200_000 / 16;
        float factor = p.cityFactor(far, 0);
        assertFalse(Float.isNaN(factor));
        assertEquals(.5f, factor, 1e-6f);
        assertEquals(1f, p.cityFactor(0, 0), 1e-6f);
    }

    @Test
    public void doesNotReproduceCITY40_aLargePredefinedCityReachesItsFullRadius() {
        // Radius 480 blocks, far beyond the search square of cityMaxRadius 128 (8 chunks): chunk 20 lies 320 blocks
        // from the center, where the contribution (480 - 320) / 480 = 1/3 is above the threshold.
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs
                .builder(
                    5L,
                    LayoutFixtures.predefinedOnly(),
                    LayoutFixtures.standardAssets(),
                    LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_desert"))
                .build());
        assertEquals(1f / 3f, p.cityFactor(20, 0), 1e-6f);
        assertTrue(p.isCityRaw(20, 0));
        assertEquals("citystyle_desert", p.cities.cityStyleName(20, 0));
        assertFalse(p.isCityRaw(25, 0), "400 blocks out: 1/6, below the threshold");
    }

    @Test
    public void strongestCenterIsTheLargestContribution() {
        LayoutPlanner p = planner(7L, LayoutFixtures.denseCity());
        List<int[]> chunks = new ArrayList<>();
        for (int x = -5; x < 5; x++) {
            chunks.add(new int[] { x, x });
        }
        for (int[] c : chunks) {
            int[] center = p.cities.strongestCenter(c[0], c[1]);
            if (center != null) {
                assertTrue(p.isCityCenter(center[0], center[1]));
            }
        }
    }
}
