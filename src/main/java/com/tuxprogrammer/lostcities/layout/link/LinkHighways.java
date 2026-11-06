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
 * Where links share a chunk, identical pieces merge, two plazas at one level merge, and a straight piece along each
 * axis at one level becomes a crossing; other pieces are all written, lowest first. A diagonal deck spills into the two
 * side neighbors at each corner it opens on; those chunks report the spill so buildings keep clear.
 *
 * <p>
 * Pure: reads only the graph and the {@link Ends} oracle (terrain-derived), so it answers the same in any order and
 * never loads a chunk. Caches are bounded LRUs; methods are synchronized.
 */
public final class LinkHighways {

    /** City facts at a link's ends. */
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
    }

    /**
     * A link's written range and level profile.
     */
    public static final class Profile {

        private final int start;
        private final int end;
        private final int[] in;
        private final int[] out;

        Profile(int start, int end, int[] in, int[] out) {
            this.start = start;
            this.end = end;
            this.in = in;
            this.out = out;
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
        if (index == profile.start || index == profile.end) {
            List<Integer> steps = new ArrayList<>();
            if (index < profile.end) {
                steps.add(step.outDx());
                steps.add(step.outDz());
            }
            if (index > profile.start) {
                steps.add(-step.inDx());
                steps.add(-step.inDz());
            }
            int[] array = new int[steps.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = steps.get(i);
            }
            return HighwayInfo.Piece.plaza(profile.in[index], array);
        }
        return HighwayInfo.Piece
            .link(-step.inDx(), -step.inDz(), profile.in[index], step.outDx(), step.outDz(), profile.out[index]);
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
        Profile profile = compute(link, ends);
        profiles.put(link, profile);
        return profile;
    }

    static Profile compute(CityLinkGraph.Link link, Ends ends) {
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
        int[] in = new int[n];
        int[] out = new int[n];
        if (start > end) {
            return new Profile(start, end, in, out);
        }
        int la = ends.level(link.ax(), link.az());
        int lb = ends.level(link.bx(), link.bz());
        int need = Math.abs(lb - la);
        List<Integer> candidates = new ArrayList<>();
        for (int i = start + 1; i < end; i++) {
            LinkRaster.Kind kind = path.get(i)
                .kind();
            if (kind == LinkRaster.Kind.DIAGONAL || kind == LinkRaster.Kind.STRAIGHT) {
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
        return new Profile(start, end, in, out);
    }

    private static boolean inside(LinkRaster.Step step, int cx, int cz, int radius) {
        if (radius <= 0) {
            return false;
        }
        long dx = step.chunkX() - cx;
        long dz = step.chunkZ() - cz;
        return 256L * (dx * dx + dz * dz) <= (long) radius * radius;
    }

    static List<HighwayInfo.Piece> merge(List<HighwayInfo.Piece> pieces) {
        List<HighwayInfo.Piece> out = new ArrayList<>();
        for (HighwayInfo.Piece piece : pieces) {
            if (!out.contains(piece)) {
                out.add(piece);
            }
        }
        // Plazas at one level, or a straight piece on each axis at one level, become one crossing.
        boolean changed = true;
        while (changed) {
            changed = false;
            outer: for (int i = 0; i < out.size(); i++) {
                for (int j = i + 1; j < out.size(); j++) {
                    HighwayInfo.Piece joined = join(out.get(i), out.get(j));
                    if (joined != null) {
                        out.remove(j);
                        out.set(i, joined);
                        changed = true;
                        break outer;
                    }
                }
            }
        }
        out.sort((a, b) -> {
            int c = Integer.compare(a.level(), b.level());
            return c != 0 ? c : Integer.compare(a.topLevel(), b.topLevel());
        });
        return Collections.unmodifiableList(out);
    }

    private static HighwayInfo.Piece join(HighwayInfo.Piece a, HighwayInfo.Piece b) {
        boolean aFlat = a.level() == a.topLevel();
        boolean bFlat = b.level() == b.topLevel();
        if (!aFlat || !bFlat || a.level() != b.level()) {
            return null;
        }
        boolean aJoins = a.shape() == HighwayInfo.Shape.CROSSING || a.shape() == HighwayInfo.Shape.STRAIGHT;
        boolean bJoins = b.shape() == HighwayInfo.Shape.CROSSING || b.shape() == HighwayInfo.Shape.STRAIGHT;
        if (!aJoins || !bJoins) {
            return null;
        }
        boolean crossing = a.shape() == HighwayInfo.Shape.CROSSING || b.shape() == HighwayInfo.Shape.CROSSING
            || a.axis() != b.axis();
        if (!crossing) {
            return null;
        }
        List<Integer> steps = new ArrayList<>();
        for (HighwayInfo.Piece p : new HighwayInfo.Piece[] { a, b }) {
            int[] o = p.openings();
            for (int i = 0; i + 2 < o.length; i += 3) {
                boolean seen = false;
                for (int k = 0; k + 1 < steps.size(); k += 2) {
                    seen |= steps.get(k) == o[i] && steps.get(k + 1) == o[i + 1];
                }
                if (!seen) {
                    steps.add(o[i]);
                    steps.add(o[i + 1]);
                }
            }
        }
        int[] array = new int[steps.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = steps.get(i);
        }
        return HighwayInfo.Piece.plaza(a.level(), array);
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
