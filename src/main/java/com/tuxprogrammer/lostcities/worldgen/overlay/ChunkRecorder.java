package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;

/**
 * Records chunk hashes for the determinism scenario (CONTRIBUTING § World generation rules): the SHA-256 of a chunk's
 * {@link ChunkBufferDigest} right after the terrain phase, and of its footprint volume right after the population
 * phase, never after the whole of population, because {@code IWorldGenerator}s of equal weight run in an order that
 * can change between JVM runs and GT places queued ore veins during later chunks' population
 * (docs/design/0005-rwg-overlay.md, "Determinism tests").
 *
 * <p>
 * Recording is off until a scenario asks for a region; it only reads chunks. Server thread only. Side: server.
 */
public final class ChunkRecorder {

    private boolean enabled;
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private final Map<Long, String> terrain = new TreeMap<>();
    private final Map<Long, String> population = new TreeMap<>();
    private final Map<Long, TerrainPhase.ChunkClass> classes = new TreeMap<>();

    /**
     * Starts recording a rectangle of chunks, forgetting earlier records.
     *
     * @param fromX first chunk X
     * @param toX   last chunk X, inclusive
     * @param fromZ first chunk Z
     * @param toZ   last chunk Z, inclusive
     */
    public void record(int fromX, int toX, int fromZ, int toZ) {
        enabled = true;
        minX = fromX;
        maxX = toX;
        minZ = fromZ;
        maxZ = toZ;
        terrain.clear();
        population.clear();
        classes.clear();
    }

    /** Stops recording. */
    public void stop() {
        enabled = false;
    }

    /** Returns whether a chunk is in the recorded rectangle. */
    boolean covers(int chunkX, int chunkZ) {
        return enabled && chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    /** Records a chunk after the terrain phase, with how the phase treated it. */
    void afterTerrain(int chunkX, int chunkZ, ChunkBuffer chunk, TerrainPhase.ChunkClass chunkClass) {
        if (covers(chunkX, chunkZ)) {
            terrain.put(key(chunkX, chunkZ), ChunkBufferDigest.sha256(chunk));
            classes.put(key(chunkX, chunkZ), chunkClass);
        }
    }

    /** Returns how the terrain phase treated each recorded chunk. */
    public Map<Long, TerrainPhase.ChunkClass> chunkClasses() {
        return classes;
    }

    /** Records a chunk's footprint volume after the population phase. */
    void afterPopulation(int chunkX, int chunkZ, ChunkBuffer chunk, ChunkPlan plan) {
        if (covers(chunkX, chunkZ)) {
            population.put(key(chunkX, chunkZ), ChunkBufferDigest.sha256(new FootprintView(chunk, plan)));
        }
    }

    /** Returns the terrain-phase hashes, by chunk key in a fixed order. */
    public Map<Long, String> terrainHashes() {
        return terrain;
    }

    /** Returns the population-phase hashes, by chunk key in a fixed order. */
    public Map<Long, String> populationHashes() {
        return population;
    }

    /** Returns the key of a chunk: X in the high and Z in the low 32 bits. */
    public static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /** A read-only view that shows a chunk's blocks inside its footprint and air elsewhere. */
    private static final class FootprintView implements ChunkBuffer {

        private final ChunkBuffer chunk;
        private final ChunkPlan plan;

        FootprintView(ChunkBuffer chunk, ChunkPlan plan) {
            this.chunk = chunk;
            this.plan = plan;
        }

        @Override
        public BlockRef get(int x, int y, int z) {
            return y >= plan.footprintBottom(x, z) ? chunk.get(x, y, z) : BlockRef.AIR;
        }

        @Override
        public void set(int x, int y, int z, BlockRef block) {
            throw new UnsupportedOperationException("the footprint view is read-only");
        }
    }
}
