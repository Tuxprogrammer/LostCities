package com.tuxprogrammer.lostcities.compat.roguelike;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

/**
 * {@code oracle.roguelike} (manual): checks the Roguelike Dungeons oracle and the steering against the real mod on a
 * fresh world (ROADMAP M08; 0006 § Still open, "the Roguelike prediction's hit rate").
 *
 * <p>
 * It predicts the grid cells within {@value #CELLS} cells of a center (default the cell of chunk 0, 0, where the
 * near-spawn city stands; {@code -Dlostcitiesnh.rlScenario.center=<chunkX>,<chunkZ>} moves it) with an oracle of its
 * own, checking that predicting loads no chunk, leaves {@code world.rand} alone and answers the same in any query
 * order. It then populates each cell's dungeon chunk that is not populated yet, so Roguelike runs its search there with
 * the steering mixin in place, and reads where each dungeon went from {@link RoguelikePlacements}. It fails when a
 * dungeon's lot touches a city chunk, an edge ring or a scattered footprint (unless it is the predicted, reserved
 * lot), when no dungeon was built at all, or when the steering never ran; it reports the hit rate (a cell is a hit
 * when the dungeon stands at the predicted site, or neither predicts nor builds one), the misses by kind and the
 * points the steering refused.
 *
 * <p>
 * Needs the test hooks (the recorder records only then). Side: server.
 */
public final class RoguelikeScenario implements Scenario {

    /** Cells on each side of the center's cell. */
    static final int CELLS = 2;
    /** Time one tick may spend generating, in nanoseconds; pacing only. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;
    /** The property that moves the center. */
    static final String CENTER_PROPERTY = "lostcitiesnh.rlScenario.center";
    /** Cells described one per log line. */
    static final int DETAIL_LINES = 40;

    private RoguelikeOracle oracle;
    private final List<RoguelikeOracle.Prediction> targets = new ArrayList<>();
    private int skipped;
    private int cursor;
    private int phase;
    private boolean done;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new RoguelikeScenario());
    }

    @Override
    public String name() {
        return "oracle.roguelike";
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
        targets.clear();
        skipped = 0;
        cursor = 0;
        phase = 0;
        done = false;
        WorldServer world = context.world();
        context.check(RoguelikeOracles.isLoaded(), "expected Roguelike Dungeons loaded, found it absent");
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        context.check(overlay != null, "expected the overworld wrapped by the overlay, found it unwrapped");
        int margin = overlay.settings()
            .landmarkMargin();

        int chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
        byte[] randBefore = serialized(world.rand);
        StructureOracle built = RoguelikeOracles.forWorld(world, HeightOracles.create(world), margin);
        context.check(
            built instanceof RoguelikeOracle,
            "expected a Roguelike oracle for this world (" + RoguelikeOracles.settingsSummary(world) + "), found none");
        oracle = (RoguelikeOracle) built;
        int[] center = center();
        RoguelikeGrid grid = oracle.grid();
        int centerCellX = grid.cellOf(center[0]);
        int centerCellZ = grid.cellOf(center[1]);
        List<RoguelikeOracle.Prediction> predictions = new ArrayList<>();
        for (int cellZ = centerCellZ - CELLS; cellZ <= centerCellZ + CELLS; cellZ++) {
            for (int cellX = centerCellX - CELLS; cellX <= centerCellX + CELLS; cellX++) {
                predictions.add(oracle.predict(cellX, cellZ));
            }
        }
        int chunksAfter = world.theChunkProviderServer.getLoadedChunkCount();
        byte[] randAfter = serialized(world.rand);
        context.check(
            chunksAfter == chunksBefore,
            "expected the oracle to load no chunk, loaded chunks went from " + chunksBefore + " to " + chunksAfter);
        context.check(
            randBefore == null || randAfter == null || Arrays.equals(randBefore, randAfter),
            "expected world.rand untouched by the oracle, its state changed");
        checkOrderIndependence(context, world, margin, predictions);

        for (RoguelikeOracle.Prediction prediction : predictions) {
            Chunk chunk = world.theChunkProviderServer.chunkExists(prediction.chunkX(), prediction.chunkZ())
                ? world.getChunkFromChunkCoords(prediction.chunkX(), prediction.chunkZ())
                : null;
            if (chunk != null && chunk.isTerrainPopulated) {
                skipped++;
            } else {
                targets.add(prediction);
            }
        }
        context.check(!targets.isEmpty(), "expected unpopulated dungeon chunks to test, found every one populated");
        LostCities.LOG.info(
            "[LCNH-RL] {} cells around chunk {},{} on {}: {} to generate, {} already populated",
            predictions.size(),
            center[0],
            center[1],
            HeightOracles.worldType(world),
            targets.size(),
            skipped);
        RoguelikePlacements.clear();
    }

    @Override
    public void tick(ScenarioContext context) {
        ChunkProviderServer server = context.world().theChunkProviderServer;
        long start = System.nanoTime();
        while (cursor < targets.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            RoguelikeOracle.Prediction target = targets.get(cursor);
            if (phase == 0) {
                // The dungeon chunk and its eight neighbors, as a player walking up would load them.
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        server.loadChunk(target.chunkX() + dx, target.chunkZ() + dz);
                    }
                }
                phase = 1;
            } else {
                Chunk chunk = server.provideChunk(target.chunkX(), target.chunkZ());
                if (!chunk.isTerrainPopulated) {
                    server.populate(server, target.chunkX(), target.chunkZ());
                }
                phase = 0;
                cursor++;
            }
        }
        done = cursor >= targets.size();
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected every dungeon chunk populated, finished " + cursor + "/" + targets.size());
        List<RoguelikePlacements.Event> events = RoguelikePlacements.events();
        List<String> failures = new ArrayList<>();
        Map<SteeringRule.Outcome, Integer> refusals = new EnumMap<>(SteeringRule.Outcome.class);
        int hits = 0;
        int built = 0;
        int predicted = 0;
        int predictedNotBuilt = 0;
        int builtNotPredicted = 0;
        int moved = 0;
        int steeredCells = 0;
        int lines = 0;
        for (RoguelikeOracle.Prediction target : targets) {
            int originX = target.chunkX() * 16 + RoguelikeGrid.ORIGIN_OFFSET;
            int originZ = target.chunkZ() * 16 + RoguelikeGrid.ORIGIN_OFFSET;
            RoguelikePlacements.Event site = null;
            int refused = 0;
            for (RoguelikePlacements.Event event : events) {
                if (event.dimension() != context.world().provider.dimensionId || event.originX() != originX
                    || event.originZ() != originZ) {
                    continue;
                }
                if (event.accepted()) {
                    site = event;
                } else if (event.refusedBy() != null) {
                    refused++;
                    refusals.merge(event.refusedBy(), 1, Integer::sum);
                }
            }
            if (refused > 0) {
                steeredCells++;
            }
            if (target.hasSite()) {
                predicted++;
            }
            String verdict;
            if (site == null) {
                verdict = target.hasSite() ? "predicted, not built" : "hit (none)";
                if (target.hasSite()) {
                    predictedNotBuilt++;
                } else {
                    hits++;
                }
            } else {
                built++;
                SteeringRule.Outcome outcome = RoguelikeSteering
                    .decide(context.world().provider.dimensionId, site.x(), site.z());
                if (outcome.refuses()) {
                    failures.add("dungeon at " + site.x() + "," + site.z() + " has its lot in " + outcome);
                }
                if (!target.hasSite()) {
                    builtNotPredicted++;
                    verdict = "built, not predicted";
                } else if (target.x() == site.x() && target.z() == site.z()) {
                    hits++;
                    verdict = "hit";
                } else {
                    moved++;
                    verdict = "moved";
                }
            }
            if (lines++ < DETAIL_LINES) {
                LostCities.LOG.info(
                    "[LCNH-RL] {}: {}; actual {}; refused {}{}",
                    target,
                    verdict,
                    site == null ? "none" : site.x() + "," + site.z(),
                    refused,
                    target.hasSite() ? "; predicted site " + describeSite(context.world(), target, events) : "");
            }
        }
        int cells = targets.size();
        String summary = "cells=" + cells
            + " predicted="
            + predicted
            + " built="
            + built
            + " hits="
            + hits
            + String.format(" (%.0f%%)", cells == 0 ? 0.0 : 100.0 * hits / cells)
            + " predictedNotBuilt="
            + predictedNotBuilt
            + " builtNotPredicted="
            + builtNotPredicted
            + " moved="
            + moved
            + " steeredCells="
            + steeredCells
            + " refusals="
            + refusals
            + " skippedPopulated="
            + skipped;
        LostCities.LOG.info("[LCNH-RL] summary {}", summary);
        context.check(built > 0, "expected at least one dungeon built, found none (" + summary + ")");
        context.check(failures.isEmpty(), String.join("; ", failures) + " (" + summary + ")");
    }

    /**
     * Describes how the world sees a predicted site: the steering's outcome there now, the zone and plan kind of its
     * chunk, whether the world's own structure oracle reports it, and how the search treated the point.
     */
    private static String describeSite(WorldServer world, RoguelikeOracle.Prediction target,
        List<RoguelikePlacements.Event> events) {
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        int chunkX = target.x() >> 4;
        int chunkZ = target.z() >> 4;
        StringBuilder out = new StringBuilder();
        out.append("steering=")
            .append(RoguelikeSteering.decide(world.provider.dimensionId, target.x(), target.z()))
            .append(" kind=")
            .append(
                overlay.plan()
                    .chunk(chunkX, chunkZ)
                    .kind());
        if (overlay.cities() != null) {
            out.append(" zone=")
                .append(
                    overlay.cities()
                        .planner()
                        .zone(chunkX, chunkZ));
            boolean known = false;
            for (Landmark landmark : overlay.cities()
                .structures()
                .landmarksTouching(chunkX, chunkZ, chunkX, chunkZ)) {
                known |= landmark.id()
                    .equals(
                        target.landmark()
                            .id());
            }
            out.append(" worldOracle=")
                .append(known);
        }
        String search = "not tried";
        for (RoguelikePlacements.Event event : events) {
            if (event.x() == target.x() && event.z() == target.z()) {
                search = event.accepted() ? "accepted"
                    : event.refusedBy() != null ? "refused " + event.refusedBy() : "rejected by Roguelike";
            }
        }
        out.append(" top=")
            .append(world.getTopSolidOrLiquidBlock(target.x(), target.z()))
            .append(" ground=")
            .append(
                target.landmark()
                    .groundEstimate());
        return out.append(" search=")
            .append(search)
            .toString();
    }

    /** Checks that two fresh oracles asked in opposite orders give the same landmarks. */
    private void checkOrderIndependence(ScenarioContext context, WorldServer world, int margin,
        List<RoguelikeOracle.Prediction> predictions) {
        StructureOracle forward = RoguelikeOracles.forWorld(world, HeightOracles.create(world), margin);
        StructureOracle backward = RoguelikeOracles.forWorld(world, HeightOracles.create(world), margin);
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        for (RoguelikeOracle.Prediction prediction : predictions) {
            a.add(
                ids(
                    forward.landmarksTouching(
                        prediction.chunkX(),
                        prediction.chunkZ(),
                        prediction.chunkX(),
                        prediction.chunkZ())));
        }
        for (int i = predictions.size() - 1; i >= 0; i--) {
            RoguelikeOracle.Prediction prediction = predictions.get(i);
            b.add(
                0,
                ids(
                    backward.landmarksTouching(
                        prediction.chunkX(),
                        prediction.chunkZ(),
                        prediction.chunkX(),
                        prediction.chunkZ())));
        }
        context.check(a.equals(b), "expected the same landmarks in either query order, found " + a + " and " + b);
    }

    private static String ids(List<Landmark> landmarks) {
        StringBuilder out = new StringBuilder();
        for (Landmark landmark : landmarks) {
            out.append(landmark.id())
                .append(' ');
        }
        return out.toString();
    }

    /** Returns the center: the property's {@code <chunkX>,<chunkZ>}, or chunk 0, 0. */
    private static int[] center() {
        String value = System.getProperty(CENTER_PROPERTY);
        if (value != null) {
            String[] parts = value.split(",");
            if (parts.length == 2) {
                try {
                    return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
                } catch (NumberFormatException e) {
                    LostCities.LOG.warn("[LCNH-RL] ignoring -D{}={}: {}", CENTER_PROPERTY, value, e.getMessage());
                }
            }
        }
        return new int[] { 0, 0 };
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
