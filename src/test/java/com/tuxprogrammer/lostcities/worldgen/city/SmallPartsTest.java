package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/** The small pieces of the city writer: loot names, templates, the palette sequence, the cache and the profile. */
public class SmallPartsTest {

    @Test
    public void doesNotReproduceLOOT06_theDispenserTableNeverFillsAChestWithArrowsOnly() {
        // Upstream offered minecraft:chests/jungle_temple_dispenser to chests; it has no 1.7.10 chest category and
        // never enters the draw, opt-in or not.
        assertFalse(LootRules.allowed("minecraft:chests/jungle_temple_dispenser", false));
        assertFalse(LootRules.allowed("minecraft:chests/jungle_temple_dispenser", true));
    }

    @Test
    public void mobIdsBecome1710EntityNames() {
        assertEquals("Zombie", LootRules.entityName("minecraft:zombie"));
        assertEquals("Skeleton", LootRules.entityName("minecraft:skeleton"));
        assertEquals("Blaze", LootRules.entityName("minecraft:blaze"));
        assertEquals("CaveSpider", LootRules.entityName("minecraft:cave_spider"));
        assertEquals("PigZombie", LootRules.entityName("minecraft:zombified_piglin"));
        assertEquals("", LootRules.entityName(null));
    }

    @Test
    public void aTemplatesFacingTurnsWithThePart() {
        JsonObject json = JsonFields.parseStrict("{\"Dir\": 2, \"Items\": [], \"other\": 2}")
            .getAsJsonObject();
        TileTemplate template = TileTemplate.of(json);
        // North (2) turned 90° clockwise faces east (5); keys that are not facings stay.
        assertEquals("{\"Dir\":5,\"Items\":[],\"other\":2}", TileTemplates.rotated(template, Transform.ROTATE_90));
        assertEquals("{\"Dir\":2,\"Items\":[],\"other\":2}", TileTemplates.rotated(template, Transform.ROTATE_NONE));
        assertEquals("", TileTemplates.rotated(null, Transform.ROTATE_90));
    }

    @Test
    public void thePaletteSequenceIsUpstreamsLcgAndAPureFunctionOfTheChunk() {
        PaletteSequence a = new PaletteSequence(1, 2, 3);
        PaletteSequence b = new PaletteSequence(1, 2, 3);
        int differences = 0;
        PaletteSequence other = new PaletteSequence(2, 2, 3);
        for (int i = 0; i < 100; i++) {
            int slot = a.next();
            assertTrue(slot >= 0 && slot < 128);
            assertEquals(slot, b.next());
            if (slot != other.next()) {
                differences++;
            }
        }
        assertTrue(differences > 50, "another world seed gives another sequence (TERR-27)");
    }

    @Test
    public void theCacheKeepsItsBoundAndRecomputesTheSameValue() {
        LruCache<Integer, String> cache = new LruCache<>(4);
        for (int i = 0; i < 20; i++) {
            cache.get(i, k -> "v" + k);
        }
        assertEquals(4, cache.size());
        assertEquals("v1", cache.get(1, k -> "v" + k), "an evicted value comes back the same");
        assertEquals(
            1,
            cache.get(19, k -> "x")
                .length() > 0 ? 1 : 0);
        assertEquals("v19", cache.get(19, k -> "x"), "a kept value is not recomputed");
    }

    @Test
    public void theLayoutReadsThePinnedNumbers() {
        TerrainSettings settings = TerrainSettings.defaults();
        LayoutProfile profile = CityProfiles.layoutProfile(settings);
        assertEquals(settings.groundLevel(), profile.groundLevel);
        assertEquals(settings.cityMinHeight(), profile.cityMinHeight);
        assertEquals(settings.cityMaxHeight(), profile.cityMaxHeight);
        assertEquals(settings.ruinChance(), profile.ruinChance, 0f);
        assertEquals(settings.landmarkMargin(), profile.landmarkMargin);
        for (int level = 0; level < 8; level++) {
            assertEquals(settings.cityLevelHeights()[level], profile.cityLevelHeight(level));
        }
        assertEquals(63, profile.waterLevel());
        assertNotEquals(0, profile.villageLinkDistance);
    }
}
