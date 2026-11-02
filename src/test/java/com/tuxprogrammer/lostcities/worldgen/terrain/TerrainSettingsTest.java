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
        assertTrue(settings.warnings()
            .isEmpty());
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
        assertEquals(settings.toValues(), TerrainSettings.fromValues(settings.toValues())
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
        assertEquals(0.002f, settings.explosionChance());
        assertEquals(3, settings.warnings()
            .size());
    }

    @Test
    public void bedrockLayerIsReadButOnlyWarns() {
        Map<String, String> values = new HashMap<>();
        values.put("bedrockLayer", "2");
        TerrainSettings settings = TerrainSettings.fromValues(values);
        assertEquals(2, settings.bedrockLayer());
        assertFalse(settings.warnings()
            .isEmpty());
    }
}
