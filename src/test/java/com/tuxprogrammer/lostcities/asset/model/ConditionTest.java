package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Condition assets and their weighted, guarded values (docs/original-spec/05-assets/conditions.md). */
public class ConditionTest {

    private static Condition condition(String values) {
        AssetSet set = TestAssets.loadFiles("lostcities/conditions/c.json", "{\"values\": " + values + "}");
        return set.condition(AssetId.parse("c"));
    }

    @Test
    public void picksAmongTheValuesWhoseTestHolds() {
        Condition condition = condition(
            "[{\"factor\": 1, \"value\": \"low\", \"range\": \"-100,-3\"}, {\"factor\": 1, \"value\": \"high\", \"range\": \"4,100\"}]");
        ConditionContext high = ConditionContext.builder()
            .floor(5)
            .build();
        for (int seed = 0; seed < 20; seed++) {
            assertEquals("high", condition.randomValue(new Random(seed), high));
        }
    }

    @Test
    public void theBundledChestLootUsesUpstreamsWeights() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        Condition chestloot = set.condition(AssetId.parse("chestloot"));
        assertEquals(
            37,
            chestloot.values()
                .size(),
            "chestloot's value count");
        ConditionContext dungeon = ConditionContext.builder()
            .part(AssetId.parse("rail_dungeon1"))
            .build();
        int dungeonChests = 0;
        Random random = new Random(7);
        for (int i = 0; i < 1000; i++) {
            if ("lostcities:chests/raildungeonchest".equals(chestloot.randomValue(random, dungeon))) {
                dungeonChests++;
            }
        }
        // 20 of the 82 weight that holds in a rail dungeon at floor 0: the dungeon chest and the unguarded values.
        assertEquals(20.0 / 82.0, dungeonChests / 1000.0, 0.04);
    }

    @Test
    public void doesNotReproduceASSET29_aConditionThatMatchesNothingGivesNothing() {
        Condition condition = condition("[{\"factor\": 1, \"value\": \"only_in_cellars\", \"cellar\": true}]");
        assertNull(
            condition.randomValue(
                new Random(1),
                ConditionContext.builder()
                    .floor(2)
                    .build()));
    }

    @Test
    public void doesNotReproduceASSET10_aZeroTotalPicksNothing() {
        Condition condition = condition("[{\"factor\": 0, \"value\": \"never\"}]");
        assertNull(
            condition.randomValue(
                new Random(1),
                ConditionContext.builder()
                    .build()));
    }
}
