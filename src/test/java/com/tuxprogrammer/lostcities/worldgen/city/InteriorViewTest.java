package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The interior shot's camera: inside a room, back to a wall, looking along it. */
public class InteriorViewTest {

    /** Feet level of the test floor. */
    private static final int FEET = 65;

    /** A chunk of solid blocks with rooms carved out; everything outside the chunk is air. */
    private static class Building implements InteriorView.Blocks {

        private final Set<Long> carved = new HashSet<>();
        private final int ceiling;

        Building(int ceiling) {
            this.ceiling = ceiling;
        }

        /** Carves a room's air from (x0, z0) to (x1, z1), feet to below the ceiling. */
        Building room(int x0, int z0, int x1, int z1) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = FEET; y < FEET + ceiling; y++) {
                        carved.add(key(x, y, z));
                    }
                }
            }
            return this;
        }

        @Override
        public boolean air(int x, int y, int z) {
            if (x < 0 || x > 15 || z < 0 || z > 15) {
                return true;
            }
            return carved.contains(key(x, y, z));
        }

        private static long key(int x, int y, int z) {
            return ((long) x << 40) | ((long) y << 20) | z;
        }
    }

    @Test
    public void looksAlongTheLongRoomFromItsEnd() {
        // A corridor-shaped room 12 blocks east-west, 3 wide; the long view is along X.
        Building building = new Building(4).room(2, 6, 13, 8);
        InteriorView view = InteriorView.choose(building, 0, 0, FEET, 8.5, 8.5);
        assertNotNull(view);
        assertEquals(11.0, view.run(), 1e-9, "from one end to the other");
        assertTrue(view.backed(), "a wall at its back");
        assertTrue(view.yaw() == 90f || view.yaw() == 270f, "east or west, along the room: " + view.yaw());
        double x = view.x() - 0.5;
        assertTrue(x == 2 || x == 13, "stands at an end of the room: " + view.x());
        assertEquals(InteriorView.PITCH, view.pitch(), 1e-6);
    }

    @Test
    public void aDiagonalCountsItsLength() {
        Building building = new Building(4).room(1, 1, 10, 10);
        InteriorView view = InteriorView.choose(building, 0, 0, FEET, 8.5, 8.5);
        assertNotNull(view);
        assertEquals(9 * Math.sqrt(2), view.run(), 1e-9, "corner to corner of a 10 × 10 room");
        assertTrue(view.backed());
    }

    @Test
    public void noRoomNoView() {
        // Two blocks of air: nowhere looks along RUN_MIN blocks.
        Building building = new Building(4).room(5, 5, 6, 5);
        assertNull(InteriorView.choose(building, 0, 0, FEET, 8.5, 8.5));
    }

    @Test
    public void outdoorsIsNotARoom() {
        // A room open to the sky: no ceiling within reach above the head.
        Building building = new Building(InteriorView.CEILING_MAX + 4).room(2, 2, 13, 13);
        assertNull(InteriorView.choose(building, 0, 0, FEET, 8.5, 8.5));
    }

    @Test
    public void theRunStopsAtTheChunksSide() {
        // A room that runs out of the chunk's west side: the run counts only what is in the chunk.
        Building building = new Building(4) {

            @Override
            public boolean air(int x, int y, int z) {
                return x < 0 ? y >= FEET && y < FEET + 4 && z == 8 : super.air(x, y, z);
            }
        }.room(0, 8, 6, 8);
        InteriorView view = InteriorView.choose(building, 0, 0, FEET, 8.5, 8.5);
        assertNotNull(view);
        assertEquals(6.0, view.run(), 1e-9, view.x() + " " + view.yaw());
    }

    @Test
    public void standingNeedsAFloorAndHeadroom() {
        Building building = new Building(4).room(3, 3, 3, 3);
        assertTrue(InteriorView.standable(building, 3, FEET, 3));
        assertTrue(!InteriorView.standable(building, 3, FEET - 1, 3), "no air at the feet");
        assertTrue(!InteriorView.standable(building, 3, FEET + 3, 3), "no headroom under the ceiling");
    }
}
