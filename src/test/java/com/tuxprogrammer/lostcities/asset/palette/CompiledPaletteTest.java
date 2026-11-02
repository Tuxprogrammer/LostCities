package com.tuxprogrammer.lostcities.asset.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Palette;

/** Layered palette compilation (docs/original-spec/05-assets/palettes-and-variants.md, "Compiling a palette"). */
public class CompiledPaletteTest {

    private static final BlockRef BRICK = BlockRef.parse("minecraft:brick_block:0");
    private static final BlockRef STONEBRICK = BlockRef.parse("minecraft:stonebrick:0");
    private static final BlockRef GLASS = BlockRef.parse("minecraft:glass:0");
    private static final BlockRef BARS = BlockRef.parse("minecraft:iron_bars:0");

    private static AssetSet palettes(String... pathsAndContents) {
        return TestAssets.loadFiles(pathsAndContents);
    }

    private static Palette palette(AssetSet set, String name) {
        return set.palette(AssetId.parse(name));
    }

    @Test
    public void laterLayersOverrideEarlierOnesByCharacter() {
        AssetSet set = palettes(
            "lostcities/palettes/base.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:bricks\"}, {\"char\": \"+\", \"block\": \"minecraft:glass\"}]}",
            "lostcities/palettes/top.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:stone_bricks\"}]}");
        CompiledPalette compiled = CompiledPalette
            .compile(MetalFlags.ALL_ON, palette(set, "base"), palette(set, "top"));
        assertEquals(STONEBRICK, compiled.get('#', new Random(0)));
        assertEquals(GLASS, compiled.get('+', new Random(0)));
        assertFalse(compiled.isDefined('?'));
        assertNull(compiled.get('?', new Random(0)));
    }

    @Test
    public void doesNotReproduceASSET11_frompaletteCopiesTheFinalValueOfItsTarget() {
        // glass_side_variant_bricks: '@' copies '#'; the building's palette later redefines '#'.
        AssetSet set = palettes(
            "lostcities/palettes/style.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:bricks\"}]}",
            "lostcities/palettes/side.json",
            "{\"palette\": [{\"char\": \"@\", \"frompalette\": \"#\"}]}",
            "lostcities/palettes/building.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:stone_bricks\"}]}");
        CompiledPalette compiled = CompiledPalette
            .compile(MetalFlags.ALL_ON, palette(set, "style"), palette(set, "side"), palette(set, "building"));
        assertEquals(STONEBRICK, compiled.get('@', new Random(0)), "upstream copied the style layer's bricks");
        CompiledPalette withPart = CompiledPalette
            .compile(MetalFlags.ALL_ON, palette(set, "style"), palette(set, "side"))
            .withLayer(palette(set, "building"));
        assertEquals(STONEBRICK, withPart.get('@', new Random(0)));
    }

    @Test
    public void aLaterBlockReplacesAnEarlierReferenceAndChainsResolve() {
        AssetSet set = palettes(
            "lostcities/palettes/a.json",
            "{\"palette\": [{\"char\": \"x\", \"frompalette\": \"y\"}, {\"char\": \"y\", \"frompalette\": \"z\"},"
                + " {\"char\": \"z\", \"block\": \"minecraft:glass\"}, {\"char\": \"q\", \"frompalette\": \"q\"}]}",
            "lostcities/palettes/b.json",
            "{\"palette\": [{\"char\": \"y\", \"block\": \"minecraft:bricks\"}]}");
        CompiledPalette compiled = CompiledPalette.compile(MetalFlags.ALL_ON, palette(set, "a"), palette(set, "b"));
        assertEquals(BRICK, compiled.get('x', new Random(0)));
        assertEquals(BRICK, compiled.get('y', new Random(0)));
        assertFalse(compiled.isDefined('q'), "a reference to itself is a loop and stays undefined");
    }

    @Test
    public void doesNotReproduceASSET12_informationComesFromTheLastLayer() {
        AssetSet set = palettes(
            "lostcities/conditions/chestloot.json",
            "{\"values\": [{\"factor\": 1, \"value\": \"x\"}]}",
            "lostcities/palettes/a.json",
            "{\"palette\": [{\"char\": \"C\", \"block\": \"minecraft:chest\", \"loot\": \"chestloot\"}]}",
            "lostcities/palettes/b.json",
            "{\"palette\": [{\"char\": \"C\", \"block\": \"minecraft:bookshelf\"}]}");
        CompiledPalette both = CompiledPalette.compile(MetalFlags.ALL_ON, palette(set, "a"), palette(set, "b"));
        assertNull(both.info('C'), "a plain redefinition drops the earlier layer's loot");
        CompiledPalette only = CompiledPalette.compile(MetalFlags.ALL_ON, palette(set, "a"));
        assertEquals(
            AssetId.parse("chestloot"),
            only.info('C')
                .loot()
                .id());
    }

    @Test
    public void doesNotReproduceASSET30_damageBelongsToTheDeclaringCharacterAndReferencesCopyIt() {
        AssetSet set = palettes(
            "lostcities/palettes/a.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:bricks\", \"damaged\": \"minecraft:iron_bars\"},"
                + " {\"char\": \"X\", \"block\": \"minecraft:bricks\"}, {\"char\": \"@\", \"frompalette\": \"#\"}]}");
        CompiledPalette compiled = CompiledPalette.compile(MetalFlags.ALL_ON, palette(set, "a"));
        assertEquals(BARS, compiled.damaged('#'));
        assertNull(compiled.damaged('X'), "the same block under another character is not damaged");
        assertEquals(BARS, compiled.damaged('@'), "a reference takes its target's damage");
    }

    @Test
    public void doesNotReproduceASSET02_getAllReturnsTheDistinctBlocksOfARandomCharacter() {
        AssetSet set = palettes(
            "lostcities/palettes/a.json",
            "{\"palette\": [{\"char\": \"r\", \"blocks\": [{\"random\": 20, \"block\": \"minecraft:mossy_stone_bricks\"},"
                + " {\"random\": 1000, \"block\": \"minecraft:mossy_cobblestone\"}]}]}");
        CompiledPalette compiled = CompiledPalette.compile(MetalFlags.ALL_ON, palette(set, "a"));
        Set<BlockRef> all = compiled.getAll('r');
        assertEquals(2, all.size());
        assertTrue(all.contains(BlockRef.parse("minecraft:stonebrick:1")));
        assertTrue(compiled.isRandom('r'));
        assertEquals(BlockRef.parse("minecraft:stonebrick:1"), compiled.get('r', 19));
        assertEquals(BlockRef.parse("minecraft:mossy_cobblestone:0"), compiled.get('r', 20));
    }

    @Test
    public void metalFlagsChooseTheBlockAtCompileTime() {
        // The cabin's copper keeps upstream's look while copper is on, spruce planks while it is off (D29).
        AssetSet set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
        Palette cabin = set.part(AssetId.parse("cabin"))
            .palette();
        CompiledPalette on = CompiledPalette.compile(MetalFlags.ALL_ON, cabin);
        CompiledPalette off = CompiledPalette.compile(MetalFlags.ALL_OFF, cabin);
        assertEquals(BlockRef.parse("etfuturum:copper_block:1"), on.get('c', new Random(0)));
        assertEquals(BlockRef.parse("minecraft:planks:1"), off.get('c', new Random(0)));
        assertEquals(BlockRef.parse("minecraft:spruce_stairs:3"), off.get('1', new Random(0)));
    }
}
