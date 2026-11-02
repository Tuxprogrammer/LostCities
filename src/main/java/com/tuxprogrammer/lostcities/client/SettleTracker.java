package com.tuxprogrammer.lostcities.client;

/**
 * Decides, tick by tick, when a capture viewpoint has settled: the camera has stopped moving, every chunk around it
 * has arrived and the terrain renderer has nothing left to build, each for long enough that a one-frame lull does not
 * count. Plain Java: {@link CleanCapture} feeds it what the client sees, and tests feed it sequences.
 *
 * <p>
 * Side: client.
 */
final class SettleTracker {

    /** What a tick changed. */
    enum Event {
        /** Nothing worth logging. */
        NONE,
        /** The camera moved away from a settled viewpoint. */
        MOVED,
        /** The viewpoint has just settled. */
        SETTLED
    }

    /** Ticks the camera must stay put: one second. */
    static final int STILL_TICKS = 20;
    /** Ticks the chunks and terrain must stay complete: one second, so a frame between builds does not count. */
    static final int COMPLETE_TICKS = 20;
    /** Movement, in blocks, below which the camera counts as still. */
    static final double MOVE_EPSILON = 0.05;
    /** Rotation, in degrees, below which the camera counts as still. */
    static final float TURN_EPSILON = 0.5f;

    private boolean started;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private int stillTicks;
    private int completeTicks;
    private int ticksSinceMove;
    private boolean settled;

    /**
     * Takes one client tick's observation.
     *
     * @param x             camera X
     * @param y             camera Y
     * @param z             camera Z
     * @param yaw           camera yaw in degrees
     * @param pitch         camera pitch in degrees
     * @param missingChunks chunks in the capture radius that have not arrived yet
     * @param terrainBuilt  whether the terrain renderer's build queue is empty
     * @return what changed
     */
    Event tick(double x, double y, double z, float yaw, float pitch, int missingChunks, boolean terrainBuilt) {
        boolean moved = !started || Math.abs(x - this.x) > MOVE_EPSILON
            || Math.abs(y - this.y) > MOVE_EPSILON
            || Math.abs(z - this.z) > MOVE_EPSILON
            || Math.abs(yaw - this.yaw) > TURN_EPSILON
            || Math.abs(pitch - this.pitch) > TURN_EPSILON;
        started = true;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        if (moved) {
            stillTicks = 0;
            completeTicks = 0;
            ticksSinceMove = 0;
            if (settled) {
                settled = false;
                return Event.MOVED;
            }
            return Event.NONE;
        }
        stillTicks++;
        ticksSinceMove++;
        completeTicks = missingChunks == 0 && terrainBuilt ? completeTicks + 1 : 0;
        if (!settled && stillTicks >= STILL_TICKS && completeTicks >= COMPLETE_TICKS) {
            settled = true;
            return Event.SETTLED;
        }
        return Event.NONE;
    }

    /** Returns whether the current viewpoint has settled. */
    boolean settled() {
        return settled;
    }

    /** Returns the ticks since the camera last moved, for the log. */
    int ticksSinceMove() {
        return ticksSinceMove;
    }

    /** Forgets everything, for a world change. */
    void reset() {
        started = false;
        settled = false;
        stillTicks = 0;
        completeTicks = 0;
        ticksSinceMove = 0;
    }
}
