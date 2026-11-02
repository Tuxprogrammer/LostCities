package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Arrays;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/**
 * Lays a village lane on the ground: each lane column takes the block VillageNames' own paths would take there, so
 * the lane and the village's paths meet in the same blocks (docs/design/0006-build-around-other-worldgen.md §3,
 * docs/new-spec/03-terrain/pipeline-on-rwg.md "Village lanes", P03-6, D31). The rule is written here from that
 * sheet's description of {@code StructureVillageVN.setPathSpecificBlock} (VillageNames 4.5.21-GTNH,
 * StructureVillageVN.java#L1224-L1328); no VillageNames code is copied.
 *
 * <p>
 * Per column, from the top solid or liquid block (raised to sea level − 1 if lower) down to sea level − 1:
 * <ul>
 * <li>grass or dirt under air (plants above count as air and are cleared): the pack's path block,
 * {@code etfuturum:grass_path}, or gravel without Et Futurum;</li>
 * <li>sand: gravel, with cobblestone in the block below;</li>
 * <li>lava: cobblestone, with cobblestone posts down through the lava at even world X and Z;</li>
 * <li>water, ice or mud: planks with the block above cleared, and log posts down through the water at even X and
 * Z;</li>
 * <li>anything else: the next block down.</li>
 * </ul>
 * A column that matches nothing down to sea level − 1 is paved as sand is (gravel over cobblestone) when its surface is
 * natural ground the rule does not name: terracotta, stained clay, stone, sandstone, gravel, clay, mycelium or snow.
 * Deviation (decision log, M05J rows): VillageNames' rule leaves such columns as they are, which on classic RWG's mesa
 * cores left the lane invisible; the fallback keeps the lane continuous in the blocks VillageNames' paths use, and city
 * blocks (streets, borders) are still never paved.
 * Every block goes through the village's material swap: a desert village's gravel and cobblestone become sandstone,
 * a mesa village's hardened clay.
 *
 * <p>
 * Then the lane gets steps people would walk (alpha review AR-11, {@link #steps}): where the next lane column is one
 * block higher, a half slab on a single step, or a stair facing up the slope where the rise goes on. Deviation:
 * VillageNames' paths and the first lanes climbed in whole blocks. Nothing else is written above or below the column.
 * The rule reads only this chunk's blocks. Side: both (plain Java).
 */
public final class VillageLanes {

    /** Et Futurum's grass path, first in the pack's VillageNames {@code Mod Priority: Grass Path} list. */
    public static final BlockRef GRASS_PATH = BlockRef.of("etfuturum:grass_path", 0);

    private static final BlockRef GRASS = BlockRef.of("minecraft:grass", 0);
    private static final BlockRef GRAVEL = BlockRef.of("minecraft:gravel", 0);
    private static final BlockRef COBBLESTONE = BlockRef.of("minecraft:cobblestone", 0);
    private static final BlockRef PLANKS = BlockRef.of("minecraft:planks", 0);
    private static final BlockRef LOG = BlockRef.of("minecraft:log", 0);
    private static final BlockRef SANDSTONE = BlockRef.of("minecraft:sandstone", 0);
    private static final BlockRef HARDENED_CLAY = BlockRef.of("minecraft:hardened_clay", 0);
    /**
     * Stair metadata ascending toward +X, −X, +Z and −Z (1.7.10's {@code BlockStairs}: 0 east, 1 west, 2 south, 3
     * north).
     */
    private static final int[] STAIR_META = { 0, 1, 2, 3 };
    /** Steps along +X, −X, +Z and −Z, in {@link #STAIR_META}'s order. */
    private static final int[][] STEPS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    /** A column without a grade target. */
    private static final int NO_TARGET = Integer.MIN_VALUE;

    private VillageLanes() {}

    /**
     * Lays the lane's columns of one chunk.
     *
     * @param buffer  the chunk, after leveling
     * @param context the world's terrain context
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @param lane    the lane's columns and the village's material
     * @return the number of columns that got a path block
     */
    public static int lay(ChunkBuffer buffer, TerrainContext context, int chunkX, int chunkZ, LanePath lane) {
        int paved = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (lane.contains(x, z)
                    && layColumn(buffer, context, (chunkX << 4) + x, (chunkZ << 4) + z, x, z, lane.material())) {
                    paved++;
                }
            }
        }
        if (paved > 0) {
            steps(buffer, context.classifier(), lane);
        }
        return paved;
    }

    /**
     * Grades a lane across the city's margin before it is laid (AR-7): each lane column moves, keeping its top
     * blocks, to a straight ramp from the city's ground at the chunk's city side to the core's edge column on the far
     * side ({@code coreEdges}, read before the blend; the column's own height where none was read). The lane then runs
     * down the slope instead of stopping at its top. A column beside the lane that differs from it by more than a
     * block meets it halfway, so the lane is neither a causeway nor a trench.
     *
     * @param buffer    the chunk after the blend and the strip
     * @param context   the world's terrain context
     * @param lane      the lane, with its {@link LanePath#gradeSide()}
     * @param coreEdges per side and position along it, the natural edge heights; {@link SoftEdges#NOT_IN_STRIP} where
     *                  none was read
     * @return the number of columns moved
     */
    public static int grade(ChunkBuffer buffer, TerrainContext context, LanePath lane, int[][] coreEdges) {
        ChunkPlan.Side citySide = lane.gradeSide();
        if (citySide == null) {
            return 0;
        }
        ChunkPlan.Side coreSide = opposite(citySide);
        BlockClassifier classes = context.classifier();
        int waterLevel = context.settings()
            .waterLevel();
        int from = lane.gradeFrom();
        int moved = 0;
        int[] targets = new int[256];
        Arrays.fill(targets, NO_TARGET);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (!lane.contains(x, z)) {
                    continue;
                }
                boolean alongX = citySide == ChunkPlan.Side.WEST || citySide == ChunkPlan.Side.EAST;
                int depth = switch (citySide) {
                    case WEST -> x;
                    case EAST -> 15 - x;
                    case NORTH -> z;
                    default -> 15 - z;
                };
                int along = alongX ? z : x;
                int end = coreEdges[coreSide.ordinal()][along];
                if (end == SoftEdges.NOT_IN_STRIP) {
                    int ex = coreSide == ChunkPlan.Side.WEST ? 0 : coreSide == ChunkPlan.Side.EAST ? 15 : x;
                    int ez = coreSide == ChunkPlan.Side.NORTH ? 0 : coreSide == ChunkPlan.Side.SOUTH ? 15 : z;
                    end = ColumnOps.surface(buffer, classes, ex, ez, true);
                    if (end < WorldLimits.MIN_Y) {
                        continue;
                    }
                }
                int target = WorldLimits.clampY(from + Math.round((end - from) * depth / 15f));
                targets[(z << 4) | x] = target;
                moved += moveTo(buffer, classes, x, z, target, waterLevel) ? 1 : 0;
            }
        }
        // Shoulders: the column beside the lane meets it halfway, so a graded lane is not a causeway or a trench.
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (lane.contains(x, z)) {
                    continue;
                }
                int laneTarget = NO_TARGET;
                for (int[] step : STEPS) {
                    int nx = x + step[0];
                    int nz = z + step[1];
                    if (inChunk(nx, nz) && targets[(nz << 4) | nx] != NO_TARGET) {
                        laneTarget = targets[(nz << 4) | nx];
                        break;
                    }
                }
                int own = ColumnOps.surface(buffer, classes, x, z, true);
                if (laneTarget == NO_TARGET || own < WorldLimits.MIN_Y || Math.abs(own - laneTarget) <= 1) {
                    continue;
                }
                moved += moveTo(buffer, classes, x, z, Math.round((own + laneTarget) / 2f), waterLevel) ? 1 : 0;
            }
        }
        return moved;
    }

    /** Moves a column's top to a height, keeping its top blocks; returns whether anything moved. */
    private static boolean moveTo(ChunkBuffer buffer, BlockClassifier classes, int x, int z, int target,
        int waterLevel) {
        int touched = ColumnOps.moveDown(buffer, classes, x, z, target + 1, WorldLimits.BUILD_HEIGHT, waterLevel);
        if (touched == ColumnOps.NOT_TOUCHED) {
            touched = ColumnOps.moveUp(buffer, classes, x, z, target);
        }
        return touched != ColumnOps.NOT_TOUCHED;
    }

    private static ChunkPlan.Side opposite(ChunkPlan.Side side) {
        return switch (side) {
            case WEST -> ChunkPlan.Side.EAST;
            case EAST -> ChunkPlan.Side.WEST;
            case NORTH -> ChunkPlan.Side.SOUTH;
            default -> ChunkPlan.Side.NORTH;
        };
    }

    /**
     * Gives a laid lane steps of half a block (AR-11): on a lane column whose lane neighbor along X or Z is exactly one
     * block higher, a stair facing that neighbor when the lane keeps rising beyond it, else a bottom half slab. Rises
     * of two or more are left as they are; the blend and the margin keep the lane's ground to one block a column where
     * they shape it. Only this chunk's columns are compared.
     *
     * @param buffer  the chunk, with the lane laid
     * @param classes the block classes
     * @param lane    the lane
     * @return the number of steps placed
     */
    static int steps(ChunkBuffer buffer, BlockClassifier classes, LanePath lane) {
        int[] tops = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                tops[(z << 4) | x] = lane.contains(x, z) ? ColumnOps.surface(buffer, classes, x, z, true)
                    : WorldLimits.MIN_Y - 1;
            }
        }
        int placed = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = tops[(z << 4) | x];
                if (top < WorldLimits.MIN_Y || top + 1 > WorldLimits.MAX_Y) {
                    continue;
                }
                BlockRef ground = buffer.get(x, top, z);
                BlockRef above = buffer.get(x, top + 1, z);
                if (classes.isLiquid(ground) || !(above.isAir() || classes.isFoliage(above))) {
                    continue;
                }
                for (int d = 0; d < STEPS.length; d++) {
                    int nx = x + STEPS[d][0];
                    int nz = z + STEPS[d][1];
                    if (!inChunk(nx, nz) || tops[(nz << 4) | nx] != top + 1) {
                        continue;
                    }
                    int fx = nx + STEPS[d][0];
                    int fz = nz + STEPS[d][1];
                    boolean rising = inChunk(fx, fz) && tops[(fz << 4) | fx] >= top + 2;
                    buffer.set(x, top + 1, z, rising ? stair(ground, d) : slab(ground));
                    placed++;
                    break;
                }
            }
        }
        return placed;
    }

    private static boolean inChunk(int x, int z) {
        return x >= 0 && x < 16 && z >= 0 && z < 16;
    }

    /** The stair that matches a path block: sandstone on sandstone, brick on hardened clay, cobblestone otherwise. */
    static BlockRef stair(BlockRef ground, int direction) {
        String name = ground.name();
        String stair = SANDSTONE.name()
            .equals(name) ? "minecraft:sandstone_stairs"
                : HARDENED_CLAY.name()
                    .equals(name) ? "minecraft:brick_stairs" : "minecraft:stone_stairs";
        return BlockRef.of(stair, STAIR_META[direction]);
    }

    /** The bottom half slab that matches a path block: sandstone, brick or cobblestone ({@code stone_slab} 1, 4, 3). */
    static BlockRef slab(BlockRef ground) {
        String name = ground.name();
        int meta = SANDSTONE.name()
            .equals(name) ? 1
                : HARDENED_CLAY.name()
                    .equals(name) ? 4 : 3;
        return BlockRef.of("minecraft:stone_slab", meta);
    }

    private static boolean layColumn(ChunkBuffer buffer, TerrainContext context, int worldX, int worldZ, int x, int z,
        LanePath.Material material) {
        BlockClassifier classes = context.classifier();
        int seaFloor = context.settings()
            .waterLevel() - 1;
        int top = Math.min(WorldLimits.MAX_Y, buffer.topY());
        while (top > WorldLimits.MIN_Y) {
            BlockRef block = buffer.get(x, top, z);
            if (!block.isAir() && !classes.isFoliage(block)) {
                break;
            }
            top--;
        }
        int surface = top;
        top = Math.max(top, seaFloor);
        boolean post = (worldX & 1) == 0 && (worldZ & 1) == 0;
        for (int y = top; y >= seaFloor && y > WorldLimits.MIN_Y; y--) {
            BlockRef block = buffer.get(x, y, z);
            String name = block.name();
            if (isGrassOrDirt(name) && isOpenAbove(buffer, classes, x, y, z)) {
                clearPlantAbove(buffer, classes, x, y, z);
                buffer.set(
                    x,
                    y,
                    z,
                    swap(
                        context.availability()
                            .orElse(GRASS_PATH, GRAVEL),
                        material));
                return true;
            }
            if ("minecraft:sand".equals(name)) {
                buffer.set(x, y, z, swap(GRAVEL, material));
                buffer.set(x, y - 1, z, swap(COBBLESTONE, material));
                return true;
            }
            if ("minecraft:lava".equals(name) || "minecraft:flowing_lava".equals(name)) {
                buffer.set(x, y, z, swap(COBBLESTONE, material));
                if (post) {
                    postDown(buffer, classes, x, y - 1, z, swap(COBBLESTONE, material));
                }
                return true;
            }
            if (isWaterIceOrMud(name)) {
                buffer.set(x, y, z, swap(PLANKS, material));
                if (y + 1 <= WorldLimits.MAX_Y) {
                    buffer.set(x, y + 1, z, BlockRef.AIR);
                }
                if (post) {
                    postDown(buffer, classes, x, y - 1, z, swap(LOG, material));
                }
                return true;
            }
        }
        if (surface > WorldLimits.MIN_Y && isNaturalGround(
            buffer.get(x, surface, z)
                .name())) {
            clearPlantAbove(buffer, classes, x, surface, z);
            buffer.set(x, surface, z, swap(GRAVEL, material));
            buffer.set(x, surface - 1, z, swap(COBBLESTONE, material));
            return true;
        }
        return false;
    }

    /** Natural ground the path rule names nothing for, which the lane paves as sand (the M05J fallback). */
    static boolean isNaturalGround(String name) {
        switch (name) {
            case "minecraft:hardened_clay":
            case "minecraft:stained_hardened_clay":
            case "minecraft:stone":
            case "minecraft:sandstone":
            case "minecraft:gravel":
            case "minecraft:clay":
            case "minecraft:mycelium":
            case "minecraft:snow":
                return true;
            default:
                return false;
        }
    }

    private static void postDown(ChunkBuffer buffer, BlockClassifier classes, int x, int fromY, int z, BlockRef post) {
        for (int y = fromY; y > WorldLimits.MIN_Y && classes.isLiquid(buffer.get(x, y, z)); y--) {
            buffer.set(x, y, z, post);
        }
    }

    private static boolean isOpenAbove(ChunkBuffer buffer, BlockClassifier classes, int x, int y, int z) {
        if (y + 1 > WorldLimits.MAX_Y) {
            return true;
        }
        BlockRef above = buffer.get(x, y + 1, z);
        return above.isAir() || classes.isFoliage(above);
    }

    private static void clearPlantAbove(ChunkBuffer buffer, BlockClassifier classes, int x, int y, int z) {
        for (int above = y + 1; above <= WorldLimits.MAX_Y; above++) {
            BlockRef block = buffer.get(x, above, z);
            if (block.isAir() || !classes.isFoliage(block)) {
                return;
            }
            buffer.set(x, above, z, BlockRef.AIR);
        }
    }

    private static boolean isGrassOrDirt(String name) {
        return GRASS.name()
            .equals(name) || "minecraft:dirt".equals(name);
    }

    private static boolean isWaterIceOrMud(String name) {
        return "minecraft:water".equals(name) || "minecraft:flowing_water".equals(name)
            || "minecraft:ice".equals(name)
            || "minecraft:packed_ice".equals(name)
            || "BiomesOPlenty:mud".equals(name);
    }

    /** The village's material swap: gravel and cobblestone become sandstone or hardened clay. */
    static BlockRef swap(BlockRef block, LanePath.Material material) {
        if (material == LanePath.Material.PLAIN) {
            return block;
        }
        if (block.equals(GRAVEL) || block.equals(COBBLESTONE)) {
            return material == LanePath.Material.SAND ? SANDSTONE : HARDENED_CLAY;
        }
        return block;
    }
}
