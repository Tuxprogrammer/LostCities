package com.tuxprogrammer.lostcities.asset.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.city.LootRules;

/** The bundled loot defaults keep the EV ceiling and the band rules (0004 §3, D20); each rule is proven to fire. */
public class LootDefaultsTest {

    private static LootDefaults read(String json) {
        return LootDefaults.read(new StringReader(json));
    }

    private static String category(String band, String entries) {
        return "{\"categories\": {\"lostcitiesnh:test\": {\"band\": \"" + band
            + "\", \"min\": 2, \"max\": 4, \"entries\": ["
            + entries
            + "]}}}";
    }

    private static final String VANILLA = "{\"item\": \"minecraft:coal\", \"tier\": \"STONE\"}";

    @Test
    public void theBundledDefaultsKeepEveryRule() {
        LootDefaults defaults = LootDefaults.bundled();
        assertEquals(new ArrayList<String>(), defaults.problems());
        assertEquals(
            LootRules.CATEGORIES,
            new ArrayList<>(
                defaults.categories()
                    .keySet()),
            "one category per tier band plus rail, in band order");
    }

    @Test
    public void nothingInTheBundledDefaultsIsAboveEvAndHigherBandsAreRarer() {
        LootDefaults defaults = LootDefaults.bundled();
        for (LootDefaults.Category category : defaults.categories()
            .values()) {
            assertTrue(
                category.band()
                    .withinCeiling(),
                category.name());
            for (LootEntry entry : category.entries()) {
                assertTrue(
                    entry.tier()
                        .withinCeiling(),
                    category.name() + " " + entry);
                assertTrue(
                    entry.tier()
                        .compareTo(category.band()) <= 0,
                    category.name() + " " + entry);
            }
        }
    }

    @Test
    public void everyBandHoldsItsOwnTiersComponentsCircuitsAndLootBag() {
        Map<String, LootDefaults.Category> categories = LootDefaults.bundled()
            .categories();
        String[][] bands = { { LootRules.CITY_LV, "LV", "4" }, { LootRules.CITY_MV, "MV", "5" },
            { LootRules.CITY_HV, "HV", "6" }, { LootRules.CITY_EV, "EV", "7" } };
        for (String[] band : bands) {
            List<String> names = new ArrayList<>();
            boolean bag = false;
            for (LootEntry entry : categories.get(band[0])
                .entries()) {
                names.addAll(entry.names());
                bag |= entry.source() == LootEntry.Source.LOOTBAG && entry.meta() == Integer.parseInt(band[2]);
            }
            assertTrue(names.contains("Electric_Motor_" + band[1]), band[0] + " " + names);
            assertTrue(names.contains("circuit" + band[1]), band[0] + " " + names);
            assertTrue(bag, band[0] + " has the " + band[1] + " loot bag");
        }
    }

    @Test
    public void anEntryAboveTheCeilingIsReported() {
        LootDefaults defaults = read(category("EV", VANILLA + ", {\"gt\": \"Electric_Motor_IV\", \"tier\": \"IV\"}"));
        assertTrue(
            String.join("\n", defaults.problems())
                .contains("above the EV ceiling"),
            defaults.problems()
                .toString());
    }

    @Test
    public void anEntryAboveItsBandIsReported() {
        LootDefaults defaults = read(category("LV", VANILLA + ", {\"ore\": \"ingotTitanium\", \"tier\": \"EV\"}"));
        assertTrue(
            String.join("\n", defaults.problems())
                .contains("above its category's band LV"),
            defaults.problems()
                .toString());
    }

    @Test
    public void aForbiddenOrMislabeledLootBagIsReported() {
        LootDefaults magic = read(category("EV", VANILLA + ", {\"lootbag\": 9, \"tier\": \"STONE\"}"));
        assertTrue(
            String.join("\n", magic.problems())
                .contains("not allowed"),
            magic.problems()
                .toString());
        LootDefaults mislabeled = read(category("EV", VANILLA + ", {\"lootbag\": 7, \"tier\": \"LV\"}"));
        assertTrue(
            String.join("\n", mislabeled.problems())
                .contains("is EV, not LV"),
            mislabeled.problems()
                .toString());
    }

    @Test
    public void anEntryWithoutATierOrWithTwoSourcesIsReported() {
        LootDefaults noTier = read(category("EV", VANILLA + ", {\"item\": \"minecraft:apple\"}"));
        assertTrue(
            String.join("\n", noTier.problems())
                .contains("no valid 'tier'"));
        LootDefaults two = read(category("EV", VANILLA + ", {\"item\": \"minecraft:apple\", \"ore\": \"x\"}"));
        assertTrue(
            String.join("\n", two.problems())
                .contains("exactly one of"));
    }

    @Test
    public void aCategoryWithoutAVanillaEntryIsReported() {
        LootDefaults defaults = read(category("LV", "{\"ore\": \"ingotSteel\", \"tier\": \"LV\"}"));
        assertTrue(
            String.join("\n", defaults.problems())
                .contains("no vanilla entry"));
    }

    @Test
    public void lootBagGroupsAboveEvOrOfMagicFamiliesAreNotAllowed() {
        for (int group : new int[] { 8, 41, 42, 43, 44, 9, 10, 16, 22, 29, 34, 52, 57 }) {
            assertEquals(null, LootBagGroups.tierOf(group), "group " + group);
        }
        assertNotNull(LootBagGroups.tierOf(7));
        for (LootTier tier : LootBagGroups.all()
            .values()) {
            assertTrue(tier.withinCeiling(), tier.toString());
        }
    }

    @Test
    public void tiersParseAndOrder() {
        assertEquals(LootTier.LV, LootTier.parse("lv"));
        assertEquals(null, LootTier.parse("nope"));
        assertTrue(LootTier.EV.withinCeiling());
        assertTrue(!LootTier.IV.withinCeiling());
    }
}
