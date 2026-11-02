package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Random;

/**
 * VillageNames' village grid, reimplemented from its source: which chunk of each grid cell may start a village, and
 * the village-free radius. The biome test and the plan are not here; {@link VillageOracle} adds them.
 *
 * <p>
 * The grid cell is {@code floor(chunk / maxSpacing)} on each axis (VillageNames shifts a negative chunk by
 * {@code maxSpacing - 1} before Java's truncating division). The cell's {@code Random} is the one
 * {@code World.setRandomSeed(cellX, cellZ, 10387312)} returns: {@code cellX × 341873128712 + cellZ × 132897987541 +
 * worldSeed + 10387312}. The start chunk is {@code cell × maxSpacing + nextInt(maxSpacing - minSpacing)}, X drawn
 * first. VillageNames reseeds the world's own {@code world.rand} to do this; the oracle never does, and draws from a
 * {@code Random} of its own (coexistence.md, "Order and determinism at the seams").
 *
 * <p>
 * Source: VillageNames 4.5.21-GTNH,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/MapGenVillageVN.java#L105-L155
 * ({@code canSpawnStructureAtCoords}); the seed formula is vanilla 1.7.10's {@code World.setRandomSeed}.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class VillageGrid {

    /** The salt VillageNames passes to {@code World.setRandomSeed} for villages. */
    public static final int VILLAGE_SALT = 10387312;
    /** {@code World.setRandomSeed}'s multiplier of the first coordinate. */
    public static final long SEED_MULTIPLIER_X = 341873128712L;
    /** {@code World.setRandomSeed}'s multiplier of the second coordinate. */
    public static final long SEED_MULTIPLIER_Z = 132897987541L;

    private final long worldSeed;
    private final int maxSpacing;
    private final int minSpacing;
    private final int noVillagesRadius;

    /**
     * Creates the grid of a world.
     *
     * @param worldSeed the world seed
     * @param settings  VillageNames' settings
     */
    public VillageGrid(long worldSeed, VillageNamesSettings settings) {
        this.worldSeed = worldSeed;
        this.maxSpacing = settings.maxSpacing();
        this.minSpacing = settings.minSpacing();
        this.noVillagesRadius = settings.noVillagesRadius();
    }

    /**
     * Returns the seed {@code World.setRandomSeed(x, z, salt)} gives {@code world.rand}, computed without touching it.
     *
     * @param worldSeed the world seed
     * @param x         the first coordinate (a grid cell X)
     * @param z         the second coordinate (a grid cell Z)
     * @param salt      the generator's salt
     * @return the seed
     */
    public static long worldRandomSeed(long worldSeed, int x, int z, int salt) {
        return x * SEED_MULTIPLIER_X + z * SEED_MULTIPLIER_Z + worldSeed + salt;
    }

    /** Returns the cell side, in chunks. */
    public int cellSize() {
        return maxSpacing;
    }

    /**
     * Returns the grid cell of a chunk coordinate, exactly as VillageNames computes it.
     *
     * @param chunk a chunk X or Z
     * @return the cell X or Z
     */
    public int cellOf(int chunk) {
        int shifted = chunk < 0 ? chunk - (maxSpacing - 1) : chunk;
        return shifted / maxSpacing;
    }

    /**
     * Returns the one chunk of a grid cell that may start a village.
     *
     * @param cellX the cell X
     * @param cellZ the cell Z
     * @return {@code {chunkX, chunkZ}}
     */
    public int[] startChunk(int cellX, int cellZ) {
        Random random = new Random(worldRandomSeed(worldSeed, cellX, cellZ, VILLAGE_SALT));
        int x = cellX * maxSpacing + random.nextInt(maxSpacing - minSpacing);
        int z = cellZ * maxSpacing + random.nextInt(maxSpacing - minSpacing);
        return new int[] { x, z };
    }

    /**
     * Returns whether a chunk is its cell's start chunk and outside the village-free radius: VillageNames' test before
     * the biome test.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether a village may start in this chunk, biome permitting
     */
    public boolean isCandidate(int chunkX, int chunkZ) {
        if (insideVillageFreeRadius(chunkX, chunkZ)) {
            return false;
        }
        int[] start = startChunk(cellOf(chunkX), cellOf(chunkZ));
        return start[0] == chunkX && start[1] == chunkZ;
    }

    /**
     * Returns whether a chunk lies inside the village-free radius, with VillageNames' {@code int} arithmetic: beyond
     * 46,340 chunks from an axis the squares overflow, and VillageNames then denies villages there whenever the sum
     * is negative. The oracle predicts what VillageNames does, so the overflow is kept.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether VillageNames denies a village here before looking at the grid
     */
    public boolean insideVillageFreeRadius(int chunkX, int chunkZ) {
        return chunkX * chunkX + chunkZ * chunkZ < noVillagesRadius * noVillagesRadius;
    }
}
