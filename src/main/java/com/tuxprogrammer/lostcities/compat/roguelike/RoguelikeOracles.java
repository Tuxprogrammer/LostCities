package com.tuxprogrammer.lostcities.compat.roguelike;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * The Roguelike Dungeons structure oracle of a world, for the layout (0006 §2; coexistence.md § Roguelike Dungeons):
 * each grid cell's predicted tower site as a {@code CANDIDATE} lot ({@link RoguelikeOracle}).
 *
 * <p>
 * Guarantees, for the World lane that wires it in:
 * <ul>
 * <li><b>Pure.</b> Answers depend only on the world seed, Roguelike's config, the world's biome provider and the height
 * oracle. No chunk is loaded or generated and {@code world.rand} is never touched: the grid draws come from a
 * {@code Random} of the oracle's own, of {@code world.rand}'s class.</li>
 * <li><b>Order-independent and complete</b>, as {@link StructureOracle} requires; landmarks come back sorted by
 * identity, {@code roguelike@<blockX>,<blockZ>} (the site).</li>
 * <li><b>Bounded:</b> {@value RoguelikeOracle#CACHE_SIZE} cells cached, owned by the returned object.</li>
 * <li><b>Empty</b> ({@link StructureOracle#NONE}) when Roguelike is not loaded, or does not generate in this
 * dimension.</li>
 * </ul>
 * Classes that touch Roguelike load only after {@code Loader.isModLoaded("Roguelike")}, so this class loads without
 * it. Server thread only. Side: server.
 */
public final class RoguelikeOracles {

    /** Roguelike Dungeons' mod ID. */
    public static final String MOD_ID = "Roguelike";

    private RoguelikeOracles() {}

    /** Returns whether Roguelike Dungeons is loaded. */
    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * Returns the oracle of a world.
     *
     * @param world          the world, whose generator already exists
     * @param heights        the world's height oracle
     * @param landmarkMargin the profile's {@code landmarkMargin}, blocks; the lot already includes the tower's margin,
     *                       so the layout grows it as for every landmark
     * @return the oracle; {@link StructureOracle#NONE} when Roguelike generates nothing here
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        if (!isLoaded() || heights == null) {
            return StructureOracle.NONE;
        }
        return Present.create(world, heights);
    }

    /**
     * Returns the Roguelike values the oracle of a world reads, as one line for the world's pinned profile snapshot
     * (0006 §2), or {@code "absent"}.
     *
     * @param world the world
     * @return the summary
     */
    public static String settingsSummary(World world) {
        return isLoaded() ? Present.settings()
            .summary() : "absent";
    }

    /**
     * Returns a fresh {@code Random} of the class of a world's {@code world.rand}, whose stream Roguelike's grid reads
     * through {@code world.setRandomSeed}. In GTNH 2.9.0 that is {@code java.util.Random}; BugTorch can swap the class
     * when a server owner turns its {@code replaceRandomInWorld} on.
     *
     * @param live the world's random
     * @return a new random of the same class, or a {@code java.util.Random} when the class cannot be built
     */
    static Random likeWorldRandom(Random live) {
        if (live == null || live.getClass() == Random.class) {
            return new Random();
        }
        try {
            return live.getClass()
                .getDeclaredConstructor()
                .newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
            | InvocationTargetException e) {
            LostCities.LOG.warn(
                "[LCNH-RL] cannot build a {} like world.rand; Roguelike sites may be mispredicted",
                live.getClass()
                    .getName(),
                e);
            return new Random();
        }
    }

    /** Everything that touches Roguelike; loaded only when it is present. */
    static final class Present {

        private Present() {}

        static RoguelikeSettings settings() {
            return RoguelikeConfig.read();
        }

        static StructureOracle create(World world, TerrainHeightOracle heights) {
            RoguelikeSettings settings = RoguelikeConfig.read();
            int dimension = world.provider.dimensionId;
            if (!settings.generatesIn(dimension)) {
                LostCities.LOG.info("[LCNH-RL] dimension {}: no oracle ({})", dimension, settings);
                return StructureOracle.NONE;
            }
            Random gridRandom = likeWorldRandom(world.rand);
            RoguelikeGrid grid = new RoguelikeGrid(world.getSeed(), settings, gridRandom);
            LostCities.LOG.info(
                "[LCNH-RL] dimension {}: oracle on ({}); grid random {}",
                dimension,
                settings,
                gridRandom.getClass()
                    .getName());
            return new RoguelikeOracle(
                dimension,
                grid,
                heights,
                new WorldRoguelikeTerrain(world.getWorldChunkManager()),
                RoguelikeOracle.CACHE_SIZE);
        }
    }
}
