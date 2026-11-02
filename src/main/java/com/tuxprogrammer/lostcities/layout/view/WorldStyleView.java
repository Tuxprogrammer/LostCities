package com.tuxprogrammer.lostcities.layout.view;

import java.util.Random;

/**
 * The layout's read-only view of the profile's world style: its city-style selection by biome, its city-chance
 * multipliers, its multibuilding settings and the world settings layout decisions read. The asset model (M02)
 * implements it over the biome provider; tests use fakes.
 *
 * <p>
 * Spec: docs/original-spec/02-profiles-dimensions/world-styles.md. Side: both.
 */
public interface WorldStyleView {

    /**
     * Picks a city style for a chunk among the world style's {@code citystyles} whose biome filter matches the chunk's
     * main biome.
     *
     * <p>
     * Contract: one {@code random.nextFloat()} when any entry matches, none otherwise; {@code null} only when none
     * matches. The loader requires a selector without a biome filter (P-PROF-8, PROF-12), so a loaded world style
     * always matches.
     *
     * @param random the caller's random
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return a city style name, or {@code null}
     */
    String pickCityStyle(Random random, int chunkX, int chunkZ);

    /**
     * Returns the city-chance multiplier for a chunk's main biome: the first matching rule's, 1 when none matches.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the multiplier
     */
    float cityChanceMultiplier(int chunkX, int chunkZ);

    /** Returns {@code multisettings}. */
    MultiSettings multiSettings();

    /** Returns the name of the {@code style} asset non-city chunks use for their palette. */
    String outsideStyleName();

    /** Returns {@code worldsettings.railpartheight6} (default 1): rail parts' height in 6-block levels. */
    int railPartHeight6();

    /** Returns whether {@code worldsettings.railwayavoidance} is {@code block_railway}: buildings block rails. */
    boolean buildingsBlockRailways();

    /**
     * Returns the world style's {@code scattered} settings (07-scattered), or {@code null} when it has none, as
     * upstream's world style without them places no scattered structure. The default is {@code null}, so a fake
     * world style in a test has none.
     *
     * @return the settings, or {@code null}
     */
    default ScatteredView scattered() {
        return null;
    }
}
