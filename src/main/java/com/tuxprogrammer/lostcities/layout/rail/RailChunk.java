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
    /** The sides a link-network line opens on (0015 §3), {@code LinkRails} bits; 0 on the legacy grid. */
    private final int openings;
    /**
     * For a straight line that stops before a landmark core it cannot pass ({@code railwayCoreEnds}, design record
     * 0013 §5): the side its buffer faces, west (north along Z) or east (south); {@code null} for a line through.
     */
    private final Direction buffer;

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
        this.openings = 0;
        this.buffer = null;
    }

    private RailChunk(RailChunk base, Direction buffer) {
        this.type = base.type;
        this.direction = base.direction;
        this.level = base.level;
        this.tracks = base.tracks;
        this.roofed = base.roofed;
        this.alongZ = base.alongZ;
        this.topLevel = base.topLevel;
        this.staircase = base.staircase;
        this.openings = base.openings;
        this.buffer = buffer;
    }

    private RailChunk(RailChunkType type, int level, boolean staircase, int openings) {
        this.type = type;
        this.direction = Direction.BI;
        this.level = level;
        this.tracks = 1;
        this.roofed = null;
        this.alongZ = false;
        this.topLevel = level;
        this.staircase = staircase;
        this.openings = openings;
        this.buffer = null;
    }

    /**
     * Creates a rail chunk of the city link network ({@code networkLayout=links}, 0015 §3): one track at one level,
     * opening on the given sides. A station is the underground station part; every other type is written in code
     * ({@link #coded()}).
     *
     * @param type      its type: a station, or the closest legacy type for the city layout and the API
     * @param level     its level
     * @param staircase for a station, whether it gets its staircase to the surface
     * @param openings  the sides it opens on, {@code LinkRails} bits, not 0
     * @return the rail chunk
     */
    public static RailChunk link(RailChunkType type, int level, boolean staircase, int openings) {
        return new RailChunk(type, level, staircase, openings);
    }

    /**
     * Returns the sides a link-network line opens on: north 1, east 2, south 4, west 8; 0 on the legacy grid.
     *
     * @return the bits
     */
    public int openings() {
        return openings;
    }

    /** Returns whether the chunk is a link-network line written in code, not a part (0015 §3). */
    public boolean coded() {
        return openings != 0 && !type.isStation();
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

    /**
     * Returns the side a straight line's buffer stop faces where the line stops before a core it cannot pass
     * ({@code railwayCoreEnds}): {@link Direction#WEST} for west or north, {@link Direction#EAST} for east or south.
     *
     * @return the side, or {@code null} for a line that runs through
     */
    public Direction buffer() {
        return buffer;
    }

    /**
     * Returns this straight line with a buffer stop on one side.
     *
     * @param side west (north along Z) or east (south)
     * @return the copy
     */
    RailChunk withBuffer(Direction side) {
        return new RailChunk(this, side);
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
        return type + " "
            + direction
            + " level "
            + level
            + (topLevel != level ? ".." + topLevel : "")
            + " tracks "
            + tracks
            + (alongZ ? " along Z" : "")
            + (openings != 0 ? " openings " + openings : "")
            + (buffer != null ? " buffer " + buffer : "");
    }
}
