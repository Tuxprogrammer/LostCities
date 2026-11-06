package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
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
 * A third kind of frame, pushed around the other mods' {@code IWorldGenerator}s ({@link TreeGuard}), drops only tree
 * parts: logs, leaves, plants, vines and cacti, so IC2 rubber trees and HarvestCraft fruit trees planted on park
 * grass no longer overhang streets and roofs, and Natura's saguaros no longer stand inside a scattered structure, while
 * GT's ore veins and every other block pass (decision log, M05J rows; D75's finding; D234).
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
    /** A frame that guards nothing: our own population phase, or RWG after its decoration. */
    static final byte OFF = 0;
    /** A frame that drops every write in the footprint: RWG's {@code populate}. */
    static final byte ALL = 1;
    /** A frame that drops only tree parts in the footprint: the other mods' {@code IWorldGenerator}s. */
    static final byte TREES = 2;
    /** RWG's snow and ice pass after {@code Post}: only snow layers on a door's approach are dropped. */
    static final byte SNOW = 3;
    /**
     * GregTech's stone boulders: every write in the footprint is dropped, like {@link #ALL}, and Post does not end it.
     */
    static final byte STONE = 4;

    /** A frame around a pack house's own write: guards nothing and is not counted as a leak. */
    static final byte HOUSE = 5;

    private static final byte[] MODE = new byte[MAX_DEPTH];
    private static int depth;
    /** The top frame's mode, kept apart so the common "no population" case is one field read. */
    private static byte topMode;
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
        return push(world, plan, active ? ALL : OFF);
    }

    /**
     * Pushes a frame that drops only tree parts, around the other mods' world generators.
     *
     * @param world the world being populated
     * @param plan  that world's plan source
     * @return whether the frame was pushed
     */
    public static boolean pushTrees(World world, CityPlan plan) {
        return push(world, plan, TREES);
    }

    /**
     * Pushes a frame around one GregTech stone-boulder generator ({@code MixinGtWorldgenStone}). GT runs after the
     * closing tree generator, so no tree frame covers it. A frame left by a throw is replaced rather than stacked.
     *
     * @param world the world being populated
     * @param plan  that world's plan source
     * @return whether the frame was pushed
     */
    public static boolean pushStone(World world, CityPlan plan) {
        if (topIsStone(world)) {
            pop();
        }
        return push(world, plan, STONE);
    }

    /**
     * Returns whether the top frame is a stone frame of a world: what the boulder generator's return pops.
     *
     * @param world the world
     * @return {@code true} when it is
     */
    public static boolean topIsStone(World world) {
        return depth > 0 && MODE[depth - 1] == STONE && WORLDS[depth - 1] == world;
    }

    /**
     * Pushes a frame around a pack house's write (a village piece placed by another mod): it drops nothing, and its
     * logs and leaves are not counted as leaked foliage, but the frame keeps {@link #populating()} true so a chunk
     * generated meanwhile still counts as a cascade.
     *
     * @param world the world being populated
     * @param plan  that world's plan source
     * @return whether the frame was pushed
     */
    public static boolean pushHouse(World world, CityPlan plan) {
        return push(world, plan, HOUSE);
    }

    private static boolean push(World world, CityPlan plan, byte mode) {
        if (depth >= MAX_DEPTH) {
            return false;
        }
        WORLDS[depth] = world;
        PLANS[depth] = plan;
        MODE[depth] = mode;
        depth++;
        topMode = mode;
        return true;
    }

    /**
     * Returns whether the top frame is a tree frame of a world: what the closing generator pops.
     *
     * @param world the world
     * @return {@code true} when it is
     */
    public static boolean topIsTrees(World world) {
        return depth > 0 && MODE[depth - 1] == TREES && WORLDS[depth - 1] == world;
    }

    /** Pops the top frame; call it in a {@code finally} for every successful {@link #push}. */
    public static void pop() {
        if (depth == 0) {
            return;
        }
        depth--;
        WORLDS[depth] = null;
        PLANS[depth] = null;
        topMode = depth > 0 ? MODE[depth - 1] : OFF;
    }

    /**
     * Switches the top frame off when it belongs to this world: RWG's snow and ice after
     * {@code PopulateChunkEvent.Post} pass the guard.
     *
     * @param world the world whose {@code Post} event fired
     */
    public static void endOfDecoration(World world) {
        if (depth > 0 && WORLDS[depth - 1] == world && MODE[depth - 1] == ALL) {
            MODE[depth - 1] = SNOW;
            topMode = SNOW;
        }
    }

    /**
     * Returns whether a write must be dropped: the top frame guards, the block is one it guards against (any block, or
     * a tree part in a tree frame), the chunk is in the guarded world, and the position is inside the footprint.
     *
     * @param world  the chunk's world
     * @param chunkX the chunk's X
     * @param chunkZ the chunk's Z
     * @param x      local X, 0 to 15
     * @param y      block Y
     * @param z      local Z, 0 to 15
     * @param block  the block being written
     * @return {@code true} to drop the write; always {@code false} for a client world
     */
    public static boolean drops(World world, int chunkX, int chunkZ, int x, int y, int z, Block block) {
        // A client world never guards: in single player the client thread runs the same chunk setter while the
        // integrated server pushes and pops frames, and read twice the depth could change between the reads.
        if (world != null && world.isRemote) {
            return false;
        }
        int frames = depth;
        byte mode = topMode;
        if (frames == 0 || mode == OFF || mode == HOUSE) {
            return false;
        }
        boolean treePart = block != null && isTreePart(block);
        boolean snow = block != null && block.getMaterial() == Material.snow;
        if (mode == TREES && !treePart || mode == SNOW && !snow) {
            return false;
        }
        int top = frames - 1;
        if (WORLDS[top] != world) {
            return false;
        }
        com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan chunk = PLANS[top].chunk(chunkX, chunkZ);
        if ((treePart || snow) && y > chunk.groundY() && y <= chunk.groundY() + 2 && chunk.keepsClear(x & 15, z & 15)) {
            // A door's approach stays open whoever writes into it, in or out of the footprint: a park's tree crown
            // or a snow layer in the doorway blocked the street doors (known issue of 0.1.0-beta).
            dropped++;
            return true;
        }
        if (mode == SNOW) {
            return false;
        }
        if (y < chunk.footprintBottom(x & 15, z & 15) && !chunk.inGuardBand(y)) {
            // The guarded band (M06): a highway tunnel's or a railway's Y range, in every column of the chunk.
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
        int frames = depth;
        if (frames == 0 || world != null && world.isRemote
            || !(block instanceof BlockLog || block instanceof BlockLeavesBase)) {
            return;
        }
        int top = frames - 1;
        if (MODE[top] == HOUSE) {
            return;
        }
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

    /**
     * Returns whether a block is part of a tree as other mods plant them: logs and leaves by class, and anything of the
     * wood, leaves, plants, vine or cactus material (IC2's rubber wood, HarvestCraft's fruit, Natura's saguaros, which
     * its tree generator plants in deserts).
     *
     * @param block the block
     * @return whether a tree frame drops it
     */
    static boolean isTreePart(Block block) {
        if (block instanceof BlockLog || block instanceof BlockLeavesBase) {
            return true;
        }
        Material material = block.getMaterial();
        return material == Material.wood || material == Material.leaves
            || material == Material.plants
            || material == Material.vine
            || material == Material.cactus;
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
        topMode = OFF;
    }
}
