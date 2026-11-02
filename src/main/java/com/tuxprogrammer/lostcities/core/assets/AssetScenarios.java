package com.tuxprogrammer.lostcities.core.assets;

import java.util.ArrayList;
import java.util.List;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;

/**
 * The asset system's in-world scenarios (docs/testing/README.md, "Scenario list"):
 * <ul>
 * <li>{@code assets.loaded}: the set loaded at {@code postInit} holds every bundled asset with no error, every
 * code-named block resolved, and a biome descriptor can be built for the arena, with RWG's realistic biome where an
 * RWG chunk manager runs the world;</li>
 * <li>{@code assets.pack_resolution} (manual-only, for the real pack): every block the assets and the variety pools
 * name resolves to its primary, with no fallback taken; the full report goes to the log, so a real-pack run lists every
 * unresolved name.</li>
 * </ul>
 * Side: server.
 */
public final class AssetScenarios {

    /** The fewest assets the loaded set holds: upstream's 322 plus this mod's two rubble variants. */
    static final int MIN_ASSETS = 324;

    /** How many unresolved names a failure message lists before "and N more". */
    private static final int LISTED = 20;

    private AssetScenarios() {}

    /**
     * Adds the scenarios to a registry.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Loaded());
        registry.register(new PackResolution());
    }

    /** The asset set is loaded and complete. */
    static final class Loaded implements Scenario {

        @Override
        public String name() {
            return "assets.loaded";
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public void setup(ScenarioContext context) {
            context.check(LoadedAssets.isLoaded(), "expected the assets loaded at postInit, found none");
        }

        @Override
        public void verify(ScenarioContext context) {
            AssetSet set = LoadedAssets.current();
            List<AssetProblem> errors = set.problems(AssetProblem.Severity.ERROR);
            context.check(errors.isEmpty(), "expected no asset errors, found " + errors.size() + ": " + first(errors));
            context
                .check(set.count() >= MIN_ASSETS, "expected at least " + MIN_ASSETS + " assets, found " + set.count());
            context.check(set.count(AssetType.WORLDSTYLES) >= 2, "expected the two bundled world styles");
            context.check(
                set.codeBlocks()
                    .size() == com.tuxprogrammer.lostcities.asset.AssetLoader.CODE_BLOCKS.size(),
                "expected every code-named block resolved, found " + set.codeBlocks()
                    .keySet());
            int x = context.arenaChunkX() * 16 + 8;
            int z = context.arenaChunkZ() * 16 + 8;
            BiomeDescriptor biome = BiomeDescriptors.at(context.world(), x, z);
            String manager = context.world()
                .getWorldChunkManager()
                .getClass()
                .getName();
            LostCities.LOG.info(LoadedAssets.TAG + "arena biome " + biome + " (chunk manager " + manager + ")");
            context.check(
                !manager.startsWith("rwg") || biome.realisticBiome() != null,
                "expected an RWG realistic biome under " + manager + ", found none");
        }
    }

    /** Every primary block resolves in the running pack. */
    static final class PackResolution implements Scenario {

        @Override
        public String name() {
            return "assets.pack_resolution";
        }

        @Override
        public boolean manualOnly() {
            return true;
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public void setup(ScenarioContext context) {
            context.check(LoadedAssets.isLoaded(), "expected the assets loaded at postInit, found none");
        }

        @Override
        public void verify(ScenarioContext context) {
            AssetSet set = LoadedAssets.current();
            LoadedAssets.log(set, true);
            List<String> fallbacks = new ArrayList<>();
            for (BlockResolver.Fallback fallback : set.fallbacks()) {
                fallbacks.add(fallback.toString());
            }
            List<String> pool = set.pools()
                .unresolved();
            LostCities.LOG.info(
                LoadedAssets.TAG + "resolution: "
                    + fallbacks.size()
                    + " fallbacks, "
                    + pool.size()
                    + " of "
                    + set.pools()
                        .blockCount()
                    + " pool blocks unresolved");
            context.check(
                fallbacks.isEmpty(),
                "expected no fallbacks, found " + fallbacks.size() + ": " + first(fallbacks));
            context.check(
                pool.isEmpty(),
                "expected every pool block registered, missing " + pool.size() + ": " + first(pool));
        }
    }

    private static String first(List<?> items) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(LISTED, items.size()); i++) {
            out.append(i == 0 ? "" : "; ")
                .append(items.get(i));
        }
        if (items.size() > LISTED) {
            out.append("; and ")
                .append(items.size() - LISTED)
                .append(" more");
        }
        return out.toString();
    }
}
