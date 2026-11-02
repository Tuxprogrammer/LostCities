package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Locale;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.compat.HeightOracles;

import astrotibs.villagenames.config.GeneralConfig;
import astrotibs.villagenames.config.village.VillageGeneratorConfigHandler;
import cpw.mods.fml.common.Loader;

/**
 * Reads the {@link VillageNamesSettings} of a world from VillageNames' loaded config and the world's own switches.
 * Touches VillageNames classes, so only {@link VillageNamesOracles} calls it, after {@code Loader.isModLoaded}.
 *
 * <p>
 * Villages generate when VillageNames' "Activate New Village Generator" is on, the world has map features, and on RWG
 * worlds RWG's own "Generate Villages" is on (both RWG versions call the village generator only then). Monuments
 * generate in the overworld when "Add Monuments" is on and the world has map features.
 *
 * <p>
 * Source: VillageNames 4.5.21-GTNH,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/config/village/VillageGeneratorConfigHandler.java#L593-L625
 * and
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/config/GeneralConfig.java#L351;
 * RWG2 2.0.6,
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L245-L247;
 * classic RWG alpha-1.5.3,
 * https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L194-L196.
 *
 * <p>
 * Side: server.
 */
final class VillageNamesConfig {

    private VillageNamesConfig() {}

    /**
     * Reads the settings of a world.
     *
     * @param world the world
     * @return the settings
     */
    static VillageNamesSettings read(World world) {
        boolean mapFeatures = world.getWorldInfo()
            .isMapFeaturesEnabled();
        boolean overworld = world.provider.dimensionId == 0;
        boolean villages = VillageGeneratorConfigHandler.newVillageGenerator && worldGeneratesVillages(world, mapFeatures);
        boolean monuments = GeneralConfig.addOceanMonuments && mapFeatures && overworld;
        return new VillageNamesSettings(
            villages,
            monuments,
            VillageGeneratorConfigHandler.newVillageSpacingMedian,
            VillageGeneratorConfigHandler.newVillageSpacingSpread,
            VillageGeneratorConfigHandler.noVillagesRadius,
            VillageNamesSettings.parseSpawnBiomeNames(VillageGeneratorConfigHandler.spawnBiomesNames));
    }

    /**
     * Returns whether the world's generator calls the village generator: on RWG2 with map features and RWG's "Generate
     * Villages"; on classic RWG with "Generate Villages" alone (it builds its village generator whatever the map
     * features say); elsewhere, as vanilla does, with map features.
     */
    private static boolean worldGeneratesVillages(World world, boolean mapFeatures) {
        String type = HeightOracles.worldType(world)
            .toUpperCase(Locale.ROOT);
        if (type.startsWith("RWG2") && Loader.isModLoaded(HeightOracles.RWG2_MOD)) {
            return mapFeatures && Rwg2Switch.generateVillages();
        }
        if ("RWG".equals(type) && Loader.isModLoaded(HeightOracles.RWG_MOD)) {
            return Rwg1Switch.generateVillages();
        }
        return mapFeatures;
    }

    /** RWG2's switch; loaded only when RWG2 is present. */
    private static final class Rwg2Switch {

        static boolean generateVillages() {
            return rwg2.config.ConfigRWG.generateVillages;
        }
    }

    /** Classic RWG's switch; loaded only when classic RWG is present. */
    private static final class Rwg1Switch {

        static boolean generateVillages() {
            return rwg.config.ConfigRWG.generateVillages;
        }
    }
}
