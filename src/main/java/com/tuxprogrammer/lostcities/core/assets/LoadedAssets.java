package com.tuxprogrammer.lostcities.core.assets;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetLoader;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetSource;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.BlockData;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;

/**
 * Loads the asset set once per game session, at {@code postInit}, and holds it: the jar's bundled files, then
 * {@code config/lostcitiesnh/assets/}, resolved against Forge's block registry (docs/new-spec/05-assets/
 * asset-format.md, "Loading and override"; D14). A changed override folder takes effect at the next start (P05-3).
 *
 * <p>
 * Logging follows CONTRIBUTING § Code standards: an asset that fails is skipped with one error line naming its file and
 * field, and loading carries on; warnings are logged once; notes (fallbacks taken, unreachable assets) go to the debug
 * log, or to the main log with {@code -Dlostcitiesnh.assetReport=true}. One summary line always reaches the log. The
 * dev
 * self-check fails on any error ({@link AssetSelfCheck}).
 *
 * <p>
 * The set is a registry-like holder the game session owns, the one mutable static the asset system has (CONTRIBUTING §
 * Architecture). Side: both; read on the server thread.
 */
public final class LoadedAssets {

    /** The JVM property that logs the full report at {@code postInit}. */
    public static final String REPORT_PROPERTY = "lostcitiesnh.assetReport";

    /** The override folder, relative to the game's config folder. */
    public static final String OVERRIDE_FOLDER = LostCities.MODID + "/assets";

    /** Prefix of every report line, so automation can grep the log. */
    public static final String TAG = "[LCNH-ASSETS] ";

    private static volatile AssetSet current;

    private LoadedAssets() {}

    /**
     * Loads the assets and logs the outcome. Call once, in {@code postInit}.
     *
     * @param configDir the game's config folder
     * @return the set
     */
    public static AssetSet load(File configDir) {
        File overrides = new File(configDir, OVERRIDE_FOLDER);
        AssetSet set = load(
            Arrays.asList(AssetSource.bundled(LoadedAssets.class), AssetSource.directory(overrides.toPath())),
            ForgeBlockRegistry.INSTANCE);
        current = set;
        log(set, Boolean.getBoolean(REPORT_PROPERTY));
        return set;
    }

    /**
     * Loads an asset set without holding it, for tests and scenarios.
     *
     * @param sources  the sources, jar first
     * @param registry the registry view
     * @return the set
     */
    public static AssetSet load(List<AssetSource> sources, BlockRegistryView registry) {
        Problems problems = new Problems();
        BlockData data = BlockData.bundled(LoadedAssets.class, problems);
        return AssetLoader.load(sources, data, registry, problems);
    }

    /**
     * Returns the loaded set.
     *
     * @return the set
     * @throws IllegalStateException before {@code postInit}
     */
    public static AssetSet current() {
        AssetSet set = current;
        if (set == null) {
            throw new IllegalStateException("assets are loaded at postInit; nothing may ask for them earlier");
        }
        return set;
    }

    /** Returns whether the assets are loaded. */
    public static boolean isLoaded() {
        return current != null;
    }

    /**
     * Logs a set's report.
     *
     * @param set  the set
     * @param full whether notes, fallbacks and pool blocks go to the main log instead of the debug log
     */
    public static void log(AssetSet set, boolean full) {
        Level notes = full ? Level.INFO : Level.DEBUG;
        for (AssetProblem problem : set.problems()) {
            switch (problem.severity()) {
                case ERROR:
                    LostCities.LOG.error(TAG + problem);
                    break;
                case WARNING:
                    LostCities.LOG.warn(TAG + problem);
                    break;
                default:
                    LostCities.LOG.log(notes, TAG + problem);
            }
        }
        for (BlockResolver.Fallback fallback : set.fallbacks()) {
            LostCities.LOG.log(notes, TAG + "fallback " + fallback);
        }
        for (String block : set.pools()
            .unresolved()) {
            LostCities.LOG.log(notes, TAG + "pool block not registered: " + block);
        }
        LostCities.LOG.info(TAG + "loaded " + set.summary());
    }

    /**
     * Resolves one block name through the block-name map against Forge's registry, as an asset's block would be: a
     * 1.20 name such as a profile's {@code liquidBlock} or {@code baseBlock} (D14, D19), or a direct
     * {@code modid:name:meta} reference.
     *
     * @param name     the name
     * @param problems where an unresolvable name's reason goes
     * @return the block, or {@code null} when it cannot be resolved
     */
    public static BlockRef resolveBlock(String name, List<String> problems) {
        Problems collected = new Problems();
        BlockData data = BlockData.bundled(LoadedAssets.class, collected);
        BlockResolver resolver = new BlockResolver(
            data.mapping(),
            data.metals(),
            data.neverPlace(),
            ForgeBlockRegistry.INSTANCE);
        BlockChoice choice = resolver.resolve(name, null, null, collected.in("profile"), "block");
        for (AssetProblem problem : collected.all()) {
            if (problem.isError()) {
                problems.add(problem.message());
            }
        }
        return choice == null ? null : choice.block();
    }
}
