package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
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
        assertEquals(1, VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(6, 6, LanePath.Material.PLAIN)));
        assertEquals(GRAVEL, buffer.get(6, 80, 6));
        assertEquals(COBBLE, buffer.get(6, 79, 6));
    }

    @Test
    public void aCitysOwnBlocksAreNeverPaved() {
        ArrayChunkBuffer buffer = column(6, 6, BlockRef.of("minecraft:double_stone_slab", 8), 71);
        assertEquals(0, VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(6, 6, LanePath.Material.PLAIN)));
        assertEquals(BlockRef.of("minecraft:double_stone_slab", 8), buffer.get(6, 71, 6));
    }

    @Test
    public void onlyLaneColumnsChange() {
        ArrayChunkBuffer buffer = column(5, 5, TestTerrain.GRASS, 70);
        buffer.set(6, 70, 5, TestTerrain.GRASS);
        VillageLanes.lay(buffer, context(BlockAvailability.VANILLA), 0, 0, laneAt(5, 5, LanePath.Material.PLAIN));
        assertEquals(TestTerrain.GRASS, buffer.get(6, 70, 5));
    }
}
