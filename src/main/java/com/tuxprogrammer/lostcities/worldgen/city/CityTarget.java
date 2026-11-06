package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * Where the city writers send their work. The same writing code runs twice per chunk: once against the chunk in the
 * terrain phase ({@link BufferTarget}), which places the blocks, and once as a dry run that only lists the population
 * jobs ({@link JobTarget}), so the jobs are a pure function of the plan and need no record of a previous write. A job
 * whose block a later step removed (a ruin, an explosion, a door) finds nothing in the population phase and does
 * nothing, as upstream's post-generation checks do.
 *
 * <p>
 * Coordinates are chunk-local X and Z and block Y. Side: both (plain Java).
 */
public interface CityTarget {

    /** Returns {@code true} for the dry run: writers skip steps that only read and write blocks. */
    boolean dryRun();

    /**
     * Returns a block; {@link BlockRef#AIR} in a dry run.
     *
     * @param x local X
     * @param y block Y
     * @param z local Z
     * @return the block
     */
    BlockRef get(int x, int y, int z);

    /**
     * Writes a block. Positions outside the chunk, outside Y 0 to 255 and in soft-edge strip columns are dropped.
     *
     * @param x     local X
     * @param y     block Y
     * @param z     local Z
     * @param block the block
     */
    void set(int x, int y, int z, BlockRef block);

    /**
     * Clears every column above a building's roof floor before the roof part is written (upstream's
     * {@code clearToMax}); nothing in a dry run.
     *
     * @param fromY the roof floor's Y
     */
    void clearToMax(int fromY);

    /**
     * Notes a torch character's torch, whose attachment the population phase fixes.
     *
     * @param x local X
     * @param y block Y
     * @param z local Z
     */
    void torch(int x, int y, int z);

    /**
     * Notes a loot character's container.
     *
     * @param x         local X
     * @param y         block Y
     * @param z         local Z
     * @param info      the palette entry with the loot condition
     * @param template  the container's tile-entity template, or {@code null}
     * @param transform the part's transform, which turns facings in the template
     * @param placement the part being written, for the loot condition's context
     */
    void chest(int x, int y, int z, PaletteEntry info, TileTemplate template, Transform transform,
        PartWriter.Placement placement);

    /**
     * Notes a spawner character's spawner.
     *
     * @param x         local X
     * @param y         block Y
     * @param z         local Z
     * @param info      the palette entry with the mob condition
     * @param placement the part being written, for the mob condition's context
     */
    void spawner(int x, int y, int z, PaletteEntry info, PartWriter.Placement placement);

    /**
     * Notes a block whose tile entity must exist, with its template.
     *
     * @param x         local X
     * @param y         block Y
     * @param z         local Z
     * @param template  the template, or {@code null} for an empty tile entity
     * @param transform the part's transform, which turns facings in the template
     */
    void tileEntity(int x, int y, int z, TileTemplate template, Transform transform);

    /**
     * Notes a furnished floor, whose tile entities and GT machines the population phase completes (design record
     * 0016). Only the dry run lists it; a real write does nothing.
     *
     * @param y        the floor's standing level
     * @param argument the floor's room kind and salt, as {@code FurnishJobs.argument} writes them
     */
    default void furnish(int y, String argument) {}
}
