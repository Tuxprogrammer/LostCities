package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Translates between {@link BlockRef} names and the live block registry, once per block: a block met in a chunk is
 * named through the registry the first time and served from an array afterwards, and a {@code BlockRef} the city
 * writes is resolved once and then looked up by identity. Unknown names resolve to air with one warning, so a missing
 * pack mod never crashes generation (CONTRIBUTING § World generation rules, "Blocks by name").
 *
 * <p>
 * Owned by one wrapped world and dropped with it. Server thread only. Side: server.
 */
public final class BlockRefs {

    /** The highest block ID the array covers: EndlessIDs' 16-bit range. */
    private static final int MAX_ID = 1 << 16;

    private BlockRef[][] byId = new BlockRef[4096][];
    private final Map<BlockRef, Block> byRef = new IdentityHashMap<>();
    private final Map<BlockRef, Block> byValue = new HashMap<>();

    /**
     * Returns the reference for a block and metadata met in a chunk.
     *
     * @param block the block
     * @param meta  its metadata, 0 to 15
     * @return the canonical reference; the same instance every time
     */
    public BlockRef ref(Block block, int meta) {
        if (block == Blocks.air) {
            return BlockRef.AIR;
        }
        int id = Block.getIdFromBlock(block);
        if (id < 0 || id >= MAX_ID) {
            return named(block, meta);
        }
        if (id >= byId.length) {
            BlockRef[][] grown = new BlockRef[Math.min(MAX_ID, Math.max(id + 1, byId.length * 2))][];
            System.arraycopy(byId, 0, grown, 0, byId.length);
            byId = grown;
        }
        BlockRef[] metas = byId[id];
        if (metas == null) {
            metas = new BlockRef[16];
            byId[id] = metas;
        }
        BlockRef ref = metas[meta & 15];
        if (ref == null) {
            ref = named(block, meta & 15);
            metas[meta & 15] = ref;
            byRef.put(ref, block);
        }
        return ref;
    }

    /**
     * Returns the block for a reference the city writes.
     *
     * @param ref the reference
     * @return the block; air when the name is not registered
     */
    public Block block(BlockRef ref) {
        Block block = byRef.get(ref);
        if (block != null) {
            return block;
        }
        block = byValue.get(ref);
        if (block == null) {
            block = resolve(ref);
            byValue.put(ref, block);
        }
        byRef.put(ref, block);
        return block;
    }

    /**
     * Binds a reference to a block without the registry: a test seam, since unit tests register nothing.
     *
     * @param ref   the reference
     * @param block the block it resolves to
     */
    void bind(BlockRef ref, Block block) {
        byValue.put(ref, block);
        byRef.put(ref, block);
    }

    /**
     * Returns whether a reference names a registered block.
     *
     * @param ref the reference
     * @return {@code true} when the registry knows the name
     */
    public static boolean exists(BlockRef ref) {
        return ref.isAir() || Block.blockRegistry.containsKey(ref.name());
    }

    private static Block resolve(BlockRef ref) {
        if (ref.isAir()) {
            return Blocks.air;
        }
        if (!Block.blockRegistry.containsKey(ref.name())) {
            LostCities.LOG.warn("Block {} is not registered; writing air instead", ref);
            return Blocks.air;
        }
        return (Block) Block.blockRegistry.getObject(ref.name());
    }

    private static BlockRef named(Block block, int meta) {
        Object name = Block.blockRegistry.getNameForObject(block);
        if (name == null) {
            return BlockRef.AIR;
        }
        return BlockRef.of(name.toString(), meta & 15);
    }
}
