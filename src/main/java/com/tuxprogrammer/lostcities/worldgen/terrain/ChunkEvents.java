package com.tuxprogrammer.lostcities.worldgen.terrain;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;

/**
 * The terrain phase's four API hooks, upstream's generation events (docs/original-spec/01-core/api.md, Events;
 * docs/api.md). The overlay of a live world posts them on Forge's bus ({@code core.api.ForgeChunkEvents}); tests and
 * worlds without listeners use {@link #NONE}. With no listener nothing changes, so the hooks never move a block.
 *
 * <p>
 * Server thread only. Side: both (plain Java).
 */
public interface ChunkEvents {

    /** No hooks: every event goes ahead. */
    ChunkEvents NONE = new ChunkEvents() {};

    /**
     * Before a city chunk's building or street is written.
     *
     * @param buffer the chunk
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code false} to skip the building or street
     */
    default boolean preCity(ChunkBuffer buffer, int chunkX, int chunkZ) {
        return true;
    }

    /**
     * After a city chunk's building or street.
     *
     * @param buffer the chunk
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    default void postCity(ChunkBuffer buffer, int chunkX, int chunkZ) {}

    /**
     * After a chunk outside every city.
     *
     * @param buffer the chunk
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    default void postOutside(ChunkBuffer buffer, int chunkX, int chunkZ) {}

    /**
     * Before explosion damage and debris.
     *
     * @param buffer the chunk
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code false} to skip both
     */
    default boolean preExplosion(ChunkBuffer buffer, int chunkX, int chunkZ) {
        return true;
    }
}
