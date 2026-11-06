package com.tuxprogrammer.lostcities.worldgen.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Which lots hold a pack house: a pure, seeded function (docs/design/0011-city-edges-and-character.md). */
public class PackHouseSelectionTest {

    private static final long SEED = 123456789L;

    @Test
    public void theChoiceIsTheSameEveryTimeAndInAnyOrder() {
        boolean[] forward = new boolean[400];
        for (int i = 0; i < 400; i++) {
            forward[i] = PackHouseSelection.picks(SEED, i % 20, i / 20, 0.3f);
        }
        for (int i = 399; i >= 0; i--) {
            assertEquals(forward[i], PackHouseSelection.picks(SEED, i % 20, i / 20, 0.3f));
        }
    }

    @Test
    public void noChanceNeverPicksAndFullChanceAlwaysDoes() {
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                assertFalse(PackHouseSelection.picks(SEED, x, z, 0f));
                assertTrue(PackHouseSelection.picks(SEED, x, z, 1f));
            }
        }
    }

    @Test
    public void theRateMatchesTheOption() {
        for (float chance : new float[] { 0.1f, 0.25f, 0.5f }) {
            int picked = 0;
            int total = 200 * 200;
            for (int x = 0; x < 200; x++) {
                for (int z = 0; z < 200; z++) {
                    if (PackHouseSelection.picks(SEED, x, z, chance)) {
                        picked++;
                    }
                }
            }
            double sigma = Math.sqrt(total * chance * (1 - chance));
            assertEquals(total * chance, picked, 4 * sigma + 1, "rate for " + chance);
        }
    }

    @Test
    public void theKindIsInRangeAndSpreadOverAllKinds() {
        int kinds = 6;
        int[] seen = new int[kinds];
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 100; z++) {
                int kind = PackHouseSelection.kind(SEED, x, z, kinds);
                assertTrue(kind >= 0 && kind < kinds);
                seen[kind]++;
            }
        }
        for (int count : seen) {
            assertTrue(count > 10000 / kinds / 2, "every kind turns up");
        }
    }
}
