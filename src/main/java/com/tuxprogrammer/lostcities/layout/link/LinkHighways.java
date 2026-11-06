package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;

/**
 * The highways of the city link network ({@code networkLayout=links}, design record 0015): one highway along every
 * link of a {@link CityLinkGraph}, as the {@link HighwayInfo} of each chunk.
 *
 * <p>
 * A link's path ({@link LinkRaster}) starts and ends at city centers. The chunks inside each end's city radius are not
 * written; the first chunk outside is a plaza (a crossing, 0013 §2) at that city's level, so traffic meets the city's
 * streets. Between the two plazas each chunk is a straight piece, a ramp, a diagonal or a transition
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
 * Where links share a chunk, identical pieces merge and all flat pieces at one level become one: a plaza, a straight
 * piece, bend, T or crossing, or a coded link junction ({@link #merge}); ramps and other levels are written lowest
 * first. A diagonal deck spills into the two side neighbors at each corner it opens on; those chunks report the spill
 * so buildings keep clear.
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
    private final Ends ends;
    private final Map<CityLinkGraph.Link, Profile> profiles = lru(CACHE);
    private final Map<Long, List<HighwayInfo.Piece>> own = lru(CACHE * 2);
    private final Map<Long, HighwayInfo> infos = lru(CACHE);

    /**
     * Creates the view.
     *
     * @param graph the link graph
     * @param ends  the city facts at link ends
     */
    public LinkHighways(CityLinkGraph graph, Ends ends) {
        this.graph = graph;
        this.ends = ends;
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
        for (CityLinkGraph.Hit hit : graph.hits(chunkX, chunkZ)) {
            HighwayInfo.Piece piece = piece(hit.link(), hit.index());
            if (piece != null) {
                out.add(piece);
            }
        }
        out.addAll(staircaseBends(chunkX, chunkZ));
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
            return HighwayInfo.Piece.plaza(profile.in[index], array);
        }
        return HighwayInfo.Piece.link(prevDx, prevDz, profile.in[index], nextDx, nextDz, profile.out[index]);
    }

    /**
     * Returns the bends a staircase link puts in a side chunk off its path (0015 §3): for a 45° step from P to Q, the
     * chunk one step along X from P, joining P's side to Q's side at the step's level.
     */
    private List<HighwayInfo.Piece> staircaseBends(int chunkX, int chunkZ) {
        List<HighwayInfo.Piece> out = new ArrayList<>();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (CityLinkGraph.Hit hit : graph.hits(chunkX - sx, chunkZ)) {
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
        // A ramp never shares its chunk with another link (0015 §5, D-p4b-15): decided from the paths alone, so it
        // does not depend on the other links' levels.
        Profile profile = compute(link, ends, (x, z) -> shared(link, x, z));
        profiles.put(link, profile);
        return profile;
    }

    /** Whether another link's path crosses a chunk of this link's path; never without a graph (unit fixtures). */
    private boolean shared(CityLinkGraph.Link link, int x, int z) {
        if (graph == null) {
            return false;
        }
        for (CityLinkGraph.Hit hit : graph.hits(x, z)) {
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
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        int ra = ends.radius(link.ax(), link.az());
        int rb = ends.radius(link.bx(), link.bz());
        int start = 0;
        while (start < n && inside(path.get(start), link.ax(), link.az(), ra)) {
            start++;
        }
        int end = n - 1;
        while (end >= 0 && inside(path.get(end), link.bx(), link.bz(), rb)) {
            end--;
        }
        if (start > end) {
            return new Profile(start, end, new int[n], new int[n], false);
        }
        Profile diagonal = levels(link, ends, start, end, false, shared);
        return staircase(link, ends, diagonal) ? levels(link, ends, start, end, true, shared) : diagonal;
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
        Shared shared) {
        List<LinkRaster.Step> path = link.path();
        int n = path.size();
        int[] in = new int[n];
        int[] out = new int[n];
        int la = ends.level(link.ax(), link.az());
        int lb = ends.level(link.bx(), link.bz());
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
        final int twiceMiddle = start + end;
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

    private static boolean inside(LinkRaster.Step step, int cx, int cz, int radius) {
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
     * crossing when they open only on sides, else a {@link HighwayInfo.Shape#LINK_JUNCTION}. Ramps and pieces at other
     * levels stay, lowest first, so the lower passes under.
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
        return corner ? HighwayInfo.Piece.linkJunction(level, array) : HighwayInfo.Piece.sides(level, array);
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
