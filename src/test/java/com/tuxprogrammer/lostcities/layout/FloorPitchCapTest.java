package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Design record 0014: at pitch 8 fewer floors fit, and every tower's roof base stays under Y 248. */
public class FloorPitchCapTest {

    @Test
    public void towersStayUnderTheBuildingTopCap() {
        for (int pitch = 6; pitch <= 10; pitch++) {
            for (int ground = 2; ground < 248; ground++) {
                int floors = LayoutPlanner.capFloors(64, ground, pitch);
                assertTrue(floors == 0 || ground + floors * pitch < 248, pitch + "/" + ground);
                // The roof part (6 tall) on top stays in the world.
                assertTrue(floors == 0 || ground + floors * pitch + ChunkPlan.LEVEL_HEIGHT - 1 <= 255);
            }
        }
    }

    @Test
    public void pitchSixIsUnchangedAndPitchEightFitsFewer() {
        // Ground 70: upstream fits floors with 70 + f * 6 < 248, so 29; at 8 it is 22.
        assertEquals(29, LayoutPlanner.capFloors(64, 70, 6));
        assertEquals(22, LayoutPlanner.capFloors(64, 70, 8));
        assertEquals(5, LayoutPlanner.capFloors(5, 70, 8));
    }
}
