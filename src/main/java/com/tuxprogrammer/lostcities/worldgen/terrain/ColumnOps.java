package com.tuxprogrammer.lostcities.worldgen.terrain;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: TERR-06, TERR-23, TERR-24

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;

/**
 * Column operations of upstream's terrain code: lowering a column's surface ({@code moveDown}), raising it
 * ({@code moveUp}), clearing a range ({@code clearRange}) and filling the empty run under a street
 * ({@code fillToBedrockStreetBlock}). City leveling, the blend and soft-edge strips are built from these.
 *
 * <p>
 * Spec: docs/original-spec/03-terrain/city-chunk-terrain.md ("Terrain preparation"), terrain-blend.md; deviations in
 * docs/new-spec/03-terrain/index.md. Fixes:
 * <ul>
 * <li>TERR-23: when {@code moveDown} lowers a column whose surface lay under liquid, the cleared blocks at or below
 * the water level minus one become that liquid, so the water stays joined to the lowered bed (P03-4, D31);</li>
 * <li>TERR-06: {@code clearRange} with water fills liquid up to the water level minus one and air from the water level
 * up, so no row is skipped;</li>
 * <li>TERR-24: every bound is a {@link WorldLimits} constant for the 1.7.10 range.</li>
 * </ul>
 *
 * <p>
 * Each operation reads and writes only the given column of one chunk buffer and allocates nothing.
 * Side: both (plain Java).
 */
public final class ColumnOps {

    /** Returned by {@link #moveDown} and {@link #moveUp} when they changed nothing (upstream's sentinel). */
    public static final int NOT_TOUCHED = Short.MIN_VALUE;

    /** Surface layers {@code moveDown} keeps when it lowers a column. */
    public static final int KEPT_LAYERS = 6;

    private ColumnOps() {}

    /**
     * Lowers a column so its terrain ends just below {@code height}: finds the real surface, looking through air,
     * liquid and foliage, keeps its top {@link #KEPT_LAYERS} layers, clears everything from the highest foliage (or
     * the surface) down to {@code height}, and writes the kept layers from {@code height − 1} downward.
     *
     * @param buffer     the chunk
     * @param classes    the block classes
     * @param x          local X
     * @param z          local Z
     * @param height     the first Y that must end up clear; the new top is {@code height − 1}
     * @param top        the exclusive top of the scan, usually {@link WorldLimits#BUILD_HEIGHT}
     * @param waterLevel the water level; with a submerged surface, cleared blocks below it become liquid (TERR-23)
     * @return the new top Y of the column, or {@link #NOT_TOUCHED} when the surface was already low enough
     */
    public static int moveDown(ChunkBuffer buffer, BlockClassifier classes, int x, int z, int height, int top,
        int waterLevel) {
        // Nothing but air lies above the buffer's top section, so the scan starts there.
        int y = Math.min(Math.min(top, WorldLimits.BUILD_HEIGHT) - 1, buffer.topY());
        int highestFoliage = NOT_TOUCHED;
        BlockRef liquidAbove = null;
        while (y > height) {
            BlockRef block = buffer.get(x, y, z);
            if (!classes.isFoliageOrEmpty(block)) {
                break;
            }
            if (classes.isLiquid(block)) {
                liquidAbove = block;
            } else if (!block.isAir()) {
                highestFoliage = Math.max(highestFoliage, y);
                liquidAbove = null;
            } else {
                liquidAbove = null;
            }
            y--;
        }
        if (y <= height) {
            return NOT_TOUCHED;
        }
        int surface = y;
        // Keep six layers from the real surface down (upstream fixed the vine-into-soil copy the same way).
        BlockRef k0 = null, k1 = null, k2 = null, k3 = null, k4 = null, k5 = null;
        int kept = 0;
        for (int sourceY = surface; sourceY >= height && kept < KEPT_LAYERS; sourceY--) {
            BlockRef block = buffer.get(x, sourceY, z);
            switch (kept) {
                case 0 -> k0 = block;
                case 1 -> k1 = block;
                case 2 -> k2 = block;
                case 3 -> k3 = block;
                case 4 -> k4 = block;
                default -> k5 = block;
            }
            kept++;
        }
        int clearFrom = highestFoliage == NOT_TOUCHED ? surface : highestFoliage;
        for (int clearY = clearFrom; clearY >= height; clearY--) {
            // TERR-23: a submerged surface keeps its water down to the lowered bed.
            boolean flood = liquidAbove != null && clearY <= waterLevel - 1;
            buffer.set(x, clearY, z, flood ? liquidAbove : BlockRef.AIR);
        }
        int writeY = height - 1;
        int newTop = writeY;
        for (int i = 0; i < kept && writeY > WorldLimits.MIN_Y; i++) {
            BlockRef block = switch (i) {
                case 0 -> k0;
                case 1 -> k1;
                case 2 -> k2;
                case 3 -> k3;
                case 4 -> k4;
                default -> k5;
            };
            buffer.set(x, writeY, z, block);
            writeY--;
        }
        return newTop;
    }

    /**
     * Raises a column so its terrain reaches {@code height}: finds the first solid block at or below {@code height}
     * and copies the column upward by the difference, from that block down until an air gap or bedrock in the
     * source, so the shifted part carries its caves along as upstream's does.
     *
     * @param buffer  the chunk
     * @param classes the block classes
     * @param x       local X
     * @param z       local Z
     * @param height  the Y the new top must reach
     * @return the highest source Y that was copied, or {@link #NOT_TOUCHED} when the column already reached it
     */
    public static int moveUp(ChunkBuffer buffer, BlockClassifier classes, int x, int z, int height) {
        int y = Math.min(height, WorldLimits.MAX_Y);
        while (y > WorldLimits.MIN_Y && classes.isFoliageOrEmpty(buffer.get(x, y, z))) {
            y--;
        }
        if (y >= height) {
            return NOT_TOUCHED;
        }
        int touched = NOT_TOUCHED;
        int target = Math.min(height, WorldLimits.MAX_Y);
        for (int source = y; source > WorldLimits.MIN_Y && target >= WorldLimits.MIN_Y; source--) {
            BlockRef block = buffer.get(x, source, z);
            if (block.isAir() || (classes.flags(block) & BlockClassifier.BEDROCK) != 0) {
                break;
            }
            if (touched == NOT_TOUCHED) {
                touched = source;
            }
            buffer.set(x, target, z, block);
            target--;
        }
        return touched;
    }

    /**
     * Clears a vertical range, optionally leaving liquid below the water level (a drowned city). Fixes TERR-06:
     * liquid from {@code fromY} to {@code waterLevel − 1}, air from {@code waterLevel} to {@code toY − 1}.
     *
     * @param buffer     the chunk
     * @param x          local X
     * @param z          local Z
     * @param fromY      first Y, inclusive
     * @param toY        last Y, exclusive
     * @param withWater  whether to leave liquid below the water level
     * @param waterLevel the water level
     * @param liquid     the liquid block
     */
    public static void clearRange(ChunkBuffer buffer, int x, int z, int fromY, int toY, boolean withWater,
        int waterLevel, BlockRef liquid) {
        int from = Math.max(fromY, WorldLimits.MIN_Y);
        int to = Math.min(toY, WorldLimits.BUILD_HEIGHT);
        for (int y = from; y < to; y++) {
            buffer.set(x, y, z, withWater && y < waterLevel ? liquid : BlockRef.AIR);
        }
    }

    /**
     * Fills the empty run directly under a street: from {@code startY} down, air, water and lava become
     * {@code base} until the first other block. A cave further down, under solid rock, stays.
     *
     * @param buffer  the chunk
     * @param classes the block classes
     * @param x       local X
     * @param z       local Z
     * @param startY  the first Y to fill, usually the city ground minus one
     * @param base    the base block
     */
    public static void fillEmptyBelow(ChunkBuffer buffer, BlockClassifier classes, int x, int z, int startY,
        BlockRef base) {
        for (int y = Math.min(startY, WorldLimits.MAX_Y); y >= WorldLimits.MIN_Y; y--) {
            if (!classes.isEmpty(buffer.get(x, y, z))) {
                return;
            }
            buffer.set(x, y, z, base);
        }
    }

    /**
     * Writes one block over a vertical range.
     *
     * @param buffer the chunk
     * @param x      local X
     * @param z      local Z
     * @param fromY  first Y, inclusive
     * @param toY    last Y, exclusive
     * @param block  the block
     */
    public static void fill(ChunkBuffer buffer, int x, int z, int fromY, int toY, BlockRef block) {
        int from = Math.max(fromY, WorldLimits.MIN_Y);
        int to = Math.min(toY, WorldLimits.BUILD_HEIGHT);
        for (int y = from; y < to; y++) {
            buffer.set(x, y, z, block);
        }
    }

    /**
     * Returns the top Y of a column that is not air or liquid, looking through foliage too when asked.
     *
     * @param buffer         the chunk
     * @param classes        the block classes
     * @param x              local X
     * @param z              local Z
     * @param throughFoliage whether plants and logs count as empty
     * @return the Y, or {@code MIN_Y − 1} for an empty column
     */
    public static int surface(ChunkBuffer buffer, BlockClassifier classes, int x, int z, boolean throughFoliage) {
        for (int y = Math.min(WorldLimits.MAX_Y, buffer.topY()); y >= WorldLimits.MIN_Y; y--) {
            BlockRef block = buffer.get(x, y, z);
            if (throughFoliage ? !classes.isFoliageOrEmpty(block) : !classes.isEmpty(block)) {
                return y;
            }
        }
        return WorldLimits.MIN_Y - 1;
    }
}
