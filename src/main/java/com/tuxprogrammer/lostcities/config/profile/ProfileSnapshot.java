package com.tuxprogrammer.lostcities.config.profile;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import com.tuxprogrammer.lostcities.asset.block.DecorativeMetal;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.core.Module;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * What a world was created with, pinned into its saved data ({@code data/lostcitiesnh.dat}) when the overlay is first
 * installed on it: the profile's name and every resolved value, the module flags, the plan source, the asset-set hash,
 * the hash of every asset file, the decorative-metal flags, the layout version and the world type
 * (docs/design/0005-rwg-overlay.md §7). From then on the world reads the snapshot, so editing the config never changes
 * a world that exists (D22, D31); a changed asset set is warned about, naming the files (P05-3).
 *
 * <p>
 * Immutable; plain data, so the saved-data class only converts it to and from NBT. Side: both (plain Java); only the
 * server pins or reads one.
 */
public final class ProfileSnapshot {

    /**
     * Version of the snapshot format; a reader treats missing keys of older versions as their defaults. Format 2 adds
     * the asset files' hashes and the decorative-metal flags.
     */
    public static final int FORMAT = 2;

    /** Asset-set hash of a world pinned before assets were pinned (M04 worlds): no change warning is possible. */
    public static final String NO_ASSETS = "none";

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
    private final Map<String, String> assetFiles;
    private final Map<String, Boolean> metals;
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
        this(
            profile,
            planSource,
            values,
            modules,
            assetSetHash,
            Collections.<String, String>emptyMap(),
            Collections.<String, Boolean>emptyMap(),
            layoutVersion,
            worldType,
            createdBy);
    }

    /**
     * Creates a snapshot with its pinned assets and metals.
     *
     * @param profile       the profile's name
     * @param planSource    {@link #PLAN_LAYOUT} or {@link #PLAN_SYNTHETIC}
     * @param values        every resolved profile value, by option name
     * @param modules       every module flag by its config key
     * @param assetSetHash  the hash of the loaded asset set, or {@link #NO_ASSETS}
     * @param assetFiles    every asset file's hash, by {@code <folder>/<id>}; empty when none were pinned
     * @param metals        every decorative-metal flag by its key; empty when none were pinned (all on)
     * @param layoutVersion the layout rules' version
     * @param worldType     the world type's name, for example {@code RWG2_CONTINENTS}
     * @param createdBy     the mod version that pinned it
     */
    public ProfileSnapshot(String profile, String planSource, Map<String, String> values, Map<String, Boolean> modules,
        String assetSetHash, Map<String, String> assetFiles, Map<String, Boolean> metals, int layoutVersion,
        String worldType, String createdBy) {
        this.profile = Objects.requireNonNull(profile);
        this.planSource = Objects.requireNonNull(planSource);
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
        this.modules = Collections.unmodifiableMap(new TreeMap<>(modules));
        this.assetSetHash = Objects.requireNonNull(assetSetHash);
        this.assetFiles = Collections.unmodifiableMap(new TreeMap<>(assetFiles));
        this.metals = Collections.unmodifiableMap(new TreeMap<>(metals));
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
        return resolve(
            profile,
            planSource,
            moduleState,
            assetSetHash,
            Collections.<String, String>emptyMap(),
            MetalFlags.ALL_ON,
            worldType,
            createdBy);
    }

    /**
     * Resolves a profile into a snapshot that pins the asset files and the decorative-metal flags.
     *
     * @param profile      the profile's name
     * @param planSource   the plan source
     * @param moduleState  the module flags as the config holds them now
     * @param assetSetHash the hash of the loaded asset set
     * @param assetFiles   every asset file's hash, by {@code <folder>/<id>}
     * @param metals       the decorative-metal flags as the config holds them now
     * @param worldType    the world type's name
     * @param createdBy    the mod version
     * @return the snapshot
     * @throws IllegalArgumentException for an unknown profile
     */
    public static ProfileSnapshot resolve(String profile, String planSource, ModuleState moduleState,
        String assetSetHash, Map<String, String> assetFiles, MetalFlags metals, String worldType, String createdBy) {
        Map<String, String> values = BuiltinProfiles.values(profile);
        Map<String, Boolean> modules = new LinkedHashMap<>();
        for (Module module : Module.values()) {
            modules.put(module.configKey(), moduleState.isActive(module));
        }
        Map<String, Boolean> metalMap = new LinkedHashMap<>();
        for (DecorativeMetal metal : DecorativeMetal.values()) {
            metalMap.put(metal.key(), metals.isOn(metal));
        }
        return new ProfileSnapshot(
            profile,
            planSource,
            values,
            modules,
            assetSetHash,
            assetFiles,
            metalMap,
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

    /** Returns every pinned asset file's hash, by {@code <folder>/<id>}, sorted; empty when none were pinned. */
    public Map<String, String> assetFiles() {
        return assetFiles;
    }

    /** Returns every pinned decorative-metal flag by its key, sorted; empty when none were pinned. */
    public Map<String, Boolean> metals() {
        return metals;
    }

    /**
     * Returns the pinned decorative-metal flags (D61); a metal the snapshot does not name is on, the config default,
     * so worlds pinned before the flags were pinned keep generating as they did.
     */
    public MetalFlags metalFlags() {
        EnumSet<DecorativeMetal> on = EnumSet.noneOf(DecorativeMetal.class);
        for (DecorativeMetal metal : DecorativeMetal.values()) {
            Boolean value = metals.get(metal.key());
            if (value == null || value) {
                on.add(metal);
            }
        }
        return MetalFlags.of(on);
    }

    /**
     * Returns the asset files that changed since the world was pinned (P05-3), or an empty list when the loaded set's
     * hash is the pinned one or nothing was pinned to compare with.
     *
     * @param loadedHash  the loaded set's hash
     * @param changedFrom the asset set's comparison of its files with pinned file hashes
     * @return the changed files, {@code <folder>/<id>}, sorted
     */
    public List<String> changedAssets(String loadedHash, Function<Map<String, String>, List<String>> changedFrom) {
        if (NO_ASSETS.equals(assetSetHash) || assetSetHash.equals(loadedHash)) {
            return Collections.emptyList();
        }
        return changedFrom.apply(assetFiles);
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
            && assetFiles.equals(that.assetFiles)
            && metals.equals(that.metals)
            && layoutVersion == that.layoutVersion
            && worldType.equals(that.worldType)
            && createdBy.equals(that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, planSource, values, modules, assetSetHash, layoutVersion, worldType);
    }
}
