package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.Random;

import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;

/**
 * What a {@link ChunkContent} writer may read: the chunk's coordinates and plan, its neighbors' plans, the world's
 * terrain context and a random seeded for this chunk alone (from the world seed and the chunk coordinates, TERR-27).
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class ContentContext {

    private final int chunkX;
    private final int chunkZ;
    private final ChunkPlan plan;
    private final CityPlan cityPlan;
    private final TerrainContext terrain;
    private final Random random;
    private final boolean[] softEdge;

    /**
     * Creates the context of one chunk.
     *
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param plan     the chunk's plan
     * @param cityPlan the plan source, for neighbors
     * @param terrain  the world's terrain context
     * @param random   the chunk's own random
     * @param softEdge 256 flags, index {@code z × 16 + x}: columns in a soft-edge strip toward a landmark core, which
     *                 the content must leave alone; {@code null} for none
     */
    public ContentContext(int chunkX, int chunkZ, ChunkPlan plan, CityPlan cityPlan, TerrainContext terrain,
        Random random, boolean[] softEdge) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.plan = plan;
        this.cityPlan = cityPlan;
        this.terrain = terrain;
        this.random = random;
        this.softEdge = softEdge;
    }

    /**
     * Returns whether a column belongs to a soft-edge strip, leveled as a ramp toward a landmark core; content must
     * not write there (0006 §4, rules E2 to E4).
     *
     * @param x local X
     * @param z local Z
     * @return {@code true} for a strip column
     */
    public boolean isSoftEdge(int x, int z) {
        return softEdge != null && softEdge[(z << 4) | x];
    }

    /** Returns chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns the chunk's plan. */
    public ChunkPlan plan() {
        return plan;
    }

    /**
     * Returns a neighbor's plan.
     *
     * @param dx chunk offset along X
     * @param dz chunk offset along Z
     * @return the plan of chunk ({@code chunkX + dx}, {@code chunkZ + dz})
     */
    public ChunkPlan neighbor(int dx, int dz) {
        return cityPlan.chunk(chunkX + dx, chunkZ + dz);
    }

    /** Returns the world's terrain context. */
    public TerrainContext terrain() {
        return terrain;
    }

    /** Returns the chunk's own random. */
    public Random random() {
        return random;
    }
}
