package com.tuxprogrammer.lostcities.compat.rwg2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

import rwg2.world.ChunkManagerRealistic;

/**
 * The {@code oracle.rwg2} scenario (manual only; docs/testing/README.md): checks the RWG2 terrain-landmark oracle
 * against RWG2's own protection rule on an RWG2 world of the real pack.
 *
 * <ol>
 * <li><b>Find.</b> Volcano and lava-cave centers on a {@value #SEARCH_SPACING}-block grid within
 * {@value #SEARCH_RADIUS} blocks of the spawn, straight from the chunk manager; junctions by asking the oracle about
 * every chunk within {@value #JUNCTION_RADIUS} chunks. Each center found must come back from the oracle under its
 * id.</li>
 * <li><b>Dense check.</b> Around up to {@value #VOLCANOES} volcanoes, {@value #CAVES} caves, {@value #JUNCTIONS}
 * junctions and {@value #WINDOWS} windows of open country, every column RWG2's rule protects (sampled every 4 blocks
 * around discs, every 2 around junctions and in the windows) must lie under a landmark's footprint, with RWG2's
 * 8-block margin. Reports the protected columns, the misses, and how much more the footprints reserve.</li>
 * <li><b>City.</b> Where the world's city layout already knows the landmarks (the oracle wired into the structure
 * oracles), no chunk a landmark's core touches may be a city chunk.</li>
 * </ol>
 * The oracle must load no chunk.
 *
 * <p>
 * Side: server. Load only when RWG2 is present.
 */
public final class Rwg2LandmarkScenario implements Scenario {

    /** Disc-search radius around the spawn, blocks. */
    static final int SEARCH_RADIUS = 12288;
    /** Disc-search spacing, blocks: well under a lava cave's influence radius. */
    static final int SEARCH_SPACING = 192;
    /** Junction-scan radius around the spawn, chunks. */
    static final int JUNCTION_RADIUS = 96;
    /** Volcanoes checked densely. */
    static final int VOLCANOES = 2;
    /** Lava caves checked densely. */
    static final int CAVES = 3;
    /** Junctions checked densely. */
    static final int JUNCTIONS = 4;
    /** Open-country windows checked densely. */
    static final int WINDOWS = 4;
    /** Time one tick may spend, in nanoseconds; pacing only. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;

    private Rwg2LandmarkOracle oracle;
    private ManagerLandforms reference;
    private int spawnX;
    private int spawnZ;
    private int chunksBefore;
    private final TreeSet<Long> volcanoCenters = new TreeSet<>();
    private final TreeSet<Long> caveCenters = new TreeSet<>();
    private final TreeMap<String, Landmark> found = new TreeMap<>();
    private final List<Check> checks = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private int searchCursor;
    private int junctionCursor;
    private int checkIndex;
    private boolean checksPlanned;
    private boolean done;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Rwg2LandmarkScenario());
    }

    @Override
    public String name() {
        return "oracle.rwg2";
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
        volcanoCenters.clear();
        caveCenters.clear();
        found.clear();
        checks.clear();
        failures.clear();
        searchCursor = 0;
        junctionCursor = 0;
        checkIndex = 0;
        checksPlanned = false;
        done = false;
        WorldServer world = context.world();
        context.check(Rwg2Landmarks.applies(world), "expected an RWG2 world, found another type");
        StructureOracle built = Rwg2Landmarks.forWorld(world, null, LayoutProfile.defaults().landmarkMargin);
        context.check(built instanceof Rwg2LandmarkOracle, "expected an RWG2 landmark oracle, found none");
        oracle = (Rwg2LandmarkOracle) built;
        reference = new ManagerLandforms((ChunkManagerRealistic) world.getWorldChunkManager());
        spawnX = world.getSpawnPoint().posX;
        spawnZ = world.getSpawnPoint().posZ;
        chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
        LostCities.LOG.info(
            "[LCNH-RWG2] landmark scenario around {},{} ({})",
            spawnX,
            spawnZ,
            Rwg2Landmarks.settingsSummary(world));
    }

    @Override
    public void tick(ScenarioContext context) {
        long start = System.nanoTime();
        int side = 2 * SEARCH_RADIUS / SEARCH_SPACING + 1;
        while (searchCursor < side * side && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            int x = spawnX - SEARCH_RADIUS + searchCursor % side * SEARCH_SPACING;
            int z = spawnZ - SEARCH_RADIUS + searchCursor / side * SEARCH_SPACING;
            searchCursor++;
            long volcano = reference.volcanoCenter(x, z);
            if (volcano != Rwg2Landforms.NONE) {
                volcanoCenters.add(volcano);
            }
            long cave = reference.lavaCaveCenter(x, z);
            if (cave != Rwg2Landforms.NONE) {
                caveCenters.add(cave);
            }
        }
        int junctionSide = 2 * JUNCTION_RADIUS / 8 + 1;
        while (searchCursor >= side * side && junctionCursor < junctionSide * junctionSide
            && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            int chunkX = (spawnX >> 4) - JUNCTION_RADIUS + junctionCursor % junctionSide * 8;
            int chunkZ = (spawnZ >> 4) - JUNCTION_RADIUS + junctionCursor / junctionSide * 8;
            junctionCursor++;
            for (Landmark landmark : oracle.landmarksTouching(chunkX, chunkZ, chunkX + 7, chunkZ + 7)) {
                found.put(landmark.id(), landmark);
            }
        }
        if (junctionCursor < junctionSide * junctionSide) {
            return;
        }
        if (!checksPlanned) {
            planChecks();
            checksPlanned = true;
        }
        while (checkIndex < checks.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            if (checks.get(checkIndex)
                .step(start)) {
                checkIndex++;
            }
        }
        done = checkIndex >= checks.size();
    }

    private void planChecks() {
        int volcanoes = 0;
        for (long center : volcanoCenters) {
            Landmark landmark = landmarkFor("volcano", center);
            if (landmark != null && volcanoes < VOLCANOES) {
                checks.add(new Check(landmark.id(), bounds(landmark).grow(16), 4));
                volcanoes++;
            }
        }
        int caves = 0;
        for (long center : caveCenters) {
            Landmark landmark = landmarkFor("lavacave", center);
            if (landmark != null && caves < CAVES) {
                checks.add(new Check(landmark.id(), bounds(landmark).grow(16), 2));
                caves++;
            }
        }
        int junctions = 0;
        for (Landmark landmark : found.values()) {
            if (landmark.id()
                .startsWith("junction@") && junctions < JUNCTIONS) {
                checks.add(new Check(landmark.id(), bounds(landmark).grow(24), 2));
                junctions++;
            }
        }
        for (int i = 0; i < WINDOWS; i++) {
            int x = spawnX + (i - WINDOWS / 2) * 1024 + 300;
            int z = spawnZ + (i % 2 == 0 ? 700 : -900);
            checks.add(new Check("window" + i, new BlockRect(x, z, x + 127, z + 127), 2));
        }
    }

    /** Returns the oracle's landmark for a center found directly, recording a failure when it is missing. */
    private Landmark landmarkFor(String kind, long center) {
        int x = Rwg2Landforms.unpackX(center);
        int z = Rwg2Landforms.unpackZ(center);
        String id = Rwg2LandmarkOracle.id(kind, x, z);
        for (Landmark landmark : oracle.landmarksTouching(x >> 4, z >> 4, x >> 4, z >> 4)) {
            if (landmark.id()
                .equals(id)) {
                found.put(id, landmark);
                return landmark;
            }
        }
        failures.add(id + " found in the chunk manager but not returned by the oracle");
        return null;
    }

    private static BlockRect bounds(Landmark landmark) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockRect rect : landmark.footprint()) {
            minX = Math.min(minX, rect.minX());
            minZ = Math.min(minZ, rect.minZ());
            maxX = Math.max(maxX, rect.maxX());
            maxZ = Math.max(maxZ, rect.maxZ());
        }
        return new BlockRect(minX, minZ, maxX, maxZ);
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected every check run, finished " + checkIndex + "/" + checks.size());
        WorldServer world = context.world();
        int chunksAfter = world.theChunkProviderServer.getLoadedChunkCount();
        if (chunksAfter > chunksBefore) {
            failures.add("loaded chunks went from " + chunksBefore + " to " + chunksAfter + " with no player");
        }
        int protectedColumns = 0;
        int misses = 0;
        long reserved = 0;
        long sampled = 0;
        for (Check check : checks) {
            protectedColumns += check.protectedColumns;
            misses += check.misses;
            reserved += check.reserved;
            sampled += check.sampled;
            LostCities.LOG.info(
                "[LCNH-RWG2] check {}: {} samples, {} protected, {} reserved, {} missed{}",
                check.name,
                check.sampled,
                check.protectedColumns,
                check.reserved,
                check.misses,
                check.firstMiss == null ? "" : " (first at " + check.firstMiss + ")");
            if (check.misses > 0) {
                failures.add(
                    check.name + ": " + check.misses + " protected columns not reserved, first " + check.firstMiss);
            }
        }
        String cityCheck = cityCheck(world);
        long volcanoes = found.keySet()
            .stream()
            .filter(id -> id.startsWith("volcano@"))
            .count();
        long caves = found.keySet()
            .stream()
            .filter(id -> id.startsWith("lavacave@"))
            .count();
        long junctions = found.keySet()
            .stream()
            .filter(id -> id.startsWith("junction@"))
            .count();
        String summary = String.format(
            Locale.ROOT,
            "found %d volcanoes, %d lava caves, %d junctions; dense: %d samples, %d protected, %d reserved, %d missed; %s",
            volcanoes,
            caves,
            junctions,
            sampled,
            protectedColumns,
            reserved,
            misses,
            cityCheck);
        LostCities.LOG.info("[LCNH-RWG2] {}", summary);
        LostCities.LOG.info("[LCNH-RWG2] landmarks {}", found.keySet());
        context.check(failures.isEmpty(), summary + "; " + failures);
    }

    private String cityCheck(WorldServer world) {
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null || overlay.cities() == null) {
            return "city check skipped (no layout on this world)";
        }
        StructureOracle structures = overlay.cities()
            .structures();
        CityPlan plan = overlay.plan();
        int margin = overlay.settings()
            .landmarkMargin();
        int known = 0;
        int chunks = 0;
        for (Landmark landmark : found.values()) {
            BlockRect first = landmark.footprint()
                .get(0);
            boolean wired = structures
                .landmarksTouching(first.minChunkX(), first.minChunkZ(), first.minChunkX(), first.minChunkZ())
                .stream()
                .anyMatch(
                    l -> l.id()
                        .equals(landmark.id()));
            if (!wired) {
                continue;
            }
            known++;
            for (BlockRect rect : landmark.footprint()) {
                BlockRect core = rect.grow(margin);
                for (int x = core.minChunkX(); x <= core.maxChunkX(); x++) {
                    for (int z = core.minChunkZ(); z <= core.maxChunkZ(); z++) {
                        chunks++;
                        if (plan.chunk(x, z)
                            .isCity()) {
                            failures.add(landmark.id() + ": core chunk " + x + "," + z + " is a city chunk");
                        }
                    }
                }
            }
        }
        return known == 0 ? "city check skipped (the layout does not know RWG2 landmarks)"
            : "city check: " + known + " landmarks, " + chunks + " core chunks";
    }

    /** One dense comparison of RWG2's rule with the oracle's footprints over a rectangle. */
    private final class Check {

        final String name;
        final BlockRect area;
        final int spacing;
        int cursor;
        int sampled;
        int protectedColumns;
        int reserved;
        int misses;
        String firstMiss;

        Check(String name, BlockRect area, int spacing) {
            this.name = name;
            this.area = area;
            this.spacing = spacing;
        }

        /** Works until the tick's budget is spent; returns {@code true} when the check is complete. */
        boolean step(long tickStart) {
            int width = (area.maxX() - area.minX()) / spacing + 1;
            int size = width * ((area.maxZ() - area.minZ()) / spacing + 1);
            int margin = Rwg2LandmarkOracle.RWG2_MARGIN;
            while (cursor < size && System.nanoTime() - tickStart < TICK_BUDGET_NANOS) {
                int x = area.minX() + cursor % width * spacing;
                int z = area.minZ() + cursor / width * spacing;
                cursor++;
                sampled++;
                if (oracle.covers(x, z)) {
                    reserved++;
                }
                if (!reference.protectedByRwg2(x, z)) {
                    continue;
                }
                protectedColumns++;
                boolean covered = oracle.covers(x, z) && oracle.covers(x - margin, z - margin)
                    && oracle.covers(x + margin, z + margin)
                    && oracle.covers(x - margin, z + margin)
                    && oracle.covers(x + margin, z - margin);
                if (!covered) {
                    misses++;
                    if (firstMiss == null) {
                        firstMiss = x + "," + z;
                    }
                }
            }
            return cursor >= size;
        }
    }
}
