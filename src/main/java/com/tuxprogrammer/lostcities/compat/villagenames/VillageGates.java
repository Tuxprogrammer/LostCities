package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/**
 * Finds a village's gates in its plan: the path ends that face the outside, where a city lane can meet the village's
 * own path (0006 §1 and §3; coexistence.md § Sewing a village into a city, item 4; decision log D87).
 *
 * <p>
 * A path piece is 3 blocks wide and runs along its longer side. Each of its two ends is a gate when:
 * <ol>
 * <li>nothing continues it: no other piece's box touches the row of blocks just beyond the end, across the path's
 * width (this rules out the end a path grew from, which abuts its parent piece); and</li>
 * <li>it faces the outside: the corridor a 3-wide lane would take from just beyond the end, straight on to the edge of
 * the village's bounding box and {@code margin} blocks further, kept {@code margin} blocks clear on both sides, crosses
 * no other piece's box. That is where 0006 §3 lets a lane run: never within {@code landmarkMargin} blocks of a piece.
 * </li>
 * </ol>
 * The gate is the center column of the path's last row, facing the way the path leaves the village. Gates come out in
 * plan order and without duplicates, so the list is a pure function of the plan.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class VillageGates {

    private VillageGates() {}

    /**
     * Returns a plan's gates.
     *
     * @param pieces the plan's pieces
     * @param margin the clearance to keep from every other piece, in blocks ({@code landmarkMargin}, 2 by default)
     * @return the gates, in plan order
     */
    public static List<Gate> find(List<VillagePlan.Piece> pieces, int margin) {
        Set<Gate> gates = new LinkedHashSet<>();
        if (pieces.isEmpty()) {
            return new ArrayList<>(gates);
        }
        BlockRect bounds = bounds(pieces);
        for (int i = 0; i < pieces.size(); i++) {
            VillagePlan.Piece piece = pieces.get(i);
            if (!piece.isPath()) {
                continue;
            }
            BlockRect box = piece.box();
            int width = box.maxX() - box.minX() + 1;
            int depth = box.maxZ() - box.minZ() + 1;
            if (width == depth) {
                continue;
            }
            RoadDirection[] ends = depth > width ? new RoadDirection[] { RoadDirection.NORTH, RoadDirection.SOUTH }
                : new RoadDirection[] { RoadDirection.WEST, RoadDirection.EAST };
            for (RoadDirection facing : ends) {
                Gate gate = gateAt(pieces, i, bounds, facing, margin);
                if (gate != null) {
                    gates.add(gate);
                }
            }
        }
        return new ArrayList<>(gates);
    }

    /** Returns the gate at one end of a path, or {@code null} when that end is continued or faces the village. */
    private static Gate gateAt(List<VillagePlan.Piece> pieces, int index, BlockRect bounds, RoadDirection facing,
        int margin) {
        BlockRect box = pieces.get(index)
            .box();
        BlockRect beyond;
        BlockRect corridor;
        Gate gate;
        int centerX = Math.floorDiv(box.minX() + box.maxX(), 2);
        int centerZ = Math.floorDiv(box.minZ() + box.maxZ(), 2);
        int lane = 1 + margin;
        switch (facing) {
            case NORTH -> {
                beyond = new BlockRect(box.minX(), box.minZ() - 1, box.maxX(), box.minZ() - 1);
                corridor = new BlockRect(
                    centerX - lane,
                    Math.min(box.minZ() - 1, bounds.minZ() - margin),
                    centerX + lane,
                    box.minZ() - 1);
                gate = new Gate(centerX, box.minZ(), facing);
            }
            case SOUTH -> {
                beyond = new BlockRect(box.minX(), box.maxZ() + 1, box.maxX(), box.maxZ() + 1);
                corridor = new BlockRect(
                    centerX - lane,
                    box.maxZ() + 1,
                    centerX + lane,
                    Math.max(box.maxZ() + 1, bounds.maxZ() + margin));
                gate = new Gate(centerX, box.maxZ(), facing);
            }
            case WEST -> {
                beyond = new BlockRect(box.minX() - 1, box.minZ(), box.minX() - 1, box.maxZ());
                corridor = new BlockRect(
                    Math.min(box.minX() - 1, bounds.minX() - margin),
                    centerZ - lane,
                    box.minX() - 1,
                    centerZ + lane);
                gate = new Gate(box.minX(), centerZ, facing);
            }
            default -> {
                beyond = new BlockRect(box.maxX() + 1, box.minZ(), box.maxX() + 1, box.maxZ());
                corridor = new BlockRect(
                    box.maxX() + 1,
                    centerZ - lane,
                    Math.max(box.maxX() + 1, bounds.maxX() + margin),
                    centerZ + lane);
                gate = new Gate(box.maxX(), centerZ, facing);
            }
        }
        for (int j = 0; j < pieces.size(); j++) {
            if (j == index) {
                continue;
            }
            BlockRect other = pieces.get(j)
                .box();
            if (intersects(other, beyond) || intersects(other, corridor)) {
                return null;
            }
        }
        return gate;
    }

    /**
     * Returns the bounding box of every piece.
     *
     * @param pieces the pieces, at least one
     * @return the smallest rectangle holding them all
     */
    static BlockRect bounds(List<VillagePlan.Piece> pieces) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (VillagePlan.Piece piece : pieces) {
            BlockRect box = piece.box();
            minX = Math.min(minX, box.minX());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        return new BlockRect(minX, minZ, maxX, maxZ);
    }

    /**
     * Returns whether two rectangles share a column.
     *
     * @param a one rectangle
     * @param b the other
     * @return whether they overlap
     */
    static boolean intersects(BlockRect a, BlockRect b) {
        return a.minX() <= b.maxX() && b.minX() <= a.maxX() && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ();
    }
}
