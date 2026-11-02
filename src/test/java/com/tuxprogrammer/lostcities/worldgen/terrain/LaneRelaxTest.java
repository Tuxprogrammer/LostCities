package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/**
 * Lane relaxation (M12, docs/design/0009-city-edges-and-character.md §5): no two lane neighbors differ by two blocks
 * or more, so every rise can get its stair or slab, and the chunk's border columns keep their heights.
 */
public class LaneRelaxTest {

    /** A three-wide lane along X at Z 7 to 9, with a 4-block cliff at X 8. */
    private static boolean[] mask() {
        boolean[] mask = new boolean[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 7; z <= 9; z++) {
                mask[(z << 4) | x] = true;
            }
        }
        return mask;
    }

    private static int[] cliff() {
        int[] heights = new int[256];
        for (int index = 0; index < 256; index++) {
            heights[index] = (index & 15) < 8 ? 70 : 74;
        }
        return heights;
    }

    @Test
    public void neighborsDifferByAtMostOne() {
        boolean[] mask = mask();
        int[] targets = VillageLanes.relaxedTargets(cliff(), mask);
        for (int index = 0; index < 256; index++) {
            if (!mask[index]) {
                continue;
            }
            int x = index & 15;
            int z = index >> 4;
            if (x < 15 && mask[index + 1]) {
                assertTrue(Math.abs(targets[index] - targets[index + 1]) <= 1, "x " + x + " z " + z);
            }
            if (z < 15 && mask[index + 16]) {
                assertTrue(Math.abs(targets[index] - targets[index + 16]) <= 1, "x " + x + " z " + z);
            }
        }
    }

    @Test
    public void theBorderColumnsKeepTheirHeights() {
        boolean[] mask = mask();
        int[] heights = cliff();
        int[] targets = VillageLanes.relaxedTargets(heights, mask);
        for (int z = 7; z <= 9; z++) {
            assertEquals(70, targets[z << 4], "west border");
            assertEquals(74, targets[(z << 4) | 15], "east border");
        }
    }

    @Test
    public void aGentleLaneIsLeftAlone() {
        boolean[] mask = mask();
        int[] heights = new int[256];
        for (int index = 0; index < 256; index++) {
            heights[index] = 70 + (index & 15) / 4;
        }
        int[] targets = VillageLanes.relaxedTargets(heights, mask);
        for (int index = 0; index < 256; index++) {
            if (mask[index]) {
                assertEquals(heights[index], targets[index]);
            }
        }
    }

    @Test
    public void relaxMovesTheColumnsInTheBuffer() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = x < 8 ? 70 : 74;
                for (int y = 0; y < top; y++) {
                    buffer.set(x, y, z, TestTerrain.STONE);
                }
                buffer.set(x, top, z, TestTerrain.GRASS);
            }
        }
        TerrainContext context = new TerrainContext(
            1L,
            0,
            TerrainSettings.defaults(),
            new NameBlockClassifier(),
            BlockAvailability.ALL);
        LanePath lane = LanePath.of(mask(), LanePath.Material.PLAIN);
        assertTrue(VillageLanes.relax(buffer, context, lane) > 0);
        for (int x = 0; x < 15; x++) {
            int a = ColumnOps.surface(buffer, context.classifier(), x, 8, true);
            int b = ColumnOps.surface(buffer, context.classifier(), x + 1, 8, true);
            assertTrue(Math.abs(a - b) <= 1, "x " + x + ": " + a + " to " + b);
        }
    }
}
