package com.tuxprogrammer.lostcities.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuxprogrammer.lostcities.asset.BlockRef;

/** {@link PartExport}: upstream's export file, with a stable missing palette and a clean failure. */
public class PartExportTest {

    private static final BlockRef STONE = BlockRef.of("minecraft:stone", 0);
    private static final BlockRef GLASS = BlockRef.of("minecraft:glass", 0);
    private static final BlockRef WOOL_RED = BlockRef.of("minecraft:wool", 14);
    private static final BlockRef BRICK = BlockRef.of("minecraft:brick_block", 0);

    @Test
    public void thePoolIsUpstreams492Characters() {
        assertEquals(492, PartExport.POOL.size());
        assertEquals(492, new HashSet<>(PartExport.POOL).size());
        assertEquals('a', (char) PartExport.POOL.get(0));
        assertEquals('ӿ', (char) PartExport.POOL.get(491));
    }

    @Test
    public void knownBlocksTakeTheirCharactersAndNewOnesGoToTheMissingPalette() {
        Map<BlockRef, Character> reverse = new LinkedHashMap<>();
        reverse.put(STONE, '#');
        reverse.put(GLASS, 'g');
        // A 2 x 1 part, two slices: stone, glass / air, wool.
        BlockRef[][] grid = { { STONE, GLASS }, { BlockRef.AIR, WOOL_RED } };
        PartExport.Result result = PartExport
            .export(2, 1, 2, reverse, new HashSet<>(reverse.values()), "bricks", (x, y, z) -> grid[y][x]);
        JsonObject root = new JsonParser().parse(result.json())
            .getAsJsonObject();
        JsonObject part = root.getAsJsonObject("exportedpart");
        assertEquals(
            2,
            part.get("xsize")
                .getAsInt());
        assertEquals(
            "bricks",
            part.get("refpalette")
                .getAsString());
        JsonArray slices = part.getAsJsonArray("slices");
        assertEquals(
            "#g",
            slices.get(0)
                .getAsJsonArray()
                .get(0)
                .getAsString());
        assertEquals(
            " a",
            slices.get(1)
                .getAsJsonArray()
                .get(0)
                .getAsString(),
            "air without a character is a space; the new block takes the first free character");
        JsonArray missing = root.getAsJsonObject("missingpalette")
            .getAsJsonArray("palette");
        assertEquals(1, missing.size());
        assertEquals(
            "minecraft:wool:14",
            missing.get(0)
                .getAsJsonObject()
                .get("block")
                .getAsString());
        assertEquals(1, result.newBlocks());
    }

    @Test
    public void doesNotReproduceCORE29_missingPaletteIsSortedAndTheFileIsStable() {
        // New blocks met in the order wool, brick, glass: the palette lists them by block, every time.
        BlockRef[][] grid = { { WOOL_RED, BRICK, GLASS } };
        Map<BlockRef, Character> none = Collections.emptyMap();
        PartExport.Result first = PartExport
            .export(3, 1, 1, none, Collections.emptySet(), null, (x, y, z) -> grid[y][x]);
        PartExport.Result second = PartExport
            .export(3, 1, 1, none, Collections.emptySet(), null, (x, y, z) -> grid[y][x]);
        assertEquals(first.json(), second.json());
        JsonArray missing = new JsonParser().parse(first.json())
            .getAsJsonObject()
            .getAsJsonObject("missingpalette")
            .getAsJsonArray("palette");
        assertEquals(
            "minecraft:brick_block:0",
            missing.get(0)
                .getAsJsonObject()
                .get("block")
                .getAsString());
        assertEquals(
            "minecraft:glass:0",
            missing.get(1)
                .getAsJsonObject()
                .get("block")
                .getAsString());
        assertEquals(
            "minecraft:wool:14",
            missing.get(2)
                .getAsJsonObject()
                .get("block")
                .getAsString());
    }

    @Test
    public void doesNotReproduceCORE29_aPartWithTooManyBlocksFailsInsteadOfWritingNull() {
        int size = 600;
        PartExport.Result result = PartExport.export(
            size,
            1,
            1,
            Collections.<BlockRef, Character>emptyMap(),
            Collections.<Character>emptySet(),
            null,
            (x, y, z) -> BlockRef.of("minecraft:wool", x));
        assertTrue(result.poolExhausted());
        assertNull(result.json());
    }

    @Test
    public void newCharactersAvoidThePalettesOwn() {
        Set<Character> used = new HashSet<>();
        used.add('a');
        used.add('b');
        PartExport.Result result = PartExport
            .export(1, 1, 1, Collections.<BlockRef, Character>emptyMap(), used, null, (x, y, z) -> BRICK);
        assertTrue(
            result.json()
                .contains("\"c\""),
            result.json());
        assertFalse(
            result.json()
                .contains("refpalette"));
    }
}
