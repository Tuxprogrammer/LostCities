package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Stuff columns (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Stuff JSON"). */
public class StuffTest {

    private static AssetSet stuff(String fields) {
        return TestAssets.loadFiles(
            "lostcities/stuff/s.json",
            "{\"tags\": [\"rubble\"], \"column\": \"{\", \"attempts\": 8, " + fields + "}");
    }

    @Test
    public void theBundledChainsKeepUpstreamsSettings() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        Stuff chains = set.stuff(AssetId.parse("chains"));
        assertEquals("{", chains.column());
        assertEquals(5, chains.minCount());
        assertEquals(10, chains.maxCount());
        assertEquals(Boolean.TRUE, chains.inBuilding());
        assertTrue(
            chains.tags()
                .contains("rubble"));
    }

    @Test
    public void doesNotReproduceASSET21_countAndHeightRangesAreCheckedAndUnsetInbuildingMeansEither() {
        assertEquals(0, stuff("\"mincount\": 5, \"maxcount\": 4").count(AssetType.STUFF));
        AssetSet equal = stuff("\"mincount\": 3, \"maxcount\": 3");
        assertEquals(1, equal.count(AssetType.STUFF), "equal counts mean exactly that many, not a crash");
        assertEquals(
            0,
            stuff("\"mincount\": 1, \"maxcount\": 2, \"minheight\": 70, \"maxheight\": 70").count(AssetType.STUFF));
        assertNull(
            equal.stuff(AssetId.parse("s"))
                .inBuilding(),
            "unset inbuilding is 'either', not 'never'");
    }
}
