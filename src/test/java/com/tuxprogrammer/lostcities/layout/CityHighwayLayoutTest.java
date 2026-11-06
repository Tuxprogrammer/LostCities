package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.link.CityHighways;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkHighways;
import com.tuxprogrammer.lostcities.layout.link.LinkRaster;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Design record 0017 through the real planner in {@code networkLayout=links}: beltways and cut-throughs are
 * deterministic, a ring encloses at least 8 x 8 chunks with city outside it, no building stands on a ring, every
 * interchange and every cut-through end meets its link at the city highway's level, cities without the room never get
 * a beltway, and every highway chunk lies on a city-to-city link or a city's own highway.
 */
public class CityHighwayLayoutTest {

    private static final int CELLS = 3;

    /** The city grid's cell edge in chunks, as {@link #planner} sets it. */
    private static final int CELL = 64;

    /** A landmark column no test chunk reaches. */
    private static final int NO_LANDMARK = Integer.MIN_VALUE;

    private static LayoutPlanner planner(long seed, CityHighwayLayout layout) {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(100, 256)
            .cityGrid(CELL, 16)
            .densityRings(1.5f)
            .cityHighwayLayout(layout)
            .build();
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, LayoutFixtures.standardAssets(), LayoutFixtures.flatTerrain())
                .networks(NetworkSettings.DEFAULTS.withLinks(true))
                .build());
    }

    /** Every node's plan in the window's cells. */
    private static Map<Long, CityHighways.Plan> plans(LayoutPlanner planner) {
        Networks networks = planner.networks();
        CityHighways cities = networks.linkHighways()
            .cities();
        Map<Long, CityHighways.Plan> out = new HashMap<>();
        for (int cx = -CELLS; cx <= CELLS; cx++) {
            for (int cz = -CELLS; cz <= CELLS; cz++) {
                int[] node = networks.links()
                    .node(cx, cz);
                if (node != null) {
                    out.put(CityHighways.key(node[0], node[1]), cities.plan(node[0], node[1]));
                }
            }
        }
        return out;
    }

    @Test
    public void theOptionIsPinnedToNoneInOldWorldsAndParsed() {
        assertEquals(
            "none",
            TerrainSettings.defaults()
                .cityHighwayLayout());
        Map<String, String> values = new HashMap<>();
        values.put("cityHighwayLayout", "Beltway");
        assertEquals(
            "beltway",
            TerrainSettings.fromValues(values)
                .cityHighwayLayout());
        values.put("cityHighwayLayout", "ring");
        assertEquals(
            "none",
            TerrainSettings.fromValues(values)
                .cityHighwayLayout());
        assertEquals(CityHighwayLayout.CUTTHROUGH, CityHighwayLayout.parse("cutthrough"));
        assertEquals(
            CityHighwayLayout.NONE,
            LayoutProfile.builder()
                .build().cityHighwayLayout);
    }

    @Test
    public void plansAreDeterministicAndRandomGivesBoth() {
        Map<Long, CityHighways.Plan> a = plans(planner(7919L, CityHighwayLayout.RANDOM));
        LayoutPlanner other = planner(7919L, CityHighwayLayout.RANDOM);
        // Read the second planner's chunks in a different order first.
        other.networks()
            .highway(40, -40);
        Map<Long, CityHighways.Plan> b = plans(other);
        assertEquals(a.keySet(), b.keySet());
        int belts = 0;
        int cuts = 0;
        for (Map.Entry<Long, CityHighways.Plan> e : a.entrySet()) {
            CityHighways.Plan p = e.getValue();
            CityHighways.Plan q = b.get(e.getKey());
            assertEquals(p.kind(), q.kind());
            assertEquals(p.level(), q.level());
            assertEquals(p.pieces(), q.pieces());
            assertEquals(p.joined(), q.joined());
            belts += p.kind() == CityHighways.Kind.BELTWAY ? 1 : 0;
            cuts += p.kind() == CityHighways.Kind.CUTTHROUGH ? 1 : 0;
        }
        assertTrue(belts > 0 && cuts > 0, "random gives beltways (" + belts + ") and cut-throughs (" + cuts + ")");
    }

    @Test
    public void aRingEnclosesEightByEightWithCityOutsideAndNoBuildingOnIt() {
        LayoutPlanner planner = planner(7919L, CityHighwayLayout.BELTWAY);
        int belts = 0;
        for (CityHighways.Plan p : plans(planner).values()) {
            if (p.kind() != CityHighways.Kind.BELTWAY) {
                continue;
            }
            belts++;
            assertTrue(2 * p.half() - 1 >= 8, "inner block at least 8x8");
            float extent = planner.cityRadius(p.centerX(), p.centerZ()) * (1f - planner.profile().cityThreshold) / 16f;
            assertTrue(p.half() * Math.sqrt(2) + CityHighways.MARGIN <= extent, "city outside the ring's corners");
            assertEquals(
                8 * p.half(),
                p.pieces()
                    .size(),
                "a closed square ring");
            for (long key : p.pieces()
                .keySet()) {
                int x = (int) (key >> 32);
                int z = (int) key;
                ChunkPlan plan = planner.plan(x, z);
                assertFalse(plan.hasBuilding(), "no building on ring chunk " + x + "," + z);
                assertTrue(planner.hasHighway(x, z), "the ring is a highway at " + x + "," + z);
            }
            // Towers stay inside: next to the ring and outside it the density ring is at least the outer one.
            int x = p.centerX() + p.half() + 1;
            if (planner.characteristics(x, p.centerZ()).isCity) {
                assertTrue(planner.ringDistance(x, p.centerZ()) >= DensityRings.INNER_END);
            }
        }
        assertTrue(belts > 0, "the window has a beltway");
    }

    @Test
    public void everyInterchangeAndCutThroughEndMeetsItsLink() {
        for (CityHighwayLayout layout : new CityHighwayLayout[] { CityHighwayLayout.BELTWAY,
            CityHighwayLayout.CUTTHROUGH }) {
            int joins = 0;
            for (long seed : SEEDS) {
                joins += meetsItsLinks(seed, layout);
            }
            assertTrue(joins > 0, layout + " joins links");
        }
    }

    /** Seeds for the checks that walk every interchange, so short links between close cities turn up. */
    private static final long[] SEEDS = { 31337L, 7919L, 1L, 4242L, 65537L };

    /** Checks every joined link of one world against its city highway; returns how many met it. */
    private static int meetsItsLinks(long seed, CityHighwayLayout layout) {
        LayoutPlanner planner = planner(seed, layout);
        LinkHighways view = planner.networks()
            .linkHighways();
        int joins = 0;
        for (CityHighways.Plan p : plans(planner).values()) {
            if (p.kind() == CityHighways.Kind.CUTTHROUGH) {
                assertEquals(
                    2,
                    p.joined()
                        .size(),
                    "a cut-through joins two links");
            }
            for (CityLinkGraph.Link link : p.joined()) {
                LinkHighways.Profile profile = view.profile(link);
                if (!profile.written()) {
                    continue;
                }
                boolean atA = link.ax() == p.centerX() && link.az() == p.centerZ();
                int index = atA ? profile.start() : profile.end();
                int x = link.path()
                    .get(index)
                    .chunkX();
                int z = link.path()
                    .get(index)
                    .chunkZ();
                assertNotNull(
                    p.pieces()
                        .get(CityHighways.key(x, z)),
                    "the link ends on the city highway at " + x + "," + z);
                assertEquals(p.level(), atA ? profile.inLevel(index) : profile.outLevel(index));
                boolean deck = false;
                for (HighwayInfo.Piece piece : view.pieces(x, z)) {
                    deck |= piece.level() == p.level() && piece.topLevel() == p.level();
                }
                assertTrue(deck, "one deck at the interchange " + x + "," + z);
                joins++;
            }
            if (p.kind() == CityHighways.Kind.BELTWAY) {
                // A link the ring does not join (no ramp room, D348) stops at the city's edge, not on the ring.
                for (CityLinkGraph.Link link : planner.networks()
                    .links()
                    .links(Math.floorDiv(p.centerX(), CELL), Math.floorDiv(p.centerZ(), CELL))) {
                    LinkHighways.Profile profile = view.profile(link);
                    if (p.joins(link) || !profile.written()) {
                        continue;
                    }
                    boolean atA = link.ax() == p.centerX() && link.az() == p.centerZ();
                    LinkRaster.Step step = link.path()
                        .get(atA ? profile.start() : profile.end());
                    assertNull(
                        p.pieces()
                            .get(CityHighways.key(step.chunkX(), step.chunkZ())),
                        "an unjoined link ends off the ring");
                }
            }
        }
        return joins;
    }

    @Test
    public void anInterchangeHasNoOpenEdge() {
        int checked = 0;
        for (long seed : SEEDS) {
            for (CityHighwayLayout layout : new CityHighwayLayout[] { CityHighwayLayout.BELTWAY,
                CityHighwayLayout.CUTTHROUGH }) {
                LayoutPlanner planner = planner(seed, layout);
                LinkHighways view = planner.networks()
                    .linkHighways();
                for (CityHighways.Plan p : plans(planner).values()) {
                    for (long key : p.pieces()
                        .keySet()) {
                        int x = (int) (key >> 32);
                        int z = (int) key;
                        for (HighwayInfo.Piece piece : view.pieces(x, z)) {
                            if (piece.level() != p.level() || piece.topLevel() != p.level()) {
                                continue;
                            }
                            int[] o = piece.openings();
                            // An elevated plaza opens on every side whatever meets it: the open edge this guards.
                            assertTrue(
                                piece.shape() != HighwayInfo.Shape.CROSSING || o.length == 12,
                                "no elevated plaza at " + x + "," + z + " (seed " + seed + ")");
                            for (int i = 0; i + 2 < o.length; i += 3) {
                                if (o[i] != 0 && o[i + 1] != 0) {
                                    continue;
                                }
                                assertTrue(
                                    opensBack(view.pieces(x + o[i], z + o[i + 1]), -o[i], -o[i + 1], o[i + 2]),
                                    "the deck at " + x
                                        + ","
                                        + z
                                        + " opens toward "
                                        + o[i]
                                        + ","
                                        + o[i + 1]
                                        + " onto a deck that opens back (seed "
                                        + seed
                                        + ")");
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked > 0);
    }

    private static boolean opensBack(List<HighwayInfo.Piece> pieces, int dx, int dz, int level) {
        for (HighwayInfo.Piece piece : pieces) {
            int[] o = piece.openings();
            for (int i = 0; i + 2 < o.length; i += 3) {
                if (o[i] == dx && o[i + 1] == dz && o[i + 2] == level) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void citiesWithoutTheRoomNeverGetABeltway() {
        CityHighways.Cities full = cities(256, NO_LANDMARK);
        assertEquals(
            CityHighways.Kind.BELTWAY,
            new CityHighways(1L, CityHighwayLayout.BELTWAY, null, full).plan(0, 0)
                .kind());
        // Small: the ring's corners would leave no city outside.
        assertNotEquals(
            CityHighways.Kind.BELTWAY,
            new CityHighways(1L, CityHighwayLayout.BELTWAY, null, cities(150, NO_LANDMARK)).plan(0, 0)
                .kind());
        // Tapered around a landmark (a village core five chunks off the center).
        assertNotEquals(
            CityHighways.Kind.BELTWAY,
            new CityHighways(1L, CityHighwayLayout.BELTWAY, null, cities(256, 5)).plan(0, 0)
                .kind());
        LayoutPlanner planner = planner(7919L, CityHighwayLayout.RANDOM);
        for (CityHighways.Plan p : plans(planner).values()) {
            if (!p.beltwayFits()) {
                assertNotEquals(CityHighways.Kind.BELTWAY, p.kind());
            }
        }
    }

    @Test
    public void everyHighwayChunkLiesOnALinkOrACityHighway() {
        LayoutPlanner planner = planner(7919L, CityHighwayLayout.RANDOM);
        Networks networks = planner.networks();
        LinkHighways view = networks.linkHighways();
        int city = 0;
        for (int x = -192; x <= 192; x++) {
            for (int z = -192; z <= 192; z++) {
                if (view.pieces(x, z)
                    .isEmpty()) {
                    continue;
                }
                boolean on = !view.cities()
                    .pieces(x, z)
                    .isEmpty();
                city += on ? 1 : 0;
                for (int dx = -1; dx <= 1 && !on; dx++) {
                    for (int dz = -1; dz <= 1 && !on; dz++) {
                        for (CityLinkGraph.Hit hit : networks.links()
                            .hits(x + dx, z + dz)) {
                            on |= hit.link()
                                .bothEndsCities()
                                && view.profile(hit.link())
                                    .written();
                        }
                    }
                }
                assertTrue(on, "highway chunk " + x + "," + z + " lies on a link or a city highway");
            }
        }
        assertTrue(city > 0);
    }

    /** One city at the origin, all city and flat, with an optional landmark at (landmarkX, 0). */
    private static CityHighways.Cities cities(int radius, int landmarkX) {
        return new CityHighways.Cities() {

            @Override
            public int cell() {
                return 64;
            }

            @Override
            public int[] center(int cellX, int cellZ) {
                return cellX == 0 && cellZ == 0 ? new int[] { 0, 0 } : null;
            }

            @Override
            public int radius(int centerX, int centerZ) {
                return radius;
            }

            @Override
            public int maxRadius() {
                return 256;
            }

            @Override
            public float threshold() {
                return 0.2f;
            }

            @Override
            public boolean city(int chunkX, int chunkZ) {
                return true;
            }

            @Override
            public boolean landmark(int chunkX, int chunkZ) {
                return chunkX == landmarkX && chunkZ == 0;
            }

            @Override
            public boolean blocked(int chunkX, int chunkZ) {
                return landmark(chunkX, chunkZ);
            }

            @Override
            public int level(int chunkX, int chunkZ) {
                return 2;
            }

            @Override
            public int maxLevel() {
                return 8;
            }
        };
    }

    /** The ring list helper's size, a sanity check of the closed square. */
    @Test
    public void aRingHasEightTimesHalfChunks() {
        CityHighways.Plan p = new CityHighways(1L, CityHighwayLayout.BELTWAY, null, cities(256, NO_LANDMARK))
            .plan(0, 0);
        assertEquals(
            8 * p.half(),
            p.pieces()
                .size());
        assertEquals(3, p.level(), "one level above the highest city level under it");
        List<HighwayInfo.Piece> corner = new CityHighways(1L, CityHighwayLayout.BELTWAY, null, cities(256, NO_LANDMARK))
            .pieces(p.half(), p.half());
        assertEquals(1, corner.size());
    }
}
