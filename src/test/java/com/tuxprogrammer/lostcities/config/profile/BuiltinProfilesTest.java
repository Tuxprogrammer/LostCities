package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/** The built-in profiles (docs/new-spec/02-profiles-dimensions/index.md, "Built-in profiles"). */
public class BuiltinProfilesTest {

    @Test
    public void theTenProfilesRwgCanCarryShip() {
        assertEquals(
            new TreeSet<>(
                Arrays.asList(
                    "default",
                    "nodamage",
                    "rarecities",
                    "onlycities",
                    "tallbuildings",
                    "safe",
                    "ancient",
                    "wasteland",
                    "atlantis",
                    "largecities")),
            BuiltinProfiles.names());
    }

    @Test
    public void overridesFollowUpstream() {
        assertEquals(
            "0.0",
            BuiltinProfiles.values("nodamage")
                .get("explosionChance"));
        assertEquals(
            "false",
            BuiltinProfiles.values("nodamage")
                .get("rubbleLayer"));
        assertEquals(
            "0.00025",
            BuiltinProfiles.values("rarecities")
                .get("cityChance"));
        assertEquals(
            "256",
            BuiltinProfiles.values("onlycities")
                .get("cityMaxRadius"));
        assertEquals(
            "19",
            BuiltinProfiles.values("tallbuildings")
                .get("buildingMaxFloors"));
        assertEquals(
            "false",
            BuiltinProfiles.values("safe")
                .get("generateSpawners"));
        assertEquals(
            "6",
            BuiltinProfiles.values("ancient")
                .get("randomLeafBlockThickness"));
        assertEquals(
            "true",
            BuiltinProfiles.values("wasteland")
                .get("avoidWater"));
        assertEquals(
            "89",
            BuiltinProfiles.values("atlantis")
                .get("seaLevel"));
        assertEquals(
            "-1.0",
            BuiltinProfiles.values("largecities")
                .get("cityChance"));
        assertEquals(
            "citystyle_border",
            BuiltinProfiles.values("largecities")
                .get("cityStyleAlternative"));
    }

    @Test
    public void tallbuildingsStaysBelowTheBuildLimit() {
        Map<String, String> tall = BuiltinProfiles.values("tallbuildings");
        assertEquals("255", tall.get("explosionMaxHeight"));
        assertEquals("255", tall.get("miniExplosionMaxHeight"));
    }

    @Test
    public void doesNotReproducePROF18_textsAreLangKeysThatPromiseOnlyWhatTheOptionsDo() throws Exception {
        for (String name : BuiltinProfiles.names()) {
            ProfileValues values = BuiltinProfiles.get(name)
                .values();
            String description = values.getString("description");
            assertEquals(BuiltinProfiles.LANG_PREFIX + name + ".description", description);
            String words = ProfileTexts.english(description);
            assertFalse(words.equals(description), name + " has no English text in en_US.lang");
            assertFalse(
                words.toLowerCase(java.util.Locale.ROOT)
                    .contains("ravine"),
                "the lake veto does not cover ravines");
            assertFalse(words.contains("Lost Worlds") || words.contains("Wastify"), name);
        }
        assertFalse(
            ProfileTexts.english("lostcitiesnh.profile.largecities.description")
                .equals(ProfileTexts.english("lostcitiesnh.profile.default.description")),
            "largecities has its own description");
        String extra = BuiltinProfiles.get("nodamage")
            .values()
            .getString("extraDescription");
        assertTrue(
            ProfileTexts.english(extra)
                .startsWith("Ruins and rubble"),
            extra);
    }

    @Test
    public void builtInsDifferFromTheDefaultsOnlyInTheirOverridesAndTexts() {
        Map<String, String> defaults = ProfileOptions.defaults();
        for (String name : BuiltinProfiles.names()) {
            Map<String, String> values = BuiltinProfiles.values(name);
            Map<String, String> overrides = BuiltinProfiles.overrides(name);
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                if (!ProfileJson.isText(key) && !overrides.containsKey(key)) {
                    assertEquals(defaults.get(key), entry.getValue(), name + "." + key);
                }
            }
        }
    }
}
