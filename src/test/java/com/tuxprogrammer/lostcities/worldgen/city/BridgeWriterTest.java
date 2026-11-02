package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** Bridges over non-city chunks (bridges.md, {@code Bridges.generateBridges}). */
public class BridgeWriterTest {

    /** A one-slice deck: a stair facing east at (0, 0), stone at (1, 0), and a character the palette lacks. */
    private static final AssetSet SET = TestAssets.loadFiles(
        "lostcities/palettes/p.json",
        "{\"palette\": [{\"char\": \"s\", \"block\": \"minecraft:oak_stairs[facing=east]\"},"
            + " {\"char\": \"a\", \"block\": \"minecraft:stone\"}]}",
        "lostcities/parts/deck.json",
        "{\"xsize\": 3, \"zsize\": 1, \"slices\": [[\"saQ\"]]}");

    private static final CompiledPalette PALETTE = CompiledPalette
        .compile(MetalFlags.ALL_ON, SET.palette(AssetId.parse("p")));

    private static final List<String> LOG = new ArrayList<>();
    private static final CityWriteContext CONTEXT = CityFixtures.plan(CityFixtures.SEED, StructureOracle.NONE, LOG::add)
        .context();

    private static ArrayChunkBuffer span(Orientation orientation) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        Part part = SET.part(AssetId.parse("deck"));
        BridgeWriter.span(
            CONTEXT,
            part,
            PALETTE,
            orientation,
            71,
            new BufferTarget(buffer, null),
            new PaletteSequence(1, 0, 0));
        return buffer;
    }

    @Test
    public void doesNotReproduceCITY30_aSpanAlongZTurnsItsBlocksAsWellAsItsPositions() {
        ArrayChunkBuffer x = span(Orientation.X);
        assertEquals(BlockRef.of("minecraft:oak_stairs", 0), x.get(0, 71, 0), "along X the stair faces east");
        assertEquals(BlockRef.of("minecraft:stone", 0), x.get(1, 71, 0));
        ArrayChunkBuffer z = span(Orientation.Z);
        // Transposed: the part's (1, 0) lands on (0, 1); the stair turns from east to south (metadata 2).
        assertEquals(BlockRef.of("minecraft:stone", 0), z.get(0, 71, 1));
        assertEquals(BlockRef.of("minecraft:oak_stairs", 2), z.get(0, 71, 0), "along Z it faces along the span");
    }

    @Test
    public void doesNotReproduceCITY38_anyHighwayWithinTheBridgesHeightStopsIt() {
        // A covered bridge is 7 high from Y 71: a level-1 highway at Y 77 would cut its roof.
        assertTrue(BridgeWriter.blocksHighway(0, 71, 7, CONTEXT));
        assertTrue(BridgeWriter.blocksHighway(1, 71, 7, CONTEXT), "upstream skipped only level 0");
        assertFalse(BridgeWriter.blocksHighway(2, 71, 7, CONTEXT), "level 2 clears every bundled bridge");
        assertFalse(BridgeWriter.blocksHighway(-1, 71, 7, CONTEXT), "no highway");
    }

    @Test
    public void doesNotReproduceHWY12_anUndefinedDeckCharacterIsSkippedNotThrown() {
        ArrayChunkBuffer x = span(Orientation.X);
        assertEquals(BlockRef.AIR, x.get(2, 71, 0));
        assertTrue(LOG.stream()
            .anyMatch(line -> line.contains("'Q'")), LOG.toString());
    }

    @Test
    public void theLogReportsEachProblemOnce() {
        span(Orientation.X);
        span(Orientation.X);
        assertEquals(
            1,
            LOG.stream()
                .filter(line -> line.contains("'Q'"))
                .count());
    }
}
