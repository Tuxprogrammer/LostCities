package com.tuxprogrammer.lostcities.compat.lootgames;

import net.minecraft.world.World;

import ru.timeconqueror.lootgames.common.config.ConfigGeneral;
import ru.timeconqueror.lootgames.common.config.LGConfigs;

/**
 * Reads LootGames' dungeon settings for a world through its own config object, the values
 * {@code LootGamesWorldGen.checkSpawnConditions} reads: {@code disableDungeonGen}, {@code isDimensionEnabledForWG} and
 * {@code getWorldGenRhombusSize}.
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/eu/usrv/legacylootgames/worldgen/LootGamesWorldGen.java#L38-L67
 * and
 * https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/ru/timeconqueror/lootgames/common/config/ConfigGeneral.java#L130-L137.
 *
 * <p>
 * Load only when {@code Loader.isModLoaded("lootgames")}. Side: server.
 */
final class LootGamesConfig {

    private LootGamesConfig() {}

    /**
     * Returns the settings of a world's dimension.
     *
     * @param world the world
     * @return the settings; {@link LootGamesSettings#OFF} when no dungeon generates there
     */
    static LootGamesSettings read(World world) {
        ConfigGeneral general = LGConfigs.GENERAL;
        if (general == null || general.worldGen == null) {
            return LootGamesSettings.OFF;
        }
        int dimension = world.provider.dimensionId;
        int rhomb = general.worldGen.isDimensionEnabledForWG(dimension)
            ? general.worldGen.getWorldGenRhombusSize(dimension)
            : -1;
        return LootGamesSettings.of(general.worldGen.disableDungeonGen, dimension, rhomb);
    }
}
