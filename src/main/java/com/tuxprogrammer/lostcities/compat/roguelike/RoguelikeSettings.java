package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Roguelike Dungeons settings the structure oracle reads: whether dungeons spawn naturally, the spawn frequency
 * that sets the grid, the Y limits of the site test and the dimension lists. Immutable and plain Java, so the grid and
 * the site rules are testable without Roguelike Dungeons; {@code RoguelikeConfig} fills it from the mod's own
 * {@code RogueConfig} at runtime.
 *
 * <p>
 * Source (Roguelike Dungeons 1.6.6-GTNH, the version GTNH 2.9.0 ships):
 * <ul>
 * <li>the grid: {@code min = max(2, 8 × frequency / 10)}, {@code max = max(8, 32 × frequency / 10)} chunks,
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L112-L144;</li>
 * <li>the keys and defaults ({@code doNaturalSpawn} true, {@code spawnFrequency} 10, {@code upperLimit} 100,
 * {@code lowerLimit} 60, {@code dimensionWL} [0], {@code dimensionBL} []),
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/config/RogueConfig.java#L72-L106;</li>
 * <li>the dimension lists are read when a site has passed, by the settings' spawn criteria; a dimension they exclude
 * gets no dungeon,
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/settings/SpawnCriteria.java#L88-L111.</li>
 * </ul>
 * The pack's values ({@code config/roguelike_dungeons/roguelike.cfg} of GTNH 2.9.0): natural spawn on, frequency 16
 * (a 51-chunk grid, offsets below 39), upper limit 100, lower limit 60, whitelist {@code 0}, empty blacklist.
 *
 * <p>
 * Side: both (plain data).
 */
public final class RoguelikeSettings {

    /** Roguelike's default spawn frequency (the pack sets 16). */
    public static final int DEFAULT_FREQUENCY = 10;
    /** Roguelike's default upper limit of the site test, block Y. */
    public static final int DEFAULT_UPPER_LIMIT = 100;
    /** Roguelike's default lower limit of the site test, block Y. */
    public static final int DEFAULT_LOWER_LIMIT = 60;

    private final boolean naturalSpawn;
    private final int frequency;
    private final int upperLimit;
    private final int lowerLimit;
    private final List<Integer> dimensionWhitelist;
    private final List<Integer> dimensionBlacklist;

    /**
     * Creates the settings.
     *
     * @param naturalSpawn       {@code doNaturalSpawn}: whether dungeons generate with the world
     * @param frequency          {@code spawnFrequency}
     * @param upperLimit         {@code upperLimit}, block Y: the site's column must be air here
     * @param lowerLimit         {@code lowerLimit}, block Y: the lowest valid ground
     * @param dimensionWhitelist {@code dimensionWL}; empty allows every dimension the blacklist does not name
     * @param dimensionBlacklist {@code dimensionBL}
     */
    public RoguelikeSettings(boolean naturalSpawn, int frequency, int upperLimit, int lowerLimit,
        List<Integer> dimensionWhitelist, List<Integer> dimensionBlacklist) {
        this.naturalSpawn = naturalSpawn;
        this.frequency = frequency;
        this.upperLimit = upperLimit;
        this.lowerLimit = lowerLimit;
        this.dimensionWhitelist = Collections.unmodifiableList(new ArrayList<>(dimensionWhitelist));
        this.dimensionBlacklist = Collections.unmodifiableList(new ArrayList<>(dimensionBlacklist));
    }

    /** Returns the GTNH 2.9.0 pack's settings. */
    public static RoguelikeSettings pack() {
        return new RoguelikeSettings(
            true,
            16,
            DEFAULT_UPPER_LIMIT,
            DEFAULT_LOWER_LIMIT,
            Collections.singletonList(0),
            Collections.emptyList());
    }

    /** Returns whether dungeons generate with the world at all. */
    public boolean naturalSpawn() {
        return naturalSpawn;
    }

    /** Returns {@code spawnFrequency}. */
    public int frequency() {
        return frequency;
    }

    /** Returns the upper limit of the site test, block Y. */
    public int upperLimit() {
        return upperLimit;
    }

    /** Returns the lower limit of the site test, block Y. */
    public int lowerLimit() {
        return lowerLimit;
    }

    /** Returns the grid's cell size in chunks: {@code max(8, 32 × frequency / 10)}, 51 in the pack. */
    public int cellSize() {
        return Math.max(8, 32 * frequency / 10);
    }

    /** Returns the grid's minimum separation in chunks: {@code max(2, 8 × frequency / 10)}, 12 in the pack. */
    public int minSeparation() {
        return Math.max(2, 8 * frequency / 10);
    }

    /**
     * Returns the bound of the cell offset draw, {@code cellSize − minSeparation} (39 in the pack). Roguelike would throw
     * on a bound below 1; the oracle then predicts nothing.
     */
    public int offsetBound() {
        return cellSize() - minSeparation();
    }

    /**
     * Returns whether dungeons generate in a dimension: natural spawn on, the dimension not blacklisted, and either no
     * whitelist or one that names it.
     *
     * @param dimension the dimension ID
     * @return {@code true} when a passing site gets a dungeon there
     */
    public boolean generatesIn(int dimension) {
        if (!naturalSpawn || offsetBound() < 1 || dimensionBlacklist.contains(dimension)) {
            return false;
        }
        return dimensionWhitelist.isEmpty() || dimensionWhitelist.contains(dimension);
    }

    /** Returns the values as one line for the pinned profile snapshot. */
    public String summary() {
        return "naturalSpawn=" + naturalSpawn
            + ",spawnFrequency="
            + frequency
            + ",upperLimit="
            + upperLimit
            + ",lowerLimit="
            + lowerLimit
            + ",dimensionWL="
            + dimensionWhitelist
            + ",dimensionBL="
            + dimensionBlacklist;
    }

    @Override
    public String toString() {
        return summary();
    }
}
