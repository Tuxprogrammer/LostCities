package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java
// Avoids: CITY-10, CITY-11, TERR-07, PROF-06, CITY-15, CITY-16, CITY-17, CITY-18, CITY-19, CITY-20, CITY-21, TERR-24,
// CITY-24, CITY-35, CITY-36, LOOT-10

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedBuilding;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedIndex;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.random.QualityRandom;
import com.tuxprogrammer.lostcities.layout.street.EffectiveStreetResolver;
import com.tuxprogrammer.lostcities.layout.street.HierarchicalBridgePlanner;
import com.tuxprogrammer.lostcities.layout.street.HierarchicalStreetPlanner;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.layout.street.PlannedBridgeInfo;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.layout.street.PlannedStreetInfo;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;
import com.tuxprogrammer.lostcities.layout.view.AssetView;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.FacadeTone;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;
import com.tuxprogrammer.lostcities.layout.view.PartContext;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;
import com.tuxprogrammer.lostcities.layout.view.Selection;
import com.tuxprogrammer.lostcities.layout.view.StyleView;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.layout.view.WorldStyleView;

/**
 * One dimension's city layout: what each chunk holds, as a pure function of the world seed, the chunk coordinates,
 * the pinned profile, the assets, the terrain oracle and the structure oracle. It is the entry point the terrain and
 * chunk writers (M04, M05) call: {@link #plan(int, int)} for a chunk's {@link ChunkPlan}, {@link #layout(int, int)} for
 * the plan with every neighbor-dependent answer the writer needs, and the individual queries below.
 *
 * <p>
 * Stages, each cached per chunk in a bounded cache this planner owns, with no time expiry and no locks (one server
 * thread): the landmark zone and sewing role, the city level, the characteristics (upstream's
 * {@code computeChunkCharacteristics}) and the plan (upstream's {@code BuildingInfo} constructor). Every value is final
 * once computed; queries never write (decision log D9).
 *
 * <p>
 * Fixes of upstream's {@code BuildingInfo}, each with a regression test:
 * <ul>
 * <li>CITY-10: the plan continues the characteristics stage's building random instead of reseeding it, so the first
 * prefers-lonely draw is not the building-chance draw again (P04-2).</li>
 * <li>CITY-11: only a neighbor that could hold a building makes a building prefer to be lonely.</li>
 * <li>CITY-12: {@link StreetType#FULL} is reachable, as a plaza facing a landmark.</li>
 * <li>CITY-13, CITY-18, CITY-19, CITY-20: plans are immutable and final; floors and cellars are fitted once; rails are
 * never deleted from another layout's cache; no provisional plans exist.</li>
 * <li>CITY-14: a floor-count bound below 1 is clamped to 1.</li>
 * <li>CITY-15: the height cap stops at 0 floors, and a chunk the cap leaves without floors holds no building.</li>
 * <li>CITY-16: cellars are drawn uniformly between the minimum and the maximum.</li>
 * <li>CITY-17: a building whose parts match no condition is reported and left out, never thrown.</li>
 * <li>CITY-21: street-majority ties go to the chunk's own style, then to the first name (P04-3).</li>
 * <li>CITY-22: corner targets carry an explicit "constrained" flag ({@link CornerHeight}).</li>
 * <li>CITY-24: structure avoidance is replaced by landmark zones, which are always known.</li>
 * <li>CITY-35: every internal wall of a multibuilding may get random doorways (P04-4).</li>
 * <li>CITY-36: west and north passages check the neighbor's {@code dontconnect} too.</li>
 * </ul>
 *
 * <p>
 * Only the hierarchical street mode is ported, so upstream's legacy street type, park chance and legacy bridges are
 * not (D11); their random draws are kept where the hierarchical mode makes them, so the stream matches upstream's.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/ (chunk-planner, chunk-characteristics, building-info, streets-parks-stairs,
 * bridges, corridors, doors-and-connections), docs/new-spec/04-city-layout/. Side: server.
 */
public final class LayoutPlanner implements HierarchicalBridgePlanner.Context {

    /** Building random seed multipliers (with the world seed added). */
    static final long BUILDING_Z = 341873128712L;
    static final long BUILDING_X = 132897987541L;
    /** "LARGEBRG": salt of the separate large-bridge random. */
    static final long LARGE_BRIDGE_SALT = 0x4c41524745425247L;
    /** Salt of a building's palette-variety seed ("VARIETY "), distinct from every other per-chunk draw. */
    static final long VARIETY_SALT = 0x5641524945545920L;
    /** Salt of a street or park block's palette-variety seed ("STREETS "). */
    static final long STREET_VARIETY_SALT = 0x5354524545545320L;
    /** Streets and parks share their variety per 2^2 = 4 chunks on each axis. */
    static final int STREET_VARIETY_SHIFT = 2;
    /** Salt of the separate random a capped building's roof part is drawn from (decision log D49). */
    static final long ROOF_SALT = 0x524f4f4643415050L;
    /** Highest floor block Y a building's roof part may start below: 1.7.10's {@code 256 - 2 - 6}. */
    static final int MAX_ROOF_FLOOR_Y = 248;
    /** The lowest cellar's floor must stay above this block Y: 1.7.10's {@code 0 + 2}. */
    static final int MIN_BOTTOM_Y = 2;
    /** Longest corridor walk, in chunks, before the walk gives up. */
    static final int MAX_CORRIDOR_WALK = 512;
    /** Salt of the separate random the taper's high-rise core draws from (0011 §2). */
    static final long TAPER_SALT = 0x5441504552494e47L;
    /** Salt of the separate random edge lots draw from (0011 §3). */
    static final int RING_ATTEMPTS = 6;
    static final long RING_SALT = 0x52494e4753504c54L;
    static final long LOT_SALT = 0x45444745_4c4f5453L;
    /** Intensity below which a chunk may become an edge lot (0011 §3). */
    static final float LOT_INTENSITY = 0.4f;
    /** An open lot's chance of becoming an edge lot, relative to a building chunk's. */
    static final float OPEN_LOT_SHARE = 0.5f;
    /** Weights of a lot's kinds: bare, pit, foundation. */
    static final float[] LOT_KIND_WEIGHTS = { 0.35f, 0.35f, 0.30f };
    /** A dug lot's depths, in blocks. */
    static final int[] LOT_DEPTHS = { 1, 2, 4 };
    /** The weights of {@link #LOT_DEPTHS}. */
    static final float[] LOT_DEPTH_WEIGHTS = { 0.30f, 0.35f, 0.35f };
    /** Most floors a foundation's columns rise above city ground. */
    static final int LOT_MAX_COLUMN_FLOORS = 3;

    final long seed;
    final DimensionKey dimension;
    final LayoutProfile profile;
    final AssetView assets;
    final WorldStyleView worldStyle;
    final TerrainView terrain;
    final InfrastructureView infrastructure;
    final Networks networks;
    final LayoutLog log;
    final HierarchicalStreetPlanner streets;
    final HierarchicalBridgePlanner bridges;
    final LandmarkZones zones;
    final ActivePredefined predefined;
    final CityMap cities;
    final CityLevels levels;
    final Sewing sewing;
    final Urbanity urbanity;
    final FacadePlanner facades;
    private final ChunkCache<boolean[]> components;
    private final ChunkCache<MultiChunk> multiChunks;
    private final ChunkCache<ChunkCharacteristics> characteristicsCache;
    private final ChunkCache<ChunkPlan> plans;
    private final ChunkCache<Derived> derived;
    private final ChunkCache<Optional<ConstructionLots.Draw>> lotCells;
    private final ChunkCache<LotSite> lotSites;
    private final Set<String> warned = new HashSet<>();

    /**
     * Builds the planner of one dimension.
     *
     * @param inputs everything the layout is a function of
     */
    public LayoutPlanner(LayoutInputs inputs) {
        this.seed = inputs.seed;
        this.dimension = inputs.dimension;
        this.profile = inputs.profile;
        this.assets = inputs.assets;
        this.worldStyle = inputs.assets.worldStyle();
        this.terrain = inputs.terrain;
        this.log = inputs.log;
        int cache = inputs.cacheSize;
        this.streets = new HierarchicalStreetPlanner(seed, dimension, profile.streets);
        this.bridges = new HierarchicalBridgePlanner(
            seed,
            dimension,
            streets,
            profile.plannedPrimaryBridgeChance,
            profile.plannedPrimaryBridgeMaxLength);
        this.zones = new LandmarkZones(inputs.structures, profile, cache);
        this.predefined = new ActivePredefined(inputs.predefined, zones, this);
        this.cities = new CityMap(seed, profile, worldStyle, terrain, predefined, cache);
        this.levels = new CityLevels(profile, terrain, cities, cache);
        this.sewing = new Sewing(profile, cities, zones, streets, predefined, cache);
        // M06: the networks read the city map, levels and zones built above, never a plan.
        this.networks = inputs.networks == null ? null : new Networks(this, inputs.networks);
        this.infrastructure = networks == null ? inputs.infrastructure : networks;
        this.urbanity = new Urbanity(seed, profile, cities, this::isCityForRoads, cache);
        this.facades = new FacadePlanner(this, cache);
        this.multiChunks = new ChunkCache<>(Math.max(64, cache / 64));
        this.characteristicsCache = new ChunkCache<>(cache);
        this.components = new ChunkCache<>(Math.max(16, Math.min(cache, 256)));
        this.plans = new ChunkCache<>(cache);
        this.derived = new ChunkCache<>(cache);
        this.lotCells = new ChunkCache<>(Math.max(64, cache / 4));
        this.lotSites = new ChunkCache<>(Math.max(64, cache / 4));
    }

    // ================================================================== public stages

    /** Returns the profile the layout reads (M09: the world's pinned profile). */
    public LayoutProfile profile() {
        return profile;
    }

    /**
     * Returns the world's highway and railway networks, or {@code null} when the inputs gave no network settings (the
     * layout then reads {@link LayoutInputs}' infrastructure view).
     *
     * @return the networks, or {@code null}
     */
    public Networks networks() {
        return networks;
    }

    /** Returns the street field (raw roads) of this dimension. */
    public HierarchicalStreetPlanner streets() {
        return streets;
    }

    /**
     * Returns a chunk's landmark zone.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the zone
     */
    public ZoneInfo zone(int chunkX, int chunkZ) {
        return zones.zone(chunkX, chunkZ);
    }

    /**
     * Returns a chunk's city factor, 0 to 1.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the factor
     */
    public float cityFactor(int chunkX, int chunkZ) {
        return cities.cityFactor(chunkX, chunkZ);
    }

    /**
     * Returns a chunk's urban intensity: 0 at a city's very edge (and outside cities) to 1 in its dense core
     * (docs/design/0011-city-edges-and-character.md §1).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the intensity
     */
    public float intensity(int chunkX, int chunkZ) {
        return urbanity.intensity(chunkX, chunkZ);
    }

    /**
     * Returns the facade tone the facade stage chose for the building unit whose top-left chunk is given, or
     * {@code null} (docs/design/0012-facade-color-feathering.md).
     *
     * @param topX top-left chunk X
     * @param topZ top-left chunk Z
     * @return the tone, or {@code null}
     */
    public FacadeTone facadeTone(int topX, int topZ) {
        return facades.active() ? facades.tone(topX, topZ) : null;
    }

    /**
     * Returns whether a chunk is raw city (factor above the threshold), before landmark zones.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the chunk is raw city
     */
    @Override
    public boolean isCityRaw(int chunkX, int chunkZ) {
        return cities.isCityRaw(chunkX, chunkZ);
    }

    /**
     * Returns whether a chunk is a city center (center mode).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether a city is centered here
     */
    public boolean isCityCenter(int chunkX, int chunkZ) {
        return cities.isCityCenter(chunkX, chunkZ);
    }

    /**
     * Returns the city centers whose grid cells intersect a chunk window, or {@code null} when this profile places
     * centers by chance ({@code cityPlacement=chance}). Pure layout: no terrain, no filters, O(cells). Predefined
     * cities are not listed. Whether a center yields a city is {@link #centerYieldsCity}.
     *
     * @param minChunkX first chunk X of the window
     * @param minChunkZ first chunk Z
     * @param maxChunkX last chunk X (inclusive)
     * @param maxChunkZ last chunk Z (inclusive)
     * @return the centers, or {@code null} outside grid mode
     */
    public java.util.List<CityGrid.Center> gridCenters(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        CityGrid grid = cities.grid();
        return grid == null ? null : grid.centersInCellsOf(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
    }

    /**
     * Returns whether a city center actually yields a city after the terrain filters: its chunk is a city chunk (the
     * height window, {@code cityAvoidOcean}, the rise cap, {@code cityConnected} and the city mask all applied). This
     * reads the terrain and zone oracles; a pure layout test uses {@link #gridCenters} instead.
     *
     * @param chunkX the center's chunk X
     * @param chunkZ the center's chunk Z
     * @return whether the center is a center and its chunk is city
     */
    public boolean centerYieldsCity(int chunkX, int chunkZ) {
        return cities.isCityCenter(chunkX, chunkZ) && characteristics(chunkX, chunkZ).isCity;
    }

    /**
     * Returns the radius, in blocks, of the city centered on a chunk; meaningful only for a {@link #isCityCenter}.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the radius
     */
    public float cityRadius(int chunkX, int chunkZ) {
        return cities.cityRadius(chunkX, chunkZ);
    }

    /**
     * Returns a chunk's city level, 0 to 8, from the height oracle (before multibuilding averaging).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level
     */
    public int cityLevel(int chunkX, int chunkZ) {
        return levels.cityLevel(chunkX, chunkZ);
    }

    /**
     * Returns a chunk's characteristics.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the characteristics
     */
    public ChunkCharacteristics characteristics(int chunkX, int chunkZ) {
        ChunkCharacteristics cached = characteristicsCache.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return characteristicsCache.put(chunkX, chunkZ, computeCharacteristics(chunkX, chunkZ));
    }

    /**
     * Returns a chunk's plan.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the plan
     */
    public ChunkPlan plan(int chunkX, int chunkZ) {
        ChunkPlan cached = plans.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return plans.put(chunkX, chunkZ, computePlan(chunkX, chunkZ));
    }

    /**
     * Returns a chunk's plan with every answer that depends on its neighbors: what a chunk writer needs.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the chunk layout
     */
    public ChunkLayout layout(int chunkX, int chunkZ) {
        return new ChunkLayout(this, chunkX, chunkZ);
    }

    /** Returns the highways and railways the layout plans around (M06's, or none). */
    public InfrastructureView infrastructure() {
        return infrastructure;
    }

    /**
     * Picks the levels of a building forced into a chunk, as upstream's {@code BuildingInfo.setBuildingType} does for
     * {@code /lostcities createbuilding}: one part and one overlay part per level, from the lowest cellar up, drawn
     * from a fresh building random of the chunk, with the chunk's city level. The chunk's cached plan is read, never
     * changed (avoids CORE-32), so generation and other queries keep seeing the planned chunk.
     *
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param building the building's name
     * @param floors   floors above ground, at least 1
     * @param cellars  cellars, at least 0
     * @return the levels, or {@code null} when the building is unknown or a level matches no part
     */
    public ForcedBuilding forcedBuilding(int chunkX, int chunkZ, String building, int floors, int cellars) {
        BuildingView view = assets.building(building);
        if (view == null || floors < 1 || cellars < 0) {
            return null;
        }
        int cityLevel = characteristics(chunkX, chunkZ).cityLevel;
        QualityRandom random = buildingRandom(chunkX, chunkZ);
        int levels = floors + cellars + 1;
        String[] parts = new String[levels];
        String[] parts2 = new String[levels];
        String below = PartContext.NONE;
        for (int i = 0; i < levels; i++) {
            int floor = i - cellars;
            String part = view.pickPart(
                random,
                new PartContext(
                    cityLevel + floor,
                    floor,
                    cellars,
                    floors,
                    PartContext.NONE,
                    below,
                    view.name(),
                    chunkX,
                    chunkZ));
            if (part == null) {
                return null;
            }
            parts[i] = part;
            below = part;
            parts2[i] = view.pickPart2(
                random,
                new PartContext(cityLevel + floor, floor, cellars, floors, part, below, view.name(), chunkX, chunkZ));
        }
        return new ForcedBuilding(view.name(), floors, cellars, parts, parts2);
    }

    // ================================================================== characteristics

    private ChunkCharacteristics computeCharacteristics(int x, int z) {
        ZoneInfo zone = zones.zone(x, z);
        LaneCell lane = sewing.cell(x, z);
        // Deviation (0006 §1): a core or avoid-ring chunk is never city; this replaces upstream's avoidance.
        boolean isCity = isCityBase(x, z, zone) && connectedToCenter(x, z);
        MultiPos pos = MultiPos.SINGLE;
        String multi = null;
        if (isCity) {
            PredefinedIndex.Occupant occupant = predefined.occupant(x, z);
            if (occupant != null) {
                if (!occupant.isStreet() && occupant.building()
                    .multi()) {
                    MultiBuildingView view = assets.multiBuilding(
                        occupant.building()
                            .building());
                    if (view != null) {
                        pos = new MultiPos(occupant.offsetX(), occupant.offsetZ(), view.dimX(), view.dimZ());
                        multi = view.name();
                    }
                }
            } else {
                MultiChunk.Placement placement = multiChunk(x, z).placementAt(x, z);
                if (placement != null) {
                    pos = new MultiPos(placement.offsetX, placement.offsetZ, placement.dimX, placement.dimZ);
                    multi = placement.name;
                }
            }
        }
        int cityLevel = pos.isSingle() ? levels.cityLevel(x, z)
            : MultiChunk.footprintLevel(this, x - pos.x(), z - pos.z(), pos.width(), pos.height());
        QualityRandom random = buildingRandom(x, z);
        PlannedStreetInfo raw = streets.getStreetInfo(x, z);
        PlannedRoadType planned = effectiveRoad(x, z, isCity, raw, pos);
        if (isCity && lane.role()
            .forcesRoad()) {
            // Deviation (0006 §3, 0011 §5): detours and village lanes are small roads around the old town.
            planned = lane.role() == LaneRole.DETOUR ? PlannedRoadType.TERTIARY
                : PlannedRoadType.strongest(planned, PlannedRoadType.TERTIARY);
        }
        if (planned == PlannedRoadType.PRIMARY && touchesMargin(x, z)) {
            // Deviation (0011 §5, AR-7, AR-10): no wide road beside a landmark's margin; it narrows to a small one.
            planned = PlannedRoadType.TERTIARY;
        }
        boolean could = isCity && checkBuildingPossibility(x, z, pos, cityLevel, planned, random, zone);
        // Zone rules, after upstream's draw (landmark-zones.md, "Random consumption").
        if (could && pos.isSingle() && !oneFloorFits(cityLevel, zone.heightCap())) {
            could = false;
        }
        String style = isCity && !could ? majorityStyle(x, z) : cities.cityStyleName(x, z);
        String building = candidateBuilding(x, z, pos, multi, style, random);
        if (isCity && profile.densityRings > 0 && pos.isSingle() && predefined.buildingAtTopLeft(x, z) == null) {
            building = ringBuilding(x, z, style, building);
        }
        return new ChunkCharacteristics(
            x,
            z,
            zone,
            lane,
            isCity,
            pos,
            multi,
            cityLevel,
            raw.roadType(),
            planned,
            could,
            style,
            building,
            random.drawCount());
    }

    /**
     * Whether a chunk is city before the connection rule: raw city, outside every core and avoid-ring, not cratered.
     */
    private boolean isCityBase(int x, int z, ZoneInfo zone) {
        return cities.isCityRaw(x, z) && !zone.zone()
            .forcesNonCity() && !underCrater(x, z, zone);
    }

    /**
     * Deviation (cityConnected): whether a city chunk joins the center of a city that reaches it, edge to edge
     * through other city chunks. A chunk that terrain, water or a landmark cut off stands alone otherwise, a stray
     * street or building with nothing around it. Applies in center mode only; a chunk no center in range reaches (a
     * distant predefined city's) and predefined content always pass. Pure in the seed, the terrain and the zones, so
     * every chunk agrees, and bounded to the search square of each center.
     */
    private boolean connectedToCenter(int x, int z) {
        if (!profile.cityConnected || cities.perlinMode() || predefined.occupant(x, z) != null) {
            return true;
        }
        int r = cities.searchRadius();
        boolean reached = false;
        java.util.List<int[]> near = cities.gridCentersNear(x, z);
        if (near != null) {
            for (int[] c : near) {
                if (inComponent(c[0], c[1], x, z, r)) {
                    return true;
                }
            }
            return near.isEmpty();
        }
        for (int cz = z - r; cz <= z + r; cz++) {
            for (int cx = x - r; cx <= x + r; cx++) {
                if (!cities.isCityCenter(cx, cz)) {
                    continue;
                }
                reached = true;
                if (inComponent(cx, cz, x, z, r)) {
                    return true;
                }
            }
        }
        return !reached;
    }

    private boolean inComponent(int cx, int cz, int x, int z, int r) {
        boolean[] member = component(cx, cz);
        int side = 2 * r + 1;
        int ix = x - cx + r;
        int iz = z - cz + r;
        return ix >= 0 && iz >= 0 && ix < side && iz < side && member[iz * side + ix];
    }

    /**
     * The chunks of the square around a city center that join the center through base city chunks, 4-connected. When
     * the center chunk itself is not city (water, a landmark), the walk starts from the nearest base city chunk.
     */
    private boolean[] component(int cx, int cz) {
        boolean[] cached = components.get(cx, cz);
        if (cached != null) {
            return cached;
        }
        int r = cities.searchRadius();
        int side = 2 * r + 1;
        boolean[] seen = new boolean[side * side];
        int startX = cx;
        int startZ = cz;
        if (!isCityBase(cx, cz, zones.zone(cx, cz))) {
            int best = Integer.MAX_VALUE;
            startX = Integer.MIN_VALUE;
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    int d = dx * dx + dz * dz;
                    if (d < best && isCityBase(cx + dx, cz + dz, zones.zone(cx + dx, cz + dz))) {
                        best = d;
                        startX = cx + dx;
                        startZ = cz + dz;
                    }
                }
            }
            if (startX == Integer.MIN_VALUE) {
                return components.put(cx, cz, seen);
            }
        }
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        seen[(startZ - cz + r) * side + (startX - cx + r)] = true;
        queue.add(new int[] { startX, startZ });
        int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int[] step : steps) {
                int nx = at[0] + step[0];
                int nz = at[1] + step[1];
                int ix = nx - cx + r;
                int iz = nz - cz + r;
                if (ix < 0 || iz < 0 || ix >= side || iz >= side || seen[iz * side + ix]) {
                    continue;
                }
                if (predefined.occupant(nx, nz) != null || isCityBase(nx, nz, zones.zone(nx, nz))) {
                    seen[iz * side + ix] = true;
                    queue.add(new int[] { nx, nz });
                }
            }
        }
        return components.put(cx, cz, seen);
    }

    /** Whether a chunk's 3 × 3 window holds a landmark's margin (an edge ring). */
    private boolean touchesMargin(int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (zones.zone(x + dx, z + dz)
                    .zone() == Zone.EDGE_RING) {
                    return true;
                }
            }
        }
        return false;
    }

    private MultiChunk multiChunk(int x, int z) {
        int area = Math.max(
            1,
            worldStyle.multiSettings()
                .areaSize());
        int mx = MultiChunk.multiCoordinate(x, area);
        int mz = MultiChunk.multiCoordinate(z, area);
        MultiChunk cached = multiChunks.get(mx, mz);
        if (cached != null) {
            return cached;
        }
        return multiChunks.put(mx, mz, MultiChunk.calculate(this, mx, mz));
    }

    /**
     * The effective road: the raw road, kept only when the chunk is city, a neighbor it continues into is city, and no
     * predefined content or multibuilding occupies it.
     */
    private PlannedRoadType effectiveRoad(int x, int z, boolean isCity, PlannedStreetInfo raw, MultiPos pos) {
        if (!isCity || !raw.isRoad()) {
            return PlannedRoadType.NONE;
        }
        boolean overridden = predefined.occupant(x, z) != null || pos.isMulti();
        boolean connected = false;
        for (RoadDirection direction : RoadDirection.values()) {
            if (raw.connects(direction) && isCityForRoads(x + direction.stepX(), z + direction.stepZ())) {
                connected = true;
                break;
            }
        }
        return EffectiveStreetResolver.resolve(raw.roadType(), isCity, connected, overridden);
    }

    /** Raw city, and not a core or avoid ring: a road continuing only into a core is a stub (0006 §3). */
    private boolean isCityForRoads(int x, int z) {
        return cities.isCityRaw(x, z) && !zones.zone(x, z)
            .zone()
            .forcesNonCity();
    }

    private boolean checkBuildingPossibility(int x, int z, MultiPos pos, int cityLevel, PlannedRoadType planned,
        QualityRandom random, ZoneInfo zone) {
        float bc = random.nextFloat();
        if (predefined.buildingAtTopLeft(x, z) != null) {
            return true;
        }
        PredefinedIndex.Occupant occupant = predefined.occupant(x, z);
        if (occupant != null && occupant.isStreet()) {
            return false;
        }
        float chance = EffectiveCitySettings.resolve(profile, cityStyle(cities.cityStyleName(x, z)))
            .buildingChance();
        if (zone.zone()
            .isTransition()) {
            // Deviation (0006 §1): transition rings halve the building chance, after the same draw.
            chance *= 0.5f;
        }
        if (pos.isMulti()) {
            return true;
        }
        if (planned != PlannedRoadType.NONE) {
            return false;
        }
        if (bc >= chance) {
            return false;
        }
        if (hasHighway(x, z)) {
            int max = Math.max(infrastructure.highwayXLevel(x, z), infrastructure.highwayZLevel(x, z));
            return cityLevel > max + 1;
        }
        RailInfo rail = infrastructure.rail(x, z);
        if (rail.isPresent()) {
            if (rail.type() == RailChunkType.STATION_UNDERGROUND) {
                return false;
            }
            return cityLevel >= rail.level() + worldStyle.railPartHeight6();
        }
        return true;
    }

    /** Whether a one-floor building (ground floor and roof part) stays under a crater cap at this level. */
    /**
     * Whether a meteorite's crater paraboloid would cut this chunk's city ground plus {@link HeightCap#CITY_HEADROOM}:
     * such a chunk leaves the city (0006 §1, the crater basin; D216), so the crater digs natural ground there, not
     * a street or a park. The ground is the chunk's own city level, which depends on heights only, never on zones.
     */
    private boolean underCrater(int x, int z, ZoneInfo zone) {
        if (!zone.inCraterBasin()) {
            return false;
        }
        int ground = profile.groundLevel + levels.cityLevel(x, z) * ChunkPlan.LEVEL_HEIGHT;
        return zone.heightCap() < ground + HeightCap.CITY_HEADROOM;
    }

    private boolean oneFloorFits(int cityLevel, int heightCap) {
        if (heightCap == Integer.MAX_VALUE) {
            return true;
        }
        // One floor and its roof, each a floor pitch tall (0014).
        return profile.groundLevel + cityLevel * ChunkPlan.LEVEL_HEIGHT + 2 * profile.floorPitch <= heightCap;
    }

    /** The 3 × 3 street-majority vote over this chunk's window of pre-vote styles. */
    private String majorityStyle(int x, int z) {
        String[] window = new String[9];
        int i = 0;
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                window[i++] = cities.cityStyleName(x + cx, z + cz);
            }
        }
        return voteMajority(window[4], window);
    }

    /**
     * Upstream's street-majority vote: the most frequent style in the window, the chunk's own style counted twice.
     * Avoids CITY-21: a tie goes to the chunk's own style when it is among the tied ones, else to the name that sorts
     * first (P04-3), never to hash order.
     *
     * @param own    the chunk's own style (also in the window)
     * @param window the nine styles of the 3 × 3 window; {@code null} entries are skipped
     * @return the winning style, or {@code own} when the window holds none
     */
    static String voteMajority(String own, String[] window) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String style : window) {
            if (style != null) {
                counts.merge(style, 1, Integer::sum);
            }
        }
        if (own != null) {
            counts.merge(own, 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            return own;
        }
        int best = 0;
        for (int count : counts.values()) {
            best = Math.max(best, count);
        }
        if (own != null && counts.get(own) == best) {
            return own;
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == best) {
                return entry.getKey();
            }
        }
        return own;
    }

    private String candidateBuilding(int x, int z, MultiPos pos, String multi, String style, QualityRandom random) {
        String name;
        if (pos.isMulti()) {
            MultiBuildingView view = assets.multiBuilding(multi);
            name = view == null ? null : view.buildingAt(pos.x(), pos.z());
        } else {
            CityStyleView view = cityStyle(style);
            name = view == null ? null : view.pick(Selection.BUILDINGS, random, x, z);
            PredefinedBuilding pinned = predefined.buildingAtTopLeft(x, z);
            if (pinned != null) {
                name = pinned.building();
            }
        }
        if (name != null && assets.building(name) == null) {
            warnOnce("building:" + name, "unknown building '" + name + "'; no building stands where it is picked");
            return null;
        }
        return name;
    }

    /** The chunk's ring distance (D306): 0 at a city's center, 1 where the city ends. */
    float ringDistance(int x, int z) {
        return DensityRings.distance(cities.cityFactor(x, z), profile.cityThreshold);
    }

    /**
     * Density rings (D306): re-picks a single's building by class for its ring, and in the outer and edge rings
     * swaps in one of the five originals. Draws from a random of its own, so the building stream is unchanged.
     *
     * @param picked the style's pick
     * @return the building for the chunk
     */
    private String ringBuilding(int x, int z, String style, String picked) {
        float strength = profile.densityRings;
        DensityRings.Ring ring = DensityRings.Ring.of(ringDistance(x, z));
        QualityRandom r = new QualityRandom((seed + z * BUILDING_Z + x * BUILDING_X) ^ RING_SALT);
        CityStyleView view = cityStyle(style);
        String chosen = picked;
        boolean accepted = false;
        for (int attempt = 0; attempt < RING_ATTEMPTS && !accepted; attempt++) {
            if (chosen != null
                && r.nextFloat() < DensityRings.acceptance(DensityRings.classOf(chosen), ring, strength)) {
                accepted = true;
            } else if (view != null) {
                String next = view.pick(Selection.BUILDINGS, r, x, z);
                if (next != null && assets.building(next) != null) {
                    chosen = next;
                }
            }
        }
        boolean swap = r.nextFloat() < DensityRings.originalChance(ring, strength) || !accepted;
        int which = r.nextInt(DensityRings.ORIGINALS.length);
        if (swap && ring.ordinal() >= DensityRings.Ring.INNER.ordinal()) {
            String original = DensityRings.ORIGINALS[which];
            if (assets.building(original) != null) {
                return original;
            }
        }
        return accepted ? chosen : picked;
    }

    /** Whether a multibuilding's class is accepted in the ring of its top-left chunk (D306). */
    private boolean ringAcceptsMulti(int x, int z, String multi) {
        QualityRandom r = new QualityRandom((seed + z * BUILDING_Z + x * BUILDING_X) ^ RING_SALT);
        float chance = DensityRings
            .acceptance(DensityRings.classOf(multi), DensityRings.Ring.of(ringDistance(x, z)), profile.densityRings);
        return r.nextFloat() < chance;
    }

    // ================================================================== plan

    private ChunkPlan computePlan(int x, int z) {
        ChunkPlan.Builder b = computeBase(x, z);
        drawLot(b, x, z);
        applyPlaza(b, x, z);
        return new ChunkPlan(b);
    }

    /**
     * Plans a chunk up to its lot: every draw but {@link #drawLot} and the plaza. Pure, so a construction lot can ask
     * it of the other chunks of its footprint (phase 3) without a plan of theirs.
     */
    private ChunkPlan.Builder computeBase(int x, int z) {
        ChunkCharacteristics c = characteristics(x, z);
        ChunkPlan.Builder b = new ChunkPlan.Builder();
        b.characteristics = c;
        // Avoids CITY-10 (P04-2): continue the characteristics stage's stream instead of reseeding it.
        QualityRandom random = buildingRandom(x, z);
        random.skip(c.draws);
        BuildingView building = c.buildingType == null ? null : assets.building(c.buildingType);
        boolean has = c.couldHaveBuilding && building != null;
        ChunkPlan topLeft = null;
        if (c.multiPos.isMulti() && !c.multiPos.isTopLeft()) {
            topLeft = plan(x - c.multiPos.x(), z - c.multiPos.z());
            // A multibuilding stands as a whole or not at all.
            has &= topLeft.hasBuilding;
        }
        if (has && c.multiPos.isSingle()) {
            for (Direction side : Direction.values()) {
                if (random.nextFloat() < lonelyPreference(x + side.stepX(), z + side.stepZ())) {
                    has = false;
                    break;
                }
            }
        }
        if (has && profile.densityRings > 0
            && topLeft == null
            && c.multiPos.isMulti()
            && predefined.occupant(x, z) == null) {
            has = ringAcceptsMulti(x, z, c.multiBuilding);
        }
        b.isCity = c.isCity;
        b.hasBuilding = has;
        PredefinedIndex.Occupant occupant = predefined.occupant(x, z);
        b.predefinedStreet = occupant != null && occupant.isStreet();
        b.plannedRoad = c.isCity && !has ? c.plannedRoad : PlannedRoadType.NONE;
        b.hierarchicalOpen = c.isCity && !has && b.plannedRoad == PlannedRoadType.NONE && !b.predefinedStreet;
        b.groundLevel = profile.groundLevel;
        b.floorPitch = profile.floorPitch;
        b.waterLevel = profile.waterLevel();
        CityStyleView style = cityStyle(c.cityStyle);
        b.settings = EffectiveCitySettings.resolve(profile, style);
        if (topLeft != null) {
            copyFromTopLeft(b, topLeft);
        } else {
            drawChunkDecisions(b, x, z, random, building, style);
        }
        drawLevels(b, x, z, random, building);
        drawTail(b, x, z, random, style);
        b.intensity = c.isCity ? urbanity.intensity(x, z) : 0f;
        return b;
    }

    /**
     * Deviation (alpha review AR-6, docs/design/0011-city-edges-and-character.md §3): turns a low-intensity building
     * chunk or open lot into an empty or construction lot, from a separate random so nothing else the chunk draws
     * moves.
     */
    private void drawLot(ChunkPlan.Builder b, int x, int z) {
        ChunkCharacteristics c = b.characteristics;
        boolean edgeLots = profile.edgeLotChance > 0 && b.intensity < LOT_INTENSITY;
        float ringLot = profile.densityRings > 0 && c.isCity
            ? DensityRings.lotChance(ringDistance(x, z), profile.densityRings)
            : 0f;
        if (!c.isCity || !edgeLots && ringLot <= 0
            || c.multiPos.isMulti()
            || predefined.occupant(x, z) != null
            || b.highwayXLevel >= 0
            || b.highwayZLevel >= 0
            || c.lane.role() != LaneRole.NONE
            || c.zone.zone()
                .isTransition()) {
            return;
        }
        boolean building = b.hasBuilding;
        boolean open = !building && b.hierarchicalOpen && b.parkPart == null && b.streetType == StreetType.PARK;
        if (!building && !open || infrastructure.rail(x, z)
            .isPresent()) {
            return;
        }
        if (profile.lotFootprints && ringLot > 0) {
            // Phase 3 (D-p3-1): ring construction lots come from the chunk's cell, at their class's real size; the
            // chunk's own roll keeps only the edge-lot chance.
            ConstructionLots.Draw lot = constructionLot(ConstructionLots.cell(x), ConstructionLots.cell(z));
            if (lot != null && lot.contains(x, z)) {
                applyConstructionLot(b, lot, x, z);
                return;
            }
            ringLot = 0f;
        }
        QualityRandom lots = new QualityRandom((seed + z * BUILDING_Z + x * BUILDING_X) ^ LOT_SALT);
        float chance = edgeLots
            ? profile.edgeLotChance * (1f - b.intensity / LOT_INTENSITY) * (building ? 1f : OPEN_LOT_SHARE)
            : 0f;
        chance += ringLot * (building ? 1f : OPEN_LOT_SHARE);
        float roll = lots.nextFloat();
        float kindRoll = lots.nextFloat();
        float depthRoll = lots.nextFloat();
        float columnRoll = lots.nextFloat();
        if (roll >= chance) {
            return;
        }
        LotKind kind = kindRoll < LOT_KIND_WEIGHTS[0] ? LotKind.BARE
            : kindRoll < LOT_KIND_WEIGHTS[0] + LOT_KIND_WEIGHTS[1] ? LotKind.PIT : LotKind.FOUNDATION;
        if (b.xRailCorridor || b.zRailCorridor || b.railDungeonPart != null) {
            // A corridor or rail dungeon runs under the lot: nothing is dug into it.
            kind = LotKind.BARE;
        }
        if (building) {
            becomeOpenLot(b);
            b.floors = 0;
            b.cellars = 0;
        }
        b.streetType = StreetType.NORMAL;
        b.parkPart = null;
        b.fountainPart = null;
        b.frontPart = null;
        b.lot = kind;
        if (kind.isDug()) {
            b.lotDepth = lotDepth(depthRoll);
        }
        if (kind == LotKind.FOUNDATION) {
            b.lotColumnFloors = 1 + Math.min(LOT_MAX_COLUMN_FLOORS - 1, (int) (columnRoll * LOT_MAX_COLUMN_FLOORS));
        }
    }

    /**
     * Phase 3 (D-p3-1 to D-p3-3): turns a chunk of a construction lot's footprint into its part of the lot. Every
     * chunk of the footprint gets the same kind, depth and frame, from the cell's draw.
     */
    private void applyConstructionLot(ChunkPlan.Builder b, ConstructionLots.Draw lot, int x, int z) {
        if (b.hasBuilding) {
            becomeOpenLot(b);
            b.floors = 0;
            b.cellars = 0;
        }
        b.streetType = StreetType.NORMAL;
        b.parkPart = null;
        b.fountainPart = null;
        b.frontPart = null;
        b.lot = lot.kind;
        b.lotFootprint = new MultiPos(x - lot.x, z - lot.z, lot.dimX, lot.dimZ);
        if (lot.kind.isDug()) {
            b.lotDepth = lot.cellars * profile.floorPitch;
            // D-p3-5: a dug lot stands for a building with cellars, so it follows upstream's rule for one: no rail
            // corridor runs through it, and a rail dungeon stays only below its cellars.
            b.xRailCorridor = false;
            b.zRailCorridor = false;
            if (b.railDungeonPart != null
                && infrastructure.railwayLevel() >= b.characteristics.cityLevel - lot.cellars) {
                b.railDungeonPart = null;
            }
        }
        if (lot.kind == LotKind.FOUNDATION) {
            b.lotColumnFloors = lot.frameFloors;
        }
    }

    /**
     * The construction lot of a cell, fitted to its chunks (phase 3), or {@code null}: the cell's draw, kept when
     * every chunk of its footprint could hold a lot at one city level, else shrunk to its top-left chunk, else none.
     * Its cellars are fitted above the world's floor and to the profile's maximum, its frame under the roof cap, and a
     * pit with no cellars is a bare lot. Cached per cell; the answer depends on nothing but the seed and the cell.
     */
    ConstructionLots.Draw constructionLot(int cellX, int cellZ) {
        Optional<ConstructionLots.Draw> cached = lotCells.get(cellX, cellZ);
        if (cached != null) {
            return cached.orElse(null);
        }
        ConstructionLots.Draw lot = fitConstructionLot(cellX, cellZ);
        lotCells.put(cellX, cellZ, Optional.ofNullable(lot));
        return lot;
    }

    private ConstructionLots.Draw fitConstructionLot(int cellX, int cellZ) {
        int cornerX = cellX * ConstructionLots.CELL;
        int cornerZ = cellZ * ConstructionLots.CELL;
        ConstructionLots.Draw lot = ConstructionLots
            .draw(seed, cellX, cellZ, ringDistance(cornerX, cornerZ), profile.densityRings, profile.buildingMaxFloors);
        if (lot == null) {
            return null;
        }
        if (!footprintFits(lot)) {
            lot = lot.single();
            if (!footprintFits(lot)) {
                return null;
            }
        }
        LotSite site = lotSite(lot.x, lot.z);
        int cellars = Math.max(0, Math.min(lot.cellars, site.maxCellars));
        int cityGround = profile.groundLevel + site.cityLevel * ChunkPlan.LEVEL_HEIGHT;
        while (cellars > 0 && cityGround - cellars * profile.floorPitch <= MIN_BOTTOM_Y) {
            cellars--;
        }
        int frame = Math.max(1, capFloors(lot.frameFloors, cityGround, profile.floorPitch));
        LotKind kind = lot.kind;
        if (kind == LotKind.PIT && cellars == 0) {
            // The class has no cellar to dig: an empty lot. Rail corridors under a dug lot give way to it (D-p3-5).
            kind = LotKind.BARE;
        }
        return new ConstructionLots.Draw(
            lot.cls,
            lot.x,
            lot.z,
            lot.dimX,
            lot.dimZ,
            kind,
            cellars,
            lot.classFloors,
            Math.min(frame, lot.classFloors));
    }

    /** Whether every chunk of a footprint could hold a lot, all at one city level. */
    private boolean footprintFits(ConstructionLots.Draw lot) {
        int level = Integer.MIN_VALUE;
        for (int x = lot.x; x < lot.x + lot.dimX; x++) {
            for (int z = lot.z; z < lot.z + lot.dimZ; z++) {
                LotSite site = lotSite(x, z);
                if (!site.eligible || level != Integer.MIN_VALUE && site.cityLevel != level) {
                    return false;
                }
                level = site.cityLevel;
            }
        }
        return true;
    }

    /**
     * Whether a chunk could be part of a construction lot, from its plan before lots: the edge-lot rules of 0011 §3
     * (a single building chunk or an open lot; never a multibuilding, predefined content, highway, rail, lane or
     * village ring). Only chunks that pass the cheap checks are planned, and a multibuilding never is, so this never
     * asks for a plan that is asking for it.
     */
    private LotSite lotSite(int x, int z) {
        LotSite cached = lotSites.get(x, z);
        if (cached != null) {
            return cached;
        }
        ChunkCharacteristics c = characteristics(x, z);
        LotSite site;
        if (!c.isCity || c.multiPos.isMulti()
            || predefined.occupant(x, z) != null
            || c.lane.role() != LaneRole.NONE
            || c.zone.zone()
                .isTransition()
            || infrastructure.rail(x, z)
                .isPresent()) {
            site = LotSite.NONE;
        } else {
            ChunkPlan.Builder b = computeBase(x, z);
            boolean building = b.hasBuilding;
            boolean open = !building && b.hierarchicalOpen && b.parkPart == null && b.streetType == StreetType.PARK;
            boolean eligible = (building || open) && b.highwayXLevel < 0 && b.highwayZLevel < 0;
            site = eligible ? new LotSite(true, c.cityLevel, maxCellars(b.settings, null, c.cityLevel)) : LotSite.NONE;
        }
        return lotSites.put(x, z, site);
    }

    /** What a construction lot needs to know of one chunk of its footprint. */
    private static final class LotSite {

        static final LotSite NONE = new LotSite(false, 0, 0);

        final boolean eligible;
        final int cityLevel;
        final int maxCellars;

        LotSite(boolean eligible, int cityLevel, int maxCellars) {
            this.eligible = eligible;
            this.cityLevel = cityLevel;
            this.maxCellars = maxCellars;
        }
    }

    /** A dug lot's depth for a roll in [0, 1). */
    static int lotDepth(float roll) {
        float running = 0;
        for (int i = 0; i < LOT_DEPTHS.length; i++) {
            running += LOT_DEPTH_WEIGHTS[i];
            if (roll < running) {
                return LOT_DEPTHS[i];
            }
        }
        return LOT_DEPTHS[LOT_DEPTHS.length - 1];
    }

    /** Upstream's prefers-lonely value of a neighbor; avoids CITY-11: 0 unless the neighbor could hold a building. */
    private float lonelyPreference(int x, int z) {
        ChunkCharacteristics neighbor = characteristics(x, z);
        if (!neighbor.couldHaveBuilding || neighbor.buildingType == null) {
            return 0f;
        }
        BuildingView view = assets.building(neighbor.buildingType);
        return view == null ? 0f : view.prefersLonely();
    }

    private static void copyFromTopLeft(ChunkPlan.Builder b, ChunkPlan top) {
        b.highwayXLevel = top.highwayXLevel;
        b.highwayZLevel = top.highwayZLevel;
        b.streetType = top.streetType;
        b.fountainPart = top.fountainPart;
        b.parkPart = top.parkPart;
        b.floors = top.floors;
        b.drawnFloors = top.drawnFloors;
        b.cellars = top.cellars;
        b.door = top.door;
        b.bridgePart = top.bridgePart;
        b.largeBridgePart = top.largeBridgePart;
        b.stairPart = top.stairPart;
        b.stairPriority = top.stairPriority;
        b.palettes = top.palettes;
        b.noLoot = top.noLoot;
        b.ruinHeight = top.ruinHeight;
    }

    /** Steps 12.1 to 12.15 of building-info.md, for single and top-left chunks, in upstream's draw order. */
    private void drawChunkDecisions(ChunkPlan.Builder b, int x, int z, QualityRandom random, BuildingView building,
        CityStyleView style) {
        ChunkCharacteristics c = b.characteristics;
        b.highwayXLevel = infrastructure.highwayXLevel(x, z);
        b.highwayZLevel = infrastructure.highwayZLevel(x, z);
        if (b.hierarchicalOpen) {
            b.streetType = StreetType.PARK;
            b.parkPart = random.nextDouble() < b.settings.openLotParkChance()
                ? pick(style, Selection.PARKS, random, x, z)
                : null;
        } else {
            b.streetType = StreetType.NORMAL;
            // Drawn on roads too, so the rest of the chunk's stream does not depend on the road.
            String fountain = random.nextFloat() < b.settings.fountainChance()
                ? pick(style, Selection.FOUNTAINS, random, x, z)
                : null;
            b.fountainPart = b.plannedRoad != PlannedRoadType.NONE ? null : fountain;
        }
        drawFloorsAndCellars(b, x, z, random, building);
        b.door = DoorKind.forDraw(random.nextInt(DoorKind.COUNT));
        b.bridgePart = pick(style, Selection.BRIDGES, random, x, z);
        QualityRandom bridgeRandom = new QualityRandom(
            seed ^ LARGE_BRIDGE_SALT ^ (long) x * BUILDING_Z ^ (long) z * BUILDING_X);
        b.largeBridgePart = pick(style, Selection.LARGE_BRIDGES, bridgeRandom, x, z);
        b.stairPart = pick(style, Selection.STAIRS, random, x, z);
        b.stairPriority = random.nextFloat();
        StyleView palettes = assets
            .style(c.isCity && style != null ? style.styleName() : worldStyle.outsideStyleName());
        b.palettes = palettes == null ? new ArrayList<>() : new ArrayList<>(palettes.pickPalettes(random));
        if (c.isCity && c.couldHaveBuilding && b.hasBuilding) {
            // Deviation (AR-17, docs/design/0012-facade-color-feathering.md §4): a building's palettes, its
            // variety included, come from the facade stage, feathered against its neighbors; the draw above still
            // runs, so the rest of the chunk's stream is unchanged.
            List<String> feathered = facades.palettes(x, z);
            if (feathered != null) {
                b.palettes = new ArrayList<>(feathered);
            } else if (palettes != null) {
                b.palettes.addAll(palettes.pickVariety(b.palettes, true, varietySeed(x, z)));
            }
        } else if (palettes != null) {
            // Deviation (D25, M11): the palette variety, drawn from a seed of its own so the stream above is
            // upstream's; a building's per chunk, a street's or park's per four-by-four block of chunks.
            long variety = b.hasBuilding ? varietySeed(x, z) : streetVarietySeed(x, z);
            b.palettes.addAll(palettes.pickVariety(b.palettes, b.hasBuilding, variety));
        }
        float loot = random.nextFloat();
        // Avoids LOOT-10: a multibuilding's top-left chunk rolls too, and its other chunks copy the result.
        b.noLoot = loot < profile.buildingWithoutLootChance;
        float fraction = random.nextFloat();
        PredefinedBuilding pinned = predefined.buildingAtTopLeft(x, z);
        if (random.nextFloat() < profile.ruinChance && (pinned == null || !pinned.preventRuins())) {
            b.ruinHeight = profile.ruinMinLevelPercent
                + (profile.ruinMaxLevelPercent - profile.ruinMinLevelPercent) * fraction;
        } else {
            b.ruinHeight = -1;
        }
    }

    private void drawFloorsAndCellars(ChunkPlan.Builder b, int x, int z, QualityRandom random, BuildingView building) {
        ChunkCharacteristics c = b.characteristics;
        int cityGround = profile.groundLevel + c.cityLevel * ChunkPlan.LEVEL_HEIGHT;
        float cityFactor = cities.cityFactor(x, z);
        float taper = profile.cityTaper;
        // Deviation (AR-5, 0011 §2): the urban intensity takes the city factor's place, by cityTaper.
        float intensity = urbanity.intensity(x, z);
        float factor = cityFactor + (intensity - cityFactor) * taper;
        int maxFloors = maxFloors(b.settings, building);
        int bound = (int) (profile.buildingMinFloorsChance
            + (factor + .1f) * (profile.buildingMaxFloorsChance - profile.buildingMinFloorsChance));
        // Avoids CITY-14: a bound below 1 would throw in nextInt.
        bound = Math.max(1, bound);
        int f = profile.buildingMinFloors + random.nextInt(bound) + 1;
        if (taper > 0) {
            f = taperFloors(f, intensity, taper, x, z);
        }
        if (profile.densityRings > 0 && c.isCity) {
            // Density rings (D306): the floors follow the curve over the ring distance.
            int cap = DensityRings.floorCap(ringDistance(x, z), profile.buildingMaxFloors, profile.densityRings);
            f = Math.max(DensityRings.floorLow(cap, profile.densityRings), Math.min(f, cap));
        }
        f = Math.min(f, maxFloors);
        f = Math.max(f, minFloors(b.settings, building));
        // Avoids CITY-15: the cap loop stops at 0, and a chunk it empties holds no building.
        boolean capped = false;
        // At a taller pitch fewer floors fit under the cap: towers stay under Y 248 (0014).
        int fitFloors = capFloors(f, cityGround, profile.floorPitch);
        if (fitFloors < f) {
            f = fitFloors;
            capped = true;
        }
        b.drawnFloors = f;
        // Deviation (0006 §1 and §3): zone caps after the draw; a multibuilding takes its strictest chunk's (P04-5).
        int zoneCap = zoneFloorCap(c, cityGround, x, z);
        if (f > zoneCap) {
            f = Math.max(0, zoneCap);
            capped = true;
        }
        b.floors = f;
        if (capped && f == 0 && b.hasBuilding) {
            becomeOpenLot(b);
        }

        int maxCellars = maxCellars(b.settings, building, c.cityLevel);
        int minCellars = Math.max(profile.buildingMinCellars, building == null ? 0 : building.minCellars());
        int cellars;
        if (maxCellars <= 0) {
            cellars = Math.min(minCellars, Math.max(0, maxCellars));
        } else if (minCellars >= maxCellars) {
            // Avoids CITY-16: keep upstream's single draw, but the result is the maximum, not an overflow.
            random.nextInt(maxCellars + 1);
            cellars = maxCellars;
        } else {
            // Avoids CITY-16: uniform between the minimum and the maximum.
            cellars = minCellars + random.nextInt(maxCellars - minCellars + 1);
        }
        int highway = Math.max(b.highwayXLevel, b.highwayZLevel);
        RailInfo rail = infrastructure.rail(x, z);
        boolean checkRail = !worldStyle.buildingsBlockRailways() && rail.isPresent();
        if (highway >= 0 || checkRail) {
            int under = checkRail ? (highway >= 0 ? Math.max(highway, rail.level()) : rail.level()) : highway;
            // The room is in city levels; at a taller pitch fewer cellars fill it (0014). Pitch 6 is upstream's cap.
            int room = c.cityLevel - under - worldStyle.railPartHeight6();
            int fit = room <= 0 ? 0 : room * ChunkPlan.LEVEL_HEIGHT / profile.floorPitch;
            cellars = Math.max(0, Math.min(fit, cellars));
        }
        cellars = Math.max(0, Math.min(cellars, Math.max(0, maxCellars)));
        // Avoids CITY-18: fitted once here; the lowest cellar's floor stays above Y 2.
        while (cellars > 0 && cityGround - cellars * profile.floorPitch <= MIN_BOTTOM_Y) {
            cellars--;
        }
        b.cellars = cellars;
    }

    /**
     * Deviation (alpha review AR-5, docs/design/0011-city-edges-and-character.md §2): a building in the dense core may
     * rise into a high-rise, up to {@code buildingMaxFloors}, and one near the edge is held low, so heights fade from
     * the core to RWG terrain. Draws from a separate random, so the building stream is unchanged.
     *
     * @param drawn     upstream's floor count
     * @param intensity the chunk's urban intensity
     * @param taper     {@code cityTaper}
     * @param x         chunk X
     * @param z         chunk Z
     * @return the tapered floor count
     */
    private int taperFloors(int drawn, float intensity, float taper, int x, int z) {
        QualityRandom core = new QualityRandom((seed + z * BUILDING_Z + x * BUILDING_X) ^ TAPER_SALT);
        float rise = core.nextFloat();
        float height = core.nextFloat();
        int f = drawn;
        if (rise < Urbanity.coreChance(intensity, taper) && f < profile.buildingMaxFloors) {
            f += 1 + (int) (height * (profile.buildingMaxFloors - f));
        }
        return Math
            .min(f, Urbanity.floorCap(intensity, profile.buildingMaxFloors, profile.buildingMinFloors + 1, taper));
    }

    /** The most floors a zone allows at this chunk (all of a multibuilding's chunks, for its top-left). */
    private int zoneFloorCap(ChunkCharacteristics c, int cityGround, int x, int z) {
        if (c.multiPos.isSingle()) {
            return chunkFloorCap(c.zone, cityGround, profile.floorPitch);
        }
        int cap = Integer.MAX_VALUE;
        for (int dx = 0; dx < c.multiPos.width(); dx++) {
            for (int dz = 0; dz < c.multiPos.height(); dz++) {
                cap = Math.min(cap, chunkFloorCap(zones.zone(x + dx, z + dz), cityGround, profile.floorPitch));
            }
        }
        return cap;
    }

    /** One chunk's floor cap: 2 or 4 in transition rings, and the crater cap: the roof part's top under the cap. */
    private static int chunkFloorCap(ZoneInfo zone, int cityGround, int pitch) {
        int cap = LandmarkZones.transitionFloorCap(zone.zone());
        if (zone.inCraterBasin()) {
            cap = Math.min(cap, Math.floorDiv(zone.heightCap() - cityGround, pitch) - 1);
        }
        return cap;
    }

    /**
     * Caps a floor count so the roof part's base stays under {@link #MAX_ROOF_FLOOR_Y}: at a taller pitch fewer floors
     * fit (design record 0014).
     *
     * @param floors     the floors wanted
     * @param cityGround the building's ground Y
     * @param pitch      blocks per floor
     * @return the most floors, from 0 to {@code floors}, with {@code cityGround + floors * pitch < 248}
     */
    static int capFloors(int floors, int cityGround, int pitch) {
        int f = floors;
        while (f > 0 && cityGround + f * pitch >= MAX_ROOF_FLOOR_Y) {
            f--;
        }
        return f;
    }

    private int maxFloors(EffectiveCitySettings settings, BuildingView building) {
        int max = profile.buildingMaxFloors;
        if (building != null && building.maxFloors() != BuildingView.UNSET) {
            if (building.overrideFloors()) {
                return building.maxFloors();
            }
            max = Math.min(max, building.maxFloors());
        }
        return settings.constrainMaximumFloors(max);
    }

    private int minFloors(EffectiveCitySettings settings, BuildingView building) {
        // +1 because the roof part is not counted.
        int min = profile.buildingMinFloors + 1;
        if (building != null && building.minFloors() != BuildingView.UNSET) {
            if (building.overrideFloors()) {
                return building.minFloors();
            }
            min = Math.max(min, building.minFloors());
        }
        return settings.constrainMinimumFloors(min);
    }

    private int maxCellars(EffectiveCitySettings settings, BuildingView building, int cityLevel) {
        int max = profile.buildingMaxCellars + cityLevel;
        if (building != null) {
            if (building.maxCellars() != BuildingView.UNSET && building.overrideFloors()) {
                return building.maxCellars();
            }
            if (building.minCellars() != BuildingView.UNSET && building.overrideFloors()) {
                return building.minCellars();
            }
            if (building.maxCellars() != BuildingView.UNSET) {
                max = Math.min(max, building.maxCellars());
            }
            if (building.minCellars() != BuildingView.UNSET) {
                max = Math.max(max, building.minCellars());
            }
        }
        return settings.constrainMaximumCellars(max);
    }

    /** Step 14: the part, overlay part and doorway flags of every level, for every chunk, as upstream draws them. */
    private void drawLevels(ChunkPlan.Builder b, int x, int z, QualityRandom random, BuildingView building) {
        ChunkCharacteristics c = b.characteristics;
        int drawnLevels = b.drawnFloors + b.cellars + 1;
        String[] parts = new String[drawnLevels];
        String[] parts2 = new String[drawnLevels];
        boolean[] atX = new boolean[drawnLevels];
        boolean[] atZ = new boolean[drawnLevels];
        boolean cityWest = characteristics(x - 1, z).isCity;
        boolean cityNorth = characteristics(x, z - 1).isCity;
        float doorway = profile.buildingDoorwayChance;
        String below = PartContext.NONE;
        boolean missing = false;
        for (int i = 0; i < drawnLevels; i++) {
            if (building != null) {
                int floor = i - b.cellars;
                PartContext context = new PartContext(
                    c.cityLevel + floor,
                    floor,
                    b.cellars,
                    b.drawnFloors,
                    PartContext.NONE,
                    below,
                    building.name(),
                    x,
                    z);
                String part = building.pickPart(random, context);
                missing |= part == null;
                parts[i] = part;
                below = part == null ? PartContext.NONE : part;
                PartContext overlay = new PartContext(
                    c.cityLevel + floor,
                    floor,
                    b.cellars,
                    b.drawnFloors,
                    below,
                    below,
                    building.name(),
                    x,
                    z);
                parts2[i] = building.pickPart2(random, overlay);
            }
            atX[i] = cityWest && random.nextFloat() < doorway;
            atZ[i] = cityNorth && random.nextFloat() < doorway;
        }
        if (missing && b.hasBuilding) {
            // Avoids CITY-17: an asset error the loader reports; here the building is left out, never thrown.
            warnOnce(
                "parts:" + building.name(),
                "building " + building.name() + " has a level no part condition matches; it is left out");
            becomeOpenLot(b);
        }
        b.connectionAtX = atX;
        b.connectionAtZ = atZ;
        if (b.floors < b.drawnFloors && building != null) {
            cappedParts(b, x, z, parts, parts2, building);
        } else {
            b.floorParts = parts;
            b.floorParts2 = parts2;
        }
    }

    /**
     * A building a zone capped keeps the drawn parts of the floors it still has and takes its roof part from a separate
     * random (decision log D49), so the cap shifts nothing else the chunk draws.
     */
    private void cappedParts(ChunkPlan.Builder b, int x, int z, String[] parts, String[] parts2,
        BuildingView building) {
        int levelCount = b.floors + b.cellars + 1;
        String[] capped = new String[levelCount];
        String[] capped2 = new String[levelCount];
        System.arraycopy(parts, 0, capped, 0, levelCount - 1);
        System.arraycopy(parts2, 0, capped2, 0, levelCount - 1);
        QualityRandom roofRandom = new QualityRandom((seed + z * BUILDING_Z + x * BUILDING_X) ^ ROOF_SALT);
        int cityLevel = b.characteristics.cityLevel;
        String below = levelCount > 1 && capped[levelCount - 2] != null ? capped[levelCount - 2] : PartContext.NONE;
        PartContext roof = new PartContext(
            cityLevel + b.floors,
            b.floors,
            b.cellars,
            b.floors,
            PartContext.NONE,
            below,
            building.name(),
            x,
            z);
        String part = building.pickPart(roofRandom, roof);
        capped[levelCount - 1] = part != null ? part : parts[parts.length - 1];
        String chosen = capped[levelCount - 1] == null ? PartContext.NONE : capped[levelCount - 1];
        PartContext overlay = new PartContext(
            cityLevel + b.floors,
            b.floors,
            b.cellars,
            b.floors,
            chosen,
            chosen,
            building.name(),
            x,
            z);
        capped2[levelCount - 1] = building.pickPart2(roofRandom, overlay);
        b.floorParts = capped;
        b.floorParts2 = capped2;
    }

    /**
     * A chunk that loses its building after the street draws (a cap left no floor, or the asset matched no part)
     * becomes an open lot: a park with no park part, so no further random is drawn for it.
     */
    private static void becomeOpenLot(ChunkPlan.Builder b) {
        b.hasBuilding = false;
        b.plannedRoad = b.characteristics.plannedRoad;
        b.hierarchicalOpen = b.isCity && b.plannedRoad == PlannedRoadType.NONE && !b.predefinedStreet;
        if (b.hierarchicalOpen) {
            b.streetType = StreetType.PARK;
            b.parkPart = null;
            b.fountainPart = null;
        }
    }

    /** Steps 15 to 18: corridor and legacy bridge candidates, rail dungeon, front. */
    private void drawTail(ChunkPlan.Builder b, int x, int z, QualityRandom random, CityStyleView style) {
        ChunkCharacteristics c = b.characteristics;
        if (b.hasBuilding && b.cellars > 0) {
            b.xRailCorridor = false;
            b.zRailCorridor = false;
        } else {
            b.xRailCorridor = random.nextFloat() < b.settings.corridorChance();
            b.zRailCorridor = random.nextFloat() < b.settings.corridorChance();
        }
        if (!c.isCity) {
            // Legacy bridge candidates: the legacy bridges are not ported (D11), the draws are kept for the stream.
            random.nextFloat();
            random.nextFloat();
        }
        if (random.nextFloat() < profile.railwayDungeonChance) {
            if (!b.hasBuilding || infrastructure.railwayLevel() < c.cityLevel - b.cellars) {
                b.railDungeonPart = pick(style, Selection.RAIL_DUNGEONS, random, x, z);
            }
        }
        if (random.nextFloat() < b.settings.frontChance()) {
            b.frontPart = pick(style, Selection.FRONTS, random, x, z);
        }
    }

    /**
     * Avoids CITY-12: a road that runs into a landmark's margin ends in the city chunk facing it, which becomes a
     * plaza ({@link StreetType#FULL}; 0006 §3, the margin of alpha review AR-10). A primary road whose detour could
     * not be laid (one of its ends is not city) ends in a plaza too, rather than running into the margin's border
     * (AR-7: a street should not stop dead where it could go on); a detour chunk keeps its road.
     */
    private void applyPlaza(ChunkPlan.Builder b, int x, int z) {
        ChunkCharacteristics c = b.characteristics;
        if (!c.isCity || b.hasBuilding || c.lane.role() != LaneRole.NONE || !b.plannedRoad.isRoad()) {
            return;
        }
        PlannedStreetInfo raw = streets.getStreetInfo(x, z);
        for (RoadDirection direction : RoadDirection.values()) {
            if (raw.connects(direction) && zones.zone(x + direction.stepX(), z + direction.stepZ())
                .zone() == Zone.EDGE_RING && (b.plannedRoad != PlannedRoadType.PRIMARY || deadEnd(raw, direction))) {
                b.streetType = StreetType.FULL;
                b.fountainPart = null;
                b.parkPart = null;
                return;
            }
        }
    }

    /**
     * Whether a primary road chunk is the end of a straight run into the margin: it connects only toward the margin
     * and back. A primary crossing there keeps its road, so a plaza never cuts a primary line that goes on.
     */
    private static boolean deadEnd(PlannedStreetInfo raw, RoadDirection toMargin) {
        for (RoadDirection other : RoadDirection.values()) {
            if (other != toMargin && other != toMargin.opposite() && raw.connects(other)) {
                return false;
            }
        }
        return true;
    }

    private static String pick(CityStyleView style, Selection list, Random random, int x, int z) {
        return style == null ? null : style.pick(list, random, x, z);
    }

    // ================================================================== neighbor queries

    /** Returns the plan of the neighbor on a side. */
    private ChunkPlan neighbor(ChunkPlan plan, Direction side) {
        return plan(plan.chunkX() + side.stepX(), plan.chunkZ() + side.stepZ());
    }

    /**
     * Returns whether a road from a neighbor may continue into a chunk: city, no building, and a planned road or a
     * predefined street.
     *
     * @param plan the chunk's plan
     * @return whether roads extend into it
     */
    public boolean doesRoadExtendTo(ChunkPlan plan) {
        return plan.isCity && !plan.hasBuilding && (plan.isPlannedRoad() || plan.predefinedStreet);
    }

    /**
     * Returns whether two neighboring chunks are joined by road: both extend roads and either share a level or one is a
     * slope up to the other.
     *
     * @param a one chunk's plan
     * @param b its neighbor's plan
     * @return whether the road connects
     */
    public boolean hasRoadConnection(ChunkPlan a, ChunkPlan b) {
        if (!doesRoadExtendTo(a) || !doesRoadExtendTo(b)) {
            return false;
        }
        if (a.cityLevel() == b.cityLevel()) {
            return true;
        }
        Direction slopeA = streetSlope(a.chunkX(), a.chunkZ());
        Direction slopeB = streetSlope(b.chunkX(), b.chunkZ());
        return slopeA != null && a.chunkX() + slopeA.stepX() == b.chunkX() && a.chunkZ() + slopeA.stepZ() == b.chunkZ()
            || slopeB != null && b.chunkX() + slopeB.stepX() == a.chunkX() && b.chunkZ() + slopeB.stepZ() == a.chunkZ();
    }

    /**
     * Returns whether a park chunk is elevated: a park with at least {@code parkStreetThreshold} of its 8 neighbors
     * street or park sections.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the park is raised
     */
    public boolean isElevatedParkSection(int chunkX, int chunkZ) {
        ChunkPlan plan = plan(chunkX, chunkZ);
        if (!plan.isStreetOrParkSection() || plan.streetType != StreetType.PARK) {
            return false;
        }
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && plan(chunkX + dx, chunkZ + dz).isStreetOrParkSection()) {
                    count++;
                }
            }
        }
        return count >= plan.settings.parkStreetThreshold();
    }

    /**
     * Returns the higher side of a full-chunk street slope, or {@code null} for a flat street: a road chunk with
     * exactly one same-class road neighbor one level up, a straight same-level approach, a departure at the upper
     * level, and no same-class side roads.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the side the slope rises toward, or {@code null}
     */
    public Direction streetSlope(int chunkX, int chunkZ) {
        return derived(chunkX, chunkZ).slope(this, chunkX, chunkZ);
    }

    Direction computeStreetSlope(int chunkX, int chunkZ) {
        ChunkPlan plan = plan(chunkX, chunkZ);
        if (!doesRoadExtendTo(plan)) {
            return null;
        }
        Direction slope = null;
        for (Direction side : Direction.values()) {
            ChunkPlan adjacent = neighbor(plan, side);
            if (sameSlopeClass(plan, adjacent) && adjacent.cityLevel() == plan.cityLevel() + 1) {
                if (slope != null) {
                    return null;
                }
                slope = side;
            }
        }
        if (slope == null) {
            return null;
        }
        ChunkPlan upper = neighbor(plan, slope);
        ChunkPlan approach = neighbor(plan, slope.opposite());
        if (!sameSlopeClass(plan, approach) || approach.cityLevel() != plan.cityLevel()) {
            return null;
        }
        ChunkPlan departure = neighbor(upper, slope);
        if (!sameSlopeClass(plan, departure) || departure.cityLevel() != upper.cityLevel()) {
            return null;
        }
        for (Direction side : Direction.values()) {
            if (side == slope || side == slope.opposite()) {
                continue;
            }
            ChunkPlan beside = neighbor(plan, side);
            if (sameSlopeClass(plan, beside) && beside.cityLevel() == plan.cityLevel()) {
                return null;
            }
            ChunkPlan upperBeside = neighbor(upper, side);
            if (sameSlopeClass(plan, upperBeside) && upperBeside.cityLevel() == upper.cityLevel()) {
                return null;
            }
        }
        return slope;
    }

    private boolean sameSlopeClass(ChunkPlan plan, ChunkPlan other) {
        return doesRoadExtendTo(other) && plan.isPrimaryRoad() == other.isPrimaryRoad();
    }

    Direction computeStairDirection(int chunkX, int chunkZ) {
        if (streetSlope(chunkX, chunkZ) != null) {
            return null;
        }
        ChunkPlan plan = plan(chunkX, chunkZ);
        if (plan.streetType == StreetType.PARK || plan.hasBuilding || !plan.isCity || plan.lot != LotKind.NONE) {
            return null;
        }
        for (Direction side : Direction.values()) {
            ChunkPlan adjacent = neighbor(plan, side);
            if (plan.cityLevel() == adjacent.cityLevel() - 1 && !adjacent.hasBuilding && adjacent.isCity) {
                return side;
            }
        }
        return null;
    }

    /**
     * Returns the side a stair climbs toward, after arbitration: a city street chunk next to a street one level up
     * gets a stair unless one of its 8 neighbors also wants one and has a higher stair priority.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the stair's side, or {@code null}
     */
    public Direction stairDirection(int chunkX, int chunkZ) {
        Direction own = derived(chunkX, chunkZ).stair(this, chunkX, chunkZ);
        if (own == null) {
            return null;
        }
        float priority = plan(chunkX, chunkZ).stairPriority;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0)
                    && derived(chunkX + dx, chunkZ + dz).stair(this, chunkX + dx, chunkZ + dz) != null
                    && plan(chunkX + dx, chunkZ + dz).stairPriority > priority) {
                    return null;
                }
            }
        }
        return own;
    }

    /**
     * Returns the hierarchical bridge span through a chunk along an axis, or {@code null}.
     *
     * @param chunkX      chunk X
     * @param chunkZ      chunk Z
     * @param orientation {@link Orientation#X} for an east-west span
     * @return the span, or {@code null}
     */
    public PlannedBridgeInfo bridge(int chunkX, int chunkZ, Orientation orientation) {
        return bridges.getBridgeInfo(this, chunkX, chunkZ, plan(chunkX, chunkZ).isCity, orientation);
    }

    /**
     * Returns the bridge part a chunk carries along an axis: the span's lower endpoint's large bridge part if it has
     * one, else its bridge part; {@code null} when there is no span.
     *
     * @param chunkX      chunk X
     * @param chunkZ      chunk Z
     * @param orientation the axis
     * @return the part name, or {@code null}
     */
    public String bridgePart(int chunkX, int chunkZ, Orientation orientation) {
        PlannedBridgeInfo span = bridge(chunkX, chunkZ, orientation);
        if (span == null) {
            return null;
        }
        ChunkPlan endpoint = plan(span.minimumX(), span.minimumZ());
        return endpoint.largeBridgePart != null ? endpoint.largeBridgePart : endpoint.bridgePart;
    }

    /**
     * Returns whether a rail corridor runs through a chunk along an axis: the chunk is a candidate, and walking both
     * ways through candidates a corridor may pass ends at a building with cellars on each side.
     *
     * @param chunkX      chunk X
     * @param chunkZ      chunk Z
     * @param orientation the axis
     * @return whether a corridor runs here
     */
    public boolean hasCorridor(int chunkX, int chunkZ, Orientation orientation) {
        ChunkPlan plan = plan(chunkX, chunkZ);
        boolean candidate = orientation == Orientation.X ? plan.xRailCorridor : plan.zRailCorridor;
        // M06: the railways module switches the subways off too; their draws stay, so nothing else moves.
        if (!candidate || networks != null && !networks.subways()) {
            return false;
        }
        return corridorEndsAtCellars(chunkX, chunkZ, orientation, -1)
            && corridorEndsAtCellars(chunkX, chunkZ, orientation, 1);
    }

    private boolean corridorEndsAtCellars(int chunkX, int chunkZ, Orientation orientation, int step) {
        int x = chunkX;
        int z = chunkZ;
        for (int walked = 0; walked < MAX_CORRIDOR_WALK; walked++) {
            if (orientation == Orientation.X) {
                x += step;
            } else {
                z += step;
            }
            ChunkPlan plan = plan(x, z);
            boolean candidate = orientation == Orientation.X ? plan.xRailCorridor : plan.zRailCorridor;
            if (!(plan.canRailGoThrough() && candidate)) {
                return plan.hasBuilding && plan.cellars > 0;
            }
        }
        return false;
    }

    /**
     * Returns whether a street chunk carries the front part of the building on one of its sides (called on the street
     * chunk, as upstream's {@code hasFrontPartFrom}).
     *
     * @param streetX the street chunk's X
     * @param streetZ the street chunk's Z
     * @param side    the side of the street chunk the building is on
     * @return whether the building's front part is placed on this street
     */
    public boolean hasFrontPartFrom(int streetX, int streetZ, Direction side) {
        ChunkPlan street = plan(streetX, streetZ);
        ChunkPlan building = neighbor(street, side);
        StreetType type = isElevatedParkSection(streetX, streetZ) ? StreetType.PARK : street.streetType;
        if (!building.hasBuilding || building.frontPart == null
            || type != StreetType.NORMAL
            || street.cityLevel() >= building.cityLevel() + building.floors) {
            return false;
        }
        RailChunkType rail = infrastructure.rail(streetX, streetZ)
            .type();
        if (rail == RailChunkType.STATION_UNDERGROUND || rail == RailChunkType.GOING_DOWN_ONE_FROM_SURFACE) {
            return false;
        }
        if (street.maxHighwayLevel() >= 0) {
            return false;
        }
        int local = street.cityLevel() - building.cityLevel();
        return !(building.isValidFloor(local) && dontConnect(building.floorPart(local)));
    }

    /**
     * Returns whether a building may open its west wall at a level (upstream's {@code hasConnectionAtX}). Avoids
     * CITY-35: no multibuilding column is excluded.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param level  the level index, 0 the lowest cellar
     * @return whether a connection is allowed
     */
    public boolean hasConnectionAtX(int chunkX, int chunkZ, int level) {
        return hasConnection(chunkX, chunkZ, level, Direction.XMIN, false);
    }

    /**
     * Returns whether a building may open its north wall at a level (upstream's {@code hasConnectionAtZ}).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param level  the level index, 0 the lowest cellar
     * @return whether a connection is allowed
     */
    public boolean hasConnectionAtZ(int chunkX, int chunkZ, int level) {
        return hasConnection(chunkX, chunkZ, level, Direction.ZMIN, false);
    }

    private boolean hasConnection(int chunkX, int chunkZ, int level, Direction side, boolean fromStreet) {
        ChunkPlan plan = plan(chunkX, chunkZ);
        boolean[] flags = side == Direction.XMIN ? plan.connectionAtX : plan.connectionAtZ;
        if (!plan.isCity || level < 0 || level >= flags.length) {
            return false;
        }
        if (!fromStreet && level < plan.floorParts.length && dontConnect(plan.floorParts[level])) {
            return false;
        }
        boolean front = fromStreet ? hasFrontPartFrom(chunkX, chunkZ, side)
            : hasFrontPartFrom(chunkX + side.stepX(), chunkZ + side.stepZ(), side.opposite());
        return front || flags[level];
    }

    /**
     * Returns what one floor of a building gets in one side wall (upstream's {@code Doors.generateDoors} decisions).
     * Avoids CITY-36: a passage on any side needs both floors to allow connections.
     *
     * @param chunkX     chunk X
     * @param chunkZ     chunk Z
     * @param localFloor the floor, 0 the ground floor, negative a cellar (not the roof)
     * @param side       the wall
     * @return the doorway
     */
    public Doorway doorway(int chunkX, int chunkZ, int localFloor, Direction side) {
        ChunkPlan plan = plan(chunkX, chunkZ);
        ChunkPlan other = neighbor(plan, side);
        if (!plan.hasBuilding) {
            return Doorway.NONE;
        }
        if (side == Direction.XMIN || side == Direction.ZMIN) {
            if (!hasConnection(chunkX, chunkZ, localFloor + plan.cellars, side, false)) {
                return Doorway.NONE;
            }
            if (passageAllowed(plan, other, localFloor)) {
                return Doorway.PASSAGE;
            }
            return connectsToTopOrOutside(plan, other, localFloor) ? Doorway.DOOR : Doorway.NONE;
        }
        int otherLevel = localFloor + plan.cityLevel() - other.cityLevel() + other.cellars;
        if (passageAllowed(plan, other, localFloor)
            && hasConnection(other.chunkX(), other.chunkZ(), otherLevel, side.opposite(), false)) {
            return Doorway.PASSAGE;
        }
        if (connectsToTopOrOutside(plan, other, localFloor)
            && hasConnection(other.chunkX(), other.chunkZ(), localFloor + other.cellars, side.opposite(), true)) {
            return Doorway.DOOR;
        }
        return Doorway.NONE;
    }

    /** A passage needs a building floor or cellar of the neighbor at the same level, and neither floor dontconnect. */
    private boolean passageAllowed(ChunkPlan plan, ChunkPlan other, int localFloor) {
        if (plan.isValidFloor(localFloor) && dontConnect(plan.floorPart(localFloor))) {
            return false;
        }
        int adjacent = localFloor + plan.cityLevel() - other.cityLevel();
        if (other.isValidFloor(adjacent) && dontConnect(other.floorPart(adjacent))) {
            return false;
        }
        return other.hasBuilding
            && (adjacent >= 0 && adjacent < other.floors || adjacent < 0 && -adjacent <= other.cellars);
    }

    private boolean connectsToTopOrOutside(ChunkPlan plan, ChunkPlan other, int localFloor) {
        if (dontConnect(plan.floorPart(localFloor))) {
            return false;
        }
        if (streetSlope(other.chunkX(), other.chunkZ()) != null) {
            return false;
        }
        int adjacent = localFloor + plan.cityLevel() - other.cityLevel();
        return other.isCity && !other.hasBuilding && localFloor == 0 && adjacent == 0
            || other.hasBuilding && adjacent == other.floors;
    }

    private boolean dontConnect(String part) {
        return part != null && assets.partDontConnect(part);
    }

    /**
     * Returns the level-0 terrain-correction target at a chunk's north-west corner, shared with its west, north and
     * north-west neighbors. Avoids CITY-22 with an explicit constraint flag. Deviation (0006 §4, E1): a corner shared
     * by a landmark core is fixed at the core's oracle height, as a city corner is fixed at the city ground.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the corner target
     */
    public CornerHeight cornerHeight(int chunkX, int chunkZ) {
        int[][] corner = { { chunkX, chunkZ }, { chunkX - 1, chunkZ }, { chunkX, chunkZ - 1 },
            { chunkX - 1, chunkZ - 1 } };
        int cityCount = 0;
        boolean core = false;
        for (int[] c : corner) {
            if (plan(c[0], c[1]).isCity) {
                cityCount++;
            }
            core |= zones.zone(c[0], c[1])
                .zone() == Zone.CORE;
        }
        if (!core && (cityCount == 0 || cityCount == corner.length)) {
            return CornerHeight.UNCONSTRAINED;
        }
        boolean constrained = false;
        int height = Integer.MAX_VALUE;
        for (int[] c : corner) {
            ChunkPlan plan = plan(c[0], c[1]);
            int h;
            if (plan.isCity) {
                h = plan.cityGroundLevel();
            } else if (zones.zone(c[0], c[1])
                .zone() == Zone.CORE) {
                    h = terrain.height(c[0], c[1]);
                } else if (terrain.isOcean(c[0], c[1])) {
                    h = profile.groundLevel - profile.oceanCorrectionBorder;
                } else {
                    continue;
                }
            constrained = true;
            height = Math.min(height, h);
        }
        return constrained ? CornerHeight.at(height) : CornerHeight.UNCONSTRAINED;
    }

    /**
     * Returns whether a chunk's side faces a landmark core, where the terrain writes a soft-edge strip instead of a
     * border wall (0006 §4, E2).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param side   the side
     * @return whether the neighbor on that side is a core chunk
     */
    public boolean facesCore(int chunkX, int chunkZ, Direction side) {
        return zones.zone(chunkX + side.stepX(), chunkZ + side.stepZ())
            .zone() == Zone.CORE;
    }

    /**
     * Returns whether a chunk has a highway on either axis.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether a highway passes
     */
    public boolean hasHighway(int chunkX, int chunkZ) {
        return infrastructure.highwayXLevel(chunkX, chunkZ) >= 0 || infrastructure.highwayZLevel(chunkX, chunkZ) >= 0;
    }

    // ================================================================== bridge context

    @Override
    public boolean isWater(int chunkX, int chunkZ) {
        return terrain.isWaterBiome(chunkX, chunkZ) || terrain.height(chunkX, chunkZ) < profile.waterLevel();
    }

    @Override
    public boolean isLandmarkCore(int chunkX, int chunkZ) {
        return zones.zone(chunkX, chunkZ)
            .zone() == Zone.CORE;
    }

    @Override
    public boolean isLevelZeroPrimaryRoad(int chunkX, int chunkZ) {
        ChunkPlan plan = plan(chunkX, chunkZ);
        return plan.isPrimaryRoad() && plan.cityLevel() == 0;
    }

    // ================================================================== helpers

    /**
     * Returns a chunk's building random: {@code QualityRandom(seed + chunkZ * 341873128712 + chunkX * 132897987541)}.
     */
    QualityRandom buildingRandom(int chunkX, int chunkZ) {
        return new QualityRandom(seed + chunkZ * BUILDING_Z + chunkX * BUILDING_X);
    }

    /**
     * Returns a chunk's palette-variety seed: a function of the world seed and the chunk alone, so a building's variety
     * never depends on generation order (a multibuilding's chunks copy their top-left chunk's palettes).
     *
     * @param x chunk X
     * @param z chunk Z
     * @return the seed
     */
    long varietySeed(int x, int z) {
        return seed ^ VARIETY_SALT ^ (long) x * BUILDING_Z ^ (long) z * BUILDING_X;
    }

    /**
     * Returns the palette-variety seed of a chunk without a building: shared by each four-by-four block of chunks, so a
     * park that spans chunks keeps one planting scheme and neighboring streets share their fittings (and their compiled
     * palette).
     *
     * @param x chunk X
     * @param z chunk Z
     * @return the seed
     */
    long streetVarietySeed(int x, int z) {
        return varietySeed(x >> STREET_VARIETY_SHIFT, z >> STREET_VARIETY_SHIFT) ^ STREET_VARIETY_SALT;
    }

    /** Looks up a city style, reporting an unknown name once. */
    CityStyleView cityStyle(String name) {
        if (name == null) {
            return null;
        }
        CityStyleView style = assets.cityStyle(name);
        if (style == null) {
            warnOnce("citystyle:" + name, "unknown city style '" + name + "'; its chunks use profile values only");
        }
        return style;
    }

    /** Reports a problem once per planner. */
    void warnOnce(String key, String message) {
        if (warned.add(key)) {
            log.warn(message);
        }
    }

    private Derived derived(int chunkX, int chunkZ) {
        Derived cached = derived.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return derived.put(chunkX, chunkZ, new Derived());
    }

    /** Lazily computed neighbor-dependent values of one chunk; each is a pure function of plans, so caching is safe. */
    private static final class Derived {

        private boolean slopeKnown;
        private Direction slope;
        private boolean stairKnown;
        private Direction stair;

        Direction slope(LayoutPlanner planner, int x, int z) {
            if (!slopeKnown) {
                slope = planner.computeStreetSlope(x, z);
                slopeKnown = true;
            }
            return slope;
        }

        Direction stair(LayoutPlanner planner, int x, int z) {
            if (!stairKnown) {
                stair = planner.computeStairDirection(x, z);
                stairKnown = true;
            }
            return stair;
        }
    }
}
