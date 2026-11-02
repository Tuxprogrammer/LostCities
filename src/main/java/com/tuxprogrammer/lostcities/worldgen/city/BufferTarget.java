package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ContentContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.CityTerrain;

/**
 * The terrain phase's {@link CityTarget}: writes into the chunk buffer, inside the chunk only and never in a soft-edge
 * strip column (0006 §4), and ignores jobs, which the plan lists from its dry run.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class BufferTarget implements CityTarget {

    private final ChunkBuffer buffer;
    private final ContentContext context;

    /**
     * Creates the target of one chunk write.
     *
     * @param buffer  the chunk
     * @param context the chunk's content context, for its soft-edge columns; {@code null} for none
     */
    public BufferTarget(ChunkBuffer buffer, ContentContext context) {
        this.buffer = buffer;
        this.context = context;
    }

    @Override
    public boolean dryRun() {
        return false;
    }

    @Override
    public BlockRef get(int x, int y, int z) {
        return ChunkBuffer.contains(x, y, z) ? buffer.get(x, y, z) : BlockRef.AIR;
    }

    @Override
    public void set(int x, int y, int z, BlockRef block) {
        if (!ChunkBuffer.contains(x, y, z) || context != null && context.isSoftEdge(x, z)) {
            return;
        }
        buffer.set(x, y, z, block);
    }

    @Override
    public void clearToMax(int fromY) {
        if (context != null) {
            CityTerrain.clearToMax(buffer, context.plan(), fromY);
        }
    }

    @Override
    public void torch(int x, int y, int z) {}

    @Override
    public void chest(int x, int y, int z, PaletteEntry info, TileTemplate template, Transform transform,
        PartWriter.Placement placement) {}

    @Override
    public void spawner(int x, int y, int z, PaletteEntry info, PartWriter.Placement placement) {}

    @Override
    public void tileEntity(int x, int y, int z, TileTemplate template, Transform transform) {}
}
