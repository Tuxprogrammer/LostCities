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
    VILLAGE(true),
    /** A VillageNames ocean monument (D32). */
    OCEAN_MONUMENT(false),
    /** A Roguelike Dungeons tower (a predicted candidate). */
    ROGUELIKE(false),
    /** A Witchery structure (the union of its possible footprints). */
    WITCHERY(false),
    /** A LootGames room and its entrance tunnel. */
    LOOTGAMES(false),
    /** An AE2 meteorite, with a crater height cap. */
    METEORITE(false),
    /** An RWG2 terrain landmark: a volcano, a lava-cave opening or a river junction. */
    TERRAIN(false);

    private final boolean sewn;

    LandmarkKind(boolean sewn) {
        this.sewn = sewn;
    }

    /** Returns whether this kind gets transition rings, gates and lanes (villages only). */
    public boolean isSewn() {
        return sewn;
    }
}
