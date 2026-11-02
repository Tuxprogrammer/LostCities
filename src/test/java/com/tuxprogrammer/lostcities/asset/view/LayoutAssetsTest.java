package com.tuxprogrammer.lostcities.asset.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.layout.view.AssetView;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.PartContext;
import com.tuxprogrammer.lostcities.layout.view.Selection;
import com.tuxprogrammer.lostcities.layout.view.WorldStyleView;

/** The asset set through the layout's views (decision log D51), with their draw contracts. */
public class LayoutAssetsTest {

    private static final AssetSet SET = TestAssets.bundled(TestAssets.vanilla());

    private static AssetView view(String... types) {
        BiomeDescriptor biome = BiomeDescriptor.of(new HashSet<>(Arrays.asList(types)), "Test", null);
        return new LayoutAssets(SET, SET.worldStyle(AssetId.parse("standard")), (x, z) -> biome);
    }

    /** A random that counts its float draws. */
    private static final class Counting extends Random {

        private static final long serialVersionUID = 1L;
        int floats;

        Counting(long seed) {
            super(seed);
        }

        @Override
        public float nextFloat() {
            floats++;
            return super.nextFloat();
        }
    }

    @Test
    public void theWorldStylePicksCityStylesByBiomeWithOneDraw() {
        WorldStyleView world = view("PLAINS").worldStyle();
        Counting random = new Counting(1);
        assertEquals("lostcities:citystyle_standard", world.pickCityStyle(random, 0, 0));
        assertEquals(1, random.floats);
        int desert = 0;
        Random sandy = new Random(2);
        WorldStyleView desertWorld = view("SANDY", "HOT").worldStyle();
        for (int i = 0; i < 100; i++) {
            if ("lostcities:citystyle_desert".equals(desertWorld.pickCityStyle(sandy, 0, 0))) {
                desert++;
            }
        }
        assertTrue(desert > 80, "factor 9 against 0.5: " + desert);
        assertEquals(
            0.3f,
            view("RIVER").worldStyle()
                .cityChanceMultiplier(0, 0),
            0f);
        assertEquals(
            10,
            world.multiSettings()
                .areaSize());
        assertEquals("lostcities:outside", world.outsideStyleName());
        assertFalse(world.buildingsBlockRailways());
    }

    @Test
    public void cityStylesAndTheirLists() {
        AssetView view = view("PLAINS");
        CityStyleView border = view.cityStyle("citystyle_border");
        assertEquals("lostcities:standard_border", border.styleName());
        assertEquals(0.2f, border.buildingChance(), 0f);
        assertEquals(Integer.valueOf(1), border.maxFloors());
        assertNull(border.pick(Selection.MULTIBUILDINGS, new Random(1), 0, 0), "D15: border has no multibuildings");
        Counting random = new Counting(3);
        String building = border.pick(Selection.BUILDINGS, random, 0, 0);
        assertNotNull(view.building(building));
        assertEquals(1, random.floats);
    }

    @Test
    public void buildingsPickPartsPerLevel() {
        AssetView view = view("PLAINS");
        BuildingView building = view.building("lostcities:building1");
        assertNotNull(building);
        String roof = building
            .pickPart(new Random(1), new PartContext(70, 3, 1, 3, PartContext.NONE, "x", building.name(), 0, 0));
        assertTrue(roof.startsWith("lostcities:top"), "building1's top floor takes a roof part: " + roof);
        String ground = building.pickPart(
            new Random(1),
            new PartContext(64, 0, 1, 3, PartContext.NONE, PartContext.NONE, building.name(), 0, 0));
        assertNotNull(ground);
        assertFalse(view.partDontConnect(ground));
        assertNull(view.building("nosuch"));
    }

    @Test
    public void stylesPickOnePalettePerGroup() {
        Counting random = new Counting(4);
        List<String> palettes = view("PLAINS").style("standard")
            .pickPalettes(random);
        assertEquals(5, palettes.size());
        assertEquals(5, random.floats);
        assertEquals("lostcities:common", palettes.get(0));
    }

    @Test
    public void multibuildingsGiveTheirGrid() {
        assertEquals(
            "lostcities:highway_restaurant_parking",
            view("PLAINS").multiBuilding("highway_restaurant")
                .buildingAt(1, 0));
    }
}
