package com.tuxprogrammer.lostcities.compat.villagenames;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.MapGenStructureData;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;

/**
 * The {@code oracle.villagenames} scenario (manual only; docs/testing/README.md): compares the VillageNames oracles
 * with what VillageNames actually generates, on a fresh world of the real pack, without the city.
 *
 * <ol>
 * <li><b>Scan.</b> Asks the oracle about every village grid cell within {@value #VILLAGE_SCAN_CELLS} cells, and every
 * monument cell within {@value #MONUMENT_SCAN_CELLS} cells, of chunk ({@value #CENTER_X}, {@value #CENTER_Z}), and
 * checks that the scan loaded no chunk and left {@code world.rand} as it was. A second, fresh oracle asked tile by tile
 * in reverse order must give the same landmarks; every village's footprint must lie within
 * {@link VillageOracle#REACH_CHUNKS} of its start. Prints {@code [LCNH-VN] oracle-hash=}: two runs of one seed print
 * the same hash.</li>
 * <li><b>Generate.</b> The nearest {@value #VILLAGES} predicted villages with every chunk their footprint touches
 * (populated) and two chunks beyond; the nearest {@value #REJECTED} start chunks whose biome VillageNames rejects, 3 ×
 * 3 chunks each; the nearest {@value #MONUMENTS} predicted monuments, 7 × 7 chunks each.</li>
 * <li><b>Compare</b> with VillageNames' own saved structures ({@code data/Village.dat}, {@code data/Monument.dat},
 * read only here, in a test): each predicted village must be there, valid, with exactly the predicted piece boxes in X
 * and Z, and its buildings must show built blocks; no rejected start may hold a valid village; every valid village and
 * every monument VillageNames recorded anywhere must have been predicted, unless the chunk's own stored biome, which
 * VillageNames reads once the start chunk is loaded, explains the difference (an explained miss, reported).
 * Prints {@code [LCNH-VN] actual-hash=} over the generated villages' real pieces.</li>
 * </ol>
 * The scan center and counts can be moved with {@code -Dlostcitiesnh.vnScenario.center=<chunkX>,<chunkZ>}.
 *
 * <p>
 * Side: server.
 */
public final class VillageNamesScenario implements Scenario {

    /** Default scan center, chunk X: far from spawn and from the other scenarios' arenas. */
    static final int CENTER_X = -4096;
    /** Default scan center, chunk Z. */
    static final int CENTER_Z = 4096;
    /** Village grid cells scanned on each side of the center's cell. */
    static final int VILLAGE_SCAN_CELLS = 6;
    /** Monument grid cells scanned on each side of the center's cell. */
    static final int MONUMENT_SCAN_CELLS = 20;
    /** Predicted villages generated. */
    static final int VILLAGES = 6;
    /** Rejected start chunks generated. */
    static final int REJECTED = 6;
    /** Predicted monuments generated. */
    static final int MONUMENTS = 8;
    /** Share of a village's buildings that must show built blocks. */
    static final double BUILT_SHARE = 0.8;
    /** Monument blocks a generated monument must show at least. */
    static final int MONUMENT_MIN_BLOCKS = 200;
    /** Time one tick may spend generating, in nanoseconds; pacing only. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;
    /** The property that moves the scan center. */
    static final String CENTER_PROPERTY = "lostcitiesnh.vnScenario.center";

    private VillageNamesStructureOracle oracle;
    private int centerX;
    private int centerZ;
    private final List<VillageOracle.Prediction> villageTargets = new ArrayList<>();
    private final List<VillageOracle.Prediction> rejectedTargets = new ArrayList<>();
    private final List<Landmark> monumentTargets = new ArrayList<>();
    private final List<Job> jobs = new ArrayList<>();
    private int jobIndex;
    private boolean done;
    private String scanSummary;
    private String oracleHash;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new VillageNamesScenario());
    }

    @Override
    public String name() {
        return "oracle.villagenames";
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
    public void setup(ScenarioContext context) throws IOException {
        villageTargets.clear();
        rejectedTargets.clear();
        monumentTargets.clear();
        jobs.clear();
        jobIndex = 0;
        done = false;
        WorldServer world = context.world();
        context.check(VillageNamesOracles.isLoaded(), "expected VillageNames loaded, found it absent");
        context.check(
            VillageHandlerOrder.isPinned(),
            "expected FML's village-creation handler order pinned at postInit (D-M05O-3), found it unpinned");
        int[] center = center();
        centerX = center[0];
        centerZ = center[1];

        int chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
        byte[] randBefore = serialized(world.rand);
        StructureOracle built = VillageNamesOracles.forWorld(world);
        context.check(
            built instanceof VillageNamesStructureOracle,
            "expected a VillageNames oracle for this world (" + VillageNamesOracles.settingsSummary(world)
                + "), found none");
        oracle = (VillageNamesStructureOracle) built;
        List<VillageOracle.Prediction> starts = scanVillages();
        List<Landmark> monuments = scanMonuments();
        int chunksAfter = world.theChunkProviderServer.getLoadedChunkCount();
        byte[] randAfter = serialized(world.rand);
        context.check(
            chunksAfter == chunksBefore,
            "expected the oracle to load no chunk, loaded chunks went from " + chunksBefore + " to " + chunksAfter);
        context.check(
            randBefore == null || randAfter == null || Arrays.equals(randBefore, randAfter),
            "expected world.rand untouched by the oracle, its state changed");

        checkOrderIndependence(context, world);
        int maxReach = checkReach(context, starts);
        oracleHash = hashLandmarks(starts, monuments);
        scanSummary = summarize(starts, monuments, maxReach);
        LostCities.LOG.info(
            "[LCNH-VN] scan {} around chunk {},{} ({}): {}",
            HeightOracles.worldType(world),
            centerX,
            centerZ,
            randBefore == null ? "world.rand not serializable, untouched-check skipped" : "world.rand untouched",
            scanSummary);
        LostCities.LOG.info("[LCNH-VN] oracle-hash={}", oracleHash);

        chooseTargets(starts, monuments);
        context.check(
            !villageTargets.isEmpty(),
            "expected at least one predicted village within " + VILLAGE_SCAN_CELLS
                + " cells of the center, found none; move it with -D"
                + CENTER_PROPERTY);
        planJobs();
    }

    @Override
    public void tick(ScenarioContext context) {
        ChunkProviderServer server = context.world().theChunkProviderServer;
        long start = System.nanoTime();
        while (jobIndex < jobs.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            if (jobs.get(jobIndex)
                .step(server, start)) {
                jobIndex++;
            }
        }
        done = jobIndex >= jobs.size();
    }

    @Override
    public void verify(ScenarioContext context) throws NoSuchAlgorithmException {
        context.check(done, "expected every region generated, finished " + jobIndex + "/" + jobs.size());
        WorldServer world = context.world();
        Map<Long, Actual> actualVillages = readStructures(world, "Village");
        Map<Long, Actual> actualMonuments = readStructures(world, "Monument");
        BiomeNames stored = storedBiomes(world);
        BiomeNames provider = new ChunkManagerBiomeNames(world.getWorldChunkManager());
        VillageNamesSettings settings = VillageNamesConfig.read(world);
        List<String> failures = new ArrayList<>();

        int matched = 0;
        StringBuilder actualText = new StringBuilder();
        for (VillageOracle.Prediction target : villageTargets) {
            Landmark landmark = target.landmark();
            Actual actual = actualVillages.get(BoundedCache.key(target.chunkX(), target.chunkZ()));
            String verdict;
            if (actual == null) {
                verdict = "missing";
                failures.add(landmark.id() + " predicted but VillageNames recorded no start");
            } else if (!actual.valid) {
                verdict = "recorded invalid";
                failures.add(landmark.id() + " predicted but VillageNames recorded it as not sizeable");
            } else if (!actual.pieces.equals(sortedRects(landmark.footprint()))) {
                verdict = "differs (" + overlap(actual.pieces, sortedRects(landmark.footprint()))
                    + "/"
                    + landmark.footprint()
                        .size()
                    + " pieces equal, "
                    + actual.pieces.size()
                    + " actual)";
                failures.add(landmark.id() + " pieces differ: " + verdict);
            } else {
                verdict = "match";
                matched++;
            }
            if (actual != null) {
                actualText.append(landmark.id())
                    .append(actual.pieces)
                    .append('\n');
            }
            int[] built = builtBuildings(world, target);
            if (built[0] < BUILT_SHARE * built[1]) {
                failures
                    .add(landmark.id() + " shows built blocks in only " + built[0] + " of " + built[1] + " buildings");
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "[LCNH-VN] village %s biome=%s pieces=%d paths=%d gates=%s ground=%d actual=%s built=%d/%d",
                    landmark.id(),
                    target.biomeName(),
                    landmark.footprint()
                        .size(),
                    paths(target.plan()),
                    gates(landmark),
                    landmark.groundEstimate(),
                    verdict,
                    built[0],
                    built[1]));
        }

        for (VillageOracle.Prediction rejected : rejectedTargets) {
            Actual actual = actualVillages.get(BoundedCache.key(rejected.chunkX(), rejected.chunkZ()));
            String verdict = actual == null ? "no start" : actual.valid ? "valid village" : "recorded, not sizeable";
            if (actual != null && actual.valid) {
                String why = explainVillage(settings, stored, rejected.chunkX(), rejected.chunkZ());
                verdict += why == null ? " (unexplained)" : " (explained miss: " + why + ")";
                if (why == null) {
                    failures.add("rejected start " + rejected.chunkX() + "," + rejected.chunkZ() + " holds a village");
                }
            }
            LostCities.LOG.info(
                "[LCNH-VN] rejected {},{} biome={} actual={}",
                rejected.chunkX(),
                rejected.chunkZ(),
                rejected.biomeName(),
                verdict);
        }

        int unpredicted = 0;
        int explained = 0;
        int validVillages = 0;
        for (Actual actual : actualVillages.values()) {
            if (!actual.valid) {
                continue;
            }
            validVillages++;
            VillageOracle.Prediction prediction = oracle.villages()
                .predict(actual.chunkX, actual.chunkZ);
            if (prediction.status() == VillageOracle.Status.VILLAGE) {
                continue;
            }
            unpredicted++;
            String why = explainVillage(settings, stored, actual.chunkX, actual.chunkZ);
            if (why != null) {
                explained++;
            } else {
                failures.add(
                    "valid village at " + actual.chunkX
                        + ","
                        + actual.chunkZ
                        + " not predicted ("
                        + prediction.status()
                        + ", provider biome "
                        + provider.nameAt(actual.chunkX * 16 + 8, actual.chunkZ * 16 + 8)
                        + ")");
            }
            LostCities.LOG.info(
                "[LCNH-VN] unpredicted village {},{}: oracle {} (provider biome {}), {}",
                actual.chunkX,
                actual.chunkZ,
                prediction.status(),
                provider.nameAt(actual.chunkX * 16 + 8, actual.chunkZ * 16 + 8),
                why == null ? "unexplained" : "explained: " + why);
        }

        int monumentsMatched = 0;
        int monumentsAbsent = 0;
        for (Landmark monument : monumentTargets) {
            BlockRect box = monument.footprint()
                .get(0);
            int startX = Math.floorDiv(box.minX() + MonumentGrid.HALF - 8, 16);
            int startZ = Math.floorDiv(box.minZ() + MonumentGrid.HALF - 8, 16);
            Actual actual = actualMonuments.get(BoundedCache.key(startX, startZ));
            int blocks = monumentBlocks(world, box);
            String verdict;
            if (actual == null) {
                boolean storedAccepts = MonumentGrid.siteAccepts(startX, startZ, stored);
                verdict = storedAccepts ? "missing" : "missing (explained: the chunk's stored biomes fail the test)";
                if (!storedAccepts) {
                    monumentsAbsent++;
                }
                if (storedAccepts) {
                    failures.add(monument.id() + " predicted but VillageNames recorded no monument");
                }
            } else if (blocks < MONUMENT_MIN_BLOCKS) {
                verdict = "recorded, " + blocks + " monument blocks";
                failures.add(monument.id() + " shows only " + blocks + " monument blocks");
            } else {
                verdict = "match";
                monumentsMatched++;
            }
            LostCities.LOG.info(
                "[LCNH-VN] monument {} ground={} actual={} blocks={}",
                monument.id(),
                monument.groundEstimate(),
                verdict,
                blocks);
        }
        int monumentsUnpredicted = 0;
        for (Actual actual : actualMonuments.values()) {
            if (oracle.monuments()
                .monumentAt(actual.chunkX, actual.chunkZ) != null) {
                continue;
            }
            monumentsUnpredicted++;
            boolean storedAccepts = MonumentGrid.siteAccepts(actual.chunkX, actual.chunkZ, stored);
            LostCities.LOG.info(
                "[LCNH-VN] unpredicted monument {},{}: {}",
                actual.chunkX,
                actual.chunkZ,
                storedAccepts ? "explained: the chunk's stored biomes pass the test, the provider's do not"
                    : "unexplained");
            if (!storedAccepts) {
                failures.add("monument at " + actual.chunkX + "," + actual.chunkZ + " not predicted");
            }
        }

        LostCities.LOG.info("[LCNH-VN] actual-hash={}", sha256(actualText.toString()));
        LostCities.LOG.info(
            String.format(
                Locale.ROOT,
                "[LCNH-VN] result %s: villages generated %d, matched %d; rejected starts %d; valid villages recorded"
                    + " %d, unpredicted %d (explained %d); monuments generated %d, matched %d, absent as explained %d,"
                    + " recorded %d, unpredicted %d",
                HeightOracles.worldType(world),
                villageTargets.size(),
                matched,
                rejectedTargets.size(),
                validVillages,
                unpredicted,
                explained,
                monumentTargets.size(),
                monumentsMatched,
                monumentsAbsent,
                actualMonuments.size(),
                monumentsUnpredicted));
        context.check(failures.isEmpty(), String.join("; ", failures));
    }

    /** Returns the scan center: the property's {@code <chunkX>,<chunkZ>}, or the default. */
    private static int[] center() {
        String value = System.getProperty(CENTER_PROPERTY);
        if (value != null) {
            String[] parts = value.split(",");
            if (parts.length == 2) {
                try {
                    return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
                } catch (NumberFormatException e) {
                    LostCities.LOG.warn("[LCNH-VN] ignoring -D{}={}: {}", CENTER_PROPERTY, value, e.getMessage());
                }
            }
        }
        return new int[] { CENTER_X, CENTER_Z };
    }

    private List<VillageOracle.Prediction> scanVillages() {
        VillageOracle villages = oracle.villages();
        VillageGrid grid = villages.grid();
        int cellX = grid.cellOf(centerX);
        int cellZ = grid.cellOf(centerZ);
        List<VillageOracle.Prediction> out = new ArrayList<>();
        for (int dz = -VILLAGE_SCAN_CELLS; dz <= VILLAGE_SCAN_CELLS; dz++) {
            for (int dx = -VILLAGE_SCAN_CELLS; dx <= VILLAGE_SCAN_CELLS; dx++) {
                int[] start = grid.startChunk(cellX + dx, cellZ + dz);
                out.add(villages.predict(start[0], start[1]));
            }
        }
        return out;
    }

    private List<Landmark> scanMonuments() {
        MonumentOracle monuments = oracle.monuments();
        MonumentGrid grid = monuments.grid();
        int cellX = grid.cellOf(centerX);
        int cellZ = grid.cellOf(centerZ);
        List<Landmark> out = new ArrayList<>();
        for (int dz = -MONUMENT_SCAN_CELLS; dz <= MONUMENT_SCAN_CELLS; dz++) {
            for (int dx = -MONUMENT_SCAN_CELLS; dx <= MONUMENT_SCAN_CELLS; dx++) {
                int[] start = grid.startChunk(cellX + dx, cellZ + dz);
                Landmark landmark = monuments.monumentAt(start[0], start[1]);
                if (landmark != null) {
                    out.add(landmark);
                }
            }
        }
        return out;
    }

    /** A fresh oracle asked tile by tile, last tile first, must give what one query over the window gives. */
    private void checkOrderIndependence(ScenarioContext context, WorldServer world) {
        int half = 3 * VillageOracle.REACH_CHUNKS;
        int minX = centerX - half;
        int minZ = centerZ - half;
        int maxX = centerX + half;
        int maxZ = centerZ + half;
        TreeSet<String> whole = new TreeSet<>();
        for (Landmark landmark : oracle.landmarksTouching(minX, minZ, maxX, maxZ)) {
            whole.add(describe(landmark));
        }
        StructureOracle fresh = VillageNamesOracles.forWorld(world);
        TreeSet<String> tiled = new TreeSet<>();
        int tile = 8;
        for (int z = maxZ; z >= minZ; z -= tile) {
            for (int x = maxX; x >= minX; x -= tile) {
                for (Landmark landmark : fresh
                    .landmarksTouching(Math.max(minX, x - tile + 1), Math.max(minZ, z - tile + 1), x, z)) {
                    tiled.add(describe(landmark));
                }
            }
        }
        context.check(
            whole.equals(tiled),
            "expected the same landmarks whatever the query order, found " + whole.size()
                + " in one query and "
                + tiled.size()
                + " tile by tile");
    }

    /** Every village's footprint lies within the oracle's reach of its start chunk; returns the farthest reach. */
    private static int checkReach(ScenarioContext context, List<VillageOracle.Prediction> starts) {
        int max = 0;
        for (VillageOracle.Prediction prediction : starts) {
            Landmark landmark = prediction.landmark();
            if (landmark == null) {
                continue;
            }
            int[] bounds = landmark.coreChunkBounds(0);
            int reach = Math.max(
                Math.max(prediction.chunkX() - bounds[0], bounds[2] - prediction.chunkX()),
                Math.max(prediction.chunkZ() - bounds[1], bounds[3] - prediction.chunkZ()));
            max = Math.max(max, reach);
        }
        context.check(
            max <= VillageOracle.REACH_CHUNKS,
            "expected every village within " + VillageOracle.REACH_CHUNKS + " chunks of its start, found " + max);
        return max;
    }

    private void chooseTargets(List<VillageOracle.Prediction> starts, List<Landmark> monuments) {
        List<VillageOracle.Prediction> villages = new ArrayList<>();
        List<VillageOracle.Prediction> rejected = new ArrayList<>();
        for (VillageOracle.Prediction prediction : starts) {
            if (prediction.status() == VillageOracle.Status.VILLAGE) {
                villages.add(prediction);
            } else if (prediction.status() == VillageOracle.Status.BIOME_REJECTED) {
                rejected.add(prediction);
            }
        }
        Comparator<VillageOracle.Prediction> nearest = Comparator
            .comparingLong((VillageOracle.Prediction p) -> distanceSquared(p.chunkX(), p.chunkZ()))
            .thenComparing(p -> VillageOracle.id(p.chunkX(), p.chunkZ()));
        villages.sort(nearest);
        rejected.sort(nearest);
        villageTargets.addAll(villages.subList(0, Math.min(VILLAGES, villages.size())));
        rejectedTargets.addAll(rejected.subList(0, Math.min(REJECTED, rejected.size())));
        List<Landmark> sortedMonuments = new ArrayList<>(monuments);
        sortedMonuments.sort(Comparator.comparingLong((Landmark m) -> {
            BlockRect box = m.footprint()
                .get(0);
            return distanceSquared(box.minChunkX(), box.minChunkZ());
        })
            .thenComparing(Landmark::id));
        monumentTargets.addAll(sortedMonuments.subList(0, Math.min(MONUMENTS, sortedMonuments.size())));
    }

    private long distanceSquared(int chunkX, int chunkZ) {
        long dx = chunkX - centerX;
        long dz = chunkZ - centerZ;
        return dx * dx + dz * dz;
    }

    private void planJobs() {
        for (VillageOracle.Prediction target : villageTargets) {
            int[] bounds = target.landmark()
                .coreChunkBounds(0);
            jobs.add(
                new Job(
                    bounds[0] - 2,
                    bounds[1] - 2,
                    bounds[2] + 2,
                    bounds[3] + 2,
                    bounds[0] - 1,
                    bounds[1] - 1,
                    bounds[2],
                    bounds[3]));
        }
        for (VillageOracle.Prediction rejected : rejectedTargets) {
            int x = rejected.chunkX();
            int z = rejected.chunkZ();
            // Row order loads the start chunk before its east and south neighbors, so VillageNames decides the start
            // again once it is loaded, from the chunk's own biome: the case an explained miss comes from.
            jobs.add(new Job(x - 1, z - 1, x + 1, z + 1, x, z, x - 1, z - 1));
        }
        for (Landmark monument : monumentTargets) {
            BlockRect box = monument.footprint()
                .get(0);
            int x = Math.floorDiv(box.minX() + MonumentGrid.HALF - 8, 16);
            int z = Math.floorDiv(box.minZ() + MonumentGrid.HALF - 8, 16);
            jobs.add(new Job(x - 3, z - 3, x + 3, z + 3, x - 1, z - 1, x + 1, z + 1));
        }
    }

    /** Counts {buildings with built blocks, buildings} over a village's non-path pieces in loaded chunks. */
    private static int[] builtBuildings(WorldServer world, VillageOracle.Prediction target) {
        int built = 0;
        int total = 0;
        for (VillagePlan.Piece piece : target.plan()
            .pieces()) {
            if (piece.isPath()) {
                continue;
            }
            total++;
            if (hasBlock(world, piece.box(), 1, 255, false)) {
                built++;
            } else {
                LostCities.LOG.info(
                    "[LCNH-VN] {} building {} without built blocks; blocks around the ground: {}",
                    target.landmark()
                        .id(),
                    piece.box(),
                    blockCounts(
                        world,
                        piece.box(),
                        target.landmark()
                            .groundEstimate() - 4,
                        target.landmark()
                            .groundEstimate() + 16));
            }
        }
        return new int[] { built, total };
    }

    /** Returns the commonest non-air blocks in a box between two heights, for a failure's diagnosis. */
    private static String blockCounts(WorldServer world, BlockRect box, int minY, int maxY) {
        Map<String, Integer> counts = new TreeMap<>();
        ChunkProviderServer server = world.theChunkProviderServer;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!server.chunkExists(x >> 4, z >> 4)) {
                    counts.merge("(unloaded)", 1, Integer::sum);
                    continue;
                }
                Chunk chunk = server.provideChunk(x >> 4, z >> 4);
                for (int y = Math.max(1, minY); y <= Math.min(255, maxY); y++) {
                    String name = Block.blockRegistry.getNameForObject(chunk.getBlock(x & 15, y, z & 15));
                    if (!"minecraft:air".equals(name)) {
                        counts.merge(name, 1, Integer::sum);
                    }
                }
            }
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(
            Map.Entry.<String, Integer>comparingByValue()
                .reversed());
        return entries.subList(0, Math.min(8, entries.size()))
            .toString();
    }

    private static boolean hasBlock(WorldServer world, BlockRect box, int minY, int maxY, boolean monument) {
        return countBlocks(world, box, minY, maxY, monument, 1) > 0;
    }

    private static int monumentBlocks(WorldServer world, BlockRect box) {
        return countBlocks(world, box, 30, 70, true, Integer.MAX_VALUE);
    }

    /** Counts structure blocks in a box's loaded columns, stopping at {@code limit}. */
    private static int countBlocks(WorldServer world, BlockRect box, int minY, int maxY, boolean monument, int limit) {
        int count = 0;
        ChunkProviderServer server = world.theChunkProviderServer;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!server.chunkExists(x >> 4, z >> 4)) {
                    continue;
                }
                Chunk chunk = server.provideChunk(x >> 4, z >> 4);
                for (int y = minY; y <= maxY; y++) {
                    Block block = chunk.getBlock(x & 15, y, z & 15);
                    String name = Block.blockRegistry.getNameForObject(block);
                    if (monument ? StructureBlocks.isMonumentBlock(name) : StructureBlocks.isVillageBlock(name)) {
                        if (++count >= limit) {
                            return count;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Explains a valid village the oracle did not predict: the start chunk, once loaded, holds a stored biome at the
     * test column that is in the spawn list while the provider's is not. VillageNames reads the stored one through
     * {@code World.getBiomeGenForCoords} whenever the chunk is loaded at the time it decides; RWG dithers stored biomes
     * near biome borders.
     */
    private static String explainVillage(VillageNamesSettings settings, BiomeNames stored, int chunkX, int chunkZ) {
        String storedName = stored.nameAt(chunkX * 16 + 8, chunkZ * 16 + 8);
        if (storedName == null || !settings.isSpawnBiome(storedName)) {
            return null;
        }
        return "the chunk's stored biome is " + storedName;
    }

    /** Biome names from loaded chunks' stored biome arrays; {@code null} where the chunk is not loaded. */
    private static BiomeNames storedBiomes(WorldServer world) {
        ChunkProviderServer server = world.theChunkProviderServer;
        return (x, z) -> {
            if (!server.chunkExists(x >> 4, z >> 4)) {
                return null;
            }
            BiomeGenBase biome = server.provideChunk(x >> 4, z >> 4)
                .getBiomeGenForWorldCoords(x & 15, z & 15, world.getWorldChunkManager());
            return biome == null ? null : biome.biomeName;
        };
    }

    /** Reads a structure generator's saved starts: test-only evidence of what VillageNames generated. */
    private static Map<Long, Actual> readStructures(WorldServer world, String name) {
        Map<Long, Actual> out = new TreeMap<>();
        MapGenStructureData data = (MapGenStructureData) world.perWorldStorage
            .loadData(MapGenStructureData.class, name);
        if (data == null) {
            return out;
        }
        NBTTagCompound features = data.func_143041_a();
        for (Object key : features.func_150296_c()) {
            NBTTagCompound start = features.getCompoundTag((String) key);
            int chunkX = start.getInteger("ChunkX");
            int chunkZ = start.getInteger("ChunkZ");
            boolean valid = !start.hasKey("Valid") || start.getBoolean("Valid");
            List<String> pieces = new ArrayList<>();
            NBTTagList children = start.getTagList("Children", 10);
            for (int i = 0; i < children.tagCount(); i++) {
                int[] bb = children.getCompoundTagAt(i)
                    .getIntArray("BB");
                if (bb.length == 6) {
                    pieces.add(new BlockRect(bb[0], bb[2], bb[3], bb[5]).toString());
                }
            }
            Collections.sort(pieces);
            out.put(BoundedCache.key(chunkX, chunkZ), new Actual(chunkX, chunkZ, valid, pieces));
        }
        return out;
    }

    private static List<String> sortedRects(List<BlockRect> rects) {
        List<String> out = new ArrayList<>();
        for (BlockRect rect : rects) {
            out.add(rect.toString());
        }
        Collections.sort(out);
        return out;
    }

    private static int overlap(List<String> a, List<String> b) {
        List<String> rest = new ArrayList<>(b);
        int same = 0;
        for (String piece : a) {
            if (rest.remove(piece)) {
                same++;
            }
        }
        return same;
    }

    private static int paths(VillagePlan plan) {
        int n = 0;
        for (VillagePlan.Piece piece : plan.pieces()) {
            if (piece.isPath()) {
                n++;
            }
        }
        return n;
    }

    private static String gates(Landmark landmark) {
        StringBuilder sb = new StringBuilder();
        for (Gate gate : landmark.gates()) {
            sb.append(
                gate.facing()
                    .name()
                    .charAt(0));
        }
        return landmark.gates()
            .size() + (sb.length() > 0 ? "(" + sb + ")" : "");
    }

    private String summarize(List<VillageOracle.Prediction> starts, List<Landmark> monuments, int maxReach) {
        Map<VillageOracle.Status, Integer> counts = new TreeMap<>();
        int gated = 0;
        int villages = 0;
        for (VillageOracle.Prediction prediction : starts) {
            counts.merge(prediction.status(), 1, Integer::sum);
            if (prediction.landmark() != null) {
                villages++;
                if (!prediction.landmark()
                    .gates()
                    .isEmpty()) {
                    gated++;
                }
            }
        }
        int monumentCells = (2 * MONUMENT_SCAN_CELLS + 1) * (2 * MONUMENT_SCAN_CELLS + 1);
        return String.format(
            Locale.ROOT,
            "village cells %d %s, villages with gates %d/%d, farthest reach %d chunks; monument cells %d,"
                + " monuments %d",
            starts.size(),
            counts,
            gated,
            villages,
            maxReach,
            monumentCells,
            monuments.size());
    }

    private static String hashLandmarks(List<VillageOracle.Prediction> starts, List<Landmark> monuments) {
        StringBuilder sb = new StringBuilder();
        for (VillageOracle.Prediction prediction : starts) {
            sb.append(prediction.chunkX())
                .append(',')
                .append(prediction.chunkZ())
                .append(' ')
                .append(prediction.status())
                .append(' ')
                .append(prediction.landmark() == null ? "" : describe(prediction.landmark()))
                .append('\n');
        }
        for (Landmark monument : monuments) {
            sb.append(describe(monument))
                .append('\n');
        }
        try {
            return sha256(sb.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 missing", e);
        }
    }

    /** Every field of a landmark, in a stable text form. */
    static String describe(Landmark landmark) {
        StringBuilder sb = new StringBuilder(landmark.id());
        sb.append(' ')
            .append(landmark.kind())
            .append(' ')
            .append(landmark.certainty())
            .append(" y")
            .append(landmark.groundEstimate())
            .append(' ')
            .append(landmark.footprint())
            .append(' ')
            .append(landmark.gates());
        return sb.toString();
    }

    private static String sha256(String text) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }
        return hex.toString();
    }

    /** Returns a {@code Random}'s serialized state, or {@code null} when its class cannot be serialized. */
    private static byte[] serialized(Random random) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(random);
        } catch (java.io.NotSerializableException e) {
            return null;
        }
        return bytes.toByteArray();
    }

    /** A start VillageNames saved. */
    private static final class Actual {

        final int chunkX;
        final int chunkZ;
        final boolean valid;
        final List<String> pieces;

        Actual(int chunkX, int chunkZ, boolean valid, List<String> pieces) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.valid = valid;
            this.pieces = pieces;
        }
    }

    /** Loads a rectangle of chunks in row order, then populates an inner rectangle wherever it is not yet. */
    private static final class Job {

        private final int minX;
        private final int minZ;
        private final int maxX;
        private final int maxZ;
        private final int popMinX;
        private final int popMinZ;
        private final int popMaxX;
        private final int popMaxZ;
        private int loadCursor;
        private int populateCursor;

        Job(int minX, int minZ, int maxX, int maxZ, int popMinX, int popMinZ, int popMaxX, int popMaxZ) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
            this.popMinX = popMinX;
            this.popMinZ = popMinZ;
            this.popMaxX = popMaxX;
            this.popMaxZ = popMaxZ;
        }

        /** Works until the tick's budget is spent; returns {@code true} when the job is complete. */
        boolean step(ChunkProviderServer server, long tickStart) {
            int width = maxX - minX + 1;
            int size = width * (maxZ - minZ + 1);
            while (loadCursor < size && System.nanoTime() - tickStart < TICK_BUDGET_NANOS) {
                server.loadChunk(minX + loadCursor % width, minZ + loadCursor / width);
                loadCursor++;
            }
            if (loadCursor < size) {
                return false;
            }
            int popWidth = Math.max(0, popMaxX - popMinX + 1);
            int popSize = popWidth * Math.max(0, popMaxZ - popMinZ + 1);
            while (populateCursor < popSize && System.nanoTime() - tickStart < TICK_BUDGET_NANOS) {
                int x = popMinX + populateCursor % popWidth;
                int z = popMinZ + populateCursor / popWidth;
                populateCursor++;
                Chunk chunk = server.provideChunk(x, z);
                if (!chunk.isTerrainPopulated) {
                    server.populate(server, x, z);
                }
            }
            return populateCursor >= popSize;
        }
    }
}
