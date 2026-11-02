package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;

/**
 * VillageNames' ocean-monument grid and site test against its source (StructureOceanMonument.java#L72-L187 at
 * 4.5.21-GTNH); start chunks worked out by an independent re-implementation outside Java.
 */
class MonumentGridTest {

    @Test
    void cellsFloorNegativeChunks() {
        MonumentGrid grid = new MonumentGrid(0L);
        assertEquals(0, grid.cellOf(31));
        assertEquals(1, grid.cellOf(32));
        assertEquals(-1, grid.cellOf(-1));
        assertEquals(-1, grid.cellOf(-32));
        assertEquals(-2, grid.cellOf(-33));
    }

    @Test
    void startChunksMatchTheReference() {
        long[] seeds = { 0L, 12345L, -4242424242424242L };
        int[][] cells = { { 0, 0 }, { 1, -1 }, { -3, 7 }, { -128, 128 } };
        int[][][] expected = { { { 21, 18 }, { 44, -22 }, { -75, 226 }, { -4090, 4111 } },
            { { 3, 19 }, { 44, -12 }, { -92, 248 }, { -4082, 4110 } },
            { { 11, 16 }, { 47, -16 }, { -72, 234 }, { -4092, 4112 } } };
        for (int s = 0; s < seeds.length; s++) {
            MonumentGrid grid = new MonumentGrid(seeds[s]);
            for (int c = 0; c < cells.length; c++) {
                assertArrayEquals(
                    expected[s][c],
                    grid.startChunk(cells[c][0], cells[c][1]),
                    "seed " + seeds[s] + " cell " + cells[c][0] + "," + cells[c][1]);
            }
        }
    }

    @Test
    void deepOceanEverywhereAccepts() {
        assertTrue(MonumentGrid.siteAccepts(3, -4, (x, z) -> "Deep Ocean"));
        assertTrue(MonumentGrid.siteAccepts(3, -4, (x, z) -> "DeepOcean"));
    }

    @Test
    void centerMustBeDeepOcean() {
        AtomicInteger lookups = new AtomicInteger();
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> {
            lookups.incrementAndGet();
            return "Ocean";
        }));
        assertEquals(1, lookups.get(), "a shallow center is rejected after one lookup");
    }

    @Test
    void innerSquareIsNineByNine() {
        // Column (8 + 4, 8) is inside the 9 x 9 square, (8 + 5, 8) only in the 15 x 15 one.
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 12 && z == 8 ? "Ocean" : "Deep Ocean"));
        assertTrue(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 13 && z == 8 ? "River" : "Deep Ocean"));
    }

    @Test
    void outerSquareIsFifteenByFifteen() {
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 15 && z == 15 ? "Plains" : "Deep Ocean"));
        assertTrue(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 16 && z == 16 ? "Plains" : "Deep Ocean"));
        assertTrue(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 1 && z == 1 ? "Frozen River" : "Deep Ocean"));
    }

    @Test
    void doesNotAcceptKelpForestOrCoralReef() {
        // RWG2's shallow-sea biomes from Biomes O' Plenty are typed WATER and name no ocean (decision log D32).
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 2 && z == 2 ? "Kelp Forest" : "Deep Ocean"));
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> x == 14 && z == 2 ? "Coral Reef" : "Deep Ocean"));
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> "Kelp Forest"));
    }

    @Test
    void nullNamesFail() {
        assertFalse(MonumentGrid.siteAccepts(0, 0, (x, z) -> null));
    }

    @Test
    void footprintIs58BlocksAroundTheCenter() {
        BlockRect box = MonumentGrid.footprint(2, -3);
        assertEquals(new BlockRect(32 + 8 - 29, -48 + 8 - 29, 32 + 8 + 28, -48 + 8 + 28), box);
        assertEquals(58, box.maxX() - box.minX() + 1);
        assertEquals(0, box.minChunkX());
        assertEquals(4, box.maxChunkX());
    }
}
