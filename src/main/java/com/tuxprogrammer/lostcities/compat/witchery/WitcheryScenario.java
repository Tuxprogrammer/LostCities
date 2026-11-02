package com.tuxprogrammer.lostcities.compat.witchery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
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

/**
 * The {@code oracle.witchery} scenario (manual only; docs/testing/README.md): compares the Witchery oracle with what
 * Witchery actually generates, on a fresh world of the real pack.
 *
 * <ol>
 * <li><b>Scan.</b> Asks a fresh oracle about every grid cell within {@value #SCAN_CELLS} cells of chunk
 * ({@value #CENTER_X}, {@value #CENTER_Z}); the scan must load no chunk and leave {@code world.rand} as it was, and a
 * second oracle asked tile by tile in reverse order must give the same lots. Prints {@code [LCNH-WITCHERY]
 * oracle-hash=}.</li>
 * <li><b>Generate</b> the nearest {@value #PREDICTED} predicted candidates and {@value #REJECTED} candidates whose
 * biome the oracle rejects, 5 × 5 chunks loaded and 3 × 3 populated around each.</li>
 * <li><b>Compare</b> with the structures Witchery's own generator recorded ({@code structuresList}, read only here): a
 * predicted candidate must be recorded and a rejected one must not, unless the chunk's stored biome (which Witchery
 * reads once the chunk exists) explains the difference; every Witchery block around a recorded structure must lie in
 * its union lot. Where the world's city layout already knows the lot (the oracle wired into the structure oracles), no
 * chunk the lot's core touches may be a city chunk.</li>
 * </ol>
 * The center can be moved with {@code -Dlostcitiesnh.witcheryScenario.center=<chunkX>,<chunkZ>}.
 *
 * <p>
 * Side: server.
 */
public final class WitcheryScenario implements Scenario {

    /** Default scan center, chunk X: far from spawn and from the other scenarios' arenas. */
    static final int CENTER_X = 3072;
    /** Default scan center, chunk Z. */
    static final int CENTER_Z = -3072;
    /** Grid cells scanned on each side of the center's cell. */
    static final int SCAN_CELLS = 8;
    /** Predicted candidates generated. */
    static final int PREDICTED = 8;
    /** Rejected candidates generated. */
    static final int REJECTED = 6;
    /** Heights scanned for Witchery blocks around a structure. */
    static final int SCAN_MIN_Y = 40;
    /** Highest height scanned. */
    static final int SCAN_MAX_Y = 160;
    /** Time one tick may spend generating, in nanoseconds; pacing only. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;
    /** The property that moves the scan center. */
    static final String CENTER_PROPERTY = "lostcitiesnh.witcheryScenario.center";
    /**
     * Vanilla blocks Witchery's standalone structures place that RWG's natural decoration does not (decompiled
     * component classes; see {@link WitcherySettings}).
     */
    static final Set<String> STRUCTURE_BLOCKS = new TreeSet<>(
        Arrays.asList(
            "minecraft:oak_stairs",
            "minecraft:spruce_stairs",
            "minecraft:birch_stairs",
            "minecraft:glass_pane",
            "minecraft:planks",
            "minecraft:web",
            "minecraft:wooden_pressure_plate",
            "minecraft:wooden_door",
            "minecraft:torch",
            "minecraft:hay_block",
            "minecraft:fence"));

    private WitcheryOracle oracle;
    private final List<int[]> predicted = new ArrayList<>();
    private final List<int[]> rejected = new ArrayList<>();
    private final List<int[]> jobs = new ArrayList<>();
    private int jobIndex;
    private int loadCursor;
    private int populateCursor;
    private boolean done;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new WitcheryScenario());
    }

    @Override
    public String name() {
        return "oracle.witchery";
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
        predicted.clear();
        rejected.clear();
        jobs.clear();
        jobIndex = 0;
        loadCursor = 0;
        populateCursor = 0;
        done = false;
        WorldServer world = context.world();
        context.check(WitcheryOracles.isLoaded(), "expected Witchery loaded, found it absent");
        int[] center = center();

        int chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
        byte[] randBefore = serialized(world.rand);
        int margin = LayoutProfile.defaults().landmarkMargin;
        StructureOracle built = WitcheryOracles.forWorld(world, HeightOracles.create(world), margin);
        context.check(
            built instanceof WitcheryOracle,
            "expected a Witchery oracle for this world (" + WitcheryOracles.settingsSummary(world) + "), found none");
        oracle = (WitcheryOracle) built;
        TreeMap<String, Landmark> scanned = scan(oracle, center, false);
        int chunksAfter = world.theChunkProviderServer.getLoadedChunkCount();
        byte[] randAfter = serialized(world.rand);
        context.check(
            chunksAfter == chunksBefore,
            "expected the oracle to load no chunk, loaded chunks went from " + chunksBefore + " to " + chunksAfter);
        context.check(
            randBefore == null || randAfter == null || Arrays.equals(randBefore, randAfter),
            "expected world.rand untouched by the oracle, its state changed");

        WitcheryOracle second = (WitcheryOracle) WitcheryOracles.forWorld(world, HeightOracles.create(world), margin);
        TreeMap<String, Landmark> reversed = scan(second, center, true);
        context.check(
            reversed.keySet()
                .equals(scanned.keySet()),
            "expected the same lots asked in reverse tiles, found " + reversed.size() + " against " + scanned.size());

        WitcheryGrid grid = oracle.grid();
        int cellX = grid.cellOf(center[0]);
        int cellZ = grid.cellOf(center[1]);
        List<int[]> accepted = new ArrayList<>();
        List<int[]> refused = new ArrayList<>();
        for (int x = cellX - SCAN_CELLS; x <= cellX + SCAN_CELLS; x++) {
            for (int z = cellZ - SCAN_CELLS; z <= cellZ + SCAN_CELLS; z++) {
                int[] candidate = grid.candidateChunk(x, z);
                (oracle.structureAt(candidate[0], candidate[1]) != null ? accepted : refused).add(candidate);
            }
        }
        Comparator<int[]> nearest = Comparator
            .comparingLong(
                (int[] c) -> (long) (c[0] - center[0]) * (c[0] - center[0])
                    + (long) (c[1] - center[1]) * (c[1] - center[1]))
            .thenComparingInt(c -> c[1])
            .thenComparingInt(c -> c[0]);
        accepted.sort(nearest);
        refused.sort(nearest);
        predicted.addAll(accepted.subList(0, Math.min(PREDICTED, accepted.size())));
        rejected.addAll(refused.subList(0, Math.min(REJECTED, refused.size())));
        jobs.addAll(predicted);
        jobs.addAll(rejected);
        LostCities.LOG.info(
            "[LCNH-WITCHERY] scan {} around chunk {},{} ({}): {} cells, {} lots, {} rejected; generating {} + {}",
            HeightOracles.worldType(world),
            center[0],
            center[1],
            oracle.settings(),
            (2 * SCAN_CELLS + 1) * (2 * SCAN_CELLS + 1),
            accepted.size(),
            refused.size(),
            predicted.size(),
            rejected.size());
        LostCities.LOG.info(
            "[LCNH-WITCHERY] oracle-hash={}",
            Integer.toHexString(
                scanned.keySet()
                    .hashCode()));
        context.check(
            !predicted.isEmpty(),
            "expected a predicted lot near the center, found none; move it with -D" + CENTER_PROPERTY);
    }

    private static TreeMap<String, Landmark> scan(WitcheryOracle oracle, int[] center, boolean reverse) {
        int span = SCAN_CELLS * oracle.grid()
            .cellSize() + oracle.grid()
                .cellSize();
        int tile = 16;
        TreeMap<String, Landmark> out = new TreeMap<>();
        for (int i = 0; i <= 2 * span / tile; i++) {
            for (int j = 0; j <= 2 * span / tile; j++) {
                int a = reverse ? 2 * span / tile - i : i;
                int b = reverse ? 2 * span / tile - j : j;
                int minX = center[0] - span + a * tile;
                int minZ = center[1] - span + b * tile;
                for (Landmark landmark : oracle.landmarksTouching(minX, minZ, minX + tile - 1, minZ + tile - 1)) {
                    out.put(landmark.id(), landmark);
                }
            }
        }
        return out;
    }

    @Override
    public void tick(ScenarioContext context) {
        ChunkProviderServer server = context.world().theChunkProviderServer;
        long start = System.nanoTime();
        while (jobIndex < jobs.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            int[] target = jobs.get(jobIndex);
            if (loadCursor < 25) {
                server.loadChunk(target[0] - 2 + loadCursor % 5, target[1] - 2 + loadCursor / 5);
                loadCursor++;
            } else if (populateCursor < 9) {
                int x = target[0] - 1 + populateCursor % 3;
                int z = target[1] - 1 + populateCursor / 3;
                populateCursor++;
                Chunk chunk = server.provideChunk(x, z);
                if (!chunk.isTerrainPopulated) {
                    server.populate(server, x, z);
                }
            } else {
                jobIndex++;
                loadCursor = 0;
                populateCursor = 0;
            }
        }
        done = jobIndex >= jobs.size();
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected every candidate generated, finished " + jobIndex + "/" + jobs.size());
        WorldServer world = context.world();
        Set<Long> recorded = recordedStructures();
        context.check(recorded != null, "expected Witchery's recorded structures readable, found them unreadable");
        Set<Integer> disallowed = WitcheryConfig.disallowedBiomes();
        List<String> failures = new ArrayList<>();
        int hits = 0;
        int explained = 0;
        int blocksIn = 0;
        for (int[] target : predicted) {
            String id = WitcheryOracle.id(target[0], target[1]);
            boolean stands = recorded.contains(BoundedCache.key(target[0], target[1]));
            int[] blocks = witcheryBlocks(world, target);
            if (stands) {
                hits++;
                blocksIn += blocks[0];
                if (blocks[1] > 0) {
                    failures.add(id + ": " + blocks[1] + " structure blocks outside the union lot");
                }
            } else if (storedBiomeDisallowed(world, target, disallowed)) {
                explained++;
            } else {
                failures.add(id + " predicted but Witchery recorded no structure");
            }
            BiomeGenBase biome = storedBiome(world, target);
            LostCities.LOG.info(
                "[LCNH-WITCHERY] lot {} {} storedBiome={} structureBlocks={} outside={}",
                id,
                stands ? "stands" : "missing",
                biome == null ? "none" : biome.biomeName,
                blocks[0],
                blocks[1]);
        }
        int rejectedClear = 0;
        for (int[] target : rejected) {
            boolean stands = recorded.contains(BoundedCache.key(target[0], target[1]));
            if (!stands) {
                rejectedClear++;
            } else if (storedBiomeDisallowed(world, target, disallowed)) {
                failures.add(target[0] + "," + target[1] + " recorded although its stored biome is disallowed");
            } else {
                explained++;
                LostCities.LOG.info(
                    "[LCNH-WITCHERY] unpredicted structure at {},{}: stored biome {} differs from the provider's",
                    target[0],
                    target[1],
                    storedBiome(world, target));
            }
        }
        String cityCheck = cityCheck(world, failures);
        String summary = String.format(
            Locale.ROOT,
            "predicted %d: %d stand, %d explained by stored biomes; rejected %d: %d clear; structure blocks in standing lots %d; %s",
            predicted.size(),
            hits,
            explained,
            rejected.size(),
            rejectedClear,
            blocksIn,
            cityCheck);
        LostCities.LOG.info("[LCNH-WITCHERY] {}", summary);
        context.check(failures.isEmpty(), summary + "; " + failures);
    }

    /** Checks that no lot's core is a city chunk where the world's layout knows the lot. */
    private String cityCheck(WorldServer world, List<String> failures) {
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null || overlay.cities() == null) {
            return "city check skipped (no layout on this world)";
        }
        StructureOracle structures = overlay.cities()
            .structures();
        CityPlan plan = overlay.plan();
        int margin = overlay.settings()
            .landmarkMargin();
        int lots = 0;
        int chunks = 0;
        for (int[] target : predicted) {
            String id = WitcheryOracle.id(target[0], target[1]);
            boolean known = structures.landmarksTouching(target[0], target[1], target[0], target[1])
                .stream()
                .anyMatch(
                    l -> l.id()
                        .equals(id));
            if (!known) {
                continue;
            }
            lots++;
            BlockRect core = oracle.lot(target[0], target[1])
                .grow(margin);
            for (int x = core.minChunkX(); x <= core.maxChunkX(); x++) {
                for (int z = core.minChunkZ(); z <= core.maxChunkZ(); z++) {
                    chunks++;
                    if (plan.chunk(x, z)
                        .isCity()) {
                        failures.add(id + ": core chunk " + x + "," + z + " is a city chunk");
                    }
                }
            }
        }
        return lots == 0 ? "city check skipped (the layout does not know Witchery lots)"
            : "city check: " + lots + " lots, " + chunks + " core chunks";
    }

    /**
     * Counts structure blocks around a structure in chunks the city does not hold: {@code {inside the lot, outside
     * it}}. Witchery's structures are built mostly of vanilla blocks; the ones counted are those its covens, shacks,
     * wicker men and hobgoblin huts place and RWG's natural decoration does not ({@link #STRUCTURE_BLOCKS}), plus any
     * block Witchery registers.
     */
    private int[] witcheryBlocks(WorldServer world, int[] target) {
        BlockRect lot = oracle.lot(target[0], target[1]);
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        CityPlan plan = overlay == null ? null : overlay.plan();
        int inside = 0;
        int outside = 0;
        for (int cx = target[0] - 1; cx <= target[0] + 1; cx++) {
            for (int cz = target[1] - 1; cz <= target[1] + 1; cz++) {
                if (!world.theChunkProviderServer.chunkExists(cx, cz) || plan != null && plan.chunk(cx, cz)
                    .isCity()) {
                    continue;
                }
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = SCAN_MIN_Y; y <= SCAN_MAX_Y; y++) {
                            Block block = chunk.getBlock(x, y, z);
                            String name = Block.blockRegistry.getNameForObject(block);
                            if (name != null && (name.startsWith(WitcheryOracles.MOD_ID + ":")
                                || STRUCTURE_BLOCKS.contains(name))) {
                                if (lot.contains((cx << 4) + x, (cz << 4) + z)) {
                                    inside++;
                                } else {
                                    outside++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return new int[] { inside, outside };
    }

    private static boolean storedBiomeDisallowed(WorldServer world, int[] target, Set<Integer> disallowed) {
        BiomeGenBase biome = storedBiome(world, target);
        return biome != null && disallowed.contains(biome.biomeID);
    }

    /** The biome Witchery read: the candidate chunk's stored biome at its test column. */
    private static BiomeGenBase storedBiome(WorldServer world, int[] target) {
        if (!world.theChunkProviderServer.chunkExists(target[0], target[1])) {
            return null;
        }
        return world.getChunkFromChunkCoords(target[0], target[1])
            .getBiomeGenForWorldCoords(
                WitcheryOracle.BIOME_OFFSET,
                WitcheryOracle.BIOME_OFFSET,
                world.getWorldChunkManager());
    }

    /** Reads Witchery's own record of the structures it generated this session, as chunk keys; test-only evidence. */
    private static Set<Long> recordedStructures() {
        Object generator = WitcheryConfig.generator();
        if (generator == null) {
            return null;
        }
        try {
            Field field = generator.getClass()
                .getDeclaredField("structuresList");
            field.setAccessible(true);
            Object list = field.get(generator);
            if (!(list instanceof Collection<?>)) {
                return null;
            }
            Set<Long> out = new TreeSet<>();
            for (Object entry : (Collection<?>) list) {
                if (entry instanceof ChunkCoordIntPair pair) {
                    // Witchery records the chunk's block origin.
                    out.add(BoundedCache.key(pair.chunkXPos >> 4, pair.chunkZPos >> 4));
                }
            }
            return out;
        } catch (ReflectiveOperationException | SecurityException e) {
            LostCities.LOG.warn("[LCNH-WITCHERY] cannot read Witchery's structure list ({})", e.toString());
            return null;
        }
    }

    private static int[] center() {
        String value = System.getProperty(CENTER_PROPERTY);
        if (value != null) {
            String[] parts = value.split(",");
            if (parts.length == 2) {
                try {
                    return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
                } catch (NumberFormatException e) {
                    LostCities.LOG.warn("[LCNH-WITCHERY] ignoring malformed {}={}", CENTER_PROPERTY, value);
                }
            }
        }
        return new int[] { CENTER_X, CENTER_Z };
    }

    private static byte[] serialized(Random random) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(random);
        } catch (NotSerializableException e) {
            return null;
        }
        return bytes.toByteArray();
    }
}
