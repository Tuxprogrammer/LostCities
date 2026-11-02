package com.tuxprogrammer.lostcities.api;

/**
 * A building asset (docs/original-spec/01-core/api.md, {@code ILostCityBuilding}; 05-assets). Upstream's
 * {@code getLocalPalette}, {@code getFillerBlock} and {@code getRubbleBlock} returned internal types and are not
 * exposed (CORE-38).
 *
 * <p>
 * Side: both.
 */
public interface ILostCityBuilding extends ILostCityAsset {

    /** Returns {@code minfloors}, or -1 when unset. */
    int getMinFloors();

    /** Returns {@code maxfloors}, or -1 when unset. */
    int getMaxFloors();

    /** Returns {@code mincellars}, or -1 when unset. */
    int getMinCellars();

    /** Returns {@code maxcellars}, or -1 when unset. */
    int getMaxCellars();

    /** Returns {@code preferslonely}: 0 by default, 1 always alone. */
    float getPrefersLonely();

    /** Returns whether the building gets doors. */
    boolean getAllowDoors();

    /** Returns whether its cellars are filled toward the street. */
    boolean getAllowFillers();

    /** Returns whether its floor limits replace the profile's instead of narrowing them. */
    boolean getOverrideFloors();
}
