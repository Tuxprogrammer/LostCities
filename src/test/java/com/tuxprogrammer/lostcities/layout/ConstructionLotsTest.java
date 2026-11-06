package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.DensityRings.BuildingClass;
import com.tuxprogrammer.lostcities.layout.DensityRings.Ring;

/** Construction lots at real sizes (phase 3, D-p3-1 to D-p3-3): the cell draw in isolation. */
public class ConstructionLotsTest {

    private static final long SEED = 4242L;
    private static final float[] RINGS = { 0.1f, 0.3f, 0.6f, 0.9f };

    private static int sizeIndex(ConstructionLots.Draw d) {
        int area = d.dimX * d.dimZ;
        return area == 1 ? 0 : area == 2 ? 1 : 2;
    }

    @Test
    public void lotSizesFollowTheRingClassDistribution() {
        for (float d : RINGS) {
            int[][] counts = new int[BuildingClass.values().length][3];
            int[] perClass = new int[BuildingClass.values().length];
            for (int cx = 0; cx < 400; cx++) {
                for (int cz = 0; cz < 400; cz++) {
                    ConstructionLots.Draw lot = ConstructionLots.draw(SEED, cx, cz, d, 1f, 8);
                    if (lot == null) {
                        continue;
                    }
                    counts[lot.cls.ordinal()][sizeIndex(lot)]++;
                    perClass[lot.cls.ordinal()]++;
                    assertTrue(
                        DensityRings.acceptance(lot.cls, Ring.of(d), 1f) > 0f,
                        lot.cls + " never stands in ring " + Ring.of(d));
                }
            }
            for (BuildingClass cls : BuildingClass.values()) {
                int n = perClass[cls.ordinal()];
                if (n < 400) {
                    continue;
                }
                for (int size = 0; size < 3; size++) {
                    double share = counts[cls.ordinal()][size] / (double) n;
                    assertEquals(
                        ConstructionLots.sizeWeight(cls, size),
                        share,
                        0.05,
                        cls + " size " + size + " at d " + d + " (" + n + " lots)");
                }
            }
        }
    }

    @Test
    public void depthAndFrameStayWithinTheClassBounds() {
        for (float d : RINGS) {
            for (int cx = -150; cx < 150; cx++) {
                for (int cz = -150; cz < 150; cz++) {
                    ConstructionLots.Draw lot = ConstructionLots.draw(SEED, cx, cz, d, 1.5f, 8);
                    if (lot == null) {
                        continue;
                    }
                    int[] cellars = ConstructionLots.cellarBounds(lot.cls);
                    int[] floors = ConstructionLots.floorBounds(lot.cls, 8);
                    assertTrue(lot.cellars >= cellars[0] && lot.cellars <= cellars[1], "cellars " + lot.cellars);
                    assertTrue(
                        lot.classFloors >= floors[0] && lot.classFloors <= floors[1],
                        "floors " + lot.classFloors);
                    assertTrue(lot.frameFloors >= 1 && lot.frameFloors <= lot.classFloors, "frame " + lot.frameFloors);
                    // The footprint stays inside its cell, so two lots never overlap.
                    assertEquals(ConstructionLots.cell(lot.x), cx);
                    assertEquals(ConstructionLots.cell(lot.x + lot.dimX - 1), cx);
                    assertEquals(ConstructionLots.cell(lot.z), cz);
                    assertEquals(ConstructionLots.cell(lot.z + lot.dimZ - 1), cz);
                }
            }
        }
    }

    @Test
    public void cellsCoverAsMuchOfARingAsTheSingleChunkLotsDid() {
        for (float d : RINGS) {
            float area = ConstructionLots.meanArea(Ring.of(d), 1f);
            float covered = ConstructionLots.cellChance(d, 1f) * area / (ConstructionLots.CELL * ConstructionLots.CELL);
            assertEquals(DensityRings.lotChance(d, 1f), covered, 1e-5f);
        }
        assertEquals(null, ConstructionLots.draw(SEED, 0, 0, 0.3f, 0f, 8), "no lots with the rings off");
    }
}
