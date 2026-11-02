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

    private LanePath(long[] columns, Material material) {
        this.columns = columns;
        this.material = material;
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
        return any ? new LanePath(bits, Objects.requireNonNull(material)) : null;
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
