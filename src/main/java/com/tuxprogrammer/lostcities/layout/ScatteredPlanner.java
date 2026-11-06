package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Scattered.java
// Avoids: SCAT-05, SCAT-06, SCAT-07, SCAT-09, SCAT-10, SCAT-11, SCAT-12, SCAT-14, SCAT-15, SCAT-01, SCAT-02, SCAT-03

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.layout.random.QualityRandom;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;
import com.tuxprogrammer.lostcities.layout.view.PartContext;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;

/**
 * Plans upstream's scattered structures (cabins, radio towers, oil rigs, and the gas stations and restaurants beside
 * highways) as a pure function of the world seed, the area, the profile, the assets, the height oracle, the biome
 * provider, the city layout and the highways. The world is cut into areas of the world style's {@code areasize}
 * chunks; each area holds at most one structure, planned once from a {@link QualityRandom} seeded by the world seed
 * and the area and replayed for every chunk of the structure, so all of them agree.
 *
 * <p>
 * Changes from upstream, each with a regression test:
 * <ul>
 * <li>SCAT-05: an entry wins when the draw is below its weight, so each entry and "none" get exactly their
 * weights;</li>
 * <li>SCAT-06: kept weights and {@code weightnone} that sum to 0 leave the area empty, with no draw;</li>
 * <li>SCAT-07: an entry is kept for selection when its biome filter accepts at least one chunk of the area, not only
 * the area's corner chunk; the footprint's own chunks are still tested (P07-1, D31);</li>
 * <li>SCAT-09: a single-chunk structure's {@code lowest}, {@code average} and {@code highest} take the chunk's
 * lowest, mean and highest oracle samples;</li>
 * <li>SCAT-10: the last floor is the top of the building in the part conditions, and the condition's level is a city
 * level, not a block Y;</li>
 * <li>SCAT-11: the overlay part's conditions see the chosen part;</li>
 * <li>SCAT-12: terrain stats are immutable values from the height oracle, never written into a shared height
 * map;</li>
 * <li>SCAT-14: plans live in two bounded caches this planner owns, and a dropped plan is recomputed to the same
 * value;</li>
 * <li>SCAT-15: the near-highway search reads only pure layout data, and stops testing a candidate at its first
 * rejected chunk;</li>
 * <li>SCAT-01 to SCAT-03: upstream's structure avoidance is not ported; footprints keep out of landmark zones, which
 * are pure and always known (0006).</li>
 * </ul>
 * Deviations, recorded in docs/new-spec/07-scattered/index.md and design record 0009: a footprint keeps out of every
 * landmark zone and meteorite cap, and has no city chunk or landmark core within {@link #BLEND_CLEARANCE} chunks, where
 * the city's terrain blend holds the ground within a few blocks of the city's, so the structure stands on the natural
 * ground its terrain stats measured; two
 * structures of neighboring areas never touch (the smaller area index keeps its structure), so neither stands in the
 * other's ground fit; an {@code ocean} structure needs the oracle floor at Y {@value #DEEP_OCEAN_FLOOR} or lower under
 * every footprint chunk (RWG2's deep ocean, D31); a placement whose top would pass Y {@value #MAX_TOP_Y} is
 * rejected; and a structure is planned whole or not at all: a missing building or a floor with no matching part
 * leaves the area empty.
 *
 * <p>
 * Not thread-safe (one generation thread). Spec: docs/original-spec/07-scattered/scattered-structures.md,
 * docs/new-spec/07-scattered/index.md. Side: server, and plain Java in tests.
 */
public final class ScatteredPlanner {

    /** Added to chunk coordinates before the integer division into areas, so it divides a positive number. */
    static final int AREA_OFFSET = 2_000_000;
    /** Plan seed multipliers: {@code seed + ax × 5564338337 + az × 25564337621}. */
    static final long AREA_X = 5564338337L;
    static final long AREA_Z = 25564337621L;
    /** Upstream's accurate-height sample offsets in a chunk: (2, 2), (2, 14), (14, 2), (14, 14). */
    static final int SAMPLE_LOW = 2;
    static final int SAMPLE_HIGH = 14;
    /** The sea level an {@code ocean} structure stands on: RWG's, the same as 1.20's. */
    public static final int SEA_LEVEL = LayoutProfile.RWG_SEA_LEVEL;
    /**
     * Highest oracle floor of deep ocean: RWG2's deep floor lies at Y 28.5 to 39.5, its shallow floor above 46 (D31).
     */
    public static final int DEEP_OCEAN_FLOOR = 45;
    /** Highest block Y a structure may reach: 1.7.10's {@code 256 − 2}. */
    public static final int MAX_TOP_Y = 254;
    /** Lowest block Y a structure's first floor may start at. */
    static final int MIN_LOWEST_Y = 2;
    /**
     * Chunks around a footprint that hold no city chunk and no landmark core. The terrain blend holds a chunk beside a
     * city within a few blocks of the city's ground and one two chunks away within about 22; three chunks away it
     * moves only ground more than about 40 blocks off the city's, which a footprint's height-difference test all but
     * rules out. So the ground under a structure is RWG's own, as its terrain stats measured it.
     */
    public static final int BLEND_CLEARANCE = 2;
    /** Chunks between two neighboring areas' footprints below which the smaller area index keeps its structure. */
    static final int TOUCH = 1;
    /** Blocks beyond the footprint where the near ring of the surroundings test samples the oracle. */
    static final int NEAR_RING = 6;
    /** Blocks beyond the footprint where the far ring samples. */
    static final int FAR_RING = 12;
    /**
     * Most blocks the ground on the near ring may lie off the structure's ground: the ground fit's fade then steepens
     * the land there by at most about one block a column (its steepest weight change is 1.5 / 13 a block).
     */
    static final int NEAR_TOLERANCE = 8;
    /** Most blocks the ground on the far ring may lie off the structure's ground, where the fade has nearly ended. */
    static final int FAR_TOLERANCE = 12;

    private final LayoutPlanner planner;
    private final ScatteredView settings;
    private final float chanceMultiplier;
    private final ChunkCache<ScatteredPlan> raw;
    private final ChunkCache<ScatteredPlan> plans;
    private final Set<String> warned = new HashSet<>();
    /** Why the last terrain check failed, for the plan's {@link ScatteredPlan#reason()}. */
    private String rejection = "terrain";

    /**
     * Creates the scattered planner of one dimension.
     *
     * @param planner          the dimension's city layout
     * @param chanceMultiplier the profile's {@code scatteredChanceMultiplier}, 0 to 100
     * @param cacheSize        entries of each plan cache, at least 64
     */
    public ScatteredPlanner(LayoutPlanner planner, float chanceMultiplier, int cacheSize) {
        this.planner = planner;
        this.settings = planner.worldStyle.scattered();
        this.chanceMultiplier = chanceMultiplier;
        int size = Math.max(64, cacheSize);
        this.raw = new ChunkCache<>(size);
        this.plans = new ChunkCache<>(size);
    }

    /** Returns whether the world style has scattered settings: without them no area holds a structure. */
    public boolean enabled() {
        return settings != null && !settings.entries()
            .isEmpty();
    }

    /** Returns the area size in chunks, or 0 when disabled. */
    public int areaSize() {
        return enabled() ? Math.max(1, settings.areaSize()) : 0;
    }

    /**
     * Returns the area index along one axis that holds a chunk coordinate.
     *
     * @param chunk chunk X or Z
     * @return the area index
     */
    public int area(int chunk) {
        return Math.floorDiv(chunk + AREA_OFFSET, areaSize());
    }

    /**
     * Returns the plan of an area: its structure, or {@link ScatteredPlan#valid()} false.
     *
     * @param areaX area X index
     * @param areaZ area Z index
     * @return the plan
     */
    public ScatteredPlan planOfArea(int areaX, int areaZ) {
        if (!enabled()) {
            return ScatteredPlan.NONE;
        }
        ScatteredPlan cached = plans.get(areaX, areaZ);
        if (cached != null) {
            return cached;
        }
        ScatteredPlan plan = rawPlan(areaX, areaZ);
        if (plan.valid() && yieldsToNeighbor(plan)) {
            plan = ScatteredPlan.none("neighbor");
        }
        return plans.put(areaX, areaZ, plan);
    }

    /**
     * Returns the structure whose footprint holds a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the plan, or {@code null}
     */
    public ScatteredPlan planAt(int chunkX, int chunkZ) {
        if (!enabled()) {
            return null;
        }
        ScatteredPlan plan = planOfArea(area(chunkX), area(chunkZ));
        return plan.covers(chunkX, chunkZ) ? plan : null;
    }

    /**
     * Returns whether a chunk lies in a scattered structure's footprint: what M08's Roguelike steering mixin rejects
     * (0006 §5, P07-5).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether a structure stands there
     */
    public boolean inFootprint(int chunkX, int chunkZ) {
        return planAt(chunkX, chunkZ) != null;
    }

    /**
     * Returns the structures whose footprint lies within some chunks of a chunk, in area order (Z, then X).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param reach  chunks around a footprint, less than the area size
     * @return the plans; empty when none
     */
    public List<ScatteredPlan> plansReaching(int chunkX, int chunkZ, int reach) {
        if (!enabled()) {
            return Collections.emptyList();
        }
        List<ScatteredPlan> out = null;
        int fromX = area(chunkX - reach);
        int toX = area(chunkX + reach);
        int fromZ = area(chunkZ - reach);
        int toZ = area(chunkZ + reach);
        for (int az = fromZ; az <= toZ; az++) {
            for (int ax = fromX; ax <= toX; ax++) {
                ScatteredPlan plan = planOfArea(ax, az);
                if (plan.reaches(chunkX, chunkZ, reach)) {
                    if (out == null) {
                        out = new ArrayList<>(2);
                    }
                    out.add(plan);
                }
            }
        }
        return out == null ? Collections.<ScatteredPlan>emptyList() : out;
    }

    /**
     * Upstream's {@code getHighwayRailingOpenings}, side by side, for M06's highway writer: whether a highway chunk
     * leaves its railing open toward a neighbor, because a structure with {@code clearhighwayrailing} stands there,
     * faces this highway chunk and was placed at this highway's height.
     *
     * @param highwayX      the highway chunk's X
     * @param highwayZ      the highway chunk's Z
     * @param side          the side toward the neighbor
     * @param highwayHeight the highway's block Y ({@code groundLevel + 6 × level})
     * @return whether the railing opens on that side
     */
    public boolean opensRailing(int highwayX, int highwayZ, Direction side, int highwayHeight) {
        ScatteredPlan plan = planAt(highwayX + side.stepX(), highwayZ + side.stepZ());
        if (plan == null || !plan.asset()
            .clearHighwayRailing() || plan.highwayHeight() == null || plan.highwayHeight() != highwayHeight) {
            return false;
        }
        return isConnectedHighwayChunk(plan, highwayX, highwayZ);
    }

    private static boolean isConnectedHighwayChunk(ScatteredPlan plan, int highwayX, int highwayZ) {
        int minX = plan.minChunkX();
        int minZ = plan.minChunkZ();
        boolean alongX = highwayX >= minX && highwayX < minX + plan.width();
        boolean alongZ = highwayZ >= minZ && highwayZ < minZ + plan.depth();
        switch (plan.rotation()
            .front()) {
            case ZMIN:
                return highwayZ == minZ - 1 && alongX;
            case ZMAX:
                return highwayZ == minZ + plan.depth() && alongX;
            case XMIN:
                return highwayX == minX - 1 && alongZ;
            default:
                return highwayX == minX + plan.width() && alongZ;
        }
    }

    // ================================================================== rates, for the scenarios

    /** Returns the world style's entry names, in list order; empty when disabled. */
    public List<String> entryNames() {
        List<String> names = new ArrayList<>();
        if (enabled()) {
            for (ScatteredView.Entry entry : settings.entries()) {
                names.add(entry.name());
            }
        }
        return names;
    }

    /**
     * Returns the entry an area's draws select, before placement: the chance draw and the weighted pick, as the plan
     * takes them. For the rate checks of the {@code scattered.real} scenario.
     *
     * @param areaX area X index
     * @param areaZ area Z index
     * @return the entry's name, or {@code null} when the chance fails or "none" is drawn
     */
    public String selected(int areaX, int areaZ) {
        if (!enabled()) {
            return null;
        }
        QualityRandom random = new QualityRandom(planSeed(planner.seed, areaX, areaZ));
        if (random.nextFloat() >= settings.chance() * chanceMultiplier) {
            return null;
        }
        int size = areaSize();
        ScatteredView.Entry entry = select(areaX * size - AREA_OFFSET, areaZ * size - AREA_OFFSET, size, random);
        return entry == null ? null : entry.name();
    }

    /**
     * Returns the odds an area selects each entry, in list order: the chance (with the profile's multiplier) times the
     * entry's weight over the weights of the entries its biomes keep plus {@code weightnone}.
     *
     * @param areaX area X index
     * @param areaZ area Z index
     * @return one probability per entry
     */
    public double[] selectionOdds(int areaX, int areaZ) {
        List<ScatteredView.Entry> entries = enabled() ? settings.entries()
            : Collections.<ScatteredView.Entry>emptyList();
        double[] odds = new double[entries.size()];
        if (entries.isEmpty()) {
            return odds;
        }
        int size = areaSize();
        int originX = areaX * size - AREA_OFFSET;
        int originZ = areaZ * size - AREA_OFFSET;
        boolean[] kept = new boolean[entries.size()];
        int bound = Math.max(0, settings.weightNone());
        for (int i = 0; i < entries.size(); i++) {
            ScatteredView.Entry entry = entries.get(i);
            kept[i] = entry.asset() != null && acceptsSomeChunk(entry, originX, originZ, size);
            bound += kept[i] ? Math.max(0, entry.weight()) : 0;
        }
        double chance = Math.min(1.0, Math.max(0.0, settings.chance() * chanceMultiplier));
        for (int i = 0; i < entries.size(); i++) {
            odds[i] = kept[i] && bound > 0 ? chance * Math.max(
                0,
                entries.get(i)
                    .weight())
                / bound : 0.0;
        }
        return odds;
    }

    /**
     * Returns whether a near-highway entry finds a spot in an area: a footprint, in any rotation it allows, whose front
     * edge meets a highway at one height and whose chunks pass the entry's terrain checks. It takes no draw. For the
     * near-highway rate check of the {@code scattered.real} scenario: such an entry is placed in about the areas that
     * have a spot, at the odds they select it.
     *
     * @param entryName the entry's name
     * @param areaX     area X index
     * @param areaZ     area Z index
     * @return {@code false} for an entry that does not stand beside highways, or an unknown one
     */
    public boolean hasHighwaySpot(String entryName, int areaX, int areaZ) {
        if (!enabled()) {
            return false;
        }
        for (ScatteredView.Entry entry : settings.entries()) {
            if (!entry.name()
                .equals(entryName) || !entry.nearHighway() || entry.asset() == null) {
                continue;
            }
            int width = 1;
            int depth = 1;
            if (entry.asset()
                .multiBuilding() != null) {
                MultiBuildingView multi = planner.assets.multiBuilding(
                    entry.asset()
                        .multiBuilding());
                if (multi == null) {
                    return false;
                }
                width = multi.dimX();
                depth = multi.dimZ();
            }
            int size = areaSize();
            return width <= size && depth <= size
                && !highwayPlacements(entry, areaX * size - AREA_OFFSET, areaZ * size - AREA_OFFSET, size, width, depth)
                    .isEmpty();
        }
        return false;
    }

    // ================================================================== the plan

    /** An area's plan before neighboring areas are compared. */
    ScatteredPlan rawPlan(int areaX, int areaZ) {
        ScatteredPlan cached = raw.get(areaX, areaZ);
        if (cached != null) {
            return cached;
        }
        return raw.put(areaX, areaZ, computePlan(areaX, areaZ));
    }

    /**
     * Returns whether a planned structure gives way to one of a neighboring area that it touches: the one of the
     * smaller area index (Z, then X) keeps its place. Both are raw plans, so the answer never depends on order.
     */
    private boolean yieldsToNeighbor(ScatteredPlan plan) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                boolean before = dz < 0 || dz == 0 && dx < 0;
                if (!before) {
                    continue;
                }
                ScatteredPlan other = rawPlan(plan.areaX() + dx, plan.areaZ() + dz);
                if (other.valid() && touches(plan, other)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether two footprints overlap or lie within {@link #TOUCH} chunk of each other. */
    static boolean touches(ScatteredPlan a, ScatteredPlan b) {
        return a.minChunkX() - TOUCH < b.minChunkX() + b.width() && b.minChunkX() - TOUCH < a.minChunkX() + a.width()
            && a.minChunkZ() - TOUCH < b.minChunkZ() + b.depth()
            && b.minChunkZ() - TOUCH < a.minChunkZ() + a.depth();
    }

    /** Upstream's {@code calculatePlan}. */
    private ScatteredPlan computePlan(int areaX, int areaZ) {
        int areaSize = areaSize();
        long seed = planSeed(planner.seed, areaX, areaZ);
        QualityRandom random = new QualityRandom(seed);
        if (random.nextFloat() >= settings.chance() * chanceMultiplier) {
            return ScatteredPlan.none("chance");
        }
        int originX = areaX * areaSize - AREA_OFFSET;
        int originZ = areaZ * areaSize - AREA_OFFSET;
        ScatteredView.Entry entry = select(originX, originZ, areaSize, random);
        if (entry == null) {
            return ScatteredPlan.none("none drawn");
        }
        ScatteredView.Asset asset = entry.asset();
        MultiBuildingView multi = null;
        if (asset.multiBuilding() != null) {
            multi = planner.assets.multiBuilding(asset.multiBuilding());
            if (multi == null) {
                warnOnce(
                    "multi:" + asset.multiBuilding(),
                    "scattered " + asset.name() + " names multibuilding " + asset.multiBuilding() + ", not loaded");
                return ScatteredPlan.none("asset");
            }
        }
        int originalWidth = multi == null ? 1 : multi.dimX();
        int originalDepth = multi == null ? 1 : multi.dimZ();
        if (originalWidth > areaSize || originalDepth > areaSize) {
            return ScatteredPlan.none("asset");
        }
        Candidate placement;
        if (entry.nearHighway()) {
            List<Candidate> candidates = highwayPlacements(
                entry,
                originX,
                originZ,
                areaSize,
                originalWidth,
                originalDepth);
            if (candidates.isEmpty()) {
                return ScatteredPlan.none("no highway");
            }
            placement = candidates.get(random.nextInt(candidates.size()));
        } else {
            int x = originX + random.nextInt(areaSize - originalWidth + 1);
            int z = originZ + random.nextInt(areaSize - originalDepth + 1);
            Stats stats = terrainStats(entry, x, z, originalWidth, originalDepth);
            if (stats == null) {
                return ScatteredPlan.none(rejection);
            }
            placement = new Candidate(x, z, originalWidth, originalDepth, ScatteredPlan.Rotation.NONE, null, stats);
        }
        String single = null;
        if (multi == null) {
            List<String> buildings = asset.buildings();
            if (buildings.isEmpty()) {
                warnOnce("buildings:" + asset.name(), "scattered " + asset.name() + " names no building");
                return ScatteredPlan.none("asset");
            }
            single = buildings.size() == 1 ? buildings.get(0) : buildings.get(random.nextInt(buildings.size()));
        }
        int lowest = lowestLevel(asset, placement);
        if (lowest < MIN_LOWEST_Y || lowest + asset.maxHeight() > MAX_TOP_Y) {
            // 1.7.10's build range: a structure that would leave it is rejected like a failed terrain check.
            return ScatteredPlan.none("build height");
        }
        if (asset.terrainHeight() != ScatteredView.TerrainHeight.OCEAN
            && lowest - asset.heightOffset() - 1 < SEA_LEVEL - 1) {
            // A land structure's ground below the sea's surface would leave its floor under water.
            return ScatteredPlan.none("under water");
        }
        if (asset.terrainHeight() != ScatteredView.TerrainHeight.OCEAN && !fitsSurroundings(placement, lowest, asset)) {
            // 0009: the ground fit would have to steepen the land around it into a wall.
            return ScatteredPlan.none("steep surroundings");
        }
        ScatteredPlan.Share[] shares = new ScatteredPlan.Share[placement.width * placement.depth];
        long draws = random.drawCount();
        for (int dz = 0; dz < placement.depth; dz++) {
            for (int dx = 0; dx < placement.width; dx++) {
                String name = single != null ? single : buildingAt(multi, dx, dz, placement.rotation);
                ScatteredPlan.Share share = share(seed, draws, placement.x + dx, placement.z + dz, name, lowest, asset);
                if (share == null) {
                    return ScatteredPlan.none("asset");
                }
                shares[dz * placement.width + dx] = share;
            }
        }
        return new ScatteredPlan(
            areaX,
            areaZ,
            entry.name(),
            asset,
            placement.x,
            placement.z,
            placement.width,
            placement.depth,
            placement.rotation,
            lowest,
            placement.highwayHeight,
            placement.stats.min,
            placement.stats.max,
            shares);
    }

    /** Returns the plan seed of an area. */
    static long planSeed(long worldSeed, int areaX, int areaZ) {
        return worldSeed + areaX * AREA_X + areaZ * AREA_Z;
    }

    /**
     * Upstream's {@code selectRandomScattered}, with SCAT-05, SCAT-06 and SCAT-07 fixed: the entries whose biome filter
     * accepts some chunk of the area, one draw over their weights and {@code weightnone}, and an entry wins when the
     * draw is below its weight.
     */
    private ScatteredView.Entry select(int originX, int originZ, int areaSize, QualityRandom random) {
        List<ScatteredView.Entry> kept = new ArrayList<>();
        int total = 0;
        for (ScatteredView.Entry entry : settings.entries()) {
            if (entry.asset() == null) {
                warnOnce("asset:" + entry.name(), "scattered entry " + entry.name() + " has no loaded asset; skipped");
                continue;
            }
            if (acceptsSomeChunk(entry, originX, originZ, areaSize)) {
                kept.add(entry);
                total += Math.max(0, entry.weight());
            }
        }
        int bound = total + Math.max(0, settings.weightNone());
        if (kept.isEmpty() || bound <= 0) {
            // Avoids SCAT-06: nothing to draw from leaves the area empty instead of throwing.
            return null;
        }
        int draw = random.nextInt(bound);
        for (ScatteredView.Entry entry : kept) {
            int weight = Math.max(0, entry.weight());
            // Avoids SCAT-05: "<" gives each entry exactly its weight; upstream's "<=" gave the first one more.
            if (draw < weight) {
                return entry;
            }
            draw -= weight;
        }
        return null;
    }

    /** Avoids SCAT-07: whether an entry's biome filter accepts at least one chunk of the area. */
    private static boolean acceptsSomeChunk(ScatteredView.Entry entry, int originX, int originZ, int areaSize) {
        for (int dz = 0; dz < areaSize; dz++) {
            for (int dx = 0; dx < areaSize; dx++) {
                if (entry.acceptsBiome(originX + dx, originZ + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Upstream's {@code findHighwayPlacements}: every rotation, X then Z, the front on a highway at one level. */
    private List<Candidate> highwayPlacements(ScatteredView.Entry entry, int originX, int originZ, int areaSize,
        int originalWidth, int originalDepth) {
        List<Candidate> candidates = new ArrayList<>();
        ScatteredPlan.Rotation[] rotations = entry.asset()
            .rotatable() ? ScatteredPlan.Rotation.values()
                : new ScatteredPlan.Rotation[] { ScatteredPlan.Rotation.NONE };
        for (ScatteredPlan.Rotation rotation : rotations) {
            int width = rotation.swapsDimensions() ? originalDepth : originalWidth;
            int depth = rotation.swapsDimensions() ? originalWidth : originalDepth;
            for (int x = originX; x <= originX + areaSize - width; x++) {
                for (int z = originZ; z <= originZ + areaSize - depth; z++) {
                    Integer height = connectedHighwayHeight(x, z, width, depth, rotation.front());
                    if (height == null) {
                        continue;
                    }
                    Stats stats = terrainStats(entry, x, z, width, depth);
                    if (stats != null) {
                        candidates.add(new Candidate(x, z, width, depth, rotation, height, stats));
                    }
                }
            }
        }
        return candidates;
    }

    /**
     * Upstream's {@code getConnectedHighwayHeight}: the common block Y of the highway along the footprint's front edge,
     * or {@code null} when a chunk of that edge has none at that axis, tunnels, or sits at another level.
     */
    Integer connectedHighwayHeight(int x, int z, int width, int depth, Direction front) {
        Integer common = null;
        boolean alongX = front.orientation() == Orientation.Z;
        int edge = alongX ? width : depth;
        for (int i = 0; i < edge; i++) {
            int hx;
            int hz;
            switch (front) {
                case ZMIN:
                    hx = x + i;
                    hz = z - 1;
                    break;
                case ZMAX:
                    hx = x + i;
                    hz = z + depth;
                    break;
                case XMIN:
                    hx = x - 1;
                    hz = z + i;
                    break;
                default:
                    hx = x + width;
                    hz = z + i;
                    break;
            }
            // A north or south front meets a highway running along X; an east or west front one along Z.
            int level = alongX ? planner.infrastructure.highwayXLevel(hx, hz)
                : planner.infrastructure.highwayZLevel(hx, hz);
            if (level < 0) {
                return null;
            }
            ChunkPlan highway = planner.plan(hx, hz);
            if (highway.isTunnel(level, planner.terrain.height(hx, hz))) {
                return null;
            }
            int height = highway.groundLevel() + level * ChunkPlan.LEVEL_HEIGHT;
            if (common != null && common != height) {
                return null;
            }
            common = height;
        }
        return common;
    }

    /**
     * Upstream's {@code calculateTerrainStats} over the oracle (SCAT-12): every footprint chunk must pass the entry's
     * biome filter, keep out of cities, landmarks, bridges and highways, and, for an {@code ocean} structure, stand in
     * deep water; the result is the lowest and highest of each chunk's height and its four samples, and the mean
     * chunk height. Stops at the first rejected chunk (SCAT-15).
     */
    Stats terrainStats(ScatteredView.Entry entry, int minX, int minZ, int width, int depth) {
        boolean ocean = entry.asset()
            .terrainHeight() == ScatteredView.TerrainHeight.OCEAN;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0;
        for (int x = minX; x < minX + width; x++) {
            for (int z = minZ; z < minZ + depth; z++) {
                if (!entry.acceptsBiome(x, z)) {
                    rejection = "biome";
                    return null;
                }
                String avoided = avoidReason(x, z);
                if (avoided != null) {
                    rejection = avoided;
                    return null;
                }
                int height = planner.terrain.height(x, z);
                if (ocean && height > DEEP_OCEAN_FLOOR) {
                    // D31: an ocean structure (the oil rig) stands in deep water only.
                    rejection = "shallow water";
                    return null;
                }
                int chunkMin = height;
                int chunkMax = height;
                for (int sx = SAMPLE_LOW; sx <= SAMPLE_HIGH; sx += SAMPLE_HIGH - SAMPLE_LOW) {
                    for (int sz = SAMPLE_LOW; sz <= SAMPLE_HIGH; sz += SAMPLE_HIGH - SAMPLE_LOW) {
                        int sample = planner.terrain.columnHeight((x << 4) + sx, (z << 4) + sz);
                        chunkMin = Math.min(chunkMin, sample);
                        chunkMax = Math.max(chunkMax, sample);
                    }
                }
                min = Math.min(min, chunkMin);
                max = Math.max(max, chunkMax);
                sum += height;
            }
        }
        Integer limit = entry.maxHeightDiff();
        if (limit != null && max - min > limit) {
            rejection = "height difference";
            return null;
        }
        return new Stats(min, max, (int) (sum / (width * depth)));
    }

    /**
     * Returns whether a chunk never holds part of a scattered structure: upstream's {@code avoidScattered} (city,
     * bridge and highway chunks) with landmark zones and the blend's clearance in place of structure avoidance.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} to keep out
     */
    public boolean avoids(int chunkX, int chunkZ) {
        return avoidReason(chunkX, chunkZ) != null;
    }

    /** Returns why a chunk holds no scattered structure, or {@code null} when it may. */
    private String avoidReason(int chunkX, int chunkZ) {
        ZoneInfo zone = planner.zone(chunkX, chunkZ);
        if (zone.zone() != Zone.NONE || zone.heightCap() != Integer.MAX_VALUE) {
            // Cores, margins, transition and avoid rings, and a meteorite crater's reach (0006 §1).
            return "landmark";
        }
        if (planner.hasHighway(chunkX, chunkZ) || planner.bridgePart(chunkX, chunkZ, Orientation.X) != null
            || planner.bridgePart(chunkX, chunkZ, Orientation.Z) != null) {
            return "bridge or highway";
        }
        for (int dz = -BLEND_CLEARANCE; dz <= BLEND_CLEARANCE; dz++) {
            for (int dx = -BLEND_CLEARANCE; dx <= BLEND_CLEARANCE; dx++) {
                // A chunk is city only if it is raw city, the cheap test; only then are its characteristics read.
                if (planner.isCityRaw(chunkX + dx, chunkZ + dz) && planner.characteristics(chunkX + dx, chunkZ + dz)
                    .isCity()) {
                    return "near a city";
                }
                if (planner.zone(chunkX + dx, chunkZ + dz)
                    .zone() == Zone.CORE) {
                    return "near a landmark";
                }
            }
        }
        return null;
    }

    /**
     * Returns whether the land around a footprint lies close enough to the structure's ground for the ground fit to
     * meet it without a wall (design record 0009): the oracle's ground at sixteen points on each of two rings, the
     * footprint's corners, quarter and middle points pushed {@link #NEAR_RING} and {@link #FAR_RING} blocks out, lies
     * within {@link #NEAR_TOLERANCE} and {@link #FAR_TOLERANCE} blocks of it. A structure on supports
     * ({@code repeatslice}) only cuts higher ground, so only ground above it counts. Upstream tested only the samples
     * under the footprint, so a cabin could stand on a ledge of a mountain.
     */
    boolean fitsSurroundings(Candidate placement, int lowest, ScatteredView.Asset asset) {
        int ground = lowest - asset.heightOffset() - 1;
        boolean supported = asset.terrainFix() == ScatteredView.TerrainFix.REPEATSLICE;
        int minX = placement.x << 4;
        int minZ = placement.z << 4;
        int maxX = ((placement.x + placement.width) << 4) - 1;
        int maxZ = ((placement.z + placement.depth) << 4) - 1;
        int[] ring = { NEAR_RING, FAR_RING };
        int[] tolerance = { NEAR_TOLERANCE, FAR_TOLERANCE };
        for (int r = 0; r < ring.length; r++) {
            for (int i = 0; i <= 4; i++) {
                int x = minX + (maxX - minX) * i / 4;
                int z = minZ + (maxZ - minZ) * i / 4;
                int[][] points = { { x, minZ - ring[r] }, { x, maxZ + ring[r] }, { minX - ring[r], z },
                    { maxX + ring[r], z } };
                for (int[] point : points) {
                    // The oracle answers the first air above the ground; the ground's top block is one lower.
                    int off = planner.terrain.columnHeight(point[0], point[1]) - 1 - ground;
                    if (off > tolerance[r] || !supported && -off > tolerance[r]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Upstream's {@code handleScatteredTerrain} and {@code handleScatteredTerrainMulti}, SCAT-09 fixed. */
    private static int lowestLevel(ScatteredView.Asset asset, Candidate placement) {
        if (placement.highwayHeight != null) {
            return placement.highwayHeight + asset.heightOffset();
        }
        int level;
        switch (asset.terrainHeight()) {
            case LOWEST:
                level = placement.stats.min;
                break;
            case AVERAGE:
                level = placement.stats.average;
                break;
            case HIGHEST:
                level = placement.stats.max;
                break;
            default:
                level = SEA_LEVEL;
                break;
        }
        return level + asset.heightOffset();
    }

    /** Upstream's {@code toOriginalCoordinate}: the multibuilding's grid position of a placed footprint chunk. */
    static String buildingAt(MultiBuildingView multi, int x, int z, ScatteredPlan.Rotation rotation) {
        switch (rotation) {
            case ROTATE_90:
                return multi.buildingAt(z, multi.dimZ() - 1 - x);
            case ROTATE_180:
                return multi.buildingAt(multi.dimX() - 1 - x, multi.dimZ() - 1 - z);
            case ROTATE_270:
                return multi.buildingAt(multi.dimX() - 1 - z, x);
            default:
                return multi.buildingAt(x, z);
        }
    }

    /**
     * Upstream's {@code generateScatteredBuilding} draws for one footprint chunk, taken at plan time: the plan's random
     * replayed (the same {@code draws} values the plan took), then the floor count and each floor's part and overlay
     * part. Returns {@code null} when the building is not loaded or a floor has no part, so the whole structure is
     * dropped rather than half placed.
     */
    private ScatteredPlan.Share share(long seed, long draws, int chunkX, int chunkZ, String name, int lowest,
        ScatteredView.Asset asset) {
        BuildingView building = name == null ? null : planner.assets.building(name);
        if (building == null) {
            warnOnce("building:" + name, "scattered " + asset.name() + " names building " + name + ", not loaded");
            return null;
        }
        QualityRandom random = new QualityRandom(seed);
        random.skip(draws);
        int min = Math.max(1, building.minFloors());
        int max = Math.max(1, building.maxFloors());
        int floors = min >= max ? min : min + random.nextInt(max - min + 1);
        String[] parts = new String[floors];
        String[] parts2 = new String[floors];
        int baseLevel = Math.floorDiv(lowest - planner.profile.groundLevel, ChunkPlan.LEVEL_HEIGHT);
        String below = PartContext.NONE;
        for (int f = 0; f < floors; f++) {
            // Avoids SCAT-10: the last floor is the top (floors − 1 above ground), and the level is a city level.
            PartContext context = new PartContext(
                baseLevel + f,
                f,
                0,
                floors - 1,
                PartContext.NONE,
                below,
                building.name(),
                chunkX,
                chunkZ);
            String part = building.pickPart(random, context);
            if (part == null) {
                warnOnce(
                    "parts:" + building.name(),
                    "scattered building " + building.name() + " has no part for floor " + f + "; nothing is placed");
                return null;
            }
            // Avoids SCAT-11: the overlay's conditions see the chosen part, as a city building's do.
            PartContext withPart = new PartContext(
                baseLevel + f,
                f,
                0,
                floors - 1,
                part,
                below,
                building.name(),
                chunkX,
                chunkZ);
            parts[f] = part;
            parts2[f] = building.pickPart2(random, withPart);
            below = part;
        }
        return new ScatteredPlan.Share(chunkX, chunkZ, building.name(), parts, parts2);
    }

    private void warnOnce(String key, String message) {
        if (warned.add(key)) {
            planner.log.warn(message);
        }
    }

    /** Terrain stats of a footprint: immutable (SCAT-12). */
    static final class Stats {

        final int min;
        final int max;
        final int average;

        Stats(int min, int max, int average) {
            this.min = min;
            this.max = max;
            this.average = average;
        }
    }

    /** A placement: footprint, rotation, highway height and terrain. */
    static final class Candidate {

        final int x;
        final int z;
        final int width;
        final int depth;
        final ScatteredPlan.Rotation rotation;
        final Integer highwayHeight;
        final Stats stats;

        Candidate(int x, int z, int width, int depth, ScatteredPlan.Rotation rotation, Integer highwayHeight,
            Stats stats) {
            this.x = x;
            this.z = z;
            this.width = width;
            this.depth = depth;
            this.rotation = rotation;
            this.highwayHeight = highwayHeight;
            this.stats = stats;
        }
    }
}
