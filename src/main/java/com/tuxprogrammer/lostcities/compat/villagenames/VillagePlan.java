package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;

/**
 * A village's plan as VillageNames builds it in its start's constructor, reduced to what the oracle needs: every
 * piece's box in X and Z, which pieces are paths, and whether the village is built at all (VillageNames builds a start
 * only when it has more than two pieces that are not paths: {@code isSizeableStructure}). Immutable.
 *
 * <p>
 * Side: both (plain data).
 */
public final class VillagePlan {

    /** One piece: its box in X and Z, and whether it is a path ({@code StructureVillagePieces.Road}). */
    public static final class Piece {

        private final BlockRect box;
        private final boolean path;

        /**
         * Creates a piece.
         *
         * @param box  the piece's box in X and Z, inclusive
         * @param path whether the piece is a path
         */
        public Piece(BlockRect box, boolean path) {
            this.box = box;
            this.path = path;
        }

        /** Returns the piece's box in X and Z. */
        public BlockRect box() {
            return box;
        }

        /** Returns whether the piece is a path. */
        public boolean isPath() {
            return path;
        }

        @Override
        public String toString() {
            return (path ? "path" : "piece") + box;
        }
    }

    private final List<Piece> pieces;
    private final boolean sizeable;

    /**
     * Creates a plan.
     *
     * @param pieces   the pieces in VillageNames' order, the town center first
     * @param sizeable whether VillageNames builds the village
     */
    public VillagePlan(List<Piece> pieces, boolean sizeable) {
        this.pieces = Collections.unmodifiableList(new ArrayList<>(pieces));
        this.sizeable = sizeable;
    }

    /** Returns the pieces, the town center first. */
    public List<Piece> pieces() {
        return pieces;
    }

    /** Returns whether VillageNames builds the village. */
    public boolean isSizeable() {
        return sizeable;
    }
}
