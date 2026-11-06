package com.tuxprogrammer.lostcities.layout;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Layout goldens at the settings a new world gets, not the legacy pin (D282 onward): the {@code default} profile with
 * every new-world option on (density rings, lot footprints, city districts, pack houses, door steps), and a
 * {@code rarecities} city with {@code cityHighwayLayout=beltway} (jittered grid, links, railway core ends, ramp holds).
 */
public class NewWorldLayoutGoldenTest {

    private static LayoutPlanner planner(long seed, ProfileValues values, boolean flat) {
        TerrainSettings settings = TerrainSettings.fromValues(values.asMap());
        WorldStyle worldStyle = CityFixtures.ASSETS.worldStyle(CityProfiles.worldStyle(values));
        NetworkSettings networks = CityProfiles.networkSettings(values, ModuleState.allActive(), PlanningGate.NONE);
        return new LayoutPlanner(
            LayoutInputs
                .builder(
                    seed,
                    CityProfiles.layoutProfile(settings, values),
                    flat ? LayoutFixtures.standardAssets()
                        : new LayoutAssets(CityFixtures.ASSETS, worldStyle, CharacterFixtures.desertWest()),
                    flat ? LayoutFixtures.flatTerrain() : CityFixtures.terrain())
                .networks(networks)
                .build());
    }

    @Test
    public void theDefaultProfileCityMatchesItsGolden() throws Exception {
        LayoutPlanner p = planner(4242L, ProfileValues.defaults(), false);
        LayoutGoldens.assertMatches(
            "newworld_default_city",
            "The default profile at every new-world setting (seed 4242, character fixture world): density rings,\n"
                + "lot footprints, city districts, pack houses; not the legacy pin.",
            LayoutText.maps(p, -30, -30, 30, 30),
            LayoutText.details(p, -30, -30, 30, 30));
    }

    @Test
    public void aRareCitiesBeltwayMatchesItsGolden() throws Exception {
        Map<String, String> map = new HashMap<>(BuiltinProfiles.values("rarecities"));
        map.put("cityHighwayLayout", "beltway");
        LayoutPlanner p = planner(7919L, ProfileValues.of(map), true);
        CityLinkGraph graph = p.networks()
            .links();
        int[] node = null;
        for (int cz = -3; cz <= 3 && node == null; cz++) {
            for (int cx = -3; cx <= 3 && node == null; cx++) {
                node = graph.node(cx, cz);
            }
        }
        if (node == null) {
            throw new AssertionError("a city node within three grid cells of the origin");
        }
        int cx = node[0];
        int cz = node[1];
        int r = 48;
        StringBuilder net = new StringBuilder();
        Networks n = p.networks();
        for (int z = cz - r; z <= cz + r; z++) {
            for (int x = cx - r; x <= cx + r; x++) {
                boolean highway = n.highway(x, z)
                    .hasHighway();
                RailChunk rail = n.railChunk(x, z);
                net.append(
                    rail.type()
                        .isStation() ? 'S' : rail.isPresent() ? (highway ? '+' : '=') : highway ? 'H' : '.');
            }
            net.append('\n');
        }
        LayoutGoldens.assertMatches(
            "newworld_beltway",
            "rarecities with cityHighwayLayout=beltway (seed 7919, flat ground): the first grid city's surroundings,\n"
                + "highways (H), track (=), stations (S) and both (+), chunks " + (cx - r)
                + ","
                + (cz - r)
                + " to "
                + (cx + r)
                + ","
                + (cz + r)
                + ".",
            net.toString(),
            "node chunk " + cx + "," + cz + "\n" + LayoutText.details(p, cx - 8, cz - 8, cx + 8, cz + 8));
    }
}
