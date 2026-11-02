package com.tuxprogrammer.lostcities.api;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/api/ILostCityProfile.java
// Avoids: CORE-38

/**
 * A profile a mod creates through {@link ILostCityProfileSetup}, configured before its callback returns. Every
 * setter sets a profile option of docs/config.md; values are checked, clamped and cross-checked as a profile file's
 * are when the callback returns, and a profile that fails is rejected with a log line.
 *
 * <p>
 * Changes from upstream (avoids CORE-38): the misspelled {@code setCityChancle} is {@link #setCityChance}; the
 * four-level {@code setCityLevelHeights} is dropped; {@link #setOption} reaches every other option.
 *
 * <p>
 * Side: server.
 */
public interface ILostCityProfile {

    /** Returns the profile's name. */
    String getName();

    /**
     * Sets the first line of text {@code /lostcities profile} shows.
     *
     * @param description the text
     */
    void setDescription(String description);

    /**
     * Sets the world style.
     *
     * @param worldStyle a loaded world style's name
     */
    void setWorldStyle(String worldStyle);

    /**
     * Sets the chance of a city.
     *
     * @param chance -1 to 1; below 0 uses the city-sphere rules
     */
    void setCityChance(double chance);

    /**
     * Sets the ruin chance and the ruined part of a ruined building.
     *
     * @param chance     0 to 1, per building
     * @param minPercent 0 to 1, the least ruined fraction
     * @param maxPercent 0 to 1, the most ruined fraction
     */
    void setRuinChance(float chance, float minPercent, float maxPercent);

    /**
     * Sets the ground level.
     *
     * @param groundLevel block Y, 2 to 244
     */
    void setGroundLevel(int groundLevel);

    /**
     * Sets the eight city level thresholds.
     *
     * @param levels block Y of each threshold, eight values, rising
     */
    void setCityLevelHeights(int... levels);

    /**
     * Sets how far from the coast a city sits at least.
     *
     * @param border in blocks, -255 to 255
     */
    void setOceanCorrectionBorder(int border);

    /**
     * Sets any profile option by its key in a profile file, as text.
     *
     * @param key   the option's key, as docs/config.md lists it
     * @param value its value, as a profile file writes it
     * @throws IllegalArgumentException for an unknown key
     */
    void setOption(String key, String value);
}
