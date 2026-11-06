package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/**
 * A lane crossing a chunk border (D-kib-1, known issue of 0.1.0-beta): the two chunks read only the plan, so the
 * lane's heights either side of the border differ by at most one block in any generation order.
 */
public class LaneSeamTest {

    private static LanePath eastWestLane() {
        boolean[] mask = new boolean[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 7; z <= 9; z++) {
                mask[(z << 4) | x] = true;
            }
        }
        return LanePath.of(mask, LanePath.Material.PLAIN);
    }

    private static ChunkPlan plan(int chunkX, int natural) {
        return ChunkPlan.builder(chunkX, 0, ChunkKind.NONE)
            .naturalHeight(natural)
            .lane(eastWestLane())
            .build();
    }

    private static ChunkPlan other(int natural) {
        return ChunkPlan.builder(0, 5, ChunkKind.NONE)
            .naturalHeight(natural)
            .build();
    }

    private static ArrayChunkBuffer flat(int top) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < top; y++) {
                    buffer.set(x, y, z, TestTerrain.STONE);
                }
                buffer.set(x, top, z, TestTerrain.GRASS);
            }
        }
        return buffer;
    }

    private static int surface(ArrayChunkBuffer buffer, TerrainContext context, int x) {
        return ColumnOps.surface(buffer, context.classifier(), x, 8, true);
    }

    @Test
    public void neighboringChunksMeetWithinOneBlock() {
        TerrainContext context = new TerrainContext(
            1L,
            0,
            TerrainSettings.defaults(),
            new NameBlockClassifier(),
            BlockAvailability.ALL);
        // West chunk's ground is 70, east chunk's is 73: before the fix the seam columns stayed 3 apart.
        ChunkPlan west = plan(0, 70);
        ChunkPlan east = plan(1, 73);
        ChunkPlan[] westSides = { other(70), east, other(70), other(70) };
        ChunkPlan[] eastSides = { west, other(73), other(73), other(73) };
        ArrayChunkBuffer a = flat(70);
        ArrayChunkBuffer b = flat(73);
        VillageLanes.relax(a, context, west.lane(), VillageLanes.seamWindows(west, westSides));
        VillageLanes.relax(b, context, east.lane(), VillageLanes.seamWindows(east, eastSides));
        int seamA = surface(a, context, 15);
        int seamB = surface(b, context, 0);
        assertTrue(Math.abs(seamA - seamB) <= 1, seamA + " to " + seamB);
        for (int x = 0; x < 15; x++) {
            assertTrue(Math.abs(surface(a, context, x) - surface(a, context, x + 1)) <= 1);
            assertTrue(Math.abs(surface(b, context, x) - surface(b, context, x + 1)) <= 1);
        }
    }

    @Test
    public void noWindowWhereTheNeighborHasNoLane() {
        ChunkPlan west = plan(0, 70);
        assertNull(VillageLanes.seamWindows(west, new ChunkPlan[] { other(70), other(70), other(70), other(70) }));
    }

    @Test
    public void windowIsTheSameFromBothSides() {
        ChunkPlan west = plan(0, 70);
        ChunkPlan east = plan(1, 73);
        int[][] w = VillageLanes.seamWindows(west, new ChunkPlan[] { other(70), east, other(70), other(70) });
        int[][] e = VillageLanes.seamWindows(east, new ChunkPlan[] { west, other(73), other(73), other(73) });
        assertEquals(w[0][(8 << 4) | 15], e[0][(8 << 4)]);
        assertEquals(w[1][(8 << 4) | 15], e[1][(8 << 4)]);
    }
}
