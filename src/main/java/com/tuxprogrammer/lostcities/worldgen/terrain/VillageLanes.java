package com.tuxprogrammer.lostcities.worldgen.terrain;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
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
 * <li>anything else: the next block down. A column that matches nothing keeps its surface.</li>
 * </ul>
 * Every block goes through the village's material swap: a desert village's gravel and cobblestone become sandstone,
 * a mesa village's hardened clay. Nothing else is written above or below the column. The rule reads only this
 * chunk's blocks. Side: both (plain Java).
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
        return paved;
    }

    private static boolean layColumn(ChunkBuffer buffer, TerrainContext context, int worldX, int worldZ, int x, int z,
        LanePath.Material material) {
        BlockClassifier classes = context.classifier();
        int seaFloor = context.settings()
            .waterLevel() - 1;
        int top = WorldLimits.MAX_Y;
        while (top > WorldLimits.MIN_Y) {
            BlockRef block = buffer.get(x, top, z);
            if (!block.isAir() && !classes.isFoliage(block)) {
                break;
            }
            top--;
        }
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
        return false;
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
