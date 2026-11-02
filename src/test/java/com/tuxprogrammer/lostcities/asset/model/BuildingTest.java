package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Buildings and floor-by-floor part selection (docs/original-spec/05-assets/buildings-and-multibuildings.md). */
public class BuildingTest {

    private static final String PART = "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]]}";

    private static AssetSet building(String json) {
        return TestAssets.loadFiles(
            "lostcities/parts/floor.json",
            PART,
            "lostcities/parts/roof.json",
            PART,
            "lostcities/parts/cellar.json",
            PART,
            "lostcities/parts/overlay.json",
            PART,
            "lostcities/buildings/b.json",
            json);
    }

    @Test
    public void readsUpstreamsFieldsAndDefaults() {
        Building building = building(
            "{\"filler\": \"#\", \"rubble\": \"}\", \"maxfloors\": 4, \"preferslonely\": 0.5, \"allowDoors\": false,"
                + " \"parts\": [{\"part\": \"floor\", \"top\": false}, {\"part\": \"roof\", \"top\": true}]}")
                    .building(AssetId.parse("b"));
        assertEquals('#', building.filler());
        assertEquals(Character.valueOf('}'), building.rubble());
        assertEquals(Building.UNSET, building.minFloors());
        assertEquals(4, building.maxFloors());
        assertEquals(0.5f, building.prefersLonely(), 0f);
        assertEquals(false, building.allowDoors());
        assertEquals(true, building.allowFillers());
        assertEquals(false, building.overrideFloors());
    }

    @Test
    public void picksUniformlyAmongTheMatchingReferences() {
        Building building = building(
            "{\"filler\": \"#\", \"parts\": [{\"part\": \"floor\", \"top\": false}, {\"part\": \"floor\", \"top\": false},"
                + " {\"part\": \"cellar\", \"cellar\": true}, {\"part\": \"roof\", \"top\": true}]}")
                    .building(AssetId.parse("b"));
        ConditionContext top = ConditionContext.builder()
            .floor(3)
            .floors(1, 3)
            .build();
        assertEquals(
            AssetId.parse("roof"),
            building.randomPart(new Random(1), top)
                .id());
        Set<AssetId> seen = new HashSet<>();
        Random random = new Random(5);
        ConditionContext cellar = ConditionContext.builder()
            .floor(-1)
            .floors(1, 3)
            .build();
        for (int i = 0; i < 50; i++) {
            seen.add(
                building.randomPart(random, cellar)
                    .id());
        }
        // A cellar is not top: the two floor references and the cellar reference match, duplicates weighing double.
        assertEquals(2, seen.size());
    }

    @Test
    public void doesNotReproduceASSET04_overlaysSeeThePartBelow() {
        Building building = building(
            "{\"filler\": \"#\", \"parts\": [{\"part\": \"floor\"}],"
                + " \"parts2\": [{\"part\": \"overlay\", \"belowpart\": \"floor\"}]}").building(AssetId.parse("b"));
        ConditionContext groundOverlay = ConditionContext.builder()
            .floor(0)
            .part(AssetId.parse("floor"))
            .build();
        assertNull(building.randomPart2(new Random(1), groundOverlay), "nothing is below the ground floor here");
        ConditionContext secondOverlay = ConditionContext.builder()
            .floor(1)
            .part(AssetId.parse("floor"))
            .belowPart(AssetId.parse("floor"))
            .build();
        assertEquals(
            AssetId.parse("overlay"),
            building.randomPart2(new Random(1), secondOverlay)
                .id());
    }

    @Test
    public void doesNotReproduceASSET23_fillerAndRubbleAreOneCharacter() {
        AssetSet empty = building("{\"filler\": \"\", \"parts\": [{\"part\": \"floor\"}]}");
        assertEquals(0, empty.count(AssetType.BUILDINGS));
        AssetSet longRubble = building("{\"filler\": \"#\", \"rubble\": \"}}\", \"parts\": [{\"part\": \"floor\"}]}");
        assertEquals(0, longRubble.count(AssetType.BUILDINGS));
    }

    @Test
    public void aMissingPartRejectsTheBuilding() {
        AssetSet set = building("{\"filler\": \"#\", \"parts\": [{\"part\": \"nosuch\"}]}");
        assertEquals(0, set.count(AssetType.BUILDINGS));
    }

    @Test
    public void aBuildingWithNoPartForSomeFloorIsReported() {
        AssetSet set = building("{\"filler\": \"#\", \"parts\": [{\"part\": \"roof\", \"top\": true}]}");
        assertEquals(1, set.count(AssetType.BUILDINGS));
        java.util.List<String> warnings = TestAssets.lines(set, AssetProblem.Severity.WARNING);
        assertTrue(
            warnings.stream()
                .anyMatch(w -> w.contains("no part matches floor 0")),
            warnings.toString());
    }

    @Test
    public void bundledBuildingsCoverEveryFloor() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        for (AssetProblem warning : set.problems(AssetProblem.Severity.WARNING)) {
            assertTrue(
                !warning.message()
                    .contains("no part matches floor"),
                warning.toString());
        }
    }
}
