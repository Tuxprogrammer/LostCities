package com.tuxprogrammer.lostcities.layout.predefined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A predefined city: a city center pinned to a chunk, with its radius, its city style and the buildings and streets it
 * pins around it. The asset loader (M02) parses {@code predefinedcities/*.json} into it; upstream ships none, so this
 * is a pack and config feature.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/predefined-content.md. Side: both.
 */
public final class PredefinedCity {

    private final String source;
    private final String dimension;
    private final int chunkX;
    private final int chunkZ;
    private final int radius;
    private final String cityStyle;
    private final List<PredefinedBuilding> buildings;
    private final List<int[]> streets;

    /**
     * Creates a predefined city.
     *
     * @param source    the asset's name or file, for log lines
     * @param dimension the dimension ID, {@code minecraft:overworld} or {@code lostcitiesnh:dim<id>}
     * @param chunkX    the center chunk X
     * @param chunkZ    the center chunk Z
     * @param radius    the city radius, blocks
     * @param cityStyle the city style's name
     * @param buildings the buildings it pins
     * @param streets   the streets it pins, each {@code {relChunkX, relChunkZ}} relative to the center
     */
    public PredefinedCity(String source, String dimension, int chunkX, int chunkZ, int radius, String cityStyle,
        List<PredefinedBuilding> buildings, List<int[]> streets) {
        this.source = source;
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.radius = radius;
        this.cityStyle = cityStyle;
        this.buildings = Collections.unmodifiableList(new ArrayList<>(buildings));
        List<int[]> copies = new ArrayList<>();
        for (int[] street : streets) {
            copies.add(street.clone());
        }
        this.streets = Collections.unmodifiableList(copies);
    }

    /** Returns the asset's name, for log lines. */
    public String source() {
        return source;
    }

    /** Returns the dimension ID. */
    public String dimension() {
        return dimension;
    }

    /** Returns the center chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns the center chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns the city radius, blocks. */
    public int radius() {
        return radius;
    }

    /** Returns the city style's name. */
    public String cityStyle() {
        return cityStyle;
    }

    /** Returns the buildings the city pins. */
    public List<PredefinedBuilding> buildings() {
        return buildings;
    }

    /** Returns the streets the city pins, each {@code {relChunkX, relChunkZ}}. */
    public List<int[]> streets() {
        return streets;
    }
}
