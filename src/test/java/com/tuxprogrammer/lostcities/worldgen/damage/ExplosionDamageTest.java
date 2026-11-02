package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;

/** Explosion damage and debris, against docs/original-spec/03-terrain/damage-ruins-rubble.md. */
public class ExplosionDamageTest {

    private static final TerrainContext CONTEXT = TestTerrain.context(5L);
    private static final BlockRef STONE = TestTerrain.STONE;
    private static final BlockRef CONCRETE = BlockRef.of("gregtech:gt.blockconcretes", 3);

    private static ChunkPlan plan() {
        return ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .build();
    }

    private static ArrayChunkBuffer solid(int top) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= top; y++) {
                    buffer.set(x, y, z, y < 5 ? TestTerrain.BEDROCK : STONE);
                }
            }
        }
        return buffer;
    }

    @Test
    public void anExplosionBreaksBlocksNearItsCenter() {
        ArrayChunkBuffer buffer = solid(90);
        DamageArea area = DamageArea.withExplosions(0, 0, Collections.singletonList(new Explosion(8, 85, 8, 10)));
        ExplosionDamage damage = new ExplosionDamage(CONTEXT);
        int changed = damage.breakBlocks(buffer, area, plan(), damage.damageRandom(0, 0));
        assertTrue(changed > 100, "a crater, changed " + changed);
        assertEquals(BlockRef.AIR, buffer.get(8, 85, 8), "the center is gone");
        assertEquals(STONE, buffer.get(8, 60, 8), "far below the radius is intact");
    }

    @Test
    public void lightDamageLeavesTheDamagedFormSometimes() {
        StyleBlocks style = StyleBlocks.builder()
            .damaged(STONE, BlockRef.of("minecraft:iron_bars", 0))
            .build();
        ChunkPlan plan = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .style(style)
            .build();
        int bars = 0;
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            BlockRef out = ExplosionDamage.damageBlock(STONE, new NameBlockClassifier(), random, 80, 0.5f, plan, 63);
            if ("minecraft:iron_bars".equals(out.name())) {
                bars++;
            }
        }
        // P(break) 0.5, then 70 % iron bars: about 350 of 1000.
        assertTrue(bars > 280 && bars < 420, "iron bars " + bars);
    }

    @Test
    public void damageSwapsASurvivingSpeedBlock() {
        BlockRef out = ExplosionDamage
            .damageBlock(CONCRETE, new NameBlockClassifier(), new Random(1), 80, 0.0011f, plan(), 63);
        assertEquals(SpeedBlocks.normalFor(CONCRETE), out, "D28 rule 3: a damaged wall is no walking surface");
    }

    @Test
    public void doesNotReproduceTERR18_enclosedAirBelowTheWaterLevelIsNotFlooded() {
        ArrayChunkBuffer buffer = solid(90);
        for (int y = 40; y <= 44; y++) {
            buffer.set(2, y, 2, BlockRef.AIR);
        }
        DamageArea area = DamageArea.withExplosions(0, 0, Collections.singletonList(new Explosion(8, 50, 8, 30)));
        ExplosionDamage damage = new ExplosionDamage(CONTEXT);
        damage.breakBlocks(buffer, area, plan(), damage.damageRandom(0, 0));
        for (int y = 40; y <= 44; y++) {
            assertEquals(BlockRef.AIR, buffer.get(2, y, 2), "air stays air at " + y);
        }
        boolean flooded = false;
        boolean airAbove = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                flooded |= TestTerrain.WATER.equals(buffer.get(x, 50, z));
                airAbove |= buffer.get(x, 70, z)
                    .isAir();
            }
        }
        assertTrue(flooded, "a broken block below the water level becomes water");
        assertTrue(airAbove, "a broken block above it becomes air");
    }

    @Test
    public void doesNotReproduceTERR04_debrisFromATallNeighborLandsOnTheGround() {
        ArrayChunkBuffer buffer = solid(70);
        ChunkPlan tall = ChunkPlan.builder(-1, 0, ChunkKind.CITY)
            .groundY(119)
            .building(22, 0)
            .build();
        assertTrue(tall.maxHeight() + 10 > 255);
        int placed = debrisFrom(buffer, tall);
        assertTrue(placed > 0, "debris placed");
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(TestTerrain.BEDROCK, buffer.get(x, 1, z), "nothing at the bottom of the world");
            }
        }
    }

    @Test
    public void doesNotReproduceTERR05_eachDebrisBlockSearchesFromTheTop() {
        ArrayChunkBuffer buffer = solid(64);
        // A tall block of stone in the half facing the neighbor; a later block must not land inside it.
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 65; y <= 100; y++) {
                    buffer.set(x, y, z, STONE);
                }
            }
        }
        ChunkPlan neighbor = ChunkPlan.builder(-1, 0, ChunkKind.CITY)
            .groundY(71)
            .building(8, 0)
            .build();
        debrisFrom(buffer, neighbor);
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 65; y <= 100; y++) {
                    assertEquals(STONE, buffer.get(x, y, z), "the stone block is intact at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    public void doesNotReproduceTERR27_theDamageRandomDependsOnTheWorldSeed() {
        long one = new ExplosionDamage(TestTerrain.context(1L)).damageRandom(4, 9)
            .nextLong();
        long two = new ExplosionDamage(TestTerrain.context(2L)).damageRandom(4, 9)
            .nextLong();
        assertNotEquals(one, two);
    }

    /** Debris from a damaged neighbor to the west, with a big explosion centered on its corner. */
    private static int debrisFrom(ArrayChunkBuffer buffer, ChunkPlan neighbor) {
        ExplosionDamage damage = new ExplosionDamage(CONTEXT);
        ChunkPlan[] neighbors = new ChunkPlan[8];
        DamageArea[] areas = new DamageArea[8];
        ChunkPlan land = ChunkPlan.none(9, 9, 0, 71, 70, false);
        for (int i = 0; i < 8; i++) {
            neighbors[i] = land;
        }
        neighbors[0] = neighbor;
        areas[0] = DamageArea.withExplosions(-1, 0, Collections.singletonList(new Explosion(-16, 100, 0, 30)));
        return damage.debris(buffer, plan(), neighbors, areas, new Random(11));
    }
}
