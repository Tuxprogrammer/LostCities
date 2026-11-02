package com.tuxprogrammer.lostcities.layout;

/**
 * The chunk order of upstream's {@code locate} and {@code locatepart} searches: 1.20's
 * {@code BlockPos.spiralAround(center, radius, EAST, SOUTH)} on chunk coordinates. It starts at the center, then walks
 * legs of length 1, 1, 2, 2, 3, 3, … east, south, west and north in turn, and ends after the
 * {@code (2 × radius + 1)²} chunks of the square, so nearer chunks always come first
 * (docs/original-spec/01-core/commands.md, Numbers).
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ChunkSpiral {

    /** Upstream's search radius of {@code locate} and {@code locatepart}, in chunks. */
    public static final int LOCATE_RADIUS = 30;

    /** Visits one chunk; returns {@code false} to stop the whole walk. */
    @FunctionalInterface
    public interface Visitor {

        /**
         * Visits a chunk.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} to go on, {@code false} to stop
         */
        boolean visit(int chunkX, int chunkZ);
    }

    private static final int[] STEP_X = { 1, 0, -1, 0 };
    private static final int[] STEP_Z = { 0, 1, 0, -1 };

    private ChunkSpiral() {}

    /**
     * Walks the square of chunks around a center, nearest first, until the visitor stops it.
     *
     * @param centerX center chunk X
     * @param centerZ center chunk Z
     * @param radius  the square's half side, in chunks, 0 or more
     * @param visitor what to do with each chunk
     * @return how many chunks were visited
     */
    public static int walk(int centerX, int centerZ, int radius, Visitor visitor) {
        long side = 2L * Math.max(0, radius) + 1;
        long total = side * side;
        int x = centerX;
        int z = centerZ;
        int visited = 1;
        if (!visitor.visit(x, z)) {
            return visited;
        }
        int leg = 1;
        int direction = 0;
        while (visited < total) {
            for (int repeat = 0; repeat < 2 && visited < total; repeat++) {
                for (int step = 0; step < leg && visited < total; step++) {
                    x += STEP_X[direction];
                    z += STEP_Z[direction];
                    visited++;
                    if (!visitor.visit(x, z)) {
                        return visited;
                    }
                }
                direction = (direction + 1) & 3;
            }
            leg++;
        }
        return visited;
    }
}
