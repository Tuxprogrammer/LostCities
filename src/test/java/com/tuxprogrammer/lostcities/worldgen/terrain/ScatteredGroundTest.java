package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.GroundFit;

/**
 * {@link ScatteredGround}: the pad ends at the structure's ground, the fade meets RWG's terrain with no cliff, and two
 * chunks fitted apart meet at their border (design record 0009).
 */
public class ScatteredGroundTest {

    private static final TerrainContext CONTEXT = TestTerrain.context(4242L);

    /** A pad in chunk (10, 10) on blocks 163..172, ground 4 blocks above or below the fixture's surface there. */
    private static GroundFit fit(int groundY, GroundFit.Mode mode) {
        return new GroundFit(new int[] { 163, 163, 172, 172 }, groundY, mode);
    }

    private static int surface(ArrayChunkBuffer buffer, int x, int z) {
        return ColumnOps.surface(buffer, CONTEXT.classifier(), x, z, true);
    }

    @Test
    public void thePadEndsAtTheGround() {
        int natural = TestTerrain.surface(168, 168);
        for (int offset : new int[] { -5, 5 }) {
            GroundFit fit = fit(natural + offset, GroundFit.Mode.LEVEL);
            ArrayChunkBuffer buffer = TestTerrain.chunk(10, 10);
            ScatteredGround.apply(buffer, CONTEXT, 10, 10, Collections.singletonList(fit));
            for (int x = 3; x <= 12; x++) {
                for (int z = 3; z <= 12; z++) {
                    assertEquals(natural + offset, surface(buffer, x, z), "offset " + offset + " at " + x + "," + z);
                }
            }
        }
    }

    @Test
    public void theFadeMeetsTheTerrainWithoutACliff() {
        int natural = TestTerrain.surface(168, 168);
        GroundFit fit = fit(natural + 6, GroundFit.Mode.LEVEL);
        // The pad's chunk and its eight neighbors, each fitted alone, then compared across every border.
        int[][] heights = new int[48][48];
        for (int cz = 9; cz <= 11; cz++) {
            for (int cx = 9; cx <= 11; cx++) {
                ArrayChunkBuffer buffer = TestTerrain.chunk(cx, cz);
                ScatteredGround.apply(buffer, CONTEXT, cx, cz, fit);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        heights[(cx - 9) * 16 + x][(cz - 9) * 16 + z] = surface(buffer, x, z);
                    }
                }
            }
        }
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 48; z++) {
                if (x + 1 < 48) {
                    assertTrue(Math.abs(heights[x + 1][z] - heights[x][z]) <= 2, "step at " + x + "," + z);
                }
                if (z + 1 < 48) {
                    assertTrue(Math.abs(heights[x][z + 1] - heights[x][z]) <= 2, "step at " + x + "," + z);
                }
            }
        }
        // Past the fade the ground is the fixture's own.
        assertEquals(TestTerrain.surface(144, 144), heights[0][0]);
    }

    @Test
    public void cutOnlyLeavesLowerGroundForTheSupports() {
        int natural = TestTerrain.surface(168, 168);
        GroundFit fit = fit(natural + 5, GroundFit.Mode.CUT);
        ArrayChunkBuffer buffer = TestTerrain.chunk(10, 10);
        ScatteredGround.apply(buffer, CONTEXT, 10, 10, fit);
        assertEquals(natural, surface(buffer, 8, 8));
        GroundFit lower = fit(natural - 5, GroundFit.Mode.CUT);
        assertTrue(ScatteredGround.apply(buffer, CONTEXT, 10, 10, lower) > 0);
        assertEquals(natural - 5, surface(buffer, 8, 8));
    }

    @Test
    public void aChunkOutOfReachIsUntouched() {
        GroundFit fit = fit(200, GroundFit.Mode.LEVEL);
        ArrayChunkBuffer buffer = TestTerrain.chunk(13, 10);
        ArrayChunkBuffer before = TestTerrain.chunk(13, 10);
        assertEquals(0, ScatteredGround.apply(buffer, CONTEXT, 13, 10, fit));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(surface(before, x, z), surface(buffer, x, z));
            }
        }
    }
}
