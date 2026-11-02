package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockColors;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;

/** Partial repairs of explosion damage (alpha review AR-9, docs/design/0008-city-edges-and-character.md §4). */
public class RepairsTest {

    private static final long SEED = 4242L;
    private static final Repairs ALWAYS = Repairs.of(CityFixtures.ASSETS, 1f);
    private static final BlockRef STONE_BRICK = BlockRef.of("minecraft:stonebrick", 0);
    private static final BlockRef GLASS = BlockRef.of("minecraft:glass", 0);
    private static final Explosion BLAST = new Explosion(8, 80, 8, 8);
    private static final DamageArea AREA = DamageArea.withExplosions(0, 0, Collections.singletonList(BLAST));

    private static ChunkPlan building() {
        return ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .building(3, 0)
            .build();
    }

    @Test
    public void theRepairChanceIsHonored() {
        Repairs half = Repairs.of(CityFixtures.ASSETS, 0.5f);
        int repaired = 0;
        for (int i = 0; i < 2000; i++) {
            repaired += half.isRepaired(SEED, new Explosion(i * 37, 70 + i % 20, i * 11, 5 + i % 7)) ? 1 : 0;
        }
        assertTrue(repaired > 850 && repaired < 1150, "repaired " + repaired + " of 2000");
        assertTrue(
            !Repairs.of(CityFixtures.ASSETS, 0f)
                .active());
    }

    @Test
    public void aPatchIsSimilarButNotIdentical() {
        BlockRef patch = ALWAYS.patch(SEED, building(), AREA, new NameBlockClassifier(), STONE_BRICK, 8, 73, 8);
        assertNotNull(patch);
        assertNotEquals(STONE_BRICK, patch);
        BlockColors colors = CityFixtures.ASSETS.colors();
        float difference = Repairs.difference(colors.get(STONE_BRICK), colors.get(patch));
        assertTrue(
            difference >= Repairs.MIN_DIFFERENCE && difference <= Repairs.MAX_DIFFERENCE,
            patch + " differs by " + difference);
        assertEquals(
            patch,
            ALWAYS.patch(SEED, building(), AREA, new NameBlockClassifier(), STONE_BRICK, 3, 74, 9),
            "one repair patches one block with one material");
    }

    @Test
    public void windowsAreBoarded() {
        BlockRef patch = ALWAYS.patch(SEED, building(), AREA, new NameBlockClassifier(), GLASS, 8, 73, 8);
        assertNotNull(patch);
        assertEquals("minecraft:planks", patch.name());
    }

    @Test
    public void onlyBelowTheRepairLineAndOnlyInTheCity() {
        int line = Repairs.repairLine(SEED, BLAST, 8, 8);
        int bottom = BLAST.y() - BLAST.radius();
        assertTrue(line >= bottom + 6 && line <= bottom + 15, "line " + line);
        assertNull(ALWAYS.patch(SEED, building(), AREA, new NameBlockClassifier(), STONE_BRICK, 8, line + 1, 8));
        ChunkPlan outside = ChunkPlan.builder(0, 0, ChunkKind.NONE)
            .groundY(71)
            .build();
        assertNull(ALWAYS.patch(SEED, outside, AREA, new NameBlockClassifier(), STONE_BRICK, 8, 73, 8));
    }

    @Test
    public void repairsChangeNoDamageDraw() {
        ArrayChunkBuffer plain = solid();
        ArrayChunkBuffer repaired = solid();
        ExplosionDamage damage = new ExplosionDamage(TestTerrain.context(SEED));
        int a = damage.breakBlocks(plain, AREA, building(), damage.damageRandom(0, 0));
        int b = damage.breakBlocks(repaired, AREA, building(), damage.damageRandom(0, 0), ALWAYS);
        assertEquals(a, b, "the same blocks change");
        int patched = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 72; y < 90; y++) {
                    if (plain.get(x, y, z)
                        .isAir()
                        && !repaired.get(x, y, z)
                            .isAir()) {
                        patched++;
                        assertTrue(y <= Repairs.repairLine(SEED, BLAST, x, z), "patched above the line at " + y);
                    }
                }
            }
        }
        assertTrue(patched > 50, "patched " + patched);
    }

    private static ArrayChunkBuffer solid() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= 95; y++) {
                    buffer.set(x, y, z, y < 72 ? TestTerrain.STONE : STONE_BRICK);
                }
            }
        }
        return buffer;
    }
}
