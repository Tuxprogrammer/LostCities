package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.street.Orientation;

/** {@link Direction}: upstream's order, rotations and opposites. */
public class DirectionTest {

    @Test
    public void upstreamOrderAndRotations() {
        assertArrayEquals(
            new Direction[] { Direction.XMIN, Direction.XMAX, Direction.ZMIN, Direction.ZMAX },
            Direction.values());
        assertEquals(0, Direction.XMIN.quarterTurns());
        assertEquals(2, Direction.XMAX.quarterTurns());
        assertEquals(1, Direction.ZMIN.quarterTurns());
        assertEquals(3, Direction.ZMAX.quarterTurns());
        assertEquals(Orientation.Z, Direction.ZMAX.orientation());
        assertEquals(Direction.ZMIN, Direction.ZMAX.opposite());
        assertEquals(-1, Direction.XMIN.stepX());
    }
}
