package com.tuxprogrammer.lostcities.asset.variety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetLoader;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.BlockData;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * The palette variety (D25, M11; asset-format.md, "Palette variety"): the bundled catalog loads and is available in the
 * pack, degrades to upstream's look without the pack, draws deterministically by weight, follows its groups'
 * conditions,
 * and keeps D28's rules and the muted desert (AR-16).
 */
public class PaletteVarietyTest {

    private static final BlockData DATA = TestAssets.blockData();
    private static final AssetSet PACK = TestAssets.bundled(TestAssets.pack(DATA));
    private static final AssetSet VANILLA = TestAssets.bundled(TestAssets.vanilla());
    private static final AssetId STANDARD = AssetId.parse("standard");
    private static final AssetId DESERT = AssetId.parse("desert");
    private static final int DRAWS = 4000;

    private static List<String> varietyErrors(AssetSet set) {
        List<String> out = new ArrayList<>();
        for (AssetProblem problem : set.problems(AssetProblem.Severity.ERROR)) {
            if (problem.toString()
                .contains("variety")) {
                out.add(problem.toString());
            }
        }
        return out;
    }

    private static List<String> picks(AssetSet set, AssetId style, String brick, boolean building, int draws) {
        List<String> chosen = Arrays
            .asList("lostcities:common", "lostcities:default", "lostcities:" + brick, "lostcities:glass_full");
        List<String> out = new ArrayList<>();
        for (int seed = 0; seed < draws; seed++) {
            out.addAll(
                set.variety()
                    .pick(style, chosen, building, seed * 7919L));
        }
        return out;
    }

    @Test
    public void theBundledVarietyLoadsAndEveryOptionIsAvailableInThePack() {
        assertEquals(Collections.emptyList(), varietyErrors(PACK));
        PaletteVariety variety = PACK.variety();
        assertTrue(
            variety.styles()
                .containsAll(Arrays.asList(STANDARD, DESERT)),
            "the two city styles have groups");
        assertTrue(
            variety.options()
                .size() > 300,
            "hundreds of options");
        assertEquals(
            variety.options()
                .size(),
            variety.availableCount(),
            "every option resolves in the pack: " + variety.unresolved());
        assertTrue(
            variety.unresolved()
                .isEmpty());
    }

    @Test
    public void withoutThePackOnlyVanillaOptionsAreDrawnAndNothingIsAnError() {
        assertEquals(Collections.emptyList(), varietyErrors(VANILLA));
        PaletteVariety variety = VANILLA.variety();
        assertTrue(variety.availableCount() > 0, "vanilla-only options stay");
        assertTrue(
            variety.availableCount() < variety.options()
                .size(),
            "pack options are unavailable");
        for (String id : picks(VANILLA, STANDARD, "bricks_standard", true, 500)) {
            assertNotNull(VANILLA.palette(AssetId.parse(id)), id + " is a loaded palette");
        }
    }

    @Test
    public void everyBlockIsCataloguedOrVanilla() {
        Set<String> known = new HashSet<>(TestAssets.vanillaNames());
        known.addAll(TestAssets.poolBlockNames(DATA));
        List<String> unknown = new ArrayList<>();
        for (JsonElement option : DATA.variety()
            .getAsJsonObject()
            .getAsJsonArray("options")) {
            JsonArray entries = option.getAsJsonObject()
                .getAsJsonArray("palette");
            for (String text : PaletteVariety.blockStrings(entries)) {
                if (!BlockRef.isDirectForm(text)) {
                    continue;
                }
                BlockRef ref = BlockRef.parse(text);
                assertTrue(ref.meta() <= BlockRef.MAX_META, text + ": plain metadata");
                if (!known.contains(ref.name())) {
                    unknown.add(text);
                }
            }
        }
        assertEquals(Collections.emptyList(), unknown, "every variety block is in the catalog's pools");
    }

    @Test
    public void theDrawIsAPureFunctionOfTheSeed() {
        List<String> chosen = Arrays.asList("lostcities:common", "lostcities:default", "lostcities:bricks_standard");
        for (long seed = -50; seed < 50; seed++) {
            assertEquals(
                PACK.variety()
                    .pick(STANDARD, chosen, true, seed),
                PACK.variety()
                    .pick(STANDARD, chosen, true, seed));
        }
        Set<List<String>> distinct = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            distinct.add(
                PACK.variety()
                    .pick(STANDARD, chosen, true, seed));
        }
        assertTrue(distinct.size() > 150, "nearly every building differs: " + distinct.size());
    }

    @Test
    public void theBaselineKeepsItsShare() {
        for (PaletteVariety.Group group : PACK.variety()
            .groups(STANDARD)) {
            int total = 0;
            int none = 0;
            for (PaletteVariety.Choice choice : group.choices()) {
                total += choice.weight();
                if (choice.option() == null) {
                    none += choice.weight();
                }
            }
            int baseline = 0;
            for (int seed = 0; seed < DRAWS; seed++) {
                if (group.draw(seed * 104729L) == null) {
                    baseline++;
                }
            }
            double expected = (double) none / total;
            double actual = (double) baseline / DRAWS;
            assertTrue(
                Math.abs(expected - actual) < 0.04,
                group.name() + ": baseline share " + actual + ", expected " + expected);
            assertTrue(expected >= 0.25, group.name() + ": the baseline is a large share");
        }
    }

    @Test
    public void wallOptionsFollowTheDrawnWallPalette() {
        for (String id : picks(PACK, STANDARD, "bricks_cyan", true, 400)) {
            assertFalse(id.contains("wall_stonebrick") || id.contains("wall_gray"), id);
        }
        assertTrue(
            picks(PACK, STANDARD, "bricks_cyan", true, 400).stream()
                .anyMatch(id -> id.contains("wall_cyan")),
            "cyan buildings take cyan options");
        assertTrue(
            picks(PACK, STANDARD, "bricks_standard", true, 400).stream()
                .anyMatch(id -> id.contains("wall_stonebrick")),
            "stone-brick buildings take stone-brick options");
    }

    @Test
    public void interiorsGoToBuildingsAndParkFloraToStreets() {
        List<String> buildings = picks(PACK, STANDARD, "bricks_standard", true, 400);
        List<String> streets = picks(PACK, STANDARD, "bricks_standard", false, 400);
        assertTrue(
            buildings.stream()
                .anyMatch(id -> id.contains("variety/floor_")));
        assertFalse(
            buildings.stream()
                .anyMatch(id -> id.contains("variety/flora_")));
        assertTrue(
            streets.stream()
                .anyMatch(id -> id.contains("variety/flora_")));
        assertFalse(
            streets.stream()
                .anyMatch(id -> id.contains("variety/floor_") || id.contains("variety/wall_")));
    }

    @Test
    public void floorsKeepSevenInTenSpawnable() {
        // D28 rule 1: every Chisel and Botania block is spawn-proof; the rest of the floors are not.
        for (AssetId style : Arrays.asList(STANDARD, DESERT)) {
            PaletteVariety.Group floors = group(style, "interior_floor");
            int total = 0;
            int proof = 0;
            for (PaletteVariety.Choice choice : floors.choices()) {
                total += choice.weight();
                if (choice.option() != null && spawnProof(choice.option())) {
                    proof += choice.weight();
                }
            }
            assertTrue(proof * 10 <= total * 3, style + ": spawn-proof floors " + proof + " of " + total);
        }
    }

    private static boolean spawnProof(PaletteVariety.Option option) {
        String block = option.palette()
            .entries()
            .values()
            .iterator()
            .next()
            .block()
            .block(com.tuxprogrammer.lostcities.asset.block.MetalFlags.ALL_ON)
            .name();
        return block.startsWith("chisel:") || block.startsWith("Botania:");
    }

    private static PaletteVariety.Group group(AssetId style, String name) {
        for (PaletteVariety.Group group : PACK.variety()
            .groups(style)) {
            if (group.name()
                .equals(name)) {
                return group;
            }
        }
        throw new AssertionError(style + " has no group " + name);
    }

    @Test
    public void noSpeedBlockIsPlaced() {
        // D28 rule 3: walls are also roofs and ruins, so no speed block goes anywhere in the variety.
        Set<String> speed = new HashSet<>();
        for (JsonElement pool : DATA.pools()
            .getAsJsonObject()
            .getAsJsonArray("pools")) {
            JsonArray candidates = pool.getAsJsonObject()
                .getAsJsonArray("candidates");
            if (candidates == null) {
                continue;
            }
            for (JsonElement candidate : candidates) {
                JsonObject row = candidate.getAsJsonObject();
                if (row.has("flags") && row.getAsJsonArray("flags")
                    .toString()
                    .contains("\"SPD\"")) {
                    for (JsonElement block : row.getAsJsonArray("blocks")) {
                        speed.add(block.getAsString());
                    }
                }
            }
        }
        assertFalse(speed.isEmpty(), "the pools flag speed blocks");
        for (JsonElement option : DATA.variety()
            .getAsJsonObject()
            .getAsJsonArray("options")) {
            for (String text : PaletteVariety.blockStrings(
                option.getAsJsonObject()
                    .getAsJsonArray("palette"))) {
                assertFalse(speed.contains(text), text + " is a speed block");
            }
        }
    }

    @Test
    public void theDesertStaysMuted() {
        // AR-16: no saturated red or orange in a desert building's walls, trims, bands or floors.
        Set<String> saturated = new HashSet<>(
            Arrays.asList(
                "minecraft:stained_hardened_clay:1",
                "minecraft:stained_hardened_clay:4",
                "minecraft:stained_hardened_clay:6",
                "minecraft:stained_hardened_clay:14",
                "minecraft:hardened_clay:0",
                "minecraft:red_sandstone",
                "etfuturum:red_sandstone"));
        for (PaletteVariety.Group group : PACK.variety()
            .groups(DESERT)) {
            if (group.name()
                .startsWith("planters")
                || group.name()
                    .startsWith("park_flora")) {
                continue;
            }
            for (PaletteVariety.Choice choice : group.choices()) {
                if (choice.option() == null) {
                    continue;
                }
                for (com.tuxprogrammer.lostcities.asset.model.PaletteEntry entry : choice.option()
                    .palette()
                    .entries()
                    .values()) {
                    if (entry.block() == null) {
                        continue;
                    }
                    BlockRef block = entry.block()
                        .block(com.tuxprogrammer.lostcities.asset.block.MetalFlags.ALL_ON);
                    String name = block.name();
                    assertFalse(
                        saturated.contains(block.toString()) || saturated.contains(name)
                            || name.contains("_red")
                            || name.contains("orange"),
                        group.name() + ": " + block);
                }
            }
        }
    }

    // ------------------------------------------------------------------ hand-written catalogs

    private static AssetSet withVariety(String varietyJson) {
        BlockData data = new BlockData(
            DATA.mapping(),
            DATA.metals(),
            DATA.neverPlace(),
            DATA.pools(),
            JsonFields.parseStrict(varietyJson));
        return AssetLoader.load(
            Collections.singletonList(
                TestAssets.source(
                    TestAssets.files(
                        "lostcities/palettes/a.json",
                        "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:stone_bricks\"}]}",
                        "lostcities/styles/s.json",
                        "{\"randompalettes\": [[{\"factor\": 1.0, \"palette\": \"a\"}]]}"))),
            data,
            TestAssets.vanilla(),
            new Problems());
    }

    @Test
    public void anUnavailableOptionsShareGoesToTheBaseline() {
        AssetSet set = withVariety(
            "{\"options\": [{\"name\": \"absent\", \"palette\": [{\"char\": \"#\", \"block\": \"nomod:block:0\"}]}],"
                + " \"styles\": [{\"style\": \"s\", \"groups\": [{\"name\": \"g\", \"options\":"
                + " [{\"option\": \"none\", \"weight\": 1}, {\"option\": \"absent\", \"weight\": 99}]}]}]}");
        assertEquals(Collections.emptyList(), varietyErrors(set));
        assertEquals(
            Collections.singletonList("absent: nomod:block"),
            set.variety()
                .unresolved());
        for (long seed = 0; seed < 100; seed++) {
            assertEquals(
                Collections.emptyList(),
                set.variety()
                    .pick(AssetId.parse("s"), Collections.singletonList("lostcities:a"), true, seed));
        }
    }

    @Test
    public void anAvailableOptionIsDrawnAndLayered() {
        AssetSet set = withVariety(
            "{\"options\": [{\"name\": \"mossy\", \"palette\": [{\"char\": \"#\", \"block\": \"minecraft:stonebrick:1\"}]}],"
                + " \"styles\": [{\"style\": \"s\", \"groups\": [{\"name\": \"g\", \"when\": \"a\", \"for\": \"building\","
                + " \"options\": [{\"option\": \"mossy\", \"weight\": 1}]}]}]}");
        assertEquals(Collections.emptyList(), varietyErrors(set));
        List<String> picked = set.variety()
            .pick(AssetId.parse("s"), Collections.singletonList("lostcities:a"), true, 42L);
        assertEquals(Collections.singletonList("lostcitiesnh:variety/mossy"), picked);
        assertNotNull(set.palette(AssetId.parse("lostcitiesnh:variety/mossy")));
        assertEquals(
            Collections.emptyList(),
            set.variety()
                .pick(AssetId.parse("s"), Collections.singletonList("lostcities:b"), true, 42L),
            "'when' not drawn: no option");
        assertEquals(
            Collections.emptyList(),
            set.variety()
                .pick(AssetId.parse("s"), Collections.singletonList("lostcities:a"), false, 42L),
            "a building group skips streets");
    }

    @Test
    public void malformedGroupsAreErrors() {
        String[] bad = {
            // an unknown option
            "{\"options\": [], \"styles\": [{\"style\": \"s\", \"groups\": [{\"name\": \"g\", \"options\":"
                + " [{\"option\": \"nope\", \"weight\": 1}]}]}]}",
            // a 'when' palette that is not loaded
            "{\"options\": [], \"styles\": [{\"style\": \"s\", \"groups\": [{\"name\": \"g\", \"when\": \"zzz\","
                + " \"options\": [{\"option\": \"none\", \"weight\": 1}]}]}]}",
            // a weight of zero
            "{\"options\": [], \"styles\": [{\"style\": \"s\", \"groups\": [{\"name\": \"g\", \"options\":"
                + " [{\"option\": \"none\", \"weight\": 0}]}]}]}",
            // an unknown scope
            "{\"options\": [], \"styles\": [{\"style\": \"s\", \"groups\": [{\"name\": \"g\", \"for\": \"roofs\","
                + " \"options\": [{\"option\": \"none\", \"weight\": 1}]}]}]}" };
        for (String json : bad) {
            assertFalse(varietyErrors(withVariety(json)).isEmpty(), json);
        }
    }
}
