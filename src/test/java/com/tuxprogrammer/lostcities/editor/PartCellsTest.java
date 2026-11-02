package com.tuxprogrammer.lostcities.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.CityWriteContext;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.city.PaletteSequence;

/**
 * {@link PartCells}: the editor places a part with its chunk's palette, reads it back into a reverse palette, and an
 * untouched part exports to slices that place the same blocks again.
 */
public class PartCellsTest {

    /** A world of blocks keyed by chunk-local position, empty as air. */
    private static final class FakeWorld {

        final Map<Long, BlockRef> blocks = new HashMap<>();

        static long key(int x, int y, int z) {
            return ((long) y << 16) | (x << 8) | z;
        }

        void set(int x, int y, int z, BlockRef block) {
            blocks.put(key(x, y, z), block);
        }

        BlockRef get(int x, int y, int z) {
            BlockRef block = blocks.get(key(x, y, z));
            return block == null ? BlockRef.AIR : block;
        }
    }

    private static final LayoutCityPlan PLAN = CityFixtures.plan();

    private static int[] buildingChunk() {
        return CityFixtures.first(
            12,
            (x, z) -> PLAN.planner()
                .plan(x, z)
                .hasBuilding());
    }

    private static CompiledPalette palette(int[] chunk, Part part) {
        CityWriteContext context = PLAN.context();
        ChunkPlan plan = context.planner()
            .plan(chunk[0], chunk[1]);
        return context.palettes()
            .forPart(context.chunkPalette(plan), part);
    }

    @Test
    public void anUntouchedPartExportsToSlicesThatPlaceTheSameBlocks() {
        int[] chunk = buildingChunk();
        ChunkPlan plan = PLAN.planner()
            .plan(chunk[0], chunk[1]);
        Part part = CityFixtures.ASSETS.part(AssetId.parse(plan.floorPart(0)));
        assertNotNull(part);
        CompiledPalette palette = palette(chunk, part);
        FakeWorld world = new FakeWorld();
        int bottom = 70;
        int written = PartCells.place(
            part,
            palette,
            new PaletteSequence(CityFixtures.SEED, chunk[0], chunk[1]),
            Transform.ROTATE_NONE,
            0,
            0,
            bottom,
            world::set);
        assertEquals(part.xsize() * part.zsize() * part.height(), written, "every cell is written, spaces as air");

        Map<BlockRef, Character> reverse = PartCells
            .reversePalette(part, palette, Transform.ROTATE_NONE, 0, 0, bottom, world::get);
        assertFalse(reverse.isEmpty());
        PartExport.Result result = PartExport.export(
            part.xsize(),
            part.zsize(),
            part.height(),
            reverse,
            palette.characters(),
            null,
            PartCells.grid(Transform.ROTATE_NONE, 0, 0, bottom, world::get));
        assertEquals(0, result.newBlocks(), "every placed block has a character");
        JsonArray slices = new JsonParser().parse(result.json())
            .getAsJsonObject()
            .getAsJsonObject("exportedpart")
            .getAsJsonArray("slices");
        for (int y = 0; y < part.height(); y++) {
            JsonArray rows = slices.get(y)
                .getAsJsonArray();
            for (int z = 0; z < part.zsize(); z++) {
                String row = rows.get(z)
                    .getAsString();
                for (int x = 0; x < part.xsize(); x++) {
                    char c = row.charAt(x);
                    BlockRef block = world.get(x, bottom + y, z);
                    Character expected = reverse.get(block);
                    if (expected == null) {
                        assertEquals(BlockRef.AIR, block, "only air may lack a character");
                        expected = ' ';
                    }
                    assertEquals(expected.charValue(), c, "cell " + x + "," + y + "," + z + " holds " + block);
                    // The character placed again gives a block of the same character (random characters give any
                    // of their blocks).
                    if (c != ' ') {
                        assertTrue(palette.isDefined(c), "an exported character the palette defines: " + c);
                    }
                }
            }
        }
    }

    @Test
    public void aTransformedPartLandsWhereGenerationPutsIt() {
        int[] chunk = buildingChunk();
        ChunkPlan plan = PLAN.planner()
            .plan(chunk[0], chunk[1]);
        Part part = CityFixtures.ASSETS.part(AssetId.parse(plan.floorPart(0)));
        CompiledPalette palette = palette(chunk, part);
        Map<String, Character> seen = new HashMap<>();
        PartCells.forEach(part, Transform.ROTATE_90, 0, 0, (px, y, pz, lx, lz, c) -> {
            assertEquals(Transform.ROTATE_90.rotateX(px, pz), lx);
            assertEquals(Transform.ROTATE_90.rotateZ(px, pz), lz);
            seen.put(lx + "," + y + "," + lz, c);
        });
        assertEquals(part.xsize() * part.zsize() * part.height(), seen.size(), "a full-chunk part stays in its chunk");
        assertTrue(
            palette.characters()
                .size() > 0);
    }

    @Test
    public void spacesUndefinedCharactersAndHardAirPlaceAir() {
        int[] chunk = buildingChunk();
        ChunkPlan plan = PLAN.planner()
            .plan(chunk[0], chunk[1]);
        Part part = CityFixtures.ASSETS.part(AssetId.parse(plan.floorPart(0)));
        CompiledPalette palette = palette(chunk, part);
        PaletteSequence sequence = new PaletteSequence(CityFixtures.SEED, 0, 0);
        assertEquals(BlockRef.AIR, PartCells.blockFor(' ', palette, sequence, Transform.ROTATE_NONE));
        char undefined = '☃';
        assertFalse(palette.isDefined(undefined));
        assertEquals(BlockRef.AIR, PartCells.blockFor(undefined, palette, sequence, Transform.ROTATE_NONE));
    }

    @Test
    public void exportWritesUpstreamsThreeKeys() {
        BlockRef stone = BlockRef.of("minecraft:stone", 0);
        PartExport.Result result = PartExport.export(
            1,
            1,
            1,
            java.util.Collections.singletonMap(stone, '#'),
            java.util.Collections.singleton('#'),
            "common",
            (x, y, z) -> stone);
        JsonObject root = new JsonParser().parse(result.json())
            .getAsJsonObject();
        assertTrue(root.has("__comment__"));
        assertTrue(root.has("exportedpart"));
        assertFalse(root.has("missingpalette"), "no new block, no missing palette");
    }
}
