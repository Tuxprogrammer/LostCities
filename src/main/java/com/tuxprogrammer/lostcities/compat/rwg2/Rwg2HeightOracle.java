package com.tuxprogrammer.lostcities.compat.rwg2;

import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;
import com.tuxprogrammer.lostcities.worldgen.timing.PerfCounters;

import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.world.ChunkGeneratorRealistic;
import rwg2.world.ChunkManagerRealistic;

/**
 * The height oracle on RWG2 and RWG2 Continents worlds: a private, world-less {@code ChunkGeneratorRealistic(manager,
 * seed, continental)} over the world's own {@code ChunkManagerRealistic}, asked for heights with
 * {@code getNewNoise(…, stride 8)} (docs/design/0005-rwg-overlay.md §4).
 *
 * <ul>
 * <li>The world-less constructor builds no caves, structures or decorations ({@code mapFeatures = false}) and fires no
 * {@code InitMapGenEvent}. Source:
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L136-L138</li>
 * <li>{@code getNewNoise} with stride 8 evaluates every eighth column; its Javadoc says the sampled columns match
 * dense generation exactly, and column (8, 8) is one of them. Source: #L310-L319 of the same file.</li>
 * <li>Both RWG versions fill stone for {@code y ≤ (int) noise}, so the height is {@code (int) noise + 1}. Source:
 * #L282-L304.</li>
 * <li>The oracle is never the world's live generator: {@code getNewNoise} records center-chunk biome weights in
 * {@code mapGenBiomes} that the next {@code provideChunk} consumes (#L480-L500, #L217-L232).</li>
 * <li>The noise implementation is a per-world choice RWG2 reads from its saved data when a generator is built
 * (rwg2/util/NoiseSelector.java), so this instance is built after the world's own generator exists: the overlay is
 * installed at {@code WorldEvent.CreateSpawnPosition} or {@code Load}, after {@code createChunkProvider}.</li>
 * </ul>
 *
 * <p>
 * Load this class only when {@code Loader.isModLoaded("rwg2")}. One instance computes noise on one thread (its sampler
 * is stateful); its cache is safe for concurrent
 * callers. Side: server.
 */
public final class Rwg2HeightOracle implements TerrainHeightOracle {

    /** Columns between the sampled columns of {@code getNewNoise}; 8 samples (0, 0), (8, 0), (0, 8), (8, 8). */
    static final int STRIDE = 8;

    private final ChunkGeneratorRealistic sampler;
    private final ChunkManagerRealistic manager;
    private final RealisticBiomeBase[] biomes = new RealisticBiomeBase[256];
    /** Chunks of noise kept. */
    public static final int CACHE_CHUNKS = 4096;
    /** Most bytes the noise cache can hold, estimated, for budget B5. */
    public static final long CACHE_BYTES = CACHE_CHUNKS * ChunkNoiseCache.ENTRY_BYTES;

    /** Recently asked chunks' stride samples; the map is locked, the noise computation is not. */
    private final ChunkNoiseCache cache = new ChunkNoiseCache(CACHE_CHUNKS, this::sampleChunk);

    private Rwg2HeightOracle(ChunkGeneratorRealistic sampler, ChunkManagerRealistic manager) {
        this.sampler = sampler;
        this.manager = manager;
    }

    /**
     * Builds the oracle of a world.
     *
     * @param world       the world, whose chunk manager must be RWG2's
     * @param continental whether the world type is RWG2 Continents
     * @return the oracle, or {@code null} when the world's chunk manager is not RWG2's
     */
    public static Rwg2HeightOracle create(World world, boolean continental) {
        WorldChunkManager chunkManager = world.getWorldChunkManager();
        if (!(chunkManager instanceof ChunkManagerRealistic)) {
            return null;
        }
        ChunkManagerRealistic manager = (ChunkManagerRealistic) chunkManager;
        return new Rwg2HeightOracle(new ChunkGeneratorRealistic(manager, world.getSeed(), continental), manager);
    }

    /**
     * Builds a plan worker's own oracle over the worker's own copy of the world's manager (D-plan-worker-2): the same
     * sampler {@link #create} builds, so the heights are the same.
     *
     * @param manager     the worker's manager, {@code new ChunkManagerRealistic(seed, continental)}
     * @param seed        the world's seed
     * @param continental whether the world type is RWG2 Continents
     * @return the oracle
     */
    public static Rwg2HeightOracle forWorker(ChunkManagerRealistic manager, long seed, boolean continental) {
        return new Rwg2HeightOracle(new ChunkGeneratorRealistic(manager, seed, continental), manager);
    }

    @Override
    public int height(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        float[] samples = cache.get(chunkX, chunkZ);
        int localX = blockX & 15;
        int localZ = blockZ & 15;
        // RWG's arrays are x-major: generateTerrain writes column (x, z) from noise[x * 16 + z]; the cache keeps the
        // four stride samples, slot (localX / 8) * 2 + (localZ / 8).
        return (int) samples[(localX >> 3) * 2 + (localZ >> 3)] + 1;
    }

    /**
     * Computes a chunk's four stride samples: {@code getNewNoise} evaluates them whatever column is asked, so a run of
     * columns of one chunk costs one evaluation. The sampler and the {@code biomes} scratch belong to this instance,
     * so one instance's computation runs on one thread; the cache around it is what is shared.
     */
    private float[] sampleChunk(int chunkX, int chunkZ) {
        long perfNoise = PerfCounters.start();
        float[] noise = sampler.getNewNoise(manager, chunkX << 4, chunkZ << 4, biomes, STRIDE);
        PerfCounters.add("ho.noise", perfNoise);
        // x-major: column (x, z) is noise[x * 16 + z]; the samples sit at x, z in {0, 8}.
        return new float[] { noise[0], noise[8], noise[8 * 16], noise[8 * 16 + 8] };
    }
}
