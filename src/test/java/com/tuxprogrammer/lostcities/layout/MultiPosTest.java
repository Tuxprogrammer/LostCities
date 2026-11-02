package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link MultiPos}: upstream's position flags. */
public class MultiPosTest {

    @Test
    public void flags() {
        assertTrue(MultiPos.SINGLE.isSingle());
        assertFalse(MultiPos.SINGLE.isRightSide());
        MultiPos topLeft = new MultiPos(0, 0, 2, 3);
        assertTrue(topLeft.isTopLeft());
        assertTrue(topLeft.isMulti());
        MultiPos corner = new MultiPos(1, 2, 2, 3);
        assertTrue(corner.isRightSide());
        assertTrue(corner.isBottomSide());
        assertFalse(corner.isTopLeft());
    }
}
