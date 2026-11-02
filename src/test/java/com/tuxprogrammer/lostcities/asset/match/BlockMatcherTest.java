package com.tuxprogrammer.lostcities.asset.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.model.Stuff;

/** Block matchers: block references and ore names in place of tags; unknown blocks never match air. */
public class BlockMatcherTest {

    private static Stuff stuff(String matcher) {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/stuff/s.json",
            "{\"tags\": [\"t\"], \"column\": \"{\", \"mincount\": 1, \"maxcount\": 2, \"attempts\": 3,"
                + " \"blocks\": "
                + matcher
                + "}");
        return set.stuff(AssetId.parse("s"));
    }

    @Test
    public void mappedNamesMatchEveryBlockOfTheirChainThatExists() {
        // minecraft:stone_bricks maps to minecraft:stonebrick, which the vanilla registry has.
        Stuff stuff = stuff("{\"if_any\": [\"minecraft:stone_bricks\"]}");
        assertTrue(
            stuff.blocks()
                .matches(BlockRef.parse("minecraft:stonebrick:2"), Collections.<String>emptySet()));
        assertFalse(
            stuff.blocks()
                .matches(BlockRef.parse("minecraft:dirt:0"), Collections.<String>emptySet()));
    }

    @Test
    public void oreNamesStandInForBlockTags() {
        Stuff stuff = stuff("{\"if_any\": [\"ore:stone\"], \"excluding\": [\"minecraft:air\"]}");
        assertTrue(
            stuff.blocks()
                .matches(BlockRef.parse("minecraft:stone:0"), Collections.singleton("stone")));
        assertFalse(
            stuff.blocks()
                .matches(BlockRef.parse("minecraft:dirt:0"), Collections.<String>emptySet()));
    }

    @Test
    public void noClausesMatchesAnyBlock() {
        assertTrue(BlockMatcher.ANY.matches(BlockRef.AIR, Collections.<String>emptySet()));
    }

    @Test
    public void doesNotReproduceCORE20_anUnknownBlockIsReportedAndMatchesNothingNotAir() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/stuff/s.json",
            "{\"tags\": [\"t\"], \"column\": \"{\", \"mincount\": 1, \"maxcount\": 2, \"attempts\": 3,"
                + " \"blocks\": {\"if_any\": [\"nosuchmod:nosuchblock:0\"]}}");
        List<String> errors = TestAssets.lines(set, AssetProblem.Severity.ERROR);
        assertTrue(
            errors.get(0)
                .contains("nosuchmod:nosuchblock"),
            errors.toString());
        assertEquals(0, set.count(com.tuxprogrammer.lostcities.asset.AssetType.STUFF));
    }

    @Test
    public void blockTagsAreRejectedWithAHint() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/stuff/s.json",
            "{\"tags\": [\"t\"], \"column\": \"{\", \"mincount\": 1, \"maxcount\": 2, \"attempts\": 3,"
                + " \"blocks\": {\"if_any\": [\"#forge:stone\"]}}");
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("ore:stone"));
    }
}
