package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** {@link PlannedBridgeInfo} holds a span. */
public class PlannedBridgeInfoTest {

    @Test
    public void holdsItsFields() {
        PlannedBridgeInfo span = new PlannedBridgeInfo(9L, Orientation.Z, 1, 2, 1, 7, 4);
        assertEquals(9L, span.id());
        assertEquals(Orientation.Z, span.orientation());
        assertEquals(2, span.minimumZ());
        assertEquals(7, span.maximumZ());
        assertEquals(4, span.gapLength());
    }
}
