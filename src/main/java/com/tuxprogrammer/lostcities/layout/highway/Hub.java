package com.tuxprogrammer.lostcities.layout.highway;

/**
 * The hub of a planning cell: the chunk of the cell's lattice samples with the highest city potential, its score
 * (0 to 1,000,000) and its city level, from which a route's level is taken.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md (step H). Side: both.
 */
public final class Hub {

    private final HubKey key;
    private final int chunkX;
    private final int chunkZ;
    private final int score;
    private final int cityLevel;

    /**
     * Creates a hub.
     *
     * @param key       its planning cell
     * @param chunkX    its chunk X
     * @param chunkZ    its chunk Z
     * @param score     its potential score, 0 to {@link CityPotential#SCORE_SCALE}
     * @param cityLevel its city level
     */
    public Hub(HubKey key, int chunkX, int chunkZ, int score, int cityLevel) {
        this.key = key;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.score = score;
        this.cityLevel = cityLevel;
    }

    /** Returns its planning cell. */
    public HubKey key() {
        return key;
    }

    /** Returns its chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns its chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns its potential score. */
    public int score() {
        return score;
    }

    /** Returns its city level. */
    public int cityLevel() {
        return cityLevel;
    }

    @Override
    public String toString() {
        return "hub(" + chunkX + "," + chunkZ + " score " + score + " level " + cityLevel + ")";
    }
}
