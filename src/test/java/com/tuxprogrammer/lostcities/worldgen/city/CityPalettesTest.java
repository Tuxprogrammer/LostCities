package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;

/** The world's palette caches and the style blocks read from them. */
public class CityPalettesTest {

    private static final AssetSet SET = TestAssets.loadFiles(
        "lostcities/palettes/base.json",
        "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:stone_bricks\", \"damaged\": \"minecraft:iron_bars\"},"
            + " {\"char\": \"b\", \"block\": \"minecraft:cobblestone\"}, {\"char\": \"w\", \"block\": \"minecraft:cobblestone_wall\"}]}",
        "lostcities/parts/own.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"x\"]], \"palette\": {\"palette\": [{\"char\": \"x\", \"block\": \"minecraft:glass\"}]}}",
        "lostcities/parts/plain.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]]}");

    @Test
    public void doesNotReproduceASSET26_aPartsPaletteIsCompiledOncePerChunkPalette() {
        CityPalettes palettes = new CityPalettes(SET, MetalFlags.ALL_ON);
        CityPalettes.ChunkPalette chunk = palettes.chunk(Collections.singletonList("lostcities:base"), null);
        Part own = SET.part(AssetId.parse("own"));
        CompiledPalette first = palettes.forPart(chunk, own);
        for (int i = 0; i < 50; i++) {
            assertSame(first, palettes.forPart(chunk, own), "the same compiled palette, not a new one per placement");
        }
        assertEquals(1, palettes.compiledPartPalettes());
        assertEquals(BlockRef.of("minecraft:glass", 0), first.get('x', 0));
        assertEquals(BlockRef.of("minecraft:stonebrick", 0), first.get('#', 0), "the chunk palette stays under it");
        assertSame(
            chunk.palette(),
            palettes.forPart(chunk, SET.part(AssetId.parse("plain"))),
            "no own palette, no compile");
    }

    @Test
    public void cachesStayWithinTheirBound() {
        CityPalettes palettes = new CityPalettes(SET, MetalFlags.ALL_ON);
        for (int i = 0; i < CityPalettes.CHUNK_CAPACITY * 3; i++) {
            palettes.chunk(Arrays.asList("lostcities:base"), "lostcities:building" + i);
        }
        assertTrue(palettes.cachedChunkPalettes() <= CityPalettes.CHUNK_CAPACITY);
    }

    @Test
    public void styleBlocksComeFromTheCityStylesCharactersAndTheDamagedEntries() {
        CityPalettes palettes = new CityPalettes(CityFixtures.ASSETS, MetalFlags.ALL_ON);
        CityPalettes.ChunkPalette chunk = palettes
            .chunk(Arrays.asList("lostcities:common", "lostcities:default"), "lostcities:building1");
        StyleBlocks style = palettes.styleBlocks(
            chunk,
            CityFixtures.ASSETS.cityStyle(AssetId.parse("citystyle_standard")),
            CityFixtures.ASSETS.building(AssetId.parse("building1")));
        assertNotNull(style.border());
        assertNotNull(style.filler());
        assertEquals(
            128,
            style.leaves()
                .size());
        assertEquals(
            128,
            style.rubbleDirt()
                .size());
        assertSame(
            style,
            palettes.styleBlocks(
                chunk,
                CityFixtures.ASSETS.cityStyle(AssetId.parse("citystyle_standard")),
                CityFixtures.ASSETS.building(AssetId.parse("building1"))));
    }
}
