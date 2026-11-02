package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** {@link EffectiveStreetResolver}: the raw road survives only for a connected, unoccupied city chunk. */
public class EffectiveStreetResolverTest {

    @Test
    public void keepsTheRawRoadOnlyWhenAllThreeHold() {
        PlannedRoadType raw = PlannedRoadType.SECONDARY;
        assertEquals(raw, EffectiveStreetResolver.resolve(raw, true, true, false));
        assertEquals(PlannedRoadType.NONE, EffectiveStreetResolver.resolve(raw, false, true, false));
        assertEquals(PlannedRoadType.NONE, EffectiveStreetResolver.resolve(raw, true, false, false));
        assertEquals(PlannedRoadType.NONE, EffectiveStreetResolver.resolve(raw, true, true, true));
    }
}
