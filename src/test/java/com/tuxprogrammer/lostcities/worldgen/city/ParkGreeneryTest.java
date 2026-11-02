package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;

/** AR-18 (alpha review): a park's grass is filled like bonemeal, for its biome, deterministically. */
public class ParkGreeneryTest {

    private static final BlockRef GRASS_BLOCK = BlockRef.of("minecraft:grass", 0);
    private static final BlockRef STONE_SLAB = BlockRef.of("minecraft:stone_slab", 0);
    private static final int Y = 72;

    private static BiomeDescriptor biome(String... types) {
        return BiomeDescriptor.of(new HashSet<>(Arrays.asList(types)), "test", null);
    }

    private static ArrayChunkBuffer lawn() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                buffer.set(x, Y - 1, z, GRASS_BLOCK);
            }
        }
        return buffer;
    }

    @Test
    public void eachBiomeGetsItsFlora() {
        assertEquals(ParkGreenery.Flora.TEMPERATE, ParkGreenery.floraOf(biome("PLAINS")));
        assertEquals(ParkGreenery.Flora.TEMPERATE, ParkGreenery.floraOf(biome("FOREST")));
        assertEquals(ParkGreenery.Flora.ARID, ParkGreenery.floraOf(biome("SANDY", "HOT", "DRY")));
        assertEquals(ParkGreenery.Flora.ARID, ParkGreenery.floraOf(biome("SAVANNA")));
        assertEquals(ParkGreenery.Flora.ARID, ParkGreenery.floraOf(biome("MESA")));
        assertEquals(ParkGreenery.Flora.COLD, ParkGreenery.floraOf(biome("CONIFEROUS", "FOREST")));
        assertEquals(ParkGreenery.Flora.COLD, ParkGreenery.floraOf(biome("SNOWY")));
        assertEquals(ParkGreenery.Flora.JUNGLE, ParkGreenery.floraOf(biome("JUNGLE", "HOT", "WET")));
        assertEquals(ParkGreenery.Flora.SWAMP, ParkGreenery.floraOf(biome("SWAMP", "WET")));
        assertEquals(ParkGreenery.Flora.TEMPERATE, ParkGreenery.floraOf(BiomeDescriptor.NONE));
    }

    @Test
    public void plantsStandOnlyOnFreeGrassAndOnlyComeFromTheTable() {
        for (ParkGreenery.Flora flora : ParkGreenery.Flora.values()) {
            ArrayChunkBuffer copy = lawn();
            copy.set(3, Y - 1, 3, STONE_SLAB);
            copy.set(5, Y, 5, STONE_SLAB);
            int placed = ParkGreenery.plant(new BufferTarget(copy, null), Y, flora, ParkGreenery.random(4242L, 7, -3));
            List<BlockRef> table = ParkGreenery.table(flora);
            assertEquals(BlockRef.AIR, copy.get(3, Y, 3), flora + ": nothing on the path");
            assertEquals(STONE_SLAB, copy.get(5, Y, 5), flora + ": a part's block stays");
            int counted = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockRef plant = copy.get(x, Y, z);
                    if (plant.isAir() || plant.equals(STONE_SLAB)) {
                        continue;
                    }
                    assertTrue(table.contains(plant), flora + ": " + plant + " is in the table");
                    assertTrue(
                        copy.get(x, Y + 1, z)
                            .isAir(),
                        flora + ": one block high");
                    counted++;
                }
            }
            assertEquals(placed, counted);
            // 254 free grass blocks; the cover is the expected share, within a wide margin.
            assertTrue(Math.abs(placed - 254 * flora.cover()) < 40, flora + " placed " + placed);
        }
    }

    @Test
    public void theSameChunkGetsTheSamePlants() {
        ArrayChunkBuffer first = lawn();
        ArrayChunkBuffer second = lawn();
        ParkGreenery
            .plant(new BufferTarget(first, null), Y, ParkGreenery.Flora.TEMPERATE, ParkGreenery.random(4242L, 2, 9));
        ParkGreenery
            .plant(new BufferTarget(second, null), Y, ParkGreenery.Flora.TEMPERATE, ParkGreenery.random(4242L, 2, 9));
        assertEquals(ChunkBufferDigest.sha256(first), ChunkBufferDigest.sha256(second));
        ArrayChunkBuffer other = lawn();
        ParkGreenery
            .plant(new BufferTarget(other, null), Y, ParkGreenery.Flora.TEMPERATE, ParkGreenery.random(4242L, 3, 9));
        assertNotEquals(ChunkBufferDigest.sha256(first), ChunkBufferDigest.sha256(other), "another chunk differs");
    }

    @Test
    public void aCityParkIsNoLongerBare() {
        LayoutCityPlan plan = CityFixtures.plan();
        LayoutPlanner planner = plan.planner();
        int[] at = CityFixtures.first(24, (x, z) -> {
            ChunkPlan chunk = planner.plan(x, z);
            return chunk.isCity() && !chunk.hasBuilding()
                && chunk.streetType() == StreetType.PARK
                && !planner.isElevatedParkSection(x, z)
                && planner.layout(x, z)
                    .stair() == null
                && planner.zone(x, z)
                    .zone() == com.tuxprogrammer.lostcities.layout.landmark.Zone.NONE;
        });
        assertNotNull(at, "a park near the origin");
        ArrayChunkBuffer buffer = CityFixtures.generate(plan, at[0], at[1]);
        int ground = planner.plan(at[0], at[1])
            .cityGroundLevel();
        int grass = 0;
        int planted = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!ParkGreenery.GRASS_BLOCK.equals(
                    buffer.get(x, ground, z)
                        .name())) {
                    continue;
                }
                grass++;
                if (!buffer.get(x, ground + 1, z)
                    .isAir()) {
                    planted++;
                }
            }
        }
        assertTrue(grass > 100, "a park's grass: " + grass);
        assertTrue(planted * 10 >= grass * 4, "at least 40 % of the grass holds something: " + planted + "/" + grass);
    }

    @Test
    public void thePaletteVarietysFloraReplacesTheTableWithTheSameDraws() {
        com.tuxprogrammer.lostcities.asset.AssetSet set = com.tuxprogrammer.lostcities.asset.TestAssets.loadFiles(
            "lostcities/palettes/flora.json",
            "{\"palette\": [{\"char\": \"\\ue010\", \"blocks\": [{\"random\": 64, \"block\": \"minecraft:red_flower:4\"},"
                + " {\"random\": 1000, \"block\": \"minecraft:red_flower:6\"}]}]}");
        com.tuxprogrammer.lostcities.asset.palette.CompiledPalette palette = com.tuxprogrammer.lostcities.asset.palette.CompiledPalette
            .compile(
                com.tuxprogrammer.lostcities.asset.block.MetalFlags.ALL_ON,
                set.palette(com.tuxprogrammer.lostcities.asset.AssetId.parse("flora")));
        ArrayChunkBuffer vanilla = lawn();
        ArrayChunkBuffer pack = lawn();
        int vanillaPlaced = ParkGreenery
            .plant(new BufferTarget(vanilla, null), Y, ParkGreenery.Flora.TEMPERATE, ParkGreenery.random(4242L, 1, 2));
        int packPlaced = ParkGreenery.plant(
            new BufferTarget(pack, null),
            Y,
            ParkGreenery.Flora.TEMPERATE,
            palette,
            ParkGreenery.random(4242L, 1, 2));
        assertEquals(vanillaPlaced, packPlaced, "the same columns are planted");
        boolean tulips = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockRef plant = pack.get(x, Y, z);
                assertEquals(
                    vanilla.get(x, Y, z)
                        .isAir(),
                    plant.isAir(),
                    "same column " + x + ", " + z);
                if (!plant.isAir()) {
                    assertEquals("minecraft:red_flower", plant.name());
                    tulips = true;
                }
            }
        }
        assertTrue(tulips);
        ArrayChunkBuffer arid = lawn();
        ParkGreenery
            .plant(new BufferTarget(arid, null), Y, ParkGreenery.Flora.ARID, palette, ParkGreenery.random(4242L, 1, 2));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockRef plant = arid.get(x, Y, z);
                assertTrue(
                    plant.isAir() || ParkGreenery.table(ParkGreenery.Flora.ARID)
                        .contains(plant),
                    "a flora the palette leaves undefined keeps the vanilla table");
            }
        }
    }
}
