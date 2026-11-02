package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Blocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.synthetic.SyntheticCityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * The overlay's in-world scenarios (docs/testing/README.md, CONTRIBUTING § Testing layer 3):
 * <ul>
 * <li>{@code overlay.installed}: the overworld is wrapped, with a pinned snapshot, and no chunk existed before the
 * wrapper on a new world;</li>
 * <li>{@code overlay.city}: generates the synthetic city at chunk (2048, 2048) and its surroundings on a fresh world
 * and checks it: streets at the city ground, no RWG logs or leaves inside the footprint, landmark cores left as RWG
 * made them, the village lane laid, every glowstone lit (ArchaicFix's Phosphor, P03-8), and prints the chunk hashes two
 * same-seed worlds must share;</li>
 * <li>{@code overlay.budget} (manual only): generates 64 × 64 chunks in spiral order around the synthetic city at
 * (1024, 1024), drops the first 512 chunks as JIT warm-up, and fails when budget B1 to B4 is exceeded (0005 §8).</li>
 * </ul>
 * The city scenarios need a world pinned with the synthetic plan source ({@code -Dlostcitiesnh.planSource=synthetic}
 * with the test hooks).
 *
 * <p>
 * Side: server.
 */
public final class OverlayScenarios {

    /** Center of the city scenario's synthetic city, in chunks; a multiple of {@link SyntheticCityPlan#PERIOD}. */
    static final int CITY_CENTER = 2048;
    /** Half-side of the generated square around it, in chunks: the city, its blend and debris reach, and margin. */
    static final int CITY_RADIUS = SyntheticCityPlan.RADIUS + 6;
    /** First candidate center of the budget scenario's region, in chunks; later candidates step along X. */
    static final int BUDGET_CENTER = 1024;
    /**
     * Offset of the budget region's center from its city, in chunks on both axes: the spiral's first 512 chunks
     * (radius 11) stay clear of the city and its blend, and the whole city still lies inside the region.
     */
    static final int BUDGET_CITY_OFFSET = 20;
    /** Candidate city centers the budget scenario tries, one city period apart. */
    static final int BUDGET_CANDIDATES = 24;
    /** City chunks the budget region's city must have: most of its 13 × 13 square, not an ocean. */
    static final int BUDGET_MIN_CITY_CHUNKS = 100;
    /** Half-side of the budget region: 64 × 64 chunks, as 0005 §8 asks (65 × 65 with the center). */
    static final int BUDGET_RADIUS = 32;
    /** Chunks generated before the budget timers start counting (0005 §8: JIT warm-up). */
    static final int WARM_UP_CHUNKS = 512;

    private OverlayScenarios() {}

    /**
     * Adds the overlay's scenarios to a registry.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Installed());
        registry.register(new City());
        registry.register(new Budget());
    }

    private static OverlayWorld requireOverlay(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(
            overlay != null,
            "expected the overlay in the overworld (world type " + HeightOracles.worldType(context.world())
                + "), found none");
        return overlay;
    }

    private static void requireSynthetic(ScenarioContext context, OverlayWorld overlay) {
        context.check(
            ProfileSnapshot.PLAN_SYNTHETIC.equals(
                overlay.snapshot()
                    .planSource()),
            "expected a world pinned with the synthetic plan (-Dlostcitiesnh.planSource=synthetic), found plan "
                + overlay.snapshot()
                    .planSource());
    }

    /** The wrapper is installed on the overworld, with a snapshot. */
    static final class Installed implements Scenario {

        @Override
        public String name() {
            return "overlay.installed";
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public void setup(ScenarioContext context) {}

        @Override
        public void verify(ScenarioContext context) {
            OverlayWorld overlay = requireOverlay(context);
            context.check(
                context.world().theChunkProviderServer.currentChunkProvider instanceof OverlayChunkProvider,
                "expected the overlay's provider as currentChunkProvider");
            LostCities.LOG.info(
                "[LCNH-OVERLAY] installed: {}; chunks loaded before install: {}",
                overlay.snapshot()
                    .summary(),
                overlay.chunksBeforeInstall());
        }
    }

    /** Generates the synthetic city at chunk (2048, 2048) and checks it. */
    static final class City implements Scenario {

        private RegionGenerator generator;
        private boolean done;

        @Override
        public String name() {
            return "overlay.city";
        }

        @Override
        public int ticks() {
            return 20 * 60 * 5;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            OverlayWorld overlay = requireOverlay(context);
            requireSynthetic(context, overlay);
            int inner = CITY_RADIUS - 2;
            overlay.recorder()
                .record(CITY_CENTER - inner, CITY_CENTER + inner, CITY_CENTER - inner, CITY_CENTER + inner);
            generator = new RegionGenerator(context.world(), CITY_CENTER, CITY_CENTER, CITY_RADIUS);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick();
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the region generated, loaded " + generator.loaded() + "/" + generator.size());
            OverlayWorld overlay = requireOverlay(context);
            ChunkRecorder recorder = overlay.recorder();
            recorder.stop();
            int inner = CITY_RADIUS - 2;
            int expected = (2 * inner + 1) * (2 * inner + 1);
            context.check(
                recorder.terrainHashes()
                    .size() == expected,
                "expected " + expected
                    + " freshly generated chunks, recorded "
                    + recorder.terrainHashes()
                        .size()
                    + "; run on a fresh world (--fresh)");
            WorldServer world = context.world();
            CityPlan plan = overlay.plan();
            int cityChunks = 0;
            int streets = 0;
            int streetsAtGround = 0;
            int foliageInFootprint = 0;
            int lights = 0;
            int litLights = 0;
            int coresTouched = 0;
            int laneColumns = 0;
            StringBuilder foliageSeen = new StringBuilder();
            // Light the region as the chunk tick would near a player: ArchaicFix's Phosphor initializes a chunk's
            // light once its 3 x 3 neighborhood exists, from func_150809_p (P03-8).
            for (int cz = CITY_CENTER - inner; cz <= CITY_CENTER + inner; cz++) {
                for (int cx = CITY_CENTER - inner; cx <= CITY_CENTER + inner; cx++) {
                    Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                    if (!chunk.isLightPopulated) {
                        chunk.func_150809_p();
                    }
                }
            }
            for (int cz = CITY_CENTER - inner; cz <= CITY_CENTER + inner; cz++) {
                for (int cx = CITY_CENTER - inner; cx <= CITY_CENTER + inner; cx++) {
                    ChunkPlan chunkPlan = plan.chunk(cx, cz);
                    Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                    if (chunkPlan.isCore()) {
                        TerrainPhase.ChunkClass treated = recorder.chunkClasses()
                            .get(ChunkRecorder.key(cx, cz));
                        if (chunkPlan.lane() == null && treated != TerrainPhase.ChunkClass.UNTOUCHED) {
                            coresTouched++;
                        }
                        if (chunkPlan.lane() != null) {
                            laneColumns += laneColumns(chunk, chunkPlan);
                        }
                        continue;
                    }
                    if (!chunkPlan.isCity()) {
                        continue;
                    }
                    cityChunks++;
                    if (!chunkPlan.hasBuilding() && chunkPlan.hasFootprint()) {
                        streets++;
                        if (chunk.getBlock(5, chunkPlan.groundY(), 5) == Blocks.double_stone_slab) {
                            streetsAtGround++;
                        }
                    }
                    foliageInFootprint += foliageInFootprint(chunk, chunkPlan, foliageSeen);
                    if (chunkPlan.hasBuilding()) {
                        int[] counts = lights(world, cx, cz, chunkPlan);
                        lights += counts[0];
                        litLights += counts[1];
                    }
                }
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "[LCNH-OVERLAY] city: %d city chunks, streets at ground %d/%d, RWG foliage in footprint %d,"
                        + " lights lit %d/%d, cores touched %d, lane columns %d, guard dropped %d writes,"
                        + " cascades during population %d, logs and leaves past the guard %d",
                    cityChunks,
                    streetsAtGround,
                    streets,
                    foliageInFootprint,
                    litLights,
                    lights,
                    coresTouched,
                    laneColumns,
                    FootprintGuard.droppedWrites(),
                    overlay.cascades(),
                    FootprintGuard.leakedFoliage()));
            if (foliageSeen.length() > 0) {
                LostCities.LOG.info(
                    "[LCNH-OVERLAY] foliage in footprint: {} (right after our population phase: {})",
                    foliageSeen,
                    recorder.foliageAfterPopulation());
            }
            LostCities.LOG.info(
                "[LCNH-HASH] worldType={} seed={} chunks={} terrain={} population={}",
                HeightOracles.worldType(world),
                world.getSeed(),
                recorder.terrainHashes()
                    .size(),
                combined(recorder.terrainHashes()),
                combined(recorder.populationHashes()));
            context
                .check(cityChunks > 0, "expected city chunks around (2048, 2048), found none (all too high or low?)");
            context.check(
                streets == 0 || streetsAtGround * 4 >= streets * 3,
                "expected at least 3/4 of street chunks paved at the city ground, found " + streetsAtGround
                    + "/"
                    + streets);
            // RWG's decoration is kept out by the guard during its populate; trees other mods plant afterwards from
            // their IWorldGenerators (IC2 rubber trees, HarvestCraft, Natura) on park grass may overhang a street.
            // They are reported above, not failed (other-worldgen.md, T3).
            context.check(FootprintGuard.droppedWrites() > 0, "expected the write guard to drop RWG's decoration");
            context.check(
                FootprintGuard.leakedFoliage() == 0,
                "expected no log or leaf past the guard during population, found " + FootprintGuard.leakedFoliage());
            context.check(coresTouched == 0, "expected landmark cores untouched, " + coresTouched + " were written");
            context
                .check(lights == 0 || litLights == lights, "expected every glowstone lit, " + litLights + "/" + lights);
        }

        private static int laneColumns(Chunk chunk, ChunkPlan plan) {
            int paved = 0;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (!plan.lane()
                        .contains(x, z)) {
                        continue;
                    }
                    int top = chunk.getHeightValue(x, z) - 1;
                    for (int y = Math.max(0, top); y >= Math.max(0, top - 2); y--) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block == Blocks.gravel || block == Blocks.planks
                            || block == Blocks.cobblestone
                            || "grass_path".equals(blockName(block))) {
                            paved++;
                            break;
                        }
                    }
                }
            }
            return paved;
        }

        private static String blockName(Block block) {
            Object name = Block.blockRegistry.getNameForObject(block);
            if (name == null) {
                return "";
            }
            String text = name.toString();
            return text.substring(text.indexOf(':') + 1);
        }

        /** RWG's logs and leaves above the city ground in footprint columns: the write guard must keep them out. */
        private static int foliageInFootprint(Chunk chunk, ChunkPlan plan, StringBuilder seen) {
            int found = 0;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int bottom = plan.footprintBottom(x, z);
                    if (bottom == ChunkPlan.NO_FOOTPRINT) {
                        continue;
                    }
                    for (int y = Math.max(bottom, plan.groundY() + 1); y <= WorldLimits.MAX_Y; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block instanceof BlockLog || block instanceof BlockLeavesBase) {
                            found++;
                            if (found <= 3 && seen.length() < 2000) {
                                seen.append(Block.blockRegistry.getNameForObject(block))
                                    .append('@')
                                    .append((chunk.xPosition << 4) + x)
                                    .append(',')
                                    .append(y)
                                    .append(',')
                                    .append((chunk.zPosition << 4) + z)
                                    .append(' ');
                            }
                        }
                    }
                }
            }
            return found;
        }

        /** Counts the synthetic building's ceiling lights and those with block light beside them. */
        private static int[] lights(WorldServer world, int chunkX, int chunkZ, ChunkPlan plan) {
            int total = 0;
            int lit = 0;
            for (int level = plan.groundY(); level < plan.maxHeight(); level += WorldLimits.FLOOR_HEIGHT) {
                int y = level + WorldLimits.FLOOR_HEIGHT - 1;
                int x = (chunkX << 4) + 7;
                int z = (chunkZ << 4) + 7;
                if (y > WorldLimits.MAX_Y || world.getBlock(x, y, z) != Blocks.glowstone
                    || !world.isAirBlock(x, y - 1, z)) {
                    continue;
                }
                total++;
                if (world.getSavedLightValue(EnumSkyBlock.Block, x, y - 1, z) > 0) {
                    lit++;
                }
            }
            return new int[] { total, lit };
        }
    }

    /** Generates 64 × 64 chunks and checks budgets B1 to B4. */
    static final class Budget implements Scenario {

        private RegionGenerator generator;
        private boolean warmedUp;
        private boolean done;

        @Override
        public String name() {
            return "overlay.budget";
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
            OverlayWorld overlay = requireOverlay(context);
            requireSynthetic(context, overlay);
            int city = budgetCenter(context, overlay.plan());
            // The spiral starts in open country beside the city, so the city is generated after the warm-up chunks.
            int center = city + BUDGET_CITY_OFFSET;
            LostCities.LOG.info(
                "[LCNH-BUDGET] 65 x 65 chunks around ({}, {}), synthetic city at ({}, {})",
                center,
                BUDGET_CENTER + BUDGET_CITY_OFFSET,
                city,
                BUDGET_CENTER);
            generator = new RegionGenerator(context.world(), center, BUDGET_CENTER + BUDGET_CITY_OFFSET, BUDGET_RADIUS);
        }

        @Override
        public void tick(ScenarioContext context) {
            done = generator.tick();
            if (!warmedUp && generator.loaded() >= WARM_UP_CHUNKS) {
                requireOverlay(context).timers()
                    .reset();
                warmedUp = true;
            }
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected the region generated, loaded " + generator.loaded() + "/" + generator.size());
            OverlayWorld overlay = requireOverlay(context);
            GenerationTimers timers = overlay.timers();
            for (String line : timers.report()) {
                LostCities.LOG.info("[LCNH-BUDGET] {}", line);
            }
            StringBuilder failed = new StringBuilder();
            for (GenerationTimers.Verdict verdict : timers.budgets(overlay.cacheBytes())) {
                LostCities.LOG.info("[LCNH-BUDGET] {}", verdict);
                if (!verdict.met() && !"B5".equals(verdict.id())) {
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

    /**
     * Picks the budget region's center: the first synthetic city along Z = {@link #BUDGET_CENTER}, stepping X by the
     * city period away from the city scenario's region, whose plan holds a mostly dry city. The plan is pure, so the
     * choice is the same for a seed and costs only oracle samples.
     */
    static int budgetCenter(ScenarioContext context, CityPlan plan) {
        for (int i = 0; i < BUDGET_CANDIDATES; i++) {
            int center = BUDGET_CENTER + i * SyntheticCityPlan.PERIOD * 2;
            if (Math.abs(center - CITY_CENTER) <= CITY_RADIUS + BUDGET_RADIUS) {
                continue;
            }
            int cities = 0;
            for (int dz = -SyntheticCityPlan.RADIUS; dz <= SyntheticCityPlan.RADIUS; dz++) {
                for (int dx = -SyntheticCityPlan.RADIUS; dx <= SyntheticCityPlan.RADIUS; dx++) {
                    if (plan.chunk(center + dx, BUDGET_CENTER + dz)
                        .isCity()) {
                        cities++;
                    }
                }
            }
            if (cities >= BUDGET_MIN_CITY_CHUNKS) {
                return center;
            }
        }
        context.check(false, "expected a dry synthetic city among " + BUDGET_CANDIDATES + " candidates, found none");
        return BUDGET_CENTER;
    }

    /** Returns the SHA-256 of every hash in key order, one per line with its chunk key. */
    static String combined(Map<Long, String> hashes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<Long, String> entry : hashes.entrySet()) {
                digest.update((entry.getKey() + "=" + entry.getValue() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            StringBuilder out = new StringBuilder();
            for (byte b : digest.digest()) {
                out.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java platform", e);
        }
    }
}
