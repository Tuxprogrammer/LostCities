package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Parts: slices, palettes and metadata (docs/original-spec/05-assets/parts-and-placement.md, "Part JSON"). */
public class PartTest {

    private static AssetSet part(String json) {
        return TestAssets.loadFiles("lostcities/parts/p.json", json);
    }

    @Test
    public void slicesAreBottomFirstRowsAlongZ() {
        AssetSet set = part(
            "{\"xsize\": 3, \"zsize\": 2, \"slices\": [[\"abc\", \"def\"], [\"   \", \" g \"]],"
                + " \"meta\": [{\"key\": \"nowater\", \"boolean\": true}, {\"key\": \"z1\", \"integer\": 4},"
                + " {\"key\": \"support\", \"char\": \"v\"}]}");
        Part part = set.part(AssetId.parse("p"));
        assertEquals(3, part.xsize());
        assertEquals(2, part.zsize());
        assertEquals(2, part.height());
        assertEquals('a', part.get(0, 0, 0));
        assertEquals('c', part.get(2, 0, 0));
        assertEquals('e', part.get(1, 0, 1));
        assertEquals('g', part.get(1, 1, 1));
        assertArrayEquals(new char[] { 'e', 'g' }, part.column(1, 1));
        assertArrayEquals(new char[] { 'a', ' ' }, part.column(0, 0));
        assertTrue(part.metaBoolean(Part.MetaKey.NOWATER));
        assertFalse(part.metaBoolean(Part.MetaKey.FORCEDAIR));
        assertEquals(Integer.valueOf(4), part.metaInteger(Part.MetaKey.Z1));
        assertEquals(Character.valueOf('v'), part.metaCharacter(Part.MetaKey.SUPPORT));
        assertNull(part.palette());
    }

    @Test
    public void allSpaceColumnsAreNull() {
        Part part = part("{\"xsize\": 2, \"zsize\": 1, \"slices\": [[\"a \"]]}").part(AssetId.parse("p"));
        assertNull(part.column(1, 0));
    }

    @Test
    public void doesNotReproduceASSET24_aShortRowOrMissingRowRejectsThePart() {
        AssetSet shortRow = part("{\"xsize\": 3, \"zsize\": 2, \"slices\": [[\"abc\", \"de\"]]}");
        assertEquals(0, shortRow.count(AssetType.PARTS));
        List<String> errors = TestAssets.lines(shortRow, AssetProblem.Severity.ERROR);
        assertTrue(
            errors.get(0)
                .contains("slices[0][1]")
                && errors.get(0)
                    .contains("xsize is 3"),
            errors.toString());
        AssetSet missingRow = part("{\"xsize\": 3, \"zsize\": 2, \"slices\": [[\"abc\"]]}");
        assertEquals(0, missingRow.count(AssetType.PARTS));
    }

    @Test
    public void doesNotReproduceASSET23_aCharMetaMustBeOneCharacter() {
        AssetSet set = part(
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"a\"]], \"meta\": [{\"key\": \"support\", \"char\": \"\"}]}");
        assertEquals(0, set.count(AssetType.PARTS));
        AssetSet wrongType = part(
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"a\"]], \"meta\": [{\"key\": \"nowater\", \"integer\": 1}]}");
        assertEquals(0, wrongType.count(AssetType.PARTS), "a known key with the wrong type is rejected at load");
    }

    @Test
    public void inlinePaletteWinsOverRefpalette() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/palettes/rails.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:dirt\"}]}",
            "lostcities/parts/p.json",
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]], \"refpalette\": \"rails\","
                + " \"palette\": {\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:glass\"}]}}",
            "lostcities/parts/q.json",
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]], \"refpalette\": \"rails\"}");
        assertEquals(
            AssetId.parse("lostcities:p/__local__"),
            set.part(AssetId.parse("p"))
                .palette()
                .id());
        assertEquals(
            AssetId.parse("rails"),
            set.part(AssetId.parse("q"))
                .palette()
                .id());
    }

    @Test
    public void theBundledPartsMatchTheCatalog() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        // docs/original-spec/05-assets/index.md, "Heights": radiotower 24 slices.
        assertEquals(
            24,
            set.part(AssetId.parse("radiotower"))
                .height());
        assertEquals(
            16,
            set.part(AssetId.parse("cabin"))
                .xsize());
    }
}
