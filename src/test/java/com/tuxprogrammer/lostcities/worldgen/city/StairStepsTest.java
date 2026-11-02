package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkLayout;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/**
 * AR-1 (alpha review): a street stair toward the higher street keeps every step. The RWG2 alpha set showed a
 * {@code stairs1} with one pair of slabs missing.
 */
public class StairStepsTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final int SEARCH = 24;

    @Test
    public void everySlabStepOfAStreetStairIsWritten() {
        int[] checked = new int[1];
        StringBuilder missing = new StringBuilder();
        CityFixtures.square(SEARCH, at -> {
            ChunkLayout layout = PLANNER.layout(at[0], at[1]);
            ChunkPlan plan = layout.plan();
            if (layout.stair() == null || plan.hasBuilding() || plan.maxHighwayLevel() >= 0 || layout.slope() != null) {
                return;
            }
            Part part = CityFixtures.ASSETS.part(CityPalettes.id(plan.stairPart()));
            assertNotNull(part, plan.stairPart());
            ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, at[0], at[1]);
            Transform transform = CityChunkWriter.rotation(layout.stair());
            int bottom = plan.cityGroundLevel() + 1;
            for (int px = 0; px < part.xsize(); px++) {
                for (int pz = 0; pz < part.zsize(); pz++) {
                    char[] column = part.column(px, pz);
                    if (column == null) {
                        continue;
                    }
                    for (int y = 0; y < column.length; y++) {
                        if (column[y] != '_' && column[y] != '=') {
                            continue;
                        }
                        int x = transform.rotateX(px, pz);
                        int z = transform.rotateZ(px, pz);
                        BlockRef block = buffer.get(x, bottom + y, z);
                        if (!block.name()
                            .contains("slab")) {
                            missing.append(
                                String.format(
                                    "%n  %s in chunk (%d, %d) at (%d, %d, %d), layer %d: %s",
                                    part.id(),
                                    at[0],
                                    at[1],
                                    x,
                                    bottom + y,
                                    z,
                                    y,
                                    block));
                        }
                    }
                }
            }
            checked[0]++;
        });
        assertTrue(checked[0] > 0, "some street stair near the origin");
        assertTrue(missing.length() == 0, "missing stair steps:" + missing);
    }
}
