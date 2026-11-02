package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** Upstream's {@code generatePart} as {@link PartWriter} (chunk-driver-and-parts.md). */
public class PartWriterTest {

    private static final BlockRef STONE = BlockRef.of("minecraft:stone", 0);
    private static final BlockRef WATER = BlockRef.of("minecraft:water", 0);
    private static final BlockRef DIRT = BlockRef.of("minecraft:dirt", 0);

    /** A palette with one of each kind of character, and three parts over it. */
    private static final AssetSet SET = TestAssets.loadFiles(
        "lostcities/palettes/p.json",
        "{\"palette\": [{\"char\": \"a\", \"block\": \"minecraft:stone\"},"
            + " {\"char\": \"s\", \"block\": \"minecraft:oak_stairs[facing=east]\"},"
            + " {\"char\": \"h\", \"block\": \"minecraft:structure_void\"},"
            + " {\"char\": \"w\", \"block\": \"minecraft:water\"},"
            + " {\"char\": \"x\", \"block\": \"minecraft:air\"},"
            + " {\"char\": \"f\", \"block\": \"minecraft:poppy\"},"
            + " {\"char\": \"T\", \"block\": \"minecraft:torch\", \"torch\": true},"
            + " {\"char\": \"1\", \"block\": \"minecraft:spawner\", \"mob\": \"mobs\"}]}",
        "lostcities/conditions/mobs.json",
        "{\"values\": [{\"factor\": 1, \"value\": \"minecraft:zombie\"}]}",
        "lostcities/parts/column.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"a\"], [\"h\"], [\"x\"], [\"w\"], [\"T\"], [\"1\"], [\"f\"]]}",
        "lostcities/parts/stairs.json",
        "{\"xsize\": 16, \"zsize\": 16, \"slices\": [[\"s               \", \"                \", \"                \","
            + " \"                \", \"                \", \"                \", \"                \","
            + " \"                \", \"                \", \"                \", \"                \","
            + " \"                \", \"                \", \"                \", \"                \","
            + " \"                \"]]}",
        "lostcities/parts/broken.json",
        "{\"xsize\": 2, \"zsize\": 1, \"slices\": [[\"aQ\"]]}");

    private static final CompiledPalette PALETTE = CompiledPalette
        .compile(MetalFlags.ALL_ON, SET.palette(AssetId.parse("p")));

    private static Part part(String name) {
        return SET.part(AssetId.parse(name));
    }

    private static PartWriter.Rules rules(boolean lighting, boolean noLoot, boolean avoidWater) {
        return new PartWriter.Rules(lighting, true, avoidWater, false, noLoot, 63, WATER);
    }

    private static ArrayChunkBuffer filled() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int y = 60; y < 80; y++) {
            buffer.set(0, y, 0, DIRT);
        }
        return buffer;
    }

    private static PartWriter.Placement at(Part part, Transform transform, int ox, int oy, int oz, HardAir hardAir) {
        return new PartWriter.Placement(part, PALETTE, transform, ox, oy, oz, hardAir, null);
    }

    @Test
    public void eachKindOfCharacterBecomesWhatUpstreamWrites() {
        ArrayChunkBuffer buffer = filled();
        BufferTarget target = new BufferTarget(buffer, null);
        PartWriter writer = new PartWriter(target, new PaletteSequence(1, 0, 0), rules(false, false, false), LayoutLog.SILENT);
        int top = writer.place(at(part("column"), Transform.ROTATE_NONE, 0, 60, 0, HardAir.VOID));
        assertEquals(67, top, "the Y above the last slice");
        assertEquals(STONE, buffer.get(0, 60, 0));
        assertEquals(DIRT, buffer.get(0, 61, 0), "hard air under VOID keeps the world's block");
        assertEquals(DIRT, buffer.get(0, 62, 0), "air in a part leaves the world's block");
        assertEquals(WATER, buffer.get(0, 63, 0), "liquid stays liquid without avoidWater");
        assertEquals(BlockRef.AIR, buffer.get(0, 64, 0), "a torch is air without generateLighting");
        assertEquals("minecraft:mob_spawner", buffer.get(0, 65, 0).name(), "a spawner with loot allowed");
        assertEquals("minecraft:red_flower", buffer.get(0, 66, 0).name(), "a flower where foliage is allowed");
    }

    @Test
    public void hardAirFollowsTheSettingAndNoLootRemovesSpawners() {
        ArrayChunkBuffer buffer = filled();
        PartWriter writer = new PartWriter(new BufferTarget(buffer, null), new PaletteSequence(1, 0, 0), rules(true, true, true),
            LayoutLog.SILENT);
        writer.place(at(part("column"), Transform.ROTATE_NONE, 0, 60, 0, HardAir.WATERLEVEL));
        assertEquals(WATER, buffer.get(0, 61, 0), "hard air below the water level becomes liquid");
        assertEquals(BlockRef.AIR, buffer.get(0, 63, 0), "avoidWater turns liquid into air");
        assertEquals("minecraft:torch", buffer.get(0, 64, 0).name(), "a torch with generateLighting");
        assertEquals(BlockRef.AIR, buffer.get(0, 65, 0), "no spawner in a building without loot");
    }

    @Test
    public void doesNotReproduceASSET18_directionalBlocksTurnWithThePart() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        PartWriter writer = new PartWriter(new BufferTarget(buffer, null), new PaletteSequence(1, 0, 0), rules(false, false, false),
            LayoutLog.SILENT);
        writer.place(at(part("stairs"), Transform.ROTATE_180, 0, 70, 0, HardAir.VOID));
        // The part's (0, 0) lands on (15, 15); stairs facing east (meta 0) turned 180° face west (meta 1).
        assertEquals(BlockRef.of("minecraft:oak_stairs", 1), buffer.get(15, 70, 15));
        assertEquals(BlockRef.AIR, buffer.get(0, 70, 0));
    }

    @Test
    public void doesNotReproduceHWY12_anUndefinedCharacterIsSkippedAndReportedOnce() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        List<String> log = new ArrayList<>();
        java.util.Set<String> once = new java.util.HashSet<>();
        PartWriter writer = new PartWriter(new BufferTarget(buffer, null), new PaletteSequence(1, 0, 0), rules(false, false, false),
            message -> {
                if (once.add(message)) {
                    log.add(message);
                }
            });
        writer.place(at(part("broken"), Transform.ROTATE_NONE, 0, 70, 0, HardAir.AIR));
        writer.place(at(part("broken"), Transform.ROTATE_NONE, 0, 71, 0, HardAir.AIR));
        assertEquals(STONE, buffer.get(0, 70, 0), "the defined character is written");
        assertEquals(BlockRef.AIR, buffer.get(1, 70, 0), "the undefined one is skipped, not thrown");
        assertEquals(1, log.size(), log.toString());
        assertTrue(log.get(0).contains("'Q'"), log.toString());
    }

    @Test
    public void doesNotReproduceTERR14_aPositionOutsideTheChunkIsDroppedNotAliased() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        PartWriter writer = new PartWriter(new BufferTarget(buffer, null), new PaletteSequence(1, 0, 0), rules(false, false, false),
            LayoutLog.SILENT);
        // Offset so the part's (0, 0) would land at x 16: upstream's driver aliased it onto x 0.
        writer.place(at(part("stairs"), Transform.ROTATE_NONE, 16, 70, 0, HardAir.VOID));
        for (int x = 0; x < 16; x++) {
            assertEquals(BlockRef.AIR, buffer.get(x, 70, 0));
        }
    }
}
