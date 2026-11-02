package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Palette files (docs/original-spec/05-assets/palettes-and-variants.md, "Palette JSON"). */
public class PaletteTest {

    @Test
    public void readsEveryEntryKind() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/conditions/chestloot.json",
            "{\"values\": [{\"factor\": 1, \"value\": \"x\"}]}",
            "lostcities/variants/v.json",
            "{\"blocks\": [{\"random\": 1000, \"block\": \"minecraft:stone_bricks\"}]}",
            "lostcities/palettes/p.json",
            """
                {"palette": [
                  {"char": "#", "block": "minecraft:bricks"},
                  {"char": "v", "variant": "v", "damaged": "minecraft:iron_bars"},
                  {"char": "@", "frompalette": "#"},
                  {"char": "%", "blocks": [{"random": 40, "block": "minecraft:cobweb"}, {"random": 1000, "block": "minecraft:air"}]},
                  {"char": "C", "block": "minecraft:chest[facing=south]", "loot": "chestloot"},
                  {"char": ";", "block": "minecraft:furnace", "tag": {"Items": [{"Slot": 0, "id": "minecraft:coal", "Count": 10}]}}
                ]}
                """);
        assertEquals(new java.util.ArrayList<String>(), TestAssets.lines(set, AssetProblem.Severity.ERROR));
        Palette palette = set.palette(AssetId.parse("p"));
        assertEquals(
            6,
            palette.entries()
                .size());
        assertEquals(
            BlockRef.parse("minecraft:brick_block:0"),
            palette.entries()
                .get('#')
                .block()
                .block());
        assertEquals(
            PaletteEntry.Kind.RANDOM,
            palette.entries()
                .get('v')
                .kind());
        assertEquals(
            '#',
            palette.entries()
                .get('@')
                .reference());
        assertEquals(
            BlockRef.parse("minecraft:chest:3"),
            palette.entries()
                .get('C')
                .block()
                .block());
        assertEquals(
            AssetId.parse("chestloot"),
            palette.entries()
                .get('C')
                .loot()
                .id());
        assertTrue(
            palette.entries()
                .get(';')
                .tag()
                .toString()
                .contains("minecraft:coal"));
    }

    @Test
    public void doesNotReproduceASSET23_charMustBeExactlyOneCharacter() {
        for (String bad : new String[] { "\"\"", "\"ab\"" }) {
            AssetSet set = TestAssets.loadFiles(
                "lostcities/palettes/p.json",
                "{\"palette\": [{\"char\": " + bad + ", \"block\": \"minecraft:dirt\"}]}");
            assertEquals(0, set.count(AssetType.PALETTES));
            assertTrue(
                TestAssets.lines(set, AssetProblem.Severity.ERROR)
                    .get(0)
                    .contains("exactly one character"));
        }
    }

    @Test
    public void doesNotReproduceASSET30_damagedBelongsToTheDeclaringCharacter() {
        AssetSet set = TestAssets.loadFiles("lostcities/palettes/p.json", """
            {"palette": [
              {"char": "a", "block": "minecraft:cyan_terracotta", "damaged": "minecraft:iron_bars"},
              {"char": "b", "block": "minecraft:cyan_terracotta"}
            ]}
            """);
        Palette palette = set.palette(AssetId.parse("p"));
        assertEquals(
            BlockRef.parse("minecraft:iron_bars:0"),
            palette.entries()
                .get('a')
                .damaged()
                .block());
        assertNull(
            palette.entries()
                .get('b')
                .damaged(),
            "upstream keyed damage by block state, so b turned into iron bars too");
    }

    @Test
    public void doesNotReproduceASSET14_variantsResolveFromTheLoadedSetWithNoServer() {
        // Loading is plain Java over a registry view: no world, no server. The variant comes from the set itself.
        AssetSet set = TestAssets.loadFiles(
            "lostcities/variants/v.json",
            "{\"blocks\": [{\"random\": 64, \"block\": \"minecraft:bricks\"}, {\"random\": 64, \"block\": \"minecraft:stone_bricks\"}]}",
            "lostcities/palettes/p.json",
            "{\"palette\": [{\"char\": \"#\", \"variant\": \"v\"}]}");
        Palette palette = set.palette(AssetId.parse("p"));
        RandomList list = palette.entries()
            .get('#')
            .random();
        assertEquals(
            BlockRef.parse("minecraft:brick_block:0"),
            list.slot(0)
                .block());
        assertEquals(
            BlockRef.parse("minecraft:stonebrick:0"),
            list.slot(127)
                .block());
    }

    @Test
    public void aMissingVariantOrConditionRejectsThePalette() {
        AssetSet set = TestAssets
            .loadFiles("lostcities/palettes/p.json", "{\"palette\": [{\"char\": \"#\", \"variant\": \"nosuch\"}]}");
        List<String> errors = TestAssets.lines(set, AssetProblem.Severity.ERROR);
        assertTrue(
            errors.get(0)
                .contains("variants asset lostcities:nosuch, which does not exist"),
            errors.toString());
    }

    @Test
    public void anEntryWithNoKindIsAnError() {
        AssetSet set = TestAssets.loadFiles("lostcities/palettes/p.json", "{\"palette\": [{\"char\": \"#\"}]}");
        assertEquals(0, set.count(AssetType.PALETTES));
    }
}
