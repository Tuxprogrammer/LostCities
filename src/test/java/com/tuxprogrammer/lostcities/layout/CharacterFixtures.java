package com.tuxprogrammer.lostcities.layout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Test worlds for M12's city character (docs/design/0010 and 0011): the bundled assets over the {@link CityFixtures}
 * terrain, with desert biomes west of X 0 and plains east of it, so desert and standard cities meet. Test code only.
 */
final class CharacterFixtures {

    private CharacterFixtures() {}

    /** Desert (sandy) west of block X 0, plains east of it. */
    static BiomeSource desertWest() {
        BiomeDescriptor desert = BiomeDescriptor
            .of(new HashSet<>(Arrays.asList("SANDY", "HOT", "DRY")), "Desert", null);
        BiomeDescriptor plains = BiomeDescriptor.of(new HashSet<>(Arrays.asList("PLAINS")), "Plains", null);
        return (x, z) -> x < 0 ? desert : plains;
    }

    /**
     * A planner over the mixed world with some profile values set.
     *
     * @param seed      the world seed
     * @param cacheSize the planner's cache size
     * @param values    {@code key, value} pairs of terrain-category options
     * @return the planner
     */
    static LayoutPlanner planner(long seed, int cacheSize, String... values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(values[i], values[i + 1]);
        }
        TerrainSettings settings = TerrainSettings.fromValues(map);
        WorldStyle worldStyle = CityFixtures.ASSETS.worldStyle(CityProfiles.WORLD_STYLE);
        return new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    CityProfiles.layoutProfile(settings),
                    new LayoutAssets(CityFixtures.ASSETS, worldStyle, desertWest()),
                    CityFixtures.terrain())
                .cacheSize(cacheSize)
                .build());
    }
}
