package com.tuxprogrammer.lostcities.layout.highway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Choosing a chunk's highway pieces (highway-parts.md, "Choosing parts"; design record 0007). */
public class HighwayInfoTest {

    static final HubKey CELL_A = new HubKey(0, 0);
    static final HubKey CELL_B = new HubKey(1, 0);

    static Route route(int level, Run... runs) {
        Hub a = new Hub(CELL_A, 0, 0, 1, level);
        Hub b = new Hub(CELL_B, 10, 10, 1, level);
        return new Route(ConnectionKey.of(CELL_A, CELL_B), a, b, level, Route.Shape.STRAIGHT, 20, 0, Arrays.asList(runs), 0);
    }

    static Run run(int level, Segment... segments) {
        Segment first = segments[0];
        Segment last = segments[segments.length - 1];
        return new Run(
            Arrays.asList(segments),
            new Run.End(Run.EndKind.HUB, first.startX(), first.startZ(), level),
            new Run.End(Run.EndKind.HUB, last.endX(), last.endZ(), level));
    }

    @Test
    public void aBendJoinsTheTwoSidesTheRouteLeavesBy() {
        // West along row 5 to x 10, then south down column 10: the corner leaves west and south.
        Run run = run(1, new Segment(0, 5, 10, 5, Axis.X), new Segment(10, 5, 10, 15, Axis.Z));
        Route route = route(1, run);
        Deck x = new Deck(Axis.X, 1, 1, false, route.key());
        Deck z = new Deck(Axis.Z, 1, 1, false, route.key());
        HighwayInfo info = new HighwayInfo(x, z, Collections.singletonList(new HighwayInfo.Hit(route, 1, x, z)));
        List<HighwayInfo.Piece> pieces = info.pieces(10, 5);
        assertEquals(1, pieces.size());
        assertEquals(
            HighwayInfo.Shape.BEND,
            pieces.get(0)
                .shape());
        assertEquals(
            0,
            pieces.get(0)
                .quarterTurns(),
            "the unrotated bend joins west and south");
    }

    @Test
    public void aRunsEndIsAPlazaNotACutStraightPiece() {
        Run run = run(0, new Segment(0, 5, 10, 5, Axis.X));
        Route route = route(0, run);
        Deck end = new Deck(Axis.X, 0, 0, true, route.key());
        HighwayInfo info = new HighwayInfo(end, null, Collections.singletonList(new HighwayInfo.Hit(route, 0, end, null)));
        HighwayInfo.Piece piece = info.pieces(10, 5)
            .get(0);
        assertEquals(HighwayInfo.Shape.CROSSING, piece.shape());
        assertTrue(piece.junction());
    }

    @Test
    public void twoLevelsGiveTwoStraightPiecesTheLowerFirst() {
        Run x = run(3, new Segment(0, 5, 10, 5, Axis.X));
        Run z = run(1, new Segment(5, 0, 5, 10, Axis.Z));
        Route high = route(3, x);
        Route low = route(1, z);
        Deck xDeck = new Deck(Axis.X, 3, 3, false, high.key());
        Deck zDeck = new Deck(Axis.Z, 1, 1, false, low.key());
        HighwayInfo info = new HighwayInfo(
            xDeck,
            zDeck,
            Arrays.asList(new HighwayInfo.Hit(high, 3, xDeck, null), new HighwayInfo.Hit(low, 1, null, zDeck)));
        List<HighwayInfo.Piece> pieces = info.pieces(5, 5);
        assertEquals(2, pieces.size());
        assertEquals(
            1,
            pieces.get(0)
                .level());
        assertEquals(
            1,
            pieces.get(0)
                .quarterTurns(),
            "the north-south piece turned 90°");
        assertEquals(
            3,
            pieces.get(1)
                .level());
        assertEquals(HighwayInfo.Classification.MULTI_LEVEL_INTERSECTION, info.classification());
    }

    @Test
    public void aRampReportsItsHigherLevelToTheCityAndTouchesBoth() {
        Run run = run(0, new Segment(0, 5, 10, 5, Axis.X));
        Route route = route(0, run);
        Deck ramp = new Deck(Axis.X, 2, 1, false, route.key());
        HighwayInfo info = new HighwayInfo(ramp, null, Collections.singletonList(new HighwayInfo.Hit(route, 2, ramp, null)));
        assertEquals(2, info.xLevel(), "a building keeps clear of the whole ramp");
        assertEquals(-1, info.zLevel());
        assertTrue(info.deckAt(1));
        assertTrue(info.deckAt(2));
        assertFalse(info.deckAt(0));
        HighwayInfo.Piece piece = info.pieces(5, 5)
            .get(0);
        assertEquals(HighwayInfo.Shape.RAMP, piece.shape());
        assertEquals(1, piece.level());
        assertEquals(2, piece.topLevel());
    }
}
