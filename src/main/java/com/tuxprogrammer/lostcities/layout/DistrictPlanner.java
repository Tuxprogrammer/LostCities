package com.tuxprogrammer.lostcities.layout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.random.QualityRandom;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.Selection;

/**
 * City districts (city-realism phase 5c, decisions D279 and D350): a paved central plaza, a few industrial yards
 * on the side of the city that faces its links, and a belt of parks between the inner and outer density rings.
 * Original to this mod.
 *
 * <p>
 * Every decision is a pure function of the seed and the chunk (plus the chunk's own base plan), so a chunk plans the
 * same whenever it is asked and never needs a neighbor's plan: nothing cascades. The planner calls {@link #apply}
 * once per chunk, before the construction-lot roll; the multibuilding placer asks {@link #reserved} so no
 * multibuilding covers a plaza.
 *
 * <ul>
 * <li><b>Plaza:</b> the city's center chunk, and in cities of at least {@value #BIG_CITY_RADIUS} blocks radius the
 * 2 x 2 chunks that start at it (east and south). It becomes {@link LotKind#PLAZA}.</li>
 * <li><b>Industry:</b> a site at ring distance {@value #SITE_DISTANCE} on the cardinal side with the most links
 * (ties and link-less worlds by hash); the 3 x 3 chunks around it each become {@link LotKind#INDUSTRY} with chance
 * {@value #YARD_CHANCE}, if they lie in the outer or edge ring.</li>
 * <li><b>Park belt:</b> chunks with ring distance in {@code [BELT_FROM, BELT_TO]} become parks with chance
 * {@value #BELT_CHANCE}.</li>
 * </ul>
 * Needs grid-placed cities (a center to measure from) and {@code densityRings > 0}. Side: both (plain Java).
 */
final class DistrictPlanner {

    /** City radius in blocks from which the plaza is 2 x 2 chunks. */
    static final int BIG_CITY_RADIUS = 112;
    /** The industrial site's ring distance. */
    static final float SITE_DISTANCE = 0.62f;
    /** Chance that a chunk around the site is a yard. */
    static final float YARD_CHANCE = 0.7f;
    /** Ring distance band of the park belt: either side of the inner to outer boundary. */
    static final float BELT_FROM = DensityRings.INNER_END - 0.07f;
    static final float BELT_TO = DensityRings.INNER_END + 0.07f;
    /** Chance that a belt chunk is a park. */
    static final float BELT_CHANCE = 0.5f;
    /** Fewest chunks between a center and its site. */
    private static final int MIN_SITE_CHUNKS = 3;
    private static final long SALT = 0x4449535452494354L;
    private static final long SITE_SALT = 0x5349544549444e44L;
    private static final long BELT_SALT = 0x42454c5450524b53L;
    private static final long YARD_SALT = 0x5941524442494c54L;
    private static final long MIX_Z = 132897987541L;
    private static final int CACHE = 128;
    /** The cardinal sides as steps: east, west, south, north. */
    private static final int[][] SIDES = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private final LayoutPlanner planner;
    private final Map<Long, int[]> sites = new LinkedHashMap<Long, int[]>(CACHE, 0.75f, true) {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > CACHE;
        }
    };

    DistrictPlanner(LayoutPlanner planner) {
        this.planner = planner;
    }

    /** Whether districts apply to this world at all. */
    private boolean enabled() {
        return planner.profile.cityDistricts && planner.profile.densityRings > 0 && !planner.cities.perlinMode();
    }

    /**
     * Returns the plaza footprint position of a chunk, or {@code null} when the chunk is not a plaza chunk:
     * {@link MultiPos#SINGLE} for a one-chunk plaza, else the chunk's place in the 2 x 2.
     */
    MultiPos plazaAt(int x, int z) {
        if (!enabled()) {
            return null;
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                int cx = x - dx;
                int cz = z - dz;
                if (!planner.cities.isCityCenter(cx, cz) || !planner.cities.isCityRaw(cx, cz)) {
                    continue;
                }
                if (planner.cities.cityRadius(cx, cz) >= BIG_CITY_RADIUS) {
                    return new MultiPos(dx, dz, 2, 2);
                }
                if (dx == 0 && dz == 0) {
                    return MultiPos.SINGLE;
                }
            }
        }
        return null;
    }

    /** Whether a chunk is part of a plaza, so no multibuilding may cover it. */
    boolean reserved(int x, int z) {
        return plazaAt(x, z) != null;
    }

    /**
     * Whether the park belt claims a chunk: in the ring band, by a hash of the seed and chunk. The chunk's plan may
     * still decline (a road, a highway).
     */
    boolean isBelt(int x, int z) {
        if (!enabled() || !planner.cities.isCityRaw(x, z)) {
            return false;
        }
        float d = planner.ringDistance(x, z);
        return d >= BELT_FROM && d <= BELT_TO && roll(BELT_SALT, x, z) < BELT_CHANCE;
    }

    /** Whether the industrial district claims a chunk: near its city's site, in the outer or edge ring. */
    boolean isYard(int x, int z) {
        if (!enabled() || !planner.cities.isCityRaw(x, z) || planner.ringDistance(x, z) < DensityRings.INNER_END) {
            return false;
        }
        int[] center = planner.cities.strongestCenter(x, z);
        if (center == null) {
            return false;
        }
        int[] site = site(center[0], center[1]);
        return site != null && Math.abs(site[0] - x) <= 1
            && Math.abs(site[1] - z) <= 1
            && roll(YARD_SALT, x, z) < YARD_CHANCE;
    }

    /** The industrial site of a city (chunk X and Z), or {@code null} when the city is too small. */
    synchronized int[] site(int cx, int cz) {
        long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
        int[] cached = sites.get(key);
        if (cached != null) {
            return cached.length == 0 ? null : cached;
        }
        int[] site = computeSite(cx, cz);
        sites.put(key, site == null ? new int[0] : site);
        return site;
    }

    private int[] computeSite(int cx, int cz) {
        float radius = planner.cities.cityRadius(cx, cz);
        int dist = Math.round(SITE_DISTANCE * radius * (1f - planner.profile.cityThreshold) / 16f);
        if (dist < MIN_SITE_CHUNKS) {
            return null;
        }
        int[] counts = new int[4];
        Networks networks = planner.networks;
        CityLinkGraph graph = networks == null ? null : networks.links();
        if (graph != null) {
            List<CityLinkGraph.Link> links = graph
                .links(Math.floorDiv(cx, graph.cell()), Math.floorDiv(cz, graph.cell()));
            for (CityLinkGraph.Link link : links) {
                boolean lower = link.ax() == cx && link.az() == cz;
                int dx = (lower ? link.bx() : link.ax()) - cx;
                int dz = (lower ? link.bz() : link.az()) - cz;
                counts[Math.abs(dx) >= Math.abs(dz) ? dx > 0 ? 0 : 1 : dz > 0 ? 2 : 3]++;
            }
        }
        int best = 0;
        for (int s = 0; s < 4; s++) {
            best = Math.max(best, counts[s]);
        }
        int tied = 0;
        for (int s = 0; s < 4; s++) {
            if (counts[s] == best) {
                tied++;
            }
        }
        int pick = new QualityRandom(planner.seed ^ SITE_SALT ^ cx * LayoutPlanner.BUILDING_Z ^ cz * MIX_Z)
            .nextInt(tied);
        int side = 0;
        for (int s = 0; s < 4; s++) {
            if (counts[s] == best && pick-- == 0) {
                side = s;
                break;
            }
        }
        return new int[] { cx + SIDES[side][0] * dist, cz + SIDES[side][1] * dist };
    }

    private float roll(long salt, int x, int z) {
        return new QualityRandom(planner.seed ^ SALT ^ salt ^ x * LayoutPlanner.BUILDING_Z ^ z * MIX_Z).nextFloat();
    }

    /** Turns a chunk into plaza, yard or belt park when a district claims it; leaves every other chunk alone. */
    void apply(ChunkPlan.Builder b, int x, int z, CityStyleView style) {
        ChunkCharacteristics c = b.characteristics;
        if (!enabled() || !c.isCity
            || b.lot != LotKind.NONE
            || c.multiPos.isMulti()
            || planner.predefined.occupant(x, z) != null
            || b.highwayXLevel >= 0
            || b.highwayZLevel >= 0
            || c.lane.role() != LaneRole.NONE
            || c.zone.zone()
                .isTransition()) {
            return;
        }
        MultiPos plaza = plazaAt(x, z);
        if (plaza != null) {
            open(b);
            clear(b);
            b.lot = LotKind.PLAZA;
            b.lotFootprint = plaza;
            return;
        }
        boolean building = b.hasBuilding;
        boolean open = !building && b.hierarchicalOpen;
        boolean plain = building && c.plannedRoad == PlannedRoadType.NONE;
        if (!open && !plain || planner.infrastructure.rail(x, z)
            .isPresent()) {
            return;
        }
        if (isYard(x, z)) {
            open(b);
            clear(b);
            b.lot = LotKind.INDUSTRY;
            return;
        }
        if (isBelt(x, z)) {
            open(b);
            b.streetType = StreetType.PARK;
            QualityRandom r = new QualityRandom(planner.seed ^ BELT_SALT ^ x * 31L ^ z * 17L);
            b.parkPart = style == null ? null : style.pick(Selection.PARKS, r, x, z);
            b.fountainPart = null;
        }
    }

    private static void open(ChunkPlan.Builder b) {
        if (b.hasBuilding) {
            LayoutPlanner.becomeOpenLot(b);
            b.floors = 0;
            b.cellars = 0;
        }
    }

    private static void clear(ChunkPlan.Builder b) {
        b.streetType = StreetType.NORMAL;
        b.parkPart = null;
        b.fountainPart = null;
        b.frontPart = null;
    }
}
