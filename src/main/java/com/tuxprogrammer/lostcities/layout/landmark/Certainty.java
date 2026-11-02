package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * How sure an oracle is of a landmark's footprint (0006 §1). The layout treats all three alike: each reserves its lot,
 * and a lot whose structure does not appear stays natural ground (0006 §5).
 *
 * <p>
 * Side: both.
 */
public enum Certainty {

    /** The footprint is the structure. */
    EXACT,
    /** The structure lies somewhere inside the footprint. */
    UNION,
    /** The structure is predicted and may not appear. */
    CANDIDATE
}
