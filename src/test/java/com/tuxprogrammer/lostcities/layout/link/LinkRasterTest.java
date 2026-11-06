package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Rasterizing a link into chunks (0015 §2). */
class LinkRasterTest {

    private static int count(List<LinkRaster.Step> path, LinkRaster.Kind kind) {
        int n = 0;
        for (LinkRaster.Step s : path) {
            n += s.kind() == kind ? 1 : 0;
        }
        return n;
    }

    @Test
    void pathIsConnectedAndEndsAtBothCities() {
        Random random = new Random(5);
        for (int i = 0; i < 2000; i++) {
            int ax = random.nextInt(400) - 200;
            int az = random.nextInt(400) - 200;
            int bx = ax + random.nextInt(321) - 160;
            int bz = az + random.nextInt(321) - 160;
            List<LinkRaster.Step> path = LinkRaster.rasterize(ax, az, bx, bz);
            assertEquals(Math.max(Math.abs(bx - ax), Math.abs(bz - az)) + 1, path.size());
            LinkRaster.Step first = path.get(0);
            LinkRaster.Step last = path.get(path.size() - 1);
            assertTrue(first.chunkX() == ax && first.chunkZ() == az || first.chunkX() == bx && first.chunkZ() == bz);
            assertTrue(last.chunkX() == ax && last.chunkZ() == az || last.chunkX() == bx && last.chunkZ() == bz);
            for (int k = 1; k < path.size(); k++) {
                LinkRaster.Step p = path.get(k - 1);
                LinkRaster.Step s = path.get(k);
                assertEquals(s.chunkX() - p.chunkX(), p.outDx());
                assertEquals(s.chunkZ() - p.chunkZ(), p.outDz());
                assertEquals(p.outDx(), s.inDx());
                assertEquals(p.outDz(), s.inDz());
            }
            if (path.size() > 1) {
                assertEquals(LinkRaster.Kind.END, first.kind());
                assertEquals(LinkRaster.Kind.END, last.kind());
            }
            // At most two transitions: straight, diagonal, straight.
            assertTrue(count(path, LinkRaster.Kind.TRANSITION) <= 2);
        }
    }

    @Test
    void sameChunksFromEitherEnd() {
        assertEquals(
            LinkRaster.rasterize(10, -40, 130, 25)
                .toString(),
            LinkRaster.rasterize(130, 25, 10, -40)
                .toString());
    }

    @Test
    void fortyFiveDegreesIsAllDiagonal() {
        List<LinkRaster.Step> path = LinkRaster.rasterize(0, 0, 50, -50);
        assertEquals(49, count(path, LinkRaster.Kind.DIAGONAL));
        assertEquals(0, count(path, LinkRaster.Kind.STRAIGHT));
        assertEquals(0, count(path, LinkRaster.Kind.TRANSITION));
    }

    @Test
    void nearDiagonalLinksAreMostlyDiagonal() {
        for (int dz = 58; dz <= 100; dz++) {
            // Angles from about 30 to 45 degrees.
            List<LinkRaster.Step> path = LinkRaster.rasterize(0, 0, 100, dz);
            assertTrue(count(path, LinkRaster.Kind.DIAGONAL) >= count(path, LinkRaster.Kind.STRAIGHT), "dz " + dz);
        }
    }

    @Test
    void axisLinksAreStraight() {
        List<LinkRaster.Step> path = LinkRaster.rasterize(0, 0, 0, 30);
        assertEquals(29, count(path, LinkRaster.Kind.STRAIGHT));
        assertEquals(0, count(path, LinkRaster.Kind.DIAGONAL));
    }

    @Test
    void transitionsSitBetweenStraightAndDiagonal() {
        List<LinkRaster.Step> path = LinkRaster.rasterize(0, 0, 40, 10);
        // 30 straight moves split 15 and 15 around 10 diagonal moves.
        assertEquals(LinkRaster.Kind.TRANSITION, path.get(15).kind());
        assertEquals(LinkRaster.Kind.DIAGONAL, path.get(16).kind());
        assertEquals(LinkRaster.Kind.TRANSITION, path.get(25).kind());
        assertEquals(LinkRaster.Kind.STRAIGHT, path.get(26).kind());
    }
}
