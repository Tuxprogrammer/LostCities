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
        assertEquals(3, Piece.link(1, -1, 0, 0, 1, 0)
            .quarterTurns(), "south side");
        assertEquals(1, Piece.link(0, -1, 0, -1, 1, 0)
            .quarterTurns(), "north side");
        assertEquals(2, Piece.link(1, 0, 0, -1, -1, 0)
            .quarterTurns(), "east side");
        assertThrows(IllegalArgumentException.class, () -> Piece.link(-1, 0, 0, -1, 1, 0), "near corner");
        assertThrows(IllegalArgumentException.class, () -> Piece.link(-1, 0, 0, 1, 1, 1), "transitions stay flat");
        assertThrows(IllegalArgumentException.class, () -> Piece.link(-1, 0, 0, 0, 1, 0), "a quarter bend");
    }

    @Test
    public void straightPiecesOnBothAxesMergeIntoACrossing() {
        List<Piece> merged = LinkHighways.merge(
            java.util.Arrays.asList(
                Piece.link(-1, 0, 2, 1, 0, 2),
                Piece.link(0, -1, 2, 0, 1, 2),
                Piece.link(-1, 0, 2, 1, 0, 2)));
        assertEquals(1, merged.size());
        assertEquals(Shape.CROSSING, merged.get(0)
            .shape());
        assertEquals(4, merged.get(0)
            .openings().length / 3);
        List<Piece> stacked = LinkHighways
            .merge(java.util.Arrays.asList(Piece.link(-1, 0, 3, 1, 0, 3), Piece.link(0, -1, 1, 0, 1, 1)));
        assertEquals(2, stacked.size(), "different levels pass over each other");
        assertEquals(1, stacked.get(0)
            .level(), "lowest first");
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
        assertEquals(Shape.CROSSING, view.piece(link, p.end())
            .shape());
    }

    @Test
    public void citiesThatCoverTheWholeLinkWriteNothing() {
        CityLinkGraph.Link link = new CityLinkGraph.Link(0, 0, 6, 0);
        assertFalse(LinkHighways.compute(link, ends(256, 0, 0, 0, 0))
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
        HighwayInfo diagonal = new HighwayInfo(java.util.Collections.singletonList(Piece.link(-1, -1, 2, 1, 1, 3)), -1,
            -1);
        assertTrue(diagonal.hasHighway());
        assertEquals(3, diagonal.xLevel());
        assertEquals(3, diagonal.zLevel());
        assertTrue(diagonal.deckAt(2));
        HighwayInfo spill = new HighwayInfo(java.util.Collections.<Piece>emptyList(), 1, 1);
        assertTrue(spill.hasHighway());
        assertTrue(spill.spill());
        assertEquals(1, spill.xLevel());
        assertTrue(spill.pieces(0, 0)
            .isEmpty());
        HighwayInfo xOnly = new HighwayInfo(java.util.Collections.singletonList(Piece.link(-1, 0, 2, 1, 0, 2)), -1, -1);
        assertEquals(2, xOnly.xLevel());
        assertEquals(-1, xOnly.zLevel());
    }

    private static long sq(long v) {
        return v * v;
    }
}
