package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Multibuildings: a grid of buildings over adjacent chunks, outer list by X. */
public class MultiBuildingTest {

    private static AssetSet multi(String json) {
        return TestAssets.loadFiles(
            "lostcities/parts/p.json",
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]]}",
            "lostcities/buildings/a.json",
            "{\"filler\": \"#\", \"parts\": [{\"part\": \"p\"}]}",
            "lostcities/buildings/b.json",
            "{\"filler\": \"#\", \"parts\": [{\"part\": \"p\"}]}",
            "lostcities/multibuildings/m.json",
            json);
    }

    @Test
    public void theOuterListIsIndexedByX() {
        MultiBuilding multi = multi("{\"dimx\": 2, \"dimz\": 1, \"buildings\": [[\"a\"], [\"b\"]]}")
            .multiBuilding(AssetId.parse("m"));
        assertEquals(
            AssetId.parse("a"),
            multi.building(0, 0)
                .id());
        assertEquals(
            AssetId.parse("b"),
            multi.building(1, 0)
                .id());
        assertEquals(
            2,
            multi.buildings()
                .size());
    }

    @Test
    public void emptyNamesLeaveAChunkOut() {
        MultiBuilding multi = multi("{\"dimx\": 1, \"dimz\": 2, \"buildings\": [[\"a\", \"\"]]}")
            .multiBuilding(AssetId.parse("m"));
        assertNull(multi.building(0, 1));
        assertEquals(
            1,
            multi.buildings()
                .size());
    }

    @Test
    public void doesNotReproduceASSET31_theGridMustMatchItsSize() {
        AssetSet wideGrid = multi("{\"dimx\": 1, \"dimz\": 1, \"buildings\": [[\"a\"], [\"b\"]]}");
        assertEquals(0, wideGrid.count(AssetType.MULTIBUILDINGS));
        AssetSet shortColumn = multi("{\"dimx\": 2, \"dimz\": 2, \"buildings\": [[\"a\", \"b\"], [\"a\"]]}");
        assertEquals(0, shortColumn.count(AssetType.MULTIBUILDINGS));
        assertTrue(
            TestAssets.lines(shortColumn, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("dimz = 2"));
    }

    @Test
    public void theBundledMultibuildingsFitTheirGrids() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        assertEquals(14, set.count(AssetType.MULTIBUILDINGS));
        MultiBuilding restaurant = set.multiBuilding(AssetId.parse("highway_restaurant"));
        assertEquals(2, restaurant.dimX());
        assertEquals(1, restaurant.dimZ());
    }
}
