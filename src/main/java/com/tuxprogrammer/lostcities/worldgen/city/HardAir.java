package com.tuxprogrammer.lostcities.worldgen.city;

/**
 * What a part's hard-air character ({@code minecraft:structure_void}) becomes when the part is written: upstream's
 * {@code HardAirSetting} (docs/original-spec/03-terrain/chunk-driver-and-parts.md, {@code generatePart} step 5).
 *
 * <p>
 * Side: both (plain Java).
 */
public enum HardAir {

    /** Air: building floors, park and fountain parts, fronts and stairs. */
    AIR,
    /** The liquid below the water level, air above, unless the part says {@code nowater} or foliage is avoided. */
    WATERLEVEL,
    /** Keep whatever is already there: street, slope, plaza and connector parts, bridges. */
    VOID
}
