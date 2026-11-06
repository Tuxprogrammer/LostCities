package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.MultiPos;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Construction lots at real sizes (phase 3, D325 to D327) on the test world: footprints of up to 2 x 2 chunks that
 * every chunk agrees on whatever order the chunks are planned in, depths of whole cellars, frames under their class's
 * floors, pits dug across the inner chunk borders, and the pinned single-chunk lots when the option is off.
 */
public class ConstructionLotFootprintTest {

    private static final long SEED = 1L;
    private static final int R = 48;

    private static LayoutCityPlan plan(String... values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(values[i], values[i + 1]);
        }
        return CityFixtures.plan(SEED, TerrainSettings.fromValues(map), StructureOracle.NONE, LayoutLog.SILENT);
    }

    private static LayoutCityPlan real() {
        return plan("densityRings", "2", "lotFootprints", "true");
    }

    private static String describe(ChunkPlan p) {
        return p.lot() + "/" + p.lotDepth() + "/" + p.lotColumnFloors() + "/" + p.lotFootprint();
    }

    @Test
    public void everyChunkOfAFootprintAgreesFromAnyQueryOrder() {
        LayoutPlanner forward = real().planner();
        LayoutPlanner backward = real().planner();
        String[][] seen = new String[2 * R + 1][2 * R + 1];
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                seen[x + R][z + R] = describe(forward.plan(x, z));
            }
        }
        int multi = 0;
        for (int x = R; x >= -R; x--) {
            for (int z = R; z >= -R; z--) {
                ChunkPlan p = backward.plan(x, z);
                assertEquals(seen[x + R][z + R], describe(p), "chunk " + x + "," + z);
                MultiPos pos = p.lotFootprint();
                if (p.lot() == LotKind.NONE || pos.isSingle()) {
                    continue;
                }
                if (pos.width() * pos.height() > 1) {
                    multi++;
                }
                int ox = x - pos.x();
                int oz = z - pos.z();
                for (int dx = 0; dx < pos.width(); dx++) {
                    for (int dz = 0; dz < pos.height(); dz++) {
                        ChunkPlan other = backward.plan(ox + dx, oz + dz);
                        assertEquals(p.lot(), other.lot(), "footprint at " + ox + "," + oz);
                        assertEquals(p.lotDepth(), other.lotDepth());
                        assertEquals(p.lotColumnFloors(), other.lotColumnFloors());
                        assertEquals(p.cityLevel(), other.cityLevel());
                        assertEquals(new MultiPos(dx, dz, pos.width(), pos.height()), other.lotFootprint());
                    }
                }
            }
        }
        assertTrue(multi > 0, "the test world has construction lots of more than one chunk");
    }

    @Test
    public void depthIsWholeCellarsAndTheFrameStaysUnderTheClassFloors() {
        LayoutPlanner planner = real().planner();
        int dug = 0;
        int deepMulti = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan p = planner.plan(x, z);
                if (p.lot() == LotKind.NONE || p.lotFootprint()
                    .isSingle()) {
                    continue;
                }
                if (p.lot()
                    .isDug()) {
                    dug++;
                    if (p.lotDepth() > 0 && p.lotFootprint()
                        .width()
                        * p.lotFootprint()
                            .height()
                        > 1) {
                        deepMulti++;
                    }
                    // D329: a dug lot gives way to no rail corridor.
                    assertTrue(!p.xRailCorridor() && !p.zRailCorridor(), "corridor through the lot at " + x + "," + z);
                    assertEquals(0, p.lotDepth() % p.floorPitch(), "depth " + p.lotDepth());
                    assertTrue(p.lotDepth() / p.floorPitch() <= 3, "cellars " + p.lotDepth() / p.floorPitch());
                }
                if (p.lot() == LotKind.FOUNDATION) {
                    assertTrue(p.lotColumnFloors() >= 1 && p.lotColumnFloors() <= 8, "frame " + p.lotColumnFloors());
                }
            }
        }
        assertTrue(dug > 0, "the test world has dug construction lots");
        assertTrue(deepMulti > 0, "the test world has a dug construction lot of more than one chunk with cellars");
    }

    /**
     * The shipped ring strengths (the default profile's 1.0 and {@code rarecities}' 1.5) dig: before D329 a rail
     * corridor candidate under any chunk of a footprint left almost every lot bare.
     */
    @Test
    public void theShippedRingStrengthsDigPitsWithCellars() {
        for (String rings : new String[] { "1.0", "1.5" }) {
            LayoutPlanner planner = plan("densityRings", rings, "lotFootprints", "true").planner();
            int pits = 0;
            int ringLots = 0;
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    ChunkPlan p = planner.plan(x, z);
                    if (p.lot() == LotKind.NONE || p.lotFootprint()
                        .isSingle()) {
                        continue;
                    }
                    ringLots++;
                    if (p.lot() == LotKind.PIT && p.lotDepth() >= p.floorPitch()) {
                        pits++;
                    }
                }
            }
            assertTrue(pits * 5 >= ringLots, "rings " + rings + ": " + pits + " pit chunks of " + ringLots);
        }
    }

    @Test
    public void aMultiChunkPitIsDugAcrossItsInnerBorders() {
        LayoutCityPlan source = real();
        LayoutPlanner planner = source.planner();
        int span = 64;
        int checked = 0;
        for (int x = -span; x <= span && checked < 3; x++) {
            for (int z = -span; z <= span && checked < 3; z++) {
                ChunkPlan p = planner.plan(x, z);
                MultiPos pos = p.lotFootprint();
                if (p.lot() != LotKind.PIT || pos.isSingle() || p.lotDepth() == 0) {
                    continue;
                }
                boolean alongX = pos.width() >= 2 && pos.x() == 0;
                boolean alongZ = pos.height() >= 2 && pos.z() == 0;
                if (!alongX && !alongZ) {
                    continue;
                }
                checked++;
                int ground = p.cityGroundLevel();
                ArrayChunkBuffer near = CityFixtures.generate(source, x, z);
                ArrayChunkBuffer far = CityFixtures.generate(source, alongX ? x + 1 : x, alongX ? z : z + 1);
                // The seam's columns inside the pit, in the footprint's other axis.
                int otherOffset = (alongX ? pos.z() : pos.x()) * 16;
                int otherSize = (alongX ? pos.height() : pos.width()) * 16;
                for (int i = 0; i < 16; i++) {
                    int f = otherOffset + i;
                    if (f < EdgeLots.RIM || f >= otherSize - EdgeLots.RIM) {
                        continue;
                    }
                    String where = " of the seam after " + x + "," + z + " at " + i;
                    if (alongX) {
                        assertTrue(
                            near.get(15, ground, i)
                                .isAir(),
                            "near side" + where);
                        assertTrue(
                            far.get(0, ground, i)
                                .isAir(),
                            "far side" + where);
                    } else {
                        assertTrue(
                            near.get(i, ground, 15)
                                .isAir(),
                            "near side" + where);
                        assertTrue(
                            far.get(i, ground, 0)
                                .isAir(),
                            "far side" + where);
                    }
                }
            }
        }
        assertTrue(checked > 0, "the test world has a pit of more than one chunk");
    }

    @Test
    public void theOptionOffKeepsThePinnedSingleChunkLots() {
        LayoutPlanner absent = plan("densityRings", "1").planner();
        LayoutPlanner off = plan("densityRings", "1", "lotFootprints", "false").planner();
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                ChunkPlan a = absent.plan(x, z);
                assertEquals(describe(a), describe(off.plan(x, z)));
                assertEquals(MultiPos.SINGLE, a.lotFootprint());
            }
        }
    }
}
