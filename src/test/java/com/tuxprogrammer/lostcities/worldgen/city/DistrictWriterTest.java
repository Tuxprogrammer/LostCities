package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/** The industrial yard shed is built from pack blocks; casings are only its machine stand-ins (D-casing-standin-1). */
public class DistrictWriterTest {

    private static final int HEIGHT = 64;

    @Test
    public void theShedWallsAndRoofAreNotCasingsAndCasingsStayInTheMachineRow() {
        Map<String, String> map = new HashMap<>();
        map.put("cityPlacement", "grid");
        map.put("networkLayout", "links");
        map.put("densityRings", "1");
        map.put("cityDistricts", "true");
        LayoutPlanner p = CityFixtures.plan(7L, TerrainSettings.fromValues(map), StructureOracle.NONE, LayoutLog.SILENT)
            .planner();
        int yards = 0;
        for (int x = -120; x <= 120 && yards < 4; x++) {
            for (int z = -120; z <= 120 && yards < 4; z++) {
                ChunkPlan plan = p.plan(x, z);
                if (plan.lot() != LotKind.INDUSTRY) {
                    continue;
                }
                yards++;
                ArrayChunkBuffer buffer = new ArrayChunkBuffer();
                new DistrictWriter(new BufferTarget(buffer, null), plan, CityFixtures.ASSETS, 7L).write(HEIGHT);
                int roofs = 0;
                int casings = 0;
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int dy = 1; dy <= 9; dy++) {
                            BlockRef b = buffer.get(bx, HEIGHT + dy, bz);
                            if (b == null || !b.name()
                                .startsWith("gregtech:gt.blockcasings")
                                || b.meta() == 10 && "gregtech:gt.blockcasings".equals(b.name())) {
                                continue;
                            }
                            assertTrue(!"gregtech:gt.blockcasings".equals(b.name()) || b.meta() != 0, "no ULV casing");
                            casings++;
                            assertTrue(dy <= 2, "casing above the machine row at dy " + dy);
                        }
                        if ("minecraft:iron_block".equals(
                            buffer.get(bx, HEIGHT + 5, bz)
                                .name())) {
                            roofs++;
                        }
                    }
                }
                assertEquals(48, roofs, "the roof is iron: 8 x 6 columns");
                assertTrue(casings > 0 && casings <= 9, "the machine row only: " + casings);
            }
        }
        assertTrue(yards > 0, "some yard");
    }
}
