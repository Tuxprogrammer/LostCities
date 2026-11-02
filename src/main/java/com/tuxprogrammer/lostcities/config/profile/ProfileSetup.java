package com.tuxprogrammer.lostcities.config.profile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.config.ProfilesConfig;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;

/**
 * The profile system's place in the mod's lifecycle, and how a dimension gets its profile without a world-preset
 * screen (docs/new-spec/02-profiles-dimensions/pinned-profile.md):
 * <ul>
 * <li>{@code preInit}: read the profile files ({@link ProfileRegistry#load});</li>
 * <li>{@code postInit}, once the assets are loaded: reject profiles that name missing styles, then check every
 * {@code dimensionProfiles} entry, so a typo is reported when the server starts and not only when a world is
 * created;</li>
 * <li>{@code /lostcities profile reload}: both again, while the server runs; worlds already pinned are unaffected.</li>
 * </ul>
 * Single player picks a profile the same way a server does: the Mods list's Config button opens the {@code profiles}
 * category, and the world created next pins what {@code dimensionProfiles} names (D31).
 *
 * <p>
 * Side: both (the integrated server reads the same config); only the server pins.
 */
public final class ProfileSetup {

    /**
     * JVM property that replaces {@code dimensionProfiles} for this run, honored only with the test hooks: entries
     * {@code <dimension>=<profile>} separated by commas, so real-pack runs can create a world with another profile
     * without editing the config file.
     */
    public static final String DIMENSION_PROFILES_PROPERTY = "lostcitiesnh.dimensionProfiles";

    /**
     * JVM property, honored only with the test hooks: option values that replace the named profile's when a world is
     * pinned, {@code <key>=<value>} separated by semicolons, so a real-pack run can try any option without a profile
     * file on the server. The result is validated like a profile file.
     */
    public static final String OVERRIDES_PROPERTY = "lostcitiesnh.profileOverrides";

    private ProfileSetup() {}

    /**
     * Returns the values to pin for a profile: its own, with the test-hook overrides applied when they are set and
     * allowed. Overrides that fail validation are logged and left out.
     *
     * @param values the profile's values
     * @return the values to pin
     */
    public static Map<String, String> withTestOverrides(Map<String, String> values) {
        String property = System.getProperty(OVERRIDES_PROPERTY);
        if (property == null || property.trim()
            .isEmpty() || !DevEnvironment.testHooksEnabled()) {
            return values;
        }
        Map<String, String> given = new TreeMap<>(values);
        for (String entry : property.split(";")) {
            int equals = entry.indexOf('=');
            if (equals > 0) {
                given.put(
                    entry.substring(0, equals)
                        .trim(),
                    entry.substring(equals + 1)
                        .trim());
            }
        }
        ProfileValidator.Result result = ProfileValidator.validate(given);
        for (String warning : result.warnings()) {
            LostCities.LOG.warn("[profiles] test overrides: {}", warning);
        }
        if (!result.ok()) {
            LostCities.LOG.error("[profiles] test overrides rejected: {}", result.errors());
            return values;
        }
        LostCities.LOG.warn("[profiles] test overrides applied to the pinned profile: {}", property);
        return result.values();
    }

    /**
     * Reads the profile files and logs the outcome. Call in {@code preInit}, after the config is registered.
     *
     * @param configDir the game's config folder
     */
    public static void preInit(File configDir) {
        log(ProfileRegistry.INSTANCE.load(new File(configDir, ProfileRegistry.FOLDER).toPath()));
    }

    /**
     * Checks the profiles against the loaded assets and the dimension mapping, and logs the outcome. Call in
     * {@code postInit}, after the assets are loaded.
     *
     * @param assets the loaded assets
     */
    public static void postInit(AssetSet assets) {
        log(checkAssets(assets));
        for (String line : describeDimensions()) {
            LostCities.LOG.info("[profiles] {}", line);
        }
    }

    /**
     * Reads the profile files again and checks them against the loaded assets.
     *
     * @param configDir the game's config folder
     * @param assets    the loaded assets
     * @return the report
     */
    public static List<ProfileRegistry.Message> reload(File configDir, AssetSet assets) {
        List<ProfileRegistry.Message> report = new ArrayList<>(
            ProfileRegistry.INSTANCE.load(new File(configDir, ProfileRegistry.FOLDER).toPath()));
        if (ApiProfiles.INSTANCE.frozen()) {
            report.addAll(ProfileRegistry.INSTANCE.applyApi(ApiProfiles.INSTANCE));
        }
        report.addAll(checkAssets(assets));
        log(report);
        return report;
    }

    /**
     * Freezes the API's profile callbacks and adds their profiles, checked against the loaded assets. Call when a
     * server is about to start: after every mod's {@code postInit}, before any world is pinned (P-CORE-3).
     *
     * @param assets the loaded assets, or {@code null} when they failed to load
     */
    public static void serverAboutToStart(AssetSet assets) {
        ApiProfiles.INSTANCE.freeze();
        if (ApiProfiles.INSTANCE.size() == 0) {
            return;
        }
        List<ProfileRegistry.Message> report = new ArrayList<>(ProfileRegistry.INSTANCE.applyApi(ApiProfiles.INSTANCE));
        if (assets != null) {
            report.addAll(checkAssets(assets));
        }
        log(report);
        for (String line : describeDimensions()) {
            LostCities.LOG.info("[profiles] {}", line);
        }
    }

    private static List<ProfileRegistry.Message> checkAssets(AssetSet assets) {
        return ProfileRegistry.INSTANCE.checkAssets(
            name -> exists(name, true, assets),
            name -> exists(name, false, assets),
            name -> LoadedAssets.resolveBlock(name, new ArrayList<String>()) != null);
    }

    private static boolean exists(String name, boolean worldStyle, AssetSet assets) {
        AssetId id;
        try {
            id = AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return worldStyle ? assets.worldStyle(id) != null : assets.cityStyle(id) != null;
    }

    private static void log(List<ProfileRegistry.Message> report) {
        for (ProfileRegistry.Message message : report) {
            if (message.error()) {
                LostCities.LOG.error("[profiles] {}", message.text());
            } else {
                LostCities.LOG.warn("[profiles] {}", message.text());
            }
        }
        LostCities.LOG.info(
            "[profiles] {} profiles: {}",
            ProfileRegistry.INSTANCE.names()
                .size(),
            String.join(", ", ProfileRegistry.INSTANCE.names()));
    }

    /** Returns one line per mapped dimension: its profile, or why it gets no cities. */
    static List<String> describeDimensions() {
        List<String> out = new ArrayList<>();
        for (String entry : entries()) {
            int equals = entry.indexOf('=');
            String profile = equals < 0 ? ""
                : entry.substring(equals + 1)
                    .trim();
            Integer dimension = dimensionOf(entry);
            if (dimension == null) {
                out.add("dimensionProfiles entry '" + entry + "' is not <dimension id>=<profile>; ignored");
                continue;
            }
            String name = profile.isEmpty() ? BuiltinProfiles.DEFAULT : profile;
            if (ProfileRegistry.INSTANCE.get(name) == null) {
                String reason = ProfileRegistry.INSTANCE.rejection(name);
                out.add(
                    "dimension " + dimension
                        + " names profile '"
                        + name
                        + "', which "
                        + (reason == null ? "does not exist" : "was rejected")
                        + ": new worlds get no cities there");
            } else {
                out.add("dimension " + dimension + ": new worlds pin profile '" + name + "'");
            }
        }
        return out;
    }

    /**
     * Returns the profile {@code dimensionProfiles} (or, with the test hooks, the JVM property) names for a dimension.
     *
     * @param dimension the dimension ID
     * @return the profile's name, or {@code null} when the dimension is not listed
     */
    public static String profileFor(int dimension) {
        for (String entry : entries()) {
            Integer id = dimensionOf(entry);
            if (id == null) {
                LostCities.LOG.warn("dimensionProfiles entry '{}' has no dimension number; ignored", entry);
                continue;
            }
            if (id == dimension) {
                String profile = entry.substring(entry.indexOf('=') + 1)
                    .trim();
                return profile.isEmpty() ? BuiltinProfiles.DEFAULT : profile;
            }
        }
        return null;
    }

    /** Returns the mapping's entries: the test-hook property's when set and allowed, else the config's. */
    static List<String> entries() {
        List<String> out = new ArrayList<>();
        String property = System.getProperty(DIMENSION_PROFILES_PROPERTY);
        if (property != null && DevEnvironment.testHooksEnabled()) {
            for (String entry : property.split(",")) {
                if (!entry.trim()
                    .isEmpty()) {
                    out.add(entry.trim());
                }
            }
            return out;
        }
        for (String entry : ProfilesConfig.dimensionProfiles) {
            if (entry != null && !entry.trim()
                .isEmpty()) {
                out.add(entry.trim());
            }
        }
        return out;
    }

    /** Returns an entry's dimension ID, or {@code null} when it has none. */
    static Integer dimensionOf(String entry) {
        int equals = entry.indexOf('=');
        if (equals <= 0) {
            return null;
        }
        try {
            return Integer.parseInt(
                entry.substring(0, equals)
                    .trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
