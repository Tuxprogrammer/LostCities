package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** One explosion's geometry. */
public class ExplosionTest {

    @Test
    public void distancesAreMeasuredFromTheCenterBlocksCenter() {
        Explosion explosion = new Explosion(10, 64, -3, 12);
        assertEquals(0.75, explosion.squaredDistanceTo(10, 64, -3), 1e-9);
        assertEquals(144, explosion.squaredRadius());
    }

    @Test
    public void aRadiusBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Explosion(0, 0, 0, 0));
    }
}
