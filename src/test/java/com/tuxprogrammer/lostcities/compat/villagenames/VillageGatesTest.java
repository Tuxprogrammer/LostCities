package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/** Gates: path ends nothing continues, facing the outside with a clear corridor. */
class VillageGatesTest {

    private static VillagePlan.Piece building(int x1, int z1, int x2, int z2) {
        return new VillagePlan.Piece(new BlockRect(x1, z1, x2, z2), false);
    }

    private static VillagePlan.Piece path(int x1, int z1, int x2, int z2) {
        return new VillagePlan.Piece(new BlockRect(x1, z1, x2, z2), true);
    }

    /** A 10 × 10 town center with a 21-block path out of each side. */
    private static List<VillagePlan.Piece> cross() {
        return new ArrayList<>(
            Arrays.asList(
                building(0, 0, 9, 9),
                path(3, -21, 5, -1),
                path(3, 10, 5, 30),
                path(-21, 3, -1, 5),
                path(10, 3, 30, 5)));
    }

    @Test
    void eachOuterEndOfACrossIsAGate() {
        List<Gate> gates = VillageGates.find(cross(), 2);
        assertEquals(
            Arrays.asList(
                new Gate(4, -21, RoadDirection.NORTH),
                new Gate(4, 30, RoadDirection.SOUTH),
                new Gate(-21, 4, RoadDirection.WEST),
                new Gate(30, 4, RoadDirection.EAST)),
            gates);
    }

    @Test
    void anEndAbuttingAPieceIsNoGate() {
        List<VillagePlan.Piece> pieces = cross();
        // A branch path running west from the north path's side: its east end abuts the north path.
        pieces.add(path(-20, -12, 2, -10));
        List<Gate> gates = VillageGates.find(pieces, 2);
        assertTrue(gates.contains(new Gate(-20, -11, RoadDirection.WEST)), gates.toString());
        for (Gate gate : gates) {
            assertTrue(!(gate.x() == 2 && gate.facing() == RoadDirection.EAST), gates.toString());
        }
    }

    @Test
    void aBuildingInTheCorridorBlocksTheGate() {
        List<VillagePlan.Piece> pieces = cross();
        // A house 8 blocks past the east path's end, across its line.
        pieces.add(building(38, 0, 45, 8));
        List<Gate> gates = VillageGates.find(pieces, 2);
        for (Gate gate : gates) {
            assertTrue(gate.facing() != RoadDirection.EAST, gates.toString());
        }
        assertEquals(3, gates.size());
    }

    @Test
    void aBuildingWithinTheClearanceBlocksTheGate() {
        List<VillagePlan.Piece> pieces = cross();
        // A house beside the south path's line, 2 blocks from the lane's edge (x 3..5): clearance 2 reaches x 7.
        pieces.add(building(7, 31, 12, 36));
        List<Gate> blocked = VillageGates.find(pieces, 2);
        for (Gate gate : blocked) {
            assertTrue(gate.facing() != RoadDirection.SOUTH, blocked.toString());
        }
        List<Gate> narrow = VillageGates.find(pieces, 1);
        assertTrue(narrow.contains(new Gate(4, 30, RoadDirection.SOUTH)), narrow.toString());
    }

    @Test
    void squarePathsAndBuildingsAreNoGates() {
        List<Gate> gates = VillageGates.find(Arrays.asList(building(0, 0, 9, 9), path(10, 0, 12, 2)), 2);
        assertTrue(gates.isEmpty(), gates.toString());
    }

    @Test
    void negativeCoordinatesCenterOnTheMiddleColumn() {
        List<Gate> gates = VillageGates.find(Arrays.asList(building(-30, -30, -21, -21), path(-27, -51, -25, -31)), 2);
        assertEquals(Arrays.asList(new Gate(-26, -51, RoadDirection.NORTH)), gates);
    }

    @Test
    void emptyPlanHasNoGates() {
        assertTrue(
            VillageGates.find(new ArrayList<>(), 2)
                .isEmpty());
    }
}
