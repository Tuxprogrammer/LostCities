package com.tuxprogrammer.lostcities.asset.match;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Name matchers compare full ids (P05-2, D31). */
public class NameMatcherTest {

    private static NameMatcher matcher(String json) {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/stuff/s.json",
            "{\"tags\": [\"t\"], \"column\": \"{\", \"mincount\": 1, \"maxcount\": 2, \"attempts\": 3,"
                + " \"buildings\": "
                + json
                + "}");
        return set.stuff(AssetId.parse("s"))
            .buildings();
    }

    @Test
    public void doesNotReproduceASSET33_bareAndFullNamesMatchEachOther() {
        NameMatcher bare = matcher("{\"if_any\": [\"building1\"]}");
        NameMatcher full = matcher("{\"if_any\": [\"lostcities:building1\"]}");
        AssetId id = AssetId.parse("lostcities:building1");
        assertTrue(bare.matches(id));
        assertTrue(full.matches(id));
        assertTrue(bare.matches(AssetId.parse("building1")));
    }

    @Test
    public void ifAnyAndExcluding() {
        NameMatcher matcher = matcher("{\"if_any\": [\"a\", \"b\"], \"excluding\": [\"b\"]}");
        assertTrue(matcher.matches(AssetId.parse("a")));
        assertFalse(matcher.matches(AssetId.parse("b")));
        assertFalse(matcher.matches(AssetId.parse("c")));
        assertFalse(matcher.matches(null));
        assertTrue(NameMatcher.ANY.matches(null));
    }

    @Test
    public void otherNamespacesStayTheirOwn() {
        NameMatcher matcher = matcher("{\"if_any\": [\"mypack:tower\"]}");
        assertTrue(matcher.matches(AssetId.parse("mypack:tower")));
        assertFalse(matcher.matches(AssetId.parse("tower")));
    }
}
