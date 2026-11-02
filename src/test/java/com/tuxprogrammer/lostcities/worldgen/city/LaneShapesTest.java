package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/** Where a village lane runs in a chunk (0006 §3). */
public class LaneShapesTest {

    @Test
    public void aLinkRunsThreeWideThroughTheMiddleAlongItsConnections() {
        LaneCell cell = new LaneCell(LaneRole.LINK, 0, null, "v").with(RoadDirection.NORTH)
            .with(RoadDirection.SOUTH);
        boolean[] columns = LaneShapes.forCell(cell, 0, 0);
        int count = 0;
        for (boolean column : columns) {
            count += column ? 1 : 0;
        }
        assertEquals(3 * 16, count);
        for (int z = 0; z < 16; z++) {
            assertTrue(columns[(z << 4) | 7] && columns[(z << 4) | 8] && columns[(z << 4) | 9]);
            assertFalse(columns[(z << 4) | 6]);
        }
    }

    @Test
    public void aLaneInsideACityHasNoPath() {
        LaneCell cell = new LaneCell(LaneRole.LANE, 0, null, "v").with(RoadDirection.EAST);
        assertNull(LaneShapes.forCell(cell, 0, 0), "its street is the lane");
        assertNull(LaneShapes.forCell(LaneCell.NONE, 0, 0));
    }

    @Test
    public void aGateChunksPathLinesUpWithTheGate() {
        // A gate at block x 3 on the north side of a village; the gate chunk lies north of it.
        Gate gate = new Gate(3, 16, RoadDirection.NORTH);
        LaneCell cell = new LaneCell(LaneRole.GATE, 0, gate, "v").with(RoadDirection.SOUTH);
        boolean[] columns = LaneShapes.forCell(cell, 0, 0);
        assertTrue(columns[(15 << 4) | 3], "the path meets the core's edge in line with the gate");
        assertTrue(columns[(15 << 4) | 2] && columns[(15 << 4) | 4]);
    }

    @Test
    public void insideTheCoreThePathRunsFromTheGateToTheCoresEdge() {
        // Footprint chunk rows 1..2; the gate on its north edge at z 16; margin 0 keeps the core to the footprint.
        Gate gate = new Gate(8, 16, RoadDirection.NORTH);
        Landmark village = Landmark.builder("v", LandmarkKind.VILLAGE)
            .footprint(new BlockRect(0, 16, 31, 47))
            .gate(gate)
            .build();
        boolean[] inside = LaneShapes.forCore(0, 1, Collections.singletonList(village), 0, (g, x, z) -> true);
        assertTrue(inside[(0 << 4) | 8], "the gate's own block");
        assertFalse(inside[(1 << 4) | 8], "nothing deeper into the village");
        assertNull(LaneShapes.forCore(0, 1, Collections.singletonList(village), 0, (g, x, z) -> false),
            "a gate the layout did not sew gets no path");
    }
}
