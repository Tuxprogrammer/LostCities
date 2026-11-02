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

    /** One palette of a group, with its weight. */
    final class Choice {

        private final String palette;
        private final float weight;

        /**
         * Creates a choice.
         *
         * @param palette the palette's full name
         * @param weight  its weight in the group
         */
        public Choice(String palette, float weight) {
            this.palette = palette;
            this.weight = weight;
        }

        /** Returns the palette's full name. */
        public String palette() {
            return palette;
        }

        /** Returns the weight within the group. */
        public float weight() {
            return weight;
        }
    }

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
     * Returns the style's groups with their weights, in group order, for the facade stage (0010 §4). A view that
     * does not expose them returns an empty list, and its chunks keep their own draw.
     *
     * @return the groups
     */
    default List<List<Choice>> groups() {
        return Collections.emptyList();
    }

    /**
     * Picks one choice from a group with one uniform float, as upstream's weighted pick does
     * ({@code asset.match.WeightedPick}): {@code r = u × total}, minus each positive weight in order until
     * {@code r ≤ 0}; float rounding past the last weight takes the last positive choice. Choices of weight 0 or less
     * are never picked.
     *
     * @param group the group, not empty
     * @param u     a float in [0, 1)
     * @return the index of the pick
     */
    static int weightedIndex(List<Choice> group, float u) {
        double total = 0;
        int last = 0;
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i)
                .weight() > 0) {
                total += group.get(i)
                    .weight();
                last = i;
            }
        }
        float r = u * (float) total;
        for (int i = 0; i < group.size(); i++) {
            float weight = group.get(i)
                .weight();
            if (weight <= 0) {
                continue;
            }
            r -= weight;
            if (r <= 0) {
                return i;
            }
        }
        return last;
    }

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
