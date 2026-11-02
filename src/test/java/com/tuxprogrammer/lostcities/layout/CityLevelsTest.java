package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/** {@link CityLevels}: upstream's height-to-level table and the averaged heightmap with a regular sample grid. */
public class CityLevelsTest {

    @Test
    public void heightToLevelThresholds() {
        LayoutPlanner p = LayoutFixtures
            .planner(1L, LayoutProfile.defaults(), LayoutFixtures.flatTerrain(), StructureOracle.NONE);
        assertEquals(0, p.levels.levelForHeight(74));
        assertEquals(1, p.levels.levelForHeight(75));
        assertEquals(1, p.levels.levelForHeight(82));
        assertEquals(2, p.levels.levelForHeight(83));
        assertEquals(7, p.levels.levelForHeight(130));
        assertEquals(8, p.levels.levelForHeight(131));
        assertEquals(8, p.levels.levelForHeight(250));
    }

    @Test
    public void doesNotReproduceCITY34_sampleCellsAroundZeroAreRegular() {
        // Sample size 3: cells [-3, -1] and [0, 2], centered on -2 and 1. Upstream's truncating division put chunks
        // -2 and -1 in a two-chunk cell centered on -1 and chunk -3 in another cell.
        assertEquals(-2, CityLevels.cellCenter(-1, 3, 0));
        assertEquals(-2, CityLevels.cellCenter(-2, 3, 0));
        assertEquals(-2, CityLevels.cellCenter(-3, 3, 0));
        assertEquals(-5, CityLevels.cellCenter(-4, 3, 0));
        assertEquals(1, CityLevels.cellCenter(0, 3, 0));
        assertEquals(1, CityLevels.cellCenter(2, 3, 0));
        assertEquals(4, CityLevels.cellCenter(2, 3, 1));
        assertEquals(-2, CityLevels.cellCenter(2, 3, -1));
    }

    @Test
    public void averagingTakesCityNeighborSamples() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(.02)
            .cityRadius(150, 200)
            .averageHeightmap(true, 3)
            .build();
        // Height 96 (level 3) at sample centers, 70 (level 0) elsewhere.
        LayoutFixtures.FakeTerrain terrain = new LayoutFixtures.FakeTerrain(
            (x, z) -> Math.floorMod(x, 3) == 1 && Math.floorMod(z, 3) == 1 ? 96 : 70);
        LayoutPlanner p = LayoutFixtures.planner(2L, profile, terrain, StructureOracle.NONE);
        int x = 4;
        int z = 3;
        int sum = 70;
        int count = 1;
        int[][] samples = { { 1, 4 }, { 7, 4 }, { 4, 1 }, { 4, 7 } };
        for (int[] s : samples) {
            if (p.isCityRaw(s[0], s[1])) {
                sum += 96;
                count++;
            }
        }
        assertEquals(p.levels.levelForHeight(sum / count), p.cityLevel(x, z));
    }
}
