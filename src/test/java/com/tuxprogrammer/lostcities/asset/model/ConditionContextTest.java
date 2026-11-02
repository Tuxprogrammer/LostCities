package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;

/** The facts condition tests read, and the floor numbering (docs/original-spec/05-assets/conditions.md). */
public class ConditionContextTest {

    @Test
    public void floorsAreSixBlocks() {
        assertEquals(6, ConditionContext.FLOOR_HEIGHT);
        assertEquals(0, ConditionContext.floorOf(64, 64));
        assertEquals(0, ConditionContext.floorOf(69, 64));
        assertEquals(1, ConditionContext.floorOf(70, 64));
    }

    @Test
    public void doesNotReproduceASSET05_cellarKReportsFloorMinusK() {
        // Upstream's (y - ground) / 6 truncates toward zero: blocks 1 to 5 below ground counted as floor 0.
        assertEquals(-1, ConditionContext.floorOf(63, 64));
        assertEquals(-1, ConditionContext.floorOf(58, 64));
        assertEquals(-2, ConditionContext.floorOf(57, 64));
        assertEquals(-2, ConditionContext.floorOf(52, 64));
    }

    @Test
    public void doesNotReproduceASSET04_theBelowPartIsItsOwnFact() {
        AssetId current = AssetId.parse("floor_a");
        AssetId below = AssetId.parse("floor_b");
        ConditionContext context = ConditionContext.builder()
            .part(current)
            .belowPart(below)
            .build();
        assertEquals(current, context.part());
        assertEquals(below, context.belowPart());
    }

    @Test
    public void doesNotReproduceASSET33_namesAreFullIds() {
        ConditionContext context = ConditionContext.builder()
            .building(AssetId.parse("building1"))
            .build();
        assertEquals(
            "lostcities:building1",
            context.building()
                .toString());
        assertTrue(context.isBuilding());
        assertNull(
            ConditionContext.builder()
                .build()
                .building());
    }

    @Test
    public void floorPredicates() {
        ConditionContext top = ConditionContext.builder()
            .floor(3)
            .floors(1, 3)
            .build();
        assertTrue(top.isTopOfBuilding());
        assertFalse(top.isGroundFloor());
        assertFalse(top.isCellar());
        ConditionContext cellar = ConditionContext.builder()
            .floor(-1)
            .floors(1, 3)
            .build();
        assertTrue(cellar.isCellar());
        assertFalse(cellar.isTopOfBuilding());
    }
}
