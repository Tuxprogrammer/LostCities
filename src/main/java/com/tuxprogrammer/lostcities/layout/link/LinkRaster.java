package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<int[]> moves = new ArrayList<>();
        moves(moves, ax, az, bx, bz);
        return steps(ax, az, moves);
    }

    /**
     * Rasterizes a detour (0015 §6, D390): the octilinear path from the first end to a waypoint, then on to the second
     * end, with every turn sharper than 45° smoothed away ({@link #smooth}), so the path uses only the straight,
     * diagonal and transition pieces of a plain link. The caller passes the lower end first; the result is a pure
     * function of the three points.
     *
     * @param ax first end's chunk X
     * @param az first end's chunk Z
     * @param wx waypoint chunk X
     * @param wz waypoint chunk Z
     * @param bx second end's chunk X
     * @param bz second end's chunk Z
     * @return the chunks in order, first end first, or {@code null} when the smoothed path visits a chunk twice
     */
    public static List<Step> rasterizeVia(int ax, int az, int wx, int wz, int bx, int bz) {
        List<int[]> moves = new ArrayList<>();
        moves(moves, ax, az, wx, wz);
        moves(moves, wx, wz, bx, bz);
        smooth(moves);
        List<Step> out = steps(ax, az, moves);
        Set<Long> seen = new HashSet<>();
        for (Step step : out) {
            if (!seen.add((long) step.chunkX() << 32 | step.chunkZ() & 0xffffffffL)) {
                return null;
            }
        }
        return out;
    }

    /** Appends the moves of the octilinear path between two chunks (class Javadoc). */
    private static void moves(List<int[]> out, int ax, int az, int bx, int bz) {
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
        for (int i = 0; i < total; i++) {
            boolean diag = i >= lead && i < lead + diagonal;
            out.add(new int[] { diag || majorX ? sx : 0, diag || !majorX ? sz : 0 });
        }
    }

    /**
     * Replaces every pair of consecutive moves that turns by more than 45° (non-positive dot product) with moves that
     * reach the same chunk: a reversal cancels, a right-angle turn between sides becomes one diagonal, one between
     * diagonals two sides, a 135° turn the single move it sums to. Each replacement shortens the list or trades two
     * diagonals for two sides, so the loop ends; the sum of the moves, and so the last chunk, never changes.
     */
    static void smooth(List<int[]> moves) {
        int i = 0;
        while (i + 1 < moves.size()) {
            int[] a = moves.get(i);
            int[] b = moves.get(i + 1);
            if (a[0] * b[0] + a[1] * b[1] > 0) {
                i++;
                continue;
            }
            int sx = a[0] + b[0];
            int sz = a[1] + b[1];
            moves.remove(i + 1);
            moves.remove(i);
            if (sx != 0 || sz != 0) {
                if (Math.abs(sx) <= 1 && Math.abs(sz) <= 1) {
                    moves.add(i, new int[] { sx, sz });
                } else {
                    moves.add(i, new int[] { sx / 2, sz / 2 });
                    moves.add(i, new int[] { sx / 2, sz / 2 });
                }
            }
            i = Math.max(0, i - 1);
        }
    }

    /** Walks a list of moves from a chunk into steps (class Javadoc). */
    private static List<Step> steps(int ax, int az, List<int[]> moves) {
        int total = moves.size();
        List<Step> out = new ArrayList<>(total + 1);
        int x = ax;
        int z = az;
        for (int i = 0; i <= total; i++) {
            int inX = i == 0 ? 0 : moves.get(i - 1)[0];
            int inZ = i == 0 ? 0 : moves.get(i - 1)[1];
            int outX = i == total ? 0 : moves.get(i)[0];
            int outZ = i == total ? 0 : moves.get(i)[1];
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
