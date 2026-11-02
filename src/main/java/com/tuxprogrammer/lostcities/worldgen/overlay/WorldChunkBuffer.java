package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;

/**
 * A {@link ChunkBuffer} backed by a real chunk's {@code ExtendedBlockStorage} sections: the terrain phase writes RWG's
 * finished chunk in place through the vanilla section setters, which EndlessIDs patches for its 16-bit IDs
 * (docs/design/0005-rwg-overlay.md §2). Missing sections are created on the first non-air write. The {@code Chunk}
 * object is never replaced, because EndlessIDs keeps its 16-bit biome array on it, and biomes are never touched.
 *
 * <p>
 * No lighting or block updates happen here; the terrain phase recomputes the height map and sky light once at the
 * end. Reads and writes allocate nothing once each block has been seen ({@link BlockRefs}). Positions outside the
 * chunk are rejected, never aliased (TERR-14).
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class WorldChunkBuffer implements ChunkBuffer {

    private final BlockRefs refs;
    private Chunk chunk;
    private ExtendedBlockStorage[] sections;
    private boolean sky;
    private int writes;

    /**
     * Creates a buffer that can be pointed at chunks one after another.
     *
     * @param refs the world's block translations
     */
    public WorldChunkBuffer(BlockRefs refs) {
        this.refs = refs;
    }

    /**
     * Points the buffer at a chunk.
     *
     * @param target the chunk to read and write
     * @return this buffer
     */
    public WorldChunkBuffer attach(Chunk target) {
        this.chunk = target;
        this.sections = target.getBlockStorageArray();
        this.sky = !target.worldObj.provider.hasNoSky;
        this.writes = 0;
        return this;
    }

    /** Returns the number of blocks written since {@link #attach}. */
    public int writes() {
        return writes;
    }

    @Override
    public BlockRef get(int x, int y, int z) {
        check(x, y, z);
        ExtendedBlockStorage section = sections[y >> 4];
        if (section == null) {
            return BlockRef.AIR;
        }
        Block block = section.getBlockByExtId(x, y & 15, z);
        return refs.ref(block, section.getExtBlockMetadata(x, y & 15, z));
    }

    @Override
    public void set(int x, int y, int z, BlockRef block) {
        check(x, y, z);
        if (block == null) {
            throw new NullPointerException("block");
        }
        ExtendedBlockStorage section = sections[y >> 4];
        if (section == null) {
            if (block.isAir()) {
                return;
            }
            section = new ExtendedBlockStorage(y & ~15, sky);
            sections[y >> 4] = section;
        }
        section.func_150818_a(x, y & 15, z, refs.block(block));
        section.setExtBlockMetadata(x, y & 15, z, block.meta());
        writes++;
    }

    /**
     * Finishes the chunk after the terrain phase: recomputes the height map and sky light, because lowered and
     * blasted columns must lower their height too (TERR-13), and marks the chunk modified.
     */
    public void finish() {
        if (writes > 0) {
            chunk.generateSkylightMap();
            chunk.setChunkModified();
        }
    }

    private static void check(int x, int y, int z) {
        if (!ChunkBuffer.contains(x, y, z)) {
            throw new IndexOutOfBoundsException("(" + x + ", " + y + ", " + z + ") is outside the chunk column");
        }
    }
}
