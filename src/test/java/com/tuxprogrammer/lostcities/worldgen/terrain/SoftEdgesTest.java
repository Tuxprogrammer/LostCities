package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/** Soft-edge strips toward landmark cores (0006 §4, rules E2 to E4; P03-13). */
public class SoftEdgesTest {

    private static final TerrainContext CONTEXT = TestTerrain.context(4L);

    private static ArrayChunkBuffer flat(int top) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < top; y++) {
                    buffer.set(x, y, z, TestTerrain.DIRT);
                }
                buffer.set(x, top, z, TestTerrain.GRASS);
            }
        }
        return buffer;
    }

    private static ChunkPlan[] coreToTheEast(ChunkPlan plan) {
        ChunkPlan[] sides = new ChunkPlan[4];
        Arrays.fill(sides, plan);
        sides[ChunkPlan.Side.EAST.ordinal()] = ChunkPlan.builder(1, 0, ChunkKind.LANDMARK_CORE)
            .groundY(66)
            .naturalHeight(66)
            .build();
        return sides;
    }

    @Test
    public void aStripRampsLinearlyFromTheCoresEdgeToTheCityGround() {
        ChunkPlan plan = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .build();
        ArrayChunkBuffer buffer = flat(66);
        int[] targets = new int[256];
        int[] depth = new int[256];
        int[] inner = new int[256];
        SoftEdges.targets(buffer, CONTEXT, 0, 0, plan, coreToTheEast(plan), targets, depth, inner);
        assertEquals(66, targets[(8 << 4) | 15], "the edge column keeps its own height");
        assertEquals(0, depth[(8 << 4) | 15]);
        assertEquals(71, targets[(8 << 4) | 3], "inside the city: the ground");
        int previous = 66;
        for (int x = 15; x >= 0 && depth[(8 << 4) | x] != SoftEdges.NOT_IN_STRIP; x--) {
            int target = targets[(8 << 4) | x];
            assertTrue(target >= previous, "rises toward the city at x " + x);
            assertTrue(target - previous <= SoftEdges.RISE_PER_COLUMN, "at most two blocks per column at x " + x);
            previous = target;
        }
    }

    @Test
    public void aStripMeetsAPondAtItsWaterlineNotItsBed() {
        ChunkPlan plan = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .build();
        // The village's pond reaches into the city chunk's edge columns: a bed at 60 under water up to Y 62.
        ArrayChunkBuffer buffer = flat(66);
        for (int x = 12; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 61; y <= 66; y++) {
                    buffer.set(x, y, z, y <= 62 ? TestTerrain.WATER : BlockRef.AIR);
                }
                buffer.set(x, 60, z, TestTerrain.DIRT);
            }
        }
        int[] targets = new int[256];
        int[] depth = new int[256];
        int[] inner = new int[256];
        SoftEdges.targets(buffer, CONTEXT, 0, 0, plan, coreToTheEast(plan), targets, depth, inner);
        for (int z = 0; z < 16; z++) {
            assertEquals(62, targets[(z << 4) | 15], "the edge column becomes shore at the waterline, row " + z);
        }
        int previous = 62;
        for (int x = 14; x >= 0 && depth[(8 << 4) | x] != SoftEdges.NOT_IN_STRIP; x--) {
            int target = targets[(8 << 4) | x];
            assertTrue(target >= previous && target - previous <= SoftEdges.RISE_PER_COLUMN, "x " + x);
            previous = target;
        }
        assertEquals(
            62,
            SoftEdges.waterline(
                buffer,
                TestTerrain.context(4L)
                    .classifier(),
                15,
                3,
                60));
        assertEquals(
            66,
            SoftEdges.waterline(
                flat(66),
                TestTerrain.context(4L)
                    .classifier(),
                15,
                3,
                66),
            "dry land");
    }

    @Test
    public void aSteepStepLeavesARetainingWallNoTallerThanNeeded() {
        ChunkPlan plan = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(101)
            .build();
        ArrayChunkBuffer buffer = flat(66);
        int[] targets = new int[256];
        int[] depth = new int[256];
        int[] inner = new int[256];
        SoftEdges.targets(buffer, CONTEXT, 0, 0, plan, coreToTheEast(plan), targets, depth, inner);
        int wallIndex = -1;
        for (int x = 0; x < 16; x++) {
            int index = (8 << 4) | x;
            if (depth[index] != SoftEdges.NOT_IN_STRIP && inner[index] != 101) {
                wallIndex = index;
                break;
            }
        }
        assertTrue(wallIndex >= 0, "a step of 35 needs a retaining wall");
        ArrayChunkBuffer leveled = flat(66);
        SoftEdges.retainingWalls(leveled, plan, targets, depth, inner);
        int x = wallIndex & 15;
        assertEquals(BlockRef.of("minecraft:cobblestone", 0), leveled.get(x, 101, 8), "the wall reaches the ground");
        assertEquals(BlockRef.AIR, leveled.get(x, 102, 8), "and no higher");
    }
}
