package com.tuxprogrammer.lostcities.layout.random;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/varia/Tools.java
// Avoids: CORE-21

import java.util.List;
import java.util.Random;

/**
 * Upstream's weighted pick ({@code Tools.getRandomFromList}): one {@link Random#nextFloat()} times the total weight,
 * then the first entry whose running weight reaches it. An empty list draws nothing and returns {@code null}; every
 * other list draws exactly once.
 *
 * <p>
 * Avoids CORE-21 (CITY-09): upstream's {@code java.util.Random} overload returns {@code null} when rounding leaves a
 * remainder after the last entry; here the pick falls to the last entry, as upstream's {@code RandomSource} overload
 * already does. Nothing else about the draw changes.
 *
 * <p>
 * Spec: docs/original-spec/01-core/utilities.md (`Tools`), new-spec 04-city-layout ("Weighted picks"). Side: both.
 */
public final class WeightedPicker {

    private WeightedPicker() {}

    /** The weight of one entry. */
    public interface Weight<T> {

        /**
         * Returns the entry's weight, zero or more.
         *
         * @param entry the entry
         * @return its weight
         */
        float of(T entry);
    }

    /**
     * Picks an entry.
     *
     * @param random the random to draw from (one {@code nextFloat} unless the list is empty)
     * @param list   the entries, in a fixed order
     * @param weight the weight of each entry
     * @return the picked entry, or {@code null} only when the list is empty
     */
    public static <T> T pick(Random random, List<T> list, Weight<T> weight) {
        if (list.isEmpty()) {
            return null;
        }
        float total = 0;
        for (T entry : list) {
            total += weight.of(entry);
        }
        float r = random.nextFloat() * total;
        for (T entry : list) {
            r -= weight.of(entry);
            if (r <= 0) {
                return entry;
            }
        }
        return list.get(list.size() - 1);
    }
}
