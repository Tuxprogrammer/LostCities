package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Reading VillageNames' config as its source does (VillageGeneratorConfigHandler.java#L593-L625 and #L1822-L1862 at
 * 4.5.21-GTNH), with entries in the shapes the pack's {@code village_gen.cfg} uses.
 */
class VillageNamesSettingsTest {

    @Test
    void biomeNameIsTheFirstColumnTrimmed() {
        Set<String> names = VillageNamesSettings.parseSpawnBiomeNames(
            new String[] { "Plains", "Heathland|savanna|oak", "  Desert M  ", "", "|plains", null, "Plains",
                "Roofed Forest| |dark_oak|nosub" });
        assertEquals(Arrays.asList("Plains", "Heathland", "Desert M", "Roofed Forest"), new ArrayList<>(names));
    }

    @Test
    void unsetListIsEmpty() {
        assertTrue(
            VillageNamesSettings.parseSpawnBiomeNames(null)
                .isEmpty());
    }

    @Test
    void packSpacingIsFiftyAndThirty() {
        VillageNamesSettings pack = new VillageNamesSettings(true, true, 40, 10, 0, Collections.<String>emptySet());
        assertEquals(50, pack.maxSpacing());
        assertEquals(30, pack.minSpacing());
    }

    @Test
    void minimumSpacingIsClampedToOne() {
        VillageNamesSettings tight = new VillageNamesSettings(true, true, 5, 10, 0, Collections.<String>emptySet());
        assertEquals(15, tight.maxSpacing());
        assertEquals(1, tight.minSpacing());
    }

    @Test
    void spawnBiomeTestIsExactAndCaseSensitive() {
        VillageNamesSettings settings = new VillageNamesSettings(
            true,
            true,
            40,
            10,
            0,
            VillageNamesSettings.parseSpawnBiomeNames(new String[] { "Plains", "Oasis" }));
        assertTrue(settings.isSpawnBiome("Plains"));
        assertTrue(settings.isSpawnBiome("Oasis"));
        assertFalse(settings.isSpawnBiome("plains"));
        assertFalse(settings.isSpawnBiome("Plains (RWG)"));
        assertFalse(settings.isSpawnBiome("Hot Plains"));
        assertFalse(settings.isSpawnBiome(null));
    }

    @Test
    void offGeneratesNothing() {
        VillageNamesSettings off = VillageNamesSettings.off();
        assertFalse(off.villagesOn());
        assertFalse(off.monumentsOn());
    }

    @Test
    void summaryNamesEveryValue() {
        VillageNamesSettings settings = new VillageNamesSettings(
            true,
            false,
            40,
            10,
            3,
            Collections.singleton("Plains"));
        String summary = settings.summary();
        assertTrue(
            summary.startsWith("villages=true monuments=false spacing=40+-10 villageFreeRadius=3 spawnBiomes=1("));
    }
}
