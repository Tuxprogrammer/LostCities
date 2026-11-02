package com.tuxprogrammer.lostcities.core.api;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;

import com.tuxprogrammer.lostcities.api.ILostChunkBlocks;
import com.tuxprogrammer.lostcities.api.ILostCityInformation;
import com.tuxprogrammer.lostcities.api.event.LostCityEvent;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.overlay.BlockRefs;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.terrain.ChunkEvents;

/**
 * Posts the API's four generation events on {@code MinecraftForge.EVENT_BUS} from a live world's terrain phase
 * (docs/api.md, Events; docs/new-spec/01-core/index.md, API). The chunk's blocks are handed over as
 * {@link ILostChunkBlocks}, writing into the chunk buffer, inside the chunk only. With no listener the events change
 * nothing, so the fresh-world hashes stay as they are.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class ForgeChunkEvents implements ChunkEvents {

    private final OverlayWorld overlay;
    private final BlockRefs refs;
    private ILostCityInformation info;

    private ForgeChunkEvents(OverlayWorld overlay) {
        this.overlay = overlay;
        this.refs = overlay.refs();
    }

    /**
     * Returns the hooks of a world with real cities.
     *
     * @param overlay the world's overlay
     * @return the hooks
     */
    public static ChunkEvents forWorld(OverlayWorld overlay) {
        return new ForgeChunkEvents(overlay);
    }

    private ILostCityInformation info() {
        if (info == null) {
            info = new LostCityInformationImpl(new OverlaySource(overlay));
        }
        return info;
    }

    @Override
    public boolean preCity(ChunkBuffer buffer, int chunkX, int chunkZ) {
        return !MinecraftForge.EVENT_BUS
            .post(new LostCityEvent.PreGenCityChunk(overlay.world(), info(), chunkX, chunkZ, blocks(buffer)));
    }

    @Override
    public void postCity(ChunkBuffer buffer, int chunkX, int chunkZ) {
        MinecraftForge.EVENT_BUS
            .post(new LostCityEvent.PostGenCityChunk(overlay.world(), info(), chunkX, chunkZ, blocks(buffer)));
    }

    @Override
    public void postOutside(ChunkBuffer buffer, int chunkX, int chunkZ) {
        MinecraftForge.EVENT_BUS
            .post(new LostCityEvent.PostGenOutsideChunk(overlay.world(), info(), chunkX, chunkZ, blocks(buffer)));
    }

    @Override
    public boolean preExplosion(ChunkBuffer buffer, int chunkX, int chunkZ) {
        return !MinecraftForge.EVENT_BUS
            .post(new LostCityEvent.PreExplosion(overlay.world(), info(), chunkX, chunkZ, blocks(buffer)));
    }

    private ILostChunkBlocks blocks(ChunkBuffer buffer) {
        return new BufferBlocks(buffer, refs);
    }

    /** A chunk buffer as the API shows it. */
    static final class BufferBlocks implements ILostChunkBlocks {

        private final ChunkBuffer buffer;
        private final BlockRefs refs;

        BufferBlocks(ChunkBuffer buffer, BlockRefs refs) {
            this.buffer = buffer;
            this.refs = refs;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            if (!ChunkBuffer.contains(x, y, z)) {
                return Blocks.air;
            }
            return refs.block(buffer.get(x, y, z));
        }

        @Override
        public int getMeta(int x, int y, int z) {
            if (!ChunkBuffer.contains(x, y, z)) {
                return 0;
            }
            return buffer.get(x, y, z)
                .meta();
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            if (block == null || !ChunkBuffer.contains(x, y, z)) {
                return;
            }
            BlockRef ref = refs.ref(block, meta);
            buffer.set(x, y, z, ref);
        }
    }
}
