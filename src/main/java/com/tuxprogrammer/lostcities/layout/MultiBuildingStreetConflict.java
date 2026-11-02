package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/MultiBuildingStreetConflict.java

import java.util.Locale;

import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;

/**
 * Which raw roads a multibuilding may not cover: the profile's {@code multiBuildingStreetConflict}. Roads a
 * multibuilding is allowed over are removed afterwards by the effective-road clip, since a multibuilding chunk is
 * "overridden" content.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/multibuildings.md#canplacebuilding. Side: both.
 */
public enum MultiBuildingStreetConflict {

    /** Any road blocks a multibuilding. */
    BLOCK_ALL,
    /** Only primary roads block; the default. */
    OVERRIDE_MINOR,
    /** No road blocks. */
    OVERRIDE_ALL;

    /**
     * Returns whether a raw road blocks a multibuilding under this policy.
     *
     * @param raw the chunk's raw road class
     * @return whether a multibuilding may not cover the chunk
     */
    public boolean roadBlocks(PlannedRoadType raw) {
        switch (this) {
            case BLOCK_ALL:
                return raw != PlannedRoadType.NONE;
            case OVERRIDE_MINOR:
                return raw == PlannedRoadType.PRIMARY;
            default:
                return false;
        }
    }

    /**
     * Parses a policy name, ignoring case under {@link Locale#ROOT}.
     *
     * @param name the profile value
     * @return the policy
     * @throws IllegalArgumentException for an unknown name, naming it
     */
    public static MultiBuildingStreetConflict byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown multi-building street conflict policy '" + name + "'", e);
        }
    }
}
