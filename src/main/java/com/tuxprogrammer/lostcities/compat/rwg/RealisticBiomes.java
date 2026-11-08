package com.tuxprogrammer.lostcities.compat.rwg;

import java.util.Locale;
import java.util.regex.Pattern;

import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;

import cpw.mods.fml.common.Loader;

import com.tuxprogrammer.lostcities.worldgen.overlay.PlanThreads;

/**
 * The RWG realistic biome behind a column, for the {@code rwg:} biome matcher (docs/new-spec/05-assets/
 * asset-format.md, "Biome matchers"; P05-6 and P-PROF-4, decision log D31). RWG's biome provider reports base biomes,
 * and the realistic biome that shapes the terrain is not visible through them: canyon and canyon forest report Hot
 * Plains and Hot Forest, and oasis and red oasis report the Oasis base biome, which has no dictionary type, so the
 * desert city style asks for them by realistic biome.
 *
 * <p>
 * Both RWG versions the pack ships are covered: classic RWG ({@code RWG}, alpha-1.5.3) and RWG2 ({@code rwg2}, 2.0.6).
 * Each is compiled against {@code compileOnly} and touched only when {@code Loader.isModLoaded} says it is present,
 * so the mod runs without either (CONTRIBUTING § GTNH integration, "Optional mods"). A realistic biome is named by its
 * class without the {@code RealisticBiome} prefix, in lower snake case: {@code RealisticBiomeCanyonForest} is
 * {@code canyon_forest}.
 *
 * <p>
 * Source: RWG2 2.0.6,
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L544-L560
 * ({@code getBiomeDataAt(blockX, blockZ)}, cached per column); classic RWG alpha-1.5.3,
 * https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkManagerRealistic.java#L202-L204.
 * Both chunk managers extend {@code WorldChunkManager} and are the world's chunk manager on their world types.
 *
 * <p>
 * Side: server (world generation).
 */
public final class RealisticBiomes {

    /** The mod ID of classic RWG. */
    public static final String RWG = "RWG";

    /** The mod ID of RWG2. */
    public static final String RWG2 = "rwg2";

    /** The class-name prefix RWG's realistic biomes share. */
    static final String PREFIX = "RealisticBiome";

    private static final Pattern WORD_BREAK = Pattern.compile("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

    private RealisticBiomes() {}

    /**
     * Returns the realistic biome that generated a column.
     *
     * @param world  the world
     * @param blockX the block X
     * @param blockZ the block Z
     * @return the realistic biome in lower snake case, or null when neither RWG version shapes this world
     */
    public static String at(World world, int blockX, int blockZ) {
        // The plan worker asks its own copy of the provider (D-plan-worker-2).
        WorldChunkManager manager = PlanThreads.manager(world);
        if (Loader.isModLoaded(RWG2)) {
            String name = Rwg2.at(manager, blockX, blockZ);
            if (name != null) {
                return name;
            }
        }
        if (Loader.isModLoaded(RWG)) {
            return Rwg1.at(manager, blockX, blockZ);
        }
        return null;
    }

    /**
     * Returns the matcher name of a realistic-biome class name.
     *
     * @param simpleName the class's simple name, such as {@code RealisticBiomeCanyonForest}
     * @return the name, such as {@code canyon_forest}
     */
    public static String nameOf(String simpleName) {
        String base = simpleName.startsWith(PREFIX) ? simpleName.substring(PREFIX.length()) : simpleName;
        return WORD_BREAK.matcher(base)
            .replaceAll("_")
            .toLowerCase(Locale.ROOT);
    }

    /** RWG2's chunk manager; loaded only when {@code rwg2} is. */
    private static final class Rwg2 {

        static String at(WorldChunkManager manager, int blockX, int blockZ) {
            if (!(manager instanceof rwg2.world.ChunkManagerRealistic)) {
                return null;
            }
            Object biome = ((rwg2.world.ChunkManagerRealistic) manager).getBiomeDataAt(blockX, blockZ);
            return biome == null ? null
                : nameOf(
                    biome.getClass()
                        .getSimpleName());
        }
    }

    /** Classic RWG's chunk manager; loaded only when {@code RWG} is. */
    private static final class Rwg1 {

        static String at(WorldChunkManager manager, int blockX, int blockZ) {
            if (!(manager instanceof rwg.world.ChunkManagerRealistic)) {
                return null;
            }
            Object biome = ((rwg.world.ChunkManagerRealistic) manager).getBiomeDataAt(blockX, blockZ);
            return biome == null ? null
                : nameOf(
                    biome.getClass()
                        .getSimpleName());
        }
    }
}
