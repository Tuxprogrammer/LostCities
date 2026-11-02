package com.tuxprogrammer.lostcities.scenario;

import net.minecraft.world.WorldServer;

/**
 * What a running scenario may use: the overworld, its own arena and the tick count.
 *
 * <p>
 * The arena is a square of {@link #ARENA_CHUNKS} × {@link #ARENA_CHUNKS} chunks with its minimum corner at chunk
 * ({@link #arenaChunkX()}, {@link #arenaChunkZ()}). Each scenario of a run gets the next arena along X, far from spawn,
 * so on a fresh world every scenario generates terrain nobody has touched (testing-gtnh-mods: "run new worlds too").
 * A second run on the same world reuses the same arenas, which are then already generated.
 *
 * <p>
 * Side: server.
 */
public final class ScenarioContext {

    /** Width of an arena, in chunks. */
    public static final int ARENA_CHUNKS = 8;

    /** Chunk X of the first arena's minimum corner: 32,768 blocks from spawn along X. */
    public static final int FIRST_ARENA_CHUNK_X = 2048;

    /** Chunk Z of every arena's minimum corner: 32,768 blocks from spawn along Z. */
    public static final int ARENA_CHUNK_Z = 2048;

    /** Distance between arena corners along X, in chunks: an arena plus a gap of the same width. */
    public static final int ARENA_SPACING_CHUNKS = 2 * ARENA_CHUNKS;

    private final WorldServer world;
    private final int arenaChunkX;
    private int ticksRun;

    /**
     * Creates the context of one scenario.
     *
     * @param world      the overworld
     * @param arenaIndex the scenario's position in its run, from 0
     */
    ScenarioContext(WorldServer world, int arenaIndex) {
        this.world = world;
        this.arenaChunkX = FIRST_ARENA_CHUNK_X + arenaIndex * ARENA_SPACING_CHUNKS;
    }

    /** Returns the overworld. */
    public WorldServer world() {
        return world;
    }

    /** Returns the chunk X of the arena's minimum corner. */
    public int arenaChunkX() {
        return arenaChunkX;
    }

    /** Returns the chunk Z of the arena's minimum corner. */
    public int arenaChunkZ() {
        return ARENA_CHUNK_Z;
    }

    /** Returns how many times {@link Scenario#tick} has been called. */
    public int ticksRun() {
        return ticksRun;
    }

    /** Counts one more tick; the runner calls it before {@link Scenario#tick}. */
    void advanceTick() {
        ticksRun++;
    }

    /**
     * Fails the scenario unless a condition holds.
     *
     * @param condition what must be true
     * @param observed  what was expected and what was observed, used as the failure message
     * @throws ScenarioFailure when {@code condition} is false
     */
    public void check(boolean condition, String observed) {
        if (!condition) {
            throw new ScenarioFailure(observed);
        }
    }
}
