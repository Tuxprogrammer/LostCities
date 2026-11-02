package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.List;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.GroundFit;

/**
 * Sets the ground under and around scattered structures into RWG's terrain (design record 0009): each column within
 * reach of a structure's pads moves from its own surface toward the structure's ground Y by the fit's weight, lowered
 * with {@link ColumnOps#moveDown} (which keeps the surface layers and keeps water joined, TERR-23 and TERR-39) or
 * raised with {@link ColumnOps#moveUp}. The pads and their walkway end exactly at the ground Y, so nothing stands
 * inside a
 * cabin and no foundation floats; the fade ends on RWG's own ground, so no cliff meets it.
 *
 * <p>
 * Deviation (new-spec 07-scattered, "Ground under a structure"): upstream wrote scattered parts over the terrain as it
 * was, so a cabin could stand in a pit or on a step and a gas station's pavement could cut a wall into a hill.
 *
 * <p>
 * Reads and writes only the chunk's own columns and allocates nothing per column. Side: both (plain Java).
 */
public final class ScatteredGround {

    private ScatteredGround() {}

    /**
     * Applies the fits that reach a chunk, in order.
     *
     * @param buffer  the chunk
     * @param context the world's terrain context
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @param fits    the fits, in the plan's order
     * @return the columns moved
     */
    public static int apply(ChunkBuffer buffer, TerrainContext context, int chunkX, int chunkZ, List<GroundFit> fits) {
        int moved = 0;
        for (GroundFit fit : fits) {
            moved += apply(buffer, context, chunkX, chunkZ, fit);
        }
        return moved;
    }

    /**
     * Applies one fit to a chunk.
     *
     * @param buffer  the chunk
     * @param context the world's terrain context
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @param fit     the fit
     * @return the columns moved
     */
    public static int apply(ChunkBuffer buffer, TerrainContext context, int chunkX, int chunkZ, GroundFit fit) {
        if (!fit.reaches(chunkX, chunkZ)) {
            return 0;
        }
        BlockClassifier classes = context.classifier();
        int waterLevel = context.settings()
            .waterLevel();
        int moved = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int blockX = (chunkX << 4) + x;
                int blockZ = (chunkZ << 4) + z;
                if (fit.beyond(blockX, blockZ)) {
                    continue;
                }
                boolean onPad = fit.distance(blockX, blockZ) <= GroundFit.WALKWAY;
                int surface = ColumnOps.surface(buffer, classes, x, z, true);
                if (surface < WorldLimits.MIN_Y) {
                    continue;
                }
                int target = WorldLimits.clampY(fit.target(surface, blockX, blockZ));
                int touched = ColumnOps.NOT_TOUCHED;
                if (target < surface) {
                    touched = ColumnOps
                        .moveDown(buffer, classes, x, z, target + 1, WorldLimits.BUILD_HEIGHT, waterLevel);
                } else if (target > surface) {
                    touched = ColumnOps.moveUp(buffer, classes, x, z, target);
                }
                if (onPad && clearPlants(buffer, classes, x, z)) {
                    touched = Math.max(touched, 0);
                }
                if (touched != ColumnOps.NOT_TOUCHED) {
                    moved++;
                }
            }
        }
        return moved;
    }

    /**
     * Clears the plants, logs and leaves standing on a pad or walkway column, which leveling leaves where the ground
     * was already at the structure's ground: a tree would otherwise stand inside the structure, whose spaces keep the
     * world's blocks.
     *
     * @return whether a block was cleared
     */
    static boolean clearPlants(ChunkBuffer buffer, BlockClassifier classes, int x, int z) {
        int surface = ColumnOps.surface(buffer, classes, x, z, true);
        boolean cleared = false;
        for (int y = Math.min(buffer.topY(), WorldLimits.MAX_Y); y > surface; y--) {
            BlockRef block = buffer.get(x, y, z);
            if (!block.isAir() && !classes.isLiquid(block) && classes.isFoliageOrEmpty(block)) {
                buffer.set(x, y, z, BlockRef.AIR);
                cleared = true;
            }
        }
        return cleared;
    }
}
