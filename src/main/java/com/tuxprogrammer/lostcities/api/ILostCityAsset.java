package com.tuxprogrammer.lostcities.api;

/**
 * A loaded asset: a building, multibuilding or city style (docs/original-spec/01-core/api.md, {@code ILostCityAsset}).
 *
 * <p>
 * Side: both.
 */
public interface ILostCityAsset {

    /** Returns the asset's name: its path, {@code building1}. */
    String getName();

    /** Returns the asset's full id, {@code namespace:path}. */
    String getId();
}
