package com.tuxprogrammer.lostcities.layout;

/**
 * What a city chunk without a building is laid out as: a {@link #NORMAL} street, a {@link #FULL} plaza or a
 * {@link #PARK}. Upstream's {@code BuildingInfo.StreetType}.
 *
 * <p>
 * Deviation (0006 §3, new-spec 04-city-layout): {@link #FULL}, which upstream defines but never reaches (CITY-12), is
 * the plaza where a secondary or tertiary road ends facing a landmark, laid with the city style's {@code full} street
 * parts.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/streets-parks-stairs.md. Side: both.
 */
public enum StreetType {

    /** A street: flat, or a slope where the plan has one. */
    NORMAL,
    /** A plaza: the full street section. */
    FULL,
    /** A park or open lot. */
    PARK
}
