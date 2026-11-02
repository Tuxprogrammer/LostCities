package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * Which generator a landmark comes from, which decides the zones around it (0006 §1, decision log D32): villages get
 * transition rings and are sewn in with gates and lanes, terrain landmarks have a core and no edge ring, and meteorites
 * add a crater basin.
 *
 * <p>
 * Side: both.
 */
public enum LandmarkKind {

    /** A VillageNames village. */
    VILLAGE(true, true),
    /** A VillageNames ocean monument (D32). */
    OCEAN_MONUMENT(true, false),
    /** A Roguelike Dungeons tower (a predicted candidate). */
    ROGUELIKE(true, false),
    /** A Witchery structure (the union of its possible footprints). */
    WITCHERY(true, false),
    /** A LootGames room and its entrance tunnel. */
    LOOTGAMES(true, false),
    /** An AE2 meteorite, with a crater height cap. */
    METEORITE(true, false),
    /** An RWG2 terrain landmark: a volcano, a lava-cave opening or a river junction. */
    TERRAIN(false, false);

    private final boolean edgeRing;
    private final boolean sewn;

    LandmarkKind(boolean edgeRing, boolean sewn) {
        this.edgeRing = edgeRing;
        this.sewn = sewn;
    }

    /** Returns whether the city puts an edge ring of parks, plazas and passing roads around this kind's core. */
    public boolean hasEdgeRing() {
        return edgeRing;
    }

    /** Returns whether this kind gets transition rings, gates and lanes (villages only). */
    public boolean isSewn() {
        return sewn;
    }
}
