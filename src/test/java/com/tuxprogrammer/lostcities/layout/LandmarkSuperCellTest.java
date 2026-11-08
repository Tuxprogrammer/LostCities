package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkRaster;

/**
 * Landmark queries per super-cell (D-perf2-route-1) with detours of the blocked stretch only (D-perf2-route-2): a
 * 600-chunk route asks the oracle a bounded number of times, far fewer than the per-cell queries of the old waypoint
 * detours; routed paths never cross a blocked chunk; the zone answer is unchanged.
 */
class LandmarkSuperCellTest {

    /** Counts oracle calls. */
    private static final class CountingOracle implements StructureOracle {

        private final StructureOracle inner;
        int calls;

        CountingOracle(Landmark... landmarks) {
            inner = new LayoutFixtures.FakeOracle(landmarks);
        }

        @Override
        public List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
            calls++;
            return inner.landmarksTouching(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        }
    }

    /** Villages straddling the straight line every 110 chunks, so the route detours four times. */
    private static Landmark[] villages() {
        List<Landmark> out = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            int x = (100 + i * 110) * 16;
            out.add(LayoutFixtures.landmark("village@" + i, LandmarkKind.VILLAGE, x, 120 * 16, x + 40, 150 * 16));
        }
        return out.toArray(new Landmark[0]);
    }

    /** Two cities 600 chunks apart in 256-chunk cells. */
    private static final class TwoCities implements CityLinkGraph.Nodes {

        private final LandmarkZones zones;

        TwoCities(LandmarkZones zones) {
            this.zones = zones;
        }

        @Override
        public int cell() {
            return 256;
        }

        @Override
        public int[] cityCenter(int cellX, int cellZ) {
            if (cellZ != 0) {
                return null;
            }
            return cellX == 0 ? new int[] { 100, 140 } : cellX == 2 ? new int[] { 700, 140 } : null;
        }

        @Override
        public boolean blocked(int chunkX, int chunkZ) {
            return zones.blocksLinks(chunkX, chunkZ);
        }
    }

    private static boolean blockedByZone(LandmarkZones zones, int chunkX, int chunkZ) {
        Zone zone = zones.zone(chunkX, chunkZ)
            .zone();
        return zone == Zone.CORE || zone == Zone.EDGE_RING || zone == Zone.AVOID_RING;
    }

    /** Waypoint offsets and positions of the old detours (D390), for the before count. */
    private static final int[] OLD_OFFSETS = { 12, 24, 36, 48, 64 };
    private static final double[] OLD_ALONG = { 0.5, 0.3, 0.7 };

    /**
     * Returns how many oracle queries the old route made between two ends: one per distinct 8 × 8-chunk cell its
     * blocked tests touched (the old link mask asked the oracle once per cell), the straight raster then up to 30
     * waypoint detours, each tested up to its first blocked chunk.
     */
    private static int oldQueries(LandmarkZones zones, int ax, int az, int bx, int bz, int maxDistance) {
        Set<Long> cells = new HashSet<>();
        List<List<LinkRaster.Step>> candidates = new ArrayList<>();
        candidates.add(LinkRaster.rasterize(ax, az, bx, bz));
        double dx = bx - ax;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dz * dz);
        double nx = -dz / length;
        double nz = dx / length;
        long max = (long) maxDistance * maxDistance;
        for (int offset : OLD_OFFSETS) {
            for (double along : OLD_ALONG) {
                for (int side = 1; side >= -1; side -= 2) {
                    int wx = (int) Math.round(ax + along * dx + side * offset * nx);
                    int wz = (int) Math.round(az + along * dz + side * offset * nz);
                    long da = (long) (wx - ax) * (wx - ax) + (long) (wz - az) * (wz - az);
                    long db = (long) (wx - bx) * (wx - bx) + (long) (wz - bz) * (wz - bz);
                    if (da <= max && db <= max) {
                        List<LinkRaster.Step> path = LinkRaster.rasterizeVia(ax, az, wx, wz, bx, bz);
                        if (path != null) {
                            candidates.add(path);
                        }
                    }
                }
            }
        }
        for (List<LinkRaster.Step> path : candidates) {
            boolean clear = true;
            for (LinkRaster.Step step : path) {
                cells.add((long) Math.floorDiv(step.chunkX(), 8) << 32 | Math.floorDiv(step.chunkZ(), 8) & 0xffffffffL);
                if (blockedByZone(zones, step.chunkX(), step.chunkZ())) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                break;
            }
        }
        return cells.size();
    }

    @Test
    public void aLongRouteAsksTheOracleOncePerSuperCell() {
        LayoutProfile profile = LayoutProfile.defaults();
        CountingOracle oracle = new CountingOracle(villages());
        LandmarkZones zones = new LandmarkZones(oracle, profile, 2048);
        CityLinkGraph graph = new CityLinkGraph(new TwoCities(zones), 64);
        List<CityLinkGraph.Link> links = graph.links(0, 0);
        assertEquals(1, links.size());
        CityLinkGraph.Link link = links.get(0);
        assertTrue(
            link.path()
                .size() >= 590);
        assertTrue(
            !link.path()
                .equals(LinkRaster.rasterize(link.ax(), link.az(), link.bx(), link.bz())),
            "the villages force a detour");
        LandmarkZones reference = new LandmarkZones(new LayoutFixtures.FakeOracle(villages()), profile, 2048);
        for (LinkRaster.Step step : link.path()) {
            assertTrue(!blockedByZone(reference, step.chunkX(), step.chunkZ()), "crosses a village at " + step);
        }
        int before = oldQueries(
            new LandmarkZones(new LayoutFixtures.FakeOracle(villages()), profile, 2048),
            link.ax(),
            link.az(),
            link.bx(),
            link.bz(),
            graph.maxDistance());
        int after = oracle.calls;
        System.out.println(
            "D-perf2-route-1 one " + link.path()
                .size() + "-chunk route: oracle queries before " + before + ", after " + after);
        // A 600-chunk link crosses about 38 columns of 16-chunk super-cells; the raster and its detours touch one or
        // two rows of each, far fewer than the whole 129-chunk corridor (D-perf2-route-3).
        assertTrue(after <= 100, "bounded: " + after);
        assertTrue(after * 5 <= before, "far fewer than before: " + after + " vs " + before);
    }

    /** Random villages and avoided landmarks: every routed path is clear by the zone answer, in any query order. */
    @Test
    public void routesNeverCrossABlockerAndDoNotDependOnOrder() {
        LayoutProfile profile = LayoutProfile.defaults();
        for (long seed = 1; seed <= 6; seed++) {
            java.util.Random random = new java.util.Random(seed);
            List<Landmark> list = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                int x = random.nextInt(800 * 16) - 50 * 16;
                int z = random.nextInt(300 * 16) - 10 * 16;
                int w = 16 + random.nextInt(30 * 16);
                int h = 16 + random.nextInt(30 * 16);
                LandmarkKind kind = i % 3 == 0 ? LandmarkKind.ROGUELIKE : LandmarkKind.VILLAGE;
                list.add(LayoutFixtures.landmark("l" + i, kind, x, z, x + w, z + h));
            }
            Landmark[] landmarks = list.toArray(new Landmark[0]);
            LandmarkZones reference = new LandmarkZones(new LayoutFixtures.FakeOracle(landmarks), profile, 2048);
            LandmarkZones a = new LandmarkZones(new LayoutFixtures.FakeOracle(landmarks), profile, 2048);
            LandmarkZones b = new LandmarkZones(new LayoutFixtures.FakeOracle(landmarks), profile, 2048);
            // Warm b's caches far from a's first query, so the two fill their super-cells in different orders.
            for (int x = 700; x >= -60; x -= 37) {
                for (int z = 300; z >= -20; z -= 29) {
                    b.blocksLinks(x, z);
                    b.zone(x, z);
                }
            }
            CityLinkGraph ga = new CityLinkGraph(new TwoCities(a), 64);
            CityLinkGraph gb = new CityLinkGraph(new TwoCities(b), 64);
            List<CityLinkGraph.Link> la = ga.links(0, 0);
            List<CityLinkGraph.Link> lb = gb.links(2, 0);
            assertEquals(la, lb, "seed " + seed);
            for (int i = 0; i < la.size(); i++) {
                assertEquals(
                    la.get(i)
                        .path()
                        .toString(),
                    lb.get(i)
                        .path()
                        .toString());
            }
            for (CityLinkGraph.Link link : la) {
                boolean straight = link.path()
                    .equals(LinkRaster.rasterize(link.ax(), link.az(), link.bx(), link.bz()));
                for (LinkRaster.Step step : link.path()) {
                    boolean hit = blockedByZone(reference, step.chunkX(), step.chunkZ());
                    // The fallback link of a node whose every path is blocked is the straight one (chosen()).
                    assertTrue(straight || !hit, "seed " + seed + " crosses a landmark at " + step);
                }
            }
        }
    }
}
