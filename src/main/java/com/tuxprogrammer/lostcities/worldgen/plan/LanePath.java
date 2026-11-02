package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.Objects;

/**
 * The columns of one chunk that a village lane crosses, and the village's material, so the terrain phase can lay the
 * lane on the ground with VillageNames' own per-column path rule (docs/design/0006-build-around-other-worldgen.md
 * §3, docs/new-spec/03-terrain/pipeline-on-rwg.md "Village lanes"). The plan decides where a lane runs (gates, the
 * edge ring, the core's ground away from the village's pieces); this class only carries the result.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class LanePath {

    /**
     * The village's biome material, which swaps the lane's blocks as VillageNames swaps its own paths
     * (StructureVillageVN.java#L945-L1003 at 4.5.21-GTNH).
     */
    public enum Material {
        /** Plains, taiga and the rest: gravel, cobblestone, planks and logs as they are. */
        PLAIN,
        /** Desert villages: gravel and cobblestone become sandstone. */
        SAND,
        /** Mesa villages: gravel and cobblestone become hardened clay. */
        MESA
    }

    private final long[] columns;
    private final Material material;
    private final ChunkPlan.Side gradeSide;
    private final int gradeFrom;

    private LanePath(long[] columns, Material material, ChunkPlan.Side gradeSide, int gradeFrom) {
        this.columns = columns;
        this.material = material;
        this.gradeSide = gradeSide;
        this.gradeFrom = gradeFrom;
    }

    /**
     * Returns this path graded across the city's margin (alpha review AR-7, AR-11): before the path is laid, its
     * columns are moved to a straight ramp from the city's ground on the city side to the core's own edge on the
     * opposite side, so the lane runs down (or up) the margin at no more than a block a column where it can.
     *
     * @param citySide   the chunk side the city lies on
     * @param cityGround the city chunk's ground there, block Y
     * @return the graded path
     */
    public LanePath gradedFrom(ChunkPlan.Side citySide, int cityGround) {
        return new LanePath(columns, material, Objects.requireNonNull(citySide), cityGround);
    }

    /** Returns the side the lane is graded from, or {@code null} when it is laid on the ground as it is. */
    public ChunkPlan.Side gradeSide() {
        return gradeSide;
    }

    /** Returns the city ground the grade starts at, block Y; meaningful only with a {@link #gradeSide()}. */
    public int gradeFrom() {
        return gradeFrom;
    }

    /**
     * Returns a lane path.
     *
     * @param columns  256 flags, index {@code z × 16 + x}: whether the lane crosses that column
     * @param material the village's material
     * @return the path; {@code null} when no flag is set
     */
    public static LanePath of(boolean[] columns, Material material) {
        if (columns.length != 256) {
            throw new IllegalArgumentException("a lane path has 256 columns, got " + columns.length);
        }
        long[] bits = new long[4];
        boolean any = false;
        for (int i = 0; i < 256; i++) {
            if (columns[i]) {
                bits[i >> 6] |= 1L << (i & 63);
                any = true;
            }
        }
        return any ? new LanePath(bits, Objects.requireNonNull(material), null, 0) : null;
    }

    /**
     * Returns whether the lane crosses a column.
     *
     * @param x local X, 0 to 15
     * @param z local Z, 0 to 15
     * @return {@code true} for a lane column
     */
    public boolean contains(int x, int z) {
        int index = (z << 4) | x;
        return (columns[index >> 6] & (1L << (index & 63))) != 0;
    }

    /** Returns the village's material. */
    public Material material() {
        return material;
    }
}
