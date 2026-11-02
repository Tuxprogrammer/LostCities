package com.tuxprogrammer.lostcities.compat.lootgames;

import java.util.Random;

/**
 * LootGames' site test, {@code LootGamesWorldGen.canSpawnInChunk_v3}, as a pure function of the world seed, the rhomb
 * size and the chunk, reproduced exactly, Java's integer semantics included:
 *
 * <ol>
 * <li>{@code xc = 2 × chunkX + chunkZ}, {@code zc = 2 × chunkZ + chunkX} (the rhomb coordinates), and
 * {@code span = 2 × rhombSize}.</li>
 * <li>A {@code java.util.Random} of the generator's own is seeded with
 * {@code worldSeed + xc / span + ((zc / span) << 14)}: truncating division and an {@code int} shift, as written.</li>
 * <li>{@code p1 = 3 + nextInt(span − 3)}, {@code p2 = 3 + nextInt(span − 3)}.</li>
 * <li>With {@code mx}, {@code mz} the floor remainders of {@code xc}, {@code zc} by {@code span}: the chunk spawns
 * when both are at least 3 and {@code (mx, mz)} is {@code (p1, p2)}, {@code (p1 + 1, p2)} or
 * {@code (p1 + 1, p2 + 1)}.</li>
 * </ol>
 * The truncating division and the floor remainder disagree below 0, so near the axes a cell's seed serves two rhomb
 * cells; the oracle inherits that, since it is what LootGames does.
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/eu/usrv/legacylootgames/worldgen/LootGamesWorldGen.java#L69-L100
 *
 * <p>
 * Not thread-safe (one {@code Random}); the oracle synchronizes. Side: both (plain Java).
 */
public final class RhombGrid {

    /** Lowest rhomb remainder a site may have. */
    static final int MIN_POSITION = 3;
    /** The shift that packs the Z cell into the seed. */
    private static final int Z_SHIFT = 14;

    private final long worldSeed;
    private final int span;
    private final Random random = new Random();

    /**
     * Creates the grid.
     *
     * @param worldSeed the world seed
     * @param rhombSize the dimension's rhomb size, 5 to 100
     */
    public RhombGrid(long worldSeed, int rhombSize) {
        this.worldSeed = worldSeed;
        this.span = rhombSize * 2;
    }

    /**
     * Returns whether LootGames tries a dungeon in a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} when the chunk is a site
     */
    public boolean spawnsIn(int chunkX, int chunkZ) {
        int xc = chunkX * 2 + chunkZ;
        int zc = chunkZ * 2 + chunkX;
        random.setSeed(worldSeed + (xc / span) + ((zc / span) << Z_SHIFT));
        int pos1 = MIN_POSITION + random.nextInt(span - MIN_POSITION);
        int pos2 = MIN_POSITION + random.nextInt(span - MIN_POSITION);
        int modX = Math.floorMod(xc, span);
        int modZ = Math.floorMod(zc, span);
        if (modX < MIN_POSITION || modZ < MIN_POSITION) {
            return false;
        }
        return modX == pos1 && modZ == pos2 || modX == pos1 + 1 && (modZ == pos2 || modZ == pos2 + 1);
    }
}
