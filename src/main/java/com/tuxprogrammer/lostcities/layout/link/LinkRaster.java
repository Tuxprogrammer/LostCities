package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rasterizes a city link into a chain of chunks (docs/design/0015-city-link-network.md §2). The path is octilinear:
 * a straight lead along the link's major axis, a 45° diagonal run, and a straight trail, the two straights each half
 * of the axial remainder, so the path stays within half a chunk-step of the true line at its middle and both ends.
 * Consecutive chunks are 8-neighbors; a diagonal step joins two chunks at a shared corner, which is where a diagonal
 * piece's road crosses.
 *
 * <p>
 * A link is always rasterized from its lower end ({@link #lower}), so the same chunks come out whichever end asks:
 * deterministic in any generation order. Pure: no world access.
 */
public final class LinkRaster {

    /** The kind of piece a chunk of a link carries. */
    public enum Kind {
        /** A link's end: the city center the link joins. */
        END,
        /** Entered and left along one axis, mid-side to mid-side. */
        STRAIGHT,
        /** Entered and left at opposite corners, 45°. */
        DIAGONAL,
        /** Entered mid-side and left at a corner, or the reverse: where a straight run meets a diagonal one. */
        TRANSITION
    }

    /** One chunk of a link. */
    public static final class Step {

        private final int chunkX;
        private final int chunkZ;
        private final Kind kind;
        private final int inDx;
        private final int inDz;
        private final int outDx;
        private final int outDz;

        Step(int chunkX, int chunkZ, Kind kind, int inDx, int inDz, int outDx, int outDz) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.kind = kind;
            this.inDx = inDx;
            this.inDz = inDz;
            this.outDx = outDx;
            this.outDz = outDz;
        }

        /** Returns the chunk X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the chunk Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the piece kind. */
        public Kind kind() {
            return kind;
        }

        /** Returns the X of the step that entered this chunk (−1, 0 or 1), 0 at the first end. */
        public int inDx() {
            return inDx;
        }

        /** Returns the Z of the step that entered this chunk, 0 at the first end. */
        public int inDz() {
            return inDz;
        }

        /** Returns the X of the step that leaves this chunk, 0 at the last end. */
        public int outDx() {
            return outDx;
        }

        /** Returns the Z of the step that leaves this chunk, 0 at the last end. */
        public int outDz() {
            return outDz;
        }

        @Override
        public String toString() {
            return kind + "@" + chunkX + "," + chunkZ;
        }
    }

    private LinkRaster() {}

    /**
     * Returns whether the first point is the lower end of a link: lower X, then lower Z.
     *
     * @return whether (ax, az) comes before (bx, bz)
     */
    public static boolean lower(int ax, int az, int bx, int bz) {
        return ax != bx ? ax < bx : az < bz;
    }

    /**
     * Rasterizes the link between two chunks, from its lower end to its upper end.
     *
     * @param ax first end's chunk X
     * @param az first end's chunk Z
     * @param bx second end's chunk X
     * @param bz second end's chunk Z
     * @return the chunks in order, each once, both ends included; one chunk when the ends coincide
     */
    public static List<Step> rasterize(int ax, int az, int bx, int bz) {
        if (!lower(ax, az, bx, bz) && (ax != bx || az != bz)) {
            return rasterize(bx, bz, ax, az);
        }
        int dx = bx - ax;
        int dz = bz - az;
        int sx = Integer.signum(dx);
        int sz = Integer.signum(dz);
        int adx = Math.abs(dx);
        int adz = Math.abs(dz);
        boolean majorX = adx >= adz;
        int diagonal = Math.min(adx, adz);
        int straight = Math.max(adx, adz) - diagonal;
        int lead = straight / 2;
        int total = straight + diagonal;
        // The step direction of each move, in order.
        int[] mx = new int[total];
        int[] mz = new int[total];
        for (int i = 0; i < total; i++) {
            boolean diag = i >= lead && i < lead + diagonal;
            mx[i] = diag || majorX ? sx : 0;
            mz[i] = diag || !majorX ? sz : 0;
        }
        List<Step> out = new ArrayList<>(total + 1);
        int x = ax;
        int z = az;
        for (int i = 0; i <= total; i++) {
            int inX = i == 0 ? 0 : mx[i - 1];
            int inZ = i == 0 ? 0 : mz[i - 1];
            int outX = i == total ? 0 : mx[i];
            int outZ = i == total ? 0 : mz[i];
            out.add(new Step(x, z, kind(i, total, inX, inZ, outX, outZ), inX, inZ, outX, outZ));
            if (i < total) {
                x += outX;
                z += outZ;
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static Kind kind(int i, int total, int inX, int inZ, int outX, int outZ) {
        if (i == 0 || i == total) {
            return Kind.END;
        }
        boolean inDiagonal = inX != 0 && inZ != 0;
        boolean outDiagonal = outX != 0 && outZ != 0;
        if (inDiagonal != outDiagonal) {
            return Kind.TRANSITION;
        }
        return inDiagonal ? Kind.DIAGONAL : Kind.STRAIGHT;
    }
}
