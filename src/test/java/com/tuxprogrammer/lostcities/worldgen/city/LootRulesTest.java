package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Upstream's loot values to this mod's tier bands, the vanilla opt-in and the EnderZoo set (D20). */
public class LootRulesTest {

    private static Map<String, Integer> spread(String value, boolean optIn) {
        Map<String, Integer> out = new HashMap<>();
        Random random = new Random(42);
        for (int i = 0; i < 16000; i++) {
            out.merge(LootRules.category(value, optIn, random), 1, Integer::sum);
        }
        return out;
    }

    @Test
    public void ordinaryLootIsSuppliesTwiceAsOftenAsLv() {
        Map<String, Integer> spread = spread("minecraft:chests/simple_dungeon", false);
        assertEquals(2, spread.size(), spread.toString());
        double ratio = spread.get(LootRules.CITY_SUPPLIES) / (double) spread.get(LootRules.CITY_LV);
        assertTrue(ratio > 1.8 && ratio < 2.2, "supplies to LV: " + ratio);
    }

    @Test
    public void valuableLootIsMvHvEvWithEvRarest() {
        Map<String, Integer> spread = spread("lostcities:chests/lostcitychest", false);
        assertEquals(3, spread.size(), spread.toString());
        assertTrue(spread.get(LootRules.CITY_MV) > spread.get(LootRules.CITY_HV));
        assertTrue(spread.get(LootRules.CITY_HV) > spread.get(LootRules.CITY_EV));
        double ev = spread.get(LootRules.CITY_EV) / 16000.0;
        assertTrue(ev > 0.04 && ev < 0.085, "EV share about 1/16: " + ev);
    }

    @Test
    public void railDungeonChestsDrawTheRailCategory() {
        assertEquals(
            LootRules.RAIL_DUNGEON,
            LootRules.category("lostcities:chests/raildungeonchest", false, new Random(1)));
    }

    @Test
    public void ownCategoriesFromAnOverridePassThrough() {
        for (String category : LootRules.CATEGORIES) {
            assertTrue(LootRules.allowed(category, false), category);
            assertEquals(category, LootRules.category(category, false, new Random(1)));
        }
    }

    @Test
    public void vanillaTablesNeedTheOptIn() {
        assertFalse(LootRules.allowed("minecraft:chests/desert_pyramid", false));
        assertTrue(LootRules.allowed("minecraft:chests/desert_pyramid", true));
        assertEquals("pyramidDesertyChest", LootRules.category("minecraft:chests/desert_pyramid", true, new Random(1)));
        assertEquals(
            "villageBlacksmith",
            LootRules.category("minecraft:chests/village/village_toolsmith", true, new Random(1)));
        assertEquals("dungeonChest", LootRules.category("minecraft:chests/simple_dungeon", true, new Random(1)));
    }

    @Test
    public void tablesWithNoCounterpartNeverEnterTheDraw() {
        for (String table : new String[] { "minecraft:chests/buried_treasure", "minecraft:chests/end_city_treasure",
            "minecraft:chests/woodland_mansion", "minecraft:chests/village/village_butcher" }) {
            assertFalse(LootRules.allowed(table, true), table);
            assertEquals("", LootRules.category(table, true, new Random(1)), table);
        }
        assertFalse(LootRules.allowed(null, true));
        assertEquals("", LootRules.category(null, false, new Random(1)), "a condition that picks nothing");
    }

    @Test
    public void theBandDrawIsTheChestsOwnRandomAndRepeatable() {
        for (long seed = 0; seed < 50; seed++) {
            assertEquals(
                LootRules.category("lostcities:chests/lostcitychest", false, new Random(seed)),
                LootRules.category("lostcities:chests/lostcitychest", false, new Random(seed)));
        }
    }

    @Test
    public void enderZooNamesStayAsRegistered() {
        for (String name : LootRules.ENDERZOO_HARDMOBS) {
            assertTrue(name.startsWith("enderzoo."), name);
            assertEquals(name, LootRules.entityName(name));
        }
        assertEquals("Skeleton", LootRules.FALLBACK_MOB);
        assertEquals("Witch", LootRules.entityName("minecraft:witch"));
    }
}
