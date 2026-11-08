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
import com.tuxprogrammer.lostcities.layout.LayoutFixtures;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.layout.NetworkSettings;
import com.tuxprogrammer.lostcities.layout.Networks;
import com.tuxprogrammer.lostcities.layout.highway.Axis;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkHighways;
import com.tuxprogrammer.lostcities.layout.link.LinkRaster;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.GoldenChunks;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Writing highways and railways (highway-parts.md, railway-parts.md; design record 0013): the part choices, ramps,
 * the bugs upstream's writers had, and golden chunks of a highway, a ramp, a railway line and a station in the
 * {@link CityFixtures} world with its networks on.
 */
public class NetworkWriterTest {

    private static final List<String> LOG = new ArrayList<>();
    private static final LayoutCityPlan PLAN = networkPlan();

    /**
     * Budget B5 with every cache a world keeps, as the overlay counts them: the plan cache, the two height caches, the
     * layout, palette and style caches, the scattered caches and the networks' caches, with a little room to spare.
     */
    @Test
    public void everyCacheTogetherStaysWithinBudgetB5() {
        long scattered = 2L * LayoutCityPlan.SCATTERED_CACHE_SIZE * LayoutCityPlan.SCATTERED_PLAN_BYTES
            + (long) ScatteredContent.FIT_CACHE * LayoutCityPlan.GROUND_FIT_BYTES;
        long planCache = (long) com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan.CAPACITY
            * (com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan.ESTIMATED_PLAN_BYTES + 16);
        long heights = com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights.MAX_BYTES + Math.max(
            com.tuxprogrammer.lostcities.worldgen.height.ChunkColumnCache.MAX_BYTES,
            com.tuxprogrammer.lostcities.worldgen.height.ColumnHeightCache.MAX_BYTES);
        long bytes = PLAN.estimatedCacheBytes() + scattered + planCache + heights;
        assertTrue(bytes <= 16L * 1024 * 1024 - 64 * 1024, "estimated " + bytes + " bytes");
    }

    /**
     * The structure oracles' caches (D-oracle-caches-1) and LandmarkZones (4096 cells, 65536 zones, lane
     * layout-footprint-caches) sit on top of {@link #everyCacheTogetherStaysWithinBudgetB5}. Measured 2026-11-07: the
     * base is 16.6 MB (it double-counts the scattered caches), the oracles 9.8 MB and the zones 8.2 MB, 34.6 MB in all,
     * over B5's 16 MB. The integrator decides whether to raise B5 or shrink caches (D-oracle-caches-3); this test pins
     * each new share so a growth is noticed.
     */
    @Test
    public void theOracleAndZoneSharesStayWithinTheirCeilings() {
        long landmarkZones = 65536L * 112 + 4096L * 200;
        assertTrue(landmarkZones <= 9L * 1024 * 1024, "zones " + landmarkZones);
        assertTrue(
            com.tuxprogrammer.lostcities.compat.OracleCacheBytes.TOTAL <= 10L * 1024 * 1024,
            "oracles " + com.tuxprogrammer.lostcities.compat.OracleCacheBytes.TOTAL);
    }

    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final Networks NETWORKS = PLANNER.networks();
    private static final int SEARCH = 160;

    /** The {@link CityFixtures} world with its highways and railways. */
    static LayoutCityPlan networkPlan() {
        return networkPlan(TerrainSettings.defaults());
    }

    /** The network fixture's plan with the given terrain settings. */
    static LayoutCityPlan networkPlan(TerrainSettings settings) {
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
    public void theGuardedBandSpansEveryTunnelAndRailPartAndThePlanCarriesIt() {
        int[] at = rail(RailChunkType.HORIZONTAL);
        assertNotNull(at);
        int natural = CityFixtures.terrain()
            .height(at[0], at[1]);
        RailChunk rail = NETWORKS.railChunk(at[0], at[1]);
        int[] band = NetworkWriter.guardBand(PLAN.context(), at[0], at[1], natural);
        assertNotNull(band);
        int floor = PLAN.context()
            .settings()
            .cityGround(rail.level());
        assertTrue(band[0] <= floor - 1, "from under the rail's floor");
        assertTrue(band[1] >= floor + NetworkWriter.PART_LEVEL_HEIGHT - 1, "up through the part");
        com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = PLAN.chunk(at[0], at[1]);
        assertTrue(plan.inGuardBand(floor));
        assertFalse(plan.inGuardBand(band[1] + 1));
        // A chunk with no network has no band.
        int[] none = CityFixtures.first(40, (x, z) -> !NetworkWriter.hasWork(PLANNER, x, z));
        assertNotNull(none);
        assertNull(NetworkWriter.guardBand(PLAN.context(), none[0], none[1], 70));
        assertFalse(
            PLAN.chunk(none[0], none[1])
                .inGuardBand(floor));
    }

    /** M12's edge lots and M06's networks: no lot on a highway or railway chunk, so no highway runs through a pit. */
    @Test
    public void noEdgeLotOnAHighwayOrRailwayChunk() {
        int networkChunks = 0;
        for (int z = -64; z <= 64; z++) {
            for (int x = -64; x <= 64; x++) {
                boolean network = NETWORKS.highway(x, z)
                    .hasHighway()
                    || NETWORKS.railChunk(x, z)
                        .isPresent();
                if (!network) {
                    continue;
                }
                networkChunks++;
                assertEquals(
                    com.tuxprogrammer.lostcities.layout.LotKind.NONE,
                    PLANNER.plan(x, z)
                        .lot(),
                    "lot on network chunk " + x + "," + z);
            }
        }
        assertTrue(networkChunks > 0);
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

    /**
     * KI-2: with {@code rampSurface} a ramp's whole-step columns are topped with the smooth double slab, so the ramp
     * is one surface with its half-step slabs; without it (worlds made before) the deck is the road's, as before.
     */
    @Test
    public void withRampSurfaceAWholeStepColumnMatchesTheSlabs() {
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
        LayoutCityPlan on = networkPlan(
            TerrainSettings.fromValues(java.util.Collections.singletonMap("rampSurface", "true")));
        ArrayChunkBuffer surfaced = CityFixtures.generate(on, at[0], at[1]);
        ArrayChunkBuffer plain = CityFixtures.generate(PLAN, at[0], at[1]);
        int base = 71 + 6 * piece.level();
        int surfacedColumns = 0;
        for (int along = 0; along < 16; along++) {
            int x = piece.axis() == Axis.X ? along : 12;
            int z = piece.axis() == Axis.X ? 3 : along;
            int deck = base + steps[along] / 2;
            if ((steps[along] & 1) == 1) {
                assertEquals(NetworkWriter.RAMP_SLAB, surfaced.get(x, deck + 1, z), "the half slab stays at " + along);
            } else if (steps[along] > 0) {
                assertEquals(NetworkWriter.RAMP_SURFACE, surfaced.get(x, deck, z), "surfaced at " + along);
                assertFalse(NetworkWriter.RAMP_SURFACE.equals(plain.get(x, deck, z)), "legacy deck at " + along);
                surfacedColumns++;
            }
        }
        assertTrue(surfacedColumns > 0, "a whole-step column on the ramp");
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
        // A building whose lowest floor (level 1 - 4 cellars = -3) reaches below the rail part's top (-3 + 1).
        assertTrue(NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, true, 1, 4, -3, 1));
        assertFalse(
            NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, true, 1, 3, -3, 1),
            "a lowest floor at the part's top leaves it");
        assertFalse(
            NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.IGNORE, true, 1, 3, -3, 1),
            "the bundled world style ignores it");
        assertFalse(NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, false, 1, 3, -3, 1));
        // The layout's answer for the chunk, which its neighbors read, never changes.
        int[] at = rail(RailChunkType.HORIZONTAL);
        RailChunk first = NETWORKS.railChunk(at[0], at[1]);
        NetworkWriter.blockedByBuilding(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, true, 1, 3, -3, 1);
        assertEquals(
            first.toString(),
            NETWORKS.railChunk(at[0], at[1])
                .toString());
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

    /**
     * A diagonal link deck spills into the side chunks at its corners (0015 §3); those chunks put the spilled levels in
     * their footprint, or in their guard band where the deck runs underground, so RWG's trees and other mods'
     * decoration cannot grow up through it.
     */
    @Test
    public void spillFromALinkDiagonalIsInTheFootprintOrGuardBand() {
        LayoutPlanner planner = new LayoutPlanner(
            LayoutInputs.builder(
                7919L,
                LayoutProfile.builder()
                    .cityChance(0.00025)
                    .cityRadius(100, 256)
                    .cityGrid(64, 16)
                    .build(),
                LayoutFixtures.standardAssets(),
                LayoutFixtures.flatTerrain())
                .networks(NetworkSettings.DEFAULTS.withLinks(true))
                .build());
        TerrainSettings settings = TerrainSettings.defaults();
        CityWriteContext context = new CityWriteContext(
            7919L,
            planner,
            CityFixtures.ASSETS,
            CityFixtures.ASSETS.worldStyle(CityProfiles.WORLD_STYLE),
            new CityPalettes(CityFixtures.ASSETS, MetalFlags.ALL_ON),
            settings,
            CityFixtures.plains(),
            new NameBlockClassifier(),
            LOG::add);
        Networks networks = planner.networks();
        LinkHighways view = networks.linkHighways();
        int checked = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                for (CityLinkGraph.Link link : networks.links()
                    .links(cx, cz)) {
                    LinkHighways.Profile p = view.profile(link);
                    for (int i = p.start(); p.written() && i <= p.end(); i++) {
                        LinkRaster.Step s = link.path()
                            .get(i);
                        for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                            int x = s.chunkX() + d[0];
                            int z = s.chunkZ() + d[1];
                            HighwayInfo info = networks.highway(x, z);
                            if (!info.spill()) {
                                continue;
                            }
                            checked++;
                            int low = settings.cityGround(info.spillLow());
                            int bottom = NetworkWriter.footprintBottom(context, x, z, 64);
                            int[] band = NetworkWriter.guardBand(context, x, z, 64);
                            boolean guarded = band != null && band[0] < low
                                && band[1] >= settings.cityGround(info.spillHigh());
                            assertTrue(bottom <= low || guarded, "spill into " + x + "," + z + " from " + link);
                        }
                    }
                }
            }
        }
        assertTrue(checked > 0, "some diagonal spills near the origin");
    }

    private static void golden(String name, int[] at) throws IOException {
        assertNotNull(at, name + " near the origin");
        GoldenChunks.assertMatches(name, CityFixtures.generate(PLAN, at[0], at[1]));
    }
}
