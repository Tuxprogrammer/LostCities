package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.ScatteredPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * A test world with scattered structures: the bundled assets and world style, plains everywhere, the
 * {@link TestTerrain} ground with its per-column oracle, and the default profile (with cities), as
 * {@code OverlayWorld} builds the real plan source.
 */
final class ScatteredWorld {

    final LayoutCityPlan plan;
    final ScatteredPlanner scattered;
    final CachedCityPlan cached;
    final TerrainPhase phase;

    ScatteredWorld(long seed) {
        TerrainSettings settings = TerrainSettings.defaults();
        WorldStyle worldStyle = CityFixtures.ASSETS.worldStyle(CityProfiles.WORLD_STYLE);
        BiomeSource biomes = CityFixtures.plains();
        TerrainView base = CityFixtures.terrain();
        TerrainView terrain = new TerrainView() {

            @Override
            public int height(int chunkX, int chunkZ) {
                return base.height(chunkX, chunkZ);
            }

            @Override
            public int columnHeight(int blockX, int blockZ) {
                return TestTerrain.surface(blockX, blockZ) + 1;
            }

            @Override
            public boolean isWaterBiome(int chunkX, int chunkZ) {
                return base.isWaterBiome(chunkX, chunkZ);
            }

            @Override
            public boolean isOcean(int chunkX, int chunkZ) {
                return false;
            }
        };
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    CityProfiles.layoutProfile(settings),
                    new LayoutAssets(CityFixtures.ASSETS, worldStyle, biomes),
                    terrain)
                .build());
        CityWriteContext context = new CityWriteContext(
            seed,
            planner,
            CityFixtures.ASSETS,
            worldStyle,
            new CityPalettes(CityFixtures.ASSETS, MetalFlags.ALL_ON),
            settings,
            biomes,
            new NameBlockClassifier(),
            LayoutLog.SILENT);
        scattered = new ScatteredPlanner(planner, 1f, 4096);
        plan = new LayoutCityPlan(context, StructureOracle.NONE, terrain, scattered, StuffWriter.NO_ORES);
        cached = new CachedCityPlan(plan);
        phase = new TerrainPhase(TestTerrain.context(seed), cached);
    }

    /** Generates one chunk: the stand-in RWG terrain, then the terrain phase. */
    ArrayChunkBuffer generate(int chunkX, int chunkZ) {
        ArrayChunkBuffer buffer = TestTerrain.chunk(chunkX, chunkZ);
        phase.run(buffer, chunkX, chunkZ);
        return buffer;
    }

    /** Returns the nearest structure of an asset to the origin, searching areas outward. */
    ScatteredPlan nearest(String name) {
        ScatteredFinder.Result found = new ScatteredFinder(scattered).find(name, 0, 0, 256);
        return found == null ? null : found.plan();
    }
}
