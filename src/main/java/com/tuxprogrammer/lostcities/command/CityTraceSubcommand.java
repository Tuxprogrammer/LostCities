package com.tuxprogrammer.lostcities.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.scenario.RegionJob;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities citytrace <x> <z> [radius] [gen]} (diagnostic, operator level 2): dumps every input of city
 * membership around a block to the log as {@code [LCNH-CITYTRACE]} lines, for finding out why chunks that should be
 * city are not. Output, in order: the profile values that decide membership; one line per city center whose city
 * reaches the square ({@link LayoutPlanner#traceCenter}); a membership map ({@link LayoutPlanner#traceSymbol}), a map
 * of the sampled city levels and a highway map, north at the top; then one line per chunk
 * ({@link LayoutPlanner#traceChunk}). With {@code gen} (test hooks only), it first generates and populates the
 * square, holding the server thread, so the {@code [LCNH-TRACE]} lines of the terrain and furnish passes are written
 * for it too when {@code -Dlostcitiesnh.cityTrace=true} is set. Changes nothing the layout decides.
 *
 * <p>
 * Side: server.
 */
public final class CityTraceSubcommand implements Subcommand {

    /** Default radius, chunks. */
    static final int DEFAULT_RADIUS = 40;
    /** Largest radius, chunks. */
    static final int MAX_RADIUS = 96;
    /** Prefix of the log lines. */
    static final String TAG = "[LCNH-CITYTRACE] ";
    /** Characters of the level map, one per city level. */
    private static final String LEVELS = "0123456789ABCDEFGHIJ";

    @Override
    public String name() {
        return "citytrace";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(LangKeys.COMMAND_COORDS_USAGE, name());
        }
        int[] block = CommandSupport.parseTarget(new String[] { args[0], args[1] }, null, name());
        int radius = DEFAULT_RADIUS;
        boolean gen = false;
        for (int i = 2; i < args.length; i++) {
            if ("gen".equals(args[i])) {
                gen = true;
            } else {
                try {
                    radius = Math.max(1, Math.min(MAX_RADIUS, Integer.parseInt(args[i])));
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(LangKeys.COMMAND_COORDS_USAGE, name());
                }
            }
        }
        if (gen && !DevEnvironment.testHooksEnabled()) {
            throw new CommandException(LangKeys.TEST_DISABLED);
        }
        OverlayWorld overlay = CommandSupport.cities(sender);
        int cx = block[0] >> 4;
        int cz = block[1] >> 4;
        if (gen) {
            long start = System.nanoTime();
            RegionJob job = RegionJob.around(cx, cz, radius);
            while (!job.step(overlay.world().theChunkProviderServer, System.nanoTime(), Long.MAX_VALUE / 4)) {
                // Runs to completion on this thread; the job's own budget check never stops it.
            }
            LostCities.LOG.info(
                "{}generated and populated chunks {},{} radius {} in {} s",
                TAG,
                cx,
                cz,
                radius,
                (System.nanoTime() - start) / 1_000_000_000L);
        }
        dump(
            overlay.cities()
                .planner(),
            overlay.dimension(),
            cx,
            cz,
            radius);
    }

    /** Writes the trace of the square of {@code radius} chunks around a chunk. */
    static void dump(LayoutPlanner planner, int dimension, int cx, int cz, int radius) {
        LayoutProfile p = planner.profile();
        LostCities.LOG.info(
            "{}dim {} around chunk {},{} radius {}: cityMinHeight={} cityMaxHeight={} cityMaxRise={} cityAvoidOcean={}"
                + " cityConnected={} seaLevel={} waterLevel={} cityThreshold={} cityHighwayLayout={}",
            TAG,
            dimension,
            cx,
            cz,
            radius,
            p.cityMinHeight,
            p.cityMaxHeight,
            p.cityMaxRise,
            p.cityAvoidOcean,
            p.cityConnected,
            p.seaLevel,
            p.waterLevel(),
            p.cityThreshold,
            p.cityHighwayLayout);
        Map<Long, int[]> centers = new LinkedHashMap<>();
        for (int z = cz - radius; z <= cz + radius; z += 4) {
            for (int x = cx - radius; x <= cx + radius; x += 4) {
                for (int[] c : planner.traceCenters(x, z)) {
                    centers.put(((long) c[0] << 32) ^ (c[1] & 0xffffffffL), c);
                }
            }
        }
        for (int[] c : centers.values()) {
            LostCities.LOG.info("{}{}", TAG, planner.traceCenter(c[0], c[1]));
        }
        List<String> symbols = new ArrayList<>();
        List<String> levels = new ArrayList<>();
        List<String> highways = new ArrayList<>();
        for (int z = cz - radius; z <= cz + radius; z++) {
            StringBuilder s = new StringBuilder();
            StringBuilder l = new StringBuilder();
            StringBuilder h = new StringBuilder();
            for (int x = cx - radius; x <= cx + radius; x++) {
                char symbol = planner.traceSymbol(x, z);
                s.append(x == cx && z == cz && symbol == '.' ? '@' : symbol);
                int level = planner.plan(x, z)
                    .cityLevel();
                l.append(level >= 0 && level < LEVELS.length() ? LEVELS.charAt(level) : '?');
                int highway = planner.plan(x, z)
                    .maxHighwayLevel();
                h.append(highway < 0 ? '.' : LEVELS.charAt(Math.min(highway, LEVELS.length() - 1)));
            }
            symbols.add(s.toString());
            levels.add(l.toString());
            highways.add(h.toString());
        }
        map(
            "membership (B building, + city, # core, d cut off, z zone, h height, o ocean, s shallow, r rise,"
                + " t threshold, . none)",
            symbols);
        map("city level per chunk", levels);
        map("highway level per chunk (. none)", highways);
        for (int z = cz - radius; z <= cz + radius; z++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                LostCities.LOG.info("{}{}", TAG, planner.traceChunk(x, z));
            }
        }
        LostCities.LOG.info("{}end", TAG);
    }

    private static void map(String title, List<String> rows) {
        LostCities.LOG.info("{}map {}", TAG, title);
        for (String row : rows) {
            LostCities.LOG.info("{}|{}|", TAG, row);
        }
    }
}
