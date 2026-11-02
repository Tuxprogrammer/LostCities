package com.tuxprogrammer.lostcities.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Roguelike Dungeons' grid and candidate math against a transcription of the mod's own steps (Dungeon.java#L51-L66,
 * #L112-L154, #L214-L225 at 1.6.6-GTNH) and FML's generator seeding, with the pack's settings.
 */
class RoguelikeGridTest {

    private static final long SEED = 4242L;

    private static RoguelikeGrid grid() {
        return new RoguelikeGrid(SEED, RoguelikeSettings.pack(), new Random());
    }

    /** The mod's own cell test, step by step, with vanilla's setRandomSeed on a plain Random. */
    private static boolean modCanSpawnInChunk(long worldSeed, int chunkX, int chunkZ, int frequency) {
        int min = Math.max(2, 8 * frequency / 10);
        int max = Math.max(8, 32 * frequency / 10);
        int tempX = chunkX < 0 ? chunkX - (max - 1) : chunkX;
        int tempZ = chunkZ < 0 ? chunkZ - (max - 1) : chunkZ;
        int m = tempX / max;
        int n = tempZ / max;
        Random r = new Random(m * 341873128712L + n * 132897987541L + worldSeed + 10387312);
        m *= max;
        n *= max;
        m += r.nextInt(max - min);
        n += r.nextInt(max - min);
        return chunkX == m && chunkZ == n;
    }

    @Test
    void packGridIs51ChunksWithOffsetsBelow39() {
        RoguelikeSettings pack = RoguelikeSettings.pack();
        assertEquals(51, pack.cellSize());
        assertEquals(12, pack.minSeparation());
        assertEquals(39, pack.offsetBound());
    }

    @Test
    void cellsRoundTowardNegativeInfinity() {
        RoguelikeGrid grid = grid();
        assertEquals(0, grid.cellOf(0));
        assertEquals(0, grid.cellOf(50));
        assertEquals(1, grid.cellOf(51));
        assertEquals(-1, grid.cellOf(-1));
        assertEquals(-1, grid.cellOf(-51));
        assertEquals(-2, grid.cellOf(-52));
    }

    @Test
    void dungeonChunksMatchTheModsTestEverywhere() {
        RoguelikeGrid grid = grid();
        int found = 0;
        for (int chunkX = -120; chunkX <= 120; chunkX++) {
            for (int chunkZ = -120; chunkZ <= 120; chunkZ++) {
                boolean mod = modCanSpawnInChunk(SEED, chunkX, chunkZ, 16);
                assertEquals(mod, grid.isDungeonChunk(chunkX, chunkZ), chunkX + "," + chunkZ);
                if (mod) {
                    found++;
                }
            }
        }
        // 241 x 241 chunks hold about 22 cells' dungeon chunks.
        assertTrue(found >= 16 && found <= 36, "dungeon chunks found: " + found);
    }

    @Test
    void dungeonChunkLiesInItsCellsFirst39Chunks() {
        RoguelikeGrid grid = grid();
        for (int cellX = -4; cellX <= 4; cellX++) {
            for (int cellZ = -4; cellZ <= 4; cellZ++) {
                int[] chunk = grid.dungeonChunk(cellX, cellZ);
                assertTrue(chunk[0] >= cellX * 51 && chunk[0] < cellX * 51 + 39);
                assertTrue(chunk[1] >= cellZ * 51 && chunk[1] < cellZ * 51 + 39);
                assertEquals(cellX, grid.cellOf(chunk[0]));
                assertEquals(cellZ, grid.cellOf(chunk[1]));
            }
        }
    }

    @Test
    void fmlSeedFollowsGameRegistry() {
        Random fml = new Random(SEED);
        long xSeed = fml.nextLong() >> 2 + 1L;
        long zSeed = fml.nextLong() >> 2 + 1L;
        assertEquals((xSeed * 17 + zSeed * -9) ^ SEED, grid().fmlChunkSeed(17, -9));
    }

    @Test
    void candidatesAreTheModsDraws() {
        RoguelikeGrid grid = grid();
        int[][] candidates = grid.candidates(12, -30);
        assertEquals(RoguelikeGrid.CANDIDATES, candidates.length);
        Random rand = new Random(grid.fmlChunkSeed(12, -30));
        int x = 12 * 16 + 4;
        int z = -30 * 16 + 4;
        for (int[] candidate : candidates) {
            int distance = 40 + rand.nextInt(100 - 40);
            double angle = rand.nextDouble() * 2 * Math.PI;
            assertArrayEquals(
                new int[] { x + (int) (Math.cos(angle) * distance), z + (int) (Math.sin(angle) * distance) },
                candidate);
            double reach = Math.hypot(candidate[0] - x, candidate[1] - z);
            assertTrue(reach >= 38 && reach < 100, "reach " + reach);
        }
    }

    @Test
    void candidatesDoNotDependOnAnEarlierQuery() {
        RoguelikeGrid grid = grid();
        int[][] first = grid.candidates(3, 4);
        grid.candidates(100, -7);
        grid.dungeonChunk(5, 5);
        assertArrayEquals(first, grid.candidates(3, 4));
    }

    @Test
    void anotherSeedMovesTheGrid() {
        RoguelikeGrid a = grid();
        RoguelikeGrid b = new RoguelikeGrid(SEED + 1, RoguelikeSettings.pack(), new Random());
        boolean differs = false;
        for (int cell = -3; cell <= 3 && !differs; cell++) {
            differs = !java.util.Arrays.equals(a.dungeonChunk(cell, cell), b.dungeonChunk(cell, cell));
        }
        assertTrue(differs);
        assertFalse(a.fmlChunkSeed(1, 1) == b.fmlChunkSeed(1, 1));
    }
}
