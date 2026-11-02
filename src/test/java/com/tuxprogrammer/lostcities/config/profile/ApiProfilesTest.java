package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/** {@link ApiProfiles}: profiles other mods add, frozen at server start, rebuilt on every read of the profiles. */
public class ApiProfilesTest {

    private static ApiProfiles oneProfile() {
        ApiProfiles api = new ApiProfiles();
        api.register(setup -> {
            com.tuxprogrammer.lostcities.api.ILostCityProfile profile = setup.createProfile("mymod_city", "rarecities");
            profile.setDescription("Cities of my mod");
            profile.setCityChance(0.2);
            profile.setGroundLevel(80);
        });
        return api;
    }

    @Test
    public void doesNotReproduceCORE03_callbacksRunAndAreFrozenAtServerStart() {
        ApiProfiles api = oneProfile();
        ProfileRegistry registry = new ProfileRegistry();
        List<ProfileRegistry.Message> report = registry.applyApi(api);
        assertTrue(report.isEmpty(), report.toString());
        Profile profile = registry.get("mymod_city");
        assertNotNull(profile, "the callback ran and its profile can be pinned");
        assertEquals(ApiProfiles.SOURCE, profile.source());
        assertEquals(
            "Cities of my mod",
            profile.values()
                .getString("description"));
        assertEquals(
            0.2,
            profile.values()
                .getDouble("cityChance"),
            1e-9);
        assertEquals(
            80,
            profile.values()
                .getInt("groundLevel"));
        assertEquals(
            BuiltinProfiles.get("rarecities")
                .values()
                .getString("worldStyle"),
            profile.values()
                .getString("worldStyle"),
            "the rest comes from the base profile");

        api.freeze();
        assertThrows(IllegalStateException.class, () -> api.register(setup -> {}));
    }

    @Test
    public void doesNotReproduceCORE06_aReloadKeepsTheApisProfiles() throws IOException {
        ApiProfiles api = oneProfile();
        api.freeze();
        ProfileRegistry registry = new ProfileRegistry();
        Path folder = Files.createTempDirectory("lcnh-api-profiles");
        try {
            registry.load(folder);
            registry.applyApi(api);
            assertTrue(
                registry.names()
                    .contains("mymod_city"));
            registry.load(folder);
            registry.applyApi(api);
            assertTrue(
                registry.names()
                    .contains("mymod_city"),
                "a reload rebuilds the profile instead of losing it");
        } finally {
            Files.deleteIfExists(folder);
        }
    }

    @Test
    public void doesNotReproduceCORE38_badValuesAndNamesAreRejectedNotLive() {
        ApiProfiles api = new ApiProfiles();
        api.register(
            setup -> setup.createProfile("too_low", "default")
                .setGroundLevel(500));
        api.register(setup -> setup.createProfile("default", "default"));
        api.register(setup -> setup.createProfile("orphan", "no_such_base"));
        api.register(
            setup -> setup.createProfile("unknown_key", "default")
                .setOption("noSuchOption", "1"));
        ProfileRegistry registry = new ProfileRegistry();
        List<ProfileRegistry.Message> report = registry.applyApi(api);
        assertNull(registry.get("orphan"));
        assertNull(registry.get("unknown_key"));
        assertEquals(BuiltinProfiles.get("default"), registry.get("default"), "a built-in keeps its name");
        assertTrue(report.size() >= 3, "each failure is reported: " + report);
        Profile low = registry.get("too_low");
        if (low != null) {
            assertTrue(
                low.values()
                    .getInt("groundLevel") <= ProfileValidator.MAX_GROUND_LEVEL,
                "a value out of range is clamped");
        }
    }

    @Test
    public void cityLevelHeightsTakeEightLevels() {
        ApiProfiles api = new ApiProfiles();
        api.register(
            setup -> setup.createProfile("levels", "default")
                .setCityLevelHeights(60, 66, 72, 78, 84, 90, 96, 102));
        api.register(
            setup -> setup.createProfile("four_levels", "default")
                .setCityLevelHeights(60, 66, 72, 78));
        ProfileRegistry registry = new ProfileRegistry();
        registry.applyApi(api);
        assertEquals(
            78,
            registry.get("levels")
                .values()
                .getInt("cityLevel3Height"));
        assertNull(registry.get("four_levels"), "upstream's four-level form is dropped");
    }
}
