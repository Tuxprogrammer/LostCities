package com.tuxprogrammer.lostcities.layout.highway;

/**
 * An undirected connection between two planning cells, stored smaller cell first; the smaller cell owns the route.
 * Ordered by its first cell, then its second.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md (step A). Side: both.
 */
public final class ConnectionKey implements Comparable<ConnectionKey> {

    private final HubKey first;
    private final HubKey second;

    private ConnectionKey(HubKey first, HubKey second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the key of a pair of distinct cells.
     *
     * @param a one cell
     * @param b the other
     * @return the key, smaller cell first
     * @throws IllegalArgumentException when the cells are equal
     */
    public static ConnectionKey of(HubKey a, HubKey b) {
        int c = a.compareTo(b);
        if (c == 0) {
            throw new IllegalArgumentException("A connection needs two different cells: " + a);
        }
        return c < 0 ? new ConnectionKey(a, b) : new ConnectionKey(b, a);
    }

    /** Returns the smaller cell. */
    public HubKey first() {
        return first;
    }

    /** Returns the larger cell. */
    public HubKey second() {
        return second;
    }

    /** Returns the owner of the route: the smaller cell. */
    public HubKey owner() {
        return first;
    }

    @Override
    public int compareTo(ConnectionKey other) {
        int c = first.compareTo(other.first);
        return c != 0 ? c : second.compareTo(other.second);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConnectionKey && ((ConnectionKey) other).first.equals(first)
            && ((ConnectionKey) other).second.equals(second);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }

    @Override
    public String toString() {
        return first + "-" + second;
    }
}
