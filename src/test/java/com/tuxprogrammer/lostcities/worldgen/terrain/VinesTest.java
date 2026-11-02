package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.damage.Explosion;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/** Vines on building walls (docs/new-spec/03-terrain/index.md, P03-9). */
public class VinesTest {

    private static TerrainContext context(float chance) {
        Map<String, String> values = new HashMap<>();
        values.put("vineChance", Float.toString(chance));
        return new TerrainContext(
            1L,
            0,
            TerrainSettings.fromValues(values),
            new NameBlockClassifier(),
            BlockAvailability.VANILLA);
    }

    private static ChunkPlan street() {
        return ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .build();
    }

    private static ChunkPlan tower() {
        return ChunkPlan.builder(1, 0, ChunkKind.CITY)
            .groundY(71)
            .building(6, 0)
            .build();
    }

    private static DamageArea none(int chunkX) {
        return DamageArea.withExplosions(chunkX, 0, Collections.emptyList());
    }

    private static ChunkPlan[] sidesWithTowerEast() {
        ChunkPlan[] sides = new ChunkPlan[4];
        Arrays.fill(sides, street());
        sides[ChunkPlan.Side.EAST.ordinal()] = tower();
        return sides;
    }

    private static DamageArea[] calm() {
        return new DamageArea[] { none(-1), none(1), none(0), none(0) };
    }

    @Test
    public void vinesHangOnTheNeighborsWallFacingIt() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        int placed = Vines.grow(buffer, context(1f), street(), sidesWithTowerEast(), calm(), Vines.random(1L, 0, 0));
        assertTrue(placed > 0);
        assertEquals(BlockRef.of("minecraft:vine", 8), buffer.get(15, 80, 8), "attached to the east wall");
        assertEquals(BlockRef.AIR, buffer.get(14, 80, 8), "only the column against the wall");
        assertEquals(BlockRef.AIR, buffer.get(15, 71 + 36, 8), "not above the wall's top");
    }

    @Test
    public void doesNotReproduceTERR11_theResultIgnoresWhetherNeighborsExist() {
        // There is no neighbor status to consult: two runs from the same plan agree.
        ArrayChunkBuffer first = new ArrayChunkBuffer();
        ArrayChunkBuffer second = new ArrayChunkBuffer();
        Vines.grow(first, context(0.05f), street(), sidesWithTowerEast(), calm(), Vines.random(1L, 0, 0));
        Vines.grow(second, context(0.05f), street(), sidesWithTowerEast(), calm(), Vines.random(1L, 0, 0));
        assertEquals(ChunkBufferDigest.sha256(first), ChunkBufferDigest.sha256(second));
    }

    @Test
    public void doesNotReproduceTERR32_theLastColumnOfAWallTakesVines() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        Vines.grow(buffer, context(1f), street(), sidesWithTowerEast(), calm(), Vines.random(1L, 0, 0));
        assertEquals(BlockRef.of("minecraft:vine", 8), buffer.get(15, 80, 15), "column 15 too");
    }

    @Test
    public void doesNotReproduceTERR26_noVineWhereTheWallIsDamaged() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        DamageArea[] damaged = calm();
        damaged[ChunkPlan.Side.EAST.ordinal()] = DamageArea
            .withExplosions(1, 0, Collections.singletonList(new Explosion(24, 85, 8, 12)));
        Vines.grow(buffer, context(1f), street(), sidesWithTowerEast(), damaged, Vines.random(1L, 0, 0));
        for (int y = 64; y < 107; y++) {
            assertEquals(
                BlockRef.AIR,
                buffer.get(15, y, 8),
                "no vine at or above the explosion's reach, where damage carrying upward may take the wall, at " + y);
        }
    }
}
