package com.tuxprogrammer.lostcities.asset.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;

/**
 * Biome matchers in the 1.7.10 vocabulary (P05-6, D31; docs/new-spec/05-assets/asset-format.md, "Biome matchers"),
 * including the converted bundled world style's desert rule with the RWG realistic biomes (P-PROF-4).
 */
public class BiomeMatcherTest {

    private static BiomeDescriptor biome(String name, String rwg, String... types) {
        return BiomeDescriptor.of(new HashSet<>(Arrays.asList(types)), name, rwg);
    }

    @Test
    public void entriesTestTypesNamesAndRealisticBiomes() {
        assertTrue(
            BiomeEntry.parse("type:ocean")
                .matches(biome("Deep Ocean", null, "OCEAN", "WATER")));
        assertTrue(
            BiomeEntry.parse("name:Kelp Forest")
                .matches(biome("kelp forest", null, "WATER")));
        assertTrue(
            BiomeEntry.parse("rwg:canyon")
                .matches(biome("Hot Plains", "canyon", "HOT", "PLAINS")));
        assertFalse(
            BiomeEntry.parse("rwg:canyon")
                .matches(biome("Hot Plains", null, "HOT")));
    }

    @Test
    public void rejects120IdsAndTagsAndUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> BiomeEntry.parse("#minecraft:is_ocean"));
        assertThrows(IllegalArgumentException.class, () -> BiomeEntry.parse("minecraft:desert"));
        assertThrows(IllegalArgumentException.class, () -> BiomeEntry.parse("type:NOT_A_TYPE"));
        assertThrows(IllegalArgumentException.class, () -> BiomeEntry.parse("rwg:CanyonForest"));
    }

    @Test
    public void clausesKeepUpstreamsMeaning() {
        BiomeMatcher matcher = BiomeMatcher.of(
            Collections.<BiomeEntry>emptyList(),
            Arrays.asList(BiomeEntry.parse("type:SANDY"), BiomeEntry.parse("rwg:oasis")),
            Collections.singletonList(BiomeEntry.parse("type:OCEAN")));
        assertTrue(matcher.matches(biome("Hot Desert", null, "HOT", "DRY", "SANDY")));
        assertTrue(matcher.matches(biome("Oasis", "oasis")));
        assertFalse(matcher.matches(biome("Beach", null, "SANDY", "BEACH", "OCEAN")));
        assertFalse(matcher.matches(biome("Plains", null, "PLAINS")));
        assertTrue(BiomeMatcher.ANY.matches(BiomeDescriptor.NONE));
    }

    @Test
    public void theBundledDesertStyleCoversRwgCanyonsAndOases() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        WorldStyle standard = set.worldStyle(AssetId.parse("standard"));
        WorldStyle.CityStyleChoice desert = null;
        for (WorldStyle.CityStyleChoice choice : standard.cityStyles()) {
            if (choice.cityStyle()
                .id()
                .equals(AssetId.parse("citystyle_desert"))) {
                desert = choice;
            }
        }
        CityStyle style = desert.cityStyle();
        assertEquals(
            AssetId.parse("desert"),
            style.style()
                .id());
        BiomeMatcher biomes = desert.biomes();
        // RWG's canyons report Hot Plains and Hot Forest; its oases report the untyped Oasis base biome.
        assertTrue(biomes.matches(biome("Hot Plains", "canyon", "HOT", "SAVANNA", "PLAINS", "SPARSE")));
        assertTrue(biomes.matches(biome("Hot Forest", "canyon_forest", "HOT", "SAVANNA", "PLAINS", "SPARSE")));
        assertTrue(biomes.matches(biome("Oasis", "oasis")));
        assertTrue(biomes.matches(biome("Oasis", "red_oasis")));
        assertTrue(biomes.matches(biome("Hot Desert", "desert", "HOT", "DRY", "SANDY")));
        assertFalse(biomes.matches(biome("Hot Ocean", null, "OCEAN", "BEACH", "SANDY")));
        assertFalse(biomes.matches(biome("Plains", "plains", "PLAINS")));
        // The ocean multiplier also covers Biomes O' Plenty's ocean sub-biomes, which are WATER, not OCEAN.
        assertEquals(0.1f, standard.cityChanceMultiplier(biome("Kelp Forest", null, "WATER", "FOREST")), 0f);
        assertEquals(0.3f, standard.cityChanceMultiplier(biome("River", null, "RIVER")), 0f);
        assertEquals(1f, standard.cityChanceMultiplier(biome("Plains", null, "PLAINS")), 0f);
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .isEmpty());
    }
}
