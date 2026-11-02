package com.tuxprogrammer.lostcities.layout.highway;

/**
 * One hub a hub could connect to: the pair, the target's sector, the distances and the two tie-break hashes, which are
 * the same from either end of the pair.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md (step C). Side: both.
 */
public final class Candidate {

    private final Hub source;
    private final Hub target;
    private final ConnectionKey key;
    private final Sector sector;
    private final long distanceSquared;
    private final int routeLength;
    private final long rank;
    private final long acceptance;

    /**
     * Creates a candidate.
     *
     * @param source          the picking hub
     * @param target          the candidate hub
     * @param key             the pair
     * @param sector          the target's sector from the source
     * @param distanceSquared the squared Euclidean chunk distance
     * @param routeLength     the Manhattan chunk distance
     * @param rank            the first tie-break hash
     * @param acceptance      the second tie-break hash
     */
    Candidate(Hub source, Hub target, ConnectionKey key, Sector sector, long distanceSquared, int routeLength,
        long rank, long acceptance) {
        this.source = source;
        this.target = target;
        this.key = key;
        this.sector = sector;
        this.distanceSquared = distanceSquared;
        this.routeLength = routeLength;
        this.rank = rank;
        this.acceptance = acceptance;
    }

    /** Returns the picking hub. */
    public Hub source() {
        return source;
    }

    /** Returns the candidate hub. */
    public Hub target() {
        return target;
    }

    /** Returns the pair. */
    public ConnectionKey key() {
        return key;
    }

    /** Returns the target's sector. */
    public Sector sector() {
        return sector;
    }

    /** Returns the squared Euclidean chunk distance. */
    public long distanceSquared() {
        return distanceSquared;
    }

    /** Returns the Manhattan chunk distance. */
    public int routeLength() {
        return routeLength;
    }

    /** Returns the first tie-break hash. */
    public long rank() {
        return rank;
    }

    /** Returns the second tie-break hash. */
    public long acceptance() {
        return acceptance;
    }
}
