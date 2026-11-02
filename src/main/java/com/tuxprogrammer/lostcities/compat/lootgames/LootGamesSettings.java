package com.tuxprogrammer.lostcities.compat.lootgames;

/**
 * The LootGames values that decide where its puzzle dungeons go in one dimension: whether dungeon generation is on
 * ({@code disable_dungeon_gen} false), whether the dimension is whitelisted, and its rhomb size
 * ({@code per_dimension_configs}, default {@code 0| 20}; the pack ships no general config, so the defaults hold).
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/ru/timeconqueror/lootgames/common/config/ConfigGeneral.java#L62-L137
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class LootGamesSettings {

    /** Settings under which no dungeon generates. */
    public static final LootGamesSettings OFF = new LootGamesSettings(false, 0, 0);

    private final boolean enabled;
    private final int dimension;
    private final int rhombSize;

    private LootGamesSettings(boolean enabled, int dimension, int rhombSize) {
        this.enabled = enabled;
        this.dimension = dimension;
        this.rhombSize = rhombSize;
    }

    /**
     * Builds the settings of one dimension.
     *
     * @param disabled  LootGames' {@code disable_dungeon_gen}
     * @param dimension the dimension ID
     * @param rhombSize the dimension's rhomb size, or -1 when it is not whitelisted
     * @return the settings; {@link #OFF} when no dungeon generates there
     */
    public static LootGamesSettings of(boolean disabled, int dimension, int rhombSize) {
        if (disabled || rhombSize <= 0) {
            return OFF;
        }
        return new LootGamesSettings(true, dimension, rhombSize);
    }

    /** Returns whether dungeons generate. */
    public boolean enabled() {
        return enabled;
    }

    /** Returns the dimension. */
    public int dimension() {
        return dimension;
    }

    /** Returns the rhomb size. */
    public int rhombSize() {
        return rhombSize;
    }

    /** Returns one line for the pinned profile snapshot. */
    public String summary() {
        return enabled ? "on rhomb=" + rhombSize : "off";
    }

    @Override
    public String toString() {
        return summary();
    }
}
