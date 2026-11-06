package com.tuxprogrammer.lostcities.config.profile;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/LostCityProfile.java
// Avoids: PROF-04, PROF-06, PROF-12, PROF-15, PROF-16, CORE-13

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Status;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;

/**
 * Turns a profile's given values into a complete, checked set of values: every kept option, each in its canonical
 * text, clamped to its range, with upstream's explicit checks and the ones upstream left out
 * (docs/new-spec/02-profiles-dimensions/index.md, "Profile system" and "Profile options").
 *
 * <ul>
 * <li>A single number out of range is clamped with a warning (PROF-04).</li>
 * <li>A value of the wrong type, an unknown allowed value, an inverted min/max pair (PROF-06) or a failed cross-option
 * check is an error: the profile is rejected with a message, and mod loading never fails (P-PROF-1, D31).</li>
 * <li>{@code landscapeType} must be {@code default}, so the allowed list and the check are one list (PROF-15).</li>
 * <li>{@code cityStyleThreshold} is -1 (off) or 0 to 1 (PROF-16); with it on, {@code cityStyleAlternative} must name
 * a city style (PROF-12; that the style exists is checked against the loaded assets by {@link ProfileRegistry}).</li>
 * <li>A duplicate {@code forceSpawnBuildings} or {@code forceSpawnParts} entry is dropped with a warning
 * (CORE-13).</li>
 * <li>Dropped options are read and ignored; unknown keys are ignored with a warning.</li>
 * </ul>
 * Upstream's messages for its own checks are kept, in English, because they go to the log with the file name, not to
 * a player.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class ProfileValidator {

    /** The highest ground level whose highways still fit below Y 255 (P06-6, D31). */
    public static final int MAX_GROUND_LEVEL = 244;

    /**
     * The tallest highway part, in blocks above its level's ground, that {@code highwayNetworkLevel} must leave room
     * for under Y 255: 16, which gives level 28 at ground 71 (docs/new-spec/02-profiles-dimensions/
     * profile-options.md). M06 confirms it against its parts.
     */
    public static final int HIGHWAY_PART_HEIGHT = 16;

    /** The lowest Y a railway may run at: RWG's bedrock is Y 0 to 4 (P06-7, D31). */
    public static final int MIN_RAIL_Y = 5;

    /** Blocks per city level. */
    static final int FLOOR_HEIGHT = 6;

    /** Upstream's railway depth: three levels below the ground ({@code -3 + railwayLevelOffset}). */
    static final int RAIL_LEVELS_BELOW = 3;

    /** The outcome: the values, or the errors that reject the profile, and the warnings either way. */
    public static final class Result {

        private final Map<String, String> values;
        private final List<String> warnings;
        private final List<String> errors;
        private final List<String> ignored;

        Result(Map<String, String> values, List<String> warnings, List<String> errors, List<String> ignored) {
            this.values = Collections.unmodifiableMap(new TreeMap<>(values));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.ignored = Collections.unmodifiableList(new ArrayList<>(ignored));
        }

        /** Returns every kept option's value, by key, sorted. Meaningless when {@link #ok()} is false. */
        public Map<String, String> values() {
            return values;
        }

        /** Returns what was fixed or ignored, one line each. */
        public List<String> warnings() {
            return warnings;
        }

        /** Returns why the profile is rejected, one line each; empty when it is accepted. */
        public List<String> errors() {
            return errors;
        }

        /** Returns the dropped keys the profile set, which were read and ignored. */
        public List<String> ignored() {
            return ignored;
        }

        /** Returns whether the profile is accepted. */
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    private ProfileValidator() {}

    /**
     * Validates given values over the defaults.
     *
     * @param given option key (or alias) to text, as a profile file or a built-in sets them
     * @return the result
     */
    public static Result validate(Map<String, String> given) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        Map<String, String> values = ProfileOptions.defaults();
        for (Map.Entry<String, String> entry : new TreeMap<>(given).entrySet()) {
            ProfileOption option = ProfileOptions.get(entry.getKey());
            if (option == null) {
                warnings.add("unknown option " + entry.getKey() + " ignored");
                continue;
            }
            if (!option.kept()) {
                ignored.add(option.key());
                continue;
            }
            String canonical = canonical(option, entry.getValue(), warnings, errors);
            if (canonical != null) {
                values.put(option.key(), canonical);
                if (option.status() == Status.DEFERRED && !canonical.equals(option.defaultValue())) {
                    warnings.add(option.key() + " has no effect: the landscapes it belongs to are not ported yet");
                }
            }
        }
        crossChecks(values, warnings, errors);
        return new Result(values, warnings, errors, ignored);
    }

    /**
     * Returns an option's canonical text for a given text, or {@code null} after recording an error.
     *
     * @param option   the option
     * @param text     the given text
     * @param warnings where a clamp goes
     * @param errors   where a wrong type or unknown value goes
     * @return the canonical text, or {@code null}
     */
    static String canonical(ProfileOption option, String text, List<String> warnings, List<String> errors) {
        String trimmed = text == null ? "" : text.trim();
        switch (option.type()) {
            case BOOLEAN:
                if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                    return trimmed.toLowerCase(Locale.ROOT);
                }
                errors.add(option.key() + "='" + text + "' is not true or false");
                return null;
            case INT:
            case FLOAT:
            case DOUBLE:
                double number;
                try {
                    number = Double.parseDouble(trimmed);
                } catch (NumberFormatException e) {
                    errors.add(option.key() + "='" + text + "' is not a number");
                    return null;
                }
                if (Double.isNaN(number) || Double.isInfinite(number)) {
                    errors.add(option.key() + "='" + text + "' is not a finite number");
                    return null;
                }
                return option.canonical(number, warnings);
            case STRING_LIST:
                List<String> list = ProfileValues.parseList(trimmed);
                if (list.isEmpty() && !"[]".equals(trimmed.replace(" ", ""))) {
                    errors.add(option.key() + "='" + text + "' is not a list of strings");
                    return null;
                }
                Set<String> unique = new LinkedHashSet<>(list);
                if (unique.size() != list.size()) {
                    // Avoids CORE-13: upstream built an immutable set over the list, and a duplicate crashed world
                    // creation.
                    warnings.add(option.key() + " lists an entry twice; the duplicate is ignored");
                }
                return ProfileValues.listText(new ArrayList<>(unique));
            default:
                if (option.allowed()
                    .isEmpty()) {
                    return text == null ? "" : text;
                }
                for (String allowed : option.allowed()) {
                    if (allowed.equalsIgnoreCase(trimmed)) {
                        return allowed;
                    }
                }
                if ("landscapeType".equals(option.key())) {
                    // Avoids PROF-15: one list of landscape types, checked; the others need terrain RWG lacks (D12).
                    errors.add("landscapeType '" + text + "' is not supported: only 'default' is ported");
                } else {
                    errors.add(option.key() + " '" + text + "' is not one of " + option.allowed());
                }
                return null;
        }
    }

    private static void crossChecks(Map<String, String> values, List<String> warnings, List<String> errors) {
        // Avoids PROF-06: every min/max pair a draw is made between is ordered here, so no draw can throw.
        pair(values, "explosionMinRadius", "explosionMaxRadius", errors);
        pair(values, "explosionMinHeight", "explosionMaxHeight", errors);
        pair(values, "miniExplosionMinRadius", "miniExplosionMaxRadius", errors);
        pair(values, "miniExplosionMinHeight", "miniExplosionMaxHeight", errors);
        pair(values, "terrainFixLowerMinOffset", "terrainFixLowerMaxOffset", errors);
        pair(values, "terrainFixUpperMinOffset", "terrainFixUpperMaxOffset", errors);
        if (number(values, "cityChance") >= 0) {
            pair(values, "cityMinRadius", "cityMaxRadius", errors);
        }
        if ("grid".equals(values.get("cityPlacement"))) {
            if (number(values, "cityChance") < 0) {
                errors.add("cityPlacement grid cannot be combined with Perlin rarity (negative cityChance)");
            }
            if (number(values, "cityGridCell") - 2 * number(values, "cityGridJitter") < 1) {
                errors.add("cityGridJitter must be below half of cityGridCell");
            }
        }
        pair(values, "buildingMinFloors", "buildingMaxFloors", errors);
        pair(values, "buildingMinCellars", "buildingMaxCellars", errors);
        pair(values, "ruinMinlevelPercent", "ruinMaxlevelPercent", errors);
        if (number(values, "secondaryRoadMinCountX") > number(values, "secondaryRoadMaxCountX")
            || number(values, "secondaryRoadMinCountZ") > number(values, "secondaryRoadMaxCountZ")) {
            errors.add("Secondary road minimum counts cannot exceed maximum counts");
        }
        if (number(values, "tertiaryRoadMinLength") > number(values, "tertiaryRoadMaxLength")) {
            errors.add("tertiaryRoadMinLength cannot exceed tertiaryRoadMaxLength");
        }
        if (number(values, "highwayHubSampleSpacing") > number(values, "highwayPlanningCellSize")) {
            errors.add("highwayHubSampleSpacing cannot exceed highwayPlanningCellSize");
        }
        if (number(values, "highwayMinimumHubDistance") > number(values, "highwayMaximumHubDistance")) {
            errors.add("highwayMinimumHubDistance cannot exceed highwayMaximumHubDistance");
        }
        for (String key : new String[] { "railwaySpacingNorthSouth", "railwaySpacingEastWest" }) {
            if (((int) number(values, key)) % 2 != 0) {
                errors.add("Railway spacing must be an even number from 4 through 128 chunks (" + key + ")");
            }
        }
        int ground = (int) number(values, "groundLevel");
        if (ground > MAX_GROUND_LEVEL) {
            errors.add(
                "groundLevel " + ground
                    + " leaves no room for highways below Y 255; at most "
                    + MAX_GROUND_LEVEL
                    + " is allowed");
        } else {
            clampHighwayLevel(values, ground, warnings);
            clampRailwayOffset(values, ground, warnings);
        }
        float threshold = (float) number(values, "cityStyleThreshold");
        if (threshold < 0 && threshold != -1f) {
            warnings.add("cityStyleThreshold " + threshold + " is negative, which means off; using -1");
            values.put("cityStyleThreshold", Float.toString(-1f));
            threshold = -1f;
        }
        if (threshold >= 0 && values.get("cityStyleAlternative")
            .trim()
            .isEmpty()) {
            // Avoids PROF-12: an empty alternative style crashed generation the first time a chunk fell below it.
            errors.add("cityStyleThreshold is on but cityStyleAlternative names no city style");
        }
        for (int i = 1; i < 8; i++) {
            if (number(values, "cityLevel" + i + "Height") < number(values, "cityLevel" + (i - 1) + "Height")) {
                warnings.add(
                    "cityLevel" + i
                        + "Height is below cityLevel"
                        + (i - 1)
                        + "Height, so city level "
                        + i
                        + " is never reached");
            }
        }
        String treatments = values.get("landmarkTreatment");
        String problem = landmarkTreatmentProblem(treatments);
        if (problem != null) {
            errors.add(problem);
        }
        if (!values.get("spawnSphere")
            .isEmpty()) {
            warnings.add("spawnSphere has no effect: city spheres are not ported yet");
        }
    }

    private static void clampHighwayLevel(Map<String, String> values, int ground, List<String> warnings) {
        int maxLevel = (int) Math.floor((ProfileOptions.MAX_Y - HIGHWAY_PART_HEIGHT - ground) / FLOOR_HEIGHT);
        int level = (int) number(values, "highwayNetworkLevel");
        if (level > maxLevel) {
            warnings.add(
                "highwayNetworkLevel " + level
                    + " would put highways above Y 255 at groundLevel "
                    + ground
                    + "; using "
                    + maxLevel);
            values.put("highwayNetworkLevel", Integer.toString(maxLevel));
        }
    }

    private static void clampRailwayOffset(Map<String, String> values, int ground, List<String> warnings) {
        int offset = (int) number(values, "railwayLevelOffset");
        int railY = ground + FLOOR_HEIGHT * (offset - RAIL_LEVELS_BELOW);
        if (railY < MIN_RAIL_Y) {
            int lowest = (int) Math.ceil((MIN_RAIL_Y - ground) / (double) FLOOR_HEIGHT) + RAIL_LEVELS_BELOW;
            warnings.add(
                "railwayLevelOffset " + offset
                    + " would put railways at Y "
                    + railY
                    + ", in RWG's bedrock; using "
                    + lowest);
            values.put("railwayLevelOffset", Integer.toString(lowest));
        }
    }

    private static void pair(Map<String, String> values, String minKey, String maxKey, List<String> errors) {
        double min = number(values, minKey);
        double max = number(values, maxKey);
        if (max < min) {
            errors.add(maxKey + " (" + values.get(maxKey) + ") is below " + minKey + " (" + values.get(minKey) + ")");
        }
    }

    private static double number(Map<String, String> values, String key) {
        String text = values.get(key);
        if (text == null) {
            text = ProfileOptions.get(key)
                .defaultValue();
        }
        return Double.parseDouble(text);
    }

    /**
     * Returns what is wrong with a {@code landmarkTreatment} value, or {@code null} when it is well formed. The value
     * is a comma-separated list: a bare treatment sets every landmark kind other than villages, and
     * {@code <kind>=<treatment>} sets one kind, the kind in lower case ({@code ocean_monument=AVOID}).
     *
     * @param text the value
     * @return the problem, or {@code null}
     */
    public static String landmarkTreatmentProblem(String text) {
        try {
            parseLandmarkTreatments(text);
            return null;
        } catch (IllegalArgumentException e) {
            return "landmarkTreatment '" + text + "': " + e.getMessage();
        }
    }

    /**
     * Parses a {@code landmarkTreatment} value into one treatment per landmark kind other than villages.
     *
     * @param text the value; empty means {@code BUILD_AROUND} for every kind
     * @return the treatments, by kind
     * @throws IllegalArgumentException for an unknown kind or treatment
     */
    public static Map<LandmarkKind, LandmarkTreatment> parseLandmarkTreatments(String text) {
        Map<LandmarkKind, LandmarkTreatment> out = new TreeMap<>();
        LandmarkTreatment all = LandmarkTreatment.BUILD_AROUND;
        Map<LandmarkKind, LandmarkTreatment> specific = new TreeMap<>();
        for (String part : text.split(",")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int equals = entry.indexOf('=');
            if (equals < 0) {
                all = treatment(entry);
                continue;
            }
            LandmarkKind kind = kind(
                entry.substring(0, equals)
                    .trim());
            specific.put(
                kind,
                treatment(
                    entry.substring(equals + 1)
                        .trim()));
        }
        for (LandmarkKind kind : LandmarkKind.values()) {
            if (kind != LandmarkKind.VILLAGE) {
                LandmarkTreatment value = specific.get(kind);
                out.put(kind, value == null ? all : value);
            }
        }
        return out;
    }

    private static LandmarkTreatment treatment(String text) {
        for (LandmarkTreatment treatment : LandmarkTreatment.values()) {
            if (treatment.name()
                .equalsIgnoreCase(text)) {
                return treatment;
            }
        }
        throw new IllegalArgumentException("unknown treatment '" + text + "'");
    }

    private static LandmarkKind kind(String text) {
        for (LandmarkKind kind : LandmarkKind.values()) {
            if (kind != LandmarkKind.VILLAGE && kind.name()
                .equalsIgnoreCase(text)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unknown landmark kind '" + text + "' (villageTreatment sets villages)");
    }
}
