package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * Which generator a landmark comes from, which decides the zones around it (0006 §1, decision log D32): every kind has
 * a core and the city's margin around it, villages also get transition rings and are sewn in with gates and lanes, and
 * meteorites add a crater basin.
 *
 * <p>
 * Some kinds are <em>overwritten</em> by default (decision log D-landmark-policy-1 and -2): the city builds over them
 * instead of around them. Their default treatment is {@link LandmarkTreatment#IGNORE}, and a bare
 * {@code landmarkTreatment} value does not reach them; only an explicit {@code <kind>=<treatment>} entry does.
 * LootGames rooms and AE2 meteorites are not wired into the structure oracle at all, so no entry gives them a lot.
 *
 * <p>
 * Side: both.
 */
public enum LandmarkKind {

    /** A VillageNames village. */
    VILLAGE(true, false, false),
    /** A VillageNames ocean monument (D32). */
    OCEAN_MONUMENT(false, false, false),
    /** A Roguelike Dungeons tower (a predicted candidate); overwritten, the steering keeps it off city chunks. */
    ROGUELIKE(false, false, true),
    /** A Witchery structure (the union of its possible footprints); overwritten, vetoed on city chunks. */
    WITCHERY(false, false, true),
    /** A LootGames room and its entrance tunnel; overwritten, and no longer a landmark (not wired). */
    LOOTGAMES(false, true, true),
    /** An AE2 meteorite, with a crater height cap; overwritten, and no longer a landmark (not wired). */
    METEORITE(false, false, true),
    /** An RWG2 terrain landmark: a volcano, a lava-cave opening or a river junction. */
    TERRAIN(false, false, false);

    private final boolean sewn;
    private final boolean underground;
    private final boolean overwritten;

    LandmarkKind(boolean sewn, boolean underground, boolean overwritten) {
        this.sewn = sewn;
        this.underground = underground;
        this.overwritten = overwritten;
    }

    /**
     * Returns whether the city builds over this kind by default (D-landmark-policy-1 and -2): Roguelike Dungeons,
     * Witchery, LootGames and AE2 meteorites. Villages, ocean monuments and RWG2 terrain landmarks are kept.
     */
    public boolean overwritten() {
        return overwritten;
    }

    /**
     * Returns the treatment a profile gives this kind when its {@code landmarkTreatment} names it neither by a
     * {@code <kind>=} entry nor, for a kept kind, by a bare value: {@link LandmarkTreatment#IGNORE} for an overwritten
     * kind, {@link LandmarkTreatment#BUILD_AROUND} otherwise.
     */
    public LandmarkTreatment defaultTreatment() {
        return overwritten ? LandmarkTreatment.IGNORE : LandmarkTreatment.BUILD_AROUND;
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
