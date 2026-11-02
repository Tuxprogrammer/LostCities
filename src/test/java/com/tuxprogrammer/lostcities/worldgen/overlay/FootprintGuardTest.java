package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/** The footprint write guard's frames (0005 §3a). The world is left null: only identity matters. */
public class FootprintGuardTest {

    /** Chunk (0, 0) is a street whose footprint starts at Y 68; every other chunk is land. */
    private static final CityPlan PLAN = (cx, cz) -> {
        if (cx == 0 && cz == 0) {
            int[] footprint = new int[256];
            Arrays.fill(footprint, 68);
            return ChunkPlan.builder(0, 0, ChunkKind.CITY)
                .footprint(footprint)
                .build();
        }
        return ChunkPlan.none(cx, cz, 0, 71, 70, false);
    };

    @AfterEach
    public void clear() {
        FootprintGuard.reset();
    }

    @Test
    public void dropsWritesInsideTheFootprintWhileTheDelegatePopulates() {
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "no population, no guard");
        FootprintGuard.push(null, PLAN, true);
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "a tree on the street is dropped");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 60, 3, null), "below the footprint passes");
        assertFalse(FootprintGuard.drops(null, 1, 0, 3, 80, 3, null), "outside the city passes");
        FootprintGuard.pop();
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null));
    }

    @Test
    public void snowAfterPostPassesAndOurOwnPopulationIsNeverGuarded() {
        FootprintGuard.push(null, PLAN, true);
        FootprintGuard.endOfDecoration(null);
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "snow and ice after Post pass");
        FootprintGuard.push(null, PLAN, true);
        FootprintGuard.push(null, PLAN, false);
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "the population phase writes freely");
        FootprintGuard.pop();
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "the nested delegate is guarded again");
    }

    /** M05J: other mods' trees are cut at the footprint; their ores and stones pass. */
    @Test
    public void aTreeFrameDropsOnlyTreeParts() {
        Block leaves = new Block(Material.leaves) {};
        Block wood = new Block(Material.wood) {};
        Block ore = new Block(Material.rock) {};
        FootprintGuard.pushTrees(null, PLAN);
        assertTrue(FootprintGuard.topIsTrees(null));
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, leaves), "a rubber tree's crown over the street");
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, wood), "its trunk");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, ore), "a GT vein passes");
        assertFalse(FootprintGuard.drops(null, 1, 0, 3, 80, 3, leaves), "a park outside the footprint keeps its tree");
        FootprintGuard.endOfDecoration(null);
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, leaves), "RWG's Post event does not end a tree frame");
        FootprintGuard.pop();
        assertFalse(FootprintGuard.topIsTrees(null));
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, leaves));
    }
}
