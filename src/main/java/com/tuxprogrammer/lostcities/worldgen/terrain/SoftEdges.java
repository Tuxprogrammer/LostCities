package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Arrays;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/**
 * Soft-edge strips: on each side of a city chunk that faces a landmark core, a strip of about
 * {@code softEdgeWidth} columns replaces upstream's border wall and ramps the ground linearly from the city ground to
 * the column's own RWG height at the core's edge (rules E2 to E4 of docs/new-spec/00-pack-integration/coexistence.md,
 * adopted by docs/design/0006-build-around-other-worldgen.md §4; the ramp shape is P03-13, D31).
 *
 * <ul>
 * <li>Width: {@code softEdgeWidth}, varied by up to ±2 per position along the side by the transition noise (E4).</li>
 * <li>Outer edge: the edge column's own surface before leveling, read from this chunk's RWG columns; inner edge: the
 * city ground (E2).</li>
 * <li>Steep step: the strip absorbs at most two blocks of height per column. A larger step leaves the rest to a
 * retaining column of the city style's border block at the strip's inner edge, no taller than the remaining step
 * (E3).</li>
 * <li>Written only in this chunk's own columns, so nothing is written into the core.</li>
 * </ul>
 *
 * <p>
 * This class computes per-column level targets before the city's leveling runs ({@link #targets}); leveling then
 * moves each strip column to its target instead of the city ground, keeping the column's own top block, and
 * {@link #retainingWalls} adds the walls afterwards. Side: both (plain Java).
 */
public final class SoftEdges {

    /** Most a strip's width varies from {@code softEdgeWidth} along a side, in columns (E4). */
    static final int WIDTH_VARIATION = 2;

    /** Height a strip absorbs per column; beyond twice the strip's width a retaining wall takes the rest (E3). */
    static final int RISE_PER_COLUMN = 2;

    /** Marks a column outside every strip in {@link #targets}' depth array. */
    public static final int NOT_IN_STRIP = -1;

    private SoftEdges() {}

    /**
     * Returns whether a city chunk has any side facing a landmark core.
     *
     * @param west  the plan toward −X
     * @param east  the plan toward +X
     * @param north the plan toward −Z
     * @param south the plan toward +Z
     * @return {@code true} when at least one is a core
     */
    public static boolean anyCoreSide(ChunkPlan west, ChunkPlan east, ChunkPlan north, ChunkPlan south) {
        return west.isCore() || east.isCore() || north.isCore() || south.isCore();
    }

    /**
     * Computes the leveling target of every column of a city chunk, with the strips toward cores.
     *
     * @param buffer  the chunk before leveling (RWG's columns)
     * @param context the world's terrain context
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @param plan    the chunk's plan
     * @param sides   the plans toward −X, +X, −Z and +Z
     * @param targets out: 256 target top Ys, index {@code z × 16 + x}
     * @param depth   out: 256 depths into a strip (0 at the core's edge), or {@link #NOT_IN_STRIP}
     * @param inner   out: 256 heights of the strip's inner end (the retaining wall's other side)
     */
    public static void targets(ChunkBuffer buffer, TerrainContext context, int chunkX, int chunkZ, ChunkPlan plan,
        ChunkPlan[] sides, int[] targets, int[] depth, int[] inner) {
        int ground = plan.groundY();
        Arrays.fill(targets, ground);
        Arrays.fill(depth, NOT_IN_STRIP);
        Arrays.fill(inner, ground);
        int baseWidth = context.settings()
            .softEdgeWidth();
        BlockClassifier classes = context.classifier();
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            if (!sides[side.ordinal()].isCore()) {
                continue;
            }
            for (int along = 0; along < 16; along++) {
                int edgeX = edgeX(side, along, 0);
                int edgeZ = edgeZ(side, along, 0);
                int outer = ColumnOps.surface(buffer, classes, edgeX, edgeZ, true);
                if (outer < WorldLimits.MIN_Y) {
                    outer = ground;
                }
                float noise = context.transitionNoise((chunkX << 4) + edgeX, (chunkZ << 4) + edgeZ);
                int variation = Math.max(-WIDTH_VARIATION, Math.min(WIDTH_VARIATION, Math.round(noise / 2f)));
                int width = Math.max(1, Math.min(15, baseWidth + variation));
                int step = outer - ground;
                int maxRamp = RISE_PER_COLUMN * width;
                int ramp = Math.max(-maxRamp, Math.min(maxRamp, step));
                int innerEnd = outer - ramp;
                for (int d = 0; d < width; d++) {
                    int x = edgeX(side, along, d);
                    int z = edgeZ(side, along, d);
                    int index = (z << 4) | x;
                    if (depth[index] != NOT_IN_STRIP && depth[index] <= d) {
                        // A corner column in two strips follows the side it is nearer to.
                        continue;
                    }
                    // Linear ramp (P03-13): the edge column keeps its own height, the inner end meets the city.
                    int target = innerEnd + Math.round(ramp * (width - d) / (float) width);
                    targets[index] = WorldLimits.clampY(target);
                    depth[index] = d;
                    inner[index] = d == width - 1 ? innerEnd : ground;
                }
            }
        }
    }

    /**
     * Writes the retaining columns where a strip could not absorb the whole step: in the strip's innermost column,
     * the border block between the strip's inner end and the city ground (E3).
     *
     * @param buffer  the leveled chunk
     * @param plan    the chunk's plan
     * @param targets the targets from {@link #targets}
     * @param depth   the depths from {@link #targets}
     * @param inner   the inner ends from {@link #targets}
     */
    public static void retainingWalls(ChunkBuffer buffer, ChunkPlan plan, int[] targets, int[] depth, int[] inner) {
        int ground = plan.groundY();
        for (int index = 0; index < 256; index++) {
            if (depth[index] == NOT_IN_STRIP || inner[index] == ground) {
                continue;
            }
            int x = index & 15;
            int z = index >> 4;
            int low = Math.min(inner[index], ground);
            int high = Math.max(inner[index], ground);
            ColumnOps.fill(
                buffer,
                x,
                z,
                low + 1,
                high + 1,
                plan.style()
                    .border());
        }
    }

    private static int edgeX(ChunkPlan.Side side, int along, int depth) {
        return switch (side) {
            case WEST -> depth;
            case EAST -> 15 - depth;
            default -> along;
        };
    }

    private static int edgeZ(ChunkPlan.Side side, int along, int depth) {
        return switch (side) {
            case NORTH -> depth;
            case SOUTH -> 15 - depth;
            default -> along;
        };
    }
}
