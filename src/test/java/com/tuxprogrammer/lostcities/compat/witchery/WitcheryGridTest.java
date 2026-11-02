package com.tuxprogrammer.lostcities.compat.witchery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Witchery's grid: the cell side, the cell of negative chunks, and the candidate chunk's seed and range. */
class WitcheryGridTest {

    private static final long SEED = 4242L;

    @Test
    void cellSideFollowsTheFrequencyClamped() {
        assertEquals(20, WitcheryGrid.cellSizeFor(12), "the pack's WorldGenFrequency=12");
        assertEquals(9, WitcheryGrid.cellSizeFor(0), "clamped up to 1");
        assertEquals(72, WitcheryGrid.cellSizeFor(500), "clamped down to 64");
    }

    @Test
    void negativeChunksFloorIntoTheirCell() {
        WitcheryGrid grid = new WitcheryGrid(SEED, 20, 8);
        assertEquals(0, grid.cellOf(0));
        assertEquals(0, grid.cellOf(19));
        assertEquals(1, grid.cellOf(20));
        assertEquals(-1, grid.cellOf(-1));
        assertEquals(-1, grid.cellOf(-20));
        assertEquals(-2, grid.cellOf(-21));
    }

    @Test
    void candidateComesFromTheWorldSetRandomSeedFormula() {
        WitcheryGrid grid = new WitcheryGrid(SEED, 20, 8);
        for (int cellX = -3; cellX <= 3; cellX++) {
            for (int cellZ = -3; cellZ <= 3; cellZ++) {
                // World.setRandomSeed(x, z, salt): rand.setSeed(x * 341873128712 + z * 132897987541 + seed + salt).
                Random reference = new Random(cellX * 341873128712L + cellZ * 132897987541L + SEED + 10387312L);
                int expectedX = cellX * 20 + reference.nextInt(12);
                int expectedZ = cellZ * 20 + reference.nextInt(12);
                int[] candidate = grid.candidateChunk(cellX, cellZ);
                assertEquals(expectedX, candidate[0]);
                assertEquals(expectedZ, candidate[1]);
                assertEquals(cellX, grid.cellOf(candidate[0]), "the candidate lies in its own cell");
                assertEquals(cellZ, grid.cellOf(candidate[1]));
                assertTrue(grid.isCandidate(candidate[0], candidate[1]));
                assertFalse(grid.isCandidate(candidate[0] + 1, candidate[1]), "one candidate per cell");
            }
        }
    }

    @Test
    void cellMustExceedTheSeparation() {
        assertThrows(IllegalArgumentException.class, () -> new WitcheryGrid(SEED, 8, 8));
    }
}
