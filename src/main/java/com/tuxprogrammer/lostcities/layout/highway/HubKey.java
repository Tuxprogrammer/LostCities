package com.tuxprogrammer.lostcities.layout.highway;

/**
 * A planning cell of the intercity highway planner: {@code highwayPlanningCellSize} chunks on a side, holding at most
 * one hub. Ordered by X, then Z, as upstream's {@code HubKey}.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md ("Planning cells"). Side: both.
 */
public final class HubKey implements Comparable<HubKey> {

    private final int cellX;
    private final int cellZ;

    /**
     * Creates a key.
     *
     * @param cellX the cell's X, {@code floorDiv(chunkX, cellSize)}
     * @param cellZ the cell's Z
     */
    public HubKey(int cellX, int cellZ) {
        this.cellX = cellX;
        this.cellZ = cellZ;
    }

    /**
     * Returns the cell of a chunk.
     *
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param cellSize chunks per cell side
     * @return the cell
     */
    public static HubKey of(int chunkX, int chunkZ, int cellSize) {
        return new HubKey(Math.floorDiv(chunkX, cellSize), Math.floorDiv(chunkZ, cellSize));
    }

    /** Returns the cell's X. */
    public int cellX() {
        return cellX;
    }

    /** Returns the cell's Z. */
    public int cellZ() {
        return cellZ;
    }

    /**
     * Returns the cell at an offset.
     *
     * @param dx cells east
     * @param dz cells south
     * @return the cell
     */
    public HubKey offset(int dx, int dz) {
        return new HubKey(Math.addExact(cellX, dx), Math.addExact(cellZ, dz));
    }

    @Override
    public int compareTo(HubKey other) {
        int c = Integer.compare(cellX, other.cellX);
        return c != 0 ? c : Integer.compare(cellZ, other.cellZ);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HubKey && ((HubKey) other).cellX == cellX && ((HubKey) other).cellZ == cellZ;
    }

    @Override
    public int hashCode() {
        return 31 * cellX + cellZ;
    }

    @Override
    public String toString() {
        return "cell(" + cellX + "," + cellZ + ")";
    }
}
