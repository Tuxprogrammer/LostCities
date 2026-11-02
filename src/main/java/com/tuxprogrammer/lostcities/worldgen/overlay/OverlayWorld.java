package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.core.Module;
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
        this.plan = new CachedCityPlan(planSource(world.getSeed(), snapshot, settings, heights));
        this.terrainPhase = new TerrainPhase(context, plan);
        this.populationPhase = new PopulationPhase(world, refs);
        this.buffer = new WorldChunkBuffer(refs);
    }

    private static CityPlan planSource(long seed, ProfileSnapshot snapshot, TerrainSettings settings,
        ChunkHeights heights) {
        if (!snapshot.moduleState()
            .isActive(Module.CITIES)) {
            return CityPlan.EMPTY;
        }
        if (ProfileSnapshot.PLAN_SYNTHETIC.equals(snapshot.planSource())) {
            return new SyntheticCityPlan(seed, settings, heights);
        }
        // M05 joins M03's layout here; until then the layout plans no cities.
        return CityPlan.EMPTY;
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
            recorder.afterTerrain(chunk.xPosition, chunk.zPosition, buffer, result.chunkClass());
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
            if (recorder.covers(chunkX, chunkZ)) {
                recorder.afterPopulation(
                    chunkX,
                    chunkZ,
                    buffer.attach(world.getChunkFromChunkCoords(chunkX, chunkZ)),
                    chunkPlan);
            }
        } catch (RuntimeException e) {
            timers.countFailure();
            LostCities.LOG
                .error("Population phase failed for chunk ({}, {}) of dimension {}", chunkX, chunkZ, dimension, e);
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

    /** Returns the timers. */
    public GenerationTimers timers() {
        return timers;
    }

    /** Returns the scenario recorder. */
    public ChunkRecorder recorder() {
        return recorder;
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
        return plan.estimatedMaxBytes() + ChunkHeights.MAX_BYTES;
    }

    /** Frees the caches; the world is unloading. */
    void close() {
        plan.clear();
        heights.clear();
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
