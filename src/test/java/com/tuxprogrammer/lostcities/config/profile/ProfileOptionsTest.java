package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Category;
import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Status;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/** The option catalog against upstream's (docs/original-spec/02-profiles-dimensions/profile-options.md). */
public class ProfileOptionsTest {

    @Test
    public void upstreamsOptionsAreAllThereInTheirCategories() {
        Map<Category, Integer> counts = new EnumMap<>(Category.class);
        int dropped = 0;
        for (ProfileOption option : ProfileOptions.all()) {
            if (option.upstream()) {
                counts.merge(option.category(), 1, Integer::sum);
                if (option.status() == Status.DROPPED) {
                    dropped++;
                }
            }
        }
        assertEquals(107, (int) counts.get(Category.LOSTCITY));
        assertEquals(12, (int) counts.get(Category.EXPLOSIONS));
        assertEquals(25, (int) counts.get(Category.CITIES));
        assertEquals(14, (int) counts.get(Category.CITYSPHERES));
        assertEquals(5, (int) counts.get(Category.CLIENT));
        assertEquals(17, dropped, "the dropped options of new-spec profile-options.md");
    }

    @Test
    public void theNewOptionsAreInTheLostcityCategory() {
        for (String key : Arrays.asList(
            "preventLakesInCities",
            "highwaysEnabled",
            "villageTreatment",
            "landmarkTreatment",
            "landmarkMargin",
            "softEdgeWidth",
            "villageTransitionRings",
            "villageLinkDistance",
            "heightSampleSize")) {
            ProfileOption option = ProfileOptions.get(key);
            assertNotNull(option, key);
            assertEquals(Category.LOSTCITY, option.category(), key);
        }
        assertEquals(
            "preventLakesInCities",
            ProfileOptions.get("preventLakesRavinesInCities")
                .key(),
            "1.12's name is an alias");
    }

    @Test
    public void everyTerrainValueIsAProfileOptionWithTheSameDefault() {
        for (Map.Entry<String, String> entry : TerrainSettings.defaults()
            .toValues()
            .entrySet()) {
            ProfileOption option = ProfileOptions.get(entry.getKey());
            assertNotNull(option, entry.getKey());
            if (!"bedrockLayer".equals(entry.getKey())) {
                assertEquals(entry.getValue(), option.defaultValue(), entry.getKey());
            }
        }
        assertEquals(
            "0",
            ProfileOptions.get("bedrockLayer")
                .defaultValue(),
            "RWG places bedrock; the default profile asks for none (0005 §6)");
    }

    @Test
    public void theDefaultsGiveUpstreamsLayoutProfile() {
        LayoutProfile fromValues = CityProfiles.layoutProfile(TerrainSettings.defaults(), ProfileValues.defaults());
        LayoutProfile upstream = LayoutProfile.defaults();
        List<String> differences = new ArrayList<>();
        compareFields(LayoutProfile.class, fromValues, upstream, "", differences);
        for (com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind kind : com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind
            .values()) {
            if (fromValues.treatment(kind) != upstream.treatment(kind)) {
                differences.add("treatment of " + kind);
            }
        }
        assertTrue(differences.isEmpty(), "the default profile must not move any city: " + differences);
    }

    /** Compares every instance field, descending into the street settings; the treatment map is compared by kind. */
    static void compareFields(Class<?> type, Object a, Object b, String prefix, List<String> differences) {
        for (java.lang.reflect.Field field : type.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || "landmarkTreatments".equals(field.getName())
                // Deliberate deviations whose defaults are on in a world and off in a bare builder (city shape).
                || java.util.Arrays.asList("cityGrade", "cityGradeRadius", "cityAvoidOcean", "cityConnected")
                    .contains(field.getName())
                || "seaLevel".equals(field.getName()) && a instanceof LayoutProfile
                    && ((LayoutProfile) a).waterLevel() == ((LayoutProfile) b).waterLevel()) {
                // The world resolves -1 to RWG's 63 before the layout sees it; the water level is what counts.
                continue;
            }
            field.setAccessible(true);
            try {
                Object x = field.get(a);
                Object y = field.get(b);
                if (x instanceof com.tuxprogrammer.lostcities.layout.street.StreetPlannerSettings) {
                    compareFields(x.getClass(), x, y, field.getName() + ".", differences);
                } else
                    if (x instanceof int[] ? !Arrays.equals((int[]) x, (int[]) y) : !java.util.Objects.equals(x, y)) {
                        differences.add(prefix + field.getName() + ": " + x + " vs " + y);
                    }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Test
    public void everyDefaultIsInsideItsRangeAndValidates() {
        ProfileValidator.Result result = ProfileValidator.validate(java.util.Collections.<String, String>emptyMap());
        assertTrue(result.ok(), result.errors()::toString);
        assertTrue(
            result.warnings()
                .isEmpty(),
            result.warnings()::toString);
        assertEquals(ProfileOptions.defaults(), result.values());
    }

    @Test
    public void valuesReadMissingKeysAsDefaults() {
        ProfileValues values = ProfileValues.of(java.util.Collections.singletonMap("groundLevel", "80"));
        assertEquals(80, values.getInt("groundLevel"));
        assertEquals(0.3f, values.getFloat("buildingChance"));
        assertEquals(
            0,
            values.getStringList("forceSpawnBuildings")
                .size());
        assertEquals(Arrays.asList("a", "b"), ProfileValues.parseList(ProfileValues.listText(Arrays.asList("a", "b"))));
    }
}
