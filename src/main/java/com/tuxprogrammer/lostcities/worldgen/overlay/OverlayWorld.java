package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.core.Module;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.worldgen.city.CityPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.city.CityWriteContext;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkColumnCache;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.height.ColumnHeightCache;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.synthetic.SyntheticCityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * Everything the overlay holds for one wrapped world: the pinned snapshot and settings, the height oracle and chunk
 * heights with their probe, the cached plan and its prefetcher, the terrain and population phases, the timers and the
 * scenario recorder. Built when the
 * wrapper is installed and dropped on {@code WorldEvent.Unload}, so every cache is owned by the world (0005 §8, B5).
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class OverlayWorld {

    private final WorldServer world;
    private final int dimension;
    private final ProfileSnapshot snapshot;
    private final TerrainSettings settings;
    private final GenerationTimers timers = new GenerationTimers();
    private final HeightProbe probe = new HeightProbe();
    private final ChunkHeights heights;
    private final TerrainHeightOracle columns;
    private final CachedCityPlan plan;
    private final PlanPrefetcher prefetcher;
    private final TerrainPhase terrainPhase;
    private final PopulationPhase populationPhase;
    private final BlockRefs refs = new BlockRefs();
    private final WorldChunkBuffer buffer;
    private final ChunkRecorder recorder = new ChunkRecorder();
    private final WorldBiomes biomes;
    private final LayoutCityPlan cities;
    private final int chunksBeforeInstall;
    private long cascades;

    OverlayWorld(WorldServer world, ProfileSnapshot snapshot, TerrainHeightOracle oracle, int chunksBeforeInstall) {
        this.world = world;
        this.dimension = world.provider.dimensionId;
        this.snapshot = snapshot;
        this.settings = snapshot.terrainSettings();
        for (String warning : settings.warnings()) {
            LostCities.LOG.warn("Profile {} of dimension {}: {}", snapshot.profile(), dimension, warning);
        }
        this.chunksBeforeInstall = chunksBeforeInstall;
        RegistryBlockClassifier classifier = new RegistryBlockClassifier(refs);
        TerrainContext context = new TerrainContext(
            world.getSeed(),
            dimension,
            settings,
            classifier,
            BlockRefs::exists);
        TerrainHeightOracle columns = ChunkColumnCache.wrap(oracle, probe);
        this.columns = columns;
        this.heights = new ChunkHeights(columns, settings.heightSampleSize(), timers::recordOracle, probe);
        this.biomes = new WorldBiomes(world);
        this.cities = layoutPlan(world, snapshot, settings, columns, heights, biomes, classifier, timers, probe);
        this.plan = new CachedCityPlan(planSource(world.getSeed(), snapshot, settings, heights, cities));
        this.prefetcher = new PlanPrefetcher(
            plan,
            probe,
            timers,
            (x, z) -> world.theChunkProviderServer != null && world.theChunkProviderServer.chunkExists(x, z));
        this.terrainPhase = new TerrainPhase(context, plan);
        this.populationPhase = new PopulationPhase(world, refs);
        this.buffer = new WorldChunkBuffer(refs);
    }

    private static CityPlan planSource(long seed, ProfileSnapshot snapshot, TerrainSettings settings,
        ChunkHeights heights, LayoutCityPlan cities) {
        if (!snapshot.moduleState()
            .isActive(Module.CITIES)) {
            return CityPlan.EMPTY;
        }
        if (ProfileSnapshot.PLAN_SYNTHETIC.equals(snapshot.planSource())) {
            return new SyntheticCityPlan(seed, settings, heights);
        }
        return cities == null ? CityPlan.EMPTY : cities;
    }

    /**
     * Builds the real plan source of a world (M05): the layout planner over the loaded assets, the profile's world
     * style, the height oracle, the world's biomes and the structure oracles, and the city writer over it. Returns
     * {@code null} for a world pinned with the synthetic source, or when the assets or the world style are missing,
     * which leaves the world without cities and logs why.
     */
    private static LayoutCityPlan layoutPlan(WorldServer world, ProfileSnapshot snapshot, TerrainSettings settings,
        TerrainHeightOracle oracle, ChunkHeights heights, WorldBiomes biomes, RegistryBlockClassifier classifier,
        GenerationTimers timers, HeightProbe probe) {
        if (!ProfileSnapshot.PLAN_LAYOUT.equals(snapshot.planSource())) {
            return null;
        }
        if (!LoadedAssets.isLoaded()) {
            LostCities.LOG.error("Assets are not loaded; dimension {} gets no cities", world.provider.dimensionId);
            return null;
        }
        AssetSet assets = LoadedAssets.current();
        ProfileValues values = snapshot.profileValues();
        AssetId worldStyleId = CityProfiles.worldStyle(values);
        WorldStyle worldStyle = assets.worldStyle(worldStyleId);
        if (worldStyle == null) {
            // Avoids PROF-13: a missing world style leaves the dimension without cities, never a crash.
            LostCities.LOG.error(
                "World style {} is not loaded; dimension {} gets no cities",
                worldStyleId,
                world.provider.dimensionId);
            return null;
        }
        WorldTerrainView terrain = new WorldTerrainView(world, heights);
        StructureOracle structures = timed(StructureOracles.forWorld(world, oracle, settings.landmarkMargin()), timers);
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    world.getSeed(),
                    CityProfiles.layoutProfile(settings, values),
                    new LayoutAssets(assets, worldStyle, biomes),
                    terrain)
                .dimension(DimensionKey.forDimension(world.provider.dimensionId))
                .structures(structures)
                .networks(CityProfiles.networkSettings(values, snapshot.moduleState(), new NetworkGate(probe)))
                .log(message -> LostCities.LOG.warn("[layout] {}", message))
                .cacheSize(LayoutCityPlan.LAYOUT_CACHE_SIZE)
                .build());
        CityWriteContext context = new CityWriteContext(
            world.getSeed(),
            planner,
            assets,
            worldStyle,
            new CityPalettes(
                assets,
                snapshot.metalFlags(),
                profileBlock(values, "baseBlock", world.provider.dimensionId),
                profileBlock(values, "liquidBlock", world.provider.dimensionId)),
            settings,
            biomes,
            classifier,
            message -> LostCities.LOG.warn("[cities] {}", message));
        return new LayoutCityPlan(context, structures, terrain);
    }

    /**
     * Resolves a profile's block option ({@code baseBlock} or {@code liquidBlock}) through the block-name map. An
     * unknown name logs once and falls back to upstream's stone or water, as upstream does
     * (docs/new-spec/02-profiles-dimensions/profile-options.md).
     *
     * @return the block, or {@code null} for the fallback
     */
    private static BlockRef profileBlock(ProfileValues values, String key, int dimension) {
        String name = values.getString(key);
        if (name.equals(
            ProfileValues.defaults()
                .getString(key))) {
            // Upstream's stone and water, which the style blocks hold already.
            return null;
        }
        java.util.List<String> problems = new java.util.ArrayList<>();
        BlockRef block = LoadedAssets.resolveBlock(name, problems);
        if (block == null) {
            LostCities.LOG.error(
                "Profile option {}='{}' of dimension {} names no block ({}); using the default",
                key,
                name,
                dimension,
                String.join("; ", problems));
        }
        return block;
    }

    /** Wraps a structure oracle so each call is timed (the {@code structures} series of the timers). */
    private static StructureOracle timed(StructureOracle oracle, GenerationTimers timers) {
        if (oracle == StructureOracle.NONE) {
            return oracle;
        }
        return (minX, minZ, maxX, maxZ) -> {
            long start = System.nanoTime();
            try {
                return oracle.landmarksTouching(minX, minZ, maxX, maxZ);
            } finally {
                timers.recordStructures(System.nanoTime() - start);
            }
        };
    }

    /**
     * Runs the terrain phase on a chunk the delegate just generated. A failure is logged once with the chunk's
     * coordinates and counted; the chunk keeps whatever was written before it (TERR-25 is avoided as far as the
     * plan goes: every pure input is read before the first write).
     *
     * @param chunk the delegate's chunk
     * @return how the chunk was treated
     */
    TerrainPhase.ChunkClass terrain(Chunk chunk) {
        if (FootprintGuard.populating()) {
            // A chunk generated while a population runs is a cascade (ArchaicFix logs it too, with a stack trace
            // when asked); the overlay never causes one itself, so count it for the scenario.
            cascades++;
        }
        buffer.attach(chunk);
        try {
            TerrainPhase.Result result = terrainPhase.run(buffer, chunk.xPosition, chunk.zPosition);
            buffer.finish();
            return result.chunkClass();
        } catch (RuntimeException e) {
            timers.countFailure();
            LostCities.LOG.error(
                "Terrain phase failed for chunk ({}, {}) of dimension {}",
                chunk.xPosition,
                chunk.zPosition,
                dimension,
                e);
            buffer.finish();
            return TerrainPhase.ChunkClass.UNTOUCHED;
        }
    }

    /**
     * Runs the population phase of a chunk the delegate just populated.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    void population(int chunkX, int chunkZ) {
        ChunkPlan chunkPlan = plan.chunk(chunkX, chunkZ);
        try {
            populationPhase.run(chunkX, chunkZ, chunkPlan);
        } catch (RuntimeException e) {
            timers.countFailure();
            LostCities.LOG
                .error("Population phase failed for chunk ({}, {}) of dimension {}", chunkX, chunkZ, dimension, e);
        }
    }

    /** Returns whether the last terrain phase wrote to its chunk, whose height map and sky light are then stale. */
    boolean terrainNeedsLight() {
        return buffer.needsLight();
    }

    /**
     * Records a chunk for the determinism scenario after its terrain phase, outside the timed span.
     *
     * @param chunk      the chunk
     * @param chunkClass how the terrain phase treated it
     */
    void recordTerrain(Chunk chunk, TerrainPhase.ChunkClass chunkClass) {
        if (recorder.covers(chunk.xPosition, chunk.zPosition)) {
            recorder.afterTerrain(
                chunk.xPosition,
                chunk.zPosition,
                buffer.attach(chunk),
                chunkClass,
                plan.chunk(chunk.xPosition, chunk.zPosition));
        }
    }

    /**
     * Records a chunk for the determinism scenario after its population phase, outside the timed span.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    void recordPopulation(int chunkX, int chunkZ) {
        if (recorder.covers(chunkX, chunkZ)) {
            recorder.afterPopulation(
                chunkX,
                chunkZ,
                buffer.attach(world.getChunkFromChunkCoords(chunkX, chunkZ)),
                plan.chunk(chunkX, chunkZ));
        }
    }

    /**
     * Returns how a chunk's population is classed for the timers: city, near a city or core (its population area
     * touches one), or untouched.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the class
     */
    TerrainPhase.ChunkClass populationClass(int chunkX, int chunkZ) {
        if (plan.chunk(chunkX, chunkZ)
            .isCity()) {
            return TerrainPhase.ChunkClass.CITY;
        }
        for (int dz = 0; dz <= 1; dz++) {
            for (int dx = 0; dx <= 1; dx++) {
                if (plan.chunk(chunkX + dx, chunkZ + dz)
                    .kind() != ChunkKind.NONE) {
                    return TerrainPhase.ChunkClass.BLEND;
                }
            }
        }
        return TerrainPhase.ChunkClass.UNTOUCHED;
    }

    /**
     * Returns whether the population area of a chunk (the chunk and its +X, +Z and +X+Z neighbors, which the vanilla
     * +8 offset spreads decoration over) touches a city chunk or a highway: then lakes, lava, dungeons and oil are
     * vetoed (0005 §3, M06).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} to veto
     */
    public boolean populationAreaTouchesCity(int chunkX, int chunkZ) {
        if (!settings.preventLakesRavinesInCities()) {
            return false;
        }
        for (int dz = 0; dz <= 1; dz++) {
            for (int dx = 0; dx <= 1; dx++) {
                ChunkPlan area = plan.chunk(chunkX + dx, chunkZ + dz);
                // M06: no lake or pit eats a highway either (alpha review AR-10).
                if (area.isCity() || area.hasHighway()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns the world. */
    public WorldServer world() {
        return world;
    }

    /** Returns the dimension ID. */
    public int dimension() {
        return dimension;
    }

    /** Returns the pinned snapshot. */
    public ProfileSnapshot snapshot() {
        return snapshot;
    }

    /** Returns the pinned terrain settings. */
    public TerrainSettings settings() {
        return settings;
    }

    /** Returns the cached plan. */
    public CityPlan plan() {
        return plan;
    }

    /**
     * Returns the real plan source (layout and assets), for the finder and the city scenarios; {@code null} for a
     * world pinned with the synthetic test city.
     */
    public LayoutCityPlan cities() {
        return cities;
    }

    /**
     * Returns whether explosion damage reaches a chunk, for the walkability scenario's exemption of upstream's own
     * damage.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the chunk's damage area holds an explosion
     */
    public boolean damaged(int chunkX, int chunkZ) {
        return terrainPhase.damageArea(chunkX, chunkZ)
            .hasExplosions();
    }

    /** Returns the timers. */
    public GenerationTimers timers() {
        return timers;
    }

    /** Returns the scenario recorder. */
    public ChunkRecorder recorder() {
        return recorder;
    }

    /**
     * Returns the layout's terrain view over this world (D51), as the real plan source's {@code LayoutPlanner} has it.
     */
    public WorldTerrainView terrainView() {
        return new WorldTerrainView(world, heights);
    }

    /** Returns the per-column height oracle, behind the world's column cache. */
    public TerrainHeightOracle columns() {
        return columns;
    }

    /** Returns the chunk heights. */
    public ChunkHeights heights() {
        return heights;
    }

    /** Returns how many plans the plan cache has computed (its misses), for the budget report. */
    public long planMisses() {
        return plan.misses();
    }

    /** Returns the plan prefetcher. */
    public PlanPrefetcher prefetcher() {
        return prefetcher;
    }

    /** Returns how many chunks were generated while a population ran (cascades). */
    public long cascades() {
        return cascades;
    }

    /** Returns how many chunks the world had loaded when the wrapper was installed; 0 on a new world. */
    public int chunksBeforeInstall() {
        return chunksBeforeInstall;
    }

    /** Returns the most memory the world's caches can hold, estimated, for budget B5. */
    public long cacheBytes() {
        // The column cache is a ChunkColumnCache on classic RWG and a smaller ColumnHeightCache on RWG2: the larger.
        return plan.estimatedMaxBytes() + ChunkHeights.MAX_BYTES
            + Math.max(ChunkColumnCache.MAX_BYTES, ColumnHeightCache.MAX_BYTES)
            + (cities == null ? 0 : cities.estimatedCacheBytes());
    }

    /** Frees the caches; the world is unloading. */
    void close() {
        plan.clear();
        heights.clear();
        biomes.clear();
        if (cities != null) {
            cities.context()
                .palettes()
                .clear();
        }
        recorder.stop();
    }

    /** Returns the wrapped provider's delegate's description, for logs. */
    static String describe(IChunkProvider provider) {
        return provider.getClass()
            .getName() + " ("
            + provider.makeString()
            + ")";
    }
}
