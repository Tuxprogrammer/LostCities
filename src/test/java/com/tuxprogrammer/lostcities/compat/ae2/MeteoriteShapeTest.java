package com.tuxprogrammer.lostcities.compat.ae2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Meteorite shapes against values recorded from AE2's {@code MeteoritePlacer} constructor draws. */
class MeteoriteShapeTest {

    @Test
    void sizesMatchAe2() {
        assertEquals(
            5.908568385891192D,
            MeteoriteShape.of(-7543508780968384435L)
                .size(),
            0);
        assertEquals(
            2.2940066428696255D,
            MeteoriteShape.of(5058118976279521559L)
                .size(),
            0);
        assertEquals(
            6.254031964657017D,
            MeteoriteShape.of(-8045570049540908799L)
                .size(),
            0);
        assertEquals(
            5.973324098177734D,
            MeteoriteShape.of(3532439230978648689L)
                .size(),
            0);
    }

    @Test
    void craterIsTwiceTheSizePlusFive() {
        for (long seed = 1; seed < 500; seed++) {
            MeteoriteShape shape = MeteoriteShape.of(seed);
            assertTrue(shape.size() >= 2 && shape.size() < 8, shape.toString());
            assertEquals(shape.size() * 2 + 5, shape.craterSize(), 0);
        }
    }

    @Test
    void aboutOneInTenHasLava() {
        int lava = 0;
        for (long seed = 1; seed <= 10_000; seed++) {
            lava += MeteoriteShape.of(seed)
                .lava() ? 1 : 0;
        }
        assertTrue(lava > 800 && lava < 1200, "lava meteorites: " + lava);
    }
}
