package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/** Village lanes with VillageNames' per-column path rule (docs/new-spec/03-terrain/pipeline-on-rwg.md, P03-6). */
public class VillageLanesTest {

    private static final BlockRef GRAVEL = BlockRef.of("minecraft:gravel", 0);
    private static final BlockRef COBBLE = BlockRef.of("minecraft:cobblestone", 0);
    private static final BlockRef PLANKS = BlockRef.of("minecraft:planks", 0);
    private static final BlockRef LOG = BlockRef.of("minecraft:log", 0);
    private static final BlockRef SANDSTONE = BlockRef.of("minecraft:sandstone", 0);

    private static TerrainContext context(BlockAvailability availability) {
        return new TerrainContext(1L, 0, TerrainSettings.defaults(), new NameBlockClassifier(), availability);
    }

    private static LanePath laneAt(int x, int z, LanePath.Material material) {
        boolean[] columns = new boolean[256];
        columns[(z << 4) | x] = true;
        return LanePath.of(columns, material);
    }

    private static ArrayChunkBuffer column(int x, int z, BlockRef top, int topY) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int y = 0; y < topY; y++) {
            buffer.set(x, y, z, TestTerrain.STONE);
        }
        buffer.set(x, topY, z, top);
        return buffer;
    }

    @Test
    public void grassBecomesThePacksPathBlock() {
        ArrayChunkBuffer buffer = column(3, 3, TestTerrain.GRASS, 70);
        buffer.set(3, 71, 3, BlockRef.of("minecraft:tallgrass", 1));
        VillageLanes.lay(buffer, context(BlockAvailability.ALL), 0, 0, laneAt(3, 3, LanePath.Material.PLAIN));
        assertEquals(VillageLanes.GRASS_PATH, buffer.get(3, 70, 3));
        assertEquals(BlockRef.AIR, buffer.get(3, 71, 3), "the plant above is cleared");
    }

    @Test
    public void withoutEtFuturumThePathIsGravel() {
        ArrayChunkBuffer buffer = column(3, 3, TestTerrain.GRASS, 70);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(3, 3, LanePath.Material.PLAIN));
        assertEquals(GRAVEL, buffer.get(3, 70, 3));
    }

    @Test
    public void sandBecomesGravelOverCobblestone() {
        ArrayChunkBuffer buffer = column(5, 5, TestTerrain.SAND, 64);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(5, 5, LanePath.Material.PLAIN));
        assertEquals(GRAVEL, buffer.get(5, 64, 5));
        assertEquals(COBBLE, buffer.get(5, 63, 5));
    }

    @Test
    public void waterBecomesPlanksOnLogPostsAtEvenCoordinates() {
        ArrayChunkBuffer buffer = column(4, 6, TestTerrain.SAND, 55);
        for (int y = 56; y < 63; y++) {
            buffer.set(4, y, 6, TestTerrain.WATER);
        }
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(4, 6, LanePath.Material.PLAIN));
        assertEquals(PLANKS, buffer.get(4, 62, 6), "planks at the water's top");
        assertEquals(LOG, buffer.get(4, 61, 6), "a log post below");
        assertEquals(LOG, buffer.get(4, 56, 6), "down through the water");
        assertEquals(TestTerrain.SAND, buffer.get(4, 55, 6));
    }

    @Test
    public void aDesertVillageSwapsGravelAndCobblestoneForSandstone() {
        ArrayChunkBuffer buffer = column(5, 5, TestTerrain.SAND, 64);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(5, 5, LanePath.Material.SAND));
        assertEquals(SANDSTONE, buffer.get(5, 64, 5));
        assertEquals(SANDSTONE, buffer.get(5, 63, 5));
    }

    @Test
    public void terracottaTheRuleNamesNothingForIsPavedAsSandIs() {
        // M05J: on classic RWG's mesa cores the rule matched nothing and the lane paved no column.
        ArrayChunkBuffer buffer = column(6, 6, BlockRef.of("minecraft:stained_hardened_clay", 1), 80);
        assertEquals(
            1,
            VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(6, 6, LanePath.Material.PLAIN)));
        assertEquals(GRAVEL, buffer.get(6, 80, 6));
        assertEquals(COBBLE, buffer.get(6, 79, 6));
    }

    @Test
    public void aCitysOwnBlocksAreNeverPaved() {
        ArrayChunkBuffer buffer = column(6, 6, BlockRef.of("minecraft:double_stone_slab", 8), 71);
        assertEquals(
            0,
            VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(6, 6, LanePath.Material.PLAIN)));
        assertEquals(BlockRef.of("minecraft:double_stone_slab", 8), buffer.get(6, 71, 6));
    }

    @Test
    public void onlyLaneColumnsChange() {
        ArrayChunkBuffer buffer = column(5, 5, TestTerrain.GRASS, 70);
        buffer.set(6, 70, 5, TestTerrain.GRASS);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(5, 5, LanePath.Material.PLAIN));
        assertEquals(TestTerrain.GRASS, buffer.get(6, 70, 5));
    }

    /** A lane 3 wide along X at Z 7 to 9, over ground whose top is {@code heights[x]}, grass on stone. */
    private static ArrayChunkBuffer laneGround(int[] heights) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                ColumnOps.fill(buffer, x, z, 0, heights[x], TestTerrain.STONE);
                buffer.set(x, heights[x], z, TestTerrain.GRASS);
            }
        }
        return buffer;
    }

    private static LanePath laneAlongX() {
        boolean[] columns = new boolean[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 7; z <= 9; z++) {
                columns[(z << 4) | x] = true;
            }
        }
        return LanePath.of(columns, LanePath.Material.PLAIN);
    }

    @Test
    public void aSingleStepGetsAHalfSlabAndARiseGetsStairsFacingUp() {
        // Flat at 70 to X 4, one step up to 71 at X 5..7, then rising one block a column from X 8 to X 11.
        int[] heights = { 70, 70, 70, 70, 70, 71, 71, 71, 72, 73, 74, 75, 75, 75, 75, 75 };
        ArrayChunkBuffer buffer = laneGround(heights);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAlongX());
        BlockRef slab = BlockRef.of("minecraft:stone_slab", 3);
        BlockRef stairEast = BlockRef.of("minecraft:stone_stairs", 0);
        assertEquals(slab, buffer.get(4, 71, 8), "a single step up: a half slab before it");
        assertEquals(BlockRef.AIR, buffer.get(3, 71, 8), "flat ground gets nothing");
        assertEquals(stairEast, buffer.get(7, 72, 8), "the rise goes on: a stair facing up the slope");
        assertEquals(stairEast, buffer.get(9, 74, 8));
        assertEquals(slab, buffer.get(10, 75, 8), "the last rise onto the landing is a half slab");
        assertEquals(GRAVEL, buffer.get(10, 74, 8), "the path stays under the step");
        assertEquals(BlockRef.AIR, buffer.get(12, 76, 8), "the landing gets nothing");
    }

    @Test
    public void aRiseOfTwoIsLeftAsItIs() {
        int[] heights = new int[16];
        Arrays.fill(heights, 70);
        for (int x = 8; x < 16; x++) {
            heights[x] = 72;
        }
        ArrayChunkBuffer buffer = laneGround(heights);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAlongX());
        assertEquals(BlockRef.AIR, buffer.get(7, 71, 8));
    }

    @Test
    public void aGradedLaneRunsStraightFromTheCityGroundToTheCoresEdge() {
        // Ground at 90 across the margin chunk, the city on the west side at 74, the core's edge (east) at 82.
        int[] heights = new int[16];
        Arrays.fill(heights, 90);
        ArrayChunkBuffer buffer = laneGround(heights);
        int[][] edges = new int[4][16];
        for (int[] row : edges) {
            Arrays.fill(row, SoftEdges.NOT_IN_STRIP);
        }
        Arrays.fill(edges[ChunkPlan.Side.EAST.ordinal()], 82);
        LanePath lane = laneAlongX().gradedFrom(ChunkPlan.Side.WEST, 74);
        TerrainContext context = context(BlockAvailability.VANILLA);
        VillageLanes.grade(buffer, context, lane, edges);
        for (int x = 0; x < 16; x++) {
            int expected = 74 + Math.round((82 - 74) * x / 15f);
            assertEquals(expected, ColumnOps.surface(buffer, context.classifier(), x, 8, true), "lane column " + x);
        }
        assertEquals(90, ColumnOps.surface(buffer, context.classifier(), 4, 3, true), "off the lane nothing moves");
        assertEquals(
            83,
            ColumnOps.surface(buffer, context.classifier(), 4, 6, true),
            "the shoulder beside the lane meets it halfway: (90 + 76) / 2");
        assertEquals(TestTerrain.GRASS, buffer.get(0, 74, 8), "a graded column keeps its top block");
    }
}
