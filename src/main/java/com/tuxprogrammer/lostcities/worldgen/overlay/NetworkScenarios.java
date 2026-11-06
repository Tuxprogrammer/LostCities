package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockWall;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.Networks;
import com.tuxprogrammer.lostcities.layout.highway.Axis;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.highway.HighwayPlanner;
import com.tuxprogrammer.lostcities.layout.highway.HubKey;
import com.tuxprogrammer.lostcities.layout.highway.Route;
import com.tuxprogrammer.lostcities.layout.highway.Run;
import com.tuxprogrammer.lostcities.layout.highway.Segment;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.city.NetworkWriter;

/**
 * The highway and railway scenarios (M06; docs/testing/README.md), for a world pinned with the layout plan source:
 * <ul>
 * <li>{@code network.highway}: takes the shortest highway run near the spawn that joins two hubs, generates the
 * chunks along it, and walks its deck block by block from one hub's city to the other's: every step must be at most
 * half a block up or down, with two blocks of headroom, and both ends must meet their city's ground (alpha review
 * AR-10: a highway ends deliberately, never cut);</li>
 * <li>{@code network.railway}: takes the railway station nearest the spawn on an east-west line with a three-way
 * split, generates the line east from it to the next station spot, and checks that every track leaving one chunk
 * enters the next at the same row, at most a block higher or lower: a minecart can ride it.</li>
 * </ul>
 * Both are manual-only: they generate several hundred chunks. Prints {@code [LCNH-NETWORK]} lines.
 *
 * <p>
 * Side: server.
 */
public final class NetworkScenarios {

    /** The tag of the scenarios' log lines. */
    static final String TAG = "[LCNH-NETWORK] ";
    /** Planning cells around the spawn's cell whose routes the highway scenario considers. */
    static final int ROUTE_CELLS = 1;
    /** Longest run, in chunks, the highway scenario walks. */
    static final int MAX_RUN = 220;
    /** Chunks either side of a path that are generated, so the path's chunks are populated. */
    static final int CORRIDOR = 2;
    /** Highest step a walk accepts, in blocks: a half slab. */
    static final double MAX_STEP = 0.5;
    /** Most chunks from the spawn the railway scenario looks for a station. */
    static final int STATION_SEARCH = 96;
    /** The part-local row a walk follows across a highway piece: in the outer lane. */
    static final int LANE_ROW = 3;

    private NetworkScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry the registry
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new HighwayWalk());
        registry.register(new RailwayRide());
    }

    private static OverlayWorld requireNetworks(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(overlay != null && overlay.cities() != null, "expected the overlay with real cities");
        context.check(
            overlay.cities()
                .planner()
                .networks() != null,
            "expected the layout's networks");
        return overlay;
    }

    /** Loads a list of chunks, then populates the chunks of the path, a tick's budget at a time. */
    static final class PathGenerator {

        private final WorldServer world;
        private final List<int[]> load = new ArrayList<>();
        private final List<int[]> populate = new ArrayList<>();
        private int loaded;
        private int populated;

        PathGenerator(WorldServer world, Set<Long> path) {
            this.world = world;
            Set<Long> all = new LinkedHashSet<>();
            for (long key : path) {
                int x = (int) (key >> 32);
                int z = (int) key;
                for (int dz = -CORRIDOR; dz <= CORRIDOR; dz++) {
                    for (int dx = -CORRIDOR; dx <= CORRIDOR; dx++) {
                        all.add(key(x + dx, z + dz));
                    }
                }
                populate.add(new int[] { x, z });
            }
            for (long key : all) {
                load.add(new int[] { (int) (key >> 32), (int) key });
            }
        }

        int size() {
            return load.size();
        }

        boolean tick() {
            ChunkProviderServer server = world.theChunkProviderServer;
            long start = System.nanoTime();
            while (loaded < load.size() && System.nanoTime() - start < RegionGenerator.TICK_BUDGET_NANOS) {
                int[] c = load.get(loaded++);
                server.loadChunk(c[0], c[1]);
            }
            if (loaded < load.size()) {
                return false;
            }
            while (populated < populate.size() && System.nanoTime() - start < RegionGenerator.TICK_BUDGET_NANOS) {
                int[] c = populate.get(populated++);
                Chunk chunk = server.provideChunk(c[0], c[1]);
                if (!chunk.isTerrainPopulated) {
                    server.populate(server, c[0], c[1]);
                }
            }
            return populated >= populate.size();
        }
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** Walks a highway's deck from one hub's city to the other's. */
    static final class HighwayWalk implements Scenario {

        private Route route;
        private Run run;
        private LinkScenarios.HighwayLeg leg;
        private PathGenerator generator;
        private boolean done;

        @Override
        public String name() {
            return "network.highway";
        }

        @Override
        public int ticks() {
            return 20 * 60 * 10;
        }

        @Override
        public boolean manualOnly() {
            return true;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            OverlayWorld overlay = requireNetworks(context);
            Networks links = overlay.cities()
                .planner()
                .networks();
            if (links.links() != null) {
                leg = LinkScenarios.HighwayLeg.find(context, links);
                context.check(leg != null, "expected a written highway link near the spawn, found none");
                generator = new PathGenerator(context.world(), leg.chunks());
                return;
            }
            HighwayPlanner planner = overlay.cities()
                .planner()
                .networks()
                .highways();
            ChunkCoordinates spawn = context.world()
                .getSpawnPoint();
            HubKey home = planner.cell(spawn.posX >> 4, spawn.posZ >> 4);
            for (int dz = -ROUTE_CELLS; dz <= ROUTE_CELLS; dz++) {
                for (int dx = -ROUTE_CELLS; dx <= ROUTE_CELLS; dx++) {
                    for (Route candidate : planner.ownedRoutes(home.offset(dx, dz))) {
                        for (Run r : candidate.runs()) {
                            boolean hubs = r.start()
                                .kind() == Run.EndKind.HUB
                                && r.end()
                                    .kind() == Run.EndKind.HUB;
                            if (hubs && r.length() <= MAX_RUN && (run == null || r.length() < run.length())) {
                                run = r;
                                route = candidate;
                            }
                        }
                    }
                }
            }
            context.check(run != null, "expected a hub-to-hub highway run near the spawn, found none");
            Set<Long> path = new LinkedHashSet<>();
            for (int[] c : chunks(run)) {
                path.add(key(c[0], c[1]));
            }
            generator = new PathGenerator(context.world(), path);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick();
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the highway's chunks generated");
            OverlayWorld overlay = requireNetworks(context);
            if (leg != null) {
                leg.verify(context, overlay);
                return;
            }
            LayoutPlanner planner = overlay.cities()
                .planner();
            Networks networks = planner.networks();
            WorldServer world = context.world();
            Walk walk = new Walk(world);
            int[] pieces = new int[HighwayInfo.Shape.values().length];
            int tunnels = 0;
            int previousX = Integer.MIN_VALUE;
            int previousZ = Integer.MIN_VALUE;
            for (int[] block : lane(run)) {
                int chunkX = block[0] >> 4;
                int chunkZ = block[1] >> 4;
                if (chunkX != previousX || chunkZ != previousZ) {
                    previousX = chunkX;
                    previousZ = chunkZ;
                    for (HighwayInfo.Piece piece : networks.highway(chunkX, chunkZ)
                        .pieces(chunkX, chunkZ)) {
                        pieces[piece.shape()
                            .ordinal()]++;
                    }
                    HighwayInfo here = networks.highway(chunkX, chunkZ);
                    ChunkPlan chunkPlan = planner.plan(chunkX, chunkZ);
                    walk.where = " in chunk " + chunkX
                        + ","
                        + chunkZ
                        + " "
                        + here.pieces(chunkX, chunkZ)
                        + (chunkPlan.isCity() ? " city level " + chunkPlan.cityLevel() : " no city")
                        + " natural "
                        + overlay.plan()
                            .chunk(chunkX, chunkZ)
                            .naturalHeight();
                    if (planner.plan(chunkX, chunkZ)
                        .isTunnel(
                            Math.max(here.xLevel(), here.zLevel()),
                            overlay.plan()
                                .chunk(chunkX, chunkZ)
                                .naturalHeight())) {
                        tunnels++;
                    }
                }
                int deck = expectedDeck(planner, networks, block[0], block[1]);
                // As city.walk exempts damaged buildings: an explosion's crater in the deck is upstream's damage.
                walk.exempt = overlay.damaged(chunkX, chunkZ);
                walk.step(block[0], block[1], deck);
            }
            boolean startMeets = meetsCity(planner, run.start());
            boolean endMeets = meetsCity(planner, run.end());
            StringBuilder counts = new StringBuilder();
            for (HighwayInfo.Shape shape : HighwayInfo.Shape.values()) {
                counts.append(' ')
                    .append(
                        shape.name()
                            .toLowerCase(Locale.ROOT))
                    .append('=')
                    .append(pieces[shape.ordinal()]);
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "%sroute %s level %d (shared %d) run %s to %s, %d chunks, %d steps walked (%d in chunks an"
                        + " explosion reaches), %d too high, %d without ground, %d without headroom, tunnel chunks %d,"
                        + " start meets city %s, end meets city %s; pieces%s",
                    TAG,
                    route.key(),
                    route.level(),
                    networks.highways()
                        .sharedLevel(route),
                    run.start(),
                    run.end(),
                    run.length() + 1,
                    walk.steps,
                    walk.exempted,
                    walk.tooHigh,
                    walk.gaps,
                    walk.blocked,
                    tunnels,
                    startMeets,
                    endMeets,
                    counts));
            for (String example : walk.examples) {
                LostCities.LOG.info("{}  {}", TAG, example);
            }
            context.check(walk.steps > 0, "expected steps on the deck");
            context.check(walk.gaps == 0, "expected ground under every step, found " + walk.gaps + " gaps");
            context.check(walk.tooHigh == 0, "expected no step above half a block, found " + walk.tooHigh);
            context.check(walk.blocked == 0, "expected headroom over every step, found " + walk.blocked + " blocked");
            context.check(startMeets && endMeets, "expected both ends at their city's ground");
        }

        /** Whether a hub end's chunk is city at the level the highway arrives at. */
        private static boolean meetsCity(LayoutPlanner planner, Run.End end) {
            ChunkPlan plan = planner.plan(end.chunkX(), end.chunkZ());
            return plan.isCity() && plan.cityLevel() == end.level();
        }

        /**
         * The deck's block Y under a block of the walk: the lowest piece's deck, raised on a ramp by the column's
         * whole steps, as the writer lays it.
         */
        private static int expectedDeck(LayoutPlanner planner, Networks networks, int blockX, int blockZ) {
            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            int best = Integer.MAX_VALUE;
            for (HighwayInfo.Piece piece : networks.highway(chunkX, chunkZ)
                .pieces(chunkX, chunkZ)) {
                int y = planner.profile().groundLevel + piece.level() * ChunkPlan.LEVEL_HEIGHT;
                if (piece.shape() == HighwayInfo.Shape.RAMP) {
                    int along = piece.axis() == Axis.X ? blockX & 15 : blockZ & 15;
                    y += NetworkWriter.rampSteps(piece)[along] / 2;
                }
                best = Math.min(best, y);
            }
            return best == Integer.MAX_VALUE ? planner.profile().groundLevel : best;
        }
    }

    /** The chunks of a run, in order from its start. */
    static List<int[]> chunks(Run run) {
        List<int[]> out = new ArrayList<>();
        for (Segment segment : run.segments()) {
            int stepX = segment.axis() == Axis.X ? segment.direction() : 0;
            int stepZ = segment.axis() == Axis.Z ? segment.direction() : 0;
            for (int i = out.isEmpty() ? 0 : 1; i <= segment.length(); i++) {
                out.add(new int[] { segment.startX() + stepX * i, segment.startZ() + stepZ * i });
            }
        }
        return out;
    }

    /**
     * The blocks of a walk along a run: in an X segment along part row {@link #LANE_ROW} (block z = chunk × 16 + 3),
     * in a Z segment along its turned image (block x = chunk × 16 + 12), joined at corners by a straight walk across
     * the corner chunk.
     */
    static List<int[]> lane(Run run) {
        List<int[]> out = new ArrayList<>();
        int[] last = null;
        for (Segment segment : run.segments()) {
            boolean alongX = segment.axis() == Axis.X;
            int fixed = alongX ? (segment.startZ() << 4) + LANE_ROW : (segment.startX() << 4) + 15 - LANE_ROW;
            int from = alongX ? (segment.startX() << 4) + 8 : (segment.startZ() << 4) + 8;
            int to = alongX ? (segment.endX() << 4) + 8 : (segment.endZ() << 4) + 8;
            int step = Integer.signum(to - from);
            int[] first = alongX ? new int[] { from, fixed } : new int[] { fixed, from };
            if (last != null) {
                // Across the corner chunk, X first, then Z.
                int x = last[0];
                int z = last[1];
                while (x != first[0]) {
                    x += Integer.signum(first[0] - x);
                    out.add(new int[] { x, z });
                }
                while (z != first[1]) {
                    z += Integer.signum(first[1] - z);
                    out.add(new int[] { x, z });
                }
            }
            for (int a = from;; a += step) {
                int[] block = alongX ? new int[] { a, fixed } : new int[] { fixed, a };
                if (last == null || block[0] != last[0] || block[1] != last[1]) {
                    out.add(block);
                }
                last = block;
                if (a == to || step == 0) {
                    break;
                }
            }
        }
        return out;
    }

    /** A walk along a deck: each step's standing height, compared with the step before. */
    static final class Walk {

        private final WorldServer world;
        int steps;
        int tooHigh;
        int gaps;
        int blocked;
        int exempted;
        /** Whether the current step lies where an explosion reaches: counted, not judged. */
        boolean exempt;
        /** The current chunk's pieces and plan, appended to a failure's note. */
        String where = "";
        final List<String> examples = new ArrayList<>();
        private double previous = Double.NaN;

        Walk(WorldServer world) {
            this.world = world;
        }

        void step(int x, int z, int deck) {
            steps++;
            if (exempt) {
                exempted++;
                previous = Double.NaN;
                return;
            }
            // The lowest surface from just under the deck up: a floor with no full block on it. Searching from above
            // would land on a lamp's arm over the road.
            double standing = Double.NaN;
            for (int y = deck - 1; y <= deck + 2; y++) {
                double top = top(x, y, z);
                if (Double.isNaN(top)) {
                    continue;
                }
                double above = top(x, y + 1, z);
                if (Double.isNaN(above)) {
                    standing = top;
                    break;
                }
                if (above - (y + 1) < 1.0) {
                    // A half slab on the floor: the step stands on the slab.
                    standing = above;
                    break;
                }
            }
            if (Double.isNaN(standing)) {
                gaps++;
                note("no ground at " + x + "," + z + " near Y " + deck + column(x, z, deck));
                previous = Double.NaN;
                return;
            }
            int head = (int) Math.ceil(standing);
            if (solid(x, head, z) || solid(x, head + 1, z)) {
                blocked++;
                note("no headroom at " + x + "," + head + "," + z);
            }
            if (!Double.isNaN(previous) && Math.abs(standing - previous) > MAX_STEP + 1e-6) {
                tooHigh++;
                note(
                    String.format(Locale.ROOT, "step %.1f -> %.1f at %d,%d", previous, standing, x, z)
                        + column(x, z, deck));
            }
            previous = standing;
        }

        /**
         * The blocks of a column from two under the expected deck to two over it, for a failure's note, so a real-pack
         * run shows what stood there without a second run.
         */
        private String column(int x, int z, int deck) {
            StringBuilder out = new StringBuilder(" [deck ").append(deck)
                .append(':');
            for (int y = deck - 2; y <= deck + 2; y++) {
                Block block = world.getBlock(x, y, z);
                out.append(' ')
                    .append(y)
                    .append('=')
                    .append(Block.blockRegistry.getNameForObject(block))
                    .append(':')
                    .append(world.getBlockMetadata(x, y, z));
            }
            return out.append(']')
                .toString();
        }

        private void note(String example) {
            if (examples.size() < 8) {
                examples.add(example + where);
            }
        }

        /** The height a player stands at on the block at Y, or NaN when it carries none (air, liquid, a rail). */
        private double top(int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            if (!solidFloor(block, x, y, z)) {
                return Double.NaN;
            }
            if (block instanceof BlockSlab && !block.isOpaqueCube() && (world.getBlockMetadata(x, y, z) & 8) == 0) {
                return y + 0.5;
            }
            AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
            return standingTop(y, box == null ? y + 1.0 : box.maxY);
        }

        /**
         * The standing height on a block at Y whose collision box reaches {@code collisionTop}: its box's top, never
         * above the block's own top (a fence's tall box is not something the deck walk stands on). A thin snow layer,
         * whose box has no height, stands at its floor's level, as a player does: on a cold biome's bridge the
         * weather's snow is not a step (KI-5, 0013 section 2 "Verification").
         */
        static double standingTop(int y, double collisionTop) {
            return Math.max(y, Math.min(y + 1.0, collisionTop));
        }

        private boolean solidFloor(Block block, int x, int y, int z) {
            if (block.isAir(world, x, y, z) || block.getMaterial()
                .isLiquid() || block instanceof BlockRailBase) {
                return false;
            }
            AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
            return box != null;
        }

        private boolean solid(int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            if (block instanceof BlockWall || block instanceof BlockFence) {
                return true;
            }
            // A slab or a thin snow layer leaves headroom: what blocks a player is a box over half a block tall.
            return solidFloor(block, x, y, z) && !(block instanceof BlockSlab) && top(x, y, z) - y > 0.5;
        }
    }

    /** Follows a railway line east from a station through a three-way split. */
    static final class RailwayRide implements Scenario {

        private int stationX;
        private int stationZ;
        private LinkScenarios.RailLeg leg;
        private int length;
        private PathGenerator generator;
        private boolean done;

        @Override
        public String name() {
            return "network.railway";
        }

        @Override
        public int ticks() {
            return 20 * 60 * 6;
        }

        @Override
        public boolean manualOnly() {
            return true;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            OverlayWorld overlay = requireNetworks(context);
            Networks networks = overlay.cities()
                .planner()
                .networks();
            if (networks.links() != null) {
                leg = LinkScenarios.RailLeg.find(context, networks);
                context.check(leg != null, "expected a railway line near the spawn, found none");
                generator = new PathGenerator(context.world(), leg.chunks());
                return;
            }
            int sx = networks.railways()
                .settings()
                .spacingEastWest();
            int sz = networks.railways()
                .settings()
                .spacingNorthSouth();
            ChunkCoordinates spawn = context.world()
                .getSpawnPoint();
            int spawnX = spawn.posX >> 4;
            int spawnZ = spawn.posZ >> 4;
            boolean found = false;
            for (int ring = 0; ring <= STATION_SEARCH && !found; ring++) {
                for (int dz = -ring; dz <= ring && !found; dz++) {
                    for (int dx = -ring; dx <= ring && !found; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                            continue;
                        }
                        int x = spawnX + dx;
                        int z = spawnZ + dz;
                        // Station spots on the rows with splits: grid row sz, columns 0 and sx.
                        if (Math.floorMod(z + 1, 2 * sz) != sz || Math.floorMod(x + 1, sx) != 0) {
                            continue;
                        }
                        if (networks.railChunk(x, z)
                            .type()
                            .isStation()) {
                            stationX = x;
                            stationZ = z;
                            found = true;
                        }
                    }
                }
            }
            context.check(found, "expected a station on a line with a split within " + STATION_SEARCH + " chunks");
            length = sx + 1;
            Set<Long> path = new LinkedHashSet<>();
            for (int i = 0; i < length; i++) {
                path.add(key(stationX + i, stationZ));
            }
            generator = new PathGenerator(context.world(), path);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick();
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the line's chunks generated");
            OverlayWorld overlay = requireNetworks(context);
            Networks networks = overlay.cities()
                .planner()
                .networks();
            if (leg != null) {
                leg.verify(context, overlay);
                return;
            }
            WorldServer world = context.world();
            int groundLevel = overlay.cities()
                .planner()
                .profile().groundLevel;
            int boundaries = 0;
            int broken = 0;
            int rails = 0;
            boolean split = false;
            StringBuilder types = new StringBuilder();
            List<String> examples = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                RailChunk rail = networks.railChunk(stationX + i, stationZ);
                types.append(i == 0 ? "" : ",")
                    .append(rail.type());
                split |= rail.type() == RailChunkType.THREE_SPLIT;
                if (i == length - 1) {
                    break;
                }
                int edge = ((stationX + i) << 4) + 15;
                int[] band = band(
                    groundLevel,
                    rail.level(),
                    networks.railChunk(stationX + i + 1, stationZ)
                        .level());
                List<int[]> out = railsAt(world, edge, stationZ, band);
                List<int[]> in = railsAt(world, edge + 1, stationZ, band);
                boundaries++;
                rails += out.size();
                boolean ok = !out.isEmpty() && matches(out, in) && matches(in, out);
                if (!ok) {
                    broken++;
                    if (examples.size() < 6) {
                        examples.add("x " + edge + "|" + (edge + 1) + ": " + describe(out) + " | " + describe(in));
                    }
                }
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "%sline from station %d,%d east, %d chunks [%s]: %d boundaries, %d broken, %d rails crossing,"
                        + " split %s",
                    TAG,
                    stationX,
                    stationZ,
                    length,
                    types,
                    boundaries,
                    broken,
                    rails,
                    split));
            for (String example : examples) {
                LostCities.LOG.info("{}  {}", TAG, example);
            }
            context.check(split, "expected the line to pass a three-way split");
            context.check(broken == 0, "expected every track to continue into the next chunk, " + broken + " broken");
        }

        /**
         * The Y band a boundary's rails are looked for in: from the lower chunk's line level up through the higher
         * one's, one city level ({@link ChunkPlan#LEVEL_HEIGHT}) each, as the writer lays a rail part at
         * {@code cityGround(level)}. A city's own rails a level or more above the line, such as a building's minecart
         * track, are not the line's (KI-5, 0013 section 5 "Verification").
         */
        static int[] band(int groundLevel, int levelA, int levelB) {
            int low = groundLevel + Math.min(levelA, levelB) * ChunkPlan.LEVEL_HEIGHT;
            int high = groundLevel + Math.max(levelA, levelB) * ChunkPlan.LEVEL_HEIGHT + ChunkPlan.LEVEL_HEIGHT - 1;
            return new int[] { Math.max(1, low), Math.min(255, high) };
        }

        /** The rails on one block column of a chunk row inside a Y band: {z, y} pairs. */
        private static List<int[]> railsAt(WorldServer world, int x, int chunkZ, int[] band) {
            List<int[]> out = new ArrayList<>();
            for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                for (int y = band[0]; y <= band[1]; y++) {
                    if (world.getBlock(x, y, z) instanceof BlockRailBase) {
                        out.add(new int[] { z, y });
                    }
                }
            }
            return out;
        }

        /** Whether every rail of one side has one on the other side at its Z, at most a block higher or lower. */
        static boolean matches(List<int[]> from, List<int[]> to) {
            for (int[] a : from) {
                boolean found = false;
                for (int[] b : to) {
                    if (a[0] == b[0] && Math.abs(a[1] - b[1]) <= 1) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        static String describe(List<int[]> rails) {
            StringBuilder out = new StringBuilder();
            for (int[] r : rails) {
                out.append(out.length() == 0 ? "" : " ")
                    .append(r[0])
                    .append('@')
                    .append(r[1]);
            }
            return out.length() == 0 ? "none" : out.toString();
        }
    }
}
