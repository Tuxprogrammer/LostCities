package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
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
    private int topY;
    // The last block read and written: scans and fills meet long runs of one block.
    private Block lastReadBlock;
    private int lastReadMeta = -1;
    private BlockRef lastReadRef;
    private BlockRef lastWrittenRef;
    private Block lastWrittenBlock;

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
        this.topY = -1;
        for (int i = sections.length - 1; i >= 0; i--) {
            if (sections[i] != null && !sections[i].isEmpty()) {
                topY = (i << 4) + 15;
                break;
            }
        }
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
        int meta = section.getExtBlockMetadata(x, y & 15, z);
        if (block == lastReadBlock && meta == lastReadMeta) {
            return lastReadRef;
        }
        BlockRef ref = refs.ref(block, meta);
        lastReadBlock = block;
        lastReadMeta = meta;
        lastReadRef = ref;
        return ref;
    }

    @Override
    public int topY() {
        return Math.max(topY, 0);
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
        } else if (block.isAir() && holdsAir(section, x, y & 15, z, block.meta())) {
            // Air over air changes nothing (D-PF-2): a building's interior air and a cleared range above the ground
            // mostly meet air, and the section setter's block-ID lookup is most of a write's cost.
            return;
        }
        if (!block.isAir() && y > topY) {
            topY = (y & ~15) + 15;
        }
        if (block != lastWrittenRef) {
            lastWrittenBlock = refs.block(block);
            lastWrittenRef = block;
        }
        section.func_150818_a(x, y & 15, z, lastWrittenBlock);
        section.setExtBlockMetadata(x, y & 15, z, block.meta());
        writes++;
    }

    /**
     * Finishes the chunk after the terrain phase: marks it modified when anything was written. The height map and sky
     * light are the caller's to recompute ({@link #needsLight()}), because lowered and blasted columns must lower their
     * height too (TERR-13): the overlay's provider lights every chunk once, after the terrain phase
     * ({@link SkylightDeferral}).
     */
    public void finish() {
        if (writes > 0) {
            chunk.setChunkModified();
        }
    }

    /** Returns whether the chunk was written since it was attached, so its height map and sky light are stale. */
    public boolean needsLight() {
        return writes > 0;
    }

    /** Returns whether a section position holds air with the given metadata. */
    private static boolean holdsAir(ExtendedBlockStorage section, int x, int y, int z, int meta) {
        return section.getBlockByExtId(x, y, z) == Blocks.air && section.getExtBlockMetadata(x, y, z) == meta;
    }

    private static void check(int x, int y, int z) {
        if (!ChunkBuffer.contains(x, y, z)) {
            throw new IndexOutOfBoundsException("(" + x + ", " + y + ", " + z + ") is outside the chunk column");
        }
    }
}
