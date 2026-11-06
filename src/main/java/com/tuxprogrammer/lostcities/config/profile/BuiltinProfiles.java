package com.tuxprogrammer.lostcities.config.profile;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/ProfileSetup.java
// Avoids: PROF-18

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The profiles that ship in the jar: the ten of upstream's seventeen whose landscape RWG can carry
 * (docs/new-spec/02-profiles-dimensions/index.md, "Built-in profiles"; docs/original-spec/02-profiles-dimensions/
 * builtin-profiles.md). Each starts from the defaults and sets upstream's overrides, with these deviations:
 * <ul>
 * <li>{@code tallbuildings}' explosion heights are 255, the 1.7.10 build limit, not 256 (0005 §6);</li>
 * <li>the texts are lang keys whose words promise only what the options do (PROF-18): {@code nodamage} no longer
 * promises disabled ravines, {@code wasteland} drops its advice about mods the pack lacks, {@code atlantis} its Lost
 * Worlds warning, and {@code largecities} gets its own description;</li>
 * <li>{@code rarecities}' {@code highwayRequiresTwoCities} is dropped with legacy highways (D11).</li>
 * </ul>
 * {@code cavern}, {@code floating}, {@code space}, {@code biosphere}, {@code biosphere_caves}, {@code bio_wasteland}
 * and {@code void_outside} wait for PersonalSpace, after beta (D12). Built-ins are never written into the config
 * folder; {@code /lostcities saveprofile} copies one there to edit.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class BuiltinProfiles {

    /** The default profile's name. */
    public static final String DEFAULT = "default";

    /** The lang-key prefix of the built-ins' texts: {@code lostcitiesnh.profile.<name>.<text>}. */
    public static final String LANG_PREFIX = "lostcitiesnh.profile.";

    /** The built-ins with a second line of text ({@code extraDescription}). */
    private static final Set<String> WITH_EXTRA_TEXT = Collections.singleton("nodamage");

    private static final Map<String, Map<String, String>> OVERRIDES = overrides();
    private static final Map<String, Profile> PROFILES = build();

    private BuiltinProfiles() {}

    /** Returns the names of the built-in profiles, sorted. */
    public static Set<String> names() {
        return Collections.unmodifiableSet(new TreeSet<>(PROFILES.keySet()));
    }

    /**
     * Returns a built-in profile.
     *
     * @param name the name
     * @return the profile, or {@code null} for an unknown name
     */
    public static Profile get(String name) {
        return PROFILES.get(name);
    }

    /**
     * Returns every resolved value of a built-in profile.
     *
     * @param name the profile's name
     * @return option key to canonical text
     * @throws IllegalArgumentException for an unknown name
     */
    public static Map<String, String> values(String name) {
        Profile profile = PROFILES.get(name);
        if (profile == null) {
            throw new IllegalArgumentException("unknown profile '" + name + "'; known: " + names());
        }
        return new TreeMap<>(
            profile.values()
                .asMap());
    }

    /**
     * Returns the overrides a built-in sets over the defaults, as given (for tests and docs).
     *
     * @param name the name
     * @return option key to text; empty for {@code default} or an unknown name
     */
    public static Map<String, String> overrides(String name) {
        Map<String, String> values = OVERRIDES.get(name);
        return values == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(values);
    }

    private static Map<String, Profile> build() {
        Map<String, Profile> out = new TreeMap<>();
        for (Map.Entry<String, Map<String, String>> entry : OVERRIDES.entrySet()) {
            String name = entry.getKey();
            Map<String, String> given = new LinkedHashMap<>(entry.getValue());
            given.put("description", LANG_PREFIX + name + ".description");
            if (WITH_EXTRA_TEXT.contains(name)) {
                given.put("extraDescription", LANG_PREFIX + name + ".extra");
            }
            ProfileValidator.Result result = ProfileValidator.validate(given);
            if (!result.ok() || !result.warnings()
                .isEmpty()) {
                throw new IllegalStateException(
                    "built-in profile " + name + " is invalid: " + result.errors() + " " + result.warnings());
            }
            out.put(name, new Profile(name, true, Profile.BUILT_IN, ProfileValues.of(result.values())));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Map<String, String>> overrides() {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        profile(out, DEFAULT);
        profile(
            out,
            "nodamage",
            "explosionChance",
            "0",
            "miniExplosionChance",
            "0",
            "ruinChance",
            "0",
            "rubbleLayer",
            "false");
        profile(
            out,
            "rarecities",
            "cityPlacement",
            "grid",
            "cityChance",
            "0.00025",
            "cityMinRadius",
            "100",
            "cityMaxRadius",
            "256",
            "ruinChance",
            "0",
            "railwaysCanEnd",
            "false",
            "networkLayout",
            "links");
        profile(out, "onlycities", "cityChance", "0.2", "cityMaxRadius", "256");
        profile(
            out,
            "tallbuildings",
            "buildingMinFloors",
            "4",
            "buildingMinFloorsChance",
            "8",
            "buildingMaxFloorsChance",
            "15",
            "buildingMaxFloors",
            "19",
            "debrisToNearbyChunkFactor",
            "175",
            "explosionChance",
            "0.006",
            "explosionMaxHeight",
            "255",
            "explosionMaxRadius",
            "60",
            "explosionMinHeight",
            "130",
            "miniExplosionChance",
            "0.09",
            "miniExplosionMaxHeight",
            "255",
            "miniExplosionMaxRadius",
            "14",
            "miniExplosionMinRadius",
            "3",
            "ruinChance",
            "0.01");
        profile(out, "safe", "generateSpawners", "false", "generateLighting", "true", "generateLoot", "false");
        profile(
            out,
            "ancient",
            "randomLeafBlockThickness",
            "6",
            "randomLeafBlockChance",
            "0.05",
            "vineChance",
            "0.1",
            "explosionChance",
            "0",
            "miniExplosionChance",
            "0",
            "rubbleLayer",
            "true",
            "rubbleDirtScale",
            "2.0",
            "rubbleLeaveScale",
            "2.0",
            "ruinChance",
            "0.9",
            "ruinMinlevelPercent",
            "0.0",
            "ruinMaxlevelPercent",
            "0.9");
        profile(
            out,
            "wasteland",
            "vineChance",
            "0.003",
            "randomLeafBlockChance",
            "0.01",
            "rubbleLayer",
            "true",
            "rubbleDirtScale",
            "2.0",
            "rubbleLeaveScale",
            "0.0",
            "ruinChance",
            "0.5",
            "ruinMinlevelPercent",
            "0.5",
            "ruinMaxlevelPercent",
            "0.9",
            "avoidWater",
            "true",
            "avoidFoliage",
            "true");
        profile(out, "atlantis", "seaLevel", "89", "ruinChance", "0.1");
        profile(
            out,
            "largecities",
            "cityChance",
            "-1",
            "cityPerlinScale",
            "7.0",
            "cityPerlinOffset",
            "0.2",
            "cityPerlinInnerScale",
            "0.1",
            "cityThreshold",
            "0.1",
            "cityStyleThreshold",
            "0.4",
            "cityStyleAlternative",
            "citystyle_border",
            "generateLighting",
            "true",
            "buildingMaxFloors",
            "9",
            "buildingMaxFloorsChance",
            "7",
            "buildingChance",
            "0.4");
        return out;
    }

    private static void profile(Map<String, Map<String, String>> out, String name, String... pairs) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        out.put(name, values);
    }
}
