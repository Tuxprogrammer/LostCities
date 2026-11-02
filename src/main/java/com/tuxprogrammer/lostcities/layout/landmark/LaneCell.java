package com.tuxprogrammer.lostcities.layout.landmark;

import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/**
 * One chunk's share of a landmark's sewing: its {@link LaneRole}, which neighbors the lane, path or detour continues
 * into, and the gate it serves when the path into a core starts here. Plan data for the terrain phase, which lays the
 * surface path (M04).
 *
 * <p>
 * Side: both.
 */
public final class LaneCell {

    /** A chunk with no sewing role. */
    public static final LaneCell NONE = new LaneCell(LaneRole.NONE, 0, null, null);

    private final LaneRole role;
    private final int connections;
    private final Gate gate;
    private final String landmarkId;

    /**
     * Creates a lane cell.
     *
     * @param role        the role
     * @param connections bit {@code 1 << direction.ordinal()} set for each {@link RoadDirection} the lane continues in
     * @param gate        the gate whose path into the core starts here (a gate chunk, or a detour chunk a gate lies
     *                    on), or {@code null}
     * @param landmarkId  the landmark being sewn in, or {@code null} for {@link #NONE}
     */
    public LaneCell(LaneRole role, int connections, Gate gate, String landmarkId) {
        this.role = role;
        this.connections = connections;
        this.gate = gate;
        this.landmarkId = landmarkId;
    }

    /** Returns the role. */
    public LaneRole role() {
        return role;
    }

    /**
     * Returns whether the lane continues into the neighbor in a direction (for a gate chunk, toward the core too).
     *
     * @param direction the direction
     * @return whether the lane connects that way
     */
    public boolean connects(RoadDirection direction) {
        return (connections & (1 << direction.ordinal())) != 0;
    }

    /** Returns the connection bits, {@code 1 << direction.ordinal()} per direction. */
    public int connectionBits() {
        return connections;
    }

    /** Returns the gate whose surface path into the core starts here, or {@code null}. */
    public Gate gate() {
        return gate;
    }

    /** Returns the landmark being sewn in, or {@code null}. */
    public String landmarkId() {
        return landmarkId;
    }

    /**
     * Returns this cell with one more connection.
     *
     * @param direction the direction to add
     * @return the new cell
     */
    public LaneCell with(RoadDirection direction) {
        return new LaneCell(role, connections | (1 << direction.ordinal()), gate, landmarkId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(role.name());
        for (RoadDirection d : RoadDirection.values()) {
            if (connects(d)) {
                sb.append(
                    d.name()
                        .charAt(0));
            }
        }
        return sb.toString();
    }
}
