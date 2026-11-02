package com.tuxprogrammer.lostcities.layout.view;

/**
 * The layout's whole read-only view of the loaded assets: lookups by name, and the profile's world style. This is the
 * narrow interface M02's asset model satisfies; the layout depends on nothing else of it.
 *
 * <p>
 * Lookups return {@code null} for an unknown name. The loader cross-checks every name an asset refers to, so in a
 * loaded pack that only happens for a broken asset; the layout then falls back with one log line rather than
 * crashing mid-generation (CONTRIBUTING § Code standards).
 *
 * <p>
 * Side: both.
 */
public interface AssetView {

    /** Returns the profile's world style. */
    WorldStyleView worldStyle();

    /**
     * Looks up a city style.
     *
     * @param name the city style's name
     * @return the resolved style, or {@code null}
     */
    CityStyleView cityStyle(String name);

    /**
     * Looks up a building.
     *
     * @param name the building's name
     * @return the building, or {@code null}
     */
    BuildingView building(String name);

    /**
     * Looks up a multibuilding.
     *
     * @param name the multibuilding's name
     * @return the multibuilding, or {@code null}
     */
    MultiBuildingView multiBuilding(String name);

    /**
     * Looks up a {@code style} (palette choices).
     *
     * @param name the style's name
     * @return the style, or {@code null}
     */
    StyleView style(String name);

    /**
     * Returns whether a part carries the {@code dontconnect} metadata: no doorway or front may open into it.
     *
     * @param partName the part's name
     * @return whether the part refuses connections; {@code false} for an unknown part
     */
    boolean partDontConnect(String partName);
}
