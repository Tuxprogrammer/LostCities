package com.tuxprogrammer.lostcities.asset.match;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/varia/Tools.java
// Avoids: ASSET-10, CORE-21

import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * Upstream's weighted choice ({@code Tools.getRandomFromList}): draw {@code r = nextFloat() · total} and walk the list
 * subtracting weights until {@code r ≤ 0}. It consumes one {@code nextFloat()}, as upstream does, so the draw order of
 * the callers' randoms is unchanged.
 *
 * <p>
 * Fixes, both from docs/new-spec/05-assets/index.md ("Evaluation and the weighted pick", "Selectors"):
 * <ul>
 * <li>Entries of weight 0 or less are never picked, and a total of 0 picks nothing: upstream returned the first entry
 * (fixes ASSET-10).</li>
 * <li>When float rounding carries {@code r} past the last entry, the last entry with a positive weight is taken: one
 * upstream variant returned null there, which callers then crashed on (fixes CORE-21).</li>
 * </ul>
 * Side: both.
 */
public final class WeightedPick {

    private WeightedPick() {}

    /**
     * Picks one element by weight.
     *
     * @param random the caller's random; one {@code nextFloat()} is drawn when the list has a positive total
     * @param list   the candidates, in file order
     * @param weight each candidate's weight
     * @param <T>    the element type
     * @return the chosen element, or null when the list is empty or no element has a positive weight
     */
    public static <T> T pick(Random random, List<T> list, ToDoubleFunction<T> weight) {
        double total = 0;
        T last = null;
        for (T element : list) {
            double w = weight.applyAsDouble(element);
            if (w > 0) {
                total += w;
                last = element;
            }
        }
        if (last == null) {
            return null;
        }
        float r = random.nextFloat() * (float) total;
        for (T element : list) {
            double w = weight.applyAsDouble(element);
            if (w <= 0) {
                continue;
            }
            r -= (float) w;
            if (r <= 0) {
                return element;
            }
        }
        return last;
    }
}
