package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Reading and writing upstream's profile format (docs/original-spec/02-profiles-dimensions/profile-system.md). */
public class ProfileJsonTest {

    /** A file as upstream 1.20 writes a built-in: read-only markers, every category, dead options. */
    static final String UPSTREAM_FILE = "{\n" + "  \"public\": false,\n"
        + "  \"lostcity\": {\n"
        + "    \"__readonly__\": \"This profile is read only\",\n"
        + "    \"description\": \"Drowned\",\n"
        + "    \"seaLevel\": 89.0,\n"
        + "    \"ruinChance\": 0.1,\n"
        + "    \"generateNether\": false,\n"
        + "    \"streetGenerationMode\": \"HIERARCHICAL_GRID_V1\",\n"
        + "    \"forceSpawnBuildings\": [\"building1\"]\n"
        + "  },\n"
        + "  \"cities\": { \"cityChance\": 0.02, \"cityMaxRadius\": 100 },\n"
        + "  \"explosions\": { \"explosionChance\": 0.0 },\n"
        + "  \"cityspheres\": { \"citySphereChance\": 0.7 },\n"
        + "  \"client\": { \"horizon\": -1.0, \"fogRed\": -1.0 }\n"
        + "}";

    @Test
    public void anUpstreamFileLoads() {
        ProfileJson.Load load = ProfileJson.read("drowned", "drowned.json", UPSTREAM_FILE);
        Profile profile = load.profile();
        assertNotNull(profile, load.errors()::toString);
        assertFalse(profile.isPublic());
        ProfileValues values = profile.values();
        assertEquals(89, values.getInt("seaLevel"));
        assertEquals(0.02, values.getDouble("cityChance"));
        assertEquals(100, values.getInt("cityMaxRadius"));
        assertEquals("Drowned", values.getString("description"));
        assertEquals(
            "building1",
            values.getStringList("forceSpawnBuildings")
                .get(0));
        assertTrue(
            load.ignored()
                .contains("horizon"),
            load.ignored()::toString);
        assertTrue(
            load.warnings()
                .isEmpty(),
            load.warnings()::toString);
    }

    @Test
    public void doesNotReproducePROF02_aMalformedFileRejectsOnlyThatProfile() {
        ProfileJson.Load broken = ProfileJson.read("broken", "broken.json", "{ \"lostcity\": { \"groundLevel\": ");
        assertNull(broken.profile());
        assertFalse(
            broken.errors()
                .isEmpty());
        ProfileJson.Load wrongType = ProfileJson
            .read("typed", "typed.json", "{ \"lostcity\": { \"groundLevel\": \"high\", \"rubbleLayer\": 1 } }");
        assertNull(wrongType.profile());
        assertEquals(
            2,
            wrongType.errors()
                .size(),
            wrongType.errors()::toString);
        assertTrue(
            wrongType.errors()
                .get(0)
                .contains("groundLevel"));
    }

    @Test
    public void doesNotReproducePROF22_theNameIsTheFileNameWithoutItsExtension() {
        assertEquals("my.city", ProfileJson.nameOf("my.city.json"));
        assertEquals("plain", ProfileJson.nameOf("plain.json"));
        assertNull(ProfileJson.nameOf("notes.txt"));
        assertNull(ProfileJson.nameOf(".json"));
    }

    @Test
    public void aKeyInAnotherCategoryIsIgnoredWithAWarning() {
        ProfileJson.Load load = ProfileJson.read("x", "x.json", "{ \"cities\": { \"groundLevel\": 90 } }");
        assertNotNull(load.profile());
        assertEquals(
            71,
            load.profile()
                .values()
                .getInt("groundLevel"));
        assertEquals(
            1,
            load.warnings()
                .size());
    }

    @Test
    public void whatIsWrittenReadsBackTheSame() {
        for (String name : BuiltinProfiles.names()) {
            Profile builtIn = BuiltinProfiles.get(name);
            String json = ProfileJson.write(builtIn, ProfileTexts::english);
            ProfileJson.Load load = ProfileJson.read(name + "_copy", "copy.json", json);
            assertNotNull(load.profile(), name + ": " + load.errors());
            for (ProfileOption option : ProfileOptions.all()) {
                if (option.kept() && !ProfileJson.isText(option.key())) {
                    assertEquals(
                        builtIn.values()
                            .text(option.key()),
                        load.profile()
                            .values()
                            .text(option.key()),
                        name + "." + option.key());
                }
            }
            assertFalse(json.contains("generateNether"), "dropped keys are not written back");
            assertFalse(json.contains("lostcitiesnh.profile."), "texts are written as words");
        }
    }
}
