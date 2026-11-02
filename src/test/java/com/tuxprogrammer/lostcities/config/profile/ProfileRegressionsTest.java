package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.api.ILostCityProfile;
import com.tuxprogrammer.lostcities.config.LostCitiesConfig;
import com.tuxprogrammer.lostcities.config.ProfilesConfig;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.statics.ProjectFiles;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Regression tests for the upstream bugs of the profile system and of upstream's mod config keys that became profile
 * options (docs/original-spec/BUGS.md; docs/new-spec/01-core/index.md, "Mod config";
 * docs/new-spec/02-profiles-dimensions/index.md).
 */
public class ProfileRegressionsTest {

    /** Upstream's dead options: no reader upstream, read and ignored here (D12). */
    private static final List<String> DEAD = Arrays.asList(
        "horizon",
        "fogRed",
        "fogGreen",
        "fogBlue",
        "fogDensity",
        "sphereSurfaceVariation",
        "outsideSurfaceVariation",
        "outsideGroundLevel");

    @Test
    public void doesNotReproduceCORE08_heightSampleSizeIsDocumentedWithItsRealDefault() throws IOException {
        ProfileOption option = ProfileOptions.get("heightSampleSize");
        assertNotNull(option, "heightSampleSize is a profile option (D73)");
        assertEquals("3", option.defaultValue());
        assertEquals(
            3,
            TerrainSettings.defaults()
                .heightSampleSize());
        String docs = ProjectFiles.read(ProjectFiles.resolve("docs/config.md"));
        assertTrue(docs.contains("| `heightSampleSize` | `3` |"), "docs/config.md states the default the code uses");
    }

    @Test
    public void doesNotReproduceCORE09_aBadTreatmentRejectsTheProfileAtLoadAndEachProfileKeepsItsOwn() {
        // Upstream parsed a JVM-wide set of structure IDs on first use and threw from generation on a bad one.
        ProfileJson.Load misspelled = ProfileJson.read(
            "huts",
            "huts.json",
            "{ \"lostcity\": { \"landmarkTreatment\": \"minecraft:swamp_huts=AVOID\" } }");
        assertNull(misspelled.profile(), "an unknown landmark kind rejects the profile when it loads");
        assertFalse(
            misspelled.errors()
                .isEmpty());
        ProfileJson.Load unknown = ProfileJson
            .read("villages", "villages.json", "{ \"lostcity\": { \"villageTreatment\": \"AVOIDED\" } }");
        assertNull(unknown.profile(), "an unknown treatment rejects the profile when it loads");

        // The treatments are values of each profile, pinned with it, not one set for the JVM's life.
        ProfileJson.Load avoid = ProfileJson
            .read("avoid", "avoid.json", "{ \"lostcity\": { \"landmarkTreatment\": \"AVOID\" } }");
        assertNotNull(avoid.profile(), avoid.errors()::toString);
        Map<LandmarkKind, LandmarkTreatment> avoided = ProfileValidator.parseLandmarkTreatments(
            avoid.profile()
                .values()
                .getString("landmarkTreatment"));
        Map<LandmarkKind, LandmarkTreatment> defaults = ProfileValidator.parseLandmarkTreatments(
            ProfileValues.defaults()
                .getString("landmarkTreatment"));
        assertEquals(LandmarkTreatment.AVOID, avoided.get(LandmarkKind.WITCHERY));
        assertEquals(LandmarkTreatment.BUILD_AROUND, defaults.get(LandmarkKind.WITCHERY));
    }

    @Test
    public void doesNotReproduceCORE10_heightSampleSizeIsPinnedWithTheWorldNotReadFromModConfig() {
        for (Class<?> configClass : LostCitiesConfig.CLASSES) {
            for (Field field : configClass.getDeclaredFields()) {
                String name = field.getName()
                    .toLowerCase(Locale.ROOT);
                assertFalse(
                    name.equals("heightsamplesize") || name.equals("optimizedheightmap"),
                    configClass.getSimpleName() + "." + field.getName() + " would change layout per installation");
            }
        }
        ProfileOption option = ProfileOptions.get("heightSampleSize");
        assertTrue(option.kept());
        ProfileSnapshot snapshot = ProfileSnapshot
            .resolve("default", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test");
        assertEquals(
            "3",
            snapshot.values()
                .get("heightSampleSize"),
            "the snapshot holds the value the world was created with");
        assertEquals(
            3,
            snapshot.terrainSettings()
                .heightSampleSize(),
            "generation reads the pinned value");
    }

    @Test
    public void doesNotReproducePROF07_aSphereLandscapeWithNoOutsideProfileIsRejectedAtLoad() {
        // Upstream resolved an empty outsideProfile to null and threw on the first outside chunk of a sphere world.
        ProfileJson.Load space = ProfileJson.read(
            "sky",
            "sky.json",
            "{ \"lostcity\": { \"landscapeType\": \"space\" }, \"cityspheres\": { \"outsideProfile\": \"\" } }");
        assertNull(space.profile(), "no landscape that reads the outside profile loads (D12)");
        assertFalse(
            space.errors()
                .isEmpty());
        ProfileJson.Load outside = ProfileJson
            .read("outside", "outside.json", "{ \"cityspheres\": { \"outsideProfile\": \"nosuch\" } }");
        assertNotNull(outside.profile(), outside.errors()::toString);
        assertTrue(
            outside.warnings()
                .stream()
                .anyMatch(w -> w.contains("outsideProfile")),
            "a deferred option set away from its default says it has no effect: " + outside.warnings());
        assertNull(new ProfileRegistry().get("space"), "the sphere built-ins are not shipped (D12)");
    }

    @Test
    public void doesNotReproducePROF08_generateNetherIsReadAndIgnoredAndTheNetherGetsNoProfile() {
        ProfileJson.Load load = ProfileJson
            .read("nether", "nether.json", "{ \"lostcity\": { \"generateNether\": true } }");
        assertNotNull(load.profile(), load.errors()::toString);
        assertTrue(
            load.ignored()
                .contains("generateNether"));
        assertFalse(
            load.profile()
                .values()
                .asMap()
                .containsKey("generateNether"),
            "a dropped option is not pinned");
        String[] saved = ProfilesConfig.dimensionProfiles;
        try {
            ProfilesConfig.dimensionProfiles = new String[] { "0=default" };
            assertNull(ProfileSetup.profileFor(-1), "only the dimensions dimensionProfiles names get cities");
        } finally {
            ProfilesConfig.dimensionProfiles = saved;
        }
    }

    @Test
    public void doesNotReproducePROF17_deadOptionsAreReadIgnoredAndNeverWrittenBack() {
        String file = "{ \"client\": { \"horizon\": 1.0, \"fogRed\": 0.5, \"fogGreen\": 0.5, \"fogBlue\": 0.5,"
            + " \"fogDensity\": 0.1 }, \"cityspheres\": { \"sphereSurfaceVariation\": 2.0,"
            + " \"outsideSurfaceVariation\": 2.0, \"outsideGroundLevel\": 80 } }";
        ProfileJson.Load load = ProfileJson.read("dead", "dead.json", file);
        assertNotNull(load.profile(), load.errors()::toString);
        String written = ProfileJson.write(load.profile(), text -> text);
        for (String key : DEAD) {
            assertEquals(
                ProfileOption.Status.DROPPED,
                ProfileOptions.get(key)
                    .status(),
                key);
            assertTrue(
                load.ignored()
                    .contains(key),
                key + " is read and ignored");
            assertFalse(
                load.profile()
                    .values()
                    .asMap()
                    .containsKey(key),
                key + " is not pinned");
            assertFalse(written.contains("\"" + key + "\""), key + " is not written back by saveprofile");
        }
    }

    @Test
    public void doesNotReproducePROF19_cityLevelHeightsTakeEightEvenLevelsOrNone() {
        ApiProfiles api = new ApiProfiles();
        Throwable[] fourLevels = new Throwable[1];
        api.register(setup -> {
            ILostCityProfile profile = setup.createProfile("levels", "default");
            try {
                // Upstream's four-argument form stretched levels 4 to 7 to l3 + 30, + 40, + 50, + 60.
                profile.setCityLevelHeights(75, 83, 91, 99);
            } catch (IllegalArgumentException e) {
                fourLevels[0] = e;
            }
            profile.setCityLevelHeights(80, 88, 96, 104, 112, 120, 128, 136);
        });
        ProfileRegistry registry = new ProfileRegistry();
        assertTrue(
            registry.applyApi(api)
                .isEmpty());
        assertNotNull(fourLevels[0], "four levels are refused, not spread unevenly");
        ProfileValues values = registry.get("levels")
            .values();
        for (int i = 0; i < ApiProfiles.CITY_LEVELS; i++) {
            assertEquals(80 + 8 * i, values.getInt("cityLevel" + i + "Height"), "level " + i);
        }
        assertThrows(
            NoSuchMethodException.class,
            () -> ILostCityProfile.class.getMethod("setCityLevelHeights", int.class, int.class, int.class, int.class));
    }
}
