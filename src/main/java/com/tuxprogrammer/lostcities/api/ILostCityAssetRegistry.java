package com.tuxprogrammer.lostcities.api;

import java.util.List;

/**
 * The loaded assets of one type (docs/original-spec/01-core/api.md, {@code ILostCityAssetRegistry}). Side: both.
 *
 * @param <T> the asset type
 */
public interface ILostCityAssetRegistry<T extends ILostCityAsset> {

    /**
     * Returns an asset by name: {@code name} means {@code lostcities:name}, or a full {@code namespace:path}.
     *
     * @param name the name
     * @return the asset, or {@code null}
     */
    T get(String name);

    /** Returns every asset of the type, sorted by id. */
    List<T> getAll();
}
