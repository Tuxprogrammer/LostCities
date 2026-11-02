package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * Where an {@code editMode} world's terrain phase records each part it writes, for the part editor
 * (docs/new-spec/01-core/index.md, Part editor). The writers call it only for real writes, never for the population
 * dry run, and only on the server thread that runs the terrain phase (avoids CORE-34). A record never feeds
 * generation, so a world with or without records generates the same blocks.
 *
 * <p>
 * Side: both (plain Java).
 */
public interface PartRecorder {

    /** Records nothing: every world without {@code editMode}. */
    PartRecorder NONE = new PartRecorder() {

        @Override
        public void beginChunk(int chunkX, int chunkZ) {}

        @Override
        public void record(int chunkX, int chunkZ, AssetId part, int y, Transform transform, int offsetX,
            int offsetZ) {}
    };

    /**
     * Starts a chunk's records, dropping any the chunk had, so a chunk written twice keeps one list.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    void beginChunk(int chunkX, int chunkZ);

    /**
     * Records one part, in the order it was written.
     *
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param part      the part's id
     * @param y         block Y of the part's first slice
     * @param transform the part's transform
     * @param offsetX   local X of the part's origin in the chunk
     * @param offsetZ   local Z of the part's origin in the chunk
     */
    void record(int chunkX, int chunkZ, AssetId part, int y, Transform transform, int offsetX, int offsetZ);
}
