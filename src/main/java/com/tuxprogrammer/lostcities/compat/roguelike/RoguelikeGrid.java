package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.Random;

/**
 * Roguelike Dungeons' placement math as pure functions of the world seed: which chunk of each grid cell starts a
 * dungeon, and the candidate points around it in the order the mod tries them. Nothing here reads a world.
 *
 * <p>
 * The grid. A chunk is a cell's dungeon chunk when it equals {@code cell × size + nextInt(size − min)} on each axis,
 * the two draws taken from {@code world.setRandomSeed(cellX, cellZ, 10387312)}: vanilla's
 * {@code cellX × 341873128712 + cellZ × 132897987541 + worldSeed + salt}. The live mod reseeds {@code world.rand} for
 * that; this class reseeds a {@code Random} of its own of the same class instead, so {@code world.rand}'s stream, which
 * Witchery reads, is never disturbed. Negative chunks round toward negative infinity
 * ({@code (chunk − (size − 1)) / size}, which is floor division).
 *
 * <p>
 * The candidates. FML hands every {@code IWorldGenerator} a {@code java.util.Random} reseeded with
 * {@code (xSeed × chunkX + zSeed × chunkZ) ^ worldSeed}, where {@code xSeed} and {@code zSeed} are the first two
 * {@code nextLong() >> 3} of {@code new Random(worldSeed)}. Roguelike's generator passes it straight to
 * {@code generateNear} at block {@code (16 × chunkX + 4, 16 × chunkZ + 4)}, which draws, for each of 50 tries, a
 * distance {@code 40 + nextInt(60)} and an angle {@code nextDouble() × 2π}, and offsets the point by
 * {@code (int) (cos × distance)} and {@code (int) (sin × distance)}. The site test draws nothing, so the 50 points
 * are fixed by the seed and the chunk; only which one passes depends on the world.
 *
 * <p>
 * Source: Roguelike Dungeons 1.6.6-GTNH,
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L51-L66
 * (the tries), #L112-L154 (the grid and the origin), #L214-L225 (the point);
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/DungeonGenerator.java#L13-L19
 * (the FML random passed through);
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/worldgen/WorldEditor.java#L82-L84
 * ({@code world.setRandomSeed}). Forge 10.13.4.1614's {@code GameRegistry.generateWorld} and vanilla 1.7.10's
 * {@code World.setRandomSeed} have no pinned public source; read from the pack's server jar with Forge applied. In the
 * pack, BugTorch could replace {@code world.rand}'s class but ships that off ({@code config/bugtorch/mixins.cfg},
 * {@code replaceRandomInWorld=false}); the runtime oracle copies the class of the live {@code world.rand} anyway.
 * Hodgepodge's {@code StdLCG} replaces only map generators' randoms, not these.
 *
 * <p>
 * Not thread-safe (it reuses its randoms): the oracle synchronizes around it. Side: both (plain Java).
 */
public final class RoguelikeGrid {

    /** The salt Roguelike passes to {@code world.setRandomSeed}. */
    public static final int GRID_SALT = 10387312;
    /** How many candidate points {@code generateNear} tries. */
    public static final int CANDIDATES = 50;
    /** The smallest candidate distance from the origin, in blocks. */
    public static final int MIN_DISTANCE = 40;
    /** The bound of the distance draw: distances run from 40 to 99 blocks. */
    public static final int DISTANCE_RANGE = 60;
    /** The origin's offset from the dungeon chunk's corner, in blocks, on both axes. */
    public static final int ORIGIN_OFFSET = 4;

    /** Vanilla {@code setRandomSeed}'s X multiplier. */
    private static final long SEED_X = 341873128712L;
    /** Vanilla {@code setRandomSeed}'s Z multiplier. */
    private static final long SEED_Z = 132897987541L;

    private final long worldSeed;
    private final RoguelikeSettings settings;
    private final Random gridRandom;
    private final Random fmlRandom = new Random();
    private final long fmlXSeed;
    private final long fmlZSeed;

    /**
     * Creates the grid of a world.
     *
     * @param worldSeed  the world seed
     * @param settings   Roguelike's settings
     * @param gridRandom a {@code Random} of the class of the world's {@code world.rand}; reseeded for every cell
     */
    public RoguelikeGrid(long worldSeed, RoguelikeSettings settings, Random gridRandom) {
        this.worldSeed = worldSeed;
        this.settings = settings;
        this.gridRandom = gridRandom;
        Random seeds = new Random(worldSeed);
        this.fmlXSeed = seeds.nextLong() >> 3;
        this.fmlZSeed = seeds.nextLong() >> 3;
    }

    /** Returns the settings. */
    public RoguelikeSettings settings() {
        return settings;
    }

    /**
     * Returns the cell a chunk lies in, on one axis.
     *
     * @param chunk chunk X or Z
     * @return the cell index
     */
    public int cellOf(int chunk) {
        return Math.floorDiv(chunk, settings.cellSize());
    }

    /**
     * Returns a cell's dungeon chunk.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return {@code {chunkX, chunkZ}}
     */
    public int[] dungeonChunk(int cellX, int cellZ) {
        int size = settings.cellSize();
        int bound = Math.max(1, settings.offsetBound());
        gridRandom.setSeed(cellX * SEED_X + cellZ * SEED_Z + worldSeed + GRID_SALT);
        int chunkX = cellX * size + gridRandom.nextInt(bound);
        int chunkZ = cellZ * size + gridRandom.nextInt(bound);
        return new int[] { chunkX, chunkZ };
    }

    /**
     * Returns whether a chunk is its cell's dungeon chunk, as {@code Dungeon.canSpawnInChunk} answers (natural spawn
     * aside).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} when the mod tries a dungeon from this chunk
     */
    public boolean isDungeonChunk(int chunkX, int chunkZ) {
        int[] chunk = dungeonChunk(cellOf(chunkX), cellOf(chunkZ));
        return chunk[0] == chunkX && chunk[1] == chunkZ;
    }

    /**
     * Returns the seed FML gives every world generator's random for a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the seed
     */
    public long fmlChunkSeed(int chunkX, int chunkZ) {
        return fmlXSeed * chunkX + fmlZSeed * chunkZ ^ worldSeed;
    }

    /**
     * Returns the candidate points of a dungeon chunk, in the order the mod tries them.
     *
     * @param chunkX the dungeon chunk's X
     * @param chunkZ the dungeon chunk's Z
     * @return {@value #CANDIDATES} points, each {@code {blockX, blockZ}}
     */
    public int[][] candidates(int chunkX, int chunkZ) {
        fmlRandom.setSeed(fmlChunkSeed(chunkX, chunkZ));
        int originX = chunkX * 16 + ORIGIN_OFFSET;
        int originZ = chunkZ * 16 + ORIGIN_OFFSET;
        int[][] out = new int[CANDIDATES][];
        for (int i = 0; i < CANDIDATES; i++) {
            int distance = MIN_DISTANCE + fmlRandom.nextInt(DISTANCE_RANGE);
            double angle = fmlRandom.nextDouble() * 2 * Math.PI;
            // Math, not StrictMath, as the mod: both run on the same JVM, so they agree bit for bit.
            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);
            out[i] = new int[] { originX + offsetX, originZ + offsetZ };
        }
        return out;
    }
}
