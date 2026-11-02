/**
 * The join of M05: real cities from M03's layout and M02's assets, as the overlay's {@code CityPlan}. Pure Java over
 * the layout, the asset model and the chunk buffer, so the whole city writer runs in unit and golden tests; the world
 * glue that builds it for a live world lives in {@code worldgen.overlay}.
 *
 * <p>
 * Spec: docs/new-spec/03-terrain/pipeline-on-rwg.md ("The plan the overlay reads"),
 * docs/original-spec/03-terrain/building-and-street-writing.md, chunk-driver-and-parts.md. Side: server (world
 * generation), and plain Java in tests.
 */
package com.tuxprogrammer.lostcities.worldgen.city;
