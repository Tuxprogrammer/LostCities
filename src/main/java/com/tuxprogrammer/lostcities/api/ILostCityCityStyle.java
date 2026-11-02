package com.tuxprogrammer.lostcities.api;

/**
 * A city style asset (docs/original-spec/01-core/api.md, {@code ILostCityCityStyle}; 05-assets). Each number is the
 * style's own value or its parent's, {@code null} when neither sets it. Upstream's block getters returned palette
 * characters of internal palettes and are not exposed (CORE-38).
 *
 * <p>
 * Side: both.
 */
public interface ILostCityCityStyle extends ILostCityAsset {

    /** Returns the style of palettes the city style uses. */
    String getStyle();

    /** Returns {@code explosionchance}. */
    Float getExplosionChance();

    /** Returns {@code minfloors}. */
    Integer getMinFloorCount();

    /** Returns {@code maxfloors}. */
    Integer getMaxFloorCount();

    /** Returns {@code mincellars}. */
    Integer getMinCellarCount();

    /** Returns {@code maxcellars}. */
    Integer getMaxCellarCount();

    /** Returns {@code buildingchance}. */
    Float getBuildingChance();

    /** Returns {@code parkchance}. */
    Float getParkChance();

    /** Returns {@code fountainchance}. */
    Float getFountainChance();

    /** Returns {@code corridorchance}. */
    Float getCorridorChance();

    /** Returns {@code frontchance}. */
    Float getFrontChance();

    /** Returns {@code avoidfoliage}. */
    Boolean getAvoidFoliage();

    /** Returns {@code parkborder}. */
    Boolean getParkBorder();

    /** Returns {@code parkelevation}. */
    Boolean getParkElevation();

    /** Returns {@code parkstreetthreshold}. */
    Integer getParkStreetThreshold();
}
