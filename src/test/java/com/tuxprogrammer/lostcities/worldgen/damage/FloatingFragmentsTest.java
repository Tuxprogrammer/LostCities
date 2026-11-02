package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/** The floating-fragment pass ported from 1.12 (docs/new-spec/03-terrain/index.md, P03-11). */
public class FloatingFragmentsTest {

    private static final BlockRef BRICK = BlockRef.of("minecraft:stonebrick", 0);

    private static ChunkPlan plan() {
        return ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .build();
    }

    private static ChunkPlan[] lowSides() {
        ChunkPlan[] sides = new ChunkPlan[4];
        Arrays.fill(sides, ChunkPlan.none(0, 0, 0, 40, 40, false));
        return sides;
    }

    private static ArrayChunkBuffer groundTo(int top) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= top; y++) {
                    buffer.set(x, y, z, TestTerrain.STONE);
                }
            }
        }
        return buffer;
    }

    @Test
    public void doesNotReproduceTERR28_aFloatingFragmentIsRemovedOrDropped() {
        ArrayChunkBuffer buffer = groundTo(70);
        // A small floating slab well above the ground, inside the damaged band.
        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) {
                buffer.set(x, 85, z, BRICK);
            }
        }
        DamageArea area = DamageArea.withExplosions(0, 0, Collections.singletonList(new Explosion(8, 85, 8, 14)));
        int changed = new FloatingFragments(TestTerrain.context(3L))
            .apply(buffer, area, plan(), lowSides(), new Random(1));
        assertTrue(changed >= 16, "the slab was handled, changed " + changed);
        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) {
                assertEquals(BlockRef.AIR, buffer.get(x, 85, z), "nothing floats at " + x + "," + z);
            }
        }
    }

    @Test
    public void groundConnectedBelowTheBandIsKept() {
        ArrayChunkBuffer buffer = groundTo(90);
        String before = ChunkBufferDigest.sha256(buffer);
        DamageArea area = DamageArea.withExplosions(0, 0, Collections.singletonList(new Explosion(8, 85, 8, 10)));
        int changed = new FloatingFragments(TestTerrain.context(3L))
            .apply(buffer, area, plan(), lowSides(), new Random(1));
        assertEquals(0, changed, "a blob reaching below the band is supported");
        assertEquals(before, ChunkBufferDigest.sha256(buffer));
    }

    @Test
    public void sameInputsGiveTheSameResult() {
        ArrayChunkBuffer first = groundTo(60);
        ArrayChunkBuffer second = groundTo(60);
        for (ArrayChunkBuffer buffer : new ArrayChunkBuffer[] { first, second }) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 90; y < 100; y++) {
                        buffer.set(x, y, z, BRICK);
                    }
                }
            }
        }
        DamageArea area = DamageArea.withExplosions(0, 0, Collections.singletonList(new Explosion(8, 95, 8, 12)));
        new FloatingFragments(TestTerrain.context(3L)).apply(first, area, plan(), lowSides(), new Random(9));
        new FloatingFragments(TestTerrain.context(3L)).apply(second, area, plan(), lowSides(), new Random(9));
        assertEquals(ChunkBufferDigest.sha256(first), ChunkBufferDigest.sha256(second));
    }
}
