package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * How the city treats one kind of landmark: the profile's {@code villageTreatment} and {@code landmarkTreatment}, which
 * replace upstream's {@code avoidVillages} and {@code avoidStructures} (0006 §3, landmark-zones.md § Profile options).
 *
 * <p>
 * Side: both.
 */
public enum LandmarkTreatment {

    /** Core, edge ring and (villages) transition rings, gates and lanes; the default. */
    BUILD_AROUND,
    /** Upstream's behavior: the core and a one-chunk ring around it are not city, and nothing is sewn in. */
    AVOID,
    /** The city ignores the landmark. */
    IGNORE
}
