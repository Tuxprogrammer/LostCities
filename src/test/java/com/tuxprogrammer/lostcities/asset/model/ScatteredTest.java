package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Scattered structures (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Scattered JSON"). */
public class ScatteredTest {

    private static AssetSet scattered(String json) {
        return TestAssets.loadFiles(
            "lostcities/parts/p.json",
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]]}",
            "lostcities/buildings/b.json",
            "{\"filler\": \"#\", \"parts\": [{\"part\": \"p\"}]}",
            "lostcities/scattered/s.json",
            json);
    }

    @Test
    public void theBundledStructuresKeepUpstreamsSettings() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        Scattered gas = set.scattered(AssetId.parse("highway_gas_station"));
        assertTrue(gas.rotatable());
        assertTrue(gas.clearHighwayRailing());
        assertEquals(Scattered.TerrainHeight.HIGHEST, gas.terrainHeight());
        assertEquals(Scattered.TerrainFix.REPEATSLICE, gas.terrainFix());
        assertEquals(
            AssetId.parse("roadside_supports"),
            gas.supportPart()
                .id());
        Scattered rig = set.scattered(AssetId.parse("oilrig"));
        assertEquals(Scattered.TerrainHeight.OCEAN, rig.terrainHeight());
        assertEquals(1, rig.heightOffset());
        assertEquals(
            AssetId.parse("oilrig"),
            rig.multiBuilding()
                .id());
    }

    @Test
    public void doesNotReproduceASSET25_anUnknownEnumIsALoadErrorNamingTheFile() {
        AssetSet set = scattered("{\"buildings\": [\"b\"], \"terrainheight\": \"middle\", \"terrainfix\": \"none\"}");
        assertEquals(0, set.count(AssetType.SCATTERED));
        String error = TestAssets.lines(set, AssetProblem.Severity.ERROR)
            .get(0);
        assertTrue(error.contains("s.json") && error.contains("terrainheight"), error);
    }

    @Test
    public void doesNotReproduceCITY32_enumsParseWithAFixedLocale() {
        Locale before = Locale.getDefault();
        try {
            // In Turkish, "repeatslice".toUpperCase() has a dotted I and matched nothing upstream.
            Locale.setDefault(new Locale("tr", "TR"));
            AssetSet set = scattered(
                "{\"buildings\": [\"b\"], \"terrainheight\": \"Highest\", \"terrainfix\": \"repeatslice\"}");
            assertEquals(
                Scattered.TerrainFix.REPEATSLICE,
                set.scattered(AssetId.parse("s"))
                    .terrainFix());
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    public void neitherBuildingsNorAMultibuildingIsRejected() {
        AssetSet set = scattered("{\"terrainheight\": \"lowest\", \"terrainfix\": \"none\"}");
        assertEquals(0, set.count(AssetType.SCATTERED));
    }
}
