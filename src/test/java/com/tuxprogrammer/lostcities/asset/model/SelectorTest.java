package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;

/** Selector weights with distance limits and feathering (styles-and-city-styles.md, "Selectors"). */
public class SelectorTest {

    private static Selector selector(float factor, int min, int max, int feather) {
        return new Selector(factor, AssetId.parse("x"), "x", min, max, feather);
    }

    @Test
    public void withoutLimitsTheWeightIsTheFactor() {
        assertEquals(2f, selector(2f, 0, Integer.MAX_VALUE, 0).weight(1000, -1000), 0f);
    }

    @Test
    public void hardLimitsAndFeatheredFades() {
        Selector hard = selector(1f, 160, 320, 0);
        assertEquals(0f, hard.weight(0, 0), 0f);
        assertEquals(1f, hard.weight(10, 0), 0f);
        assertEquals(1f, hard.weight(20, 0), 0f);
        assertEquals(0f, hard.weight(21, 0), 0f);
        Selector soft = selector(1f, 160, Integer.MAX_VALUE, 80);
        // Chunk 7 is 112 blocks from the origin: 32 of the 80-block fade that ends at 160.
        assertEquals(0.4f, soft.weight(0, 7), 1e-6f);
        assertEquals(0f, soft.weight(0, 4), 0f);
    }

    @Test
    public void doesNotReproduceASSET09_distancesDoNotOverflowFarFromTheOrigin() {
        // At chunk 3000 (48,000 blocks) the squared distance exceeds 2^31; upstream's int math went negative.
        Selector near = selector(1f, 0, 1000, 0);
        assertEquals(0f, near.weight(3000, 3000), 0f);
        Selector far = selector(1f, 40000, Integer.MAX_VALUE, 0);
        assertEquals(1f, far.weight(3000, 3000), 0f);
        assertEquals(0f, far.weight(10, 10), 0f);
    }

    @Test
    public void doesNotReproduceASSET10_noSelectorIsPickedWhenEveryWeightIsZero() {
        assertNull(
            Selector.pick(
                new Random(1),
                Arrays.asList(
                    selector(1f, 1_000_000, Integer.MAX_VALUE, 0),
                    selector(1f, 2_000_000, Integer.MAX_VALUE, 0)),
                0,
                0));
    }

    @Test
    public void doesNotReproduceCITY31_theRealDistanceFieldsAreKept() {
        Selector selector = selector(0.5f, 100, 900, 25);
        assertEquals(100, selector.minSpawnDistance());
        assertEquals(900, selector.maxSpawnDistance());
        assertEquals(25, selector.feather());
    }
}
