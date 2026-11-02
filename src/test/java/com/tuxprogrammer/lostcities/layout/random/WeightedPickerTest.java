package com.tuxprogrammer.lostcities.layout.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** {@link WeightedPicker}: upstream's weighted pick, one draw, never {@code null} for a non-empty list. */
public class WeightedPickerTest {

    /** A random whose next float is fixed. */
    private static final class FixedFloat extends Random {

        private static final long serialVersionUID = 1L;
        private final float value;
        int draws;

        FixedFloat(float value) {
            this.value = value;
        }

        @Override
        public float nextFloat() {
            draws++;
            return value;
        }
    }

    @Test
    public void picksByRunningWeight() {
        List<String> list = Arrays.asList("a", "b", "c");
        WeightedPicker.Weight<String> weights = s -> s.equals("b") ? 2f : 1f;
        assertEquals("a", WeightedPicker.pick(new FixedFloat(0.2f), list, weights));
        assertEquals("b", WeightedPicker.pick(new FixedFloat(0.5f), list, weights));
        assertEquals("c", WeightedPicker.pick(new FixedFloat(0.9f), list, weights));
    }

    @Test
    public void emptyListDrawsNothing() {
        FixedFloat random = new FixedFloat(0.5f);
        assertNull(WeightedPicker.pick(random, Collections.<String>emptyList(), s -> 1f));
        assertEquals(0, random.draws);
    }

    @Test
    public void nonEmptyListDrawsOnce() {
        FixedFloat random = new FixedFloat(0.5f);
        WeightedPicker.pick(random, Arrays.asList("x", "y"), s -> 1f);
        assertEquals(1, random.draws);
    }

    @Test
    public void doesNotReproduceCORE21_roundingRemainderReturnsTheLastEntryNotNull() {
        // With these weights, float rounding leaves r slightly above 0 after the last entry for a draw just below 1
        // (r = 1.49e-8, found by a float32 simulation of upstream's loop). Upstream returned null there.
        List<Float> weights = Arrays.asList(0.15f, 0.7f, 1f / 3f, 0.2f);
        float justBelowOne = Math.nextDown(1.0f);
        float total = 0;
        float r = 0;
        for (float w : weights) {
            total += w;
        }
        r = justBelowOne * total;
        for (float w : weights) {
            r -= w;
        }
        assertEquals(true, r > 0, "the fixture must leave a remainder, or it tests nothing");
        Float picked = WeightedPicker.pick(new FixedFloat(justBelowOne), weights, w -> w);
        assertEquals(0.2f, picked);
        // And a list whose only weight is zero still returns its entry.
        assertEquals("only", WeightedPicker.pick(new FixedFloat(0.5f), Collections.singletonList("only"), s -> 0f));
    }
}
