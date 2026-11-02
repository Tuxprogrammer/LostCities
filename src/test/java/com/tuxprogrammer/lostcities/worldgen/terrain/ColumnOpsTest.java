package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** Upstream's column operations, against docs/original-spec/03-terrain/city-chunk-terrain.md. */
public class ColumnOpsTest {

    private static final BlockClassifier CLASSES = new NameBlockClassifier();
    private static final BlockRef STONE = TestTerrain.STONE;

    private static ArrayChunkBuffer column(int top, BlockRef surface) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int y = 0; y < top; y++) {
            buffer.set(0, y, 0, y < 5 ? TestTerrain.BEDROCK : STONE);
        }
        buffer.set(0, top, 0, surface);
        return buffer;
    }

    @Test
    public void moveDownKeepsSixSurfaceLayersBelowTheTarget() {
        ArrayChunkBuffer buffer = column(90, TestTerrain.GRASS);
        for (int y = 86; y < 90; y++) {
            buffer.set(0, y, 0, TestTerrain.DIRT);
        }
        buffer.set(0, 91, 0, TestTerrain.LOG);
        buffer.set(0, 92, 0, TestTerrain.LEAVES);
        int top = ColumnOps.moveDown(buffer, CLASSES, 0, 0, 72, WorldLimits.BUILD_HEIGHT, 63);
        assertEquals(71, top, "new top is the target minus one");
        assertEquals(TestTerrain.GRASS, buffer.get(0, 71, 0), "the surface block moves down");
        assertEquals(TestTerrain.DIRT, buffer.get(0, 70, 0));
        assertEquals(STONE, buffer.get(0, 66, 0), "the sixth kept layer is the stone under four dirt");
        for (int y = 72; y <= 92; y++) {
            assertEquals(BlockRef.AIR, buffer.get(0, y, 0), "cleared, plants included, at " + y);
        }
    }

    @Test
    public void moveDownLeavesALowColumnAlone() {
        ArrayChunkBuffer buffer = column(60, TestTerrain.GRASS);
        assertEquals(ColumnOps.NOT_TOUCHED, ColumnOps.moveDown(buffer, CLASSES, 0, 0, 72, 256, 63));
        assertEquals(TestTerrain.GRASS, buffer.get(0, 60, 0));
    }

    @Test
    public void doesNotReproduceTERR39_terrainExactlyAtTheTargetIsLowered() {
        ArrayChunkBuffer buffer = column(72, TestTerrain.SAND);
        int top = ColumnOps.moveDown(buffer, CLASSES, 0, 0, 72, WorldLimits.BUILD_HEIGHT, 63);
        assertEquals(71, top, "a surface exactly at the first Y that must be clear is lowered too");
        assertEquals(BlockRef.AIR, buffer.get(0, 72, 0), "nothing is left standing at the target");
        assertEquals(TestTerrain.SAND, buffer.get(0, 71, 0), "the surface block moves down one");
    }

    @Test
    public void moveDownLeavesAColumnJustBelowTheTargetAlone() {
        ArrayChunkBuffer buffer = column(71, TestTerrain.GRASS);
        assertEquals(ColumnOps.NOT_TOUCHED, ColumnOps.moveDown(buffer, CLASSES, 0, 0, 72, 256, 63));
        assertEquals(TestTerrain.GRASS, buffer.get(0, 71, 0));
    }

    @Test
    public void moveUpCopiesTheColumnUntilAnAirGap() {
        ArrayChunkBuffer buffer = column(64, TestTerrain.GRASS);
        buffer.set(0, 55, 0, BlockRef.AIR);
        int touched = ColumnOps.moveUp(buffer, CLASSES, 0, 0, 71);
        assertEquals(64, touched, "the copy starts at the old surface");
        assertEquals(TestTerrain.GRASS, buffer.get(0, 71, 0), "the surface rises to the target");
        assertEquals(STONE, buffer.get(0, 70, 0));
        // Source rows 64 down to 56 were copied to 71 down to 63; the air gap at 55 ends the copy.
        assertEquals(STONE, buffer.get(0, 63, 0));
        assertEquals(STONE, buffer.get(0, 64, 0), "the old surface is covered by the shifted column");
        assertEquals(BlockRef.AIR, buffer.get(0, 55, 0), "the column under the shifted part keeps its cave");
    }

    @Test
    public void fillEmptyBelowStopsAtTheFirstSolidBlock() {
        ArrayChunkBuffer buffer = column(60, STONE);
        buffer.set(0, 50, 0, BlockRef.AIR);
        ColumnOps.fillEmptyBelow(buffer, CLASSES, 0, 0, 70, STONE);
        for (int y = 61; y <= 70; y++) {
            assertEquals(STONE, buffer.get(0, y, 0));
        }
        assertEquals(BlockRef.AIR, buffer.get(0, 50, 0), "a cave under solid rock stays");
    }

    @Test
    public void doesNotReproduceTERR23_loweredSubmergedSurfaceKeepsItsWater() {
        ArrayChunkBuffer buffer = column(60, TestTerrain.SAND);
        for (int y = 61; y < 63; y++) {
            buffer.set(0, y, 0, TestTerrain.WATER);
        }
        ColumnOps.moveDown(buffer, CLASSES, 0, 0, 56, 256, 63);
        for (int y = 56; y <= 60; y++) {
            assertEquals(TestTerrain.WATER, buffer.get(0, y, 0), "cleared bed below the water level is water at " + y);
        }
        assertEquals(TestTerrain.SAND, buffer.get(0, 55, 0), "the bed moved down");
    }

    @Test
    public void doesNotReproduceTERR06_clearRangeWithWaterLeavesNoRowBehind() {
        ArrayChunkBuffer buffer = column(100, STONE);
        ColumnOps.clearRange(buffer, 0, 0, 50, 80, true, 63, TestTerrain.WATER);
        for (int y = 50; y < 63; y++) {
            assertEquals(TestTerrain.WATER, buffer.get(0, y, 0), "liquid below the water level at " + y);
        }
        for (int y = 63; y < 80; y++) {
            assertEquals(BlockRef.AIR, buffer.get(0, y, 0), "air from the water level up at " + y);
        }
    }

    @Test
    public void doesNotReproduceTERR24_boundsAreTheWorldLimits() {
        ArrayChunkBuffer buffer = column(254, STONE);
        assertDoesNotThrow(() -> ColumnOps.moveUp(buffer, CLASSES, 0, 0, 400));
        assertDoesNotThrow(() -> ColumnOps.moveDown(buffer, CLASSES, 0, 0, 10, 400, 63));
        assertDoesNotThrow(() -> ColumnOps.clearRange(buffer, 0, 0, -20, 300, false, 63, TestTerrain.WATER));
        assertDoesNotThrow(() -> ColumnOps.fill(buffer, 0, 0, -5, 512, STONE));
        assertEquals(STONE, buffer.get(0, WorldLimits.MAX_Y, 0));
    }
}
