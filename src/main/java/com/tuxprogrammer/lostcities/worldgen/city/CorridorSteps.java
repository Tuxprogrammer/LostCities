package com.tuxprogrammer.lostcities.worldgen.city;

/**
 * Where a rail corridor meets a cellar that is taller than a city level (design record 0014, D296). Corridors run
 * at a fixed height: their floor block is {@code groundLevel - 6} and the opening upstream cuts into the cellar wall
 * spans {@code groundLevel - 5 .. groundLevel - 3}. At floor pitch 6 a cellar's floor block always sits on that same
 * row, because the city ground is a whole number of 6-block levels above {@code groundLevel}. At a taller pitch the
 * cellar floor sits lower, and a straight opening would leave a drop a player cannot climb back up.
 *
 * <p>
 * The fix is a staircase of 1-block steps cut down from the corridor into the cellar: column {@code i} (0 is the wall
 * column, then inward) keeps solid blocks up to {@link #stepTop(int, int) stepTop} and is cleared above it up to the
 * top of the opening. A drop of {@code d} needs {@code d} columns; the last is the landing on the cellar floor. At
 * pitch 6 the drop is 0 and the opening is upstream's.
 */
final class CorridorSteps {

    /** The corridor's floor block, relative to {@code groundLevel}. */
    static final int CORRIDOR_FLOOR = -6;
    /** The highest block of the opening, relative to {@code groundLevel}. */
    static final int OPENING_TOP = -3;

    private CorridorSteps() {}

    /**
     * Returns the floor block of the cellar the corridor arrives in: the floor at or below the corridor's floor block,
     * among the cellar floors {@code lowest + k * pitch} for {@code k} in {@code 0 .. cellars - 1}.
     *
     * @param groundLevel the world's ground level, where corridors are measured from
     * @param lowest      the building's lowest block, its bottom cellar's floor
     * @param pitch       the world's floor pitch
     * @param cellars     the building's cellar count
     * @return the cellar's floor block Y, or {@link Integer#MIN_VALUE} when the corridor arrives below every cellar
     */
    static int cellarFloor(int groundLevel, int lowest, int pitch, int cellars) {
        int corridorFloor = groundLevel + CORRIDOR_FLOOR;
        if (cellars <= 0 || pitch <= 0 || corridorFloor < lowest) {
            return Integer.MIN_VALUE;
        }
        int k = Math.min(cellars - 1, (corridorFloor - lowest) / pitch);
        return lowest + k * pitch;
    }

    /**
     * Returns how far the cellar floor sits below the corridor floor: 0 at pitch 6 and when there is no cellar to
     * arrive in, 2 for the first cellar of a level-0 building at pitch 8.
     *
     * @param groundLevel the world's ground level
     * @param lowest      the building's lowest block
     * @param pitch       the world's floor pitch
     * @param cellars     the building's cellar count
     * @return the drop in blocks, never negative
     */
    static int drop(int groundLevel, int lowest, int pitch, int cellars) {
        int floor = cellarFloor(groundLevel, lowest, pitch, cellars);
        if (floor == Integer.MIN_VALUE) {
            return 0;
        }
        return Math.max(0, groundLevel + CORRIDOR_FLOOR - floor);
    }

    /**
     * Returns the top solid block of step column {@code inward}: one block lower than the corridor floor per column,
     * so column {@code drop - 1} is the cellar floor itself.
     *
     * @param groundLevel the world's ground level
     * @param inward      0 for the wall column, counting into the building
     * @return the step's top block Y
     */
    static int stepTop(int groundLevel, int inward) {
        return groundLevel + CORRIDOR_FLOOR - 1 - inward;
    }
}
