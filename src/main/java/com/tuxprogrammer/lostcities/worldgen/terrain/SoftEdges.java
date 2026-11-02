package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Arrays;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/**
 * Soft-edge strips. The layout puts the city's margin (a ring of non-city chunks) between a city and every landmark
 * core, so the strip that stitches the city to the core lies in the margin chunk ({@link #recordCoreEdges},
 * {@link #stitchToCores}): after the blend has sloped the margin from the city's edge toward the core's oracle height,
 * a strip of about {@code softEdgeWidth} columns on each side facing a core ramps the blended ground to the core's own
 * edge columns, so the margin meets the village's ground without a step (alpha review AR-10, AR-13).
 *
 * <p>
 * A city chunk that faces a core directly, which only a plan without margins makes (the synthetic test city), gets
 * the strip inside the city chunk instead: on each side of a city chunk that faces a landmark core, a strip of about
 * {@code softEdgeWidth} columns replaces upstream's border wall and ramps the ground linearly from the city ground to
 * the column's own RWG height at the core's edge (rules E2 to E4 of docs/new-spec/00-pack-integration/coexistence.md,
 * adopted by docs/design/0006-build-around-other-worldgen.md §4; the ramp shape is P03-13, D31).
 *
 * <ul>
 * <li>Width: {@code softEdgeWidth}, varied by up to ±2 per position along the side by the transition noise (E4).</li>
 * <li>Outer edge: the edge column's own surface before leveling, or the top of the water standing on it (a pond or
 * river at the core's edge meets the strip at its waterline), read from this chunk's RWG columns; inner edge: the
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
                } else {
                    outer = waterline(buffer, classes, edgeX, edgeZ, outer);
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

    /**
     * Returns the height a strip ends at on its edge column: the column's solid surface, or the top of the liquid
     * standing on it. A core's pond or river then meets the strip at its waterline, as a shore, instead of the strip
     * ramping down to the bed under the water (0006 §4, E2).
     *
     * @param buffer  the chunk before leveling
     * @param classes the block classes
     * @param x       local X of the edge column
     * @param z       local Z of the edge column
     * @param surface the column's solid surface, through foliage
     * @return block Y
     */
    static int waterline(ChunkBuffer buffer, BlockClassifier classes, int x, int z, int surface) {
        int y = surface + 1;
        while (y <= WorldLimits.MAX_Y && classes.isLiquid(buffer.get(x, y, z))) {
            y++;
        }
        return y - 1;
    }

    /**
     * Reads the natural height of every edge column of a non-city chunk that faces a core, before the blend moves it:
     * the column's surface through foliage, or the top of the water standing on it.
     *
     * @param buffer  the chunk as RWG made it
     * @param classes the block classes
     * @param sides   the plans toward −X, +X, −Z and +Z
     * @param edges   out: per side ({@link ChunkPlan.Side} order) and position along it, the edge column's height;
     *                {@link #NOT_IN_STRIP} for a side that faces no core
     * @return whether any side faces a core
     */
    public static boolean recordCoreEdges(ChunkBuffer buffer, BlockClassifier classes, ChunkPlan[] sides,
        int[][] edges) {
        boolean any = false;
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            int[] row = edges[side.ordinal()];
            if (!sides[side.ordinal()].isCore()) {
                Arrays.fill(row, NOT_IN_STRIP);
                continue;
            }
            any = true;
            for (int along = 0; along < 16; along++) {
                int x = edgeX(side, along, 0);
                int z = edgeZ(side, along, 0);
                int surface = ColumnOps.surface(buffer, classes, x, z, true);
                row[along] = surface < WorldLimits.MIN_Y ? NOT_IN_STRIP : waterline(buffer, classes, x, z, surface);
            }
        }
        return any;
    }

    /**
     * Ramps the blended ground of a non-city chunk back to the natural edge columns recorded before the blend, over a
     * strip of {@code softEdgeWidth} ± 2 columns on each side facing a core (E2 to E4 in the margin): the strip's
     * inner end keeps the blended height, its edge column its own natural height, linearly between. A column in two
     * strips follows the side it is nearer to. Columns keep their own top blocks.
     *
     * @param buffer  the chunk after the blend
     * @param context the world's terrain context
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @param edges   the heights {@link #recordCoreEdges} read
     * @return the number of columns moved
     */
    public static int stitchToCores(ChunkBuffer buffer, TerrainContext context, int chunkX, int chunkZ, int[][] edges) {
        BlockClassifier classes = context.classifier();
        int baseWidth = context.settings()
            .softEdgeWidth();
        int waterLevel = context.settings()
            .waterLevel();
        int[] targets = new int[256];
        int[] depth = new int[256];
        Arrays.fill(depth, NOT_IN_STRIP);
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            int[] row = edges[side.ordinal()];
            for (int along = 0; along < 16; along++) {
                int outer = row[along];
                if (outer == NOT_IN_STRIP) {
                    continue;
                }
                int edgeX = edgeX(side, along, 0);
                int edgeZ = edgeZ(side, along, 0);
                float noise = context.transitionNoise((chunkX << 4) + edgeX, (chunkZ << 4) + edgeZ);
                int variation = Math.max(-WIDTH_VARIATION, Math.min(WIDTH_VARIATION, Math.round(noise / 2f)));
                int width = Math.max(1, Math.min(15, baseWidth + variation));
                int innerX = edgeX(side, along, width);
                int innerZ = edgeZ(side, along, width);
                int inner = ColumnOps.surface(buffer, classes, innerX, innerZ, true);
                if (inner < WorldLimits.MIN_Y) {
                    continue;
                }
                for (int d = 0; d < width; d++) {
                    int index = (edgeZ(side, along, d) << 4) | edgeX(side, along, d);
                    if (depth[index] != NOT_IN_STRIP && depth[index] <= d) {
                        continue;
                    }
                    targets[index] = WorldLimits.clampY(outer + Math.round((inner - outer) * d / (float) width));
                    depth[index] = d;
                }
            }
        }
        int moved = 0;
        for (int index = 0; index < 256; index++) {
            if (depth[index] == NOT_IN_STRIP) {
                continue;
            }
            int x = index & 15;
            int z = index >> 4;
            int touched = ColumnOps
                .moveDown(buffer, classes, x, z, targets[index] + 1, WorldLimits.BUILD_HEIGHT, waterLevel);
            if (touched == ColumnOps.NOT_TOUCHED) {
                touched = ColumnOps.moveUp(buffer, classes, x, z, targets[index]);
            }
            moved += touched == ColumnOps.NOT_TOUCHED ? 0 : 1;
        }
        return moved;
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
