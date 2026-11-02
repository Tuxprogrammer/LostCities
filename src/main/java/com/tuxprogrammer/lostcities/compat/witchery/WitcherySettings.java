package com.tuxprogrammer.lostcities.compat.witchery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The Witchery values that decide where its standalone structures can stand: which of the four types are on, the grid
 * cell side, and whether the world is one Witchery generates them in. Plain Java; {@code WitcheryConfig} fills it from
 * Witchery's loaded config.
 *
 * <p>
 * The four types and their boxes (width {@code dimX}, depth {@code dimZ}): coven 11 × 11, shack 7 × 7, wicker man 6 ×
 * 5 and hobgoblin hut 7 × 7. A type that is off is skipped by Witchery's generator; the others are tried in an order
 * shuffled with {@code world.rand} (decision unpredictable, coexistence.md § Witchery), so the oracle reserves the
 * union of every type that is on, in every facing ({@link #unionOffsets()}).
 *
 * <p>
 * Decompiled: witchery-1.7.10-0.24.1.jar (GTNH 2.9.0), {@code com.emoniph.witchery.worldgen.ComponentCoven},
 * {@code ComponentShack}, {@code ComponentWickerMan} constructors, {@code WitcheryWorldGenerator} constructor (the
 * hobgoblin hut's 7 × 7 × 7) and {@code WitcheryComponent#calcBox}; inferred from decompiled bytecode and may be wrong.
 * Third-party decompile cited by the spec:
 * https://github.com/GTNH-Museum/Witchery/blob/4b7a98b60de7901e3503247016aa90c90099d0ce/src/main/java/com/emoniph/witchery/worldgen/WitcheryComponent.java#L131-L164.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class WitcherySettings {

    /** One structure type: its config name and its box. */
    public enum StructureType {

        /** A coven, 11 × 11. */
        COVEN(11, 11),
        /** A shack, 7 × 7. */
        SHACK(7, 7),
        /** A wicker man, 6 × 5. */
        WICKER_MAN(6, 5),
        /** A hobgoblin hut, 7 × 7. */
        HOBGOBLIN_HUT(7, 7);

        private final int dimX;
        private final int dimZ;

        StructureType(int dimX, int dimZ) {
            this.dimX = dimX;
            this.dimZ = dimZ;
        }

        /** Returns the box's extent along X before turning, blocks. */
        public int dimX() {
            return dimX;
        }

        /** Returns the box's extent along Z before turning, blocks. */
        public int dimZ() {
            return dimZ;
        }

        /**
         * Returns the block offsets from the candidate chunk's origin that the box reaches in any of its four facings,
         * {@code {minX, minZ, maxX, maxZ}}, inclusive. Witchery starts the box at
         * {@code (x + (16 − dimX) / 2, z + (16 − dimZ) / 2)} and turns it with {@code calcBox}, whose maxima are
         * {@code min + length} (one block more than the length).
         *
         * @return the offsets
         */
        public int[] offsets() {
            int baseX = (16 - dimX) / 2;
            int baseZ = (16 - dimZ) / 2;
            int longest = Math.max(dimX, dimZ);
            return new int[] { baseX - dimZ, baseZ - dimZ, baseX + longest, baseZ + longest };
        }
    }

    private final List<StructureType> types;
    private final int cellSize;
    private final boolean generates;

    /**
     * Creates the settings.
     *
     * @param types     the structure types that are on
     * @param cellSize  the grid cell side in chunks ({@link WitcheryGrid#cellSizeFor})
     * @param generates whether Witchery generates its structures in this world at all (the overworld)
     */
    public WitcherySettings(List<StructureType> types, int cellSize, boolean generates) {
        List<StructureType> sorted = new ArrayList<>(types);
        Collections.sort(sorted);
        this.types = Collections.unmodifiableList(sorted);
        this.cellSize = cellSize;
        this.generates = generates;
    }

    /** Returns the pack's settings: every type on, {@code WorldGenFrequency=12}, the overworld. */
    public static WitcherySettings pack() {
        List<StructureType> all = new ArrayList<>();
        Collections.addAll(all, StructureType.values());
        return new WitcherySettings(all, WitcheryGrid.PACK_CELL_SIZE, true);
    }

    /** Returns the types that are on, in declaration order. */
    public List<StructureType> types() {
        return types;
    }

    /** Returns the grid cell side, in chunks. */
    public int cellSize() {
        return cellSize;
    }

    /** Returns whether any structure can generate in this world. */
    public boolean on() {
        return generates && !types.isEmpty();
    }

    /**
     * Returns the block offsets from a candidate chunk's origin that any enabled type reaches in any facing,
     * {@code {minX, minZ, maxX, maxZ}}, inclusive: −9 to +13 on each axis with the coven on (a 23 × 23 lot over the
     * candidate chunk and its west, north and north-west neighbors).
     *
     * @return the offsets; meaningless when {@link #on()} is false
     */
    public int[] unionOffsets() {
        int[] out = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        for (StructureType type : types) {
            int[] own = type.offsets();
            out[0] = Math.min(out[0], own[0]);
            out[1] = Math.min(out[1], own[1]);
            out[2] = Math.max(out[2], own[2]);
            out[3] = Math.max(out[3], own[3]);
        }
        return out;
    }

    /** Returns the settings as one line, for the pinned snapshot. */
    public String summary() {
        StringBuilder out = new StringBuilder();
        out.append(generates ? "on" : "off")
            .append(",cell=")
            .append(cellSize)
            .append(",types=");
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                out.append('+');
            }
            out.append(
                types.get(i)
                    .name()
                    .toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    @Override
    public String toString() {
        return summary();
    }
}
