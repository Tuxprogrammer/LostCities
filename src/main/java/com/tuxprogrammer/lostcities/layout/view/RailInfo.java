package com.tuxprogrammer.lostcities.layout.view;

/**
 * What the city layout reads about the railway at a chunk: its {@link RailChunkType} and its city level.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/chunk-characteristics.md (railway clearance),
 * docs/original-spec/04-city-layout/multibuildings.md (canPlaceBuilding). Side: both.
 */
public final class RailInfo {

    /** No railway at the chunk. */
    public static final RailInfo NONE = new RailInfo(RailChunkType.NONE, -1);

    private final RailChunkType type;
    private final int level;

    /**
     * Creates rail info.
     *
     * @param type  the rail part's type
     * @param level its city level (the rail part's floor is at {@code groundLevel + level * 6})
     */
    public RailInfo(RailChunkType type, int level) {
        this.type = type;
        this.level = level;
    }

    /** Returns the rail part's type. */
    public RailChunkType type() {
        return type;
    }

    /** Returns the rail part's city level. */
    public int level() {
        return level;
    }

    /** Returns whether there is a rail at all. */
    public boolean isPresent() {
        return type != RailChunkType.NONE;
    }

    /** Returns whether the rail reaches the surface or is a station. */
    public boolean isSurfaceOrStation() {
        return type.isSurface() || type.isStation();
    }
}
