package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/** City chunk terrain, against docs/original-spec/03-terrain/city-chunk-terrain.md. */
public class CityTerrainTest {

    private static final TerrainContext CONTEXT = TestTerrain.context(1L);

    private static ChunkPlan city(int level, int floors, int cellars) {
        ChunkPlan.Builder builder = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .cityLevel(level)
            .groundY(71 + 6 * level)
            .naturalHeight(70);
        if (floors > 0) {
            builder.building(floors, cellars);
        }
        return builder.build();
    }

    private static ChunkPlan[] sides(ChunkPlan plan) {
        ChunkPlan[] sides = new ChunkPlan[4];
        Arrays.fill(sides, plan);
        return sides;
    }

    @Test
    public void levelingPutsEveryColumnTopAtTheCityGround() {
        ArrayChunkBuffer buffer = TestTerrain.chunk(3, 5);
        int[] targets = new int[256];
        Arrays.fill(targets, 77);
        CityTerrain.level(buffer, CONTEXT, targets);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(77, ColumnOps.surface(buffer, CONTEXT.classifier(), x, z, true), "column " + x + "," + z);
            }
        }
    }

    @Test
    public void bordersOnlyTowardLowerOrNonCityNeighbors() {
        ChunkPlan high = city(2, 0, 0);
        ChunkPlan lowCity = city(1, 0, 0);
        ChunkPlan sameCity = city(2, 0, 0);
        ChunkPlan land = ChunkPlan.none(1, 0, 2, 83, 82, false);
        ChunkPlan higherLand = ChunkPlan.none(1, 0, 3, 89, 90, false);
        assertTrue(CityTerrain.doBorder(high, lowCity), "toward a lower street");
        assertFalse(CityTerrain.doBorder(high, sameCity), "not toward the same level");
        assertTrue(CityTerrain.doBorder(high, land), "toward non-city land at the same level");
        assertFalse(CityTerrain.doBorder(high, higherLand), "not toward higher land");
    }

    @Test
    public void noBorderWallTowardALandmarkCore() {
        ChunkPlan street = city(0, 0, 0);
        ChunkPlan core = ChunkPlan.builder(1, 0, ChunkKind.LANDMARK_CORE)
            .groundY(60)
            .naturalHeight(60)
            .build();
        ChunkPlan[] sides = sides(street);
        sides[ChunkPlan.Side.EAST.ordinal()] = core;
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        CityTerrain.streetBaseAndBorders(buffer, CONTEXT, street, sides, true, null);
        assertEquals(BlockRef.AIR, buffer.get(15, 72, 8), "no wall on the side facing the core");
    }

    @Test
    public void doesNotReproduceTERR06_drownedBuildingClearsTheWaterLevelRow() {
        Map<String, String> values = new HashMap<>();
        values.put("seaLevel", "90");
        TerrainContext drowned = new TerrainContext(
            1L,
            0,
            TerrainSettings.fromValues(values),
            new NameBlockClassifier(),
            BlockAvailability.VANILLA);
        ChunkPlan plan = city(0, 5, 0);
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int y = 0; y < 120; y++) {
            buffer.set(5, y, 5, TestTerrain.STONE);
        }
        CityTerrain.makeRoomForBuilding(buffer, drowned, plan, sides(plan), null);
        assertEquals(TestTerrain.WATER, buffer.get(5, 89, 5), "the row below the water level is water");
        assertEquals(BlockRef.AIR, buffer.get(5, 90, 5), "the water level row itself is cleared, not skipped");
    }

    @Test
    public void doesNotReproduceTERR24_clearToMaxStopsAtTheBuildingCap() {
        ChunkPlan plan = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(119)
            .naturalHeight(250)
            .build();
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int y = 200; y <= 255; y++) {
            buffer.set(0, y, 0, TestTerrain.STONE);
        }
        CityTerrain.clearToMax(buffer, plan, 200);
        assertEquals(BlockRef.AIR, buffer.get(0, WorldLimits.BUILDING_TOP_CAP - 1, 0));
        assertEquals(TestTerrain.STONE, buffer.get(0, WorldLimits.BUILDING_TOP_CAP, 0), "Y 248 and up are not cleared");
    }

    @Test
    public void doesNotReproduceTERR31_aBuildingThatCannotFitIsRejectedByThePlan() {
        assertThrows(
            IllegalStateException.class,
            () -> ChunkPlan.builder(0, 0, ChunkKind.CITY)
                .groundY(10)
                .building(2, 3)
                .build(),
            "a bottom below Y 2 never reaches generation");
        ChunkPlan lowest = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(14)
            .building(1, 2)
            .build();
        assertEquals(WorldLimits.BUILDING_BOTTOM_MIN, lowest.buildingBottom());
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        CityTerrain.makeRoomForBuilding(buffer, CONTEXT, lowest, sides(lowest), null);
        assertEquals(BlockRef.of("minecraft:stonebrick", 0), buffer.get(5, 2, 5), "filler at the lowest level");
    }
}
