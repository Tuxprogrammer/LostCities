package com.tuxprogrammer.lostcities.layout.rail;

import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;

/**
 * The railway in one chunk: its type, which way it runs, its level, its number of tracks (1 to 3), and for a surface
 * station whether it takes the roofed part (upstream's {@code RailChunkInfo}), plus what this port adds: the axis a
 * slope runs along, the higher level a slope reaches, and whether an underground station gets its staircase.
 *
 * <p>
 * {@link #direction()}: for straight parts and ends upstream's {@code WEST} or {@code EAST} (which way the part is
 * mirrored); for a slope, the side its low end faces: west or east along X, north ({@code WEST}) or south
 * ({@code EAST}) along Z.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/railway-layout.md ("Output"). Side: both.
 */
public final class RailChunk {

    /** Upstream's {@code RailDirection}. */
    public enum Direction {
        /** Both ways: a station or a line through it. */
        BI,
        /** West, or north for a slope along Z. */
        WEST,
        /** East, or south for a slope along Z. */
        EAST
    }

    /** Nothing. */
    public static final RailChunk NONE = new RailChunk(RailChunkType.NONE, Direction.BI, 0, 0);

    private final RailChunkType type;
    private final Direction direction;
    private final int level;
    private final int tracks;
    private final Boolean roofed;
    private final boolean alongZ;
    private final int topLevel;
    private final boolean staircase;

    /**
     * Creates an upstream rail chunk.
     *
     * @param type      its type
     * @param direction its direction
     * @param level     its level: the part's bottom is at {@code groundLevel + 6 × level}
     * @param tracks    its tracks, 1 to 3
     */
    public RailChunk(RailChunkType type, Direction direction, int level, int tracks) {
        this(type, direction, level, tracks, null, false, level, true);
    }

    /**
     * Creates a rail chunk.
     *
     * @param type      its type
     * @param direction its direction
     * @param level     its level
     * @param tracks    its tracks
     * @param roofed    for a surface station, whether it takes the roofed part; {@code null} otherwise
     * @param alongZ    whether a slope runs north-south
     * @param topLevel  the highest level its rails reach (a slope's upper end), at least {@code level}
     * @param staircase whether an underground station gets its staircase to the surface
     */
    public RailChunk(RailChunkType type, Direction direction, int level, int tracks, Boolean roofed, boolean alongZ,
        int topLevel, boolean staircase) {
        this.type = type;
        this.direction = direction;
        this.level = level;
        this.tracks = tracks;
        this.roofed = roofed;
        this.alongZ = alongZ;
        this.topLevel = Math.max(level, topLevel);
        this.staircase = staircase;
    }

    /** Returns its type. */
    public RailChunkType type() {
        return type;
    }

    /** Returns its direction. */
    public Direction direction() {
        return direction;
    }

    /** Returns its level. */
    public int level() {
        return level;
    }

    /** Returns its tracks. */
    public int tracks() {
        return tracks;
    }

    /** Returns, for a surface station, whether it takes the roofed part; {@code null} otherwise. */
    public Boolean roofed() {
        return roofed;
    }

    /** Returns whether a slope runs north-south. */
    public boolean alongZ() {
        return alongZ;
    }

    /** Returns the highest level its rails reach. */
    public int topLevel() {
        return topLevel;
    }

    /** Returns whether an underground station gets its staircase. */
    public boolean staircase() {
        return staircase;
    }

    /** Returns whether there is a rail at all. */
    public boolean isPresent() {
        return type != RailChunkType.NONE;
    }

    /**
     * Returns the same rail at another level, as a station's approach copies its neighbor.
     *
     * @param newType      the type
     * @param newDirection the direction
     * @param newLevel     the level
     * @return the copy
     */
    RailChunk with(RailChunkType newType, Direction newDirection, int newLevel) {
        return new RailChunk(newType, newDirection, newLevel, tracks);
    }

    /**
     * Returns what the city layout reads: the type, and the level the rails reach up to, so a building over a slope
     * keeps clear of its upper end.
     *
     * @return the layout's view
     */
    public RailInfo info() {
        return type == RailChunkType.NONE ? RailInfo.NONE : new RailInfo(type, topLevel);
    }

    @Override
    public String toString() {
        return type + " " + direction + " level " + level + (topLevel != level ? ".." + topLevel : "")
            + " tracks "
            + tracks
            + (alongZ ? " along Z" : "");
    }
}
