package com.tuxprogrammer.lostcities.compat.ae2;

import net.minecraft.world.World;

import appeng.api.features.IWorldGen.WorldGenType;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import appeng.core.features.registries.WorldGenRegistry;

/**
 * Reads AE2's meteorite settings for a world through AE2's own config objects, the values its generator reads: the
 * {@code MeteoriteWorldGen} feature (without which the generator is never registered), the dimension whitelist as
 * {@code WorldGenRegistry} holds it, {@code minMeteoriteDistance} and {@code meteoriteSpawnChance}.
 *
 * <p>
 * Sources:
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/core/Registration.java#L815-L817
 * (registration behind the feature) and
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/core/features/registries/WorldGenRegistry.java#L66-L84
 * (the dimension test {@code MeteoriteWorldGen.generate} makes first).
 *
 * <p>
 * Load only when {@code Loader.isModLoaded("appliedenergistics2")}. Side: server.
 */
final class Ae2Config {

    private Ae2Config() {}

    /**
     * Returns the settings of a world's dimension.
     *
     * @param world the world
     * @return the settings; {@link MeteoriteSettings#OFF} when no meteorite generates there
     */
    static MeteoriteSettings read(World world) {
        AEConfig config = AEConfig.instance;
        if (config == null) {
            return MeteoriteSettings.OFF;
        }
        boolean generates = config.isFeatureEnabled(AEFeature.MeteoriteWorldGen)
            && WorldGenRegistry.INSTANCE.isWorldGenEnabled(WorldGenType.Meteorites, world);
        return MeteoriteSettings
            .of(generates, world.provider.dimensionId, config.minMeteoriteDistance, config.meteoriteSpawnChance);
    }
}
