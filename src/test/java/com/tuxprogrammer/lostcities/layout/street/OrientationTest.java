package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** {@link Orientation#along}. */
public class OrientationTest {

    @Test
    public void alongPicksTheAxisCoordinate() {
        assertEquals(3, Orientation.X.along(3, 9));
        assertEquals(9, Orientation.Z.along(3, 9));
    }
}
