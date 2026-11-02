package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.ScatteredPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.city.BufferTarget;
import com.tuxprogrammer.lostcities.worldgen.city.CityWriteContext;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.city.PaletteSequence;
import com.tuxprogrammer.lostcities.worldgen.city.ScatteredFinder;
import com.tuxprogrammer.lostcities.worldgen.city.ScatteredWriter;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.GroundFit;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/**
 * {@code scattered.real}: the scattered structures of a real world (07-scattered, design record 0009), on any world
 * pinned with the layout plan source:
 * <ol>
 * <li><b>Rates</b>, from the plans alone over {@value #RATE_RADIUS} areas each way from the spawn: how many areas pass
 * the chance, how many select each entry against the odds the world style gives them (within four standard deviations
 * plus one), how many each entry places; and for every placed structure, that no footprint chunk lies in a landmark
 * zone, is city, or has a city chunk or landmark core within the blend's reach, and that an oil rig stands only in
 * deep ocean. Classic RWG must place no oil rig, RWG2 some.</li>
 * <li><b>Structures in the world</b>: generates and populates the region around the {@value #GENERATED} land
 * structures nearest the spawn outside its spawn area (and the nearest oil rig on a world with one), then checks each:
 * every block the structure writes stands in the world (a cut or half-placed structure fails); no dungeon spawner, no
 * lava within {@value #LAVA_DEPTH} blocks below its ground and no lake water over a pad in its footprint (deeper lava
 * is cave fill, under the ground the structure stands on); no terrain inside it; and the
 * ground around it meets RWG's terrain without a cliff: no step between neighboring ground columns of more than
 * {@value #MAX_STEP} blocks, where the ground fit acts, that the ground before the fit (the fit inverted) does
 * not have too.</li>
 * <li>Prints {@code [LCNH-HASH] scattered … terrain=… population=…} over the first structure's region, which two
 * fresh worlds of one seed and world type must share.</li>
 * </ol>
 *
 * <p>
 * Side: server.
 */
final class ScatteredScenario implements Scenario {

    /** Areas each way from the spawn's area that the rate check plans. */
    static final int RATE_RADIUS = 32;
    /** Land structures generated. */
    static final int GENERATED = 3;
    /** Chunks each way of a structure's footprint that are generated: the fade and the population margin. */
    static final int REGION_MARGIN = 3;
    /** Chunks searched for a structure. */
    static final int SEARCH = 256;
    /** Blocks below a structure's ground within which lava would be a lava lake cutting into it. */
    static final int LAVA_DEPTH = 8;
    /** The largest step between neighboring ground columns that is never a cliff. */
    static final int MAX_STEP = 2;
    /** Least share of a structure's blocks that must stand in the world: torch fix-ups may change a few. */
    static final double WHOLE_SHARE = 0.98;
    /** Most share of ground steps that may exceed the limit: a tree root or a boulder RWG's decoration placed. */
    static final double CLIFF_SHARE = 0.02;
    /** Tolerance of the rate check: four standard deviations plus one. */
    static final double SIGMAS = 4.0;
    /** Fixed-point steps that invert the ground fit's move. */
    static final int INVERSION_STEPS = 8;
    /** Failing places named per check. */
    static final int EXAMPLES = 6;

    private final List<ScatteredPlan> targets = new ArrayList<>();
    private final List<RegionGenerator> generators = new ArrayList<>();
    private int current;
    private boolean done;

    @Override
    public String name() {
        return "scattered.real";
    }

    @Override
    public int ticks() {
        return 20 * 60 * 10;
    }

    @Override
    public boolean finished(ScenarioContext context) {
        return done;
    }

    private static OverlayWorld overlay(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(overlay != null && overlay.cities() != null, "expected the overlay with the layout plan source");
        context.check(
            overlay.cities()
                .scattered() != null,
            "expected scattered structures: the module on and the world style's settings present");
        return overlay;
    }

    @Override
    public void setup(ScenarioContext context) {
        OverlayWorld overlay = overlay(context);
        ScatteredPlanner planner = overlay.cities()
            .scattered();
        ChunkCoordinates spawn = context.world()
            .getSpawnPoint();
        int min = CityScenarios.SPAWN_AREA + REGION_MARGIN + 2;
        int spawnX = spawn.posX >> 4;
        int spawnZ = spawn.posZ >> 4;
        Set<Long> seen = new HashSet<>();
        // The land structures nearest the spawn whose region lies outside the spawn area, so the run generates them.
        for (int ring = 0; ring <= SEARCH / planner.areaSize() && targets.size() < GENERATED; ring++) {
            List<ScatteredPlan> found = ringPlans(planner, planner.area(spawnX), planner.area(spawnZ), ring);
            for (ScatteredPlan plan : found) {
                if (targets.size() >= GENERATED || plan.asset()
                    .terrainHeight() == ScatteredView.TerrainHeight.OCEAN) {
                    continue;
                }
                if (Math.max(Math.abs(plan.minChunkX() - spawnX), Math.abs(plan.minChunkZ() - spawnZ)) >= min
                    && seen.add(key(plan))) {
                    targets.add(plan);
                }
            }
        }
        ScatteredFinder.Result rig = new ScatteredFinder(planner).find("oilrig", spawn.posX, spawn.posZ, SEARCH);
        if (rig != null) {
            targets.add(rig.plan());
        }
        context.check(!targets.isEmpty(), "expected a scattered structure within " + SEARCH + " chunks of the spawn");
        ScatteredPlan first = targets.get(0);
        overlay.recorder()
            .record(
                first.minChunkX() - 1,
                first.minChunkX() + first.width(),
                first.minChunkZ() - 1,
                first.minChunkZ() + first.depth());
        for (ScatteredPlan plan : targets) {
            int radius = Math.max(plan.width(), plan.depth()) / 2 + REGION_MARGIN;
            generators.add(
                new RegionGenerator(
                    context.world(),
                    plan.minChunkX() + plan.width() / 2,
                    plan.minChunkZ() + plan.depth() / 2,
                    radius));
        }
    }

    private static long key(ScatteredPlan plan) {
        return ((long) plan.areaX() << 32) ^ (plan.areaZ() & 0xffffffffL);
    }

    /** The valid plans of a square ring of areas, in a fixed order. */
    private static List<ScatteredPlan> ringPlans(ScatteredPlanner planner, int centerX, int centerZ, int ring) {
        List<ScatteredPlan> out = new ArrayList<>();
        for (int dz = -ring; dz <= ring; dz++) {
            for (int dx = -ring; dx <= ring; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                    continue;
                }
                ScatteredPlan plan = planner.planOfArea(centerX + dx, centerZ + dz);
                if (plan.valid()) {
                    out.add(plan);
                }
            }
        }
        return out;
    }

    @Override
    public void tick(ScenarioContext context) {
        if (current < generators.size() && generators.get(current)
            .tick()) {
            current++;
        }
        done = current >= generators.size();
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected every region generated, finished " + current + "/" + generators.size());
        OverlayWorld overlay = overlay(context);
        overlay.recorder()
            .stop();
        WorldServer world = context.world();
        boolean classic = "RWG".equalsIgnoreCase(HeightOracles.worldType(world));
        rates(context, overlay, classic);
        LayoutCityPlan cities = overlay.cities();
        CityWriteContext probe = cities.context();
        BlockRefs refs = new BlockRefs();
        for (ScatteredPlan plan : targets) {
            inWorld(context, overlay, probe, refs, plan);
        }
        LostCities.LOG.info(
            "[LCNH-SCATTERED] population vetoes beside scattered structures: {}",
            Overlays.INSTANCE.vetoes()
                .vetoedScattered());
        LostCities.LOG.info(
            "[LCNH-HASH] scattered worldType={} seed={} chunks={} terrain={} population={}",
            HeightOracles.worldType(world),
            world.getSeed(),
            overlay.recorder()
                .terrainHashes()
                .size(),
            OverlayScenarios.combined(
                overlay.recorder()
                    .terrainHashes()),
            OverlayScenarios.combined(
                overlay.recorder()
                    .populationHashes()));
        context.check(
            overlay.recorder()
                .terrainHashes()
                .size()
                == (targets.get(0)
                    .width() + 2)
                    * (targets.get(0)
                        .depth() + 2),
            "expected the first structure's region freshly generated, recorded " + overlay.recorder()
                .terrainHashes()
                .size() + " chunks; run on a fresh world (--fresh)");
    }

    /** The rate check over the plans of a square of areas around the spawn. */
    private void rates(ScenarioContext context, OverlayWorld overlay, boolean classic) {
        ScatteredPlanner planner = overlay.cities()
            .scattered();
        LayoutPlanner layout = overlay.cities()
            .planner();
        ChunkCoordinates spawn = context.world()
            .getSpawnPoint();
        int centerX = planner.area(spawn.posX >> 4);
        int centerZ = planner.area(spawn.posZ >> 4);
        List<String> names = planner.entryNames();
        double[] expected = new double[names.size()];
        double[] variance = new double[names.size()];
        int[] selected = new int[names.size()];
        // Near-highway entries: the areas with a spot beside a highway, and the odds summed over them.
        int[] spots = new int[names.size()];
        double[] spotExpected = new double[names.size()];
        double[] spotVariance = new double[names.size()];
        TreeMap<String, Integer> placed = new TreeMap<>();
        TreeMap<String, TreeMap<String, Integer>> reasons = new TreeMap<>();
        int areas = 0;
        int structures = 0;
        List<String> violations = new ArrayList<>();
        for (int dz = -RATE_RADIUS; dz <= RATE_RADIUS; dz++) {
            for (int dx = -RATE_RADIUS; dx <= RATE_RADIUS; dx++) {
                int ax = centerX + dx;
                int az = centerZ + dz;
                areas++;
                double[] odds = planner.selectionOdds(ax, az);
                for (int i = 0; i < odds.length; i++) {
                    expected[i] += odds[i];
                    variance[i] += odds[i] * (1 - odds[i]);
                    if (odds[i] > 0 && planner.hasHighwaySpot(names.get(i), ax, az)) {
                        spots[i]++;
                        spotExpected[i] += odds[i];
                        spotVariance[i] += odds[i] * (1 - odds[i]);
                    }
                }
                String pick = planner.selected(ax, az);
                if (pick != null) {
                    selected[names.indexOf(pick)]++;
                }
                ScatteredPlan plan = planner.planOfArea(ax, az);
                if (!plan.valid()) {
                    if (pick != null) {
                        reasons.computeIfAbsent(pick, k -> new TreeMap<>())
                            .merge(plan.reason(), 1, Integer::sum);
                    }
                    continue;
                }
                structures++;
                placed.merge(plan.entry(), 1, Integer::sum);
                check(overlay, layout, plan, violations);
            }
        }
        StringBuilder line = new StringBuilder();
        boolean ratesHold = true;
        for (int i = 0; i < names.size(); i++) {
            double sigma = Math.sqrt(variance[i]);
            boolean holds = Math.abs(selected[i] - expected[i]) <= SIGMAS * sigma + 1;
            ratesHold &= holds;
            line.append(
                String.format(
                    Locale.ROOT,
                    " %s selected=%d expected=%.1f±%.1f placed=%d%s",
                    names.get(i),
                    selected[i],
                    expected[i],
                    sigma,
                    placed.getOrDefault(names.get(i), 0),
                    holds ? "" : " OFF"));
        }
        LostCities.LOG.info(
            "[LCNH-SCATTERED] rates over {} areas of {} chunks: {} structures ({} per 100 areas);{}",
            areas,
            planner.areaSize(),
            structures,
            String.format(Locale.ROOT, "%.1f", 100.0 * structures / areas),
            line);
        LostCities.LOG.info("[LCNH-SCATTERED] selected but not placed, by reason: {}", reasons);
        StringBuilder highways = new StringBuilder();
        boolean highwayRatesHold = true;
        int highwaySpots = 0;
        for (int i = 0; i < names.size(); i++) {
            if (spots[i] == 0) {
                continue;
            }
            highwaySpots += spots[i];
            double sigma = Math.sqrt(spotVariance[i]);
            int count = placed.getOrDefault(names.get(i), 0);
            // Placement can still fail after a spot is found (build height, steep surroundings, a neighbor's
            // structure), so the expectation is a ceiling with the usual spread.
            boolean holds = count <= spotExpected[i] + SIGMAS * sigma + 1
                && count >= spotExpected[i] / 2 - SIGMAS * sigma - 1;
            highwayRatesHold &= holds;
            highways.append(
                String.format(
                    Locale.ROOT,
                    " %s areas with a spot=%d expected=%.1f±%.1f placed=%d%s",
                    names.get(i),
                    spots[i],
                    spotExpected[i],
                    sigma,
                    count,
                    holds ? "" : " OFF"));
        }
        LostCities.LOG.info("[LCNH-SCATTERED] beside highways:{}", highways.length() == 0 ? " no spots" : highways);
        for (int i = 0; i < Math.min(EXAMPLES, violations.size()); i++) {
            LostCities.LOG.info("[LCNH-SCATTERED] placement violation: {}", violations.get(i));
        }
        int rigs = placed.getOrDefault("lostcities:oilrig", 0);
        context.check(ratesHold, "expected each entry selected at the world style's odds, found" + line);
        context.check(
            highwayRatesHold,
            "expected near-highway structures placed at the odds of the areas beside highways, found" + highways);
        if (overlay.cities()
            .planner()
            .networks() != null && overlay.cities()
                .planner()
                .networks()
                .highways()
                .settings()
                .enabled()) {
            context.check(highwaySpots > 0, "expected spots beside highways around the spawn, found none");
        }
        context.check(violations.isEmpty(), violations.size() + " placement violations, first " + first(violations));
        context.check(structures > 0, "expected scattered structures around the spawn, found none");
        if (classic) {
            context.check(rigs == 0, "expected no oil rig on classic RWG, found " + rigs);
        } else {
            context.check(rigs > 0, "expected oil rigs in RWG2's deep ocean around the spawn, found none");
        }
    }

    /** Checks one placed structure's footprint against the zones, the cities and, for an oil rig, the deep ocean. */
    private static void check(OverlayWorld overlay, LayoutPlanner layout, ScatteredPlan plan, List<String> violations) {
        boolean ocean = plan.asset()
            .terrainHeight() == ScatteredView.TerrainHeight.OCEAN;
        for (int x = plan.minChunkX(); x < plan.minChunkX() + plan.width(); x++) {
            for (int z = plan.minChunkZ(); z < plan.minChunkZ() + plan.depth(); z++) {
                if (layout.zone(x, z)
                    .zone() != Zone.NONE) {
                    violations.add(plan.entry() + " in a landmark zone at " + x + "," + z);
                }
                ChunkPlan chunk = overlay.plan()
                    .chunk(x, z);
                if (chunk.isCity() || !chunk.isScattered()) {
                    violations.add(plan.entry() + " footprint chunk not scattered at " + x + "," + z);
                }
                for (int dz = -ScatteredPlanner.BLEND_CLEARANCE; dz <= ScatteredPlanner.BLEND_CLEARANCE; dz++) {
                    for (int dx = -ScatteredPlanner.BLEND_CLEARANCE; dx <= ScatteredPlanner.BLEND_CLEARANCE; dx++) {
                        ChunkPlan near = overlay.plan()
                            .chunk(x + dx, z + dz);
                        if (near.isCity() || near.isCore()) {
                            violations
                                .add(plan.entry() + " within the blend's reach of a city or core at " + x + "," + z);
                        }
                    }
                }
                if (ocean && (chunk.naturalHeight() > ScatteredPlanner.DEEP_OCEAN_FLOOR || !chunk.ocean())) {
                    violations.add(plan.entry() + " outside deep ocean at " + x + "," + z);
                }
            }
        }
    }

    /** The checks of one generated structure. */
    private static void inWorld(ScenarioContext context, OverlayWorld overlay, CityWriteContext probe, BlockRefs refs,
        ScatteredPlan plan) {
        WorldServer world = context.world();
        int expected = 0;
        int standing = 0;
        int dungeons = 0;
        int lava = 0;
        int lakes = 0;
        int inside = 0;
        List<String> missing = new ArrayList<>();
        GroundFit fit = null;
        for (ScatteredPlan.Share share : plan.shares()) {
            int cx = share.chunkX();
            int cz = share.chunkZ();
            ChunkPlan chunkPlan = overlay.plan()
                .chunk(cx, cz);
            if (!chunkPlan.groundFits()
                .isEmpty()) {
                fit = chunkPlan.groundFits()
                    .get(0);
            }
            Set<Long> ourSpawners = new HashSet<>();
            for (PopulationJob job : chunkPlan.jobs()) {
                if (job.kind() == PopulationJob.Kind.SPAWNER) {
                    ourSpawners.add(position(job.x(), job.y(), job.z()));
                }
            }
            ArrayChunkBuffer alone = new ArrayChunkBuffer();
            new ScatteredWriter(
                probe,
                plan,
                share,
                new BufferTarget(alone, null),
                new PaletteSequence(probe.worldSeed(), cx, cz)).write();
            boolean ocean = plan.asset()
                .terrainHeight() == ScatteredView.TerrainHeight.OCEAN;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int wx = (cx << 4) + x;
                    int wz = (cz << 4) + z;
                    boolean pad = fit != null && fit.distance(wx, wz) <= 0;
                    for (int y = 1; y < 256; y++) {
                        Block block = world.getBlock(wx, y, wz);
                        BlockRef want = alone.get(x, y, z);
                        // A repeated support slice reaches down to whatever ground is there; the probe has none.
                        if (!want.isAir() && y >= plan.lowestLevel()) {
                            expected++;
                            if (block == refs.block(want)) {
                                standing++;
                            } else if (missing.size() < EXAMPLES) {
                                missing.add(
                                    wx + ","
                                        + y
                                        + ","
                                        + wz
                                        + " "
                                        + want
                                        + " is "
                                        + Block.blockRegistry.getNameForObject(block));
                            }
                        }
                        if (block == Blocks.mob_spawner && !ourSpawners.contains(position(x, y, z))) {
                            dungeons++;
                        }
                        if ((block == Blocks.lava || block == Blocks.flowing_lava)
                            && y >= plan.groundY() - LAVA_DEPTH) {
                            if (lava++ == 0) {
                                missing.add("lava at " + wx + "," + y + "," + wz);
                            }
                        }
                        if (!ocean && pad
                            && y > plan.groundY()
                            && (block == Blocks.water || block == Blocks.flowing_water)) {
                            lakes++;
                        }
                        if (!ocean && pad && y == plan.groundY() + 1 && want.isAir() && isTerrain(block)) {
                            inside++;
                        }
                    }
                }
            }
        }
        double whole = expected == 0 ? 0 : (double) standing / expected;
        int[] steps = fit == null ? new int[3] : ground(world, plan, fit);
        LostCities.LOG.info(
            String.format(
                Locale.ROOT,
                "[LCNH-SCATTERED] %s at %d,%d (%dx%d chunks, %s, lowest %d, ground %d): blocks %d/%d standing,"
                    + " dungeon spawners %d, lava %d, lake water %d, terrain inside %d, ground steps in the fade %d cliffs of %d (beyond it %d)",
                plan.entry(),
                plan.minChunkX() << 4,
                plan.minChunkZ() << 4,
                plan.width(),
                plan.depth(),
                plan.rotation(),
                plan.lowestLevel(),
                plan.groundY(),
                standing,
                expected,
                dungeons,
                lava,
                lakes,
                inside,
                steps[0],
                steps[1],
                steps[2]));
        for (String place : missing) {
            LostCities.LOG.info("[LCNH-SCATTERED] example: {}", place);
        }
        context.check(expected > 0 && whole >= WHOLE_SHARE, plan.entry() + " stands " + standing + "/" + expected);
        context.check(dungeons == 0 && lava == 0 && lakes == 0, plan.entry() + " has a dungeon, lava or a lake");
        context.check(inside == 0, plan.entry() + " has terrain inside it: " + inside + " columns");
        context.check(
            steps[0] <= Math.max(1, (int) (steps[1] * CLIFF_SHARE)),
            plan.entry() + " meets the terrain with " + steps[0] + " cliffs");
    }

    /**
     * Counts the ground steps around a structure, over its footprint and the chunks around it, outside its pads: a
     * step of more than {@link #MAX_STEP} blocks between neighboring ground columns, where the fit acts, that the
     * ground before the fit (estimated by inverting it) does not have too, within a block, is a cliff the fit made.
     * Steps past
     * the fade are RWG's own and counted apart.
     *
     * @return {cliffs in the fade, steps compared there, cliffs past it}
     */
    private static int[] ground(WorldServer world, ScatteredPlan plan, GroundFit fit) {
        int minX = (plan.minChunkX() - GroundFit.REACH_CHUNKS) << 4;
        int minZ = (plan.minChunkZ() - GroundFit.REACH_CHUNKS) << 4;
        int sizeX = (plan.width() + 2 * GroundFit.REACH_CHUNKS) << 4;
        int sizeZ = (plan.depth() + 2 * GroundFit.REACH_CHUNKS) << 4;
        int[][] actual = new int[sizeX][sizeZ];
        double[][] before = new double[sizeX][sizeZ];
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                actual[x][z] = groundTop(world, minX + x, minZ + z);
                before[x][z] = beforeFit(fit, actual[x][z], minX + x, minZ + z);
            }
        }
        int cliffs = 0;
        int compared = 0;
        int beyond = 0;
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                double here = fit.distance(minX + x, minZ + z);
                if (here <= 0) {
                    continue;
                }
                for (int[] step : new int[][] { { 1, 0 }, { 0, 1 } }) {
                    int nx = x + step[0];
                    int nz = z + step[1];
                    if (nx >= sizeX || nz >= sizeZ) {
                        continue;
                    }
                    double there = fit.distance(minX + nx, minZ + nz);
                    if (there <= 0) {
                        continue;
                    }
                    int rise = Math.abs(actual[nx][nz] - actual[x][z]);
                    double naturalRise = Math.abs(before[nx][nz] - before[x][z]);
                    boolean cliff = rise > MAX_STEP && rise > naturalRise + 1;
                    if (fit.beyond(minX + x, minZ + z) && fit.beyond(minX + nx, minZ + nz)) {
                        // RWG's own ground, past the fade: its own cliffs (ravines, river banks), counted apart.
                        beyond += cliff ? 1 : 0;
                        continue;
                    }
                    compared++;
                    if (cliff && cliffs++ < EXAMPLES) {
                        LostCities.LOG.info(
                            "[LCNH-SCATTERED] step {} at {},{} to {},{}: ground {} to {}, before the fit {} to {}, fade weight {}",
                            rise,
                            minX + x,
                            minZ + z,
                            minX + nx,
                            minZ + nz,
                            actual[x][z],
                            actual[nx][nz],
                            String.format(Locale.ROOT, "%.1f", before[x][z]),
                            String.format(Locale.ROOT, "%.1f", before[nx][nz]),
                            String.format(Locale.ROOT, "%.2f", fit.weightAt(actual[x][z], minX + x, minZ + z)));
                    }
                }
            }
        }
        return new int[] { cliffs, compared, beyond };
    }

    /**
     * Estimates a column's ground before the fit from its ground after it, by inverting the fit's move ({@code t = s +
     * (groundY - s) w}): the scenario sees only the finished world, and RWG's ravines and river banks are cliffs of
     * their own that the fit must not be blamed for. Within a block of a pad, where the weight is 1, the answer is the
     * fit's ground; a column the cut-only mode left alone is its own.
     */
    private static double beforeFit(GroundFit fit, int after, int x, int z) {
        if (fit.distance(x, z) <= GroundFit.WALKWAY) {
            return fit.groundY();
        }
        if (fit.mode() == GroundFit.Mode.CUT && after <= fit.groundY()) {
            return after;
        }
        // The fade's length depends on the ground before the fit, so the inversion is a fixed-point iteration.
        double before = after;
        for (int i = 0; i < INVERSION_STEPS; i++) {
            double w = fit.weightAt((int) Math.round(before), x, z);
            if (w >= 1.0) {
                return fit.groundY();
            }
            before = (after - fit.groundY() * w) / (1.0 - w);
        }
        return before;
    }

    /**
     * The top ground block of a column: the highest block that is not air, a plant, a tree part, snow, liquid or a
     * boulder's cobblestone. RWG's boulders are decoration placed after the terrain phase, on whatever ground is
     * there; counted as ground they read as cliffs on every mountain.
     */
    private static int groundTop(WorldServer world, int x, int z) {
        for (int y = 255; y > 0; y--) {
            Block block = world.getBlock(x, y, z);
            if (isTerrain(block) && block != Blocks.cobblestone && block != Blocks.mossy_cobblestone) {
                return y;
            }
        }
        return 0;
    }

    private static boolean isTerrain(Block block) {
        Material material = block.getMaterial();
        return block != Blocks.air && !material.isLiquid()
            && material != Material.leaves
            && material != Material.wood
            && material != Material.plants
            && material != Material.vine
            && material != Material.snow
            && material != Material.gourd
            && material != Material.cactus
            && material != Material.web
            && material.blocksMovement();
    }

    private static long position(int x, int y, int z) {
        return ((long) y << 8) | (x << 4) | z;
    }

    private static String first(List<String> list) {
        return list.isEmpty() ? "none" : list.get(0);
    }
}
