package com.tuxprogrammer.lostcities.layout.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link StableHash}: SplitMix64's finalizer, FNV-1a and the two conversions. The planners that use it are checked
 * against upstream's recorded output, which covers the hash end to end; these tests pin the primitives' own numbers.
 */
public class StableHashTest {

    @Test
    public void mix64IsSplitMix64sFinalizer() {
        // SplitMix64 seeded with 0: the first output is mix64(0x9e3779b97f4a7c15), a published reference value.
        assertEquals(0xe220a8397b1dcdafL, StableHash.mix64(0x9e3779b97f4a7c15L));
        assertEquals(0L, StableHash.mix64(0L));
    }

    @Test
    public void fnvBasisOfTheEmptyString() {
        assertEquals(StableHash.mix64(0xcbf29ce484222325L), StableHash.stableStringHash(""));
    }

    @Test
    public void stringHashDependsOnEveryCharacter() {
        assertNotEquals(
            StableHash.stableStringHash("minecraft:overworld"),
            StableHash.stableStringHash("minecraft:overworle"));
    }

    @Test
    public void unitDoubleUsesTheTop53Bits() {
        assertEquals(0.0, StableHash.unitDouble(0L));
        assertEquals(0.5, StableHash.unitDouble(1L << 63));
        double max = StableHash.unitDouble(-1L);
        assertTrue(max < 1.0 && max > 0.9999999);
    }

    @Test
    public void floorModHashIsNonNegative() {
        assertEquals(3, StableHash.floorModHash(-1L, 4));
        assertEquals(1, StableHash.floorModHash(9L, 4));
    }
}
