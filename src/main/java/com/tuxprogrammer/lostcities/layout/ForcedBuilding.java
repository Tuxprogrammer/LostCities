package com.tuxprogrammer.lostcities.layout;

import java.util.Arrays;

/**
 * The levels of a building {@code /lostcities createbuilding} forces into a chunk: the building, its floor and cellar
 * counts, and the part (and overlay part) of every level from the lowest cellar to the roof. It is computed from a copy
 * of the chunk's plan by {@link LayoutPlanner#forcedBuilding}, so the cached layout is never touched (avoids CORE-32).
 *
 * <p>
 * Spec: docs/new-spec/01-core/commands.md ({@code createbuilding}), docs/original-spec/01-core/commands.md
 * ({@code createbuilding}, step 5). Immutable. Side: both (plain Java).
 */
public final class ForcedBuilding {

    private final String building;
    private final int floors;
    private final int cellars;
    private final String[] parts;
    private final String[] parts2;

    ForcedBuilding(String building, int floors, int cellars, String[] parts, String[] parts2) {
        this.building = building;
        this.floors = floors;
        this.cellars = cellars;
        this.parts = parts.clone();
        this.parts2 = parts2.clone();
    }

    /** Returns the building's name. */
    public String building() {
        return building;
    }

    /** Returns the floors above ground, the roof part's level included as upstream counts it. */
    public int floors() {
        return floors;
    }

    /** Returns the cellars below ground. */
    public int cellars() {
        return cellars;
    }

    /** Returns how many levels the building has: {@code cellars + floors + 1}. */
    public int levels() {
        return parts.length;
    }

    /**
     * Returns a level's part.
     *
     * @param floor the floor, {@code -cellars} to {@code floors}
     * @return the part's name
     */
    public String part(int floor) {
        return parts[floor + cellars];
    }

    /**
     * Returns a level's overlay part.
     *
     * @param floor the floor, {@code -cellars} to {@code floors}
     * @return the overlay part's name, or {@code null}
     */
    public String part2(int floor) {
        return parts2[floor + cellars];
    }

    @Override
    public String toString() {
        return building + " floors=" + floors + " cellars=" + cellars + " parts=" + Arrays.toString(parts);
    }
}
