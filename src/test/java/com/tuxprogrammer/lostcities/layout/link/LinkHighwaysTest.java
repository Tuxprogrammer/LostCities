package com.tuxprogrammer.lostcities.layout.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.highway.Axis;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo.Piece;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo.Shape;

/**
 * Pieces, level profiles and code-written geometry of the link highways (design record 0015 §3, §5).
 */
public class LinkHighwaysTest {

    private static LinkHighways.Ends ends(int radius, int levelA, int levelB, int ax, int az) {
        return new LinkHighways.Ends() {

            @Override
            public int level(int chunkX, int chunkZ) {
                return chunkX == ax && chunkZ == az ? levelA : levelB;
            }

            @Override
            public int radius(int chunkX, int chunkZ) {
                return radius;
            }
        };
    }

    // ------------------------------------------------------------------ piece shapes

    @Test
    public void straightAndRampPiecesFollowTheLegacyConvention() {
        Piece straight = Piece.link(-1, 0, 2, 1, 0, 2);
        assertEquals(Shape.STRAIGHT, straight.shape());
        assertEquals(Axis.X, straight.axis());
        assertEquals(0, straight.quarterTurns());
        // Entered from the east at level 3, leaves west at 2: low (west) 2, high (east) 3.
        Piece ramp = Piece.link(1, 0, 3, -1, 0, 2);
        assertEquals(Shape.RAMP, ramp.shape());
        assertEquals(2, ramp.lowLevel());
        assertEquals(3, ramp.highLevel());
        assertEquals(3, ramp.levelAt(1, 0));
        assertEquals(2, ramp.levelAt(-1, 0));
        assertEquals(-1, ramp.levelAt(0, 1));
        Piece z = Piece.link(0, 1, 1, 0, -1, 1);
        assertEquals(Axis.Z, z.axis());
        assertEquals(1, z.quarterTurns());
    }

    @Test
    public void diagonalPiecesTakeTheirTurnFromTheLine() {
        Piece nwSe = Piece.link(-1, -1, 1, 1, 1, 2);
        assertEquals(Shape.DIAGONAL, nwSe.shape());
        assertEquals(0, nwSe.quarterTurns());
        assertEquals(1, nwSe.lowLevel(), "west corner");
        assertEquals(2, nwSe.highLevel(), "east corner");
        assertTrue(nwSe.coded());
        Piece neSw = Piece.link(1, -1, 4, -1, 1, 4);
        assertEquals(Shape.DIAGONAL, neSw.shape());
        assertEquals(1, neSw.quarterTurns());
        assertEquals(4, neSw.levelAt(-1, 1));
        assertEquals(-1, neSw.levelAt(1, 1));
    }

    @Test
    public void transitionsJoinASideToAFarCorner() {
        // Enters from the west, leaves by the south-east corner.
        Piece t = Piece.link(-1, 0, 3, 1, 1, 3);
        assertEquals(Shape.TRANSITION, t.shape());
        assertEquals(0, t.quarterTurns(), "west side");
        assertEquals(
            3,
            Piece.link(1, -1, 0, 0, 1, 0)
                .quarterTurns(),
            "south side");
        assertEquals(
            1,
            Piece.link(0, -1, 0, -1, 1, 0)
                .quarterTurns(),
            "north side");
        assertEquals(
            2,
            Piece.link(1, 0, 0, -1, -1, 0)
                .quarterTurns(),
            "east side");
        assertThrows(IllegalArgumentException.class, () -> Piece.link(-1, 0, 0, -1, 1, 0), "near corner");
        assertThrows(IllegalArgumentException.class, () -> Piece.link(-1, 0, 0, 1, 1, 1), "transitions stay flat");
        assertEquals(Shape.BEND, Piece.link(-1, 0, 0, 0, 1, 0).shape(), "a quarter bend is the staircase's bend");
        assertThrows(IllegalArgumentException.class, () -> Piece.link(-1, 0, 0, 0, 1, 1), "bends stay flat");
    }

    @Test
    public void straightPiecesOnBothAxesMergeIntoACrossing() {
        List<Piece> merged = LinkHighways.merge(
            java.util.Arrays
                .asList(Piece.link(-1, 0, 2, 1, 0, 2), Piece.link(0, -1, 2, 0, 1, 2), Piece.link(-1, 0, 2, 1, 0, 2)));
        assertEquals(1, merged.size());
        assertEquals(
            Shape.CROSSING,
            merged.get(0)
                .shape());
        assertEquals(
            4,
            merged.get(0)
                .openings().length / 3);
        List<Piece> stacked = LinkHighways
            .merge(java.util.Arrays.asList(Piece.link(-1, 0, 3, 1, 0, 3), Piece.link(0, -1, 1, 0, 1, 1)));
        assertEquals(2, stacked.size(), "different levels pass over each other");
        assertEquals(
            1,
            stacked.get(0)
                .level(),
            "lowest first");
    }

    // ------------------------------------------------------------------ profiles

    @Test
    public void cityChunksAreSkippedAndTheFirstChunkOutsideIsAPlaza() {
        CityLinkGraph.Link link = new CityLinkGraph.Link(0, 0, 40, 10);
        // 100 blocks: chunks up to 6.25 chunks from a center are inside.
        LinkHighways.Profile p = LinkHighways.compute(link, ends(100, 2, 2, 0, 0));
        List<LinkRaster.Step> path = link.path();
        assertTrue(p.written());
        LinkRaster.Step first = path.get(p.start());
        LinkRaster.Step before = path.get(p.start() - 1);
        assertTrue(256 * (sq(first.chunkX()) + sq(first.chunkZ())) > 100 * 100);
        assertTrue(256 * (sq(before.chunkX()) + sq(before.chunkZ())) <= 100 * 100);
        LinkHighways view = new LinkHighways(null, ends(100, 2, 2, 0, 0));
        Piece plaza = view.piece(link, p.start());
        assertEquals(Shape.CROSSING, plaza.shape());
        assertEquals(2, plaza.level());
        assertNull(view.piece(link, p.start() - 1), "inside the city");
        assertEquals(
            Shape.CROSSING,
            view.piece(link, p.end())
                .shape());
    }

    @Test
    public void citiesThatCoverTheWholeLinkWriteNothing() {
        CityLinkGraph.Link link = new CityLinkGraph.Link(0, 0, 6, 0);
        assertFalse(
            LinkHighways.compute(link, ends(256, 0, 0, 0, 0))
                .written());
    }

    @Test
    public void levelsMatchAtEverySeamAndRampsPreferDiagonals() {
        int[][] links = { { 0, 0, 40, 10 }, { 0, 0, 30, 30 }, { 0, 0, 50, 0 }, { 5, 9, -20, 30 }, { 0, 0, 3, 1 },
            { 0, 0, 2, 2 } };
        for (int[] l : links) {
            for (int[] levels : new int[][] { { 1, 4 }, { 5, 0 }, { 3, 3 } }) {
                CityLinkGraph.Link link = new CityLinkGraph.Link(l[0], l[1], l[2], l[3]);
                LinkHighways.Ends ends = ends(0, levels[0], levels[1], link.ax(), link.az());
                LinkHighways view = new LinkHighways(null, ends);
                LinkHighways.Profile p = view.profile(link);
                List<LinkRaster.Step> path = link.path();
                assertEquals(0, p.start());
                assertEquals(path.size() - 1, p.end());
                assertEquals(levels[0], p.inLevel(p.start()), link + " leaves its lower end at its level");
                int ramps = 0;
                for (int i = p.start(); i <= p.end(); i++) {
                    Piece piece = view.piece(link, i);
                    assertNotNull(piece);
                    if (p.inLevel(i) != p.outLevel(i)) {
                        ramps++;
                        assertTrue(piece.shape() == Shape.RAMP || piece.shape() == Shape.DIAGONAL, piece.toString());
                    }
                    if (i < p.end()) {
                        LinkRaster.Step s = path.get(i);
                        Piece next = view.piece(link, i + 1);
                        int here = piece.levelAt(s.outDx(), s.outDz());
                        assertTrue(here >= 0, link + " opens toward its next chunk at " + i);
                        assertEquals(here, next.levelAt(-s.outDx(), -s.outDz()), link + " seam " + i);
                    }
                }
                int rampable = 0;
                for (int i = p.start() + 1; i < p.end(); i++) {
                    LinkRaster.Kind k = path.get(i)
                        .kind();
                    rampable += k == LinkRaster.Kind.STRAIGHT || k == LinkRaster.Kind.DIAGONAL ? 1 : 0;
                }
                assertEquals(Math.min(Math.abs(levels[1] - levels[0]), rampable), ramps, link.toString());
                if (rampable >= Math.abs(levels[1] - levels[0])) {
                    assertEquals(levels[1], p.outLevel(p.end()), link + " reaches the far city's level");
                }
            }
        }
    }

    // ------------------------------------------------------------------ geometry

    @Test
    public void diagonalDecksSpillOnlyIntoTheSideNeighborsOfTheirCorners() {
        Piece d = Piece.link(-1, -1, 0, 1, 1, 0);
        boolean[][] touched = new boolean[5][5];
        for (int x = -32; x < 48; x++) {
            for (int z = -32; z < 48; z++) {
                if (LinkGeometry.column(0, 0, d, x, z) != null) {
                    touched[Math.floorDiv(x, 16) + 2][Math.floorDiv(z, 16) + 2] = true;
                }
            }
        }
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                boolean expected = cx == 0 && cz == 0 || LinkHighways.spillsInto(d, cx, cz);
                assertEquals(expected, touched[cx + 2][cz + 2], "chunk " + cx + "," + cz);
            }
        }
        // The diagonal's own chunks on the path are not reached past its corners.
        assertFalse(touched[3][3]);
        assertFalse(touched[1][1]);
    }

    @Test
    public void aDiagonalChainLeavesNoHoleAlongItsLine() {
        Piece d = Piece.link(-1, -1, 0, 1, 1, 0);
        int[] xs = { 0, 1, 2 };
        int[] zs = { 0, 1, 2 };
        Piece[] pieces = { d, d, d };
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 48; z++) {
                double across = (z - x) / Math.sqrt(2);
                double along = (x + z + 1) / Math.sqrt(2);
                if (Math.abs(across) < 7.9 && along > 1 && along < 48 * Math.sqrt(2) - 1) {
                    assertNotNull(LinkGeometry.highest(xs, zs, pieces, 3, x, z), "hole at " + x + "," + z);
                }
            }
        }
    }

    @Test
    public void diagonalRampsClimbOneLevelByHalfBlocks() {
        Piece d = Piece.link(-1, -1, 2, 1, 1, 3);
        LinkGeometry.Column start = LinkGeometry.column(0, 0, d, 0, 0);
        LinkGeometry.Column end = LinkGeometry.column(0, 0, d, 15, 15);
        assertEquals(2, start.level());
        assertTrue(start.half() <= 1, "starts at the lower level");
        assertTrue(end.half() >= LinkGeometry.RAMP_HALF_STEPS - 1, "ends at the upper level");
        for (int x = -8; x < 24; x++) {
            for (int z = -8; z < 24; z++) {
                LinkGeometry.Column c = LinkGeometry.column(0, 0, d, x, z);
                LinkGeometry.Column e = LinkGeometry.column(0, 0, d, x + 1, z);
                LinkGeometry.Column s = LinkGeometry.column(0, 0, d, x, z + 1);
                if (c != null && e != null) {
                    assertTrue(Math.abs(c.height() - e.height()) <= 1, x + "," + z);
                }
                if (c != null && s != null) {
                    assertTrue(Math.abs(c.height() - s.height()) <= 1, x + "," + z);
                }
            }
        }
        // Falling the other way: the same deck seen from the higher corner.
        Piece back = Piece.link(1, 1, 3, -1, -1, 2);
        assertEquals(
            LinkGeometry.column(0, 0, d, 5, 7)
                .height(),
            LinkGeometry.column(0, 0, back, 5, 7)
                .height());
    }

    @Test
    public void transitionsCoverTheirSideSeamAndMeetTheDiagonalAtTheCorner() {
        // From the west side to the south-east corner, then a diagonal in chunk (1, 1).
        Piece t = Piece.link(-1, 0, 1, 1, 1, 1);
        Piece d = Piece.link(-1, -1, 1, 1, 1, 1);
        for (int z = 0; z < 16; z++) {
            assertNotNull(LinkGeometry.column(0, 0, t, 0, z), "west seam row " + z);
        }
        // The outside of the turn near the center is filled by the disc.
        assertNotNull(LinkGeometry.column(0, 0, t, 9, 2));
        int[] xs = { 0, 1 };
        int[] zs = { 0, 1 };
        Piece[] pieces = { t, d };
        for (int i = 10; i < 22; i++) {
            assertNotNull(LinkGeometry.highest(xs, zs, pieces, 2, i, i), "line at " + i);
        }
        assertTrue(LinkHighways.spillsInto(t, 1, 0));
        assertTrue(LinkHighways.spillsInto(t, 0, 1));
        assertFalse(LinkHighways.spillsInto(t, -1, 0));
    }

    @Test
    public void plazasFillTheWedgesBesideADiagonalLeavingTheirCorner() {
        Piece plaza = Piece.plaza(2, 1, 1);
        assertTrue(LinkGeometry.coded(plaza));
        assertNull(LinkGeometry.column(0, 0, plaza, 15, 15), "the plaza's own part covers its chunk");
        assertNotNull(LinkGeometry.column(0, 0, plaza, 17, 14), "east wedge");
        assertNotNull(LinkGeometry.column(0, 0, plaza, 14, 17), "south wedge");
        assertFalse(LinkGeometry.coded(Piece.plaza(2, 1, 0)));
    }

    @Test
    public void linkInfoReportsLevelsOnBothAxesForDiagonalsAndSpills() {
        HighwayInfo diagonal = new HighwayInfo(
            java.util.Collections.singletonList(Piece.link(-1, -1, 2, 1, 1, 3)),
            -1,
            -1);
        assertTrue(diagonal.hasHighway());
        assertEquals(3, diagonal.xLevel());
        assertEquals(3, diagonal.zLevel());
        assertTrue(diagonal.deckAt(2));
        HighwayInfo spill = new HighwayInfo(java.util.Collections.<Piece>emptyList(), 1, 1);
        assertTrue(spill.hasHighway());
        assertTrue(spill.spill());
        assertEquals(1, spill.xLevel());
        assertTrue(
            spill.pieces(0, 0)
                .isEmpty());
        HighwayInfo xOnly = new HighwayInfo(java.util.Collections.singletonList(Piece.link(-1, 0, 2, 1, 0, 2)), -1, -1);
        assertEquals(2, xOnly.xLevel());
        assertEquals(-1, xOnly.zLevel());
    }

    // ------------------------------------------------------------------ junctions and staircase (0015 §3, §5)

    @Test
    public void flatPiecesAtOneLevelBecomeOneJunction() {
        Piece j = one(Piece.link(-1, 0, 2, 1, 0, 2), Piece.link(-1, 0, 2, 1, -1, 2));
        assertEquals(Shape.LINK_JUNCTION, j.shape(), "straight and branch");
        assertTrue(j.coded());
        assertEquals(2, j.levelAt(-1, 0));
        assertEquals(2, j.levelAt(1, 0));
        assertEquals(2, j.levelAt(1, -1));
        assertEquals(-1, j.levelAt(0, -1), "no opening north");
        Piece x = one(Piece.link(-1, -1, 0, 1, 1, 0), Piece.link(1, -1, 0, -1, 1, 0));
        assertEquals(Shape.LINK_JUNCTION, x.shape(), "X of two diagonals");
        assertEquals(4, x.openings().length / 3);
        Piece fork = one(Piece.link(-1, 0, 1, 1, -1, 1), Piece.link(-1, 0, 1, 1, 1, 1));
        assertEquals(3, fork.openings().length / 3, "fork of two transitions");
        Piece plaza = one(Piece.plaza(1, 1, 0), Piece.link(-1, -1, 1, 1, 1, 1));
        assertEquals(Shape.CROSSING, plaza.shape(), "a plaza takes in a diagonal");
        assertEquals(1, plaza.levelAt(1, 1));
        assertEquals(1, plaza.levelAt(-1, -1));
        // Sides only: parts with the legacy quarter turns.
        Piece bend = Piece.sides(0, -1, 0, 0, 1);
        assertEquals(Shape.BEND, bend.shape());
        assertEquals(0, bend.quarterTurns(), "west and south");
        assertEquals(1, Piece.sides(0, 0, -1, -1, 0).quarterTurns(), "north and west");
        assertEquals(2, Piece.sides(0, 0, -1, 1, 0).quarterTurns(), "north and east");
        assertEquals(3, Piece.sides(0, 0, 1, 1, 0).quarterTurns(), "south and east");
        Piece t = one(bend, Piece.link(-1, 0, 0, 1, 0, 0));
        assertEquals(Shape.T_JUNCTION, t.shape());
        assertEquals(0, t.quarterTurns(), "no north side");
        assertEquals(3, one(Piece.sides(0, 0, -1, 1, 0), Piece.link(0, -1, 0, 0, 1, 0)).quarterTurns(), "no west");
        assertFalse(t.coded());
        List<Piece> apart = LinkHighways.merge(
            java.util.Arrays
                .asList(Piece.link(-1, -1, 0, 1, 1, 1), Piece.link(-1, 0, 1, 1, 0, 1), Piece.link(0, -1, 3, 0, 1, 3)));
        assertEquals(3, apart.size(), "ramps and other levels stay apart");
    }

    @Test
    public void aLinkJunctionIsOneDeckWithRailingsOnlyOnItsOuterEdge() {
        Piece branch = Piece.link(-1, 0, 0, 1, -1, 0);
        Piece j = one(Piece.link(-1, 0, 0, 1, 0, 0), branch);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                LinkGeometry.Column c = LinkGeometry.column(0, 0, j, x, z);
                assertNotNull(c, "the straight road is all deck at " + x + "," + z);
                if (z >= 1 && z <= 14) {
                    assertTrue(c.row() >= 1 && c.row() <= 14, "no railing across the straight road at " + x + "," + z);
                }
            }
        }
        Piece d1 = Piece.link(-1, -1, 0, 1, 1, 0);
        Piece d2 = Piece.link(1, -1, 0, -1, 1, 0);
        Piece xj = one(d1, d2);
        for (Piece[] pair : new Piece[][] { { j, branch }, { xj, d1 }, { xj, d2 } }) {
            for (int x = -16; x < 32; x++) {
                for (int z = -16; z < 32; z++) {
                    LinkGeometry.Column part = LinkGeometry.column(0, 0, pair[1], x, z);
                    boolean disc = part != null && part.along() == 0 && part.across() == 0;
                    if (part == null || disc || Math.abs(part.across()) >= LinkGeometry.HALF_WIDTH - 1) {
                        continue;
                    }
                    LinkGeometry.Column c = LinkGeometry.column(0, 0, pair[0], x, z);
                    assertNotNull(c, pair[0] + " covers " + x + "," + z);
                    assertTrue(c.row() >= 1 && c.row() <= 14, pair[0] + " has no railing inside a road at " + x + "," + z);
                }
            }
        }
    }

    @Test
    public void aLinkOverABridgeOrTunnelTakesTheStaircase() {
        CityLinkGraph.Link link = new CityLinkGraph.Link(0, 0, 30, 20);
        LinkHighways.Ends river = new LinkHighways.Ends() {

            @Override
            public int level(int chunkX, int chunkZ) {
                return chunkX == 0 && chunkZ == 0 ? 1 : 3;
            }

            @Override
            public int radius(int chunkX, int chunkZ) {
                return 0;
            }

            @Override
            public boolean bridgeOrTunnel(int chunkX, int chunkZ, int lowLevel, int highLevel) {
                return chunkX == 15;
            }
        };
        assertFalse(new LinkHighways(null, ends(0, 1, 3, 0, 0)).profile(link).staircase(), "no fallback on open land");
        LinkHighways view = new LinkHighways(null, river);
        LinkHighways.Profile p = view.profile(link);
        assertTrue(p.staircase());
        List<LinkRaster.Step> path = link.path();
        int ramps = 0;
        for (int i = p.start(); i <= p.end(); i++) {
            Piece piece = view.piece(link, i);
            assertFalse(LinkGeometry.coded(piece), "no 45° piece on the staircase: " + piece);
            if (p.inLevel(i) != p.outLevel(i)) {
                ramps++;
                assertEquals(Shape.RAMP, piece.shape());
            }
            if (i < p.end()) {
                LinkRaster.Step s = path.get(i);
                Piece next = view.piece(link, i + 1);
                boolean corner = s.outDx() != 0 && s.outDz() != 0;
                int here = piece.levelAt(s.outDx(), corner ? 0 : s.outDz());
                int there = next.levelAt(corner ? 0 : -s.outDx(), -s.outDz());
                assertTrue(here >= 0, "opens toward the next chunk at " + i);
                assertEquals(here, there, "seam " + i);
                if (corner) {
                    Piece between = Piece.sides(here, -s.outDx(), 0, 0, s.outDz());
                    assertEquals(Shape.BEND, between.shape(), "the side chunk between is a bend");
                }
            }
        }
        assertEquals(2, ramps, "the climb moves to straight chunks");
        assertEquals(3, p.outLevel(p.end()));
    }

    private static Piece one(Piece... pieces) {
        List<Piece> merged = LinkHighways.merge(java.util.Arrays.asList(pieces));
        assertEquals(1, merged.size(), merged.toString());
        return merged.get(0);
    }

    private static long sq(long v) {
        return v * v;
    }
}
