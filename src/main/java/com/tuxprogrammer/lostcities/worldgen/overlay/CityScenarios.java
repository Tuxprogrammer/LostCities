package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockStairs;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.Doorway;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.city.CityFinder;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * The real cities' in-world scenarios (M05; docs/testing/README.md), for a world pinned with the layout plan source:
 * <ul>
 * <li>{@code city.real}: finds the city center nearest the world's spawn, measures how far the nearest city chunk is
 * from the spawn and from block (0, 0), generates the region around that center on a fresh world, checks its streets,
 * buildings and footprint, and prints the chunk hashes two same-seed worlds must share (D80);</li>
 * <li>{@code city.walk}: the walkability check over the same region: every floor of a building has a floor, its outer
 * walls are closed except at doorways, stairs or ladders join each floor to the next, and every door the layout puts in
 * a ground floor opens onto open space. Ruined buildings and chunks an explosion reaches are exempt: that damage is
 * upstream's own;</li>
 * <li>{@code city.budget} (manual only): budgets B1 to B5 over 65 × 65 chunks beside that city, the method of D81
 * with real content.</li>
 * </ul>
 *
 * <p>
 * Side: server.
 */
public final class CityScenarios {

    /** Half-side of the spawn area a dedicated server generates and keeps loaded at start, in chunks. */
    static final int SPAWN_AREA = 13;
    /** Chunks searched around the spawn for the nearest city center. */
    static final int CENTER_SEARCH = 96;
    /** Chunks searched around a point for the nearest city chunk. */
    static final int CITY_SEARCH = 64;
    /** Half-side of the generated region, in chunks: the recorded square plus the blend and population margin. */
    static final int REGION_RADIUS = 10;
    /** Budget region: the D81 method's 65 × 65 chunks, its center this far from the city. */
    static final int BUDGET_RADIUS = 32;
    static final int BUDGET_CITY_OFFSET = 20;
    static final int WARM_UP_CHUNKS = 512;

    /** Least share of a floor's inner columns that must be solid for the floor to count as a floor. */
    static final double FLOOR_SOLID_SHARE = 0.5;
    /** How far inside the chunk's edge an outer wall may stand, in columns. */
    static final int WALL_INSET = 3;
    /** Least share of a floor's outer wall blocks, doorways aside, that must be closed. */
    static final double WALL_CLOSED_SHARE = 0.9;
    /** Least share of checked items that must pass, per check. */
    static final double FLOORS_PASS = 0.95;
    static final double WALLS_PASS = 0.9;
    static final double STAIRS_PASS = 0.9;
    static final double DOORS_PASS = 0.95;
    /** Failures named per check in the log. */
    static final int EXAMPLES = 8;

    private CityScenarios() {}

    /**
     * Adds the scenarios to a registry.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Real());
        registry.register(new Walk());
        registry.register(new Budget());
        registry.register(new SewingScenario());
        registry.register(new ScatteredScenario());
    }

    private static OverlayWorld requireCities(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(
            overlay != null,
            "expected the overlay in the overworld (world type " + HeightOracles.worldType(context.world())
                + "), found none");
        context.check(
            overlay.cities() != null,
            "expected a world pinned with the layout plan source, found plan " + overlay.snapshot()
                .planSource());
        return overlay;
    }

    /** The city center nearest the world's spawn. */
    static CityFinder.Result centerNearSpawn(ScenarioContext context, OverlayWorld overlay) {
        return center(context, overlay, 0);
    }

    /**
     * The city center nearest the world's spawn whose region lies wholly outside the spawn area the server generates
     * at start, so a fresh world generates it during the scenario and the recorder sees every chunk.
     */
    static CityFinder.Result freshCenter(ScenarioContext context, OverlayWorld overlay) {
        return center(context, overlay, SPAWN_AREA + REGION_RADIUS + 1);
    }

    private static CityFinder.Result center(ScenarioContext context, OverlayWorld overlay, int minChunks) {
        ChunkCoordinates spawn = context.world()
            .getSpawnPoint();
        CityFinder finder = new CityFinder(
            overlay.cities()
                .planner(),
            overlay.plan());
        int spawnX = spawn.posX >> 4;
        int spawnZ = spawn.posZ >> 4;
        CityFinder.Result center = finder.find(
            CityFinder.Kind.CENTER,
            spawn.posX,
            spawn.posZ,
            CENTER_SEARCH,
            found -> Math.max(Math.abs(found.chunkX() - spawnX), Math.abs(found.chunkZ() - spawnZ)) >= minChunks);
        context.check(center != null, "expected a city center within " + CENTER_SEARCH + " chunks of the spawn");
        return center;
    }

    /** The distance in blocks from a point to the center of the nearest city chunk, searched ring by ring. */
    static double nearestCityChunk(LayoutPlanner planner, int blockX, int blockZ) {
        int startX = blockX >> 4;
        int startZ = blockZ >> 4;
        double best = Double.MAX_VALUE;
        for (int ring = 0; ring <= CITY_SEARCH; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring || !planner.plan(startX + dx, startZ + dz)
                        .isCity()) {
                        continue;
                    }
                    double cx = ((startX + dx) << 4) + 8;
                    double cz = ((startZ + dz) << 4) + 8;
                    best = Math.min(best, Math.hypot(cx - blockX, cz - blockZ));
                }
            }
            if (best <= Math.max(0, ring - 1) * 16) {
                return best;
            }
        }
        return best;
    }

    /** Generates the region around the spawn's city on a fresh world and checks it. */
    static final class Real implements Scenario {

        private RegionGenerator generator;
        private CityFinder.Result center;
        private boolean done;

        @Override
        public String name() {
            return "city.real";
        }

        @Override
        public int ticks() {
            return 20 * 60 * 8;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            OverlayWorld overlay = requireCities(context);
            center = freshCenter(context, overlay);
            int inner = REGION_RADIUS - 2;
            overlay.recorder()
                .record(
                    center.chunkX() - inner,
                    center.chunkX() + inner,
                    center.chunkZ() - inner,
                    center.chunkZ() + inner);
            generator = new RegionGenerator(context.world(), center.chunkX(), center.chunkZ(), REGION_RADIUS);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick();
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the region generated, loaded " + generator.loaded() + "/" + generator.size());
            OverlayWorld overlay = requireCities(context);
            ChunkRecorder recorder = overlay.recorder();
            recorder.stop();
            WorldServer world = context.world();
            LayoutPlanner planner = overlay.cities()
                .planner();
            ChunkCoordinates spawn = world.getSpawnPoint();
            double fromSpawn = nearestCityChunk(planner, spawn.posX, spawn.posZ);
            double fromOrigin = nearestCityChunk(planner, 0, 0);
            CityFinder.Result nearest = centerNearSpawn(context, overlay);
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "[LCNH-CITY] spawn=%d,%d,%d nearestCityChunkFromSpawn=%.0f nearestCityChunkFromOrigin=%.0f"
                        + " nearestCenter=%d,%d (%.0f blocks from spawn) hashedCenter=%d,%d (%s)",
                    spawn.posX,
                    spawn.posY,
                    spawn.posZ,
                    fromSpawn,
                    fromOrigin,
                    nearest.x(),
                    nearest.z(),
                    Math.hypot(nearest.x() - spawn.posX, nearest.z() - spawn.posZ),
                    center.x(),
                    center.z(),
                    center.label()));
            int inner = REGION_RADIUS - 2;
            int expected = (2 * inner + 1) * (2 * inner + 1);
            context.check(
                recorder.terrainHashes()
                    .size() == expected,
                "expected " + expected
                    + " freshly generated chunks, recorded "
                    + recorder.terrainHashes()
                        .size()
                    + "; run on a fresh world (--fresh)");
            CityPlan plan = overlay.plan();
            int cityChunks = 0;
            int buildings = 0;
            int streets = 0;
            int pavedStreets = 0;
            int parks = 0;
            int bridges = 0;
            int jobs = 0;
            int addedFoliage = 0;
            java.util.Set<String> buildingNames = new java.util.TreeSet<>();
            for (int cz = center.chunkZ() - inner; cz <= center.chunkZ() + inner; cz++) {
                for (int cx = center.chunkX() - inner; cx <= center.chunkX() + inner; cx++) {
                    com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan chunkPlan = plan.chunk(cx, cz);
                    jobs += chunkPlan.jobs()
                        .size();
                    if ("bridge".equals(chunkPlan.label())) {
                        bridges++;
                    }
                    if (!chunkPlan.isCity()) {
                        continue;
                    }
                    cityChunks++;
                    ChunkPlan layout = planner.plan(cx, cz);
                    Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                    addedFoliage += Math.max(
                        0,
                        foliage(chunk, chunkPlan) - recorder.footprintFoliageAfterTerrain()
                            .getOrDefault(ChunkRecorder.key(cx, cz), 0));
                    if (layout.hasBuilding()) {
                        buildings++;
                        buildingNames.add(layout.building());
                    } else if (layout.streetType() == StreetType.PARK || planner.isElevatedParkSection(cx, cz)) {
                        parks++;
                    } else {
                        streets++;
                        if (paved(chunk, layout.cityGroundLevel())) {
                            pavedStreets++;
                        }
                    }
                }
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "[LCNH-CITY] region: %d city chunks, %d buildings of %d kinds, %d streets (%d paved), %d parks,"
                        + " %d bridges, %d population jobs, decoration logs and leaves in footprints %d, guard dropped"
                        + " %d writes, cascades during population %d",
                    cityChunks,
                    buildings,
                    buildingNames.size(),
                    streets,
                    pavedStreets,
                    parks,
                    bridges,
                    jobs,
                    addedFoliage,
                    FootprintGuard.droppedWrites(),
                    overlay.cascades()));
            LostCities.LOG.info("[LCNH-CITY] buildings: {}", buildingNames);
            LostCities.LOG.info(
                "[LCNH-HASH] worldType={} seed={} chunks={} terrain={} population={}",
                HeightOracles.worldType(world),
                world.getSeed(),
                recorder.terrainHashes()
                    .size(),
                OverlayScenarios.combined(recorder.terrainHashes()),
                OverlayScenarios.combined(recorder.populationHashes()));
            context.check(cityChunks > 0, "expected city chunks around the spawn's city, found none");
            context.check(buildings > 0, "expected buildings in the spawn's city, found none");
            context.check(
                streets == 0 || pavedStreets * 10 >= streets * 9,
                "expected at least 9/10 of street chunks paved at the city ground, found " + pavedStreets
                    + "/"
                    + streets);
            context.check(
                FootprintGuard.leakedFoliage() == 0,
                "expected no log or leaf past the guard during population, found " + FootprintGuard.leakedFoliage());
        }

        /** Whether at least 9 in 10 of a street chunk's inner columns hold a block at the city ground. */
        private static boolean paved(Chunk chunk, int ground) {
            int solid = 0;
            for (int z = 1; z < 15; z++) {
                for (int x = 1; x < 15; x++) {
                    if (!chunk.getBlock(x, ground, z)
                        .isAir(chunk.worldObj, (chunk.xPosition << 4) + x, ground, (chunk.zPosition << 4) + z)) {
                        solid++;
                    }
                }
            }
            return solid * 10 >= 14 * 14 * 9;
        }

        private static int foliage(Chunk chunk, com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan) {
            int found = 0;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int bottom = plan.footprintBottom(x, z);
                    if (bottom == com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.NO_FOOTPRINT) {
                        continue;
                    }
                    for (int y = Math.max(bottom, plan.groundY() + 1); y <= WorldLimits.MAX_Y; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block instanceof BlockLog || block instanceof BlockLeavesBase) {
                            found++;
                        }
                    }
                }
            }
            return found;
        }
    }

    /** Walks the buildings of the spawn's city and of the next fresh city: floors, walls, stairs and doors. */
    static final class Walk implements Scenario {

        private RegionGenerator generator;
        private RegionGenerator second;
        private CityFinder.Result center;
        private CityFinder.Result fresh;
        private boolean done;

        @Override
        public String name() {
            return "city.walk";
        }

        @Override
        public int ticks() {
            return 20 * 60 * 8;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            OverlayWorld overlay = requireCities(context);
            center = centerNearSpawn(context, overlay);
            fresh = freshCenter(context, overlay);
            generator = new RegionGenerator(context.world(), center.chunkX(), center.chunkZ(), REGION_RADIUS);
            second = new RegionGenerator(context.world(), fresh.chunkX(), fresh.chunkZ(), REGION_RADIUS);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick() && second.tick();
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the region generated, loaded " + generator.loaded() + "/" + generator.size());
            OverlayWorld overlay = requireCities(context);
            Walkability walk = new Walkability(context.world(), overlay);
            int inner = generator.innerRadius() - 1;
            for (CityFinder.Result city : new CityFinder.Result[] { center, fresh }) {
                for (int cz = city.chunkZ() - inner; cz <= city.chunkZ() + inner; cz++) {
                    for (int cx = city.chunkX() - inner; cx <= city.chunkX() + inner; cx++) {
                        walk.building(cx, cz);
                    }
                }
            }
            for (String line : walk.report()) {
                LostCities.LOG.info("[LCNH-WALK] {}", line);
            }
            context.check(walk.buildings() > 0, "expected buildings to walk, found none");
            context.check(walk.floors.share() >= FLOORS_PASS, "floors with a floor: " + walk.floors);
            context.check(walk.walls.share() >= WALLS_PASS, "floors with closed walls: " + walk.walls);
            context
                .check(walk.stairs.share() >= STAIRS_PASS, "floor pairs joined by stairs or ladders: " + walk.stairs);
            context.check(walk.doors.share() >= DOORS_PASS, "doors opening onto open space: " + walk.doors);
        }
    }

    /** A pass count with a few failing examples. */
    static final class Tally {

        private final String name;
        private int passed;
        private int total;
        private final StringBuilder examples = new StringBuilder();
        private int named;
        private final int cap;

        Tally(String name) {
            this(name, EXAMPLES);
        }

        Tally(String name, int cap) {
            this.name = name;
            this.cap = cap;
        }

        void add(boolean pass, String where) {
            total++;
            if (pass) {
                passed++;
            } else if (named++ < cap) {
                examples.append(' ')
                    .append(where);
            }
        }

        double share() {
            return total == 0 ? 1.0 : (double) passed / total;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s %d/%d (%.1f%%)%s", name, passed, total, share() * 100, examples);
        }
    }

    /** The walkability checks over one world's generated buildings. */
    static final class Walkability {

        private final WorldServer world;
        private final OverlayWorld overlay;
        private final LayoutPlanner planner;
        final Tally floors = new Tally("floors");
        final Tally walls = new Tally("walls");
        final Tally stairs = new Tally("stairs");
        final Tally doors = new Tally("doors", 40);
        private int buildings;
        private int exempt;

        Walkability(WorldServer world, OverlayWorld overlay) {
            this.world = world;
            this.overlay = overlay;
            this.planner = overlay.cities()
                .planner();
        }

        int buildings() {
            return buildings;
        }

        String[] report() {
            return new String[] { "buildings walked " + buildings + ", exempt (ruined or damaged) " + exempt,
                floors.toString(), walls.toString(), stairs.toString(), doors.toString() };
        }

        void building(int chunkX, int chunkZ) {
            ChunkPlan plan = planner.plan(chunkX, chunkZ);
            if (!plan.hasBuilding()) {
                return;
            }
            if (plan.ruinHeight() >= 0 || overlay.damaged(chunkX, chunkZ)) {
                exempt++;
                return;
            }
            buildings++;
            int bottom = plan.buildingBottomHeight();
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;
            for (int f = -plan.cellars(); f < plan.floors(); f++) {
                int y = bottom + plan.floorPitch() * (f + plan.cellars());
                String where = plan.building() + "@" + (baseX + 8) + "," + y + "," + (baseZ + 8) + "/f" + f;
                floors.add(floorShare(baseX, y, baseZ) >= FLOOR_SOLID_SHARE, where);
                walls.add(wallShare(plan, chunkX, chunkZ, f, y) >= WALL_CLOSED_SHARE, where);
                if (f + 1 < plan.floors()) {
                    stairs.add(hasClimb(plan, chunkX, chunkZ, y), where);
                }
                if (f == 0) {
                    doors(plan, chunkX, chunkZ, y);
                }
            }
        }

        private double floorShare(int baseX, int y, int baseZ) {
            int solid = 0;
            for (int z = 1; z < 15; z++) {
                for (int x = 1; x < 15; x++) {
                    if (!world.isAirBlock(baseX + x, y, baseZ + z)) {
                        solid++;
                    }
                }
            }
            return solid / (14.0 * 14.0);
        }

        /**
         * The share of a floor's outer wall blocks that are closed, doorways and sides inside a multibuilding aside.
         */
        private double wallShare(ChunkPlan plan, int chunkX, int chunkZ, int floor, int y) {
            int closed = 0;
            int counted = 0;
            for (Direction side : Direction.values()) {
                ChunkPlan neighbor = planner.plan(chunkX + side.stepX(), chunkZ + side.stepZ());
                if (plan.multiBuilding() != null && plan.multiBuilding()
                    .equals(neighbor.multiBuilding()) && neighbor.hasBuilding()) {
                    continue;
                }
                boolean doorway = planner.doorway(chunkX, chunkZ, floor, side) != Doorway.NONE;
                for (int along = 0; along < 16; along++) {
                    if (doorway && along >= 7 && along <= 8) {
                        continue;
                    }
                    int x = (chunkX << 4) + (side == Direction.XMIN ? 0 : side == Direction.XMAX ? 15 : along);
                    int z = (chunkZ << 4) + (side == Direction.ZMIN ? 0 : side == Direction.ZMAX ? 15 : along);
                    for (int dy = 1; dy <= 3; dy++) {
                        counted++;
                        // A wall may stand a little inside the chunk's edge (balconies, notched corners): the
                        // position is closed when a block stands within WALL_INSET columns inward.
                        for (int in = 0; in < WALL_INSET; in++) {
                            if (!world.isAirBlock(x - side.stepX() * in, y + dy, z - side.stepZ() * in)) {
                                closed++;
                                break;
                            }
                        }
                    }
                }
            }
            return counted == 0 ? 1.0 : (double) closed / counted;
        }

        /**
         * Whether stairs or a ladder rise from a floor toward the next, anywhere in the building: a multibuilding is
         * one building, whose stairwell stands in one of its chunks (M12: its floors grew with the high-rise core, and
         * counting each chunk alone failed every chunk without the stairwell).
         */
        private boolean hasClimb(ChunkPlan plan, int chunkX, int chunkZ, int y) {
            if (plan.multiPos()
                .isSingle()) {
                return hasClimb(chunkX << 4, y, chunkZ << 4);
            }
            int topX = chunkX - plan.multiPos()
                .x();
            int topZ = chunkZ - plan.multiPos()
                .z();
            for (int dx = 0; dx < plan.multiPos()
                .width(); dx++) {
                for (int dz = 0; dz < plan.multiPos()
                    .height(); dz++) {
                    if (hasClimb((topX + dx) << 4, y, (topZ + dz) << 4)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** Whether stairs or a ladder rise from a floor toward the next within one chunk. */
        private boolean hasClimb(int baseX, int y, int baseZ) {
            for (int dy = 1; dy <= ChunkPlan.LEVEL_HEIGHT; dy++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block block = world.getBlock(baseX + x, y + dy, baseZ + z);
                        if (block instanceof BlockStairs || block instanceof BlockLadder) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /** Every door the layout puts in the ground floor: a door block, with open space in front of it. */
        private void doors(ChunkPlan plan, int chunkX, int chunkZ, int y) {
            for (Direction side : Direction.values()) {
                if (planner.doorway(chunkX, chunkZ, 0, side) != Doorway.DOOR) {
                    continue;
                }
                for (int along = 7; along <= 8; along++) {
                    int lx = side == Direction.XMIN ? 0 : side == Direction.XMAX ? 15 : along;
                    int lz = side == Direction.ZMIN ? 0 : side == Direction.ZMAX ? 15 : along;
                    int x = (chunkX << 4) + lx;
                    int z = (chunkZ << 4) + lz;
                    int ox = x + side.stepX();
                    int oz = z + side.stepZ();
                    boolean door = world.getBlock(x, y + 1, z) instanceof BlockDoor;
                    boolean open = passable(ox, y + 1, oz) && passable(ox, y + 2, oz);
                    String blocker = open ? ""
                        : ":blocked by " + Block.blockRegistry
                            .getNameForObject(world.getBlock(ox, passable(ox, y + 1, oz) ? y + 2 : y + 1, oz));
                    doors.add(
                        door && open,
                        plan.building() + "@" + x + "," + (y + 1) + "," + z + (door ? "" : ":nodoor") + blocker);
                }
            }
        }

        private boolean passable(int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            return block.isAir(world, x, y, z) || block.getCollisionBoundingBoxFromPool(world, x, y, z) == null;
        }
    }

    /** Budgets B1 to B5 with real content (D81's method). */
    static final class Budget implements Scenario {

        private RegionGenerator generator;
        private boolean warmedUp;
        private boolean done;
        /** The plan cache's misses and the height cache's oracle calls when the warm-up ended. */
        private long planMissesAtReset;
        private long heightComputationsAtReset;

        @Override
        public String name() {
            return "city.budget";
        }

        @Override
        public boolean manualOnly() {
            return true;
        }

        @Override
        public int ticks() {
            return 20 * 60 * 10;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            OverlayWorld overlay = requireCities(context);
            CityFinder.Result center = centerNearSpawn(context, overlay);
            int x = center.chunkX() + BUDGET_CITY_OFFSET;
            int z = center.chunkZ() + BUDGET_CITY_OFFSET;
            LostCities.LOG.info(
                "[LCNH-BUDGET] 65 x 65 chunks around ({}, {}), real city at ({}, {})",
                x,
                z,
                center.chunkX(),
                center.chunkZ());
            generator = new RegionGenerator(context.world(), x, z, BUDGET_RADIUS);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick();
            if (!warmedUp && generator.loaded() >= WARM_UP_CHUNKS) {
                OverlayWorld overlay = requireCities(context);
                overlay.timers()
                    .reset();
                overlay.prefetcher()
                    .resetStats();
                planMissesAtReset = overlay.planMisses();
                heightComputationsAtReset = overlay.heightComputations();
                warmedUp = true;
            }
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the region generated, loaded " + generator.loaded() + "/" + generator.size());
            OverlayWorld overlay = requireCities(context);
            GenerationTimers timers = overlay.timers();
            for (String line : timers.report()) {
                LostCities.LOG.info("[LCNH-BUDGET] {}", line);
            }
            LostCities.LOG.info(
                "[LCNH-BUDGET] {}, plan cache {} misses ({} after the warm-up), oracle calls after the warm-up {}",
                overlay.prefetcher()
                    .summary(),
                overlay.planMisses(),
                overlay.planMisses() - planMissesAtReset,
                overlay.heightComputations() - heightComputationsAtReset);
            StringBuilder failed = new StringBuilder();
            for (GenerationTimers.Verdict verdict : timers.budgets(overlay.cacheBytes())) {
                LostCities.LOG.info("[LCNH-BUDGET] {}", verdict);
                if (!verdict.met()) {
                    failed.append(verdict.id())
                        .append(' ');
                }
            }
            LostCities.LOG.info(
                "[LCNH-BUDGET] worldType={} seed={} chunks={} cascades={} dropped={}",
                HeightOracles.worldType(context.world()),
                context.world()
                    .getSeed(),
                generator.size(),
                overlay.cascades(),
                FootprintGuard.droppedWrites());
            context.check(
                failed.length() == 0,
                "budgets exceeded: " + failed.toString()
                    .trim());
        }
    }

    /** Returns the real plan source of a world, or {@code null}. */
    static LayoutCityPlan cities(OverlayWorld overlay) {
        return overlay == null ? null : overlay.cities();
    }
}
