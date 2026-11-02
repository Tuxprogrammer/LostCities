package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/EffectiveStreetResolver.java

/**
 * The final clip of the raw street field to the city: a chunk keeps its raw road only when it is city, at least one
 * neighbor the road continues into is raw city, and no higher-precedence content (a predefined building or street, a
 * multibuilding) occupies it. It removes one-chunk stubs where the city mask barely touches the field. A landmark core
 * is not city, so roads are clipped there too (0006 §3).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/chunk-characteristics.md#geteffectiveplannedroadtype. Side: both.
 */
public final class EffectiveStreetResolver {

    private EffectiveStreetResolver() {}

    /**
     * Resolves the effective road type.
     *
     * @param raw                   the raw road type at the chunk
     * @param isCity                whether the chunk is city
     * @param connectedCityNeighbor whether the road continues into a raw-city neighbor
     * @param overridden            whether higher-precedence content occupies the chunk
     * @return {@code raw}, or {@link PlannedRoadType#NONE}
     */
    public static PlannedRoadType resolve(PlannedRoadType raw, boolean isCity, boolean connectedCityNeighbor,
        boolean overridden) {
        if (!isCity || !connectedCityNeighbor || overridden) {
            return PlannedRoadType.NONE;
        }
        return raw;
    }
}
