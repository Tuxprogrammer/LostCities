package com.tuxprogrammer.lostcities.layout.highway;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Highways.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What highway runs through one chunk: the deck on each axis, every route that hits the chunk, upstream's
 * classification, and the pieces a writer places, in order (upstream's {@code HighwayInfo} with the part choice of
 * {@code Highways.generateHighways} and {@code getJunction}, as pure data).
 *
 * <p>
 * Pieces: where both axes carry a flat deck at one level, or one axis a flat deck that ends a run, the chunk is a
 * junction at that level: a bend, a T or a crossing from the sides the routes at that level leave by. A run's end with
 * one route leaving is a crossing: the plaza a highway ends on (deviation, design record 0013; upstream ends a route
 * with a plain straight piece). Otherwise each axis gets a straight piece or a ramp, the lower one first, because each
 * piece clears the space above it.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/highway-occupancy.md, highway-parts.md ("Choosing parts"). Side: both.
 */
public final class HighwayInfo {

    /** No highway. */
    public static final HighwayInfo NONE = new HighwayInfo(null, null, Collections.<Hit>emptyList());

    /** Upstream's classification of a chunk's highways. */
    public enum Classification {
        /** No highway. */
        NONE,
        /** Only an east-west highway. */
        X_HIGHWAY,
        /** Only a north-south highway. */
        Z_HIGHWAY,
        /** Both, at one level. */
        SAME_LEVEL_INTERSECTION,
        /** Both, at different levels. */
        MULTI_LEVEL_INTERSECTION
    }

    /** The kind of part a piece is written with. */
    public enum Shape {
        /** A straight piece along its axis. */
        STRAIGHT,
        /** A crossing: open on every side; also a run's end, the plaza. */
        CROSSING,
        /** A bend; unrotated it joins west and south. */
        BEND,
        /** A T; unrotated it joins west, east and south. */
        T_JUNCTION,
        /** A straight piece whose deck climbs or falls one level across the chunk. */
        RAMP,
        /**
         * A 45° piece of the city link network (0015 §3), written in code: it enters and leaves the chunk at opposite
         * corners and may climb or fall one level along its length. Its deck spills into the two side neighbors at each
         * corner.
         */
        DIAGONAL,
        /**
         * A link piece that turns 45° (0015 §3), written in code: half straight from the middle of one side to the
         * chunk's center, half diagonal from the center to a corner. Always flat.
         */
        TRANSITION
    }

    /** One route through the chunk. */
    public static final class Hit {

        private final Route route;
        private final int level;
        private final Deck xDeck;
        private final Deck zDeck;

        /**
         * Creates a hit.
         *
         * @param route the route
         * @param level its level after overlapping routes share their lowest
         * @param xDeck its deck along X here, or {@code null}
         * @param zDeck its deck along Z here, or {@code null}
         */
        public Hit(Route route, int level, Deck xDeck, Deck zDeck) {
            this.route = route;
            this.level = level;
            this.xDeck = xDeck;
            this.zDeck = zDeck;
        }

        /** Returns the route. */
        public Route route() {
            return route;
        }

        /** Returns the route's level after overlapping routes share their lowest. */
        public int level() {
            return level;
        }

        /** Returns its deck along X, or {@code null}. */
        public Deck xDeck() {
            return xDeck;
        }

        /** Returns its deck along Z, or {@code null}. */
        public Deck zDeck() {
            return zDeck;
        }

        /** Returns whether the chunk is a bend of the route: on an X and a Z segment. */
        public boolean bend() {
            return xDeck != null && zDeck != null;
        }
    }

    /** One part to write. */
    public static final class Piece {

        private final Shape shape;
        private final Axis axis;
        private final int lowLevel;
        private final int highLevel;
        private final int quarterTurns;
        private final int[] openings;

        /**
         * Creates a piece.
         *
         * @param shape        its part kind
         * @param axis         its axis; {@code null} for a junction
         * @param lowLevel     its level at the west or north side
         * @param highLevel    its level at the east or south side (equal but for a ramp)
         * @param quarterTurns clockwise quarter turns from the part's unrotated form, 0 to 3
         */
        public Piece(Shape shape, Axis axis, int lowLevel, int highLevel, int quarterTurns) {
            this(shape, axis, lowLevel, highLevel, quarterTurns, null);
        }

        private Piece(Shape shape, Axis axis, int lowLevel, int highLevel, int quarterTurns, int[] openings) {
            this.shape = shape;
            this.axis = axis;
            this.lowLevel = lowLevel;
            this.highLevel = highLevel;
            this.quarterTurns = quarterTurns;
            this.openings = openings;
        }

        /**
         * Creates a piece of the city link network from the sides it opens on (0015 §3). An opening is a step toward a
         * neighbor chunk: a side ({@code (±1, 0)}, {@code (0, ±1)}) or a corner ({@code (±1, ±1)}), with the deck's
         * level where it leaves by it.
         *
         * <ul>
         * <li>Two sides opposite each other: {@link Shape#STRAIGHT}, or {@link Shape#RAMP} when the levels differ.</li>
         * <li>Two opposite corners: {@link Shape#DIAGONAL}; quarter turn 0 for the north-west to south-east line, 1
         * for north-east to south-west.</li>
         * <li>A side and a corner away from it: {@link Shape#TRANSITION}; its quarter turn is the side's (west 0,
         * north 1, east 2, south 3).</li>
         * </ul>
         *
         * @param aDx    first opening's X step
         * @param aDz    first opening's Z step
         * @param aLevel the level at the first opening
         * @param bDx    second opening's X step
         * @param bDz    second opening's Z step
         * @param bLevel the level at the second opening
         * @return the piece
         * @throws IllegalArgumentException for any other pair of openings
         */
        public static Piece link(int aDx, int aDz, int aLevel, int bDx, int bDz, int bLevel) {
            int[] openings = { aDx, aDz, aLevel, bDx, bDz, bLevel };
            boolean aCorner = aDx != 0 && aDz != 0;
            boolean bCorner = bDx != 0 && bDz != 0;
            boolean opposite = aDx == -bDx && aDz == -bDz && (aDx != 0 || aDz != 0);
            if (!aCorner && !bCorner && opposite) {
                Axis axis = aDx != 0 ? Axis.X : Axis.Z;
                // Legacy convention: lowLevel is the level at the west or north side.
                boolean aFirst = aDx + aDz < 0;
                int west = aFirst ? aLevel : bLevel;
                int east = aFirst ? bLevel : aLevel;
                Shape shape = west == east ? Shape.STRAIGHT : Shape.RAMP;
                return new Piece(shape, axis, west, east, axis == Axis.X ? 0 : 1, openings);
            }
            if (aCorner && bCorner && opposite) {
                boolean aWest = aDx < 0;
                int turns = aDx == aDz ? 0 : 1;
                return new Piece(Shape.DIAGONAL, null, aWest ? aLevel : bLevel, aWest ? bLevel : aLevel, turns, openings);
            }
            if (aCorner != bCorner && aLevel == bLevel) {
                int sDx = aCorner ? bDx : aDx;
                int sDz = aCorner ? bDz : aDz;
                int cDx = aCorner ? aDx : bDx;
                int cDz = aCorner ? aDz : bDz;
                // The corner lies on the far half: it shares no sign with the side's step.
                if (sDx != 0 && cDx == -sDx || sDz != 0 && cDz == -sDz) {
                    int turns = sDx < 0 ? 0 : sDz < 0 ? 1 : sDx > 0 ? 2 : 3;
                    return new Piece(Shape.TRANSITION, null, aLevel, aLevel, turns, openings);
                }
            }
            throw new IllegalArgumentException(
                "no link piece opens on " + aDx + "," + aDz + " and " + bDx + "," + bDz);
        }

        /**
         * Creates the plaza a link ends on (0013 §2, 0015 §5): a crossing at one level, with the steps the link leaves
         * by. A plaza is open on every side; a corner step means a diagonal leaves from its corner.
         *
         * @param level the plaza's level
         * @param steps the steps the links leave by, as {@code dx, dz} pairs
         * @return the piece
         */
        public static Piece plaza(int level, int... steps) {
            int[] openings = new int[steps.length / 2 * 3];
            for (int i = 0; i < steps.length / 2; i++) {
                openings[i * 3] = steps[i * 2];
                openings[i * 3 + 1] = steps[i * 2 + 1];
                openings[i * 3 + 2] = level;
            }
            return new Piece(Shape.CROSSING, null, level, level, 0, openings);
        }

        /**
         * Returns the openings of a link piece as {@code dx, dz, level} triples, or an empty array for a legacy piece.
         * Callers must not modify it.
         */
        public int[] openings() {
            return openings == null ? new int[0] : openings;
        }

        /**
         * Returns the deck's level where this piece meets the neighbor one step away, or −1 when the piece does not
         * open toward it. A crossing opens at its level on every side and on each of its corner openings; a straight
         * piece or ramp on the two ends of its axis; a link piece on its openings.
         *
         * @param dx the step's X, −1 to 1
         * @param dz the step's Z, −1 to 1
         * @return the level, or −1
         */
        public int levelAt(int dx, int dz) {
            if (openings != null) {
                for (int i = 0; i + 2 < openings.length; i += 3) {
                    if (openings[i] == dx && openings[i + 1] == dz) {
                        return openings[i + 2];
                    }
                }
            }
            boolean side = (dx == 0) != (dz == 0);
            if (!side) {
                return -1;
            }
            if (junction()) {
                return level();
            }
            if ((shape == Shape.STRAIGHT || shape == Shape.RAMP) && openings == null) {
                if (axis == Axis.X && dz == 0) {
                    return dx < 0 ? lowLevel : highLevel;
                }
                if (axis == Axis.Z && dx == 0) {
                    return dz < 0 ? lowLevel : highLevel;
                }
            }
            return -1;
        }

        /** Returns whether a writer places this piece in code rather than from a part (0015 §3). */
        public boolean coded() {
            return shape == Shape.DIAGONAL || shape == Shape.TRANSITION;
        }

        /** Returns whether this piece opens on a corner, so its deck spills into the two chunks beside it. */
        public boolean opensOnCorner() {
            for (int i = 0; openings != null && i + 2 < openings.length; i += 3) {
                if (openings[i] != 0 && openings[i + 1] != 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Piece)) {
                return false;
            }
            Piece p = (Piece) o;
            return shape == p.shape && axis == p.axis
                && lowLevel == p.lowLevel
                && highLevel == p.highLevel
                && quarterTurns == p.quarterTurns
                && java.util.Arrays.equals(sortedOpenings(), p.sortedOpenings());
        }

        @Override
        public int hashCode() {
            return ((shape.hashCode() * 31 + lowLevel) * 31 + highLevel) * 31 + quarterTurns;
        }

        private long[] sortedOpenings() {
            int[] o = openings();
            long[] keys = new long[o.length / 3];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = ((long) (o[i * 3] + 1) * 3 + (o[i * 3 + 1] + 1)) << 32 | (o[i * 3 + 2] & 0xffffffffL);
            }
            java.util.Arrays.sort(keys);
            return keys;
        }

        /** Returns the part kind. */
        public Shape shape() {
            return shape;
        }

        /** Returns its axis, or {@code null} for a junction. */
        public Axis axis() {
            return axis;
        }

        /** Returns its level at the west or north side. */
        public int lowLevel() {
            return lowLevel;
        }

        /** Returns its level at the east or south side. */
        public int highLevel() {
            return highLevel;
        }

        /** Returns the lower of its levels. */
        public int level() {
            return Math.min(lowLevel, highLevel);
        }

        /** Returns the higher of its levels. */
        public int topLevel() {
            return Math.max(lowLevel, highLevel);
        }

        /** Returns its clockwise quarter turns, 0 to 3. */
        public int quarterTurns() {
            return quarterTurns;
        }

        /** Returns whether this piece is a junction: a crossing, bend or T. */
        public boolean junction() {
            return shape == Shape.CROSSING || shape == Shape.BEND || shape == Shape.T_JUNCTION;
        }

        @Override
        public String toString() {
            return shape + (axis == null ? "" : " " + axis)
                + " "
                + (lowLevel == highLevel ? "level " + lowLevel : lowLevel + "->" + highLevel)
                + (quarterTurns == 0 ? "" : " turn " + quarterTurns);
        }
    }

    private enum Side {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    private final Deck xDeck;
    private final Deck zDeck;
    private final List<Hit> hits;
    private final List<Piece> linkPieces;
    private final int spillLow;
    private final int spillHigh;

    /**
     * Creates the info of a chunk.
     *
     * @param xDeck the deck along X, or {@code null}
     * @param zDeck the deck along Z, or {@code null}
     * @param hits  every route through the chunk, in connection order
     */
    public HighwayInfo(Deck xDeck, Deck zDeck, List<Hit> hits) {
        this.xDeck = xDeck;
        this.zDeck = zDeck;
        this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
        this.linkPieces = null;
        this.spillLow = -1;
        this.spillHigh = -1;
    }

    /**
     * Creates the info of a chunk of the city link network ({@code networkLayout=links}, 0015): its own pieces, lowest
     * first, and the levels of any diagonal deck that spills into it from a neighbor (0015 §3).
     *
     * @param pieces    the chunk's own pieces, lowest first
     * @param spillLow  the lowest level of a deck spilling in from a neighbor, or −1 for none
     * @param spillHigh the highest such level, or −1
     */
    public HighwayInfo(List<Piece> pieces, int spillLow, int spillHigh) {
        this.xDeck = null;
        this.zDeck = null;
        this.hits = Collections.emptyList();
        this.linkPieces = Collections.unmodifiableList(new ArrayList<>(pieces));
        this.spillLow = spillLow;
        this.spillHigh = spillHigh;
    }

    /** Returns whether this chunk belongs to the city link network rather than the legacy planner. */
    public boolean linked() {
        return linkPieces != null;
    }

    /** Returns whether a link deck from a neighbor spills into this chunk (0015 §3). */
    public boolean spill() {
        return spillLow >= 0;
    }

    private int linkLevel(Axis skip) {
        int level = spillHigh;
        for (Piece piece : linkPieces) {
            if (piece.axis() != skip || piece.axis() == null) {
                level = Math.max(level, piece.topLevel());
            }
        }
        return level;
    }

    /** Returns the deck along X, or {@code null}. */
    public Deck xDeck() {
        return xDeck;
    }

    /** Returns the deck along Z, or {@code null}. */
    public Deck zDeck() {
        return zDeck;
    }

    /** Returns every route through the chunk. */
    public List<Hit> hits() {
        return hits;
    }

    /** Returns whether a highway passes. */
    public boolean hasHighway() {
        return xDeck != null || zDeck != null || linkPieces != null && (!linkPieces.isEmpty() || spillLow >= 0);
    }

    /**
     * Returns the city level of the east-west highway, as the city layout reads it: the deck's higher level, so a
     * building or cellar stays clear of the whole ramp; −1 when there is none.
     */
    public int xLevel() {
        if (linkPieces != null) {
            // A diagonal, transition, plaza or spill counts on both axes, so buildings keep clear (0015 §3).
            return linkLevel(Axis.Z);
        }
        return xDeck == null ? -1 : xDeck.max();
    }

    /** Returns the city level of the north-south highway, as {@link #xLevel()}; −1 when there is none. */
    public int zLevel() {
        if (linkPieces != null) {
            return linkLevel(Axis.X);
        }
        return zDeck == null ? -1 : zDeck.max();
    }

    /**
     * Returns whether a deck lies at a city level anywhere across the chunk, so a street at that level gives way to it.
     *
     * @param level the city level
     * @return whether a deck touches it
     */
    public boolean deckAt(int level) {
        if (linkPieces != null) {
            if (spillLow >= 0 && spillLow <= level && level <= spillHigh) {
                return true;
            }
            for (Piece piece : linkPieces) {
                if (piece.level() <= level && level <= piece.topLevel()) {
                    return true;
                }
            }
            return false;
        }
        return xDeck != null && xDeck.min() <= level && level <= xDeck.max()
            || zDeck != null && zDeck.min() <= level && level <= zDeck.max();
    }

    /** Returns upstream's classification from the two levels. */
    public Classification classification() {
        int x = xLevel();
        int z = zLevel();
        if (x >= 0 && z >= 0) {
            return x == z ? Classification.SAME_LEVEL_INTERSECTION : Classification.MULTI_LEVEL_INTERSECTION;
        }
        if (x >= 0) {
            return Classification.X_HIGHWAY;
        }
        return z >= 0 ? Classification.Z_HIGHWAY : Classification.NONE;
    }

    /**
     * Returns the pieces to write in the chunk, in order.
     *
     * @param chunkX the chunk's X
     * @param chunkZ the chunk's Z
     * @return the pieces; empty when no highway passes
     */
    public List<Piece> pieces(int chunkX, int chunkZ) {
        if (linkPieces != null) {
            return new ArrayList<>(linkPieces);
        }
        List<Piece> out = new ArrayList<>();
        if (!hasHighway()) {
            return out;
        }
        if (xDeck != null && zDeck != null && xDeck.flat() && zDeck.flat() && xDeck.low() == zDeck.low()) {
            out.add(junction(chunkX, chunkZ, xDeck.low()));
            return out;
        }
        Deck only = xDeck == null ? zDeck : zDeck == null ? xDeck : null;
        if (only != null && only.flat() && only.end()) {
            out.add(junction(chunkX, chunkZ, only.low()));
            return out;
        }
        List<Deck> decks = new ArrayList<>();
        if (xDeck != null) {
            decks.add(xDeck);
        }
        if (zDeck != null) {
            decks.add(zDeck);
        }
        // Lower first: each piece clears the space above it (upstream).
        decks.sort((a, b) -> Integer.compare(a.min(), b.min()));
        for (Deck deck : decks) {
            int turns = deck.axis() == Axis.X ? 0 : 1;
            out.add(new Piece(deck.flat() ? Shape.STRAIGHT : Shape.RAMP, deck.axis(), deck.low(), deck.high(), turns));
        }
        return out;
    }

    /** Upstream's {@code getJunction}: the sides the routes at a level leave by pick a bend, a T or a crossing. */
    private Piece junction(int chunkX, int chunkZ, int level) {
        Set<Side> sides = EnumSet.noneOf(Side.class);
        for (Hit hit : hits) {
            for (Run run : hit.route()
                .runs()) {
                for (Segment segment : run.segments()) {
                    if (!segment.contains(chunkX, chunkZ)) {
                        continue;
                    }
                    Deck deck = segment.axis() == Axis.X ? hit.xDeck() : hit.zDeck();
                    if (deck == null || !deck.flat() || deck.low() != level) {
                        continue;
                    }
                    if (segment.axis() == Axis.X) {
                        if (chunkX > Math.min(segment.startX(), segment.endX())) {
                            sides.add(Side.WEST);
                        }
                        if (chunkX < Math.max(segment.startX(), segment.endX())) {
                            sides.add(Side.EAST);
                        }
                    } else {
                        if (chunkZ > Math.min(segment.startZ(), segment.endZ())) {
                            sides.add(Side.NORTH);
                        }
                        if (chunkZ < Math.max(segment.startZ(), segment.endZ())) {
                            sides.add(Side.SOUTH);
                        }
                    }
                }
            }
        }
        boolean opposite = sides.contains(Side.NORTH) && sides.contains(Side.SOUTH)
            || sides.contains(Side.WEST) && sides.contains(Side.EAST);
        if (sides.size() == 2 && !opposite) {
            int turns;
            if (sides.contains(Side.WEST) && sides.contains(Side.SOUTH)) {
                turns = 0;
            } else if (sides.contains(Side.NORTH) && sides.contains(Side.WEST)) {
                turns = 1;
            } else if (sides.contains(Side.NORTH) && sides.contains(Side.EAST)) {
                turns = 2;
            } else {
                turns = 3;
            }
            return new Piece(Shape.BEND, null, level, level, turns);
        }
        if (sides.size() == 3) {
            int turns;
            if (!sides.contains(Side.NORTH)) {
                turns = 0;
            } else if (!sides.contains(Side.EAST)) {
                turns = 1;
            } else if (!sides.contains(Side.SOUTH)) {
                turns = 2;
            } else {
                turns = 3;
            }
            return new Piece(Shape.T_JUNCTION, null, level, level, turns);
        }
        return new Piece(Shape.CROSSING, null, level, level, 0);
    }

    @Override
    public String toString() {
        return "highway(x " + xDeck + ", z " + zDeck + ", " + hits.size() + " routes)";
    }
}
