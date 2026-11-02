package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Styles: one palette drawn per group (docs/original-spec/05-assets/styles-and-city-styles.md, "Style"). */
public class StyleTest {

    @Test
    public void drawsOnePalettePerGroupInOrder() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        Style standard = set.style(AssetId.parse("standard"));
        assertEquals(
            5,
            standard.groups()
                .size());
        Set<AssetId> walls = new HashSet<>();
        Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            List<Palette> layers = standard.randomPalettes(random);
            assertEquals(5, layers.size());
            assertEquals(
                AssetId.parse("common"),
                layers.get(0)
                    .id());
            walls.add(
                layers.get(2)
                    .id());
        }
        assertEquals(4, walls.size(), "bricks_standard, bricks_cyan, bricks_gray and bricks_silver all appear");
    }

    @Test
    public void doesNotReproduceASSET08_anEmptyOrWeightlessGroupIsRejectedAtLoad() {
        AssetSet empty = TestAssets.loadFiles("lostcities/styles/s.json", "{\"randompalettes\": [[]]}");
        assertEquals(0, empty.count(AssetType.STYLES));
        assertTrue(
            TestAssets.lines(empty, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("non-empty"));
        AssetSet weightless = TestAssets.loadFiles(
            "lostcities/palettes/p.json",
            "{\"palette\": []}",
            "lostcities/styles/s.json",
            "{\"randompalettes\": [[{\"factor\": 0, \"palette\": \"p\"}]]}");
        assertEquals(0, weightless.count(AssetType.STYLES));
    }
}
