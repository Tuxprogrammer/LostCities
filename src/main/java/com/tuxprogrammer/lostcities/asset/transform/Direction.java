package com.tuxprogrammer.lostcities.asset.transform;

/**
 * The six block directions with their unit vectors (X grows east, Z grows south), for turning block facings with a
 * part's {@link Transform}. Side: both.
 */
public enum Direction {

    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final int dx;
    private final int dy;
    private final int dz;

    Direction(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    /** Returns the X step. */
    public int dx() {
        return dx;
    }

    /** Returns the Y step. */
    public int dy() {
        return dy;
    }

    /** Returns the Z step. */
    public int dz() {
        return dz;
    }

    /** Returns whether the direction is horizontal. */
    public boolean isHorizontal() {
        return dy == 0;
    }

    /** Returns the opposite direction. */
    public Direction opposite() {
        return of(-dx, -dy, -dz);
    }

    /**
     * Returns the horizontal direction with a vector.
     *
     * @param dx the X step, −1, 0 or 1
     * @param dz the Z step, −1, 0 or 1
     * @return the direction
     * @throws IllegalArgumentException when the vector is not a horizontal unit vector
     */
    public static Direction of(int dx, int dz) {
        return of(dx, 0, dz);
    }

    private static Direction of(int dx, int dy, int dz) {
        for (Direction direction : values()) {
            if (direction.dx == dx && direction.dy == dy && direction.dz == dz) {
                return direction;
            }
        }
        throw new IllegalArgumentException("not a unit direction: " + dx + "," + dy + "," + dz);
    }
}
