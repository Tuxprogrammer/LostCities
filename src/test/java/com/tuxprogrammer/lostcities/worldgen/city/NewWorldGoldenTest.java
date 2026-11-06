package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.Networks;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.worldgen.buffer.GoldenChunks;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Golden chunks at the settings a new world gets (D282 onward), not the legacy pin the older goldens use: floor pitch 8,
 * lot footprints, pack houses, furniture with room rules, open door steps, the ramp surface, railway core ends and ramp
 * holds. The world is the {@link CityFixtures} one with every option at its default.
 */
public class NewWorldGoldenTest {

    private static final int SEARCH = 48;
    private static final ProfileValues VALUES = ProfileValues.defaults();
    private static final LayoutCityPlan PLAN = plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final Networks NETWORKS = PLANNER.networks();

    private static LayoutCityPlan plan() {
        TerrainSettings settings = TerrainSettings.fromValues(VALUES.asMap());
        WorldStyle worldStyle = CityFixtures.ASSETS.worldStyle(CityProfiles.worldStyle(VALUES));
        BiomeSource biomes = CityFixtures.plains();
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    CityFixtures.SEED,
                    CityProfiles.layoutProfile(settings, VALUES),
                    new LayoutAssets(CityFixtures.ASSETS, worldStyle, biomes),
                    CityFixtures.terrain())
                .networks(CityProfiles.networkSettings(VALUES, ModuleState.allActive(), PlanningGate.NONE))
                .build());
        CityWriteContext context = new CityWriteContext(
            CityFixtures.SEED,
            planner,
            CityFixtures.ASSETS,
            worldStyle,
            new CityPalettes(CityFixtures.ASSETS, MetalFlags.ALL_ON),
            settings,
            biomes,
            new NameBlockClassifier(),
            LayoutLog.SILENT);
        return new LayoutCityPlan(context, StructureOracle.NONE, CityFixtures.terrain());
    }

    /** The first chunk, ring by ring from the origin, with a piece of a shape that is not a tunnel. */
    private static int[] piece(HighwayInfo.Shape shape) {
        for (int ring = 0; ring <= SEARCH; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    for (HighwayInfo.Piece p : NETWORKS.highway(dx, dz)
                        .pieces(dx, dz)) {
                        int natural = CityFixtures.terrain()
                            .height(dx, dz);
                        if (p.shape() == shape && !PLANNER.plan(dx, dz)
                            .isTunnel(p.topLevel(), natural)) {
                            return new int[] { dx, dz };
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void golden(String name, int[] at) throws IOException {
        assertNotNull(at, name + " near the origin");
        GoldenChunks.assertMatches(name, CityFixtures.generate(PLAN, at[0], at[1]));
    }

    @Test
    public void goldenNewWorldBuildings() throws IOException {
        int[] building = CityFixtures.first(SEARCH, (x, z) -> {
            ChunkPlan plan = PLANNER.plan(x, z);
            return plan.hasBuilding() && plan.floors() >= 3 && plan.cellars() >= 1 && plan.ruinHeight() < 0;
        });
        assertEquals(
            8,
            PLANNER.plan(building[0], building[1])
                .floorPitch());
        golden("newworld_city_building", building);
        golden(
            "newworld_pack_house",
            CityFixtures.first(
                SEARCH,
                (x, z) -> PLANNER.plan(x, z)
                    .hasBuilding()
                    && String.valueOf(
                        PLANNER.plan(x, z)
                            .building())
                        .startsWith("nh_")));
    }

    @Test
    public void goldenNewWorldLots() throws IOException {
        golden(
            "newworld_lot_foundation",
            CityFixtures.first(
                SEARCH,
                (x, z) -> PLANNER.plan(x, z)
                    .lot() == LotKind.FOUNDATION));
        golden(
            "newworld_lot_plaza",
            CityFixtures.first(
                SEARCH,
                (x, z) -> PLANNER.plan(x, z)
                    .lot() == LotKind.PLAZA));
    }

    @Test
    public void goldenNewWorldNetworks() throws IOException {
        golden("newworld_network_ramp", piece(HighwayInfo.Shape.RAMP));
        golden("newworld_network_highway", piece(HighwayInfo.Shape.STRAIGHT));
        golden(
            "newworld_network_rail",
            CityFixtures.first(
                40,
                (x, z) -> NETWORKS.railChunk(x, z)
                    .type() == RailChunkType.HORIZONTAL));
    }
}
