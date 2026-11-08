package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;

/**
 * The link mask (D-linkgraph-shrink-1) blocks exactly the chunks whose zone is {@link Zone#CORE},
 * {@link Zone#EDGE_RING} or {@link Zone#AVOID_RING}, and a long route read from it asks the oracle far less.
 */
class LandmarkLinkMaskTest {

    /** Villages, avoided and ignored landmarks of several sizes, some touching and some across cell edges. */
    private static Landmark[] landmarks() {
        return new Landmark[] { LayoutFixtures.landmark("village@5,5", LandmarkKind.VILLAGE, 84, 84, 91, 91),
            LayoutFixtures.landmark("village@big", LandmarkKind.VILLAGE, -300, 40, -140, 260),
            LayoutFixtures.landmark("rogue", LandmarkKind.ROGUELIKE, 120, -70, 135, -60),
            LayoutFixtures.landmark("rogue2", LandmarkKind.ROGUELIKE, 127, -59, 140, -30),
            LayoutFixtures.landmark("loot", LandmarkKind.LOOTGAMES, 200, 200, 260, 230),
            LayoutFixtures.landmark("meteor", LandmarkKind.METEORITE, -17, -17, -1, -1),
            LayoutFixtures.landmark("witch", LandmarkKind.WITCHERY, 300, -250, 301, -249) };
    }

    private static boolean blockedByZone(LandmarkZones zones, int chunkX, int chunkZ) {
        Zone zone = zones.zone(chunkX, chunkZ)
            .zone();
        return zone == Zone.CORE || zone == Zone.EDGE_RING || zone == Zone.AVOID_RING;
    }

    @Test
    public void maskEqualsTheZoneAnswerAtEveryMargin() {
        for (int margin : new int[] { 0, 2, 17, 600 }) {
            LayoutProfile profile = LayoutProfile.builder()
                .coexistence(margin, 2, 4)
                .landmarkTreatment(LandmarkKind.ROGUELIKE, LandmarkTreatment.AVOID)
                .landmarkTreatment(LandmarkKind.LOOTGAMES, LandmarkTreatment.IGNORE)
                .build();
            LandmarkZones byZone = new LandmarkZones(new LayoutFixtures.FakeOracle(landmarks()), profile, 4096);
            LandmarkZones byMask = new LandmarkZones(new LayoutFixtures.FakeOracle(landmarks()), profile, 4096);
            int blocked = 0;
            for (int x = -40; x <= 40; x++) {
                for (int z = -40; z <= 40; z++) {
                    boolean expected = blockedByZone(byZone, x, z);
                    assertEquals(expected, byMask.blocksLinks(x, z), "margin " + margin + " chunk " + x + "," + z);
                    blocked += expected ? 1 : 0;
                }
            }
            assertTrue(blocked > 0, "margin " + margin + " blocks something");
        }
    }

    /** Counts oracle calls and the chunk area they ask about. */
    private static final class CountingOracle implements StructureOracle {

        private final StructureOracle inner = new LayoutFixtures.FakeOracle(longRouteLandmarks());
        long calls;
        long area;

        @Override
        public List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
            calls++;
            area += (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            return inner.landmarksTouching(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        }
    }

    private static Landmark[] longRouteLandmarks() {
        List<Landmark> out = new ArrayList<>();
        // Villages straddling the straight line every 100 chunks, so the route detours.
        for (int i = 1; i <= 4; i++) {
            int x = (128 + i * 110) * 16;
            out.add(LayoutFixtures.landmark("village@" + i, LandmarkKind.VILLAGE, x, 120 * 16, x + 40, 150 * 16));
        }
        return out.toArray(new Landmark[0]);
    }

    /** Two cities 532 chunks apart in 256-chunk cells, routed by a given blocked test. */
    private static final class TwoCities implements CityLinkGraph.Nodes {

        private final LandmarkZones zones;
        private final boolean mask;

        TwoCities(LandmarkZones zones, boolean mask) {
            this.zones = zones;
            this.mask = mask;
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
            return cellX == 0 ? new int[] { 128, 140 } : cellX == 2 ? new int[] { 660, 140 } : null;
        }

        @Override
        public boolean blocked(int chunkX, int chunkZ) {
            return mask ? zones.blocksLinks(chunkX, chunkZ) : blockedByZone(zones, chunkX, chunkZ);
        }
    }

    @Test
    public void aLongRouteAsksTheOracleLessThroughTheMask() {
        LayoutProfile profile = LayoutProfile.defaults();
        CountingOracle before = new CountingOracle();
        CountingOracle after = new CountingOracle();
        CityLinkGraph byZone = new CityLinkGraph(new TwoCities(new LandmarkZones(before, profile, 2048), false), 64);
        CityLinkGraph byMask = new CityLinkGraph(new TwoCities(new LandmarkZones(after, profile, 2048), true), 64);
        List<CityLinkGraph.Link> zoneLinks = byZone.links(0, 0);
        List<CityLinkGraph.Link> maskLinks = byMask.links(0, 0);
        assertEquals(1, zoneLinks.size());
        assertEquals(zoneLinks, maskLinks);
        assertEquals(
            zoneLinks.get(0)
                .path()
                .toString(),
            maskLinks.get(0)
                .path()
                .toString());
        assertTrue(
            zoneLinks.get(0)
                .path()
                .size() >= 500);
        System.out.println(
            "D-linkgraph-shrink-1 one " + zoneLinks.get(0)
                .path()
                .size()
                + "-chunk route: oracle calls before "
                + before.calls
                + " (area "
                + before.area
                + " chunks), after "
                + after.calls
                + " (area "
                + after.area
                + " chunks)");
        assertTrue(after.area < before.area, "the mask asks about less area");
    }
}
