package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * The rise cap (alpha review AR-4, docs/new-spec/03-terrain/height-mapping.md "Rise cap"): a city climbs at most
 * {@code cityMaxRise} levels above its base, terrain one level higher is cut down to that, and steeper ground is left
 * out of the city.
 */
public class RiseCapTest {

    /** Flat at Y 70 (level 0) up to chunk X 10, then a slope rising 8 blocks (one level) per chunk eastward. */
    private static LayoutFixtures.FakeTerrain slope() {
        return new LayoutFixtures.FakeTerrain((x, z) -> 70 + Math.max(0, x - 10) * 8);
    }

    private static LayoutPlanner planner(LayoutProfile profile, LayoutFixtures.FakeTerrain terrain) {
        return new LayoutPlanner(
            LayoutInputs.builder(7L, profile, LayoutFixtures.standardAssets(), terrain)
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .structures(StructureOracle.NONE)
                .build());
    }

    @Test
    public void aCityClimbsTwoLevelsAndCutsEverythingAbove() {
        LayoutPlanner p = planner(LayoutFixtures.predefinedOnly(), slope());
        assertEquals(0, p.cities.riseBase(5, 0), "the base is the ground around the center");
        assertTrue(p.isCityRaw(11, 0));
        assertEquals(1, p.cityLevel(11, 0));
        assertTrue(p.isCityRaw(12, 0));
        assertEquals(2, p.cityLevel(12, 0), "two levels up: the cap");
        assertTrue(p.isCityRaw(13, 0), "above the cap is still city");
        assertEquals(2, p.cityLevel(13, 0), "and is cut down to the cap");
        assertTrue(p.isCityRaw(14, 0), "D-rise-rim-1: a hill inside the city is never dropped");
        assertEquals(2, p.cityLevel(14, 0), "it is cut down to the cap");
        assertTrue(p.isCityRaw(18, 0));
        assertEquals(2, p.cityLevel(18, 0));
    }

    @Test
    public void noCapAtTheNumberOfLevels() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0)
            .cityMaxRise(8)
            .build();
        LayoutPlanner p = planner(profile, slope());
        assertTrue(p.isCityRaw(14, 0), "upstream climbs any slope up to cityMaxHeight");
        assertEquals(4, p.cityLevel(14, 0));
        assertEquals(CityMap.NO_BASE, p.cities.riseBase(5, 0));
    }

    @Test
    public void theBaseIsTheMedianAroundTheCenterNotOneSpike() {
        // A spike at the center chunk itself, flat around it.
        LayoutFixtures.FakeTerrain spike = new LayoutFixtures.FakeTerrain((x, z) -> x == 0 && z == 0 ? 130 : 70);
        LayoutPlanner p = planner(LayoutFixtures.predefinedOnly(), spike);
        assertEquals(0, p.cities.riseBase(3, 3));
    }

    @Test
    public void aCityOnAHillStepsDownFreely() {
        // Level 4 ground around the center, falling one level per chunk west of chunk X -4.
        LayoutFixtures.FakeTerrain hill = new LayoutFixtures.FakeTerrain(
            (x, z) -> x >= -4 ? 102 : Math.max(70, 102 - (-4 - x) * 8));
        LayoutPlanner p = planner(LayoutFixtures.predefinedOnly(), hill);
        assertEquals(4, p.cities.riseBase(0, 0));
        assertTrue(p.isCityRaw(-10, 0), "lower ground stays city: the cap limits rises only");
        assertEquals(0, p.cityLevel(-10, 0));
    }

    @Test
    public void perlinCitiesHaveNoBase() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(-1)
            .build();
        LayoutPlanner p = LayoutFixtures.planner(9L, profile, slope(), StructureOracle.NONE);
        assertEquals(CityMap.NO_BASE, p.cities.riseBase(5, 0));
    }
}
