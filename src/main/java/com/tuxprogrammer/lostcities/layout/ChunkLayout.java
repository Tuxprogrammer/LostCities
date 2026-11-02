package com.tuxprogrammer.lostcities.layout;

import com.tuxprogrammer.lostcities.layout.street.Orientation;

/**
 * A chunk's {@link ChunkPlan} together with every layout answer that depends on its neighbors, computed once: what
 * the terrain and chunk writers (M04, M05) need to lay out one chunk. Upstream reads these from neighboring
 * {@code BuildingInfo}s while it writes; here they come from final plans, so they never change once a neighbor has
 * generated (CITY-13).
 *
 * <p>
 * Doorways stay a query ({@link LayoutPlanner#doorway}), since they are asked per floor and side.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/streets-parks-stairs.md, bridges.md, corridors.md. Side: server.
 */
public final class ChunkLayout {

    private final ChunkPlan plan;
    private final boolean elevatedPark;
    private final Direction slope;
    private final Direction stair;
    private final String xBridgePart;
    private final String zBridgePart;
    private final boolean xCorridor;
    private final boolean zCorridor;
    private final boolean[] frontFrom = new boolean[Direction.values().length];
    private final boolean[] facesCore = new boolean[Direction.values().length];
    private final CornerHeight corner;

    ChunkLayout(LayoutPlanner planner, int chunkX, int chunkZ) {
        plan = planner.plan(chunkX, chunkZ);
        elevatedPark = planner.isElevatedParkSection(chunkX, chunkZ);
        slope = planner.streetSlope(chunkX, chunkZ);
        stair = planner.stairDirection(chunkX, chunkZ);
        String bridgeX = planner.bridgePart(chunkX, chunkZ, Orientation.X);
        xBridgePart = bridgeX;
        // Upstream's generator takes the X bridge first; a chunk never has both (one orientation per intersection).
        zBridgePart = bridgeX != null ? null : planner.bridgePart(chunkX, chunkZ, Orientation.Z);
        xCorridor = planner.hasCorridor(chunkX, chunkZ, Orientation.X);
        zCorridor = planner.hasCorridor(chunkX, chunkZ, Orientation.Z);
        for (Direction side : Direction.values()) {
            frontFrom[side.ordinal()] = plan.isStreetOrParkSection() && planner.hasFrontPartFrom(chunkX, chunkZ, side);
            facesCore[side.ordinal()] = planner.facesCore(chunkX, chunkZ, side);
        }
        corner = planner.cornerHeight(chunkX, chunkZ);
    }

    /** Returns the chunk's plan. */
    public ChunkPlan plan() {
        return plan;
    }

    /** Returns whether the chunk is an elevated park. */
    public boolean isElevatedPark() {
        return elevatedPark;
    }

    /** Returns the side a full-chunk street slope rises toward, or {@code null}. */
    public Direction slope() {
        return slope;
    }

    /** Returns the side the chunk's stair climbs toward after arbitration, or {@code null}. */
    public Direction stair() {
        return stair;
    }

    /**
     * Returns the bridge part the chunk carries along an axis, or {@code null}.
     *
     * @param orientation the axis
     * @return the part name
     */
    public String bridgePart(Orientation orientation) {
        return orientation == Orientation.X ? xBridgePart : zBridgePart;
    }

    /**
     * Returns whether a rail corridor runs through the chunk along an axis.
     *
     * @param orientation the axis
     * @return whether a corridor runs
     */
    public boolean hasCorridor(Orientation orientation) {
        return orientation == Orientation.X ? xCorridor : zCorridor;
    }

    /**
     * Returns whether this street chunk carries the front part of the building on a side.
     *
     * @param side the side the building is on
     * @return whether the front is placed here
     */
    public boolean hasFrontPartFrom(Direction side) {
        return frontFrom[side.ordinal()];
    }

    /**
     * Returns whether a side faces a landmark core (a soft-edge strip, no border wall).
     *
     * @param side the side
     * @return whether the neighbor is a core chunk
     */
    public boolean facesCore(Direction side) {
        return facesCore[side.ordinal()];
    }

    /** Returns the level-0 terrain-correction target at the chunk's north-west corner. */
    public CornerHeight corner() {
        return corner;
    }
}
