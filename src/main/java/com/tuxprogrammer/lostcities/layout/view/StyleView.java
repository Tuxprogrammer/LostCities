package com.tuxprogrammer.lostcities.layout.view;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The layout's read-only view of a {@code style} asset: groups of weighted palettes, one of which is chosen per group
 * for each chunk.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/building-info.md, step 12.13 (05-assets owns the format). Side: both.
 */
public interface StyleView {

    /**
     * Picks one palette per random-palette group.
     *
     * <p>
     * Contract: exactly one {@code random.nextFloat()} per group, in the style's group order, as upstream's
     * {@code Style.getRandomPalette} draws.
     *
     * @param random the chunk's random
     * @return the chosen palette names, one per group, in group order
     */
    List<String> pickPalettes(Random random);

    /**
     * Picks the palette variety a building adds over the style's palettes (D25; the asset model's
     * {@code PaletteVariety}): no draw from the chunk's random, so the layout's draws are upstream's whatever the
     * variety holds.
     *
     * @param chosen   the palette names {@link #pickPalettes} returned for the chunk
     * @param building whether the chunk holds a building
     * @param seed     the building's variety seed, a function of the world seed and its (top-left) chunk
     * @return the option palettes' names, lowest layer first, appended after the chosen palettes; empty by default
     */
    default List<String> pickVariety(List<String> chosen, boolean building, long seed) {
        return Collections.emptyList();
    }
}
