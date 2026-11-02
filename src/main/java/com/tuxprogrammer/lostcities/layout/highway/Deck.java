package com.tuxprogrammer.lostcities.layout.highway;

/**
 * A highway's deck across one chunk along one axis: its city level at the chunk's west or north side ({@link #low()},
 * the lower coordinate) and at its east or south side ({@link #high()}). Equal for a flat piece; one apart for a ramp
 * (design record 0011). {@link #end()} marks the end chunk of a run, which becomes a plaza.
 *
 * <p>
 * Side: both.
 */
public final class Deck {

    private final Axis axis;
    private final int low;
    private final int high;
    private final boolean end;
    private final ConnectionKey route;

    /**
     * Creates a deck.
     *
     * @param axis  its axis
     * @param low   its level at the west (X) or north (Z) side
     * @param high  its level at the east or south side
     * @param end   whether the chunk ends a run
     * @param route the route it belongs to
     */
    public Deck(Axis axis, int low, int high, boolean end, ConnectionKey route) {
        this.axis = axis;
        this.low = low;
        this.high = high;
        this.end = end;
        this.route = route;
    }

    /** Returns its axis. */
    public Axis axis() {
        return axis;
    }

    /** Returns its level at the west or north side. */
    public int low() {
        return low;
    }

    /** Returns its level at the east or south side. */
    public int high() {
        return high;
    }

    /** Returns whether the chunk ends a run. */
    public boolean end() {
        return end;
    }

    /** Returns its route. */
    public ConnectionKey route() {
        return route;
    }

    /** Returns whether both sides are at one level. */
    public boolean flat() {
        return low == high;
    }

    /** Returns the lower of its two levels. */
    public int min() {
        return Math.min(low, high);
    }

    /** Returns the higher of its two levels. */
    public int max() {
        return Math.max(low, high);
    }

    @Override
    public String toString() {
        return axis + (flat() ? " level " + low : " ramp " + low + "->" + high) + (end ? " end" : "");
    }
}
