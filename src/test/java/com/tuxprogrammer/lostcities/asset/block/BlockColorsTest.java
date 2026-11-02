package com.tuxprogrammer.lostcities.asset.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.Problems;

/** The shipped block colors (docs/design/0008-facade-color-feathering.md §1). */
public class BlockColorsTest {

    @Test
    public void theBundledTableHoldsTheCatalogsBlocks() {
        Problems problems = new Problems();
        BlockColors colors = BlockColors.bundled(BlockColorsTest.class, problems);
        assertTrue(
            problems.all()
                .isEmpty(),
            problems.all()
                .toString());
        assertTrue(colors.size() > 7000, "blocks: " + colors.size());
        for (String block : new String[] { "minecraft:sandstone:0", "minecraft:stonebrick:0",
            "minecraft:stained_hardened_clay:14", "minecraft:hardened_clay:0", "etfuturum:concrete:8",
            "minecraft:glass:0", "minecraft:dirt:1" }) {
            assertNotNull(colors.get(BlockRef.parse(block)), block);
        }
    }

    @Test
    public void glassIsSeeThroughAndStoneIsOpaque() {
        BlockColors colors = BlockColors.bundled(BlockColorsTest.class, new Problems());
        assertFalse(
            colors.get(BlockRef.of("minecraft:glass", 0))
                .isOpaque());
        assertTrue(
            colors.get(BlockRef.of("minecraft:stonebrick", 0))
                .isOpaque());
    }

    @Test
    public void sandstoneIsPaleAndRedTerracottaDarkAndSaturated() {
        BlockColors colors = BlockColors.bundled(BlockColorsTest.class, new Problems());
        BlockColors.Entry sandstone = colors.get(BlockRef.of("minecraft:sandstone", 0));
        BlockColors.Entry red = colors.get(BlockRef.of("minecraft:stained_hardened_clay", 14));
        assertTrue(sandstone.lightness() > 75, "sandstone L* " + sandstone.lightness());
        assertTrue(red.lightness() < 45, "red terracotta L* " + red.lightness());
        assertTrue(red.a() > 20, "red terracotta a* " + red.a());
    }

    @Test
    public void parsesLinesAndFallsBackToMetaZero() throws IOException {
        BlockColors colors = BlockColors.parse(
            new StringReader("# comment\nmod:block:0\t#ffffff\t100\nmod:glass:0\t#808080\t20\n"),
            new Problems());
        assertEquals(2, colors.size());
        BlockColors.Entry white = colors.get(BlockRef.of("mod:block", 7));
        assertNotNull(white, "meta 7 falls back to meta 0");
        assertEquals(100f, white.lightness(), 0.1f);
        assertEquals(0f, white.a(), 0.1f);
        assertEquals(0f, white.b(), 0.1f);
        assertNull(colors.get(BlockRef.of("mod:other", 0)));
    }

    @Test
    public void reportsAMalformedLine() throws IOException {
        Problems problems = new Problems();
        BlockColors colors = BlockColors.parse(new StringReader("mod:block:0\tffffff\n"), problems);
        assertEquals(0, colors.size());
        assertFalse(
            problems.all()
                .isEmpty());
    }

    @Test
    public void labOfBlackAndPrimaryRed() {
        float[] black = BlockColors.lab(0x000000);
        assertEquals(0f, black[0], 0.01f);
        float[] red = BlockColors.lab(0xFF0000);
        assertEquals(53.24f, red[0], 0.1f);
        assertEquals(80.09f, red[1], 0.2f);
        assertEquals(67.20f, red[2], 0.2f);
    }
}
