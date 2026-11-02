package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.List;

import greymerk.roguelike.config.RogueConfig;

/**
 * Reads the {@link RoguelikeSettings} from Roguelike Dungeons' own config object, {@code RogueConfig}, which loads
 * {@code config/roguelike_dungeons/roguelike.cfg} on first use and supplies the mod's defaults for missing keys, as the
 * mod itself reads them. Touches Roguelike classes, so only {@link RoguelikeOracles} calls it, after
 * {@code Loader.isModLoaded("Roguelike")}.
 *
 * <p>
 * Source: Roguelike Dungeons 1.6.6-GTNH,
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/config/RogueConfig.java#L150-L182.
 *
 * <p>
 * Side: server.
 */
final class RoguelikeConfig {

    private RoguelikeConfig() {}

    /**
     * Reads the settings.
     *
     * @return the settings
     */
    static RoguelikeSettings read() {
        boolean naturalSpawn = RogueConfig.getBoolean(RogueConfig.DONATURALSPAWN);
        int frequency = RogueConfig.getInt(RogueConfig.SPAWNFREQUENCY);
        int upper = RogueConfig.getInt(RogueConfig.UPPERLIMIT);
        int lower = RogueConfig.getInt(RogueConfig.LOWERLIMIT);
        List<Integer> whitelist = RogueConfig.getIntList(RogueConfig.DIMENSIONWL);
        List<Integer> blacklist = RogueConfig.getIntList(RogueConfig.DIMENSIONBL);
        return new RoguelikeSettings(naturalSpawn, frequency, upper, lower, whitelist, blacklist);
    }
}
