package com.tuxprogrammer.lostcities.compat.witchery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** The union footprint over every type and facing, and the settings line. */
class WitcherySettingsTest {

    @Test
    void packUnionIsTheCovensTwentyThreeSquare() {
        // coexistence.md § Witchery: offsets −9 to +13 of the candidate chunk's origin on each axis.
        assertArrayEquals(
            new int[] { -9, -9, 13, 13 },
            WitcherySettings.pack()
                .unionOffsets());
    }

    @Test
    void eachTypeReachesItsBoxInEveryFacing() {
        assertArrayEquals(new int[] { -9, -9, 13, 13 }, WitcherySettings.StructureType.COVEN.offsets());
        assertArrayEquals(new int[] { -3, -3, 11, 11 }, WitcherySettings.StructureType.SHACK.offsets());
        assertArrayEquals(new int[] { -3, -3, 11, 11 }, WitcherySettings.StructureType.HOBGOBLIN_HUT.offsets());
        // 6 × 5, started at (+5, +5): turned west it reaches back 5 blocks, south 6.
        assertArrayEquals(new int[] { 0, 0, 11, 11 }, WitcherySettings.StructureType.WICKER_MAN.offsets());
    }

    @Test
    void unionShrinksWithTheTypesThatAreOn() {
        WitcherySettings wickerOnly = new WitcherySettings(
            Collections.singletonList(WitcherySettings.StructureType.WICKER_MAN),
            20,
            true);
        assertArrayEquals(new int[] { 0, 0, 11, 11 }, wickerOnly.unionOffsets());
    }

    @Test
    void offWhenNothingGeneratesHere() {
        assertFalse(new WitcherySettings(Collections.emptyList(), 20, true).on(), "every type off");
        assertFalse(
            new WitcherySettings(Arrays.asList(WitcherySettings.StructureType.values()), 20, false).on(),
            "not the overworld");
        assertTrue(
            WitcherySettings.pack()
                .on());
    }

    @Test
    void summaryNamesCellAndTypesInAFixedOrder() {
        WitcherySettings shuffled = new WitcherySettings(
            Arrays.asList(WitcherySettings.StructureType.SHACK, WitcherySettings.StructureType.COVEN),
            20,
            true);
        assertEquals("on,cell=20,types=coven+shack", shuffled.summary());
    }
}
