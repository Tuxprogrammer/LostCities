package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLog;
import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/**
 * The footprint write guard of docs/design/0005-rwg-overlay.md §3a: while a wrapped world's delegate runs
 * {@code populate}, every block write that lands inside the city's footprint is dropped, so RWG's trees, boulders,
 * small ruins, mineshafts and strongholds stay out of buildings and streets while its decoration outside the footprint
 * stays (D22, D31). The mixin on {@code Chunk.func_150807_a} ({@code mixins.MixinChunkFootprintGuard}) asks
 * {@link #drops} before each write.
 *
 * <p>
 * The guard is a stack of frames, one per {@code populate} on the stack: the overlay pushes an active frame around the
 * delegate's {@code populate} and an inactive one around its own population phase, so a population nested inside
 * another (a cascade) is handled frame by frame. RWG adds snow and ice after {@code PopulateChunkEvent.Post}; the Post
 * listener switches the top frame off so snow passes, as the footprint sheet requires.
 *
 * <p>
 * This is the one mutable static of the overlay, and it must be: the chunk setter has no other cheap way to reach the
 * overlay. It holds only transient state of the server thread's current population, empty whenever no population is
 * running, and it never decides what generates on its own: the footprint comes from the pure plan. Server thread only.
 * Side: server.
 */
public final class FootprintGuard {

    /** Deepest nesting of populations the guard tracks; deeper ones are unguarded and logged by the overlay. */
    static final int MAX_DEPTH = 64;

    private static final World[] WORLDS = new World[MAX_DEPTH];
    private static final CityPlan[] PLANS = new CityPlan[MAX_DEPTH];
    private static final boolean[] ACTIVE = new boolean[MAX_DEPTH];
    private static int depth;
    /** Whether the top frame is active, kept apart so the common "no population" case is one field read. */
    private static boolean topActive;
    private static long dropped;
    private static long leakedFoliage;

    private FootprintGuard() {}

    /**
     * Pushes a frame.
     *
     * @param world  the world being populated
     * @param plan   that world's plan source
     * @param active whether writes are guarded in this frame
     * @return whether the frame was pushed; {@code false} past {@link #MAX_DEPTH}
     */
    public static boolean push(World world, CityPlan plan, boolean active) {
        if (depth >= MAX_DEPTH) {
            return false;
        }
        WORLDS[depth] = world;
        PLANS[depth] = plan;
        ACTIVE[depth] = active;
        depth++;
        topActive = active;
        return true;
    }

    /** Pops the top frame; call it in a {@code finally} for every successful {@link #push}. */
    public static void pop() {
        if (depth == 0) {
            return;
        }
        depth--;
        WORLDS[depth] = null;
        PLANS[depth] = null;
        topActive = depth > 0 && ACTIVE[depth - 1];
    }

    /**
     * Switches the top frame off when it belongs to this world: RWG's snow and ice after
     * {@code PopulateChunkEvent.Post} pass the guard.
     *
     * @param world the world whose {@code Post} event fired
     */
    public static void endOfDecoration(World world) {
        if (depth > 0 && WORLDS[depth - 1] == world) {
            ACTIVE[depth - 1] = false;
            topActive = false;
        }
    }

    /**
     * Returns whether a write must be dropped: the top frame is active, the chunk is in the guarded world, and the
     * position is inside the footprint.
     *
     * @param world  the chunk's world
     * @param chunkX the chunk's X
     * @param chunkZ the chunk's Z
     * @param x      local X, 0 to 15
     * @param y      block Y
     * @param z      local Z, 0 to 15
     * @return {@code true} to drop the write
     */
    public static boolean drops(World world, int chunkX, int chunkZ, int x, int y, int z) {
        if (!topActive) {
            return false;
        }
        int top = depth - 1;
        if (WORLDS[top] != world) {
            return false;
        }
        if (y < PLANS[top].chunk(chunkX, chunkZ)
            .footprintBottom(x & 15, z & 15)) {
            return false;
        }
        dropped++;
        return true;
    }

    /**
     * Counts a log or leaf block written into a footprint while a population frame is on the stack but not guarding:
     * after {@code PopulateChunkEvent.Post} or during our own population phase. Neither should ever write one, so the
     * worldgen scenario fails when this count rises (RWG's decoration must not reach the footprint by any path).
     *
     * @param world  the chunk's world
     * @param chunkX the chunk's X
     * @param chunkZ the chunk's Z
     * @param x      local X
     * @param y      block Y
     * @param z      local Z
     * @param block  the block being written
     */
    public static void noteUnguardedWrite(World world, int chunkX, int chunkZ, int x, int y, int z, Block block) {
        if (depth == 0 || !(block instanceof BlockLog || block instanceof BlockLeavesBase)) {
            return;
        }
        int top = depth - 1;
        if (WORLDS[top] == world && y >= PLANS[top].chunk(chunkX, chunkZ)
            .footprintBottom(x & 15, z & 15)) {
            leakedFoliage++;
        }
    }

    /** Returns how many logs and leaves reached a footprint during a population without the guard. */
    public static long leakedFoliage() {
        return leakedFoliage;
    }

    /** Returns whether any population frame is on the stack. */
    public static boolean populating() {
        return depth > 0;
    }

    /** Returns the number of writes dropped since the server started. */
    public static long droppedWrites() {
        return dropped;
    }

    /** Clears every frame; the server calls it when it stops, so a crashed population leaves nothing behind. */
    public static void reset() {
        while (depth > 0) {
            pop();
        }
        topActive = false;
    }
}
