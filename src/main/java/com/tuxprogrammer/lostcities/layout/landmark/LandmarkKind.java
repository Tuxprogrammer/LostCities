package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * Which generator a landmark comes from, which decides the zones around it (0006 §1, decision log D32): every kind has
 * a core and the city's margin around it, villages also get transition rings and are sewn in with gates and lanes, and
 * meteorites add a crater basin.
 *
 * <p>
 * Side: both.
 */
public enum LandmarkKind {

    /** A VillageNames village. */
    VILLAGE(true, false),
    /** A VillageNames ocean monument (D32). */
    OCEAN_MONUMENT(false, false),
    /** A Roguelike Dungeons tower (a predicted candidate). */
    ROGUELIKE(false, false),
    /** A Witchery structure (the union of its possible footprints). */
    WITCHERY(false, false),
    /** A LootGames room and its entrance tunnel. */
    LOOTGAMES(false, true),
    /** An AE2 meteorite, with a crater height cap. */
    METEORITE(false, false),
    /** An RWG2 terrain landmark: a volcano, a lava-cave opening or a river junction. */
    TERRAIN(false, false);

    private final boolean sewn;
    private final boolean underground;

    LandmarkKind(boolean sewn, boolean underground) {
        this.sewn = sewn;
        this.underground = underground;
    }

    /**
     * Returns whether this kind's structure reaches deep underground inside its footprint, whatever the ground: a
     * LootGames room. No link-network line passes under such a core (D359). Roguelike Dungeons towers are not
     * counted: their levels spread far past the lot, so the steering rule keeps the dungeon off link lines instead
     * ({@code SteeringRule}, D361).
     */
    public boolean reachesUnderground() {
        return underground;
    }

    /** Returns whether this kind gets transition rings, gates and lanes (villages only). */
    public boolean isSewn() {
        return sewn;
    }
}
