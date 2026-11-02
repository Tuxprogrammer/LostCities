package com.tuxprogrammer.lostcities.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** The grid sizes from the spawn frequency, the clamps, and the dimension lists. */
class RoguelikeSettingsTest {

    private static RoguelikeSettings frequency(int frequency) {
        return new RoguelikeSettings(true, frequency, 100, 60, Collections.singletonList(0), Collections.emptyList());
    }

    @Test
    void defaultFrequencyGives32ChunkCells() {
        RoguelikeSettings settings = frequency(RoguelikeSettings.DEFAULT_FREQUENCY);
        assertEquals(32, settings.cellSize());
        assertEquals(8, settings.minSeparation());
        assertEquals(24, settings.offsetBound());
    }

    @Test
    void smallFrequenciesAreClamped() {
        RoguelikeSettings settings = frequency(1);
        assertEquals(8, settings.cellSize());
        assertEquals(2, settings.minSeparation());
        assertEquals(6, settings.offsetBound());
    }

    @Test
    void packGeneratesInTheOverworldOnly() {
        RoguelikeSettings pack = RoguelikeSettings.pack();
        assertTrue(pack.generatesIn(0));
        assertFalse(pack.generatesIn(-1));
        assertFalse(pack.generatesIn(7));
    }

    @Test
    void emptyWhitelistAllowsAllButTheBlacklist() {
        RoguelikeSettings settings = new RoguelikeSettings(
            true,
            16,
            100,
            60,
            Collections.emptyList(),
            Arrays.asList(-1, 1));
        assertTrue(settings.generatesIn(0));
        assertTrue(settings.generatesIn(7));
        assertFalse(settings.generatesIn(-1));
    }

    @Test
    void naturalSpawnOffGeneratesNothing() {
        RoguelikeSettings off = new RoguelikeSettings(
            false,
            16,
            100,
            60,
            Collections.singletonList(0),
            Collections.emptyList());
        assertFalse(off.generatesIn(0));
    }

    @Test
    void summaryNamesEveryValue() {
        assertEquals(
            "naturalSpawn=true,spawnFrequency=16,upperLimit=100,lowerLimit=60,dimensionWL=[0],dimensionBL=[]",
            RoguelikeSettings.pack()
                .summary());
    }
}
