package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * The city shape rules: a city chunk joins its center edge to edge ({@code cityConnected}), sits within
 * {@code cityGrade} levels of its city's base, and follows the mean height of its neighborhood
 * ({@code cityGradeRadius}).
 */
public class CityShapeTest {

    private static LayoutPlanner planner(LayoutProfile profile, LayoutFixtures.FakeTerrain terrain) {
        return new LayoutPlanner(
            LayoutInputs.builder(7L, profile, LayoutFixtures.standardAssets(), terrain)
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .structures(StructureOracle.NONE)
                .build());
    }

    /** Flat at Y 70, except a trench (below {@code cityMinHeight}) along chunk X 4 that cuts the city in two. */
    private static LayoutFixtures.FakeTerrain trench() {
        return new LayoutFixtures.FakeTerrain((x, z) -> x == 4 ? 10 : 70);
    }

    @Test
    public void aChunkCutOffFromTheCenterIsNotCity() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0)
            .cityShape(false, true)
            .build();
        LayoutPlanner p = planner(profile, trench());
        assertTrue(p.characteristics(2, 0).isCity, "joined to the center");
        assertFalse(p.characteristics(4, 0).isCity, "the trench itself");
        assertFalse(p.characteristics(6, 0).isCity, "raw city, but cut off by the trench");
        assertTrue(p.isCityRaw(6, 0));
    }

    @Test
    public void withoutTheRuleTheCutOffChunkStaysCity() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0)
            .cityShape(false, false)
            .build();
        LayoutPlanner p = planner(profile, trench());
        assertTrue(p.characteristics(6, 0).isCity);
    }

    @Test
    public void theGradeCutsAHillAndFillsAHollowToWithinTheGrade() {
        // Base level 2 (ground 91 around the center); a hollow at level 0 west of chunk -3.
        LayoutFixtures.FakeTerrain hollow = new LayoutFixtures.FakeTerrain((x, z) -> x < -3 ? 70 : 90);
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0)
            .cityGrading(1, 0)
            .build();
        LayoutPlanner p = planner(profile, hollow);
        assertEquals(2, p.cities.riseBase(0, 0));
        assertEquals(2, p.cityLevel(0, 0));
        assertEquals(1, p.cityLevel(-6, 0), "the hollow is filled up to one level under the base, not left at 0");
    }

    @Test
    public void theRadiusSmoothsALoneBump() {
        LayoutFixtures.FakeTerrain bump = new LayoutFixtures.FakeTerrain((x, z) -> x == 2 && z == 0 ? 100 : 70);
        LayoutProfile own = LayoutProfile.builder()
            .cityChance(0)
            .build();
        LayoutProfile smooth = LayoutProfile.builder()
            .cityChance(0)
            .cityGrading(8, 1)
            .build();
        assertTrue(planner(own, bump).cityLevel(2, 0) > 0, "without smoothing the bump is its own level");
        assertEquals(0, planner(smooth, bump).cityLevel(2, 0), "the mean of nine chunks, 70 + 30 / 9, is level 0");
    }
}
