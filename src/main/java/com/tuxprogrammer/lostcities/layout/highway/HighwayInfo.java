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
 * one route leaving is a crossing: the plaza a highway ends on (deviation, design record 0008; upstream ends a route
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
        RAMP
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
            this.shape = shape;
            this.axis = axis;
            this.lowLevel = lowLevel;
            this.highLevel = highLevel;
            this.quarterTurns = quarterTurns;
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
        return xDeck != null || zDeck != null;
    }

    /**
     * Returns the city level of the east-west highway, as the city layout reads it: the deck's higher level, so a
     * building or cellar stays clear of the whole ramp; −1 when there is none.
     */
    public int xLevel() {
        return xDeck == null ? -1 : xDeck.max();
    }

    /** Returns the city level of the north-south highway, as {@link #xLevel()}; −1 when there is none. */
    public int zLevel() {
        return zDeck == null ? -1 : zDeck.max();
    }

    /**
     * Returns whether a deck lies at a city level anywhere across the chunk, so a street at that level gives way to it.
     *
     * @param level the city level
     * @return whether a deck touches it
     */
    public boolean deckAt(int level) {
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
