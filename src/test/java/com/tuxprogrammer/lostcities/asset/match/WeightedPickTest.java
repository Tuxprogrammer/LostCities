package com.tuxprogrammer.lostcities.asset.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Upstream's weighted choice with its two fixes (docs/new-spec/05-assets/index.md, "Evaluation"). */
public class WeightedPickTest {

    /** A random whose next float is fixed, to drive the walk to its edges. */
    private static final class FixedFloat extends Random {

        private static final long serialVersionUID = 1L;
        private final float value;

        FixedFloat(float value) {
            this.value = value;
        }

        @Override
        public float nextFloat() {
            return value;
        }
    }

    @Test
    public void picksInProportionToWeight() {
        List<String> list = Arrays.asList("a", "b", "c");
        Map<String, Double> weights = new HashMap<>();
        weights.put("a", 1.0);
        weights.put("b", 2.0);
        weights.put("c", 1.0);
        Map<String, Integer> counts = new HashMap<>();
        Random random = new Random(42);
        for (int i = 0; i < 40000; i++) {
            counts.merge(WeightedPick.pick(random, list, weights::get), 1, Integer::sum);
        }
        assertEquals(0.5, counts.get("b") / 40000.0, 0.02);
        assertEquals(0.25, counts.get("a") / 40000.0, 0.02);
    }

    @Test
    public void walksTheListInOrderAsUpstream() {
        List<String> list = Arrays.asList("a", "b", "c");
        // total 3: r = 0.4 * 3 = 1.2; minus a (1) = 0.2; minus b (1) <= 0, so b.
        assertEquals("b", WeightedPick.pick(new FixedFloat(0.4f), list, s -> 1.0));
        assertEquals("a", WeightedPick.pick(new FixedFloat(0f), list, s -> 1.0));
    }

    @Test
    public void doesNotReproduceASSET10_zeroWeightEntriesArePickedNeverAndAZeroTotalPicksNothing() {
        List<String> list = Arrays.asList("zero", "one");
        // Upstream: r = 0 * total = 0, and "zero" is returned at once because r <= 0.
        assertEquals("one", WeightedPick.pick(new FixedFloat(0f), list, s -> "zero".equals(s) ? 0.0 : 1.0));
        assertNull(WeightedPick.pick(new FixedFloat(0.5f), list, s -> 0.0));
        assertNull(WeightedPick.pick(new Random(1), Collections.<String>emptyList(), s -> 1.0));
    }

    @Test
    public void doesNotReproduceCORE21_roundingPastTheLastEntryTakesTheLastEntry() {
        List<Float> list = Arrays.asList(0.1f, 0.2f, 0.3f, 0.7f);
        // The largest float below 1: float subtraction can leave r a hair above 0 after the last entry.
        Float picked = WeightedPick.pick(new FixedFloat(Math.nextDown(1f)), list, f -> f);
        assertNotNull(picked);
        assertEquals(0.7f, picked, 0f);
    }
}
