package com.tuxprogrammer.lostcities.compat.ae2;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * The AE2 meteorite oracle of a world, for the layout (0006 §2): {@link MeteoriteOracle} when AE2 is loaded and drops
 * meteorites in the world's dimension, {@link StructureOracle#NONE} otherwise.
 *
 * <p>
 * Classes that touch AE2 load only after {@code Loader.isModLoaded("appliedenergistics2")}, so this class loads
 * without it. Server thread only.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § AE2 meteorites. Side: server.
 */
public final class Ae2Oracles {

    /** AE2's mod ID. */
    public static final String MOD_ID = "appliedenergistics2";

    private Ae2Oracles() {}

    /** Returns whether AE2 is loaded. */
    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * Returns the oracle of a world.
     *
     * @param world          the world, whose generator already exists
     * @param heights        the world's height oracle, or {@code null} to estimate every height at Y 64
     * @param landmarkMargin the profile's {@code landmarkMargin}, blocks; the meteorite's footprint needs none
     * @return the oracle; {@link StructureOracle#NONE} when no meteorite falls here
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        if (!isLoaded()) {
            return StructureOracle.NONE;
        }
        return Present.create(world, heights);
    }

    /**
     * Returns the AE2 values the oracle of a world reads, as one line for the pinned profile snapshot (0006 §2), or
     * {@code "absent"}.
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
     * @return the settings; {@link MeteoriteSettings#OFF} when AE2 is absent
     */
    static MeteoriteSettings settings(World world) {
        return isLoaded() ? Present.settings(world) : MeteoriteSettings.OFF;
    }

    /** Everything that touches AE2; loaded only when it is present. */
    static final class Present {

        private Present() {}

        static MeteoriteSettings settings(World world) {
            return Ae2Config.read(world);
        }

        static StructureOracle create(World world, TerrainHeightOracle heights) {
            MeteoriteSettings settings = Ae2Config.read(world);
            if (!settings.enabled()) {
                LostCities.LOG.info("[LCNH-AE2] dimension {}: no oracle ({})", world.provider.dimensionId, settings);
                return StructureOracle.NONE;
            }
            LostCities.LOG.info("[LCNH-AE2] dimension {}: oracle on ({})", world.provider.dimensionId, settings);
            return new MeteoriteOracle(world.getSeed(), settings, heights, MeteoriteOracle.CACHE_SIZE);
        }
    }
}
