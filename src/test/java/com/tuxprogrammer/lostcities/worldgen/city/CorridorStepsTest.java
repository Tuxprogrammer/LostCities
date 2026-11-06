package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;

/**
 * Corridor openings into cellars taller than a city level (design record 0014, D-p1b-8): pitch 6 keeps upstream's
 * flat opening, and any taller pitch gets a staircase whose every step is at most one block, so a player can walk
 * from the corridor to the cellar floor and back.
 */
public class CorridorStepsTest {

    private static final int GROUND = 65;

    /** The building's lowest block for a building at a city level with some cellars. */
    private static int lowest(int level, int cellars, int pitch) {
        return GROUND + level * ChunkPlan.LEVEL_HEIGHT - cellars * pitch;
    }

    @Test
    public void pitchSixNeverDrops() {
        for (int level = 0; level <= 3; level++) {
            for (int cellars = 1; cellars <= 4; cellars++) {
                assertEquals(
                    0,
                    CorridorSteps.drop(GROUND, lowest(level, cellars, 6), 6, cellars),
                    "level " + level + ", cellars " + cellars);
            }
        }
    }

    @Test
    public void aPitchEightFirstCellarIsTwoBelowTheCorridor() {
        assertEquals(GROUND - 8, CorridorSteps.cellarFloor(GROUND, lowest(0, 1, 8), 8, 1));
        assertEquals(2, CorridorSteps.drop(GROUND, lowest(0, 1, 8), 8, 1));
        assertEquals(2, CorridorSteps.drop(GROUND, lowest(0, 3, 8), 8, 3));
    }

    @Test
    public void noCellarAtTheCorridorMeansNoSteps() {
        // A level-2 building with one cellar ends above the corridor.
        assertEquals(0, CorridorSteps.drop(GROUND, lowest(2, 1, 8), 8, 1));
        assertEquals(0, CorridorSteps.drop(GROUND, lowest(0, 0, 8), 8, 0));
    }

    @Test
    public void everyStepIsAtMostOneBlockFromCorridorToCellarFloor() {
        for (int pitch = 6; pitch <= 10; pitch++) {
            for (int level = 0; level <= 2; level++) {
                for (int cellars = 1; cellars <= 4; cellars++) {
                    int lowest = lowest(level, cellars, pitch);
                    int drop = CorridorSteps.drop(GROUND, lowest, pitch, cellars);
                    if (drop == 0) {
                        continue;
                    }
                    int floor = CorridorSteps.cellarFloor(GROUND, lowest, pitch, cellars);
                    // Standing heights: the corridor floor, then each step column, then the cellar floor.
                    int standing = GROUND + CorridorSteps.CORRIDOR_FLOOR;
                    for (int inward = 0; inward < drop; inward++) {
                        int top = CorridorSteps.stepTop(GROUND, inward);
                        assertEquals(1, standing - top, "pitch " + pitch + " level " + level + " column " + inward);
                        assertTrue(top >= floor);
                        // Head room: two clear blocks above the step, inside the opening.
                        assertTrue(top + 2 <= GROUND + CorridorSteps.OPENING_TOP);
                        standing = top;
                    }
                    assertEquals(floor, standing, "the last column is the cellar floor");
                }
            }
        }
    }
}
