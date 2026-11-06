package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPass;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomKind;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The furnish pass inside the city writer (design record 0016): over a region of the test world, a furnished chunk
 * differs from the bare one only where the bare one had air, holds at most {@link FurnishPass#TILES_PER_CHUNK} tile
 * entities from the pass, and comes out the same every time.
 */
public class FurnishCityTest {

    private static final LayoutCityPlan BARE = CityFixtures.plan(CityFixtures.SEED, StructureOracle.NONE, LayoutLog.SILENT);
    private static final LayoutCityPlan FURNISHED = CityFixtures.plan(
        CityFixtures.SEED,
        TerrainSettings.fromValues(java.util.Collections.singletonMap("furnish", "true")),
        StructureOracle.NONE,
        LayoutLog.SILENT);
    private static final int R = 6;

    private static boolean tileBlock(BlockRef block) {
        if (FurnishItem.GT_MACHINES.equals(block.name())) {
            return true;
        }
        for (RoomKind kind : RoomKind.values()) {
            if (FurnishPalettes.bundled()
                .palette(kind)
                .isTileBlock(block.name())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void furnitureOnlyFillsAirAndStaysUnderTheChunkCap() {
        int[] totals = new int[2];
        CityFixtures.square(R, c -> {
            ArrayChunkBuffer bare = CityFixtures.generate(BARE, c[0], c[1]);
            ArrayChunkBuffer furnished = CityFixtures.generate(FURNISHED, c[0], c[1]);
            int tiles = 0;
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockRef before = bare.get(x, y, z);
                        BlockRef after = furnished.get(x, y, z);
                        if (before.equals(after)) {
                            continue;
                        }
                        assertTrue(
                            before.isAir(),
                            "chunk " + c[0] + "," + c[1] + " at " + x + "," + y + "," + z + ": " + before + " -> " + after);
                        totals[0]++;
                        if (tileBlock(after)) {
                            tiles++;
                        }
                    }
                }
            }
            assertTrue(tiles <= FurnishPass.TILES_PER_CHUNK, "chunk " + c[0] + "," + c[1] + " has " + tiles + " tiles");
            totals[1] += tiles;
        });
        assertTrue(totals[0] > 0, "the region got no furniture");
    }

    @Test
    public void theSameChunkIsFurnishedTheSameEveryTime() {
        CityFixtures.square(2, c -> {
            ArrayChunkBuffer a = CityFixtures.generate(FURNISHED, c[0], c[1]);
            ArrayChunkBuffer b = CityFixtures.generate(FURNISHED, c[0], c[1]);
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        assertEquals(a.get(x, y, z), b.get(x, y, z));
                    }
                }
            }
        });
    }
}
