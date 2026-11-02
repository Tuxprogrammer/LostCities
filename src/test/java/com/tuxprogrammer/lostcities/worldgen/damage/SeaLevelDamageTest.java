package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockAvailability;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Explosion damage floods to the profile's {@code seaLevel}, not to the world's (docs/new-spec/02-profiles-dimensions/
 * profile-options.md, {@code seaLevel}; avoids PROF-14).
 */
public class SeaLevelDamageTest {

    private static final long SEED = 5L;

    /** Between RWG's sea level (63) and {@code atlantis}' (89). */
    private static final int CENTER_Y = 80;

    private static ArrayChunkBuffer solid(int top) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= top; y++) {
                    buffer.set(x, y, z, y < 5 ? TestTerrain.BEDROCK : TestTerrain.STONE);
                }
            }
        }
        return buffer;
    }

    /** Returns the block an explosion centered at Y 80 leaves at its center under the given settings. */
    private static BlockRef centerAfterExplosion(TerrainSettings settings) {
        TerrainContext context = new TerrainContext(
            SEED,
            0,
            settings,
            new NameBlockClassifier(),
            BlockAvailability.VANILLA);
        ArrayChunkBuffer buffer = solid(100);
        DamageArea area = DamageArea
            .withExplosions(0, 0, Collections.singletonList(new Explosion(8, CENTER_Y, 8, 10)));
        ChunkPlan plan = ChunkPlan.builder(0, 0, ChunkKind.CITY)
            .groundY(71)
            .build();
        ExplosionDamage damage = new ExplosionDamage(context);
        damage.breakBlocks(buffer, area, plan, damage.damageRandom(0, 0));
        return buffer.get(8, CENTER_Y, 8);
    }

    @Test
    public void doesNotReproducePROF14_explosionDamageFloodsToTheProfilesSeaLevel() {
        ProfileSnapshot atlantis = ProfileSnapshot
            .resolve("atlantis", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test");
        TerrainSettings drowned = atlantis.terrainSettings();
        assertEquals(89, drowned.waterLevel(), "atlantis' seaLevel override");
        assertEquals(
            TestTerrain.WATER,
            centerAfterExplosion(drowned),
            "a block broken below the profile's sea level becomes water, not air");
        assertEquals(
            BlockRef.AIR,
            centerAfterExplosion(TerrainSettings.defaults()),
            "with RWG's sea level (63) the same block becomes air");
    }
}
