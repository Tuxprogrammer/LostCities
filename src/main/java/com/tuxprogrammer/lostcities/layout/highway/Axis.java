package com.tuxprogrammer.lostcities.layout.highway;

/**
 * The axis a highway runs along: {@link #X} east-west (a row of chunks, parts unrotated), {@link #Z} north-south (a
 * column, parts turned 90°).
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/highway-occupancy.md ("Axes"). Side: both.
 */
public enum Axis {
    /** East-west. */
    X,
    /** North-south. */
    Z
}
