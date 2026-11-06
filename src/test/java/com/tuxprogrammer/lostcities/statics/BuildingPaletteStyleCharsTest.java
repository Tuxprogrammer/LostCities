package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Palette;

/**
 * No bundled building palette redefines a character a city style uses for its own terrain blocks (street border and
 * wall, rail, park, corridor, sphere and general blocks). The chunk palette layers the building's palette over the
 * style's, as upstream's {@code BuildingInfo.getCompiledPalette} does, so such a character silently restyles the
 * street border, the foundation fill and the walls of every chunk the building stands on.
 *
 * <p>
 * Pins the RWG2 seed 777 boot crash: {@code nh_residential} mapped {@code y}, the style's
 * {@code streetblocks.border}, to farmland and {@code w} ({@code streetblocks.wall}) to wheat, so a residential lot
 * raised over low ground filled its foundation with a solid cube of farmland. Farmland under a solid block turns to
 * dirt on any neighbor update, and each conversion notifies the next, so the first update recursed through the cube
 * until the stack overflowed (decision log D380).
 */
public class BuildingPaletteStyleCharsTest {

    @Test
    public void noBuildingPaletteRedefinesACityStyleCharacter() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        Map<Character, List<String>> styleChars = styleCharacters(set);
        assertFalse(styleChars.isEmpty(), "the bundled city styles name terrain characters");
        assertTrue(styleChars.containsKey('y'), "the bundled street border character is known: " + styleChars.keySet());

        List<String> clashes = new ArrayList<>();
        for (AssetId id : set.ids(AssetType.BUILDINGS)) {
            Building building = set.building(id);
            Palette palette = building.palette();
            if (palette == null) {
                continue;
            }
            for (Character c : palette.entries()
                .keySet()) {
                List<String> uses = styleChars.get(c);
                if (uses != null) {
                    clashes.add(id + " redefines '" + c + "' used by " + uses);
                }
            }
        }
        assertEquals(new ArrayList<String>(), clashes);
    }

    /** Returns each character a bundled city style sets, with the settings that use it. */
    private static Map<Character, List<String>> styleCharacters(AssetSet set) {
        Map<Character, List<String>> out = new TreeMap<>();
        for (AssetId id : set.ids(AssetType.CITYSTYLES)) {
            CityStyle style = set.cityStyle(id);
            for (CityStyle.Setting setting : CityStyle.Setting.values()) {
                if (!setting.isCharacter()) {
                    continue;
                }
                Character c = style.character(setting);
                if (c != null) {
                    out.computeIfAbsent(c, k -> new ArrayList<>())
                        .add(id + ":" + setting.path());
                }
            }
        }
        return out;
    }
}
