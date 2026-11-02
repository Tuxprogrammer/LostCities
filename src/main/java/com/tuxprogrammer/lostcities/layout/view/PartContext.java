package com.tuxprogrammer.lostcities.layout.view;

/**
 * What a building's part conditions see when the layout picks the part for one level (upstream's
 * {@code ConditionContext} as {@code BuildingInfo} fills it): the global and local level, the building's floors and
 * cellars, the part already chosen (for the overlay pick) and the part below, the building and the chunk. The asset
 * model evaluates its conditions against it; biome conditions read the biome at the chunk's center themselves.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/building-info.md, step 14. Side: both.
 */
public final class PartContext {

    /** The part name upstream uses when nothing is chosen or there is nothing below. */
    public static final String NONE = "<none>";

    private final int level;
    private final int floor;
    private final int cellars;
    private final int floors;
    private final String part;
    private final String belowPart;
    private final String buildingName;
    private final int chunkX;
    private final int chunkZ;

    /**
     * Creates a context.
     *
     * @param level        the global city level of this floor ({@code cityLevel + floor})
     * @param floor        the local floor: 0 the ground floor, negative a cellar, {@code floors} the roof part
     * @param cellars      the building's cellars
     * @param floors       the building's floors above ground, not counting the roof part
     * @param part         the part chosen for this level, or {@link #NONE}
     * @param belowPart    the part of the level below, or {@link #NONE}
     * @param buildingName the building's name
     * @param chunkX       chunk X
     * @param chunkZ       chunk Z
     */
    public PartContext(int level, int floor, int cellars, int floors, String part, String belowPart,
        String buildingName, int chunkX, int chunkZ) {
        this.level = level;
        this.floor = floor;
        this.cellars = cellars;
        this.floors = floors;
        this.part = part;
        this.belowPart = belowPart;
        this.buildingName = buildingName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** Returns the global city level of this floor. */
    public int level() {
        return level;
    }

    /** Returns the local floor (0 ground, negative cellar, {@link #floors()} the roof part). */
    public int floor() {
        return floor;
    }

    /** Returns the building's cellars. */
    public int cellars() {
        return cellars;
    }

    /** Returns the building's floors above ground. */
    public int floors() {
        return floors;
    }

    /** Returns the part chosen for this level, or {@link #NONE}. */
    public String part() {
        return part;
    }

    /** Returns the part of the level below, or {@link #NONE}. */
    public String belowPart() {
        return belowPart;
    }

    /** Returns the building's name. */
    public String buildingName() {
        return buildingName;
    }

    /** Returns chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns whether this is the roof part ({@code floor == floors}). */
    public boolean isTopOfBuilding() {
        return floor == floors;
    }

    /** Returns whether this is the ground floor. */
    public boolean isGroundFloor() {
        return floor == 0;
    }

    /** Returns whether this is a cellar. */
    public boolean isCellar() {
        return floor < 0;
    }
}
