package com.tuxprogrammer.lostcities.compat.roguelike;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * Roguelike Dungeons' site test ({@code Dungeon.validLocation}) estimated from the biome provider and the height
 * oracle instead of the world's blocks, so a dungeon's site is known before any chunk exists (0006 §2).
 *
 * <p>
 * The mod's test, at a point {@code (x, z)}:
 * <ol>
 * <li>the biome there has none of the dictionary types river, beach, mushroom, ocean;</li>
 * <li>the block at the upper limit (Y 100) is air;</li>
 * <li>scanning down from there, the first "valid ground" block G (anything solid except wood, water, cactus, snow,
 * grass, gourds, leaves and plants) is at or above the lower limit (Y 60), and no water is passed on the way;</li>
 * <li>no block of the 9 × 9 layer at G + 4 around the point is valid ground;</li>
 * <li>at most 8 of the 81 blocks of the 9 × 9 layer at G − 3 are not valid ground.</li>
 * </ol>
 * The estimate reads each column as RWG builds it: solid up to the oracle height {@code h} (its top block at
 * {@code h − 1}), a grass top not counting as ground, so a column's valid top is {@code h − 2} under grass and
 * {@code h − 1} otherwise; water above every column below sea level. It cannot see trees taller than the upper limit,
 * caves under the lower layer, lakes or overhangs, so it is right where the land is plain and the prediction is a
 * {@code CANDIDATE} (coexistence.md § Roguelike Dungeons).
 *
 * <p>
 * Source: Roguelike Dungeons 1.6.6-GTNH,
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L165-L212
 * and
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/worldgen/WorldEditor.java#L134-L150.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class SiteRules {

    /** Why a point fails, or {@link #PASS}. */
    public enum Verdict {
        /** Every rule holds. */
        PASS,
        /** The biome is a river, beach, mushroom or ocean type. */
        BIOME,
        /** The column is solid at the upper limit. */
        NO_AIR,
        /** The scan meets water before ground. */
        WATER,
        /** The valid ground is below the lower limit. */
        TOO_LOW,
        /** Valid ground reaches the layer four blocks above the site's ground. */
        CROWDED,
        /** More than eight blocks of the layer three blocks below the ground are not ground. */
        HOLLOW
    }

    /** Half the side of the two 9 × 9 layers, blocks. */
    public static final int LAYER_RADIUS = 4;
    /** The layer above the ground that must hold no ground, blocks above G. */
    public static final int ABOVE = 4;
    /** The layer below the ground that must be mostly ground, blocks below G. */
    public static final int BELOW = 3;
    /** The most blocks of the lower layer that may be something other than ground. */
    public static final int MAX_GAPS = 8;
    /** Sea level on RWG worlds: a column whose height is below it has water over it. */
    public static final int SEA_LEVEL = 63;

    private final RoguelikeSettings settings;
    private final TerrainHeightOracle heights;
    private final RoguelikeTerrain terrain;

    /**
     * Creates the rules over a world's inputs.
     *
     * @param settings Roguelike's settings (the Y limits)
     * @param heights  the height oracle
     * @param terrain  the biome-provider facts
     */
    public SiteRules(RoguelikeSettings settings, TerrainHeightOracle heights, RoguelikeTerrain terrain) {
        this.settings = settings;
        this.heights = heights;
        this.terrain = terrain;
    }

    /**
     * Returns the Y of a column's first valid ground block from the top, as the estimate reads it.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return block Y
     */
    public int validTop(int blockX, int blockZ) {
        return heights.height(blockX, blockZ) - 1 - (terrain.grassTop(blockX, blockZ) ? 1 : 0);
    }

    /**
     * Tests a point.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the verdict
     */
    public Verdict test(int blockX, int blockZ) {
        if (terrain.biomeRejects(blockX, blockZ)) {
            return Verdict.BIOME;
        }
        int height = heights.height(blockX, blockZ);
        if (height > settings.upperLimit()) {
            return Verdict.NO_AIR;
        }
        // The scan meets the sea's top block (Y 62) first; the mod checks the lower limit before water at each step.
        if (height < SEA_LEVEL) {
            return SEA_LEVEL - 1 < settings.lowerLimit() ? Verdict.TOO_LOW : Verdict.WATER;
        }
        int ground = height - 1 - (terrain.grassTop(blockX, blockZ) ? 1 : 0);
        if (ground < settings.lowerLimit()) {
            return Verdict.TOO_LOW;
        }
        for (int dx = -LAYER_RADIUS; dx <= LAYER_RADIUS; dx++) {
            for (int dz = -LAYER_RADIUS; dz <= LAYER_RADIUS; dz++) {
                if (validTop(blockX + dx, blockZ + dz) >= ground + ABOVE) {
                    return Verdict.CROWDED;
                }
            }
        }
        int gaps = 0;
        for (int dx = -LAYER_RADIUS; dx <= LAYER_RADIUS; dx++) {
            for (int dz = -LAYER_RADIUS; dz <= LAYER_RADIUS; dz++) {
                if (validTop(blockX + dx, blockZ + dz) < ground - BELOW && ++gaps > MAX_GAPS) {
                    return Verdict.HOLLOW;
                }
            }
        }
        return Verdict.PASS;
    }
}
