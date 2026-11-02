package com.tuxprogrammer.lostcities.worldgen.plan;

/**
 * What the city plan says one chunk is, as far as the overlay's terrain and population phases are concerned
 * (docs/design/0005-rwg-overlay.md §2, docs/design/0006-build-around-other-worldgen.md §1).
 *
 * <p>
 * Side: both (plain Java).
 */
public enum ChunkKind {

    /**
     * Not part of a city. The overlay may still blend it toward a nearby city, or damage it with a city's explosion.
     */
    NONE,

    /** A city chunk: a building, a street, a park or a plaza, leveled to its city ground. */
    CITY,

    /**
     * A landmark core: chunks another mod's structure is predicted to occupy. The city writes nothing here except a
     * village lane's surface path; the blend never reshapes it and no veto or write guard acts in it (0006 §1).
     */
    LANDMARK_CORE
}
