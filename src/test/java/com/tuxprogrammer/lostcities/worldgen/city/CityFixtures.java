package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Real cities for tests: the bundled assets resolved as the full pack would resolve them, the layout over the
 * {@link TestTerrain} stand-in for RWG, and the real plan source and writer over it. Test code only.
 */
public final class CityFixtures {

    /** The bundled assets over the full pack's registry, loaded once. */
    public static final AssetSet ASSETS = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));

    /** The seed most tests use. */
    public static final long SEED = 4242L;

    private CityFixtures() {}

    /** The layout's terrain over {@link TestTerrain}: each chunk's height at its block (8, 8), water below Y 63. */
    public static TerrainView terrain() {
        return new TerrainView() {

            @Override
            public int height(int chunkX, int chunkZ) {
                return TestTerrain.surface((chunkX << 4) + 8, (chunkZ << 4) + 8) + 1;
            }

            @Override
            public boolean isWaterBiome(int chunkX, int chunkZ) {
                return height(chunkX, chunkZ) < 63;
            }

            @Override
            public boolean isOcean(int chunkX, int chunkZ) {
                return false;
            }
        };
    }

    /** A plains biome everywhere. */
    public static BiomeSource plains() {
        BiomeDescriptor plains = BiomeDescriptor.of(new HashSet<>(Arrays.asList("PLAINS")), "Plains", null);
        return (x, z) -> plains;
    }

    /**
     * Returns the real plan source of a test world.
     *
     * @param seed   the world seed
     * @param oracle the structure oracle
     * @param log    where problems go
     * @return the plan source
     */
    public static LayoutCityPlan plan(long seed, StructureOracle oracle, LayoutLog log) {
        TerrainSettings settings = TerrainSettings.defaults();
        WorldStyle worldStyle = ASSETS.worldStyle(CityProfiles.WORLD_STYLE);
        BiomeSource biomes = plains();
        TerrainView terrain = terrain();
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    CityProfiles.layoutProfile(settings),
                    new LayoutAssets(ASSETS, worldStyle, biomes),
                    terrain)
                .structures(oracle)
                .log(log)
                .build());
        CityWriteContext context = new CityWriteContext(
            seed,
            planner,
            ASSETS,
            worldStyle,
            new CityPalettes(ASSETS, MetalFlags.ALL_ON),
            settings,
            biomes,
            new NameBlockClassifier(),
            log);
        return new LayoutCityPlan(context, oracle, terrain);
    }

    /**
     * Returns the layout of a test world pinned with a profile: its terrain settings and its layout profile both from
     * the profile's values, as {@code OverlayWorld} builds them.
     *
     * @param seed   the world seed
     * @param values the profile's values
     * @return the planner
     */
    public static LayoutPlanner planner(long seed, com.tuxprogrammer.lostcities.config.profile.ProfileValues values) {
        TerrainSettings settings = TerrainSettings.fromValues(values.asMap());
        WorldStyle worldStyle = ASSETS.worldStyle(CityProfiles.worldStyle(values));
        return new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    CityProfiles.layoutProfile(settings, values),
                    new LayoutAssets(ASSETS, worldStyle, plains()),
                    terrain())
                .build());
    }

    /** Returns the real plan source of the default test world. */
    public static LayoutCityPlan plan() {
        return plan(SEED, StructureOracle.NONE, LayoutLog.SILENT);
    }

    /**
     * Generates one chunk: the stand-in RWG terrain, then the overlay's terrain phase with the real plan.
     *
     * @param plan   the plan source
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the chunk
     */
    public static ArrayChunkBuffer generate(LayoutCityPlan plan, int chunkX, int chunkZ) {
        ArrayChunkBuffer buffer = TestTerrain.chunk(chunkX, chunkZ);
        new TerrainPhase(
            TestTerrain.context(
                plan.context()
                    .worldSeed()),
            new CachedCityPlan(plan)).run(buffer, chunkX, chunkZ);
        return buffer;
    }

    /**
     * Visits every chunk of a square around the origin.
     *
     * @param radius half the side, in chunks
     * @param visit  called with {x, z}
     */
    public static void square(int radius, Consumer<int[]> visit) {
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                visit.accept(new int[] { x, z });
            }
        }
    }

    /** Returns the first chunk of a square that passes a test, or {@code null}. */
    public static int[] first(int radius, java.util.function.BiPredicate<Integer, Integer> test) {
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                if (test.test(x, z)) {
                    return new int[] { x, z };
                }
            }
        }
        return null;
    }

    /** Returns a list as a readable string, for messages. */
    static String list(List<?> items) {
        return items.toString();
    }
}
