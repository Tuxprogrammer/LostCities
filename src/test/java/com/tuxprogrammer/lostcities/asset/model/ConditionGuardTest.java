package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;

/** The shared condition tests (docs/original-spec/05-assets/conditions.md, "Condition tests"). */
public class ConditionGuardTest {

    private static ConditionGuard guard(String fields) {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/conditions/c.json",
            "{\"values\": [{\"factor\": 1, \"value\": \"v\"" + (fields.isEmpty() ? "" : ", " + fields) + "}]}");
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .isEmpty(),
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .toString());
        return set.condition(AssetId.parse("c"))
            .values()
            .get(0)
            .test();
    }

    private static ConditionContext.Builder at(int floor) {
        return ConditionContext.builder()
            .floor(floor)
            .floors(2, 4);
    }

    @Test
    public void noFieldsAlwaysHolds() {
        assertTrue(guard("").isAlways());
        assertTrue(guard("").test(at(0).build()));
    }

    @Test
    public void floorTests() {
        assertTrue(guard("\"top\": true").test(at(4).build()));
        assertFalse(guard("\"top\": true").test(at(3).build()));
        assertTrue(guard("\"top\": false").test(at(-1).build()));
        assertTrue(guard("\"ground\": true").test(at(0).build()));
        assertTrue(guard("\"cellar\": true").test(at(-2).build()));
        assertTrue(guard("\"floor\": 2").test(at(2).build()));
        assertTrue(guard("\"range\": \"-100,-3\"").test(at(-3).build()));
        assertFalse(guard("\"range\": \"-100,-3\"").test(at(-2).build()));
        assertTrue(guard("\"range\": \"4,100\"").test(at(4).build()));
    }

    @Test
    public void aBadRangeIsAnErrorAtLoad() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/conditions/c.json",
            "{\"values\": [{\"factor\": 1, \"value\": \"v\", \"range\": \"4\"}]}");
        assertEquals(0, set.count(com.tuxprogrammer.lostcities.asset.AssetType.CONDITIONS));
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("values[0].range"));
    }

    @Test
    public void doesNotReproduceASSET04_belowpartTestsThePartBelow() {
        ConditionGuard guard = guard("\"belowpart\": \"lower\"");
        ConditionContext context = at(1).part(AssetId.parse("upper"))
            .belowPart(AssetId.parse("lower"))
            .build();
        assertTrue(guard.test(context));
        ConditionContext sameFloor = at(1).part(AssetId.parse("lower"))
            .belowPart(AssetId.parse("other"))
            .build();
        assertFalse(guard.test(sameFloor), "upstream compared the current part");
    }

    @Test
    public void doesNotReproduceASSET33_bareAndFullNamesMatch() {
        ConditionContext context = at(0).part(AssetId.parse("lostcities:rail_dungeon1"))
            .building(AssetId.parse("building1"))
            .build();
        assertTrue(guard("\"inpart\": \"rail_dungeon1\"").test(context));
        assertTrue(guard("\"inpart\": [\"lostcities:rail_dungeon1\"]").test(context));
        assertTrue(guard("\"inbuilding\": \"lostcities:building1\"").test(context));
        assertFalse(guard("\"inpart\": \"mypack:rail_dungeon1\"").test(context));
    }

    @Test
    public void inbiomeTakesThe1710Vocabulary() {
        ConditionGuard guard = guard("\"inbiome\": [\"type:SANDY\", \"rwg:oasis\"]");
        assertTrue(
            guard.test(
                at(0).biome(BiomeDescriptor.of(new HashSet<>(Arrays.asList("SANDY")), "Desert", null))
                    .build()));
        assertTrue(
            guard.test(
                at(0).biome(BiomeDescriptor.of(new HashSet<String>(), "Oasis", "oasis"))
                    .build()));
        assertFalse(guard.test(at(0).build()));
    }

    @Test
    public void buildingSphereAndChunkTests() {
        ConditionContext context = at(0).chunk(3, -4)
            .sphere(true)
            .build();
        assertTrue(guard("\"chunkx\": 3, \"chunkz\": -4, \"issphere\": true, \"isbuilding\": false").test(context));
        assertFalse(guard("\"chunkx\": 2").test(context));
    }
}
