package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.World;

/**
 * Defers the delegate's sky light pass on the chunk it is generating, so a chunk the terrain phase writes is lit
 * once, after the writes, instead of twice. RWG2 and classic RWG both end {@code provideChunk} with
 * {@code chunk.generateSkylightMap()} and return the chunk (RWG2 2.0.6 ChunkGeneratorRealistic.java#L262, RWG
 * alpha-1.5.3 ChunkGeneratorRealistic.java#L203); the overlay's provider then lights the chunk itself, whether or not
 * the terrain phase wrote to it, so every chunk still gets exactly one pass.
 *
 * <p>
 * Only the one chunk the active call asked for, in the active call's world, is deferred: a sky light pass on any other
 * chunk runs as usual. A nested {@code provideChunk} on the same thread does not defer. Server thread only.
 * Side: server.
 */
public final class SkylightDeferral {

    private static World world;
    private static int chunkX;
    private static int chunkZ;
    private static boolean active;
    private static boolean skipped;

    private SkylightDeferral() {}

    /**
     * Starts deferring the sky light pass of one chunk.
     *
     * @param target the world being generated
     * @param x      chunk X
     * @param z      chunk Z
     * @return whether this call defers; {@code false} inside another deferral, which then must not be ended here
     */
    static boolean begin(World target, int x, int z) {
        if (active) {
            return false;
        }
        world = target;
        chunkX = x;
        chunkZ = z;
        skipped = false;
        active = true;
        return true;
    }

    /**
     * Stops deferring.
     *
     * @return whether a sky light pass of the chunk was skipped, which the caller then owes the chunk
     */
    static boolean end() {
        boolean result = skipped;
        active = false;
        skipped = false;
        world = null;
        return result;
    }

    /**
     * Called at the head of {@code Chunk.generateSkylightMap}: whether to skip this pass.
     *
     * @param target the chunk's world
     * @param x      the chunk's X
     * @param z      the chunk's Z
     * @return {@code true} when the pass belongs to the chunk being deferred
     */
    public static boolean skips(World target, int x, int z) {
        if (!active || target != world || x != chunkX || z != chunkZ) {
            return false;
        }
        skipped = true;
        return true;
    }
}
