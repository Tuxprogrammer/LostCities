package com.tuxprogrammer.lostcities.config.profile;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The profiles that ship in the jar. M04 has one, {@code default}: upstream's default profile on RWG, with the numbers
 * of docs/new-spec/03-terrain/height-mapping.md. M09 adds upstream's other overlay profiles and custom profiles from
 * {@code config/lostcitiesnh/profiles/} (docs/design/0005-rwg-overlay.md §7).
 *
 * <p>
 * Side: both (plain Java).
 */
public final class BuiltinProfiles {

    /** The default profile's name. */
    public static final String DEFAULT = "default";

    private BuiltinProfiles() {}

    /** Returns the names of the built-in profiles, sorted. */
    public static Set<String> names() {
        Set<String> names = new TreeSet<>();
        names.add(DEFAULT);
        return Collections.unmodifiableSet(names);
    }

    /**
     * Returns every resolved value of a built-in profile.
     *
     * @param name the profile's name
     * @return option name to text value
     * @throws IllegalArgumentException for an unknown name
     */
    public static Map<String, String> values(String name) {
        if (DEFAULT.equals(name)) {
            Map<String, String> values = TerrainSettings.defaults()
                .toValues();
            // RWG already places bedrock at Y 0 to 4 and the overlay writes none (0005 §6), so the built-in profile
            // says so; a custom profile asking for a bedrock layer gets a warning that it has no effect.
            values.put("bedrockLayer", "0");
            return values;
        }
        throw new IllegalArgumentException("unknown profile '" + name + "'; known: " + names());
    }
}
