package com.tuxprogrammer.lostcities.asset.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** The catalog's variety pools as data (D25, D28, D29; block-catalog/index.md, "Variety pools"). */
public class VarietyPoolsTest {

    @Test
    public void everyPoolTableIsLoaded() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        VarietyPools pools = set.pools();
        assertTrue(
            pools.pools()
                .size() >= 60,
            "the catalog's pool tables: " + pools.pools()
                .keySet());
        assertTrue(pools.blockCount() > 300, "distinct candidate registry names: " + pools.blockCount());
        for (AssetProblem problem : set.problems()) {
            assertTrue(
                !problem.file()
                    .contains("pools.json") || problem.severity() == AssetProblem.Severity.NOTE,
                problem.toString());
        }
    }

    @Test
    public void theStoneBrickWallKeepsTheCatalogsWeights() {
        VarietyPools.Pool wall = TestAssets.bundled(TestAssets.vanilla())
            .pools()
            .pool("stone-brick-main-wall");
        assertNotNull(wall);
        int total = 0;
        for (VarietyPools.Candidate candidate : wall.candidates()) {
            total += candidate.weight();
        }
        assertEquals(100, total);
        VarietyPools.Candidate baseline = wall.candidates()
            .get(0);
        assertTrue(baseline.baseline());
        assertEquals(40, baseline.weight());
        assertTrue(
            baseline.blocks()
                .contains(BlockRef.parse("minecraft:stonebrick:0")));
        VarietyPools.Candidate chisel = wall.candidates()
            .get(1);
        assertTrue(
            chisel.flags()
                .contains(VarietyPools.Flag.SP));
        assertTrue(chisel.fallbackToBaseline());
    }

    @Test
    public void unavailableCandidatesAreNeverPickedAndAreReported() {
        VarietyPools.Pool wall = TestAssets.bundled(TestAssets.vanilla())
            .pools()
            .pool("stone-brick-main-wall");
        Random random = new Random(11);
        for (int i = 0; i < 200; i++) {
            assertTrue(
                wall.pick(random)
                    .baseline(),
                "only the vanilla baseline exists without the pack");
        }
        assertTrue(
            !TestAssets.bundled(TestAssets.vanilla())
                .pools()
                .unresolved()
                .isEmpty());
    }

    @Test
    public void inTheFullPackEveryCandidateBlockResolves() {
        AssetSet vanilla = TestAssets.bundled(TestAssets.vanilla());
        Set<String> names = new HashSet<>(TestAssets.vanillaNames());
        for (VarietyPools.Pool pool : vanilla.pools()
            .pools()
            .values()) {
            for (VarietyPools.Candidate candidate : pool.candidates()) {
                for (BlockRef block : candidate.blocks()) {
                    names.add(block.name());
                }
            }
        }
        VarietyPools pools = VarietyPools.parse(
            TestAssets.blockData()
                .pools(),
            BlockRegistryView.of(names),
            new Problems().in("pools"));
        assertEquals(
            0,
            pools.unresolved()
                .size());
    }
}
