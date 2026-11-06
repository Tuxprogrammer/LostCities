package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.core.assets.GenerationSafety;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;

/**
 * Keeps blocks whose light opacity reads the world out of a chunk while {@code provideChunk} builds it.
 *
 * <p>
 * The overlay lights each chunk once, with {@code Chunk.generateSkylightMap}, before the chunk is in the server's
 * chunk map. That pass asks every block for {@code getLightOpacity(IBlockAccess, x, y, z)}. GT's
 * {@code BlockMachines} answers by reading its tile entity through the world (GT5-Unofficial 5.09.54.265,
 * {@code BlockMachines.getLightOpacity}), the world asks the provider for the chunk, which is not in the map yet, and
 * generation starts over: a {@code StackOverflowError} on a fresh RWG2 world (decision D-gtl-1).
 *
 * <p>
 * So the terrain phase writes a GT machine as a stand-in, {@value #STAND_IN} at the machine's metadata: a plain opaque
 * cube whose opacity, 255, is the one a machine without a light has, so the sky light it gets is the machine's. The
 * mod never places that block itself ({@code blockmap/never_place.json}, "infested"). Reads through
 * {@link WorldChunkBuffer} see the stand-in as the machine, so every pass behaves as it did with the machine in place,
 * and the population phase, where the chunk is loaded, swaps each stand-in back before it creates the machine's tile
 * entity ({@link WorldChunkBuffer#restoreStandIns}). Any other block whose opacity reads the world is not written at
 * all during the terrain phase, unless {@link GenerationSafety} lists it as read and safe.
 *
 * <p>
 * Side: server. Thread-safe.
 */
public final class TerrainStandIns {

    /** The block a GT machine is written as during the terrain phase. */
    public static final String STAND_IN = "minecraft:monster_egg";

    /** The stand-in's reference at metadata 0, as {@link BlockRefs} resolves it. */
    static final BlockRef STAND_IN_REF = BlockRef.of(STAND_IN, 0);

    private static final Map<Class<?>, Boolean> READS_WORLD = new ConcurrentHashMap<>();

    private TerrainStandIns() {}

    /**
     * Returns whether a block's light opacity may depend on the world: whether a mod class of it overrides Forge's
     * {@code Block.getLightOpacity(IBlockAccess, int, int, int)}, read from its bytecode
     * ({@link GenerationSafety#declaresWorldOpacity}; reflection would resolve client-only signatures on a server).
     * Such a block may read a tile entity or a neighbor through the world, which is not safe before the chunk is in the
     * chunk map.
     *
     * @param block the block
     * @return {@code true} when the opacity lookup is the block's own
     */
    public static boolean readsWorld(Block block) {
        return block != null && readsWorld(block.getClass());
    }

    /**
     * As {@link #readsWorld(Block)}, for a block class.
     *
     * @param type the block class
     * @return {@code true} when the class or a mod superclass below {@code Block} overrides the lookup
     */
    public static boolean readsWorld(Class<?> type) {
        return READS_WORLD.computeIfAbsent(type, GenerationSafety::declaresWorldOpacity);
    }

    /**
     * Returns the block the terrain phase writes for a block: the block itself when its opacity does not read the
     * world or was read and cleared ({@link GenerationSafety#isChecked}), the stand-in for a GT machine, and
     * {@code null}, nothing, for any other block whose opacity does.
     *
     * @param block   the block the city wants
     * @param name    its registry name
     * @param standIn the stand-in block
     * @return the block to write, or {@code null} to write nothing
     */
    public static Block terrainBlock(Block block, String name, Block standIn) {
        if (!readsWorld(block)) {
            return block;
        }
        if (FurnishItem.GT_MACHINES.equals(name)) {
            return standIn;
        }
        return GenerationSafety.isChecked(name) ? block : null;
    }

}
