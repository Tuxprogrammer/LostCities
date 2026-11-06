package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.CityHighwayLayout;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;

/**
 * Each link-network city's own highway (design record 0017): a beltway or a cut-through, chosen per city by its room.
 *
 * <p>
 * <b>Beltway.</b> A square ring of chunks at Chebyshev distance {@code half} from the center, {@code half} at least
 * {@link #MIN_HALF} (an inner block of at least 9 x 9 chunks), sized so the ring sits about halfway out (ring distance
 * {@link #RING_AT}) and its corners keep {@link #MARGIN} chunks of city outside. Every link of the city with the ramp
 * room ({@link #climbs}) stops on the ring instead of at the edge plaza: its path's first chunk on the ring joins the
 * ring's piece at the ring's level (an interchange, one deck with no open edge), and the link's own ramps carry it to
 * the other city's level. A link without the room stops at the city's edge as any other link does.
 *
 * <p>
 * <b>Cut-through.</b> Two of the city's links, the pair whose edge plazas point most apart, are joined by a straight or
 * one-bend highway between their plazas, the bend at the corner nearer the center. A route that would cross a
 * landmark's core or margin, or a pair whose links lack the ramp room, tries the other bend, then the next pair; with
 * none left the city has no city highway.
 *
 * <p>
 * Both are flat at one level above the highest city level under them (capped at the top city level), so streets pass
 * under; a ring or route chunk carries a highway piece, which keeps buildings off it (the planner's existing highway
 * rule). Eligibility (owner decision 3): a beltway needs the size above, no landmark zone in the city's extent (a city
 * tapering around a village, dungeon or landmark is always cut-through) and at least {@link #MIN_RING_CITY} of its ring
 * chunks city; {@code random} then picks it with a chance that rises with the share of the city's disc that is city.
 *
 * <p>
 * Pure: reads the link graph and the {@link Cities} oracle (terrain, zones and the city map only, never a chunk plan),
 * so every chunk agrees in any order. Plans are cached in a bounded LRU; methods are synchronized.
 */
public final class CityHighways {

    /** Smallest ring half-size: the inner block is {@code 2 * half - 1} chunks wide, so 9 x 9. */
    public static final int MIN_HALF = 5;
    /** Chunks of city a ring corner keeps outside it. */
    public static final int MARGIN = 2;
    /** The ring's distance from the center as a share of the city's extent (the density rings' distance). */
    public static final float RING_AT = 0.5f;
    /** Share of ring chunks that must be city for a beltway. */
    public static final float MIN_RING_CITY = 0.6f;
    private static final long SALT = 0x5EEDBE17L;
    private static final int CACHE = 1024;

    /** The city and terrain facts the plans read. */
    public interface Cities {

        /** Returns the grid cell edge in chunks. */
        int cell();

        /**
         * Returns the city center of a grid cell that yields a city, or {@code null}.
         *
         * @param cellX cell X
         * @param cellZ cell Z
         * @return {chunkX, chunkZ}, or null
         */
        int[] center(int cellX, int cellZ);

        /** Returns the radius in blocks of the city centered on a chunk. */
        int radius(int centerX, int centerZ);

        /** Returns the largest city radius in blocks. */
        int maxRadius();

        /** Returns the city threshold: the city factor at the city's edge. */
        float threshold();

        /** Returns whether a chunk is city by terrain and zones (before the plan). */
        boolean city(int chunkX, int chunkZ);

        /** Returns whether a chunk lies in any landmark zone (core, margin, avoid or transition ring). */
        boolean landmark(int chunkX, int chunkZ);

        /** Returns whether a highway must not cross a chunk: a landmark's core or margin. */
        boolean blocked(int chunkX, int chunkZ);

        /** Returns a chunk's city level. */
        int level(int chunkX, int chunkZ);

        /** Returns the highest city level. */
        int maxLevel();
    }

    /** What a city carries. */
    public enum Kind {
        /** Nothing: links stop at edge plazas. */
        NONE,
        /** A ring highway. */
        BELTWAY,
        /** A straight or one-bend highway between two links. */
        CUTTHROUGH
    }

    /** One city's highway. */
    public static final class Plan {

        private final Kind kind;
        private final int centerX;
        private final int centerZ;
        private final int half;
        private final int level;
        private final boolean beltwayFits;
        private final List<CityLinkGraph.Link> joined;
        private final Map<Long, HighwayInfo.Piece> pieces;

        Plan(Kind kind, int centerX, int centerZ, int half, int level, boolean beltwayFits,
            List<CityLinkGraph.Link> joined, Map<Long, HighwayInfo.Piece> pieces) {
            this.kind = kind;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.half = half;
            this.level = level;
            this.beltwayFits = beltwayFits;
            this.joined = joined;
            this.pieces = pieces;
        }

        /** Returns what the city carries. */
        public Kind kind() {
            return kind;
        }

        /** Returns the center's chunk X. */
        public int centerX() {
            return centerX;
        }

        /** Returns the center's chunk Z. */
        public int centerZ() {
            return centerZ;
        }

        /** Returns the ring's Chebyshev distance from the center (beltway), else 0. */
        public int half() {
            return half;
        }

        /** Returns the deck level of the ring or the cut-through. */
        public int level() {
            return level;
        }

        /** Returns whether the city had the room for a beltway (size, no landmark, ring mostly city). */
        public boolean beltwayFits() {
            return beltwayFits;
        }

        /** Returns the links that end on this city's highway: all of them on a beltway, two on a cut-through. */
        public List<CityLinkGraph.Link> joined() {
            return joined;
        }

        /** Returns the chunks of the ring or cut-through and their pieces, keyed by {@link #key}. */
        public Map<Long, HighwayInfo.Piece> pieces() {
            return pieces;
        }

        /** Returns whether a link ends on this city's highway. */
        public boolean joins(CityLinkGraph.Link link) {
            return joined.contains(link);
        }

        /** Returns whether a chunk lies strictly inside the ring (beltway only). */
        public boolean insideRing(int chunkX, int chunkZ) {
            return kind == Kind.BELTWAY && Math.max(Math.abs(chunkX - centerX), Math.abs(chunkZ - centerZ)) < half;
        }
    }

    private final long seed;
    private final CityHighwayLayout layout;
    private final CityLinkGraph graph;
    private final Cities cities;
    private final int reachCells;
    private final Map<Long, Plan> plans = new LinkedHashMap<Long, Plan>(256, 0.75f, true) {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Plan> eldest) {
            return size() > CACHE;
        }
    };

    /** Each city's highest possible city highway level ({@link #peak}). */
    private final Map<Long, Integer> peaks = new LinkedHashMap<Long, Integer>(256, 0.75f, true) {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest) {
            return size() > CACHE;
        }
    };

    /**
     * Creates the city highways of a world.
     *
     * @param seed   the world seed
     * @param layout the profile's {@code cityHighwayLayout}
     * @param graph  the link graph
     * @param cities the city facts
     */
    public CityHighways(long seed, CityHighwayLayout layout, CityLinkGraph graph, Cities cities) {
        this.seed = seed;
        this.layout = layout;
        this.graph = graph;
        this.cities = cities;
        int reachChunks = (cities.maxRadius() + 15) / 16 + 2;
        this.reachCells = 1 + reachChunks / Math.max(1, cities.cell());
    }

    /** Returns the layout option. */
    public CityHighwayLayout layout() {
        return layout;
    }

    /**
     * Returns the highway of the city centered on a chunk.
     *
     * @param centerX the center's chunk X
     * @param centerZ the center's chunk Z
     * @return the plan; {@link Kind#NONE} with no pieces when the option is {@code none}
     */
    public synchronized Plan plan(int centerX, int centerZ) {
        long key = key(centerX, centerZ);
        Plan cached = plans.get(key);
        if (cached == null) {
            cached = compute(centerX, centerZ);
            plans.put(key, cached);
        }
        return cached;
    }

    /**
     * Returns the city-highway pieces in a chunk, from every city near enough to reach it.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the pieces, empty off every ring and cut-through
     */
    public synchronized List<HighwayInfo.Piece> pieces(int chunkX, int chunkZ) {
        if (layout == CityHighwayLayout.NONE) {
            return Collections.emptyList();
        }
        List<HighwayInfo.Piece> out = null;
        long key = key(chunkX, chunkZ);
        for (Plan plan : near(chunkX, chunkZ)) {
            HighwayInfo.Piece piece = plan.pieces.get(key);
            if (piece != null) {
                if (out == null) {
                    out = new ArrayList<>(2);
                }
                out.add(piece);
            }
        }
        return out == null ? Collections.emptyList() : out;
    }

    /**
     * Returns the beltway plan whose city a chunk lies on or outside the inner edge of, within the city's disc: where
     * the density rings keep towers out (0017). {@code null} when there is none.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the beltway, or null
     */
    public synchronized Plan beltwayAtOrBeyondRing(int chunkX, int chunkZ) {
        if (layout == CityHighwayLayout.NONE || layout == CityHighwayLayout.CUTTHROUGH) {
            return null;
        }
        for (Plan plan : near(chunkX, chunkZ)) {
            if (plan.kind != Kind.BELTWAY) {
                continue;
            }
            long dx = chunkX - plan.centerX;
            long dz = chunkZ - plan.centerZ;
            long r = cities.radius(plan.centerX, plan.centerZ);
            if (Math.max(Math.abs(dx), Math.abs(dz)) >= plan.half - 1 && 256L * (dx * dx + dz * dz) < r * r) {
                return plan;
            }
        }
        return null;
    }

    /** The plans of the cities whose cells lie within reach of a chunk. */
    private List<Plan> near(int chunkX, int chunkZ) {
        int cell = cities.cell();
        int cx = Math.floorDiv(chunkX, cell);
        int cz = Math.floorDiv(chunkZ, cell);
        List<Plan> out = new ArrayList<>();
        for (int ox = -reachCells; ox <= reachCells; ox++) {
            for (int oz = -reachCells; oz <= reachCells; oz++) {
                int[] c = cities.center(cx + ox, cz + oz);
                if (c != null) {
                    out.add(plan(c[0], c[1]));
                }
            }
        }
        return out;
    }

    private Plan compute(int cx, int cz) {
        if (layout == CityHighwayLayout.NONE) {
            return none(cx, cz, false);
        }
        int radius = cities.radius(cx, cz);
        float extent = radius * (1f - cities.threshold()) / 16f;
        int half = Math.max(MIN_HALF, Math.round(extent * RING_AT));
        boolean fits = beltwayFits(cx, cz, half, extent);
        boolean beltway;
        switch (layout) {
            case BELTWAY:
                beltway = fits;
                break;
            case RANDOM:
                beltway = fits && roll(cx, cz) < beltwayChance(fullness(cx, cz, extent));
                break;
            default:
                beltway = false;
                break;
        }
        return beltway ? beltway(cx, cz, half) : cutThrough(cx, cz, radius, fits);
    }

    /**
     * Returns whether a city has the room for a beltway: the ring's corners keep {@link #MARGIN} chunks inside the
     * city's extent, no landmark zone lies in the extent, and enough ring chunks are city.
     */
    private boolean beltwayFits(int cx, int cz, int half, float extent) {
        if (half * Math.sqrt(2) + MARGIN > extent) {
            return false;
        }
        int e = (int) Math.ceil(extent);
        for (int dx = -e; dx <= e; dx++) {
            for (int dz = -e; dz <= e; dz++) {
                if (dx * dx + dz * dz <= extent * extent && cities.landmark(cx + dx, cz + dz)) {
                    return false;
                }
            }
        }
        int ring = 0;
        int city = 0;
        for (int[] c : ring(cx, cz, half)) {
            ring++;
            if (cities.city(c[0], c[1])) {
                city++;
            }
        }
        return city >= MIN_RING_CITY * ring;
    }

    /** The share of the city's disc (its extent) that is city. */
    private float fullness(int cx, int cz, float extent) {
        int e = (int) Math.ceil(extent);
        int all = 0;
        int city = 0;
        for (int dx = -e; dx <= e; dx++) {
            for (int dz = -e; dz <= e; dz++) {
                if (dx * dx + dz * dz <= extent * extent) {
                    all++;
                    if (cities.city(cx + dx, cz + dz)) {
                        city++;
                    }
                }
            }
        }
        return all == 0 ? 0f : (float) city / all;
    }

    /**
     * Returns the chance that {@code random} gives a city that fits a beltway one: 0.15 for a city half in its
     * terrain or less, rising to 0.95 for a full one.
     *
     * @param fullness the share of the city's disc that is city
     * @return the chance
     */
    static float beltwayChance(float fullness) {
        return Math.max(0.15f, Math.min(0.95f, 0.15f + 0.8f * (fullness - 0.5f) / 0.5f));
    }

    private float roll(int cx, int cz) {
        long h = seed ^ SALT;
        h = h * 0x9E3779B97F4A7C15L + cx;
        h = h * 0x9E3779B97F4A7C15L + cz;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1L << 24);
    }

    private Plan beltway(int cx, int cz, int half) {
        List<int[]> ring = ring(cx, cz, half);
        int level = 0;
        for (int[] c : ring) {
            level = Math.max(level, cities.level(c[0], c[1]));
        }
        level = Math.min(cities.maxLevel(), level + 1);
        Map<Long, HighwayInfo.Piece> pieces = new LinkedHashMap<>();
        for (int[] c : ring) {
            int dx = c[0] - cx;
            int dz = c[1] - cz;
            // Openings toward the two ring neighbors: along the side, or around the corner.
            int ax;
            int az;
            int bx;
            int bz;
            if (Math.abs(dx) == half && Math.abs(dz) == half) {
                ax = -Integer.signum(dx);
                az = 0;
                bx = 0;
                bz = -Integer.signum(dz);
            } else if (Math.abs(dx) == half) {
                ax = 0;
                az = -1;
                bx = 0;
                bz = 1;
            } else {
                ax = -1;
                az = 0;
                bx = 1;
                bz = 0;
            }
            pieces.put(key(c[0], c[1]), HighwayInfo.Piece.sides(level, ax, az, bx, bz));
        }
        List<CityLinkGraph.Link> joined = new ArrayList<>();
        if (graph != null) {
            for (CityLinkGraph.Link link : graph
                .links(Math.floorDiv(cx, cities.cell()), Math.floorDiv(cz, cities.cell()))) {
                if (climbs(link, cx, cz, half, level)) {
                    joined.add(link);
                }
            }
        }
        return new Plan(
            Kind.BELTWAY,
            cx,
            cz,
            half,
            level,
            true,
            Collections.unmodifiableList(new ArrayList<>(joined)),
            Collections.unmodifiableMap(pieces));
    }

    /** The ring's chunks at Chebyshev distance {@code half}, clockwise from the north-west corner. */
    static List<int[]> ring(int cx, int cz, int half) {
        List<int[]> out = new ArrayList<>(8 * half);
        for (int i = -half; i < half; i++) {
            out.add(new int[] { cx + i, cz - half });
        }
        for (int i = -half; i < half; i++) {
            out.add(new int[] { cx + half, cz + i });
        }
        for (int i = half; i > -half; i--) {
            out.add(new int[] { cx + i, cz + half });
        }
        for (int i = half; i > -half; i--) {
            out.add(new int[] { cx - half, cz + i });
        }
        return out;
    }

    private Plan cutThrough(int cx, int cz, int radius, boolean fits) {
        if (graph == null) {
            return none(cx, cz, fits);
        }
        List<CityLinkGraph.Link> links = graph
            .links(Math.floorDiv(cx, cities.cell()), Math.floorDiv(cz, cities.cell()));
        List<CityLinkGraph.Link> usable = new ArrayList<>();
        List<int[]> entries = new ArrayList<>();
        for (CityLinkGraph.Link link : links) {
            int[] entry = entry(link, cx, cz, radius);
            if (entry != null) {
                usable.add(link);
                entries.add(entry);
            }
        }
        // Pairs, most opposite first (lowest cosine between the entries' directions), then by index.
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                if (entries.get(i)[0] != entries.get(j)[0] || entries.get(i)[1] != entries.get(j)[1]) {
                    pairs.add(new int[] { i, j });
                }
            }
        }
        pairs.sort((p, q) -> {
            int c = Double.compare(cosine(entries, p, cx, cz), cosine(entries, q, cx, cz));
            return c != 0 ? c : p[0] != q[0] ? Integer.compare(p[0], q[0]) : Integer.compare(p[1], q[1]);
        });
        for (int[] pair : pairs) {
            int[] a = entries.get(pair[0]);
            int[] b = entries.get(pair[1]);
            int[][] bends = { { a[0], b[1] }, { b[0], a[1] } };
            long d0 = dist2(bends[0], cx, cz);
            long d1 = dist2(bends[1], cx, cz);
            if (d1 < d0) {
                bends = new int[][] { bends[1], bends[0] };
            }
            for (int[] bend : bends) {
                List<int[]> route = route(a, bend, b);
                if (route == null) {
                    continue;
                }
                int level = 0;
                for (int[] c : route) {
                    level = Math.max(level, cities.level(c[0], c[1]));
                }
                level = Math.min(cities.maxLevel(), level + 1);
                if (!climbs(usable.get(pair[0]), cx, cz, 0, level) || !climbs(usable.get(pair[1]), cx, cz, 0, level)) {
                    continue;
                }
                Map<Long, HighwayInfo.Piece> pieces = new LinkedHashMap<>();
                for (int k = 0; k < route.size(); k++) {
                    int[] c = route.get(k);
                    List<Integer> steps = new ArrayList<>(4);
                    if (k > 0) {
                        steps.add(route.get(k - 1)[0] - c[0]);
                        steps.add(route.get(k - 1)[1] - c[1]);
                    }
                    if (k + 1 < route.size()) {
                        steps.add(route.get(k + 1)[0] - c[0]);
                        steps.add(route.get(k + 1)[1] - c[1]);
                    }
                    int[] array = new int[steps.size()];
                    for (int s = 0; s < array.length; s++) {
                        array[s] = steps.get(s);
                    }
                    // An end opens only inward; its joined link adds its own opening and LinkHighways.merge makes the
                    // two one deck with railings on every other edge (a straight, bend or link junction).
                    pieces.put(
                        key(c[0], c[1]),
                        array.length == 2 ? HighwayInfo.Piece.linkJunction(level, array)
                            : HighwayInfo.Piece.sides(level, array));
                }
                List<CityLinkGraph.Link> joined = new ArrayList<>(2);
                joined.add(usable.get(pair[0]));
                joined.add(usable.get(pair[1]));
                return new Plan(
                    Kind.CUTTHROUGH,
                    cx,
                    cz,
                    0,
                    level,
                    fits,
                    Collections.unmodifiableList(joined),
                    Collections.unmodifiableMap(pieces));
            }
        }
        return none(cx, cz, fits);
    }

    /**
     * Returns whether a link has the ramp room to meet a city highway at {@code level} here, whatever its other end
     * turns out to be (D348). The other end is that city's own level, or its city highway's level, which lies between
     * {@code min(1, top)} and that city's {@link #peak}; the bound takes the worst of these, so it never needs the
     * other
     * city's plan and two cities never wait on each other. The room counts the straight chunks no other link crosses
     * strictly between the written ends, which {@link LinkHighways} can always ramp, diagonal or staircase: from the
     * ring here ({@code half} &gt; 0) or this city's radius, to the other city's radius, which encloses any ring there.
     *
     * @param link  the link
     * @param cx    this city's center chunk X
     * @param cz    this city's center chunk Z
     * @param half  the ring's half-size for a beltway, else 0
     * @param level the city highway's level
     * @return whether the link's ramps can always reach the city highway
     */
    boolean climbs(CityLinkGraph.Link link, int cx, int cz, int half, int level) {
        boolean fromA = link.ax() == cx && link.az() == cz;
        int ox = fromA ? link.bx() : link.ax();
        int oz = fromA ? link.bz() : link.az();
        int low = Math.min(1, cities.maxLevel());
        int need = Math.max(Math.abs(level - cities.level(ox, oz)), Math.max(level - low, peak(ox, oz) - level));
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        int start = 0;
        int end = n - 1;
        if (fromA) {
            while (start < n && enclosedHere(path.get(start), cx, cz, half)) {
                start++;
            }
            while (end >= 0 && LinkHighways.inside(path.get(end), ox, oz, cities.radius(ox, oz))) {
                end--;
            }
        } else {
            while (start < n && LinkHighways.inside(path.get(start), ox, oz, cities.radius(ox, oz))) {
                start++;
            }
            while (end >= 0 && enclosedHere(path.get(end), cx, cz, half)) {
                end--;
            }
        }
        if (start > end) {
            return false;
        }
        int room = 0;
        for (int i = start + 1; i < end && room < need; i++) {
            LinkRaster.Step step = path.get(i);
            if (step.kind() == LinkRaster.Kind.STRAIGHT && !crossed(link, step.chunkX(), step.chunkZ())) {
                room++;
            }
        }
        return room >= need;
    }

    /**
     * Returns the highest level a city highway of the city centered on a chunk can take: one above the highest city
     * level in the square that holds its radius and its edge plazas, capped at the top city level. It reads the city
     * map only, never a plan.
     */
    private int peak(int cx, int cz) {
        Long key = key(cx, cz);
        Integer cached = peaks.get(key);
        if (cached != null) {
            return cached;
        }
        int top = cities.maxLevel();
        int reach = cities.radius(cx, cz) / 16 + 2;
        int high = 0;
        for (int x = cx - reach; x <= cx + reach && high < top; x++) {
            for (int z = cz - reach; z <= cz + reach && high < top; z++) {
                high = Math.max(high, cities.level(x, z));
            }
        }
        int peak = Math.min(top, high + 1);
        peaks.put(key, peak);
        return peak;
    }

    private boolean enclosedHere(LinkRaster.Step step, int cx, int cz, int half) {
        if (half > 0) {
            return Math.max(Math.abs(step.chunkX() - cx), Math.abs(step.chunkZ() - cz)) < half;
        }
        return LinkHighways.inside(step, cx, cz, cities.radius(cx, cz));
    }

    /** Whether another link's path crosses a chunk, as {@link LinkHighways} decides where a ramp may go. */
    private boolean crossed(CityLinkGraph.Link link, int x, int z) {
        for (CityLinkGraph.Hit hit : graph.hits(x, z)) {
            if (!hit.link()
                .equals(link)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a link's edge plaza at this city: the first chunk of its path outside the city's radius, counted from
     * this end, or {@code null} when the whole path lies inside (as {@link LinkHighways} decides).
     */
    static int[] entry(CityLinkGraph.Link link, int cx, int cz, int radius) {
        List<LinkRaster.Step> path = link.path();
        boolean fromA = link.ax() == cx && link.az() == cz;
        int n = path.size();
        for (int k = 0; k < n; k++) {
            LinkRaster.Step step = path.get(fromA ? k : n - 1 - k);
            long dx = step.chunkX() - cx;
            long dz = step.chunkZ() - cz;
            if (radius <= 0 || 256L * (dx * dx + dz * dz) > (long) radius * radius) {
                return new int[] { step.chunkX(), step.chunkZ() };
            }
        }
        return null;
    }

    private static double cosine(List<int[]> entries, int[] pair, int cx, int cz) {
        int[] a = entries.get(pair[0]);
        int[] b = entries.get(pair[1]);
        double ax = a[0] - cx;
        double az = a[1] - cz;
        double bx = b[0] - cx;
        double bz = b[1] - cz;
        double na = Math.sqrt(ax * ax + az * az);
        double nb = Math.sqrt(bx * bx + bz * bz);
        return na == 0 || nb == 0 ? 1 : (ax * bx + az * bz) / (na * nb);
    }

    private static long dist2(int[] c, int cx, int cz) {
        long dx = c[0] - cx;
        long dz = c[1] - cz;
        return dx * dx + dz * dz;
    }

    /** The chunks from a through the bend to b, axis-aligned; null when one is blocked. */
    private List<int[]> route(int[] a, int[] bend, int[] b) {
        List<int[]> out = new ArrayList<>();
        walk(out, a, bend);
        walk(out, bend, b);
        out.add(b);
        for (int[] c : out) {
            if (cities.blocked(c[0], c[1])) {
                return null;
            }
        }
        return out;
    }

    /** Adds the chunks from {@code from} up to, not including, {@code to} along one axis. */
    private static void walk(List<int[]> out, int[] from, int[] to) {
        int sx = Integer.signum(to[0] - from[0]);
        int sz = Integer.signum(to[1] - from[1]);
        int x = from[0];
        int z = from[1];
        while (x != to[0] || z != to[1]) {
            out.add(new int[] { x, z });
            x += sx;
            z += sz;
        }
    }

    private static Plan none(int cx, int cz, boolean fits) {
        return new Plan(
            Kind.NONE,
            cx,
            cz,
            0,
            0,
            fits,
            Collections.<CityLinkGraph.Link>emptyList(),
            Collections.<Long, HighwayInfo.Piece>emptyMap());
    }

    /**
     * Returns the key of a chunk in {@link Plan#pieces}.
     *
     * @param x chunk X
     * @param z chunk Z
     * @return the key
     */
    public static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }
}
