package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;

/** Profile validation (docs/new-spec/02-profiles-dimensions/profile-options.md, "Rules for every option"). */
public class ProfileValidatorTest {

    private static ProfileValidator.Result validate(String... pairs) {
        Map<String, String> given = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            given.put(pairs[i], pairs[i + 1]);
        }
        return ProfileValidator.validate(given);
    }

    @Test
    public void doesNotReproducePROF04_declaredRangesAreEnforcedOnLoad() {
        ProfileValidator.Result result = validate("buildingChance", "1.5", "buildingMaxFloors", "40");
        assertTrue(result.ok(), result.errors()::toString);
        assertEquals(
            "1.0",
            result.values()
                .get("buildingChance"));
        assertEquals(
            "22",
            result.values()
                .get("buildingMaxFloors"),
            "0 to 22 on 1.7.10");
        assertEquals(
            2,
            result.warnings()
                .size(),
            result.warnings()::toString);
    }

    @Test
    public void buildingMinFloorsIsClampedToTheSameRangeAsTheMaximum() {
        ProfileValidator.Result result = validate("buildingMinFloors", "40", "buildingMaxFloors", "40");
        assertTrue(result.ok(), result.errors()::toString);
        assertEquals(
            "22",
            result.values()
                .get("buildingMinFloors"),
            "0 to 22 on 1.7.10, since upstream lets the minimum win over the maximum (P03-14)");
    }

    @Test
    public void doesNotReproducePROF06_anInvertedPairRejectsTheProfile() {
        ProfileValidator.Result result = validate("explosionMinRadius", "40", "explosionMaxRadius", "20");
        assertFalse(result.ok());
        assertTrue(
            result.errors()
                .get(0)
                .contains("explosionMaxRadius"),
            result.errors()::toString);
        assertFalse(validate("cityMinRadius", "200", "cityMaxRadius", "100").ok());
        assertTrue(
            validate("cityChance", "-1", "cityMinRadius", "200", "cityMaxRadius", "100").ok(),
            "the radius pair is unused in Perlin mode");
        assertFalse(validate("buildingMinCellars", "5", "buildingMaxCellars", "2").ok());
        assertTrue(validate("explosionMinRadius", "20", "explosionMaxRadius", "20").ok(), "an equal pair draws once");
    }

    @Test
    public void doesNotReproducePROF12_anEmptyAlternativeStyleIsRejectedWhenTheThresholdIsOn() {
        assertFalse(validate("cityStyleThreshold", "0.4").ok());
        assertTrue(validate("cityStyleThreshold", "0.4", "cityStyleAlternative", "citystyle_border").ok());
    }

    @Test
    public void doesNotReproducePROF15_onlyTheDefaultLandscapeLoads() {
        assertTrue(validate("landscapeType", "DEFAULT").ok());
        ProfileValidator.Result spheres = validate("landscapeType", "spheres");
        assertFalse(spheres.ok());
        assertTrue(
            spheres.errors()
                .get(0)
                .contains("only 'default'"));
    }

    @Test
    public void doesNotReproducePROF16_theStyleThresholdDefaultIsInsideItsRange() {
        ProfileOption threshold = ProfileOptions.get("cityStyleThreshold");
        double value = Double.parseDouble(threshold.defaultValue());
        assertTrue(value >= threshold.min() && value <= threshold.max());
        ProfileValidator.Result negative = validate("cityStyleThreshold", "-0.5");
        assertTrue(negative.ok());
        assertEquals(
            "-1.0",
            negative.values()
                .get("cityStyleThreshold"),
            "any negative value means off");
    }

    @Test
    public void doesNotReproduceCORE13_aDuplicateForcedSpawnEntryIsDroppedNotFatal() {
        ProfileValidator.Result result = validate("forceSpawnBuildings", "[\"building1\",\"building1\",\"building2\"]");
        assertTrue(result.ok());
        assertEquals(
            "[\"building1\",\"building2\"]",
            result.values()
                .get("forceSpawnBuildings"));
        assertEquals(
            1,
            result.warnings()
                .size());
    }

    @Test
    public void wrongTypesAndUnknownValuesAreErrors() {
        assertFalse(validate("generateLoot", "maybe").ok());
        assertFalse(validate("groundLevel", "high").ok());
        assertFalse(validate("multiBuildingStreetConflict", "SOMETIMES").ok());
        assertEquals(
            "BLOCK_ALL",
            validate("multiBuildingStreetConflict", "block_all").values()
                .get("multiBuildingStreetConflict"));
    }

    @Test
    public void droppedAndUnknownKeysAreIgnored() {
        ProfileValidator.Result result = validate("horizon", "128", "highwayDistanceMask", "0", "noSuchOption", "1");
        assertTrue(result.ok());
        assertEquals(Arrays.asList("highwayDistanceMask", "horizon"), result.ignored());
        assertFalse(
            result.values()
                .containsKey("horizon"));
        assertEquals(
            1,
            result.warnings()
                .size());
    }

    @Test
    public void heightLimitsOf1710() {
        assertFalse(validate("groundLevel", "250").ok(), "no room for highways above ground 244 (P06-6)");
        ProfileValidator.Result high = validate("groundLevel", "200", "highwayNetworkLevel", "30");
        assertTrue(high.ok());
        assertEquals(
            "6",
            high.values()
                .get("highwayNetworkLevel"),
            "(255 - 16 - 200) / 6");
        ProfileValidator.Result rails = validate("groundLevel", "20", "railwayLevelOffset", "-8");
        assertTrue(rails.ok());
        int offset = Integer.parseInt(
            rails.values()
                .get("railwayLevelOffset"));
        assertTrue(20 + 6 * (offset - 3) >= ProfileValidator.MIN_RAIL_Y, "rails stay above RWG's bedrock");
        assertFalse(validate("railwaySpacingNorthSouth", "11").ok(), "an odd spacing is rejected");
    }

    @Test
    public void landmarkTreatmentsParse() {
        Map<LandmarkKind, LandmarkTreatment> treatments = ProfileValidator
            .parseLandmarkTreatments("AVOID, ocean_monument=IGNORE");
        assertEquals(LandmarkTreatment.IGNORE, treatments.get(LandmarkKind.OCEAN_MONUMENT));
        assertEquals(LandmarkTreatment.AVOID, treatments.get(LandmarkKind.ROGUELIKE));
        assertFalse(treatments.containsKey(LandmarkKind.VILLAGE), "villageTreatment owns villages");
        assertThrows(IllegalArgumentException.class, () -> ProfileValidator.parseLandmarkTreatments("village=AVOID"));
        assertFalse(validate("landmarkTreatment", "castle=AVOID").ok());
    }
}
