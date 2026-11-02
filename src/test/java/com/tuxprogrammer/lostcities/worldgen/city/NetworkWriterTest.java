package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.view.LayoutAssets;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.NetworkSettings;
import com.tuxprogrammer.lostcities.layout.Networks;
import com.tuxprogrammer.lostcities.layout.highway.Axis;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.GoldenChunks;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Writing highways and railways (highway-parts.md, railway-parts.md; design record 0007): the part choices, ramps,
 * the bugs upstream's writers had, and golden chunks of a highway, a ramp, a railway line and a station in the
 * {@link CityFixtures} world with its networks on.
 */
public class NetworkWriterTest {

    private static final List<String> LOG = new ArrayList<>();
    private static final LayoutCityPlan PLAN = networkPlan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final Networks NETWORKS = PLANNER.networks();
    private static final int SEARCH = 160;

    /** The {@link CityFixtures} world with its highways and railways. */
    static LayoutCityPlan networkPlan() {
        TerrainSettings settings = TerrainSettings.defaults();
        WorldStyle worldStyle = CityFixtures.ASSETS.worldStyle(CityProfiles.WORLD_STYLE);
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs
                .builder(
                    CityFixtures.SEED,
                    CityProfiles.layoutProfile(settings),
                    new LayoutAssets(CityFixtures.ASSETS, worldStyle, CityFixtures.plains()),
                    CityFixtures.terrain())
                .networks(NetworkSettings.DEFAULTS)
                .build());
        CityWriteContext context = new CityWriteContext(
            CityFixtures.SEED,
            planner,
            CityFixtures.ASSETS,
            worldStyle,
            new CityPalettes(CityFixtures.ASSETS, MetalFlags.ALL_ON),
            settings,
            CityFixtures.plains(),
            new NameBlockClassifier(),
            LOG::add);
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

    private static int[] rail(RailChunkType type) {
        return CityFixtures.first(
            40,
            (x, z) -> NETWORKS.railChunk(x, z)
                .type() == type);
    }

    @Test
    public void theFixtureWorldHasHighwaysAndRailways() {
        assertNotNull(NETWORKS);
        assertNotNull(piece(HighwayInfo.Shape.STRAIGHT), "a highway near the origin");
        assertNotNull(rail(RailChunkType.HORIZONTAL), "a railway near the origin");
    }

    @Test
    public void aStraightHighwayHasItsDeckAndClearHeadroom() {
        int[] at = piece(HighwayInfo.Shape.STRAIGHT);
        assertNotNull(at);
        ArrayChunkBuffer chunk = CityFixtures.generate(PLAN, at[0], at[1]);
        HighwayInfo.Piece piece = NETWORKS.highway(at[0], at[1])
            .pieces(at[0], at[1])
            .get(0);
        int deck = 71 + 6 * piece.level();
        int x = piece.axis() == Axis.X ? 8 : 12;
        int z = piece.axis() == Axis.X ? 3 : 8;
        assertFalse(
            chunk.get(x, deck, z)
                .isAir(),
            "deck at " + x + "," + deck + "," + z);
        assertTrue(
            chunk.get(x, deck + 1, z)
                .isAir());
        assertTrue(
            chunk.get(x, deck + 2, z)
                .isAir());
    }

    @Test
    public void aRampClimbsHalfABlockAtMostBetweenColumns() {
        int[] at = piece(HighwayInfo.Shape.RAMP);
        assertNotNull(at, "a ramp near the origin");
        HighwayInfo.Piece piece = null;
        for (HighwayInfo.Piece p : NETWORKS.highway(at[0], at[1])
            .pieces(at[0], at[1])) {
            if (p.shape() == HighwayInfo.Shape.RAMP) {
                piece = p;
            }
        }
        int[] steps = NetworkWriter.rampSteps(piece);
        boolean up = piece.highLevel() > piece.lowLevel();
        assertEquals(up ? 0 : NetworkWriter.RAMP_HALF_STEPS, steps[0], "the low end at the low side");
        for (int i = 1; i < 16; i++) {
            assertTrue(Math.abs(steps[i] - steps[i - 1]) <= 1, "a half block at most at " + i);
        }
        ArrayChunkBuffer chunk = CityFixtures.generate(PLAN, at[0], at[1]);
        int base = 71 + 6 * piece.level();
        double previous = Double.NaN;
        for (int along = 0; along < 16; along++) {
            int x = piece.axis() == Axis.X ? along : 12;
            int z = piece.axis() == Axis.X ? 3 : along;
            int deck = base + steps[along] / 2;
            assertFalse(
                chunk.get(x, deck, z)
                    .isAir(),
                "deck at " + along);
            double standing = deck + 1 + ((steps[along] & 1) == 1 ? 0.5 : 0);
            if ((steps[along] & 1) == 1) {
                assertEquals(NetworkWriter.RAMP_SLAB, chunk.get(x, deck + 1, z), "a half slab at " + along);
            }
            if (!Double.isNaN(previous)) {
                assertTrue(Math.abs(standing - previous) <= 0.5, "a step of at most half a block at " + along);
            }
            previous = standing;
        }
    }

    @Test
    public void doesNotReproduceHWY02_aNorthSouthPieceTestsItsWestAndEastNeighbors() {
        HighwayInfo.Piece northSouth = new HighwayInfo.Piece(HighwayInfo.Shape.STRAIGHT, Axis.Z, 0, 0, 1);
        // West is not city, east is: upstream asked the east neighbor twice and called this an open road.
        NetworkWriter.NeighborLevel neighbors = (dx, dz) -> dx < 0 ? -1 : 0;
        assertFalse(NetworkWriter.openRoad(northSouth, 0, true, neighbors));
        NetworkWriter.NeighborLevel bothCity = (dx, dz) -> 0;
        assertTrue(NetworkWriter.openRoad(northSouth, 0, true, bothCity));
        assertFalse(NetworkWriter.openRoad(northSouth, 0, false, bothCity), "never open outside a city");
    }

    @Test
    public void doesNotReproduceHWY12_aSupportOrRailBlockMissingFromThePaletteIsSkippedNotThrown() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/palettes/p.json",
            "{\"palette\": [{\"char\": \"a\", \"block\": \"minecraft:cobblestone\"}]}",
            "lostcities/parts/deck.json",
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"a\"]]}");
        CompiledPalette palette = CompiledPalette.compile(MetalFlags.ALL_ON, set.palette(AssetId.parse("p")));
        Part part = set.part(AssetId.parse("deck"));
        CityWriteContext context = PLAN.context();
        int before = LOG.size();
        assertNull(NetworkWriter.supportBlock(context, palette, 'Q', part));
        assertNull(NetworkWriter.railMainBlock(context, palette, 'Q', "citystyle_test", RailChunkType.HORIZONTAL));
        assertEquals(BlockRef.of("minecraft:cobblestone", 0), NetworkWriter.supportBlock(context, palette, 'a', part));
        assertTrue(LOG.size() >= before + 2, "each missing block reported");
    }

    @Test
    public void doesNotReproduceCITY19_aBuildingBlocksOnlyItsOwnChunksRail() {
        // A building whose lowest floor (level 1 - 3 cellars = -2) reaches below the rail's top (-3 + 1).
        assertTrue(
            NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, true, 1, 3, -3, 1));
        assertFalse(
            NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.IGNORE, true, 1, 3, -3, 1),
            "the bundled world style ignores it");
        assertFalse(
            NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, false, 1, 3, -3, 1));
        // The layout's answer for the chunk, which its neighbors read, never changes.
        int[] at = rail(RailChunkType.HORIZONTAL);
        RailChunk first = NETWORKS.railChunk(at[0], at[1]);
        NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, true, 1, 3, -3, 1);
        assertEquals(first.toString(), NETWORKS.railChunk(at[0], at[1]).toString());
    }

    @Test
    public void aRailwayLineKeepsItsTracksAtItsFloor() {
        int[] at = rail(RailChunkType.HORIZONTAL);
        RailChunk rail = NETWORKS.railChunk(at[0], at[1]);
        ArrayChunkBuffer chunk = CityFixtures.generate(PLAN, at[0], at[1]);
        int floor = 71 + 6 * rail.level();
        int rails = 0;
        for (int z = 0; z < 16; z++) {
            String name = chunk.get(8, floor + 1, z)
                .name();
            if (name.equals("minecraft:rail") || name.equals("minecraft:golden_rail")) {
                rails++;
            }
        }
        assertEquals(rail.tracks(), rails, "one rail per track across the line at x 8");
    }

    @Test
    public void goldenNetworkChunks() throws IOException {
        golden("network_highway", piece(HighwayInfo.Shape.STRAIGHT));
        golden("network_ramp", piece(HighwayInfo.Shape.RAMP));
        golden("network_plaza", piece(HighwayInfo.Shape.CROSSING));
        golden("network_rail", rail(RailChunkType.HORIZONTAL));
        golden("network_split", rail(RailChunkType.THREE_SPLIT));
    }

    private static void golden(String name, int[] at) throws IOException {
        assertNotNull(at, name + " near the origin");
        GoldenChunks.assertMatches(name, CityFixtures.generate(PLAN, at[0], at[1]));
    }
}
