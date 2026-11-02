package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/** Explosions and their damage, against docs/original-spec/03-terrain/damage-ruins-rubble.md. */
public class DamageAreaTest {

    /** Every chunk a city chunk at level 1. */
    private static final CityPlan ALL_CITY = (cx, cz) -> ChunkPlan.builder(cx, cz, ChunkKind.CITY)
        .cityLevel(1)
        .groundY(77)
        .build();

    private static TerrainSettings settings(String... pairs) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return TerrainSettings.fromValues(values);
    }

    @Test
    public void searchRangeIsTwoChunksForDefaults() {
        // AR-8: the default big radius is below 24, so (23 + 15) / 16 = 2; upstream's 35 gave 3.
        assertEquals(2, DamageArea.searchRange(TerrainSettings.defaults()));
        assertEquals(3, DamageArea.searchRange(settings("explosionMaxRadius", "35")));
    }

    @Test
    public void damageIsThreeTimesTheRemainingFraction() {
        Explosion explosion = new Explosion(100, 80, 100, 30);
        DamageArea area = DamageArea.withExplosions(6, 6, Collections.singletonList(explosion));
        assertEquals(3.0f * (30 - 0.5f * (float) Math.sqrt(3)) / 30, area.damageAt(100, 80, 100), 1e-4);
        assertEquals(0f, area.damageAt(200, 80, 200), "outside the radius");
        assertTrue(area.damageAt(110, 80, 100) >= 1f, "at least 1 within two thirds of the radius");
    }

    @Test
    public void explosionsArePureFunctionsOfSeedAndCoordinates() {
        TerrainSettings always = settings("explosionChance", "1", "miniExplosionChance", "1");
        DamageArea first = DamageArea.of(42L, 10, -3, always, ALL_CITY);
        DamageArea second = DamageArea.of(42L, 10, -3, always, ALL_CITY);
        assertEquals(
            first.explosions()
                .toString(),
            second.explosions()
                .toString());
        assertTrue(first.hasExplosions());
        Explosion big = DamageArea.bigExplosionAt(42L, ALL_CITY.chunk(10, -3), always);
        assertTrue(big.y() >= 6 + 75 && big.y() < 6 + 90, "center Y = 6 x level + 75..89, got " + big.y());
        assertTrue(
            big.radius() >= TerrainSettings.DEFAULT_EXPLOSION_MIN_RADIUS
                && big.radius() < TerrainSettings.DEFAULT_EXPLOSION_MAX_RADIUS,
            "radius " + big.radius());
    }

    @Test
    public void explosionsStartOnlyInCitiesByDefault() {
        CityPlan noCity = (cx, cz) -> ChunkPlan.none(cx, cz, 0, 71, 70, false);
        TerrainSettings always = settings("explosionChance", "1", "miniExplosionChance", "1");
        assertFalse(
            DamageArea.of(42L, 0, 0, always, noCity)
                .hasExplosions());
    }

    @Test
    public void doesNotReproducePROF06_equalMinAndMaxDoNotThrow() {
        TerrainSettings equal = settings(
            "explosionChance",
            "1",
            "explosionMinRadius",
            "20",
            "explosionMaxRadius",
            "20",
            "explosionMinHeight",
            "80",
            "explosionMaxHeight",
            "80");
        assertDoesNotThrow(() -> DamageArea.bigExplosionAt(1L, ALL_CITY.chunk(0, 0), equal));
        Explosion big = DamageArea.bigExplosionAt(1L, ALL_CITY.chunk(0, 0), equal);
        assertEquals(20, big.radius(), "an equal pair gives the minimum");
        assertEquals(6 + 80, big.y());
        TerrainSettings swapped = settings("miniExplosionMinRadius", "9", "miniExplosionMaxRadius", "4");
        assertEquals(9, swapped.miniExplosionMaxRadius(), "a maximum below the minimum takes the minimum");
        assertEquals(9, DamageArea.between(new Random(3), 9, 9));
    }

    @Test
    public void doesNotReproduceTERR19_sectionsCoverTheWholeWorldRange() {
        Explosion top = new Explosion(8, 250, 8, 12);
        DamageArea area = DamageArea.withExplosions(0, 0, Collections.singletonList(top));
        assertTrue(area.hasExplosions(15), "the top section, Y 240 to 255");
        assertEquals(255, area.highestExplosionY());
        Explosion bottom = new Explosion(8, 3, 8, 6);
        assertEquals(
            0,
            DamageArea.withExplosions(0, 0, Collections.singletonList(bottom))
                .lowestExplosionY());
    }

    /** AR-1: the reach the floating-fragment pass trusts is the sphere's column, widened by one block. */
    @Test
    public void damageReachIsTheSpheresColumnPlusOneBlock() {
        // Chunk (1, 0); an explosion at block (20, 90, 8), radius 6.
        DamageArea area = DamageArea.withExplosions(1, 0, Collections.singletonList(new Explosion(20, 90, 8, 6)));
        assertTrue(area.mayHaveDamaged(4, 90, 8), "the center");
        assertTrue(area.mayHaveDamaged(4, 140, 8), "far above, where the upward carry can reach");
        assertTrue(area.mayHaveDamaged(10, 90, 8), "one block past the radius");
        assertTrue(area.mayHaveDamaged(4, 83, 8), "one block under the sphere's bottom");
        assertFalse(area.mayHaveDamaged(4, 82, 8), "below that");
        assertFalse(area.mayHaveDamaged(11, 90, 8), "two blocks past the radius");
        assertFalse(area.mayHaveDamaged(15, 95, 15), "the far corner");
    }
}
