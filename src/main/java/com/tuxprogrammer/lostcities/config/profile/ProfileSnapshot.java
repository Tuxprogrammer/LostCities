package com.tuxprogrammer.lostcities.config.profile;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.core.Module;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * What a world was created with, pinned into its saved data ({@code data/lostcitiesnh.dat}) when the overlay is first
 * installed on it: the profile's name and every resolved value, the module flags, the plan source, the asset-set hash,
 * the layout version and the world type (docs/design/0005-rwg-overlay.md §7). From then on the world reads the
 * snapshot, so editing the config never changes a world that exists (D22, D31).
 *
 * <p>
 * Immutable; plain data, so the saved-data class only converts it to and from NBT. Side: both (plain Java); only the
 * server pins or reads one.
 */
public final class ProfileSnapshot {

    /** Version of the snapshot format; a reader treats missing keys of older versions as their defaults. */
    public static final int FORMAT = 1;

    /**
     * Version of the layout rules. A change that moves cities for an existing seed raises it, and a world pinned with
     * an older version logs that its new chunks may not match (CONTRIBUTING § World generation rules).
     */
    public static final int LAYOUT_VERSION = 1;

    /** Plan source of real cities: the layout (M03), joined to the overlay in M05; until then it plans none. */
    public static final String PLAN_LAYOUT = "layout";

    /** Plan source of the synthetic test city (M04), selectable only with the test hooks. */
    public static final String PLAN_SYNTHETIC = "synthetic";

    private final String profile;
    private final String planSource;
    private final Map<String, String> values;
    private final Map<String, Boolean> modules;
    private final String assetSetHash;
    private final int layoutVersion;
    private final String worldType;
    private final String createdBy;

    /**
     * Creates a snapshot.
     *
     * @param profile       the profile's name
     * @param planSource    {@link #PLAN_LAYOUT} or {@link #PLAN_SYNTHETIC}
     * @param values        every resolved profile value, by option name
     * @param modules       every module flag by its config key
     * @param assetSetHash  the hash of the loaded asset set
     * @param layoutVersion the layout rules' version
     * @param worldType     the world type's name, for example {@code RWG2_CONTINENTS}
     * @param createdBy     the mod version that pinned it
     */
    public ProfileSnapshot(String profile, String planSource, Map<String, String> values, Map<String, Boolean> modules,
        String assetSetHash, int layoutVersion, String worldType, String createdBy) {
        this.profile = Objects.requireNonNull(profile);
        this.planSource = Objects.requireNonNull(planSource);
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
        this.modules = Collections.unmodifiableMap(new TreeMap<>(modules));
        this.assetSetHash = Objects.requireNonNull(assetSetHash);
        this.layoutVersion = layoutVersion;
        this.worldType = Objects.requireNonNull(worldType);
        this.createdBy = Objects.requireNonNull(createdBy);
    }

    /**
     * Resolves a profile into a snapshot for a world being created or opted in.
     *
     * @param profile      the profile's name
     * @param planSource   the plan source
     * @param moduleState  the module flags as the config holds them now
     * @param assetSetHash the hash of the loaded asset set
     * @param worldType    the world type's name
     * @param createdBy    the mod version
     * @return the snapshot
     * @throws IllegalArgumentException for an unknown profile
     */
    public static ProfileSnapshot resolve(String profile, String planSource, ModuleState moduleState,
        String assetSetHash, String worldType, String createdBy) {
        Map<String, String> values = BuiltinProfiles.values(profile);
        Map<String, Boolean> modules = new LinkedHashMap<>();
        for (Module module : Module.values()) {
            modules.put(module.configKey(), moduleState.isActive(module));
        }
        return new ProfileSnapshot(
            profile,
            planSource,
            values,
            modules,
            assetSetHash,
            LAYOUT_VERSION,
            worldType,
            createdBy);
    }

    /** Returns the profile's name. */
    public String profile() {
        return profile;
    }

    /** Returns the plan source. */
    public String planSource() {
        return planSource;
    }

    /** Returns every pinned value, by option name, sorted. */
    public Map<String, String> values() {
        return values;
    }

    /** Returns every pinned module flag, by config key, sorted. */
    public Map<String, Boolean> modules() {
        return modules;
    }

    /** Returns the pinned module flags as a module state; a missing key is active, the config default. */
    public ModuleState moduleState() {
        EnumSet<Module> active = EnumSet.noneOf(Module.class);
        for (Module module : Module.values()) {
            Boolean on = modules.get(module.configKey());
            if (on == null || on) {
                active.add(module);
            }
        }
        return ModuleState.of(active);
    }

    /** Returns the terrain settings the pinned values give. */
    public TerrainSettings terrainSettings() {
        return TerrainSettings.fromValues(values);
    }

    /** Returns the hash of the asset set the world was created with. */
    public String assetSetHash() {
        return assetSetHash;
    }

    /** Returns the layout rules' version the world was created with. */
    public int layoutVersion() {
        return layoutVersion;
    }

    /** Returns the world type's name. */
    public String worldType() {
        return worldType;
    }

    /** Returns the mod version that pinned the snapshot. */
    public String createdBy() {
        return createdBy;
    }

    /** Returns a one-line summary for logs and commands. */
    public String summary() {
        return profile + " (plan "
            + planSource
            + ", layout v"
            + layoutVersion
            + ", "
            + worldType
            + ", "
            + values.size()
            + " values, pinned by "
            + createdBy
            + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ProfileSnapshot)) {
            return false;
        }
        ProfileSnapshot that = (ProfileSnapshot) other;
        return profile.equals(that.profile) && planSource.equals(that.planSource)
            && values.equals(that.values)
            && modules.equals(that.modules)
            && assetSetHash.equals(that.assetSetHash)
            && layoutVersion == that.layoutVersion
            && worldType.equals(that.worldType)
            && createdBy.equals(that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, planSource, values, modules, assetSetHash, layoutVersion, worldType);
    }
}
