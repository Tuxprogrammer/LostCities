package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.worldgen.timing.PerfCounters;

/**
 * The highways of the city link network ({@code networkLayout=links}, design record 0015): one highway along every
 * link of a {@link CityLinkGraph}, as the {@link HighwayInfo} of each chunk.
 *
 * <p>
 * A link's path ({@link LinkRaster}, or a detour around a landmark, D390) starts and ends at city centers. The chunks
 * inside each end's city radius are not written; the first chunk outside is a plaza (a crossing, 0013 §2) at that
 * city's level, so traffic meets the city's streets. When the chunk inward of that plaza is not city (the city's rim
 * failed the threshold), the link runs on as a spur and its plaza is the first city chunk on the path ({@link #spur},
 * D391), so no highway ends in open ground. Between the two plazas each chunk is a straight piece, a ramp, a diagonal
 * or a transition
 * ({@link HighwayInfo.Piece#link}).
 *
 * <p>
 * Levels (0015 §3). The highway leaves each plaza at its city's level and climbs or falls one level per ramp chunk.
 * Ramps go on diagonal chunks first, nearest the middle of the written path, then on straight chunks nearest the
 * middle; transitions and plazas stay flat. So two links that leave a city along the same row stay at the city's level
 * together. When a path has too few rampable chunks, the far plaza takes the level the ramps reach, so the deck never
 * steps at a seam.
 *
 * <p>
 * Crossings (0015 §5, D-link-crossing-1). Where links cross, they meet at one level and merge into a junction. Each
 * link is first planned alone as above (its base). A chunk written by several links takes one level: their base level
 * when they agree, else the level between their base levels that each reaches from both of its plazas with the fewest
 * extra ramps, the lower on a tie. A link keeps a crossing when its ramp room between its plazas and the crossings it
 * already keeps reaches that level ({@link #keeps}); a crossing every link there keeps is matched, and each link
 * re-places its ramps so it passes every matched crossing at its level, one level per ramp chunk, never on a shared
 * chunk. A crossing that cannot be matched keeps its grade separation. A link with no crossing to move keeps its base
 * profile unchanged. Every step reads only base profiles (its own and its neighbors', and theirs to two hops), so the
 * answer is the same in any order.
 *
 * <p>
 * Where links share a chunk, identical pieces merge and all flat pieces at one level become one: a plaza, a straight
 * piece, bend, T or crossing, or a coded link junction ({@link #merge}); ramps and other levels (a crossing that could
 * not be matched) are written lowest first. A diagonal deck spills into the two side neighbors at each corner it opens
 * on; those chunks report the spill so buildings keep clear.
 *
 * <p>
 * Staircase fallback (0015 §3). A link whose 45° deck would cover a bridge or a tunnel chunk
 * ({@link Ends#bridgeOrTunnel}) takes a corner staircase instead: each 45° step becomes a step along X and one along Z
 * through the side chunk between, which holds a bend, so every piece is a part and none is written in code.
 *
 * <p>
 * Pure: reads only the graph and the {@link Ends} oracle (terrain-derived), so it answers the same in any order and
 * never loads a chunk. Caches are bounded LRUs; methods are synchronized.
 */
public final class LinkHighways {

    /** City facts at a link's ends, and the terrain a 45° deck must not cross. */
    public interface Ends {

        /**
         * Returns the city level of a city's center chunk.
         *
         * @param chunkX the center's chunk X
         * @param chunkZ the center's chunk Z
         * @return the level
         */
        int level(int chunkX, int chunkZ);

        /**
         * Returns the radius of the city centered on a chunk, in blocks.
         *
         * @param chunkX the center's chunk X
         * @param chunkZ the center's chunk Z
         * @return the radius in blocks; 0 or less writes the link from the center itself
         */
        int radius(int chunkX, int chunkZ);

        /**
         * Returns whether a 45° deck between two levels in a chunk would be a bridge or a tunnel (0015 §3, the
         * staircase fallback): water or ground more than a level below the lower deck, or ground above the higher.
         * The default never falls back.
         *
         * @param chunkX    chunk X
         * @param chunkZ    chunk Z
         * @param lowLevel  the deck's lower level there
         * @param highLevel the deck's higher level there
         * @return whether the link takes the staircase instead
         */
        default boolean bridgeOrTunnel(int chunkX, int chunkZ, int lowLevel, int highLevel) {
            return false;
        }

        /**
         * Returns whether a chunk is city by terrain and zones (before the plan), as {@link CityHighways.Cities#city}
         * answers. A link whose edge plaza does not touch a city chunk runs on as a spur to the first city chunk on
         * its path ({@link #spur}, D391). The default treats every chunk as city, so no spur.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is city
         */
        default boolean city(int chunkX, int chunkZ) {
            return true;
        }

        /**
         * Returns whether a highway must not cross a chunk: a landmark's core or margin. A spur stops short of one.
         * The default blocks nothing.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is blocked
         */
        default boolean blocked(int chunkX, int chunkZ) {
            return false;
        }
    }

    /**
     * A link's written range and level profile.
     */
    public static final class Profile {

        private final int start;
        private final int end;
        private final int[] in;
        private final int[] out;
        private final boolean staircase;

        Profile(int start, int end, int[] in, int[] out, boolean staircase) {
            this.start = start;
            this.end = end;
            this.in = in;
            this.out = out;
            this.staircase = staircase;
        }

        /**
         * Returns whether the link takes the corner staircase (0015 §3): each 45° step is two straight steps, along X
         * first, through the side chunk between, which holds a bend.
         */
        public boolean staircase() {
            return staircase;
        }

        /** Returns the path index of the first written chunk, the plaza at the lower end. */
        public int start() {
            return start;
        }

        /**
         * Returns the path index of the last written chunk, the plaza at the upper end; below {@link #start} when none.
         */
        public int end() {
            return end;
        }

        /** Returns whether any chunk of the link is written (the two cities' radii do not cover it all). */
        public boolean written() {
            return start <= end;
        }

        /**
         * Returns the deck's level where the path enters a chunk.
         *
         * @param index a path index from {@link #start} to {@link #end}
         * @return the level
         */
        public int inLevel(int index) {
            return in[index];
        }

        /**
         * Returns the deck's level where the path leaves a chunk.
         *
         * @param index a path index from {@link #start} to {@link #end}
         * @return the level
         */
        public int outLevel(int index) {
            return out[index];
        }
    }

    private static final int CACHE = 4096;

    private final CityLinkGraph graph;
    /** The link steps in a chunk: the graph's, or a fixture's in unit tests. */
    private final Hits hits;
    private final Ends ends;
    /** Each city's beltway or cut-through (0017), or {@code null}. */
    private final CityHighways cities;
    private final Map<CityLinkGraph.Link, Profile> profiles = lru(CACHE);
    /** Each link's profile planned alone, before crossings (§ Crossings in the class Javadoc). */
    private final Map<CityLinkGraph.Link, Base> bases = lru(CACHE);
    /** Each link's crossings it can meet, by path index (§ Crossings). */
    private final Map<CityLinkGraph.Link, boolean[]> keeps = lru(CACHE);
    /** Each shared chunk's crossing, or {@link #NO_CROSSING}. */
    private final Map<Long, Crossing> crossings = lru(CACHE * 2);
    private final Map<Long, List<HighwayInfo.Piece>> own = lru(CACHE * 2);
    private final Map<Long, HighwayInfo> infos = lru(CACHE);

    /**
     * Creates the view.
     *
     * @param graph the link graph
     * @param ends  the city facts at link ends
     */
    public LinkHighways(CityLinkGraph graph, Ends ends) {
        this(graph, ends, null);
    }

    /**
     * Creates the view with the cities' own highways (design record 0017): a link of a beltway city stops on the
     * ring, a link a cut-through joins leaves at the cut-through's level, and their pieces join the network.
     *
     * @param graph  the link graph
     * @param ends   the city facts at link ends
     * @param cities the city highways, or {@code null} for none
     */
    public LinkHighways(CityLinkGraph graph, Ends ends, CityHighways cities) {
        this.graph = graph;
        this.hits = graph == null ? null : graph::hits;
        this.ends = ends;
        this.cities = cities;
    }

    /**
     * Creates the view over a fixture's links instead of a graph (unit tests).
     *
     * @param hits the link steps in each chunk
     * @param ends the city facts at link ends
     * @return the view
     */
    static LinkHighways over(Hits hits, Ends ends) {
        return new LinkHighways(null, hits, ends);
    }

    private LinkHighways(CityLinkGraph graph, Hits hits, Ends ends) {
        this.graph = graph;
        this.hits = hits;
        this.ends = ends;
        this.cities = null;
    }

    /** The link steps in a chunk, as {@link CityLinkGraph#hits} answers. */
    interface Hits {

        List<CityLinkGraph.Hit> hits(int chunkX, int chunkZ);
    }

    /** Returns the cities' own highways (0017), or {@code null}. */
    public CityHighways cities() {
        return cities;
    }

    /** Returns the city and terrain facts the view reads. */
    public Ends ends() {
        return ends;
    }

    /** Returns the graph. */
    public CityLinkGraph graph() {
        return graph;
    }

    /**
     * Returns a chunk's highway: its own pieces and any spill from a neighbor's diagonal.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the info; {@link HighwayInfo#hasHighway()} is false off the network
     */
    public synchronized HighwayInfo info(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        HighwayInfo cached = infos.get(key);
        if (cached != null) {
            return cached;
        }
        long perfInfo = PerfCounters.start();
        List<HighwayInfo.Piece> pieces = pieces(chunkX, chunkZ);
        int low = -1;
        int high = -1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (HighwayInfo.Piece piece : pieces(chunkX + dx, chunkZ + dz)) {
                    if (spillsInto(piece, -dx, -dz)) {
                        low = low < 0 ? piece.level() : Math.min(low, piece.level());
                        high = Math.max(high, piece.topLevel());
                    }
                }
            }
        }
        HighwayInfo info = pieces.isEmpty() && low < 0 ? HighwayInfo.NONE : new HighwayInfo(pieces, low, high);
        infos.put(key, info);
        return info;
    }

    /**
     * Returns whether a piece's deck spills into the neighbor one step away: the neighbor is a side chunk of a corner
     * the piece opens on (0015 §3).
     *
     * @param piece the piece
     * @param dx    the step to the neighbor, X
     * @param dz    the step to the neighbor, Z
     * @return whether its deck reaches the neighbor
     */
    static boolean spillsInto(HighwayInfo.Piece piece, int dx, int dz) {
        if ((dx == 0) == (dz == 0)) {
            return false;
        }
        int[] o = piece.openings();
        for (int i = 0; i + 2 < o.length; i += 3) {
            if (o[i] != 0 && o[i + 1] != 0 && (dx != 0 ? o[i] == dx : o[i + 1] == dz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a chunk's own pieces, lowest first: every link step written here, merged (class Javadoc).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the pieces; empty off the network
     */
    public synchronized List<HighwayInfo.Piece> pieces(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        List<HighwayInfo.Piece> cached = own.get(key);
        if (cached != null) {
            return cached;
        }
        List<HighwayInfo.Piece> out = new ArrayList<>();
        for (CityLinkGraph.Hit hit : hits.hits(chunkX, chunkZ)) {
            HighwayInfo.Piece piece = piece(hit.link(), hit.index());
            if (piece != null) {
                out.add(piece);
            }
        }
        out.addAll(staircaseBends(chunkX, chunkZ));
        if (cities != null) {
            out.addAll(cities.pieces(chunkX, chunkZ));
        }
        List<HighwayInfo.Piece> merged = merge(out);
        own.put(key, merged);
        return merged;
    }

    /**
     * Returns the piece a link writes at one index of its path, or {@code null} when the index lies inside a city.
     *
     * @param link  the link
     * @param index the path index
     * @return the piece, or null
     */
    public synchronized HighwayInfo.Piece piece(CityLinkGraph.Link link, int index) {
        Profile profile = profile(link);
        if (index < profile.start || index > profile.end) {
            return null;
        }
        LinkRaster.Step step = link.path()
            .get(index);
        // Openings toward the previous and the next chunk. On the staircase a corner toward the next chunk becomes the
        // side along X (the bend chunk between lies along X first), and a corner toward the previous one the side along
        // Z.
        int prevDx = -step.inDx();
        int prevDz = -step.inDz();
        int nextDx = step.outDx();
        int nextDz = step.outDz();
        if (profile.staircase) {
            if (prevDx != 0 && prevDz != 0) {
                prevDx = 0;
            }
            if (nextDx != 0 && nextDz != 0) {
                nextDz = 0;
            }
        }
        if (index == profile.start || index == profile.end) {
            List<Integer> steps = new ArrayList<>();
            if (index < profile.end) {
                steps.add(nextDx);
                steps.add(nextDz);
            }
            if (index > profile.start) {
                steps.add(prevDx);
                steps.add(prevDz);
            }
            int[] array = new int[steps.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = steps.get(i);
            }
            // A one-chunk link stays a plaza unless both its ends are interchanges, so a city end still meets streets.
            boolean joinedA = index == profile.start && joinedEnd(link, true);
            boolean joinedB = index == profile.end && joinedEnd(link, false);
            if (profile.start == profile.end ? joinedA && joinedB : joinedA || joinedB) {
                // An interchange on a city highway (0017): the link only adds its own opening to the city highway's
                // piece, which merge joins into one deck. An elevated plaza would be open on every side, so its other
                // edges would drop off into the air.
                return HighwayInfo.Piece.linkJunction(profile.in[index], array);
            }
            return HighwayInfo.Piece.plaza(profile.in[index], array);
        }
        return HighwayInfo.Piece.link(prevDx, prevDz, profile.in[index], nextDx, nextDz, profile.out[index]);
    }

    /** Whether a city highway joins the link at its A end ({@code atA}) or its B end (0017). */
    private boolean joinedEnd(CityLinkGraph.Link link, boolean atA) {
        if (cities == null) {
            return false;
        }
        CityHighways.Plan plan = atA ? cities.plan(link.ax(), link.az()) : cities.plan(link.bx(), link.bz());
        return plan.joins(link);
    }

    /**
     * Returns the bends a staircase link puts in a side chunk off its path (0015 §3): for a 45° step from P to Q, the
     * chunk one step along X from P, joining P's side to Q's side at the step's level.
     */
    private List<HighwayInfo.Piece> staircaseBends(int chunkX, int chunkZ) {
        List<HighwayInfo.Piece> out = new ArrayList<>();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (CityLinkGraph.Hit hit : hits.hits(chunkX - sx, chunkZ)) {
                Profile profile = profile(hit.link());
                int i = hit.index();
                if (!profile.staircase || i < profile.start || i >= profile.end) {
                    continue;
                }
                LinkRaster.Step step = hit.link()
                    .path()
                    .get(i);
                if (step.outDx() == sx && step.outDz() != 0) {
                    out.add(HighwayInfo.Piece.sides(profile.out[i], -sx, 0, 0, step.outDz()));
                }
            }
        }
        return out;
    }

    /**
     * Returns a link's written range and levels (class Javadoc).
     *
     * @param link the link
     * @return the profile
     */
    public synchronized Profile profile(CityLinkGraph.Link link) {
        Profile cached = profiles.get(link);
        if (cached != null) {
            return cached;
        }
        long perfProfile = PerfCounters.start();
        Base base = base(link);
        Profile profile = crossed(link, base);
        profiles.put(link, profile);
        PerfCounters.add("lh.profile.miss", perfProfile);
        if (com.tuxprogrammer.lostcities.layout.LayoutTrace.on()) {
            List<LinkRaster.Step> path = link.path();
            int n = path.size();
            String first = profile.start < n ? path.get(profile.start)
                .chunkX() + ","
                + path.get(profile.start)
                    .chunkZ()
                : "-";
            String last = profile.end >= 0 && profile.end < n ? path.get(profile.end)
                .chunkX() + ","
                + path.get(profile.end)
                    .chunkZ()
                : "-";
            com.tuxprogrammer.lostcities.layout.LayoutTrace.log(
                "link a=%d,%d b=%d,%d pathChunks=%d written=%d..%d firstWritten=%s lastWritten=%s radiusA=%d radiusB=%d"
                    + " levelA=%d levelB=%d staircase=%b replanned=%b",
                link.ax(),
                link.az(),
                link.bx(),
                link.bz(),
                n,
                profile.start,
                profile.end,
                first,
                last,
                ends.radius(link.ax(), link.az()),
                ends.radius(link.bx(), link.bz()),
                ends.level(link.ax(), link.az()),
                ends.level(link.bx(), link.bz()),
                profile.staircase,
                profile != base.profile);
            for (int i = profile.start; i <= profile.end; i++) {
                if (!base.shared[i]) {
                    continue;
                }
                LinkRaster.Step at = path.get(i);
                Crossing c = crossing(at.chunkX(), at.chunkZ());
                if (c == NO_CROSSING) {
                    continue;
                }
                // One line per link at each crossing; a chunk's lines agree, so callers count unique chunks.
                com.tuxprogrammer.lostcities.layout.LayoutTrace.log(
                    "linkCrossing chunk=%d,%d links=%d level=%s matched=%b",
                    at.chunkX(),
                    at.chunkZ(),
                    c.links.size(),
                    c.level == NO_LEVEL ? "none" : Integer.toString(c.level),
                    matched(c));
            }
        }
        return profile;
    }

    // ------------------------------------------------------------------ crossings (0015 §5, D-link-crossing-1)

    /** No level suits every link at a crossing. */
    private static final int NO_LEVEL = Integer.MIN_VALUE;

    /** A link's profile planned alone, with the facts crossing planning reads. */
    private static final class Base {

        final Profile profile;
        /** Whether another link's path hits each path index (D323): never a ramp. */
        final boolean[] shared;
        /**
         * Whether no other link writes a step at each path index of the written range. Filled by {@link #open} before
         * crossing planning reads it.
         */
        boolean[] open;
        /** {@code room[i]}: the open rampable chunks at path indices below {@code i}, for the base's piece kinds. */
        int[] room;

        Base(Profile profile, boolean[] shared) {
            this.profile = profile;
            this.shared = shared;
        }

        /** Returns the rampable chunks strictly between two path indices. */
        int room(int from, int to) {
            return to <= from + 1 ? 0 : room[to] - room[from + 1];
        }

        int startLevel() {
            return profile.in[profile.start];
        }

        int endLevel() {
            return profile.out[profile.end];
        }

        /** Whether the base reaches {@code level} at path index {@code i} from both of its ends. */
        boolean reaches(int i, int level) {
            return Math.abs(level - startLevel()) <= room(profile.start, i)
                && Math.abs(endLevel() - level) <= room(i, profile.end);
        }

        /** The ramps it costs to pass {@code level} at {@code i}, beyond the base's own. */
        int cost(int level) {
            return Math.abs(level - startLevel()) + Math.abs(endLevel() - level) - Math.abs(endLevel() - startLevel());
        }
    }

    /** The links written in one chunk, at their path indices, and the level they meet at. */
    private static final class Crossing {

        final List<CityLinkGraph.Link> links;
        final List<Integer> indices;
        /** The meeting level, or {@link #NO_LEVEL}. */
        final int level;

        Crossing(List<CityLinkGraph.Link> links, List<Integer> indices, int level) {
            this.links = links;
            this.indices = indices;
            this.level = level;
        }
    }

    private static final Crossing NO_CROSSING = new Crossing(
        Collections.emptyList(),
        Collections.emptyList(),
        NO_LEVEL);

    /** Returns a link's profile planned alone (class Javadoc, Levels), cached. */
    private Base base(CityLinkGraph.Link link) {
        Base cached = bases.get(link);
        if (cached != null) {
            return cached;
        }
        // A ramp never shares its chunk with another link (0015 §5, D323): decided from the paths alone, so it
        // does not depend on the other links' levels.
        Profile profile = compute(link, ends, (x, z) -> shared(link, x, z), cities);
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        boolean[] shared = new boolean[n];
        for (int i = 0; i < n; i++) {
            LinkRaster.Step at = path.get(i);
            shared[i] = shared(link, at.chunkX(), at.chunkZ());
        }
        Base base = new Base(profile, shared);
        bases.put(link, base);
        return base;
    }

    /**
     * Returns a link's base with its open chunks filled in: the chunks of its written range where no other link writes
     * a step, so a re-placed ramp never shares its chunk with another link's piece (D323 for written pieces). Unlike
     * the base's
     * own ramp rule, a chunk where another link's path runs unwritten (inside a city) is open: links that converge on
     * one city from the same side otherwise never have room to meet. Reads the neighbors' base profiles only.
     */
    private Base open(CityLinkGraph.Link link) {
        Base base = base(link);
        if (base.open != null) {
            return base;
        }
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        Profile p = base.profile;
        boolean[] open = new boolean[n];
        int[] room = new int[n + 1];
        for (int i = 0; i < n; i++) {
            LinkRaster.Step at = path.get(i);
            open[i] = i >= p.start && i <= p.end && !occupied(link, at.chunkX(), at.chunkZ());
            room[i + 1] = room[i] + (open[i] && rampable(at, p.staircase) ? 1 : 0);
        }
        base.open = open;
        base.room = room;
        return base;
    }

    /**
     * Whether another link writes a step in a chunk, by base profiles. City highways and staircase bends are not
     * counted, as for the base's own ramps, so every chunk the base ramps on stays open.
     */
    private boolean occupied(CityLinkGraph.Link link, int chunkX, int chunkZ) {
        for (CityLinkGraph.Hit hit : hits.hits(chunkX, chunkZ)) {
            if (hit.link()
                .equals(link)) {
                continue;
            }
            Profile p = base(hit.link()).profile;
            if (hit.index() >= p.start && hit.index() <= p.end) {
                return true;
            }
        }
        return false;
    }

    /** Whether a path chunk may hold a ramp: straight, or diagonal off the staircase (class Javadoc, Levels). */
    private static boolean rampable(LinkRaster.Step at, boolean staircase) {
        LinkRaster.Kind kind = at.kind();
        return kind == LinkRaster.Kind.STRAIGHT || kind == LinkRaster.Kind.DIAGONAL && !staircase;
    }

    /**
     * Returns the crossing in a chunk: every link written here, at its path index, and the level they meet at. That is
     * their shared base level when they agree; else the level between their base levels that every one reaches from
     * both of its ends on its own, at the fewest extra ramps, the lower on a tie; else {@link #NO_LEVEL}. Reads only
     * base profiles, so it never depends on another crossing.
     */
    private Crossing crossing(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        Crossing cached = crossings.get(key);
        if (cached != null) {
            return cached;
        }
        List<CityLinkGraph.Link> links = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (CityLinkGraph.Hit hit : hits.hits(chunkX, chunkZ)) {
            if (links.contains(hit.link())) {
                continue;
            }
            Profile p = base(hit.link()).profile;
            int i = hit.index();
            if (i < p.start || i > p.end) {
                continue;
            }
            links.add(hit.link());
            indices.add(i);
            low = Math.min(low, p.in[i]);
            high = Math.max(high, p.in[i]);
        }
        Crossing crossing;
        if (links.size() < 2) {
            crossing = NO_CROSSING;
        } else if (low == high) {
            crossing = new Crossing(links, indices, low);
        } else {
            int best = NO_LEVEL;
            int bestCost = Integer.MAX_VALUE;
            for (int level = low; level <= high; level++) {
                int cost = 0;
                for (int k = 0; k < links.size() && cost != Integer.MAX_VALUE; k++) {
                    Base b = open(links.get(k));
                    cost = b.reaches(indices.get(k), level) ? cost + b.cost(level) : Integer.MAX_VALUE;
                }
                if (cost < bestCost) {
                    bestCost = cost;
                    best = level;
                }
            }
            crossing = new Crossing(links, indices, best);
        }
        crossings.put(key, crossing);
        return crossing;
    }

    /**
     * Returns the crossings a link can meet, by path index. Walking from its lower plaza, it keeps every crossing at
     * its
     * own base level (the base meets those together), and another crossing when the ramp room reaches its level from
     * the last kept one and, from there, the next crossing at a base level or the far plaza. So the kept crossings can
     * always be met together, and so can any subset of them.
     */
    private boolean[] keeps(CityLinkGraph.Link link) {
        boolean[] cached = keeps.get(link);
        if (cached != null) {
            return cached;
        }
        Base base = open(link);
        Profile p = base.profile;
        List<LinkRaster.Step> path = link.path();
        boolean[] keep = new boolean[path.size()];
        int[] level = new int[path.size()];
        boolean[] own = new boolean[path.size()];
        for (int i = p.start; i <= p.end; i++) {
            if (base.shared[i]) {
                LinkRaster.Step at = path.get(i);
                Crossing c = crossing(at.chunkX(), at.chunkZ());
                level[i] = c.level;
                own[i] = c.level == p.in[i];
            } else {
                level[i] = NO_LEVEL;
            }
        }
        int prev = p.start;
        int prevLevel = base.startLevel();
        for (int i = p.start; i <= p.end; i++) {
            if (level[i] == NO_LEVEL) {
                continue;
            }
            if (!own[i]) {
                int next = i + 1;
                while (next <= p.end && !own[next]) {
                    next++;
                }
                int nextLevel = next <= p.end ? level[next] : base.endLevel();
                int to = Math.min(next, p.end);
                if (Math.abs(level[i] - prevLevel) > base.room(prev, i)
                    || Math.abs(nextLevel - level[i]) > base.room(i, to)) {
                    continue;
                }
            }
            keep[i] = true;
            prev = i;
            prevLevel = level[i];
        }
        keeps.put(link, keep);
        return keep;
    }

    /** Whether every link at a crossing meets it at its level. */
    private boolean matched(Crossing c) {
        if (c.level == NO_LEVEL) {
            return false;
        }
        for (int k = 0; k < c.links.size(); k++) {
            if (!keeps(c.links.get(k))[c.indices.get(k)]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a link's profile with its matched crossings met: the base itself when it already meets them all, else
     * the ramps re-placed between the plazas and the matched crossings ({@link #replan}).
     */
    private Profile crossed(CityLinkGraph.Link link, Base base) {
        Profile p = base.profile;
        if (hits == null || !p.written()) {
            return p;
        }
        List<LinkRaster.Step> path = link.path();
        List<int[]> waypoints = new ArrayList<>();
        boolean moves = false;
        for (int i = p.start; i <= p.end; i++) {
            if (!base.shared[i]) {
                continue;
            }
            LinkRaster.Step at = path.get(i);
            Crossing c = crossing(at.chunkX(), at.chunkZ());
            if (c == NO_CROSSING || !matched(c)) {
                continue;
            }
            waypoints.add(new int[] { i, c.level });
            moves |= c.level != p.in[i];
        }
        if (!moves) {
            return p;
        }
        Base ready = open(link);
        Profile diagonal = replan(link, ready, waypoints, p.staircase);
        if (diagonal == null) {
            return p;
        }
        if (!p.staircase && staircase(link, ends, diagonal)) {
            Profile stairs = replan(link, ready, waypoints, true);
            if (stairs != null) {
                return stairs;
            }
        }
        return diagonal;
    }

    /**
     * Places a link's ramps between fixed levels (D-link-crossing-1): its plazas at the base's levels and each matched
     * crossing at the crossing's level. In each stretch between two of these, the ramps go as in {@link #levels},
     * diagonal chunks first, nearest the stretch's middle. Returns {@code null} when a stretch lacks the room, which
     * {@link #keeps} rules out for the base's own piece kinds.
     */
    private static Profile replan(CityLinkGraph.Link link, Base base, List<int[]> waypoints, boolean staircase) {
        Profile p = base.profile;
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        List<int[]> fixed = new ArrayList<>();
        fixed.add(new int[] { p.start, base.startLevel() });
        fixed.addAll(waypoints);
        fixed.add(new int[] { p.end, base.endLevel() });
        boolean[] ramp = new boolean[n];
        int[] step = new int[n];
        for (int k = 0; k + 1 < fixed.size(); k++) {
            int from = fixed.get(k)[0];
            int to = fixed.get(k + 1)[0];
            int delta = fixed.get(k + 1)[1] - fixed.get(k)[1];
            List<Integer> candidates = new ArrayList<>();
            for (int i = from + 1; i < to; i++) {
                if (rampable(path.get(i), staircase) && base.open[i]) {
                    candidates.add(i);
                }
            }
            if (candidates.size() < Math.abs(delta)) {
                return null;
            }
            sortRamps(path, candidates, from + to);
            for (int i = 0; i < Math.abs(delta); i++) {
                ramp[candidates.get(i)] = true;
                step[candidates.get(i)] = Integer.signum(delta);
            }
        }
        int[] in = new int[n];
        int[] out = new int[n];
        int level = base.startLevel();
        for (int i = p.start; i <= p.end; i++) {
            in[i] = level;
            if (ramp[i]) {
                level += step[i];
            }
            out[i] = level;
        }
        return new Profile(p.start, p.end, in, out, staircase);
    }

    /** Sorts ramp candidates: diagonal chunks first, then nearest the middle, then by index. */
    private static void sortRamps(List<LinkRaster.Step> path, List<Integer> candidates, int twiceMiddle) {
        candidates.sort((x, y) -> {
            boolean dx = path.get(x)
                .kind() == LinkRaster.Kind.DIAGONAL;
            boolean dy = path.get(y)
                .kind() == LinkRaster.Kind.DIAGONAL;
            if (dx != dy) {
                return dx ? -1 : 1;
            }
            int c = Integer.compare(Math.abs(2 * x - twiceMiddle), Math.abs(2 * y - twiceMiddle));
            return c != 0 ? c : Integer.compare(x, y);
        });
    }

    /** Whether another link's path crosses a chunk of this link's path; never without a graph (unit fixtures). */
    private boolean shared(CityLinkGraph.Link link, int x, int z) {
        if (hits == null) {
            return false;
        }
        for (CityLinkGraph.Hit hit : hits.hits(x, z)) {
            if (!hit.link()
                .equals(link)) {
                return true;
            }
        }
        return false;
    }

    /** Answers whether a chunk of a link's path is shared with another link. */
    interface Shared {

        boolean shared(int chunkX, int chunkZ);
    }

    static Profile compute(CityLinkGraph.Link link, Ends ends) {
        return compute(link, ends, (x, z) -> false);
    }

    static Profile compute(CityLinkGraph.Link link, Ends ends, Shared shared) {
        return compute(link, ends, shared, null);
    }

    /**
     * Computes a link's profile. With city highways (0017), a link of a beltway city starts or ends on the ring (the
     * path chunk at Chebyshev distance {@code half} from the center) and every link a city highway joins leaves that
     * city at the city highway's level.
     */
    static Profile compute(CityLinkGraph.Link link, Ends ends, Shared shared, CityHighways cities) {
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        int ra = ends.radius(link.ax(), link.az());
        int rb = ends.radius(link.bx(), link.bz());
        CityHighways.Plan pa = cities == null ? null : cities.plan(link.ax(), link.az());
        CityHighways.Plan pb = cities == null ? null : cities.plan(link.bx(), link.bz());
        int start = 0;
        while (start < n && enclosed(path.get(start), link, link.ax(), link.az(), ra, pa)) {
            start++;
        }
        int end = n - 1;
        while (end >= 0 && enclosed(path.get(end), link, link.bx(), link.bz(), rb, pb)) {
            end--;
        }
        if (start > end) {
            return new Profile(start, end, new int[n], new int[n], false);
        }
        // A city whose rim chunks fail the threshold leaves the edge plaza in open ground: the link runs on as a spur
        // to the first city chunk on its path (D391). A beltway end stops on the ring, which lies in the city; a link
        // the beltway does not join never runs on to the ring.
        if (!(pa != null && pa.kind() == CityHighways.Kind.BELTWAY && pa.joins(link))) {
            start = spur(
                path,
                start,
                -1,
                ends::city,
                (x, z) -> ends.blocked(x, z) || pa != null && pa.withinRing(x, z));
        }
        if (!(pb != null && pb.kind() == CityHighways.Kind.BELTWAY && pb.joins(link))) {
            end = spur(path, end, 1, ends::city, (x, z) -> ends.blocked(x, z) || pb != null && pb.withinRing(x, z));
        }
        int la = pa != null && pa.joins(link) ? pa.level() : ends.level(link.ax(), link.az());
        int lb = pb != null && pb.joins(link) ? pb.level() : ends.level(link.bx(), link.bz());
        Profile diagonal = levels(link, ends, start, end, false, shared, la, lb);
        return staircase(link, ends, diagonal) ? levels(link, ends, start, end, true, shared, la, lb) : diagonal;
    }

    /** A yes-or-no question about a chunk. */
    interface ChunkTest {

        boolean test(int chunkX, int chunkZ);
    }

    /**
     * Returns where a link's written range ends at a city (D391). {@code edge} is the path index of the first chunk
     * outside the city's radius, {@code inward} the index step toward the center (−1 at the lower end, +1 at the
     * upper). When the chunk inward of the edge is city, or there is none, the edge stays: its plaza touches the city.
     * Otherwise the range runs inward to the first city chunk on the path, which becomes the plaza, so the highway
     * reaches the city's streets instead of ending in open ground. A blocked chunk before any city chunk, or no city
     * chunk at all, keeps the edge.
     *
     * @param path    the link's path
     * @param edge    the first written index at this end, by radius
     * @param inward  −1 or +1
     * @param city    whether a chunk is city
     * @param blocked whether a chunk must not be written
     * @return the written range's index at this end
     */
    static int spur(List<LinkRaster.Step> path, int edge, int inward, ChunkTest city, ChunkTest blocked) {
        int k = edge + inward;
        if (k < 0 || k >= path.size()) {
            return edge;
        }
        LinkRaster.Step next = path.get(k);
        if (city.test(next.chunkX(), next.chunkZ())) {
            return edge;
        }
        for (; k >= 0 && k < path.size(); k += inward) {
            LinkRaster.Step step = path.get(k);
            if (blocked.test(step.chunkX(), step.chunkZ())) {
                return edge;
            }
            if (city.test(step.chunkX(), step.chunkZ())) {
                return k;
            }
        }
        return edge;
    }

    /**
     * Whether a path chunk is not written at an end: inside the ring of a beltway that joins the link, else inside the
     * city's radius. A link the beltway does not join (it lacks the ramp room, D348) stops at the city's edge
     * like any other, outside the ring.
     */
    static boolean enclosed(LinkRaster.Step step, CityLinkGraph.Link link, int cx, int cz, int radius,
        CityHighways.Plan plan) {
        if (plan != null && plan.kind() == CityHighways.Kind.BELTWAY && plan.joins(link)) {
            return plan.insideRing(step.chunkX(), step.chunkZ());
        }
        return inside(step, cx, cz, radius);
    }

    /**
     * Returns whether a link falls back to the staircase (0015 §3): some chunk its 45° deck covers, a diagonal or
     * transition chunk or a side chunk a corner spills into, would hold a bridge or a tunnel.
     */
    private static boolean staircase(CityLinkGraph.Link link, Ends ends, Profile p) {
        List<LinkRaster.Step> path = link.path();
        for (int i = p.start; i <= p.end; i++) {
            LinkRaster.Step step = path.get(i);
            boolean cornerIn = i > p.start && step.inDx() != 0 && step.inDz() != 0;
            boolean cornerOut = i < p.end && step.outDx() != 0 && step.outDz() != 0;
            if (!cornerIn && !cornerOut) {
                continue;
            }
            int low = Math.min(p.in[i], p.out[i]);
            int high = Math.max(p.in[i], p.out[i]);
            if (ends.bridgeOrTunnel(step.chunkX(), step.chunkZ(), low, high)) {
                return true;
            }
            if (cornerOut && (ends.bridgeOrTunnel(step.chunkX() + step.outDx(), step.chunkZ(), p.out[i], p.out[i])
                || ends.bridgeOrTunnel(step.chunkX(), step.chunkZ() + step.outDz(), p.out[i], p.out[i]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a link's ramps (class Javadoc). On the staircase the 45° chunks become flat bends, so only straight chunks
     * ramp.
     */
    private static Profile levels(CityLinkGraph.Link link, Ends ends, int start, int end, boolean staircase,
        Shared shared, int la, int lb) {
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        int[] in = new int[n];
        int[] out = new int[n];
        int need = Math.abs(lb - la);
        List<Integer> candidates = new ArrayList<>();
        for (int i = start + 1; i < end; i++) {
            LinkRaster.Kind kind = path.get(i)
                .kind();
            LinkRaster.Step at = path.get(i);
            if ((kind == LinkRaster.Kind.STRAIGHT || kind == LinkRaster.Kind.DIAGONAL && !staircase)
                && !shared.shared(at.chunkX(), at.chunkZ())) {
                candidates.add(i);
            }
        }
        sortRamps(path, candidates, start + end);
        boolean[] ramp = new boolean[n];
        for (int i = 0; i < Math.min(need, candidates.size()); i++) {
            ramp[candidates.get(i)] = true;
        }
        int direction = Integer.signum(lb - la);
        int level = la;
        for (int i = start; i <= end; i++) {
            in[i] = level;
            if (ramp[i]) {
                level += direction;
            }
            out[i] = level;
        }
        return new Profile(start, end, in, out, staircase);
    }

    static boolean inside(LinkRaster.Step step, int cx, int cz, int radius) {
        if (radius <= 0) {
            return false;
        }
        long dx = step.chunkX() - cx;
        long dz = step.chunkZ() - cz;
        return 256L * (dx * dx + dz * dz) <= (long) radius * radius;
    }

    /**
     * Merges the pieces several links write in one chunk (0015 §5). Identical pieces merge. All flat pieces at one
     * level
     * become one: a plaza when one is a plaza (it opens on all of their openings), else a straight piece, bend, T or
     * crossing when they open only on sides, else a {@link HighwayInfo.Shape#LINK_JUNCTION}. Matched crossings put
     * their links at one level first (class Javadoc, Crossings); ramps and pieces at other levels (an unmatched
     * crossing, a city highway) stay, lowest first, so the lower passes under.
     *
     * @param pieces the pieces, in any order
     * @return the merged pieces, lowest first
     */
    static List<HighwayInfo.Piece> merge(List<HighwayInfo.Piece> pieces) {
        List<HighwayInfo.Piece> out = new ArrayList<>();
        Map<Integer, List<HighwayInfo.Piece>> flat = new LinkedHashMap<>();
        for (HighwayInfo.Piece piece : pieces) {
            if (out.contains(piece)) {
                continue;
            }
            out.add(piece);
            if (piece.level() == piece.topLevel()) {
                flat.computeIfAbsent(piece.level(), k -> new ArrayList<>())
                    .add(piece);
            }
        }
        for (Map.Entry<Integer, List<HighwayInfo.Piece>> entry : flat.entrySet()) {
            List<HighwayInfo.Piece> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            out.removeAll(group);
            out.add(join(entry.getKey(), group));
        }
        out.sort((a, b) -> {
            int c = Integer.compare(a.level(), b.level());
            return c != 0 ? c : Integer.compare(a.topLevel(), b.topLevel());
        });
        return Collections.unmodifiableList(out);
    }

    /** Joins flat pieces at one level into one ({@link #merge}). */
    private static HighwayInfo.Piece join(int level, List<HighwayInfo.Piece> group) {
        List<Integer> steps = new ArrayList<>();
        boolean plaza = false;
        boolean corner = false;
        for (HighwayInfo.Piece p : group) {
            plaza |= p.shape() == HighwayInfo.Shape.CROSSING;
            int[] o = p.openings();
            for (int i = 0; i + 2 < o.length; i += 3) {
                boolean seen = false;
                for (int k = 0; k + 1 < steps.size(); k += 2) {
                    seen |= steps.get(k) == o[i] && steps.get(k + 1) == o[i + 1];
                }
                if (!seen) {
                    steps.add(o[i]);
                    steps.add(o[i + 1]);
                    corner |= o[i] != 0 && o[i + 1] != 0;
                }
            }
        }
        int[] array = new int[steps.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = steps.get(i);
        }
        if (plaza) {
            return HighwayInfo.Piece.plaza(level, array);
        }
        // One opening left (a city highway's end that its link leaves the same way, 0017) is a stub: a junction.
        return corner || array.length < 4 ? HighwayInfo.Piece.linkJunction(level, array)
            : HighwayInfo.Piece.sides(level, array);
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }

    private static <K, V> Map<K, V> lru(int capacity) {
        return new LinkedHashMap<K, V>(256, 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }
}
