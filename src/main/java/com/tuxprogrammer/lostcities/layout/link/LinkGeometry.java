package com.tuxprogrammer.lostcities.layout.link;

import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;

/**
 * The column-by-column deck of the link pieces written in code (design record 0015 §3): which world columns a
 * diagonal, a transition or a plaza's corner opening covers, at what height, and where each column sits across the
 * road, so a writer can copy the straight part's cross-section into it.
 *
 * <p>
 * Geometry, in world block coordinates with column centers at {@code +0.5}. A piece's openings sit at the middle of a
 * side or at a corner of its chunk. A <b>diagonal</b> is the strip within {@link #HALF_WIDTH} blocks of the line
 * between its two corners, cut square at both corners; near each corner it spills into the two side chunks. A
 * <b>transition</b> is a straight strip from the middle of its side to the chunk's center, a diagonal strip from the
 * center to its corner, and a disc at the center that fills the outside of the turn. A <b>plaza</b> with a corner
 * opening adds a disc at that corner outside its own chunk (the plaza's part covers its chunk), which fills the wedges
 * between the plaza and the diagonal leaving it.
 *
 * <p>
 * Height. A diagonal ramp climbs {@link #RAMP_HALF_STEPS} half blocks (one city level) evenly along its length, so
 * neighboring columns differ by at most one half block; everything else is flat. Where pieces overlap, the highest deck
 * wins, so every chunk that computes a column gets the same answer. Pure functions.
 */
public final class LinkGeometry {

    /** Half the road's width in blocks: the straight parts are 16 wide. */
    public static final double HALF_WIDTH = 8.0;

    /** Half blocks a ramp climbs: one city level. */
    public static final int RAMP_HALF_STEPS = 12;

    private static final double CHUNK = 16.0;

    /** One covered column. */
    public static final class Column {

        private final int level;
        private final int half;
        private final double along;
        private final double across;

        Column(int level, int half, double along, double across) {
            this.level = level;
            this.half = half;
            this.along = along;
            this.across = across;
        }

        /** Returns the city level the deck stands on. */
        public int level() {
            return level;
        }

        /** Returns the half blocks the deck stands above {@link #level}, 0 to {@link #RAMP_HALF_STEPS}. */
        public int half() {
            return half;
        }

        /** Returns the distance along the road from the piece's start, in blocks (0 on discs). */
        public double along() {
            return along;
        }

        /** Returns the signed distance across the road from its center line, in blocks (0 on discs). */
        public double across() {
            return across;
        }

        /** Returns the deck height in half blocks above level 0's ground, for comparing columns. */
        public int height() {
            return level * RAMP_HALF_STEPS + half;
        }

        /**
         * Returns the row of a straight X-axis part whose cross-section this column copies, 0 to 15.
         *
         * @return the part's Z row
         */
        public int row() {
            return Math.max(0, Math.min(15, (int) Math.floor(HALF_WIDTH + across)));
        }
    }

    private LinkGeometry() {}

    /**
     * Returns whether a writer copies a piece's deck column by column: a diagonal, a transition, or a plaza that opens
     * on a corner.
     *
     * @param piece the piece
     * @return whether {@link #column} can cover anything for it
     */
    public static boolean coded(HighwayInfo.Piece piece) {
        return piece.coded() || piece.shape() == HighwayInfo.Shape.CROSSING && piece.opensOnCorner();
    }

    /**
     * Returns the deck a piece puts on a world column, or {@code null} when it does not cover it.
     *
     * @param chunkX the piece's chunk X
     * @param chunkZ the piece's chunk Z
     * @param piece  the piece
     * @param x      the column's world X
     * @param z      the column's world Z
     * @return the column, or null
     */
    public static Column column(int chunkX, int chunkZ, HighwayInfo.Piece piece, int x, int z) {
        double cx = x + 0.5;
        double cz = z + 0.5;
        double ox = chunkX * CHUNK + CHUNK / 2;
        double oz = chunkZ * CHUNK + CHUNK / 2;
        int[] o = piece.openings();
        switch (piece.shape()) {
            case DIAGONAL: {
                double ax = ox + o[0] * HALF_WIDTH;
                double az = oz + o[1] * HALF_WIDTH;
                double bx = ox + o[3] * HALF_WIDTH;
                double bz = oz + o[4] * HALF_WIDTH;
                double[] s = strip(ax, az, bx, bz, cx, cz);
                if (s == null) {
                    return null;
                }
                int la = o[2];
                int lb = o[5];
                double length = Math.hypot(bx - ax, bz - az);
                double frac = s[0] / length;
                int half = la == lb ? 0
                    : (int) Math.round(RAMP_HALF_STEPS * (lb > la ? frac : 1 - frac));
                return new Column(Math.min(la, lb), half, s[0], s[1]);
            }
            case TRANSITION: {
                boolean aCorner = o[0] != 0 && o[1] != 0;
                int side = aCorner ? 3 : 0;
                int corner = aCorner ? 0 : 3;
                int level = o[2];
                double sx = ox + o[side] * HALF_WIDTH;
                double sz = oz + o[side + 1] * HALF_WIDTH;
                double kx = ox + o[corner] * HALF_WIDTH;
                double kz = oz + o[corner + 1] * HALF_WIDTH;
                double[] s = strip(sx, sz, ox, oz, cx, cz);
                if (s != null) {
                    return new Column(level, 0, s[0], s[1]);
                }
                s = strip(ox, oz, kx, kz, cx, cz);
                if (s != null) {
                    return new Column(level, 0, HALF_WIDTH + s[0], s[1]);
                }
                return Math.hypot(cx - ox, cz - oz) < HALF_WIDTH ? new Column(level, 0, 0, 0) : null;
            }
            case CROSSING: {
                if (Math.floorDiv(x, 16) == chunkX && Math.floorDiv(z, 16) == chunkZ) {
                    return null;
                }
                for (int i = 0; i + 2 < o.length; i += 3) {
                    if (o[i] == 0 || o[i + 1] == 0) {
                        continue;
                    }
                    double kx = ox + o[i] * HALF_WIDTH;
                    double kz = oz + o[i + 1] * HALF_WIDTH;
                    if (Math.hypot(cx - kx, cz - kz) < HALF_WIDTH) {
                        return new Column(o[i + 2], 0, 0, 0);
                    }
                }
                return null;
            }
            default:
                return null;
        }
    }

    /**
     * Returns the highest deck any of several pieces puts on a world column.
     *
     * @param chunkXs the pieces' chunk X
     * @param chunkZs the pieces' chunk Z
     * @param pieces  the pieces
     * @param count   how many of the arrays to read
     * @param x       the column's world X
     * @param z       the column's world Z
     * @return the column, or null when none covers it
     */
    public static Column highest(int[] chunkXs, int[] chunkZs, HighwayInfo.Piece[] pieces, int count, int x, int z) {
        Column best = null;
        for (int i = 0; i < count; i++) {
            Column c = column(chunkXs[i], chunkZs[i], pieces[i], x, z);
            if (c != null && (best == null || c.height() > best.height())) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Returns {@code {along, across}} of a point in the strip of half-width {@link #HALF_WIDTH} along a segment, cut
     * square at both ends, or null outside it.
     */
    private static double[] strip(double ax, double az, double bx, double bz, double cx, double cz) {
        double length = Math.hypot(bx - ax, bz - az);
        double ux = (bx - ax) / length;
        double uz = (bz - az) / length;
        double px = cx - ax;
        double pz = cz - az;
        double along = px * ux + pz * uz;
        double across = -px * uz + pz * ux;
        if (along < 0 || along >= length || Math.abs(across) >= HALF_WIDTH) {
            return null;
        }
        return new double[] { along, across };
    }
}
