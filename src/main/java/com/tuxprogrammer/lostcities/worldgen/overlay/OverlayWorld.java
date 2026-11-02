package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
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
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
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
 * heights, the cached plan, the terrain and population phases, the timers and the scenario recorder. Built when the
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
    private final ChunkHeights heights;
    private final CachedCityPlan plan;
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
        this.heights = new ChunkHeights(oracle, settings.heightSampleSize(), timers::recordOracle);
        this.biomes = new WorldBiomes(world);
        this.cities = layoutPlan(world, snapshot, settings, heights, biomes, classifier);
        this.plan = new CachedCityPlan(planSource(world.getSeed(), snapshot, settings, heights, cities));
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
        ChunkHeights heights, WorldBiomes biomes, RegistryBlockClassifier classifier) {
        if (!ProfileSnapshot.PLAN_LAYOUT.equals(snapshot.planSource())) {
            return null;
        }
        if (!LoadedAssets.isLoaded()) {
            LostCities.LOG.error("Assets are not loaded; dimension {} gets no cities", world.provider.dimensionId);
            return null;
        }
        AssetSet assets = LoadedAssets.current();
        WorldStyle worldStyle = assets.worldStyle(CityProfiles.WORLD_STYLE);
        if (worldStyle == null) {
            LostCities.LOG.error(
                "World style {} is not loaded; dimension {} gets no cities",
                CityProfiles.WORLD_STYLE,
                world.provider.dimensionId);
            return null;
        }
        WorldTerrainView terrain = new WorldTerrainView(world, heights);
        StructureOracle structures = StructureOracles.forWorld(world);
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    world.getSeed(),
                    CityProfiles.layoutProfile(settings),
                    new LayoutAssets(assets, worldStyle, biomes),
                    terrain)
                .dimension(DimensionKey.forDimension(world.provider.dimensionId))
                .structures(structures)
                .log(message -> LostCities.LOG.warn("[layout] {}", message))
                .cacheSize(LayoutCityPlan.LAYOUT_CACHE_SIZE)
                .build());
        CityWriteContext context = new CityWriteContext(
            world.getSeed(),
            planner,
            assets,
            worldStyle,
            new CityPalettes(assets, snapshot.metalFlags()),
            settings,
            biomes,
            classifier,
            message -> LostCities.LOG.warn("[cities] {}", message));
        return new LayoutCityPlan(context, structures, terrain);
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

    /**
     * Records a chunk for the determinism scenario after its terrain phase, outside the timed span.
     *
     * @param chunk      the chunk
     * @param chunkClass how the terrain phase treated it
     */
    void recordTerrain(Chunk chunk, TerrainPhase.ChunkClass chunkClass) {
        if (recorder.covers(chunk.xPosition, chunk.zPosition)) {
            recorder.afterTerrain(chunk.xPosition, chunk.zPosition, buffer.attach(chunk), chunkClass);
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
     * +8 offset spreads decoration over) touches a city chunk: then lakes, lava, dungeons and oil are vetoed (0005 §3).
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
                if (plan.chunk(chunkX + dx, chunkZ + dz)
                    .isCity()) {
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

    /** Returns the timers. */
    public GenerationTimers timers() {
        return timers;
    }

    /** Returns the scenario recorder. */
    public ChunkRecorder recorder() {
        return recorder;
    }

    /** Returns the layout's terrain view over this world (D51), for M05's {@code LayoutPlanner}. */
    public WorldTerrainView terrainView() {
        return new WorldTerrainView(world, heights);
    }

    /** Returns the chunk heights. */
    public ChunkHeights heights() {
        return heights;
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
        return plan.estimatedMaxBytes() + ChunkHeights.MAX_BYTES + (cities == null ? 0 : cities.estimatedCacheBytes());
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
