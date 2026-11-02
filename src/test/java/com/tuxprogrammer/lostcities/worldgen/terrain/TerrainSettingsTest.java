package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** The terrain numbers of docs/new-spec/03-terrain/height-mapping.md. */
public class TerrainSettingsTest {

    @Test
    public void defaultsAreUpstreamsOnRwg() {
        TerrainSettings settings = TerrainSettings.defaults();
        assertEquals(71, settings.groundLevel());
        assertEquals(63, settings.waterLevel(), "seaLevel -1 resolves to RWG's 63");
        assertEquals(71, settings.cityGround(0));
        assertEquals(119, settings.cityGround(8));
        assertEquals(50, settings.cityMinHeight());
        assertEquals(150, settings.cityMaxHeight());
        assertEquals(3, settings.heightSampleSize());
        assertEquals(-4, settings.terrainFixLowerMinOffset());
        assertEquals(-3, settings.terrainFixLowerMaxOffset());
        assertEquals(-1, settings.terrainFixUpperMinOffset());
        assertEquals(1, settings.terrainFixUpperMaxOffset());
        assertEquals(6, settings.softEdgeWidth());
        assertEquals(2, settings.landmarkMargin());
        assertTrue(settings.preventLakesRavinesInCities());
        assertTrue(
            settings.warnings()
                .isEmpty());
    }

    /**
     * AR-8: damage is an accent. The expected crater area per city chunk (chance times the mean squared radius) is at
     * most a fifth of upstream's for both kinds of explosion, and ruins are under half as common.
     */
    @Test
    public void damageDefaultsAreFarBelowUpstreams() {
        TerrainSettings settings = TerrainSettings.defaults();
        double upstreamBig = 0.002 * meanSquare(15, 35);
        double upstreamMini = 0.03 * meanSquare(5, 12);
        double big = settings.explosionChance()
            * meanSquare(settings.explosionMinRadius(), settings.explosionMaxRadius());
        double mini = settings.miniExplosionChance()
            * meanSquare(settings.miniExplosionMinRadius(), settings.miniExplosionMaxRadius());
        assertTrue(big <= upstreamBig / 5, "big crater area " + big + " vs upstream " + upstreamBig);
        assertTrue(mini <= upstreamMini / 5, "mini crater area " + mini + " vs upstream " + upstreamMini);
        assertTrue(settings.ruinChance() <= 0.05f / 2, "ruin chance " + settings.ruinChance());
        assertTrue(settings.explosionChance() > 0 && settings.miniExplosionChance() > 0, "damage stays on");
        assertTrue(settings.ruinChance() > 0, "ruins stay on");
    }

    /** Small chances are written as plain decimals, as docs/config.md shows them. */
    @Test
    public void floatsAreWrittenInPlainDecimal() {
        assertEquals("0.0004", TerrainSettings.text(0.0004f));
        assertEquals("0.002", TerrainSettings.text(0.002f));
        assertEquals("3.0", TerrainSettings.text(3.0f));
        assertEquals(
            "0.0004",
            TerrainSettings.defaults()
                .toValues()
                .get("explosionChance"));
    }

    /** AR-8: the damage numbers stay profile options, so a profile can ask for upstream's look. */
    @Test
    public void upstreamsDamageNumbersStayConfigurable() {
        Map<String, String> values = new HashMap<>();
        values.put("explosionChance", "0.002");
        values.put("explosionMinRadius", "15");
        values.put("explosionMaxRadius", "35");
        values.put("miniExplosionChance", "0.03");
        values.put("miniExplosionMinRadius", "5");
        values.put("miniExplosionMaxRadius", "12");
        values.put("ruinChance", "0.05");
        TerrainSettings settings = TerrainSettings.fromValues(values);
        assertEquals(0.002f, settings.explosionChance());
        assertEquals(15, settings.explosionMinRadius());
        assertEquals(35, settings.explosionMaxRadius());
        assertEquals(0.03f, settings.miniExplosionChance());
        assertEquals(5, settings.miniExplosionMinRadius());
        assertEquals(12, settings.miniExplosionMaxRadius());
        assertEquals(0.05f, settings.ruinChance());
        assertTrue(
            settings.warnings()
                .isEmpty());
    }

    /** Returns the mean of r² for a radius drawn as {@code min + nextInt(max − min)}. */
    private static double meanSquare(int min, int max) {
        double sum = 0;
        for (int r = min; r < max; r++) {
            sum += (double) r * r;
        }
        return sum / (max - min);
    }

    @Test
    public void cityLevelsFollowUpstreamsThresholds() {
        TerrainSettings settings = TerrainSettings.defaults();
        assertEquals(0, settings.levelForHeight(74));
        assertEquals(1, settings.levelForHeight(75));
        assertEquals(1, settings.levelForHeight(82));
        assertEquals(7, settings.levelForHeight(130));
        assertEquals(8, settings.levelForHeight(131));
    }

    @Test
    public void valuesRoundTripThroughASnapshot() {
        TerrainSettings settings = TerrainSettings.defaults();
        assertEquals(
            settings.toValues(),
            TerrainSettings.fromValues(settings.toValues())
                .toValues());
    }

    @Test
    public void outOfRangeValuesAreClampedWithAWarning() {
        Map<String, String> values = new HashMap<>();
        values.put("groundLevel", "400");
        values.put("cityLevel3Height", "300");
        values.put("explosionChance", "banana");
        TerrainSettings settings = TerrainSettings.fromValues(values);
        assertEquals(255, settings.groundLevel());
        assertEquals(TerrainSettings.DEFAULT_EXPLOSION_CHANCE, settings.explosionChance());
        assertEquals(
            3,
            settings.warnings()
                .size());
    }

    @Test
    public void bedrockLayerIsReadButOnlyWarns() {
        Map<String, String> values = new HashMap<>();
        values.put("bedrockLayer", "2");
        TerrainSettings settings = TerrainSettings.fromValues(values);
        assertEquals(2, settings.bedrockLayer());
        assertFalse(
            settings.warnings()
                .isEmpty());
    }
}
