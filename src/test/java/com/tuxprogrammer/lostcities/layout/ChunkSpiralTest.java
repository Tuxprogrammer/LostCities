package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** {@link ChunkSpiral}: upstream's spiral order over the whole square, nearest first. */
public class ChunkSpiralTest {

    @Test
    public void startsAtTheCenterThenEastThenSouth() {
        List<String> order = new ArrayList<>();
        ChunkSpiral.walk(10, 20, 1, (x, z) -> order.add(x + "," + z));
        assertEquals("10,20", order.get(0));
        assertEquals("11,20", order.get(1), "the first step goes east");
        assertEquals("11,21", order.get(2), "then south");
        assertEquals("10,21", order.get(3), "then west");
        assertEquals(9, order.size());
    }

    @Test
    public void visitsEveryChunkOfTheSquareOnce() {
        int radius = ChunkSpiral.LOCATE_RADIUS;
        Set<Long> seen = new HashSet<>();
        int[] count = { 0 };
        int visited = ChunkSpiral.walk(-3, 5, radius, (x, z) -> {
            assertTrue(Math.abs(x + 3) <= radius && Math.abs(z - 5) <= radius, "outside the square: " + x + "," + z);
            assertTrue(seen.add(((long) x << 32) ^ (z & 0xffffffffL)), "visited twice: " + x + "," + z);
            count[0]++;
            return true;
        });
        int side = 2 * radius + 1;
        assertEquals(side * side, visited);
        assertEquals(side * side, count[0]);
    }

    @Test
    public void nearerRingsComeFirst() {
        int[] lastRing = { 0 };
        ChunkSpiral.walk(0, 0, 6, (x, z) -> {
            int ring = Math.max(Math.abs(x), Math.abs(z));
            assertTrue(ring >= lastRing[0] - 1, "ring " + ring + " after ring " + lastRing[0]);
            lastRing[0] = Math.max(lastRing[0], ring);
            return true;
        });
    }

    @Test
    public void theVisitorStopsTheWalk() {
        int visited = ChunkSpiral.walk(0, 0, 30, (x, z) -> x != 2);
        assertTrue(visited < 30, "stopped after " + visited);
    }
}
