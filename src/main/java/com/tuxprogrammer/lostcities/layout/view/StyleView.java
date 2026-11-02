package com.tuxprogrammer.lostcities.layout.view;

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
}
