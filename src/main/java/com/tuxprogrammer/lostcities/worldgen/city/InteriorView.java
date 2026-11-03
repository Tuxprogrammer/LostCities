package com.tuxprogrammer.lostcities.worldgen.city;

/**
 * Where the interior shot's camera stands in a building's floor, read from the generated blocks: a spot a player could
 * stand on, under the building's ceiling, with its back to a wall and the longest stretch of open room in front of it,
 * so the frame shows the room and not the wall beside the camera (the alpha shot list's {@code interior} view, which
 * stood in the chunk's middle facing the door and on RWG2 filled the frame with one wall).
 *
 * <ul>
 * <li>A spot is a column of the chunk with two blocks of air at the feet and the head, a block under the feet, and a
 * block above within {@link #CEILING_MAX} of the feet, so the camera is indoors.</li>
 * <li>From each spot it looks along the eight compass directions and counts, in blocks, how far the eye level stays
 * open, indoors (air with a ceiling above) and in the chunk, at most {@link #RUN_MAX}. The longest run wins; on a
 * tie, a spot with a wall at its back, then the spot nearer the finder's suggested point, then the lower Z and X, then
 * the first direction.</li>
 * <li>A run shorter than {@link #RUN_MIN} is no room: the finder keeps its own viewpoint.</li>
 * </ul>
 *
 * <p>
 * Pure: the blocks come through {@link Blocks}. Side: both (plain Java); the command reads the server's world.
 */
public final class InteriorView {

    /** Whether a block is air, the only thing the view reads. */
    @FunctionalInterface
    public interface Blocks {

        /**
         * Returns whether the block at a position is air.
         *
         * @param x block X
         * @param y block Y
         * @param z block Z
         * @return whether it is air
         */
        boolean air(int x, int y, int z);
    }

    /** Highest a ceiling may be above the feet for a spot to count as indoors, in blocks. */
    static final int CEILING_MAX = 8;
    /** Longest open run counted, in blocks: across a chunk's diagonal. */
    static final int RUN_MAX = 22;
    /** Shortest open run that counts as a room, in blocks. */
    static final int RUN_MIN = 4;
    /** Pitch of the interior view, degrees: a little down, so the floor and the furniture show. */
    static final float PITCH = 12f;
    /** The eight compass steps, X and Z, cardinal first. */
    private static final int[][] STEPS = { { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 0 }, { 1, 1 }, { -1, 1 }, { -1, -1 },
        { 1, -1 } };

    private final double x;
    private final double z;
    private final float yaw;
    private final double run;
    private final boolean backed;

    private InteriorView(double x, double z, float yaw, double run, boolean backed) {
        this.x = x;
        this.z = z;
        this.yaw = yaw;
        this.run = run;
        this.backed = backed;
    }

    /** Returns the camera's X, the middle of its block. */
    public double x() {
        return x;
    }

    /** Returns the camera's Z, the middle of its block. */
    public double z() {
        return z;
    }

    /** Returns the view's yaw, degrees (0 south, 90 west, 180 north, 270 east). */
    public float yaw() {
        return yaw;
    }

    /** Returns the view's pitch, degrees. */
    public float pitch() {
        return PITCH;
    }

    /** Returns how far the room lies open in front of the camera, in blocks. */
    public double run() {
        return run;
    }

    /** Returns whether the camera stands with a wall (anything not air) at its back, at eye level. */
    public boolean backed() {
        return backed;
    }

    /**
     * Chooses the camera's spot and direction in one chunk's floor.
     *
     * @param blocks the world's blocks
     * @param chunkX the building's chunk X
     * @param chunkZ the building's chunk Z
     * @param feet   the floor's feet level (the block above its floor)
     * @param nearX  the suggested camera X, for ties
     * @param nearZ  the suggested camera Z, for ties
     * @return the view, or {@code null} when no spot looks along {@link #RUN_MIN} blocks of room
     */
    public static InteriorView choose(Blocks blocks, int chunkX, int chunkZ, int feet, double nearX, double nearZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        InteriorView best = null;
        double bestNear = Double.MAX_VALUE;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                if (!standable(blocks, x, feet, z)) {
                    continue;
                }
                double near = (x + 0.5 - nearX) * (x + 0.5 - nearX) + (z + 0.5 - nearZ) * (z + 0.5 - nearZ);
                for (int[] step : STEPS) {
                    double run = run(blocks, baseX, baseZ, x, feet + 1, z, step[0], step[1]);
                    if (run < RUN_MIN) {
                        continue;
                    }
                    int backX = dx - step[0];
                    int backZ = dz - step[1];
                    // Outside the chunk is the building's outer wall; inside it, anything but air.
                    boolean backed = backX < 0 || backX > 15
                        || backZ < 0
                        || backZ > 15
                        || !blocks.air(baseX + backX, feet + 1, baseZ + backZ);
                    if (best == null || run > best.run
                        || run == best.run && (backed && !best.backed || backed == best.backed && near < bestNear)) {
                        best = new InteriorView(
                            x + 0.5,
                            z + 0.5,
                            CityFinder.yawToward(0, 0, step[0], step[1]),
                            run,
                            backed);
                        bestNear = near;
                    }
                }
            }
        }
        return best;
    }

    /** Whether a player stands at a column: air at the feet and the head, a floor, and a ceiling above. */
    static boolean standable(Blocks blocks, int x, int feet, int z) {
        return blocks.air(x, feet, z) && blocks.air(x, feet + 1, z)
            && !blocks.air(x, feet - 1, z)
            && indoors(blocks, x, feet + 1, z);
    }

    /** Whether a block has a non-air block above it within the ceiling height of the feet below it. */
    private static boolean indoors(Blocks blocks, int x, int eye, int z) {
        for (int y = eye + 1; y < eye + CEILING_MAX; y++) {
            if (!blocks.air(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** How far, in blocks, the eye level stays open, indoors and in the chunk from a column along a step. */
    private static double run(Blocks blocks, int baseX, int baseZ, int x, int eye, int z, int stepX, int stepZ) {
        double length = Math.hypot(stepX, stepZ);
        int steps = 0;
        while (steps * length < RUN_MAX) {
            int nx = x + stepX * (steps + 1);
            int nz = z + stepZ * (steps + 1);
            if (nx < baseX || nx > baseX + 15
                || nz < baseZ
                || nz > baseZ + 15
                || !blocks.air(nx, eye, nz)
                || !indoors(blocks, nx, eye, nz)) {
                break;
            }
            steps++;
        }
        return Math.min(RUN_MAX, steps * length);
    }
}
