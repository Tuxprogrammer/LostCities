package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Builds the layout's view of a world's pinned profile ({@link LayoutProfile}) from the values M04 pins
 * ({@link TerrainSettings}): ground and sea level, the city level thresholds, the height window, the height sample
 * size, the rise cap, the ruin chances, the ocean correction border and the landmark margin. Every other layout option
 * keeps
 * upstream's default until M09 pins custom profiles (docs/new-spec/02-profiles-dimensions/), so both halves of a world
 * read the same numbers.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class CityProfiles {

    /** The world style of upstream's {@code default} profile; M09 makes it a pinned profile value. */
    public static final AssetId WORLD_STYLE = AssetId.parse("lostcities:standard");

    private CityProfiles() {}

    /**
     * Returns the layout profile of pinned settings.
     *
     * @param settings the world's pinned settings
     * @return the profile
     */
    public static LayoutProfile layoutProfile(TerrainSettings settings) {
        return LayoutProfile.builder()
            .levels(settings.groundLevel(), settings.waterLevel())
            .cityLevelHeights(settings.cityLevelHeights())
            .cityHeightWindow(settings.cityMinHeight(), settings.cityMaxHeight())
            .cityMaxRise(settings.cityMaxRise())
            .averageHeightmap(false, settings.heightSampleSize())
            .ruins(settings.ruinChance(), settings.ruinMinlevelPercent(), settings.ruinMaxlevelPercent())
            .oceanCorrectionBorder(settings.oceanCorrectionBorder())
            .coexistence(
                settings.landmarkMargin(),
                LayoutProfile.defaults().villageTransitionRings,
                LayoutProfile.defaults().villageLinkDistance)
            .build();
    }
}
