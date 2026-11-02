package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** City styles and their inheritance (docs/original-spec/05-assets/styles-and-city-styles.md). */
public class CityStyleTest {

    private static final AssetSet BUNDLED = TestAssets.bundled(TestAssets.vanilla());

    private static CityStyle bundled(String name) {
        return BUNDLED.cityStyle(AssetId.parse(name));
    }

    @Test
    public void childrenTakeUnsetSettingsFromTheirParents() {
        CityStyle standard = bundled("citystyle_standard");
        assertEquals(
            AssetId.parse("standard"),
            standard.style()
                .id());
        assertEquals(Character.valueOf('S'), standard.character(CityStyle.Setting.STREET));
        assertEquals(Character.valueOf('x'), standard.character(CityStyle.Setting.PARK_ELEVATION_BLOCK));
        assertTrue(
            standard.stuffTags()
                .contains("rubble"));
        assertTrue(
            standard.stuffTags()
                .contains(CityStyle.ALL_STUFF));
        assertEquals(
            8,
            standard.selectors(CityStyle.SelectorKind.BUILDINGS)
                .size());
        assertEquals(
            AssetId.parse("street_large_full"),
            standard.streetParts(CityStyle.StreetSet.LARGE_PARTS, CityStyle.StreetSlot.FULL)
                .get(0)
                .id());
        assertEquals(
            AssetId.parse("street_full"),
            standard.streetParts(CityStyle.StreetSet.PARTS, CityStyle.StreetSlot.FULL)
                .get(0)
                .id(),
            "a set no style names uses upstream's default part names");
        assertEquals(
            standard.streetParts(CityStyle.StreetSet.PARTS, CityStyle.StreetSlot.BEND),
            standard.streetParts(CityStyle.StreetSet.TERTIARY_PARTS, CityStyle.StreetSlot.BEND),
            "minor streets fall back to normal streets");
    }

    @Test
    public void doesNotReproduceASSET06_aChildsSelectorListReplacesItsParents() {
        CityStyle border = bundled("citystyle_border");
        assertTrue(
            border.selectors(CityStyle.SelectorKind.MULTIBUILDINGS)
                .isEmpty(),
            "citystyle_border's empty multibuildings list means none (D15)");
        assertEquals(
            5,
            border.selectors(CityStyle.SelectorKind.BUILDINGS)
                .size(),
            "its five buildings are not doubled by the parent's");
        assertEquals(
            12,
            bundled("citystyle_common").selectors(CityStyle.SelectorKind.MULTIBUILDINGS)
                .size());
    }

    @Test
    public void doesNotReproduceASSET07_generalBlocksAndParkGrassInherit() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/citystyles/parent.json",
            "{\"generalblocks\": {\"ironbars\": \"|\", \"rubbledirt\": \"r\"}, \"parkblocks\": {\"grass\": \"g\"}}",
            "lostcities/citystyles/child.json",
            "{\"inherit\": \"parent\", \"parkblocks\": {\"elevation\": \"e\"}}");
        CityStyle child = set.cityStyle(AssetId.parse("child"));
        assertEquals(Character.valueOf('|'), child.character(CityStyle.Setting.IRON_BARS));
        assertEquals(Character.valueOf('r'), child.character(CityStyle.Setting.RUBBLE_DIRT));
        assertEquals(Character.valueOf('g'), child.character(CityStyle.Setting.PARK_GRASS));
        assertEquals(Character.valueOf('e'), child.character(CityStyle.Setting.PARK_ELEVATION_BLOCK));
    }

    @Test
    public void doesNotReproduceASSET22_deadStreetFieldsWarn() {
        // Upstream's bundled files carry them: a note, so the boot has no warnings.
        List<String> notes = TestAssets.lines(BUNDLED, AssetProblem.Severity.NOTE);
        assertTrue(
            notes.stream()
                .anyMatch(w -> w.contains("citystyle_config.json") && w.contains("streetblocks.width")),
            notes.toString());
        // An override that sets one gets the warning.
        AssetSet override = TestAssets
            .loadFiles("lostcities/citystyles/c.json", "{\"streetblocks\": {\"streetvariant\": \"B\"}}");
        assertTrue(
            TestAssets.lines(override, AssetProblem.Severity.WARNING)
                .stream()
                .anyMatch(w -> w.contains("streetblocks.streetvariant")));
    }

    @Test
    public void doesNotReproduceASSET23_characterFieldsAreOneCharacter() {
        AssetSet set = TestAssets.loadFiles("lostcities/citystyles/c.json", "{\"streetblocks\": {\"street\": \"\"}}");
        assertEquals(0, set.count(AssetType.CITYSTYLES));
    }

    @Test
    public void doesNotReproduceCITY37_noApiExposesTheStreetWidth() {
        for (Method method : CityStyle.class.getMethods()) {
            assertFalse(
                method.getName()
                    .toLowerCase()
                    .contains("width"),
                method.toString());
        }
    }

    @Test
    public void doesNotReproduceASSET28_aMissingParentOrACycleRejectsTheStyle() {
        AssetSet missing = TestAssets.loadFiles(
            "lostcities/citystyles/orphan.json",
            "{\"inherit\": \"nosuch\", \"streetblocks\": {\"street\": \"S\"}}");
        assertNull(missing.cityStyle(AssetId.parse("orphan")));
        assertTrue(
            TestAssets.lines(missing, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("does not exist"));
        AssetSet cycle = TestAssets.loadFiles(
            "lostcities/citystyles/a.json",
            "{\"inherit\": \"b\"}",
            "lostcities/citystyles/b.json",
            "{\"inherit\": \"a\"}",
            "lostcities/citystyles/c.json",
            "{\"inherit\": \"a\"}");
        assertEquals(0, cycle.count(AssetType.CITYSTYLES));
        assertTrue(
            TestAssets.lines(cycle, AssetProblem.Severity.ERROR)
                .stream()
                .anyMatch(e -> e.contains("cycle")));
    }

    @Test
    public void doesNotReproduceASSET32_anEmptyStreetPartListIsRejected() {
        AssetSet set = TestAssets
            .loadFiles("lostcities/citystyles/c.json", "{\"streetblocks\": {\"parts\": {\"full\": []}}}");
        assertEquals(0, set.count(AssetType.CITYSTYLES));
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("must not be empty"));
    }

    @Test
    public void optionalSelectorsWarnAndActAsAbsent() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/citystyles/c.json",
            "{\"selectors\": {\"stairs\": [{\"factor\": 1, \"value\": \"nosuch_stair\"}]}}");
        CityStyle style = set.cityStyle(AssetId.parse("c"));
        assertTrue(
            style.selectors(CityStyle.SelectorKind.STAIRS)
                .isEmpty());
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.WARNING)
                .stream()
                .anyMatch(w -> w.contains("nosuch_stair")));
    }
}
