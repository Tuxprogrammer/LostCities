package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.asset.variety.PaletteVariety;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/**
 * AR-3 (alpha review): a building's floor parts take the palette variety's interior floor, ceiling and ceiling-light
 * characters where the palette defines them, and only inside the building.
 */
public class InteriorCharactersTest {

    private static final char F = PaletteVariety.FLOOR;
    private static final char C = PaletteVariety.CEILING;
    private static final char L = PaletteVariety.CEILING_LIGHT;
    private static final int Y = 70;

    /**
     * A 3 by 3 room four slices high: walls round the edge, a floor and a ceiling over the middle column, and a corner
     * column open to the sky, as a roof terrace is.
     */
    private static final AssetSet SET = TestAssets.loadFiles(
        "lostcities/palettes/walls.json",
        "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:stonebrick:0\"},"
            + " {\"char\": \"S\", \"block\": \"minecraft:double_stone_slab:0\"}]}",
        "lostcities/palettes/interior.json",
        "{\"palette\": [{\"char\": \"\\ue001\", \"block\": \"minecraft:planks:1\"},"
            + " {\"char\": \"\\ue002\", \"block\": \"minecraft:quartz_block:0\"},"
            + " {\"char\": \"\\ue003\", \"block\": \"minecraft:glowstone:0\"}]}",
        "lostcities/parts/room.json",
        "{\"xsize\": 3, \"zsize\": 3, \"slices\": [[\"###\", \"#S#\", \"###\"], [\"###\", \"# #\", \"## \"],"
            + " [\"###\", \"# #\", \"## \"], [\"###\", \"###\", \"## \"]]}");

    private static ArrayChunkBuffer write(boolean interior, String... palettes) {
        java.util.List<com.tuxprogrammer.lostcities.asset.model.Palette> layers = new java.util.ArrayList<>();
        for (String name : palettes) {
            layers.add(SET.palette(AssetId.parse(name)));
        }
        CompiledPalette palette = CompiledPalette.compile(MetalFlags.ALL_ON, layers);
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        new PartWriter(
            new BufferTarget(buffer, null),
            new PaletteSequence(1, 0, 0),
            new PartWriter.Rules(true, true, false, false, false, 63, BlockRef.of("minecraft:water", 0)),
            LayoutLog.SILENT).place(
                new PartWriter.Placement(
                    SET.part(AssetId.parse("room")),
                    palette,
                    Transform.ROTATE_NONE,
                    0,
                    Y,
                    0,
                    HardAir.AIR,
                    null).interior(interior));
        return buffer;
    }

    @Test
    public void theRoomTakesItsFloorAndCeilingAndTheWallsStay() {
        ArrayChunkBuffer buffer = write(true, "walls", "interior");
        assertEquals(BlockRef.of("minecraft:planks", 1), buffer.get(1, Y, 1), "the floor under the room");
        assertEquals(BlockRef.of("minecraft:quartz_block", 0), buffer.get(1, Y + 3, 1), "the ceiling over it");
        assertEquals(BlockRef.of("minecraft:stonebrick", 0), buffer.get(0, Y + 1, 0), "a wall");
        assertEquals(BlockRef.of("minecraft:stonebrick", 0), buffer.get(0, Y, 0), "the floor under a wall");
        assertEquals(
            BlockRef.of("minecraft:stonebrick", 0),
            buffer.get(2, Y, 2),
            "a terrace open to the sky keeps the part's block");
    }

    @Test
    public void withoutTheVarietyOrOutsideABuildingNothingChanges() {
        assertEquals(BlockRef.of("minecraft:double_stone_slab", 0), write(true, "walls").get(1, Y, 1));
        assertEquals(BlockRef.of("minecraft:double_stone_slab", 0), write(false, "walls", "interior").get(1, Y, 1));
        assertEquals(BlockRef.of("minecraft:stonebrick", 0), write(false, "walls", "interior").get(1, Y + 3, 1));
    }

    @Test
    public void theRulesReadOneColumn() {
        char[] room = { '#', ' ', ' ', ' ', '#', '#' };
        assertEquals(F, PartWriter.interiorCharacter(room, 0, 0, 0, true, true, true));
        assertEquals(C, PartWriter.interiorCharacter(room, 4, 0, 0, true, true, true));
        assertEquals('#', PartWriter.interiorCharacter(room, 5, 0, 0, true, true, true), "only the lowest course");
        assertEquals(L, PartWriter.interiorCharacter(room, 4, 2, 6, true, true, true), "on the light grid");
        assertEquals('#', PartWriter.interiorCharacter(room, 4, 2, 6, true, false, false), "no ceiling characters");
        assertEquals(L, PartWriter.interiorCharacter(room, 4, 2, 6, true, false, true), "lights alone");
        char[] roof = { '#', ' ', ' ', ' ' };
        assertEquals('#', PartWriter.interiorCharacter(roof, 0, 0, 0, true, true, true), "no ceiling: a roof");
        char[] slab = { 'S', ' ', 'Q' };
        assertEquals(F, PartWriter.interiorCharacter(slab, 0, 0, 0, true, true, true), "a slab floor too");
        assertEquals('Q', PartWriter.interiorCharacter(slab, 2, 0, 0, true, true, true), "quartz keeps its look");
        char[] solid = { '#', '#', '#' };
        for (int y = 0; y < solid.length; y++) {
            assertEquals('#', PartWriter.interiorCharacter(solid, y, 2, 2, true, true, true), "a wall column");
        }
    }

    @Test
    public void theLightGridIsOnePerFourByFourBay() {
        int lights = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (PartWriter.onLightGrid(x, z)) {
                    lights++;
                    assertTrue(x % 4 == 2 && z % 4 == 2);
                }
            }
        }
        assertEquals(16, lights);
        assertFalse(PartWriter.onLightGrid(0, 0));
    }
}
