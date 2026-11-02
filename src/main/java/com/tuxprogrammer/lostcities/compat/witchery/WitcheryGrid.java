package com.tuxprogrammer.lostcities.compat.witchery;

import java.util.Random;

/**
 * Witchery's structure grid, reimplemented from its contract: which chunk of each grid cell may hold a Witchery
 * structure. The biome test and the footprint are not here; {@link WitcheryOracle} adds them.
 *
 * <p>
 * The cell side is {@code 8 + clamp(WorldGenFrequency, 1, 64)} chunks (20 in the pack, {@code WorldGenFrequency=12}).
 * The cell of a chunk is {@code floor(chunk / cell)}: Witchery shifts a negative chunk by {@code cell − 1} before
 * Java's truncating division. The cell's {@code Random} is the one {@code World.setRandomSeed(cellX, cellZ, 10387312)}
 * returns, {@code cellX × 341873128712 + cellZ × 132897987541 + worldSeed + 10387312}; the candidate chunk is
 * {@code cell × side + nextInt(side − 8)} on each axis, X drawn first. Witchery reseeds the world's own
 * {@code world.rand} for this; the oracle never does and draws from a {@code Random} of its own (coexistence.md, "Order
 * and determinism at the seams").
 *
 * <p>
 * Decompiled: witchery-1.7.10-0.24.1.jar (GTNH 2.9.0), {@code com.emoniph.witchery.worldgen.WitcheryWorldGenerator}
 * constructor and {@code #nonInRange}; inferred from decompiled bytecode and may be wrong. Third-party decompile cited
 * by the spec:
 * https://github.com/GTNH-Museum/Witchery/blob/4b7a98b60de7901e3503247016aa90c90099d0ce/src/main/java/com/emoniph/witchery/worldgen/WitcheryWorldGenerator.java#L98-L119.
 * The seed formula is vanilla 1.7.10's {@code World.setRandomSeed}.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class WitcheryGrid {

    /** The salt Witchery passes to {@code World.setRandomSeed}. */
    public static final int SALT = 10387312;
    /** {@code World.setRandomSeed}'s multiplier of the first coordinate. */
    public static final long SEED_MULTIPLIER_X = 341873128712L;
    /** {@code World.setRandomSeed}'s multiplier of the second coordinate. */
    public static final long SEED_MULTIPLIER_Z = 132897987541L;
    /** The chunks at the high end of each cell that never hold a candidate ({@code field_82666_h}). */
    public static final int SEPARATION = 8;
    /** The cell side when {@code WorldGenFrequency} is 12, as the pack ships it. */
    public static final int PACK_CELL_SIZE = 20;

    private final long worldSeed;
    private final int cellSize;
    private final int separation;

    /**
     * Creates the grid of a world.
     *
     * @param worldSeed  the world seed
     * @param cellSize   the cell side in chunks, more than {@code separation}
     * @param separation the chunks at each cell's high end that hold no candidate
     */
    public WitcheryGrid(long worldSeed, int cellSize, int separation) {
        if (cellSize <= separation || separation < 0) {
            throw new IllegalArgumentException("cell " + cellSize + " must exceed separation " + separation);
        }
        this.worldSeed = worldSeed;
        this.cellSize = cellSize;
        this.separation = separation;
    }

    /**
     * Returns the cell side Witchery derives from its {@code WorldGenFrequency}.
     *
     * @param frequency the config value
     * @return {@code 8 + clamp(frequency, 1, 64)}
     */
    public static int cellSizeFor(int frequency) {
        return SEPARATION + Math.max(Math.min(frequency, 64), 1);
    }

    /**
     * Returns the seed {@code World.setRandomSeed(x, z, salt)} gives {@code world.rand}, computed without touching it.
     *
     * @param worldSeed the world seed
     * @param x         the first coordinate (a cell X)
     * @param z         the second coordinate (a cell Z)
     * @param salt      the salt
     * @return the seed
     */
    public static long worldRandomSeed(long worldSeed, int x, int z, int salt) {
        return x * SEED_MULTIPLIER_X + z * SEED_MULTIPLIER_Z + worldSeed + salt;
    }

    /** Returns the cell side, in chunks. */
    public int cellSize() {
        return cellSize;
    }

    /**
     * Returns the cell of a chunk coordinate, exactly as Witchery computes it.
     *
     * @param chunk a chunk X or Z
     * @return the cell X or Z
     */
    public int cellOf(int chunk) {
        int shifted = chunk < 0 ? chunk - (cellSize - 1) : chunk;
        return shifted / cellSize;
    }

    /**
     * Returns the one chunk of a cell that may hold a structure.
     *
     * @param cellX the cell X
     * @param cellZ the cell Z
     * @return {@code {chunkX, chunkZ}}
     */
    public int[] candidateChunk(int cellX, int cellZ) {
        Random random = new Random(worldRandomSeed(worldSeed, cellX, cellZ, SALT));
        int x = cellX * cellSize + random.nextInt(cellSize - separation);
        int z = cellZ * cellSize + random.nextInt(cellSize - separation);
        return new int[] { x, z };
    }

    /**
     * Returns whether a chunk is its cell's candidate.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether Witchery tries a structure here
     */
    public boolean isCandidate(int chunkX, int chunkZ) {
        int[] candidate = candidateChunk(cellOf(chunkX), cellOf(chunkZ));
        return candidate[0] == chunkX && candidate[1] == chunkZ;
    }
}
