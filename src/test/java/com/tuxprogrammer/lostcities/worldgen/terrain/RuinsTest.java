package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;

/** Ruins and the street rubble layer (damage-ruins-rubble.md, "Ruins" and "Rubble layer"). */
public class RuinsTest {

    private static final BlockRef WALL = BlockRef.of("minecraft:stonebrick", 0);
    private static final BlockRef RUBBLE = BlockRef.of("minecraft:gravel", 0);

    private static ChunkPlan building(int ground, int floors, float ruin) {
        int[] footprint = new int[256];
        Arrays.fill(footprint, ground - 1);
        return ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(ground)
            .building(floors, 0)
            .ruinHeight(ruin)
            .style(
                StyleBlocks.builder()
                    .ruinRubble(RUBBLE)
                    .damaged(WALL, BlockRef.of("minecraft:iron_bars", 0))
                    .build())
            .footprint(footprint)
            .build();
    }

    /** A solid building: every block from the ground to its top is wall. */
    private static ArrayChunkBuffer solid(ChunkPlan plan) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = plan.groundY(); y < plan.maxHeight(); y++) {
                    buffer.set(x, y, z, WALL);
                }
            }
        }
        return buffer;
    }

    @Test
    public void aRuinLosesItsTopAndKeepsItsBottom() {
        Ruins ruins = new Ruins(TestTerrain.context(7));
        ChunkPlan plan = building(71, 6, 0.5f);
        ArrayChunkBuffer buffer = solid(plan);
        int changed = ruins.ruin(buffer, 0, 0, plan, ruins.random(0, 0), null);
        assertTrue(changed > 0);
        assertEquals(WALL, buffer.get(8, 75, 8), "the lower floors stand");
        int cut = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!WALL.equals(buffer.get(x, plan.maxHeight() - 1, z))) {
                    cut++;
                }
            }
        }
        assertTrue(cut > 200, "the top is gone almost everywhere: " + cut);
    }

    @Test
    public void doesNotReproduceTERR29_aRuinNearTheTopOfTheWorldStaysInIt() {
        Ruins ruins = new Ruins(TestTerrain.context(7));
        // Ground 230 with 4 floors: the top is 254, and upstream's column ran to 263 without a real cap.
        ChunkPlan plan = building(230, 4, 0.0f);
        ArrayChunkBuffer buffer = solid(plan);
        ruins.ruin(buffer, 0, 0, plan, ruins.random(0, 0), null);
        // The buffer would have thrown on Y 256; reaching here is the check, and the cut went to the top.
        assertEquals(BlockRef.AIR, buffer.get(8, 253, 8));
    }

    @Test
    public void doesNotReproduceTERR30_fallingLeavesStopAboveTheBottomOfTheWorld() {
        Ruins ruins = new Ruins(TestTerrain.context(7));
        ChunkPlan plan = building(10, 1, 0.0f);
        // An empty column below the cut: upstream's leaves fell forever and threw.
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        ruins.ruin(buffer, 0, 0, plan, ruins.random(0, 0), null);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(BlockRef.AIR, buffer.get(x, 0, z), "nothing below Y 1");
            }
        }
    }

    @Test
    public void doesNotReproduceTERR37_theRubbleHeightHoldsThisChunksGroundAtItsCenter() {
        ChunkPlan[] around = new ChunkPlan[9];
        for (int i = 0; i < 9; i++) {
            around[i] = ChunkPlan.builder(0, 0, ChunkKind.CITY)
                .groundY(i == 4 ? 71 : 83)
                .build();
        }
        // At the center the chunk's own ground; upstream's reversed weights gave the diagonal neighbor's.
        assertEquals(71, Ruins.interpolatedHeight(around, 8, 8));
        assertTrue(Ruins.interpolatedHeight(around, 0, 0) > 71, "toward a corner it leans to the neighbors");
        // Flat when every ground matches.
        Arrays.fill(around, around[4]);
        for (int x = 0; x < 16; x++) {
            assertEquals(71, Ruins.interpolatedHeight(around, x, 15 - x));
        }
    }

    @Test
    public void doesNotReproduceTERR27_theNoisesFollowTheWorldSeed() {
        double[] a = new double[256];
        double[] b = new double[256];
        new OctaveNoise(1, 5, 4).region(a, 0, 0, Ruins.RUIN_SCALE, 1.0);
        new OctaveNoise(2, 5, 4).region(b, 0, 0, Ruins.RUIN_SCALE, 1.0);
        assertTrue(!Arrays.equals(a, b), "two world seeds, two ruin shapes");
        double[] again = new double[256];
        new OctaveNoise(1, 5, 4).region(again, 0, 0, Ruins.RUIN_SCALE, 1.0);
        assertTrue(Arrays.equals(a, again), "the same seed, the same shape");
    }

    @Test
    public void theRubbleLayerLeavesADoorsApproachClear() {
        Ruins ruins = new Ruins(TestTerrain.context(7));
        boolean[] clear = new boolean[256];
        Arrays.fill(clear, true);
        ChunkPlan street = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .keepClear(clear)
            .build();
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                buffer.set(x, 70, z, BlockRef.of("minecraft:stone", 0));
            }
        }
        ChunkPlan[] around = new ChunkPlan[9];
        Arrays.fill(around, street);
        assertEquals(0, ruins.rubble(buffer, 0, 0, street, around, ruins.random(0, 0), null));
    }

    /** AR-18: a rubble pile covers a park's one-block plants instead of floating on top of them. */
    @Test
    public void rubbleCoversSmallPlantsButNotShrubsOrSaplings() {
        assertTrue(Ruins.isSmallPlant(BlockRef.of("minecraft:tallgrass", 1)));
        assertTrue(Ruins.isSmallPlant(BlockRef.of("minecraft:tallgrass", 2)));
        assertTrue(Ruins.isSmallPlant(BlockRef.of("minecraft:yellow_flower", 0)));
        assertTrue(Ruins.isSmallPlant(BlockRef.of("minecraft:red_flower", 8)));
        assertFalse(Ruins.isSmallPlant(BlockRef.of("minecraft:leaves", 4)));
        assertFalse(Ruins.isSmallPlant(BlockRef.of("minecraft:sapling", 8)));
        assertFalse(Ruins.isSmallPlant(BlockRef.of("minecraft:double_plant", 2)));
    }
}
