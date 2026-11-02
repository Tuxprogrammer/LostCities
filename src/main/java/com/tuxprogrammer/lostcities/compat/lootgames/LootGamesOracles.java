package com.tuxprogrammer.lostcities.compat.lootgames;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * The LootGames dungeon oracle of a world, for the layout (0006 §2): {@link LootGamesOracle} when LootGames is loaded
 * and digs dungeons in the world's dimension, {@link StructureOracle#NONE} otherwise.
 *
 * <p>
 * Classes that touch LootGames load only after {@code Loader.isModLoaded("lootgames")}, so this class loads without
 * it. Server thread only.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § LootGames. Side: server.
 */
public final class LootGamesOracles {

    /** LootGames' mod ID. */
    public static final String MOD_ID = "lootgames";

    private LootGamesOracles() {}

    /** Returns whether LootGames is loaded. */
    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * Returns the oracle of a world.
     *
     * @param world          the world
     * @param heights        the world's height oracle, or {@code null} to estimate every ground at Y 64
     * @param landmarkMargin the profile's {@code landmarkMargin}, blocks; the footprint needs none
     * @return the oracle; {@link StructureOracle#NONE} when no dungeon generates here
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        if (!isLoaded()) {
            return StructureOracle.NONE;
        }
        return Present.create(world, heights);
    }

    /**
     * Returns the LootGames values the oracle of a world reads, as one line for the pinned profile snapshot (0006
     * §2), or {@code "absent"}.
     *
     * @param world the world
     * @return the summary
     */
    public static String settingsSummary(World world) {
        return isLoaded() ? Present.settings(world)
            .summary() : "absent";
    }

    /**
     * Returns the settings of a world, for the scenario.
     *
     * @param world the world
     * @return the settings; {@link LootGamesSettings#OFF} when LootGames is absent
     */
    static LootGamesSettings settings(World world) {
        return isLoaded() ? Present.settings(world) : LootGamesSettings.OFF;
    }

    /** Everything that touches LootGames; loaded only when it is present. */
    static final class Present {

        private Present() {}

        static LootGamesSettings settings(World world) {
            return LootGamesConfig.read(world);
        }

        static StructureOracle create(World world, TerrainHeightOracle heights) {
            LootGamesSettings settings = LootGamesConfig.read(world);
            if (!settings.enabled()) {
                LostCities.LOG.info("[LCNH-LG] dimension {}: no oracle ({})", world.provider.dimensionId, settings);
                return StructureOracle.NONE;
            }
            LostCities.LOG.info("[LCNH-LG] dimension {}: oracle on ({})", world.provider.dimensionId, settings);
            return new LootGamesOracle(world.getSeed(), settings, heights, LootGamesOracle.CACHE_SIZE);
        }
    }
}
