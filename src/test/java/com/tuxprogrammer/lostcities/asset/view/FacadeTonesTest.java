package com.tuxprogrammer.lostcities.asset.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.layout.view.FacadeTone;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;

/** Facade tones measured from the bundled assets (docs/design/0012-facade-color-feathering.md §2 and §3). */
public class FacadeTonesTest {

    private static final FacadeTones TONES = new FacadeTones(CityFixtures.ASSETS, CityFixtures.ASSETS.colors());

    private static List<String> stack(String bricks) {
        return Arrays.asList(
            "lostcities:common",
            "lostcities:default_desert",
            "lostcities:" + bricks,
            "lostcities:glass_full",
            "lostcities:glass_side_variant_glass");
    }

    @Test
    public void aBuildingsFacadeCharactersAreOnItsOuterRing() {
        Map<Character, Integer> counts = TONES
            .facadeCharacters(CityFixtures.ASSETS.building(AssetId.parse("lostcities:building1")));
        assertFalse(counts.isEmpty());
        assertTrue(counts.containsKey('#'), "the main wall: " + counts);
    }

    @Test
    public void sandstoneIsPalerThanTaupe() {
        FacadeTone sandstone = TONES.tone(stack("bricks_desert"), "lostcities:building1");
        FacadeTone taupe = TONES.tone(stack("bricks_desert_taupe"), "lostcities:building1");
        assertNotNull(sandstone);
        assertNotNull(taupe);
        assertTrue(sandstone.lightness() > taupe.lightness() + 10, "sandstone " + sandstone + ", taupe " + taupe);
    }

    @Test
    public void redTerracottaBesideSandstoneBreaksTheDefaultLimit() {
        FacadeTone sandstone = TONES.tone(stack("bricks_desert"), "lostcities:building1");
        FacadeTone red = TONES.tone(stack("bricks_desert_red"), "lostcities:building1");
        FacadeTone stone = TONES.tone(stack("bricks_standard"), "lostcities:building1");
        assertTrue(FacadeTone.contrast(sandstone, red) > 45, "sandstone and red " + sandstone + " " + red);
        assertTrue(FacadeTone.contrast(sandstone, stone) <= 45, "sandstone and stone " + sandstone + " " + stone);
    }

    @Test
    public void anUnknownBuildingHasNoTone() {
        assertNull(TONES.tone(stack("bricks_desert"), "lostcities:nosuchbuilding"));
    }

    @Test
    public void tonesAreCachedAndStable() {
        FacadeTone first = TONES.tone(stack("bricks_cyan"), "lostcities:building2");
        FacadeTone second = TONES.tone(stack("bricks_cyan"), "lostcities:building2");
        assertEquals(first.lightness(), second.lightness(), 0f);
        assertEquals(first.saturation(), second.saturation(), 0f);
    }
}
