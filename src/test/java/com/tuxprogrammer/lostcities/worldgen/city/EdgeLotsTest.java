package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;

/**
 * Edge lots in the world (alpha review AR-6, docs/design/0009-city-edges-and-character.md §3): bare clay and dirt
 * ground with nothing growing, pits dug 1, 2 or 4 blocks with their walls and ramp, foundations with concrete columns,
 * written through the terrain phase on the test world.
 */
public class EdgeLotsTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final int R = 40;
    private static final Set<String> GROUND = new HashSet<>(
        Arrays.asList(
            "minecraft:dirt:1",
            "minecraft:hardened_clay:0",
            "minecraft:stained_hardened_clay:12",
            "minecraft:stained_hardened_clay:14",
            "minecraft:gravel:0"));
    private static final Set<String> GROWTH = new HashSet<>(
        Arrays.asList(
            "minecraft:grass",
            "minecraft:tallgrass",
            "minecraft:leaves",
            "minecraft:leaves2",
            "minecraft:log",
            "minecraft:yellow_flower",
            "minecraft:red_flower",
            "minecraft:sapling",
            "minecraft:double_plant"));

    private static List<int[]> lots() {
        LayoutPlanner planner = PLAN.planner();
        List<int[]> out = new ArrayList<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (planner.plan(x, z)
                    .lot() != LotKind.NONE) {
                    out.add(new int[] { x, z });
                }
            }
        }
        return out;
    }

    private static String key(BlockRef block) {
        return block.name() + ":" + block.meta();
    }

    @Test
    public void lotsAreBareGroundWithNothingGrowing() {
        List<int[]> lots = lots();
        assertFalse(lots.isEmpty(), "the test world has edge lots");
        for (int[] c : lots) {
            ChunkPlan plan = PLAN.planner()
                .plan(c[0], c[1]);
            ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, c[0], c[1]);
            int ground = plan.cityGroundLevel();
            int rim = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = ground - 5; y < 256; y++) {
                        assertFalse(
                            GROWTH.contains(
                                buffer.get(x, y, z)
                                    .name()),
                            "nothing grows on lot " + c[0] + "," + c[1] + " at " + x + "," + y + "," + z);
                    }
                    if (x < EdgeLots.RIM || z < EdgeLots.RIM || x > 15 - EdgeLots.RIM || z > 15 - EdgeLots.RIM) {
                        rim += GROUND.contains(key(buffer.get(x, ground, z))) ? 1 : 0;
                    }
                }
            }
            assertTrue(rim > 100, "clay and dirt ground on lot " + c[0] + "," + c[1] + ": " + rim);
        }
    }

    @Test
    public void aDugLotIsOpenDownToItsDepth() {
        int dug = 0;
        for (int[] c : lots()) {
            ChunkPlan plan = PLAN.planner()
                .plan(c[0], c[1]);
            if (!plan.lot()
                .isDug()) {
                continue;
            }
            dug++;
            ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, c[0], c[1]);
            int ground = plan.cityGroundLevel();
            int depth = plan.lotDepth();
            int open = 0;
            for (int x = EdgeLots.RIM; x < 16 - EdgeLots.RIM; x++) {
                for (int z = EdgeLots.RIM; z < 16 - EdgeLots.RIM; z++) {
                    if (buffer.get(x, ground, z)
                        .isAir()
                        && !buffer.get(x, ground - depth, z)
                            .isAir()) {
                        open++;
                    }
                }
            }
            assertTrue(open > 40, "pit of lot " + c[0] + "," + c[1] + " depth " + depth + ": " + open + " open");
            if (plan.lot() == LotKind.FOUNDATION) {
                int top = ground + plan.lotColumnFloors() * com.tuxprogrammer.lostcities.layout.ChunkPlan.FLOOR_HEIGHT
                    - 1;
                int[] grid = depth >= 4 ? EdgeLots.DEEP_COLUMNS : EdgeLots.SHALLOW_COLUMNS;
                BlockRef column = buffer.get(grid[0], top, grid[0]);
                assertFalse(column.isAir(), "a column rises to " + top);
                assertEquals(
                    "minecraft:iron_bars",
                    buffer.get(grid[0], top + 1, grid[0])
                        .name(),
                    "rebar on top");
            }
        }
        assertTrue(dug > 0, "the test world has dug lots");
    }

    @Test
    public void theRampClimbsOneBlockAColumnToTheRim() {
        for (int depth : new int[] { 1, 2, 4 }) {
            for (int side = 0; side < 4; side++) {
                int previous = depth - 1;
                for (int in = 0; in < depth; in++) {
                    int x = side == 0 ? EdgeLots.RIM + in : side == 1 ? 15 - EdgeLots.RIM - in : 7;
                    int z = side == 2 ? EdgeLots.RIM + in : side == 3 ? 15 - EdgeLots.RIM - in : 7;
                    int raised = EdgeLots.ramp(x, z, side, depth);
                    assertEquals(depth - 1 - in, raised);
                    assertTrue(previous - raised <= 1);
                    previous = raised;
                }
            }
        }
    }

    @Test
    public void aLotIsWrittenTheSameEveryTime() {
        int[] c = lots().get(0);
        assertEquals(
            ChunkBufferDigest.sha256(CityFixtures.generate(PLAN, c[0], c[1])),
            ChunkBufferDigest.sha256(CityFixtures.generate(PLAN, c[0], c[1])));
    }
}
