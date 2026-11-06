package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;

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
 * No block whose light opacity reads the world is ever written: a GT machine goes in as its stand-in and reads back as
 * the machine, any other such block is dropped ({@link TerrainStandIns}).
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class WorldChunkBuffer implements ChunkBuffer {

    /**
     * Debug aid ({@code -Dlostcitiesnh.debugWool=true}): every solid block the mod writes is red wool, so what it
     * built stands out from the terrain it sits in. Never on in play.
     */
    private static final boolean DEBUG_WOOL = Boolean.getBoolean("lostcitiesnh.debugWool");
    /** The debug wool's color; the terrain phase sets it per stage so a screenshot shows which stage built what. */
    public static volatile int debugWoolMeta = 1;
    /** Slots of the written-block cache; a power of two. */
    private static final int WRITABLE_SLOTS = 64;

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
    private Block standIn;
    private final BlockRef[] machineRefs = new BlockRef[16];
    // What each recently written reference writes, direct-mapped by identity (D381): a building alternates a
    // handful of blocks, and the registry and opacity lookups behind writable() were a twentieth of the terrain phase.
    private final BlockRef[] writableKeys = new BlockRef[WRITABLE_SLOTS];
    private final Block[] writableBlocks = new Block[WRITABLE_SLOTS];
    private final Set<String> dropped = new HashSet<>();

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
        BlockRef ref = isStandIn(block) ? machineRef(meta) : refs.ref(block, meta);
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
            // Air over air changes nothing (D264): a building's interior air and a cleared range above the ground
            // mostly meet air, and the section setter's block-ID lookup is most of a write's cost.
            return;
        }
        if (!block.isAir() && y > topY) {
            topY = (y & ~15) + 15;
        }
        if (block != lastWrittenRef) {
            lastWrittenBlock = cachedWritable(block);
            lastWrittenRef = block;
        }
        Block written = lastWrittenBlock;
        if (written == null) {
            return;
        }
        int writtenMeta = block.meta();
        if (DEBUG_WOOL && !block.isAir()
            && !written.getMaterial()
                .isLiquid()) {
            written = net.minecraft.init.Blocks.wool;
            writtenMeta = debugWoolMeta;
        }
        section.func_150818_a(x, y & 15, z, written);
        section.setExtBlockMetadata(x, y & 15, z, writtenMeta);
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

    /**
     * Returns the block this buffer writes for a reference: its registered block, the stand-in for a GT machine, or
     * {@code null} for any other block whose light opacity reads the world ({@link TerrainStandIns}).
     *
     * @param ref the reference
     * @return the block to write, or {@code null} to write nothing
     */
    Block writable(BlockRef ref) {
        return cachedWritable(ref);
    }

    /**
     * {@link #writable} through a small direct-mapped cache keyed by the reference's identity. The answer for a
     * reference never changes in a world, so the cache changes no block written; it is bounded and belongs to this
     * buffer, which belongs to the world. A slot holding {@code null} for a block not written is told apart from an
     * empty slot by its key.
     */
    private Block cachedWritable(BlockRef ref) {
        int slot = System.identityHashCode(ref) & (WRITABLE_SLOTS - 1);
        if (writableKeys[slot] == ref) {
            return writableBlocks[slot];
        }
        Block out = resolveWritable(ref);
        writableKeys[slot] = ref;
        writableBlocks[slot] = out;
        return out;
    }

    private Block resolveWritable(BlockRef ref) {
        Block block = refs.block(ref);
        Block out = TerrainStandIns.terrainBlock(block, ref.name(), standIn());
        if (out == null && dropped.add(ref.name())) {
            LostCities.LOG.warn(
                "Block {} reads the world for its light opacity, which would regenerate the chunk; not written",
                ref.name());
        }
        return out;
    }

    /**
     * Swaps the GT machine stand-ins between two levels back to the machine, at the same metadata, straight in the
     * chunk's sections. Run by the population phase, where the chunk is loaded, before the machines' tile entities
     * are made. The stand-in's opacity is the machine's, so the chunk's light stays right; nothing outside the chunk
     * is read or told.
     *
     * @param minY the lowest level, inclusive
     * @param maxY the highest level, inclusive
     * @return the number of blocks swapped
     */
    public int restoreStandIns(int minY, int maxY) {
        Block marker = standIn();
        Block machine = refs.block(BlockRef.of(FurnishItem.GT_MACHINES, 0));
        if (!isStandIn(marker) || machine == null || machine == Blocks.air) {
            return 0;
        }
        int swapped = 0;
        for (int y = Math.max(minY, 0); y <= Math.min(maxY, 255); y++) {
            ExtendedBlockStorage section = sections[y >> 4];
            if (section == null) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (section.getBlockByExtId(x, y & 15, z) == marker) {
                        int meta = section.getExtBlockMetadata(x, y & 15, z);
                        section.func_150818_a(x, y & 15, z, machine);
                        section.setExtBlockMetadata(x, y & 15, z, meta);
                        swapped++;
                    }
                }
            }
        }
        if (swapped > 0) {
            lastReadBlock = null;
            lastReadMeta = -1;
            chunk.setChunkModified();
        }
        return swapped;
    }

    /** Whether a block met in the chunk is the GT machine stand-in; never air, should the stand-in not resolve. */
    private boolean isStandIn(Block block) {
        return block != null && block != Blocks.air && block == standIn();
    }

    /** Returns the GT machine a stand-in at a metadata reads as. */
    private BlockRef machineRef(int meta) {
        BlockRef ref = machineRefs[meta & 15];
        if (ref == null) {
            ref = BlockRef.of(FurnishItem.GT_MACHINES, meta & 15);
            machineRefs[meta & 15] = ref;
        }
        return ref;
    }

    private Block standIn() {
        if (standIn == null) {
            standIn = refs.block(TerrainStandIns.STAND_IN_REF);
        }
        return standIn;
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
