package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * VillageNames' village grid against values worked out from its source (MapGenVillageVN.java#L105-L155 at
 * 4.5.21-GTNH) by an independent re-implementation of {@code java.util.Random} and the grid outside Java, with the
 * pack's spacing (median 40, range 10).
 */
class VillageGridTest {

    private static VillageNamesSettings pack(int noVillagesRadius) {
        return new VillageNamesSettings(true, true, 40, 10, noVillagesRadius, Collections.singleton("Plains"));
    }

    @Test
    void seedFormulaIsWorldSetRandomSeeds() {
        assertEquals(341883516024L, VillageGrid.worldRandomSeed(0L, 1, 0, VillageGrid.VILLAGE_SALT));
        assertEquals(-285041895144L, VillageGrid.worldRandomSeed(12345L, -2, 3, VillageGrid.VILLAGE_SALT));
    }

    @Test
    void cellsFloorNegativeChunks() {
        VillageGrid grid = new VillageGrid(0L, pack(0));
        assertEquals(50, grid.cellSize());
        assertEquals(0, grid.cellOf(0));
        assertEquals(0, grid.cellOf(49));
        assertEquals(1, grid.cellOf(50));
        assertEquals(-1, grid.cellOf(-1));
        assertEquals(-1, grid.cellOf(-50));
        assertEquals(-2, grid.cellOf(-51));
    }

    @Test
    void startChunksMatchTheReference() {
        long[] seeds = { 0L, 12345L, -4242424242424242L };
        int[][] cells = { { 0, 0 }, { 1, -1 }, { -3, 7 }, { -82, 82 } };
        int[][][] expected = { { { 11, 8 }, { 53, -38 }, { -144, 360 }, { -4099, 4110 } },
            { { 3, 19 }, { 53, -47 }, { -132, 359 }, { -4082, 4113 } },
            { { 0, 17 }, { 58, -41 }, { -134, 365 }, { -4096, 4101 } } };
        for (int s = 0; s < seeds.length; s++) {
            VillageGrid grid = new VillageGrid(seeds[s], pack(0));
            for (int c = 0; c < cells.length; c++) {
                assertArrayEquals(
                    expected[s][c],
                    grid.startChunk(cells[c][0], cells[c][1]),
                    "seed " + seeds[s] + " cell " + cells[c][0] + "," + cells[c][1]);
            }
        }
    }

    @Test
    void onlyTheStartChunkIsACandidate() {
        VillageGrid grid = new VillageGrid(0L, pack(0));
        assertTrue(grid.isCandidate(-144, 360));
        assertFalse(grid.isCandidate(-145, 360));
        assertFalse(grid.isCandidate(-144, 361));
        assertTrue(grid.isCandidate(11, 8));
    }

    @Test
    void startStaysInsideItsCell() {
        VillageGrid grid = new VillageGrid(99L, pack(0));
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                int[] start = grid.startChunk(cx, cz);
                assertEquals(cx, grid.cellOf(start[0]));
                assertEquals(cz, grid.cellOf(start[1]));
                assertTrue(Math.floorMod(start[0], 50) < 20 && Math.floorMod(start[1], 50) < 20);
            }
        }
    }

    @Test
    void villageFreeRadiusUsesVillageNamesIntArithmetic() {
        VillageGrid grid = new VillageGrid(0L, pack(10));
        assertTrue(grid.insideVillageFreeRadius(5, 5));
        assertFalse(grid.insideVillageFreeRadius(10, 0));
        VillageGrid none = new VillageGrid(0L, pack(0));
        assertFalse(none.insideVillageFreeRadius(46340, 0));
        // 46341² overflows int to a negative number, which VillageNames then finds below 0².
        assertTrue(none.insideVillageFreeRadius(46341, 0));
    }

    @Test
    void defaultSpacingGivesVanillaLikeGrid() {
        VillageNamesSettings defaults = new VillageNamesSettings(
            true,
            true,
            VillageNamesSettings.DEFAULT_SPACING_MEDIAN,
            VillageNamesSettings.DEFAULT_SPACING_RANGE,
            0,
            Collections.<String>emptySet());
        assertEquals(32, defaults.maxSpacing());
        assertEquals(8, defaults.minSpacing());
    }
}
