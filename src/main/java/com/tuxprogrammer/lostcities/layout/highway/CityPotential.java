package com.tuxprogrammer.lostcities.layout.highway;

/**
 * How much city a chunk would have, for the highway planner's hubs and route penalties: the layout's own city factor,
 * so hubs sit on the cities that generate (new-spec 06, the {@code ApproximateCityPotential} row). It must be pure
 * and must not ask for highways.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/city-potential.md. Side: both.
 */
public interface CityPotential {

    /** The integer score of potential 1. */
    int SCORE_SCALE = 1_000_000;

    /**
     * Returns the score of a potential: {@code round(clamp(p, 0, 1) × 1,000,000)}.
     *
     * @param potential the potential
     * @return the score, 0 to {@link #SCORE_SCALE}
     */
    static int score(float potential) {
        float clamped = Math.min(Math.max(potential, 0f), 1f);
        return Math.round(clamped * SCORE_SCALE);
    }

    /**
     * Returns a chunk's potential, 0 to 1, with the height window and the biome multiplier.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the potential
     */
    float potential(int chunkX, int chunkZ);

    /**
     * Returns a chunk's score, or −1 when even the largest biome multiplier could not lift it to {@code required}, so
     * the expensive height and biome reads are skipped. Pruning never changes which sample wins.
     *
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param required the score the caller needs
     * @return the score, or −1
     */
    int scoreWithUpperBound(int chunkX, int chunkZ, int required);
}
