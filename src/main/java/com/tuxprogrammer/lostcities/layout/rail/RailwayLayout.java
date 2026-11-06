package com.tuxprogrammer.lostcities.layout.rail;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Railway.java
// Avoids: HWY-01, HWY-08, HWY-15

import java.util.LinkedHashMap;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.ChunkKeys;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkRails;
import com.tuxprogrammer.lostcities.layout.random.QualityRandom;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;

/**
 * Upstream's railway grid: which chunks carry railway, of which type, at which level, with how many tracks and which
 * way they face. The grid repeats every {@code 2 × spacing} chunks with three-, two- and one-track station spots where
 * it meets a city, three-way splits, double bends and north-south lines; stations are at the surface or underground,
 * with approaches that descend to the railway level.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids HWY-01: the three-track spot's "city to the west" test looks west.</li>
 * <li>Avoids HWY-08 and HWY-15: the cache belongs to this world's layout, is bounded, and every value comes from this
 * world's one pinned profile.</li>
 * <li>Avoids CITY-19 (HWY-07) by never deleting a chunk's rail here: where a building blocks it
 * ({@code block_railway}), the writer leaves that one chunk out, which no neighbor reads.</li>
 * <li>Landmark cores (0006 §1, D30, D31 P06-4): a station spot in a core is no station; one in a margin is an
 * underground station without a staircase. A straight line whose part would reach within 8 blocks of a core chunk's
 * lowest ground dips under it: it descends before the core with upstream's descent parts (one or two levels a chunk,
 * by parity), runs under it at the depth the shallowest core chunk needs, and climbs back after it, all in chunks
 * outside the core. Where the line has no room for that before its junctions, or a junction or station itself would be
 * too shallow under a core, that core chunk carries no railway.</li>
 * </ul>
 *
 * <p>
 * City link network ({@code networkLayout=links}, 0015 §3): with a {@link CityLinkGraph} the grid is not used. Every
 * line follows a link ({@link LinkRails}) at the railway level from one city's center to another's, so no line ends in
 * the open whatever {@code railwaysCanEnd} says; each center a line ends at is an underground station along X (with
 * its staircase, except in a landmark margin), and every other chunk is a straight, bend, T or crossing written in
 * code ({@link RailChunk#coded()}).
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/railway-layout.md, docs/new-spec/06-highways-railways/index.md. Not
 * thread-safe; one per world. Side: both.
 */
public final class RailwayLayout {

    /** Per-chunk seed multipliers ({@code QualityRandom(seed + chunkZ × 2600003897 + chunkX × 43600002517)}). */
    static final long SEED_Z = 2600003897L;
    static final long SEED_X = 43600002517L;
    /** Surface-station chance and open versus roofed station (upstream's 0.5). */
    static final float SURFACE_CHANCE = 0.5f;
    /** Extension chance (upstream's 0.4). */
    static final float EXTENSION_CHANCE = 0.4f;
    /** Highest city level a surface station may stand at (upstream's {@code cityLevel > 2} test). */
    static final int MAX_SURFACE_STATION_LEVEL = 2;
    /** Blocks a railway part's top stays below a core chunk's lowest ground to pass under it (D30). */
    public static final int CORE_CLEARANCE = 8;
    /** The lowest block Y a railway's floor may stand at: above RWG's bedrock (P06-7). */
    public static final int MIN_RAIL_Y = 5;
    /** Blocks per city level. */
    static final int FLOOR_HEIGHT = 6;
    /** Entries kept in each cache: the generation front's rail chunks and the lines they read along. */
    static final int CACHE_SIZE = 512;
    /** Estimated bytes of one cache entry: the key, the rail chunk and the map entry. */
    static final int ENTRY_BYTES = 112;

    private final long seed;
    private final RailwaySettings settings;
    private final RailwayInputs inputs;
    private final int groundLevel;
    private final int partHeight6;
    private final Map<Long, RailChunk> flagged = lru(CACHE_SIZE);
    private final Map<Long, RailChunk> finals = lru(CACHE_SIZE);
    /** The lines of the city link network, or {@code null} on the legacy grid. */
    private final LinkRails links;

    /**
     * Builds the railway layout of one world.
     *
     * @param seed        the world seed
     * @param settings    the railway settings
     * @param inputs      what it reads
     * @param groundLevel the profile's ground level, block Y of city level 0
     * @param partHeight6 the world style's {@code railpartheight6}: a rail part's height in levels
     */
    public RailwayLayout(long seed, RailwaySettings settings, RailwayInputs inputs, int groundLevel, int partHeight6) {
        this(seed, settings, inputs, groundLevel, partHeight6, null);
    }

    /**
     * Builds the railway layout of one world, along a city link graph when one is given (0015 §3).
     *
     * @param seed        the world seed
     * @param settings    the railway settings
     * @param inputs      what it reads
     * @param groundLevel the profile's ground level, block Y of city level 0
     * @param partHeight6 the world style's {@code railpartheight6}: a rail part's height in levels
     * @param graph       the city link graph, or {@code null} for upstream's grid
     */
    public RailwayLayout(long seed, RailwaySettings settings, RailwayInputs inputs, int groundLevel, int partHeight6,
        CityLinkGraph graph) {
        this.seed = seed;
        this.settings = settings;
        this.inputs = inputs;
        this.groundLevel = groundLevel;
        this.partHeight6 = Math.max(1, partHeight6);
        // A core the railway level does not fit under keeps the whole line out (0013 §5, D-p4b-14).
        this.links = graph == null ? null
            : new LinkRails(
                graph,
                (x, z) -> inputs.isCore(x, z) && deepestFit(x, z) < settings.railwayLevel());
    }

    /** Returns the lines of the city link network, or {@code null} on upstream's grid. */
    public LinkRails links() {
        return links;
    }

    private static <V> Map<Long, V> lru(int capacity) {
        return new LinkedHashMap<Long, V>(1024, 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > capacity;
            }
        };
    }

    private static long key(int x, int z) {
        return ChunkKeys.of(x, z);
    }

    /**
     * Returns the most memory the layout's two caches can hold, estimated, in bytes, for budget B5.
     *
     * @return bytes
     */
    public long estimatedCacheBytes() {
        return 2L * CACHE_SIZE * ENTRY_BYTES;
    }

    /** Returns how many entries the two caches hold, for tests of their bound. */
    int cachedEntries() {
        return flagged.size() + finals.size();
    }

    /** Returns the settings. */
    public RailwaySettings settings() {
        return settings;
    }

    /** Returns the main railway's city level. */
    public int railwayLevel() {
        return settings.railwayLevel();
    }

    /**
     * Returns the railway in a chunk, with the rules for landmark cores applied.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the rail chunk, {@link RailChunk#NONE} when none
     */
    public RailChunk rail(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        RailChunk cached = finals.get(key);
        if (cached != null) {
            return cached;
        }
        RailChunk value = links != null ? linkRail(chunkX, chunkZ)
            : underCores(chunkX, chunkZ, grid(chunkX, chunkZ));
        finals.put(key, value);
        return value;
    }

    /** A chunk of the city link network's lines (class Javadoc). */
    private RailChunk linkRail(int chunkX, int chunkZ) {
        int bits = links.openings(chunkX, chunkZ);
        int sides = bits & LinkRails.SIDES;
        if (sides == 0 || !settings.enabled()) {
            return RailChunk.NONE;
        }
        int level = settings.railwayLevel();
        boolean alongX = (sides & (LinkRails.NORTH | LinkRails.SOUTH)) == 0;
        if ((bits & LinkRails.END) != 0 && alongX && settings.stations()) {
            return RailChunk.link(RailChunkType.STATION_UNDERGROUND, level, !inputs.isMargin(chunkX, chunkZ), sides);
        }
        RailChunkType type;
        if (sides == (LinkRails.EAST | LinkRails.WEST)) {
            type = RailChunkType.HORIZONTAL;
        } else if (sides == (LinkRails.NORTH | LinkRails.SOUTH)) {
            type = RailChunkType.VERTICAL;
        } else if (Integer.bitCount(sides) == 1) {
            type = RailChunkType.RAILS_END_HERE;
        } else if (Integer.bitCount(sides) == 2) {
            type = RailChunkType.DOUBLE_BEND;
        } else {
            type = RailChunkType.THREE_SPLIT;
        }
        return RailChunk.link(type, level, false, sides);
    }

    // ================================================================== upstream's grid

    /**
     * Returns upstream's rail chunk with its enable flags applied (upstream's {@code getRailChunkType}); neighbors on
     * a station's approach read each other through this.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the rail chunk
     */
    public RailChunk grid(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        RailChunk cached = flagged.get(key);
        if (cached != null) {
            return cached;
        }
        RailChunk info = compute(chunkX, chunkZ);
        if (info.type()
            .isStation() ? !settings.stations() : !settings.enabled()) {
            info = RailChunk.NONE;
        }
        flagged.put(key, info);
        return info;
    }

    /** A station spot's city: raw city, but never a landmark's core, where no station stands (D30). */
    private boolean stationCity(int chunkX, int chunkZ) {
        return inputs.isCityRaw(chunkX, chunkZ) && !inputs.isCore(chunkX, chunkZ);
    }

    private RailChunk compute(int chunkX, int chunkZ) {
        QualityRandom random = new QualityRandom(seed + chunkZ * SEED_Z + chunkX * SEED_X);
        int level = settings.railwayLevel();
        float r = random.nextFloat();
        int sx = settings.spacingEastWest();
        int sz = settings.spacingNorthSouth();
        int hx = sx / 2;
        // The +1 keeps upstream's grid origin.
        int mx = Math.floorMod(chunkX + 1, sx * 2);
        int mz = Math.floorMod(chunkZ + 1, sz * 2);
        if (mx == 0 && mz == sz) {
            if (!stationCity(chunkX, chunkZ)) {
                if (settings.canEnd()) {
                    boolean east = stationCity(chunkX + sx, chunkZ) || stationCity(chunkX + sx, chunkZ - sz)
                        || stationCity(chunkX + sx, chunkZ + sz);
                    // Avoids HWY-01: upstream's first west test repeated the east offset (+sx, 0).
                    boolean west = stationCity(chunkX - sx, chunkZ) || stationCity(chunkX - sx, chunkZ - sz)
                        || stationCity(chunkX - sx, chunkZ + sz);
                    RailChunk end = endOrLine(east, west, level, 3);
                    if (end != null) {
                        return end;
                    }
                }
                return new RailChunk(RailChunkType.HORIZONTAL, RailChunk.Direction.BI, level, 3);
            }
            return station(chunkX, chunkZ, r, 3, random.nextFloat() >= SURFACE_CHANCE);
        }
        if (mx == sx && mz == 0) {
            if (!stationCity(chunkX, chunkZ)) {
                if (settings.canEnd()) {
                    boolean east = stationCity(chunkX + sx, chunkZ - sz) || stationCity(chunkX + sx, chunkZ + sz);
                    boolean west = stationCity(chunkX - sx, chunkZ - sz) || stationCity(chunkX - sx, chunkZ + sz);
                    RailChunk end = endOrLine(east, west, level, 2);
                    if (end != null) {
                        return end;
                    }
                }
                return new RailChunk(RailChunkType.HORIZONTAL, RailChunk.Direction.BI, level, 2);
            }
            return station(chunkX, chunkZ, r, 2, random.nextFloat() >= SURFACE_CHANCE);
        }
        if (mx == sx && mz == sz) {
            if (!stationCity(chunkX, chunkZ)) {
                if (settings.canEnd()) {
                    boolean east = stationCity(chunkX + sx, chunkZ);
                    boolean west = stationCity(chunkX - sx, chunkZ);
                    RailChunk end = endOrLine(east, west, level, 1);
                    if (end != null) {
                        return end;
                    }
                }
                return new RailChunk(RailChunkType.HORIZONTAL, RailChunk.Direction.BI, level, 1);
            }
            return station(chunkX, chunkZ, r, 1, random.nextFloat() >= SURFACE_CHANCE);
        }
        if (mx == 0 && mz == 0) {
            return RailChunk.NONE;
        }
        if (mz == 0 || mz == sz) {
            return row(chunkX, chunkZ, r, mx, mz, level);
        }
        if (mx == hx || mx == sx + hx) {
            if (settings.canEnd()) {
                int toRow = mz % sz;
                RailChunk north = grid(chunkX, chunkZ - toRow);
                RailChunk south = grid(chunkX, chunkZ - toRow + sz);
                if (!north.isPresent() || !south.isPresent()) {
                    return RailChunk.NONE;
                }
            }
            return new RailChunk(
                RailChunkType.VERTICAL,
                mx == hx ? RailChunk.Direction.EAST : RailChunk.Direction.WEST,
                level,
                1);
        }
        return RailChunk.NONE;
    }

    /**
     * {@code railwaysCanEnd} at a station spot without a city: nothing with no city on either side, an end facing the
     * side that has one, or {@code null} for a line through when both have.
     */
    private static RailChunk endOrLine(boolean east, boolean west, int level, int tracks) {
        if (!east && !west) {
            return RailChunk.NONE;
        }
        if (!east) {
            return new RailChunk(RailChunkType.RAILS_END_HERE, RailChunk.Direction.WEST, level, tracks);
        }
        if (!west) {
            return new RailChunk(RailChunkType.RAILS_END_HERE, RailChunk.Direction.EAST, level, tracks);
        }
        return null;
    }

    /** The chunks of an east-west row: approaches either side of each station, double bends and splits. */
    private RailChunk row(int chunkX, int chunkZ, float r, int mx, int mz, int level) {
        int sx = settings.spacingEastWest();
        int sz = settings.spacingNorthSouth();
        int hx = sx / 2;
        if (mx >= sx + hx + 1 && mz != 0 || mx >= hx + 1 && mx < sx) {
            RailChunk adjacent = grid(chunkX + 1, chunkZ);
            RailChunk.Direction direction = adjacent.direction();
            if (direction == RailChunk.Direction.BI || adjacent.type() == RailChunkType.RAILS_END_HERE) {
                direction = RailChunk.Direction.WEST;
            }
            return adjacentRail(r, adjacent, direction, chunkX - 1, chunkZ);
        }
        if (mx >= 1 && mx < hx && mz != 0 || mx > sx && mx < sx + hx) {
            RailChunk adjacent = grid(chunkX - 1, chunkZ);
            RailChunk.Direction direction = adjacent.direction();
            if (direction == RailChunk.Direction.BI || adjacent.type() == RailChunkType.RAILS_END_HERE) {
                direction = RailChunk.Direction.EAST;
            }
            return adjacentRail(r, adjacent, direction, chunkX + 1, chunkZ);
        }
        if (mz == 0 && mx == hx) {
            if (settings.canEnd() && !stationCity(chunkX + hx, chunkZ)
                && !stationCity(chunkX - hx, chunkZ - sz)
                && !stationCity(chunkX - hx, chunkZ + sz)) {
                return RailChunk.NONE;
            }
            return new RailChunk(RailChunkType.DOUBLE_BEND, RailChunk.Direction.EAST, level, 1);
        }
        if (mz == 0 && mx == sx + hx) {
            if (settings.canEnd() && !stationCity(chunkX - hx, chunkZ)
                && !stationCity(chunkX + hx, chunkZ - sz)
                && !stationCity(chunkX + hx, chunkZ + sz)) {
                return RailChunk.NONE;
            }
            return new RailChunk(RailChunkType.DOUBLE_BEND, RailChunk.Direction.WEST, level, 1);
        }
        if (mz == sz && (mx == hx || mx == sx + hx)) {
            if (settings.canEnd() && !stationCity(chunkX + hx, chunkZ)
                && !stationCity(chunkX - hx, chunkZ)
                && !grid(chunkX, chunkZ - sz).isPresent()
                && !grid(chunkX, chunkZ + sz).isPresent()) {
                return RailChunk.NONE;
            }
            return new RailChunk(
                RailChunkType.THREE_SPLIT,
                mx == hx ? RailChunk.Direction.EAST : RailChunk.Direction.WEST,
                level,
                3);
        }
        return RailChunk.NONE;
    }

    /**
     * Upstream's {@code getStationType}: underground when the city is above level 2, too far above the railway,
     * surface stations are off, or a highway is at or below the city's level here or (north-south) beside it; else a
     * surface station at the city's level with probability 0.5. Deviation (D30): in a landmark's margin it is always
     * underground, and without a staircase, since the margin carries nothing the city builds.
     */
    private RailChunk station(int chunkX, int chunkZ, float r, int tracks, boolean roofed) {
        int cityLevel = inputs.cityLevel(chunkX, chunkZ);
        int level = settings.railwayLevel();
        int maximumDescent = settings.spacingEastWest() - 2;
        if (inputs.isMargin(chunkX, chunkZ)) {
            return new RailChunk(
                RailChunkType.STATION_UNDERGROUND,
                RailChunk.Direction.BI,
                level,
                tracks,
                null,
                false,
                level,
                false);
        }
        RailChunk underground = new RailChunk(RailChunkType.STATION_UNDERGROUND, RailChunk.Direction.BI, level, tracks);
        if (cityLevel > MAX_SURFACE_STATION_LEVEL || cityLevel - level > maximumDescent
            || !settings.surfaceStations()) {
            return underground;
        }
        if (atOrBelow(inputs.highwayXLevel(chunkX, chunkZ), cityLevel)
            || atOrBelow(inputs.highwayZLevel(chunkX, chunkZ), cityLevel)
            || atOrBelow(inputs.highwayZLevel(chunkX - 1, chunkZ), cityLevel)
            || atOrBelow(inputs.highwayZLevel(chunkX + 1, chunkZ), cityLevel)) {
            return underground;
        }
        if (r < SURFACE_CHANCE) {
            return new RailChunk(
                RailChunkType.STATION_SURFACE,
                RailChunk.Direction.BI,
                cityLevel,
                tracks,
                roofed,
                false,
                cityLevel,
                true);
        }
        return underground;
    }

    private static boolean atOrBelow(int highwayLevel, int cityLevel) {
        return highwayLevel != -1 && cityLevel >= highwayLevel;
    }

    /** Upstream's {@code testAdjacentRailChunk}: a chunk on a station's approach, from its station-side neighbor. */
    private RailChunk adjacentRail(float r, RailChunk adjacent, RailChunk.Direction direction, int nextX, int nextZ) {
        int level = settings.railwayLevel();
        switch (adjacent.type()) {
            case NONE:
                return RailChunk.NONE;
            case STATION_SURFACE: {
                int highwayX = inputs.highwayXLevel(nextX, nextZ);
                int highwayZ = inputs.highwayZLevel(nextX, nextZ);
                float draw = r;
                if (highwayX != -1 && adjacent.level() == highwayX || highwayZ != -1 && adjacent.level() == highwayZ) {
                    // A highway at the station's level in the next chunk: go down here already.
                    draw = 1;
                }
                // An extension takes one approach chunk; each slope after it descends at most two levels.
                int maximumDescentAfterExtension = settings.spacingEastWest() - 4;
                if (draw < EXTENSION_CHANCE && adjacent.level() - level <= maximumDescentAfterExtension) {
                    return adjacent.with(RailChunkType.STATION_EXTENSION_SURFACE, direction, adjacent.level());
                }
                return startDescending(adjacent, direction, level);
            }
            case STATION_UNDERGROUND:
                return r < EXTENSION_CHANCE
                    ? adjacent.with(RailChunkType.STATION_EXTENSION_UNDERGROUND, direction, adjacent.level())
                    : adjacent.with(RailChunkType.HORIZONTAL, direction, adjacent.level());
            case STATION_EXTENSION_SURFACE:
                return startDescending(adjacent, direction, level);
            case STATION_EXTENSION_UNDERGROUND:
                return adjacent.with(RailChunkType.HORIZONTAL, direction, adjacent.level());
            case GOING_DOWN_FURTHER:
            case GOING_DOWN_ONE_FROM_SURFACE:
            case GOING_DOWN_TWO_FROM_SURFACE:
                if (adjacent.level() == level) {
                    return adjacent.with(RailChunkType.HORIZONTAL, direction, adjacent.level());
                }
                return adjacent.with(RailChunkType.GOING_DOWN_FURTHER, direction, adjacent.level() - 2);
            case RAILS_END_HERE:
                return direction == adjacent.direction()
                    ? adjacent.with(RailChunkType.HORIZONTAL, direction, adjacent.level())
                    : RailChunk.NONE;
            case HORIZONTAL:
                return adjacent;
            default:
                // A split, bend or north-south line next to a station's approach cannot occur with even spacings of
                // 4 or more; upstream throws here, this port lays nothing.
                return RailChunk.NONE;
        }
    }

    private static RailChunk startDescending(RailChunk adjacent, RailChunk.Direction direction, int level) {
        int difference = adjacent.level() - level;
        if ((difference & 1) == 1) {
            return adjacent.with(RailChunkType.GOING_DOWN_ONE_FROM_SURFACE, direction, adjacent.level() - 1);
        }
        return adjacent.with(RailChunkType.GOING_DOWN_TWO_FROM_SURFACE, direction, adjacent.level() - 2);
    }

    // ================================================================== landmark cores

    /** The highest level whose part stays {@link #CORE_CLEARANCE} blocks under a core chunk's lowest ground. */
    private int deepestFit(int chunkX, int chunkZ) {
        int ground = inputs.lowestGround(chunkX, chunkZ);
        return Math.floorDiv(ground - CORE_CLEARANCE - partHeight6 * FLOOR_HEIGHT - groundLevel, FLOOR_HEIGHT);
    }

    /** The lowest level whose floor stays at or above {@link #MIN_RAIL_Y}. */
    private int lowestLevel() {
        return Math.floorDiv(MIN_RAIL_Y - groundLevel + FLOOR_HEIGHT - 1, FLOOR_HEIGHT);
    }

    private RailChunk underCores(int chunkX, int chunkZ, RailChunk info) {
        if (!info.isPresent()) {
            return info;
        }
        boolean straight = info.type() == RailChunkType.HORIZONTAL || info.type() == RailChunkType.VERTICAL;
        if (!straight) {
            // A station, junction or slope under a core it is too shallow for is left out.
            boolean shallow = inputs.isCore(chunkX, chunkZ) && deepestFit(chunkX, chunkZ) < info.topLevel();
            return shallow ? RailChunk.NONE : info;
        }
        boolean alongZ = info.type() == RailChunkType.VERTICAL;
        int stepX = alongZ ? 0 : 1;
        int stepZ = alongZ ? 1 : 0;
        int limit = 2 * Math.max(settings.spacingEastWest(), settings.spacingNorthSouth()) + 2;
        int back = 0;
        while (back < limit && sameLine(info, chunkX - stepX * (back + 1), chunkZ - stepZ * (back + 1))) {
            back++;
        }
        int ahead = 0;
        while (ahead < limit && sameLine(info, chunkX + stepX * (ahead + 1), chunkZ + stepZ * (ahead + 1))) {
            ahead++;
        }
        int length = back + ahead + 1;
        int startX = chunkX - stepX * back;
        int startZ = chunkZ - stepZ * back;
        int depth = Integer.MAX_VALUE;
        int first = -1;
        int last = -1;
        for (int k = 0; k < length; k++) {
            int x = startX + stepX * k;
            int z = startZ + stepZ * k;
            if (!inputs.isCore(x, z)) {
                continue;
            }
            int fit = deepestFit(x, z);
            if (fit < info.level()) {
                depth = Math.min(depth, fit);
                first = first < 0 ? k : first;
                last = k;
            }
        }
        if (first < 0) {
            return info;
        }
        int here = back;
        int dip = info.level() - Math.max(depth, lowestLevel());
        int slopes = (dip + 1) / 2;
        boolean fits = depth >= lowestLevel() && first - slopes >= 0 && last + slopes <= length - 1;
        for (int k = first - slopes; fits && k <= last + slopes; k++) {
            if ((k < first || k > last) && inputs.isCore(startX + stepX * k, startZ + stepZ * k)) {
                fits = false;
            }
        }
        if (!fits) {
            // No room to dip before the line's junctions: the shallow core chunks carry no railway.
            boolean shallow = here >= first && here <= last
                && inputs.isCore(chunkX, chunkZ)
                && deepestFit(chunkX, chunkZ) < info.level();
            return shallow ? RailChunk.NONE : info;
        }
        if (here < first - slopes || here > last + slopes) {
            return info;
        }
        int bottom = info.level() - dip;
        if (here >= first && here <= last) {
            return new RailChunk(info.type(), info.direction(), bottom, info.tracks());
        }
        // A slope: t counts the slopes from the line's own level, from the outer end inward.
        int t = here < first ? here - (first - slopes) : (last + slopes) - here;
        int firstStep = (dip & 1) == 1 ? 1 : 2;
        int upper = info.level() - (t == 0 ? 0 : firstStep + 2 * (t - 1));
        int step = t == 0 ? firstStep : 2;
        int lower = upper - step;
        RailChunkType type = step == 1 ? RailChunkType.GOING_DOWN_ONE_FROM_SURFACE : RailChunkType.GOING_DOWN_FURTHER;
        // The low end faces the core: toward +X or +Z before it, toward −X or −Z after it.
        RailChunk.Direction low = here < first ? RailChunk.Direction.EAST : RailChunk.Direction.WEST;
        return new RailChunk(type, low, lower, info.tracks(), null, alongZ, upper, true);
    }

    private boolean sameLine(RailChunk info, int chunkX, int chunkZ) {
        RailChunk other = grid(chunkX, chunkZ);
        return other.type() == info.type() && other.level() == info.level();
    }
}
