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
    private long foliageAfterPopulation;

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
        foliageAfterPopulation = 0;
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

    /** Records a chunk's footprint volume after the population phase, and the logs and leaves left in it. */
    void afterPopulation(int chunkX, int chunkZ, ChunkBuffer chunk, ChunkPlan plan) {
        if (!covers(chunkX, chunkZ)) {
            return;
        }
        population.put(key(chunkX, chunkZ), ChunkBufferDigest.sha256(new FootprintView(chunk, plan)));
        if (!plan.isCity()) {
            return;
        }
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int bottom = plan.footprintBottom(x, z);
                if (bottom == ChunkPlan.NO_FOOTPRINT) {
                    continue;
                }
                for (int y = Math.max(bottom, plan.groundY() + 1); y < ChunkBuffer.HEIGHT; y++) {
                    String name = chunk.get(x, y, z)
                        .name();
                    if (name.endsWith(":log") || name.endsWith(":log2") || name.contains("leaves")) {
                        foliageAfterPopulation++;
                    }
                }
            }
        }
    }

    /**
     * Returns the vanilla-named logs and leaves found in city footprints right after our population phase: RWG's
     * decoration is done by then, so anything here passed the write guard, and anything the scenario finds later came
     * from an {@code IWorldGenerator}.
     */
    public long foliageAfterPopulation() {
        return foliageAfterPopulation;
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

    /**
     * A read-only view that shows a chunk's blocks inside its footprint from its city ground up, without logs and
     * leaves, and air elsewhere: the part of the chunk only RWG's guarded decoration and our phases decide.
     */
    private static final class FootprintView implements ChunkBuffer {

        private final ChunkBuffer chunk;
        private final ChunkPlan plan;

        FootprintView(ChunkBuffer chunk, ChunkPlan plan) {
            this.chunk = chunk;
            this.plan = plan;
        }

        @Override
        public BlockRef get(int x, int y, int z) {
            if (y < plan.groundY() || y < plan.footprintBottom(x, z)) {
                // Below the ground, GT's ore veins and other IWorldGenerators may already have written, in an
                // order that changes between JVM runs.
                return BlockRef.AIR;
            }
            BlockRef block = chunk.get(x, y, z);
            String name = block.name();
            // Trees other mods plant from IWorldGenerators on park grass may overhang a footprint (T3); which one
            // wins a spot depends on that same order, so they are left out of the hash.
            return name.endsWith(":log") || name.endsWith(":log2") || name.contains("eaves") ? BlockRef.AIR : block;
        }

        @Override
        public void set(int x, int y, int z, BlockRef block) {
            throw new UnsupportedOperationException("the footprint view is read-only");
        }
    }
}
