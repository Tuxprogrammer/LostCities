package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.scenario.RegionJob;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The {@code oracle.ae2} scenario (manual only; docs/testing/README.md): compares the meteorite oracle with what AE2
 * actually drops, on a world of the real pack with the city, and checks that no city block stands where AE2's crater
 * or decay can reach it.
 *
 * <ol>
 * <li><b>Scan.</b> Asks a fresh oracle for every cell within {@value #SCAN_CELLS} cells of the spawn's cell, and
 * checks that the scan loaded no chunk. The world's own layout must know each meteorite: its impact chunk is a core
 * (of it, or of another landmark there). For every city chunk within the crater pass's reach of a target below, the
 * plan must keep a street's headroom and every roof under the predicted paraboloid, and no building in the decay
 * square.</li>
 * <li><b>Generate.</b> Up to {@value #NEAR_CITY} predicted meteorites with a city chunk within
 * {@value #CITY_NEAR_CHUNKS} chunks, and the {@value #NEAREST} nearest others: their chunk and
 * {@value #RADIUS} around it populated. Then {@value #SETTLE_TICKS} ticks for AE2's tick callables.</li>
 * <li><b>Compare</b> with AE2's own spawn data: every meteorite AE2 recorded near a target must stand at a predicted
 * site, with exactly the predicted size; its crater's lowest point ({@code y − size + 1}, plus 2 on sand, ice or snow)
 * must not be below the predicted paraboloid; a sky-stone or sky-chest block must stand at its center; and every city
 * chunk within 200 blocks must keep its street headroom and roofs under the actual paraboloid, with no building in
 * the actual decay square. At least one target must have fallen.</li>
 * </ol>
 *
 * <p>
 * Side: server.
 */
public final class Ae2Scenario implements Scenario {

    /** Cells scanned around the spawn's cell, each way. */
    static final int SCAN_CELLS = 6;
    /** Meteorites near a city to generate. */
    static final int NEAR_CITY = 3;
    /** Other nearest meteorites to generate. */
    static final int NEAREST = 2;
    /** How close a city chunk must be for a meteorite to count as near a city, chunks. */
    static final int CITY_NEAR_CHUNKS = 8;
    /** Populated radius around each meteor chunk, chunks. */
    static final int RADIUS = 1;
    /** Ticks AE2 gets after the last job to place its meteorites. */
    static final int SETTLE_TICKS = 100;
    /** How far the city checks reach from an impact, chunks (the crater pass's 200 blocks). */
    static final int CAP_REACH_CHUNKS = HeightCap.AE2_REACH / 16 + 1;
    /** Generation time per tick. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;
    /** Sky-count threshold above which AE2 digs a crater. */
    static final int CRATER_SKY = 10;
    /** Sky-count threshold above which AE2 decays the ground around the meteor. */
    static final int DECAY_SKY = 3;
    /** Crater adjustment on sand, ice and snow ({@code FalloutSand}, {@code FalloutSnow}). */
    static final int SOFT_ADJUST = 2;

    private MeteoriteOracle oracle;
    private final List<Landmark> targets = new ArrayList<>();
    private final List<RegionJob> jobs = new ArrayList<>();
    private int jobIndex;
    private int settled;
    private boolean done;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Ae2Scenario());
    }

    @Override
    public String name() {
        return "oracle.ae2";
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
        targets.clear();
        jobs.clear();
        jobIndex = 0;
        settled = 0;
        done = false;
        WorldServer world = context.world();
        context.check(Ae2Oracles.isLoaded(), "expected AE2 loaded, found it absent");
        MeteoriteSettings settings = Ae2Oracles.settings(world);
        context.check(settings.enabled(), "expected AE2 meteorites in the overworld, found " + settings);
        LayoutPlanner planner = planner(context);

        int chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
        StructureOracle built = Ae2Oracles.forWorld(world, HeightOracles.create(world), 2);
        context.check(built instanceof MeteoriteOracle, "expected a meteorite oracle, found " + built);
        oracle = (MeteoriteOracle) built;
        ChunkCoordinates spawn = world.getSpawnPoint();
        MeteoriteGrid grid = oracle.grid();
        int spawnCellX = grid.cellOf(spawn.posX);
        int spawnCellZ = grid.cellOf(spawn.posZ);
        List<Landmark> all = new ArrayList<>();
        for (int cellX = spawnCellX - SCAN_CELLS; cellX <= spawnCellX + SCAN_CELLS; cellX++) {
            for (int cellZ = spawnCellZ - SCAN_CELLS; cellZ <= spawnCellZ + SCAN_CELLS; cellZ++) {
                Landmark landmark = oracle.meteoriteAt(cellX, cellZ);
                if (landmark != null) {
                    all.add(landmark);
                }
            }
        }
        context.check(
            world.theChunkProviderServer.getLoadedChunkCount() == chunksBefore,
            "expected the oracle to load no chunk, loaded chunks went from " + chunksBefore
                + " to "
                + world.theChunkProviderServer.getLoadedChunkCount());

        List<Landmark> nearCity = new ArrayList<>();
        int planned = 0;
        for (Landmark landmark : all) {
            HeightCap cap = landmark.heightCap();
            int chunkX = cap.centerX() >> 4;
            int chunkZ = cap.centerZ() >> 4;
            ZoneInfo impact = planner.zone(chunkX, chunkZ);
            // D-landmark-policy-1: a meteorite is not a landmark; the city builds over its impact.
            context.check(
                !landmark.id()
                    .equals(impact.landmarkId()),
                "expected no zone for " + landmark.id() + "'s impact chunk, found " + impact);
            if (cityWithin(planner, chunkX, chunkZ, CITY_NEAR_CHUNKS)) {
                nearCity.add(landmark);
            }
        }
        ChunkCoordinates from = spawn;
        Comparator<Landmark> nearest = Comparator.comparingLong(l -> distanceSquared(l, from));
        nearCity.sort(nearest);
        all.sort(nearest);
        for (int i = 0; i < Math.min(NEAR_CITY, nearCity.size()); i++) {
            targets.add(nearCity.get(i));
        }
        for (Landmark landmark : all) {
            if (targets.size() >= NEAR_CITY + NEAREST || targets.size() >= all.size()) {
                break;
            }
            if (!targets.contains(landmark)) {
                targets.add(landmark);
            }
        }
        for (Landmark target : targets) {
            HeightCap cap = target.heightCap();
            planned += checkActual(
                context,
                planner,
                target,
                cap.centerX(),
                cap.centerZ(),
                cap.centerY(),
                CraterBasin.DECAY_HALF_WIDTH);
        }
        LostCities.LOG.info(
            "[LCNH-AE2] scan {} cells {}..{} x {}..{}: {} meteorites, {} near a city, {} city chunks around the targets"
                + " checked under the predicted crater; targets {}",
            HeightOracles.worldType(world),
            spawnCellX - SCAN_CELLS,
            spawnCellX + SCAN_CELLS,
            spawnCellZ - SCAN_CELLS,
            spawnCellZ + SCAN_CELLS,
            all.size(),
            nearCity.size(),
            planned,
            targets);
        for (Landmark target : targets) {
            jobs.add(
                RegionJob.around(
                    target.heightCap()
                        .centerX() >> 4,
                    target.heightCap()
                        .centerZ() >> 4,
                    RADIUS));
        }
    }

    @Override
    public void tick(ScenarioContext context) {
        long start = System.nanoTime();
        while (jobIndex < jobs.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            if (jobs.get(jobIndex)
                .step(context.world().theChunkProviderServer, start, TICK_BUDGET_NANOS)) {
                jobIndex++;
            }
        }
        if (jobIndex >= jobs.size()) {
            settled++;
        }
        done = settled > SETTLE_TICKS;
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected every region generated, finished " + jobIndex + "/" + jobs.size());
        WorldServer world = context.world();
        LayoutPlanner planner = planner(context);
        int fallen = 0;
        int craters = 0;
        int cityChunks = 0;
        for (Landmark target : targets) {
            HeightCap predicted = target.heightCap();
            int chunkX = predicted.centerX() >> 4;
            int chunkZ = predicted.centerZ() >> 4;
            for (Fallen actual : SpawnData.near(world.provider.dimensionId, chunkX, chunkZ)) {
                MeteoriteGrid.Site site = oracle.grid()
                    .site(
                        oracle.grid()
                            .cellOf(actual.x),
                        oracle.grid()
                            .cellOf(actual.z));
                context.check(
                    site.x() == actual.x && site.z() == actual.z,
                    "expected every meteorite AE2 recorded at a predicted site, found " + actual
                        + " where "
                        + site
                        + " was predicted");
                if (actual.x != predicted.centerX() || actual.z != predicted.centerZ()) {
                    continue;
                }
                fallen++;
                MeteoriteShape shape = MeteoriteShape.of(site.seed());
                context.check(
                    shape.size() == actual.size,
                    "expected size " + shape.size() + " for " + target.id() + ", AE2 recorded " + actual);
                Block center = world.getBlock(actual.x, actual.y, actual.z);
                GameRegistry.UniqueIdentifier name = GameRegistry.findUniqueIdentifierFor(center);
                context.check(
                    name != null && Ae2Oracles.MOD_ID.equals(name.modId),
                    "expected AE2's sky stone or sky chest at the center of " + actual + ", found " + name);
                int adjust = actual.soft ? SOFT_ADJUST : 0;
                double floor = actual.y - actual.size + 1 + adjust;
                LostCities.LOG.info(
                    "[LCNH-AE2] {}: fell at y {} (start {}), size {}, sky {}, crater floor {} against predicted {}",
                    target.id(),
                    actual.y,
                    site.startY(),
                    String.format("%.2f", actual.size),
                    actual.skyMode,
                    String.format("%.2f", floor),
                    predicted.centerY());
                if (actual.skyMode > CRATER_SKY) {
                    craters++;
                    context.check(
                        floor >= predicted.centerY(),
                        "expected the crater of " + target
                            .id() + " no lower than Y " + predicted.centerY() + ", AE2 dug it from " + floor);
                }
                int decay = actual.skyMode > DECAY_SKY ? CraterBasin.DECAY_HALF_WIDTH : Integer.MIN_VALUE;
                int floorY = actual.skyMode > CRATER_SKY ? (int) Math.floor(floor) : Integer.MAX_VALUE / 2;
                cityChunks += checkActual(context, planner, target, actual.x, actual.z, floorY, decay);
            }
        }
        LostCities.LOG.info(
            "[LCNH-AE2] {} of {} targets fell, {} with a crater; {} city chunks checked against the actual craters",
            fallen,
            targets.size(),
            craters,
            cityChunks);
        context.check(fallen > 0, "expected at least one target meteorite to fall, none of " + targets + " did");
    }

    /**
     * Checks every city chunk within the crater pass's reach: street headroom and roofs under the paraboloid of
     * lowest Y {@code floorY} (none when it is huge), and no building in the decay square of half-width
     * {@code decay} (none when negative).
     *
     * @return the city chunks checked
     */
    private static int checkActual(ScenarioContext context, LayoutPlanner planner, Landmark landmark, int x, int z,
        int floorY, int decay) {
        HeightCap cap = HeightCap.ae2(x, z, floorY);
        int checked = 0;
        for (int chunkX = (x >> 4) - CAP_REACH_CHUNKS; chunkX <= (x >> 4) + CAP_REACH_CHUNKS; chunkX++) {
            for (int chunkZ = (z >> 4) - CAP_REACH_CHUNKS; chunkZ <= (z >> 4) + CAP_REACH_CHUNKS; chunkZ++) {
                ChunkPlan plan = planner.plan(chunkX, chunkZ);
                if (!plan.isCity()) {
                    continue;
                }
                checked++;
                String where = landmark.id() + ": city chunk " + chunkX + "," + chunkZ + " " + plan.zone();
                int lowest = cap.lowestCapOver(chunkX, chunkZ);
                context.check(
                    plan.cityGroundLevel() + com.tuxprogrammer.lostcities.layout.landmark.HeightCap.CITY_HEADROOM - 1
                        <= lowest,
                    "expected a street's headroom under the crater at " + where
                        + ", ground "
                        + plan.cityGroundLevel()
                        + " cap "
                        + lowest);
                if (plan.hasBuilding()) {
                    int top = plan.cityGroundLevel() + plan.floors() * plan.floorPitch() + ChunkPlan.LEVEL_HEIGHT - 1;
                    context.check(
                        top <= lowest,
                        "expected the roof under the crater at " + where + ", top " + top + " cap " + lowest);
                    context.check(
                        decay < 0 || !overlaps(chunkX, chunkZ, x, z, decay),
                        "expected no building in the decay square at " + where);
                }
            }
        }
        return checked;
    }

    private static boolean overlaps(int chunkX, int chunkZ, int x, int z, int half) {
        return (chunkX << 4) <= x + half - 1 && (chunkX << 4) + 15 >= x - half
            && (chunkZ << 4) <= z + half - 1
            && (chunkZ << 4) + 15 >= z - half;
    }

    private static boolean cityWithin(LayoutPlanner planner, int chunkX, int chunkZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (planner.isCityRaw(chunkX + dx, chunkZ + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long distanceSquared(Landmark landmark, ChunkCoordinates from) {
        long dx = landmark.heightCap()
            .centerX() - from.posX;
        long dz = landmark.heightCap()
            .centerZ() - from.posZ;
        return dx * dx + dz * dz;
    }

    private static LayoutPlanner planner(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(
            overlay != null && overlay.cities() != null,
            "expected the overlay with the layout plan in the overworld (world type "
                + HeightOracles.worldType(context.world())
                + "), found none");
        return overlay.cities()
            .planner();
    }

    /** A meteorite AE2 recorded. */
    static final class Fallen {

        final int x;
        final int y;
        final int z;
        final double size;
        final int skyMode;
        final boolean soft;

        Fallen(int x, int y, int z, double size, int skyMode, boolean soft) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
            this.skyMode = skyMode;
            this.soft = soft;
        }

        @Override
        public String toString() {
            return "meteorite(" + x
                + ","
                + y
                + ","
                + z
                + " size "
                + size
                + " sky "
                + skyMode
                + (soft ? " soft" : "")
                + ")";
        }
    }

    /**
     * Reads AE2's spawn data; the only place that touches AE2's world data, and only in this test.
     *
     * <p>
     * Source:
     * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/core/worlddata/SpawnData.java#L92-L115
     * and the keys {@code MeteoritePlacer} writes, #L199-L212 of MeteoritePlacer.java at the same tag.
     */
    static final class SpawnData {

        private SpawnData() {}

        static List<Fallen> near(int dimension, int chunkX, int chunkZ) {
            Collection<NBTTagCompound> data = appeng.core.worlddata.WorldData.instance()
                .spawnData()
                .getNearByMeteorites(dimension, chunkX, chunkZ);
            List<Fallen> out = new ArrayList<>();
            for (NBTTagCompound tag : data) {
                Block block = Block.getBlockById(tag.getInteger("blk"));
                boolean soft = block == Blocks.sand || block == Blocks.ice || block == Blocks.snow;
                out.add(
                    new Fallen(
                        tag.getInteger("x"),
                        tag.getInteger("y"),
                        tag.getInteger("z"),
                        tag.getDouble("real_sizeOfMeteorite"),
                        tag.getInteger("skyMode"),
                        soft));
            }
            return out;
        }
    }
}
