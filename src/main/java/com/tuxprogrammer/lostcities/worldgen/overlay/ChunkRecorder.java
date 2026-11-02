package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;

/**
 * Records chunk hashes for the determinism scenario (CONTRIBUTING § World generation rules): the SHA-256 of a chunk's
 * {@link ChunkBufferDigest} right after the terrain phase, and of what the population phase made (the block at each of
 * the plan's job sites) right after it. Never the whole chunk after population: {@code IWorldGenerator}s of equal
 * weight run in an order that can change between JVM runs, GT places queued ore veins during later chunks' population
 * (docs/design/0005-rwg-overlay.md, "Determinism tests"), and RWG's own decoration reacts to what those placed in
 * neighbors, so even the writes the guard drops differ from run to run (D80).
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
    private final Map<Long, Integer> footprintFoliage = new TreeMap<>();
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
    void afterTerrain(int chunkX, int chunkZ, ChunkBuffer chunk, TerrainPhase.ChunkClass chunkClass, ChunkPlan plan) {
        if (covers(chunkX, chunkZ)) {
            terrain.put(key(chunkX, chunkZ), ChunkBufferDigest.sha256(chunk));
            classes.put(key(chunkX, chunkZ), chunkClass);
            if (plan != null) {
                footprintFoliage.put(key(chunkX, chunkZ), foliageInFootprint(chunk, plan));
            }
        }
    }

    /**
     * Returns the logs and leaves the city itself put in each recorded chunk's footprint above its ground (vegetation
     * along buildings, ruins, rubble), counted right after the terrain phase, so a scenario can tell them from what
     * decoration added later.
     */
    public Map<Long, Integer> footprintFoliageAfterTerrain() {
        return footprintFoliage;
    }

    /**
     * Counts the vanilla-named logs and leaves in a chunk's footprint above its ground.
     *
     * @param chunk the chunk
     * @param plan  its plan
     * @return the count; 0 for a chunk that is not city
     */
    static int foliageInFootprint(ChunkBuffer chunk, ChunkPlan plan) {
        if (!plan.isCity()) {
            return 0;
        }
        int found = 0;
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
                        found++;
                    }
                }
            }
        }
        return found;
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
        StringBuilder sites = new StringBuilder();
        for (PopulationJob job : plan.jobs()) {
            sites.append(job)
                .append('=')
                .append(chunk.get(job.x(), job.y(), job.z()))
                .append('\n');
        }
        population.put(key(chunkX, chunkZ), ChunkBufferDigest.sha256(sites.toString()));
        Integer own = footprintFoliage.get(key(chunkX, chunkZ));
        foliageAfterPopulation += Math.max(0, foliageInFootprint(chunk, plan) - (own == null ? 0 : own));
    }

    /**
     * Returns the vanilla-named logs and leaves found in city footprints right after our population phase beyond those
     * the city's own terrain phase put there: RWG's decoration is done by then, so anything here passed the write
     * guard, and anything the scenario finds later came from an {@code IWorldGenerator}.
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
}
