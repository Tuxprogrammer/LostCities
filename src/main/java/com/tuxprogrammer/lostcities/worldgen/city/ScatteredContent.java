package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkKeys;
import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.ScatteredPlanner;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkContent;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.GroundFit;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/**
 * What the overlay's plan of a non-city chunk takes from the scattered planner (07-scattered, design record 0009): in
 * a footprint chunk, the structure's writer, its population jobs (from a dry run of the same writer), the write guard
 * over the columns the structure stands on and the flag that denies lakes, lava, dungeons and oil; in every chunk the
 * fade of a structure's ground reaches, its {@link GroundFit}.
 *
 * <p>
 * A structure's ground fit is made once per structure from its parts: its pads are, per footprint chunk, the smallest
 * rectangle holding every column a floor's part, overlay part or support part fills; {@code clear} takes the whole
 * chunk, which it empties; an {@code ocean} structure stands in water and has none. A {@code repeatslice} structure
 * stands on its supports, so its ground is only cut ({@link GroundFit.Mode#CUT}).
 *
 * <p>
 * Not thread-safe. Side: both (plain Java).
 */
final class ScatteredContent {

    /** Blocks below the lower of the lowest level and the lowest ground sample where the write guard starts. */
    static final int FOOTPRINT_DEPTH = 2;
    /** Structures whose ground fit is kept. */
    static final int FIT_CACHE = 256;
    /** Stands for "no fit" in the cache, which holds no nulls. */
    private static final GroundFit[] NO_FIT = new GroundFit[0];

    private final CityWriteContext context;
    private final ScatteredPlanner planner;
    private final LruCache<Long, GroundFit[]> fits = new LruCache<>(FIT_CACHE);

    ScatteredContent(CityWriteContext context, ScatteredPlanner planner) {
        this.context = context;
        this.planner = planner;
    }

    /** Returns the scattered planner. */
    ScatteredPlanner planner() {
        return planner;
    }

    /**
     * Adds what scattered structures put in a non-city chunk's plan.
     *
     * @param builder   the chunk's plan
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param footprint the footprint so far, or {@code null}
     * @return the footprint, with the structure's columns when one stands here
     */
    int[] addTo(ChunkPlan.Builder builder, int chunkX, int chunkZ, int[] footprint) {
        for (ScatteredPlan plan : planner.plansReaching(chunkX, chunkZ, GroundFit.REACH_CHUNKS)) {
            GroundFit fit = fit(plan);
            if (fit != null && fit.reaches(chunkX, chunkZ)) {
                builder.groundFit(fit);
            }
        }
        ScatteredPlan plan = planner.planAt(chunkX, chunkZ);
        if (plan == null) {
            return footprint;
        }
        ScatteredPlan.Share share = plan.share(chunkX, chunkZ);
        builder.scattered(true)
            .content(content(plan, share))
            .jobSource(() -> dryRun(plan, share))
            .label(label(plan));
        int[] rect = rect(plan, share);
        if (rect == null) {
            return footprint;
        }
        int[] out = footprint;
        if (out == null) {
            out = new int[256];
            Arrays.fill(out, ChunkPlan.NO_FOOTPRINT);
        }
        int bottom = Math.min(plan.lowestLevel(), plan.minHeight() - 1) - FOOTPRINT_DEPTH;
        for (int z = rect[1]; z <= rect[3]; z++) {
            for (int x = rect[0]; x <= rect[2]; x++) {
                out[(z << 4) | x] = Math.min(out[(z << 4) | x], bottom);
            }
        }
        return out;
    }

    /** Returns the label of a footprint chunk: the structure's name, for the finder and logs. */
    static String label(ScatteredPlan plan) {
        return "scattered " + plan.entry();
    }

    private ChunkContent content(ScatteredPlan plan, ScatteredPlan.Share share) {
        return (buffer,
            contentContext) -> new ScatteredWriter(
                context,
                plan,
                share,
                new BufferTarget(buffer, contentContext),
                new PaletteSequence(context.worldSeed(), share.chunkX(), share.chunkZ())).write();
    }

    /**
     * Lists a footprint chunk's jobs with a dry run of its writer; floors count from the plan's lowest level (P07-6).
     */
    List<PopulationJob> dryRun(ScatteredPlan plan, ScatteredPlan.Share share) {
        int chunkX = share.chunkX();
        int chunkZ = share.chunkZ();
        com.tuxprogrammer.lostcities.layout.ChunkPlan layout = context.planner()
            .plan(chunkX, chunkZ);
        JobTarget target = new JobTarget(
            new JobTarget.Facts(
                context.worldSeed(),
                chunkX,
                chunkZ,
                context.settings()
                    .groundLevel(),
                plan.lowestLevel(),
                0,
                share.floors(),
                context.biomes()
                    .biomeOfChunk(chunkX, chunkZ),
                layout.noLoot(),
                context.settings()
                    .generateLoot(),
                context.settings()
                    .chestWithoutLootChance()));
        new ScatteredWriter(context, plan, share, target, new PaletteSequence(context.worldSeed(), chunkX, chunkZ))
            .write();
        return target.jobs();
    }

    /**
     * Returns a structure's ground fit, or {@code null} for one that stands in water.
     *
     * @param plan the structure
     * @return the fit
     */
    GroundFit fit(ScatteredPlan plan) {
        long key = ChunkKeys.of(plan.areaX(), plan.areaZ());
        GroundFit[] cached = fits.get(key, ignored -> {
            GroundFit made = makeFit(plan);
            return made == null ? NO_FIT : new GroundFit[] { made };
        });
        return cached.length == 0 ? null : cached[0];
    }

    private GroundFit makeFit(ScatteredPlan plan) {
        ScatteredView.Asset asset = plan.asset();
        if (asset.terrainHeight() == ScatteredView.TerrainHeight.OCEAN) {
            return null;
        }
        List<Integer> rects = new ArrayList<>();
        for (ScatteredPlan.Share share : plan.shares()) {
            int[] rect = asset.terrainFix() == ScatteredView.TerrainFix.CLEAR ? new int[] { 0, 0, 15, 15 }
                : rect(plan, share);
            if (rect == null) {
                continue;
            }
            int baseX = share.chunkX() << 4;
            int baseZ = share.chunkZ() << 4;
            rects.add(baseX + rect[0]);
            rects.add(baseZ + rect[1]);
            rects.add(baseX + rect[2]);
            rects.add(baseZ + rect[3]);
        }
        if (rects.isEmpty()) {
            return null;
        }
        int[] values = new int[rects.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = rects.get(i);
        }
        GroundFit.Mode mode = asset.terrainFix() == ScatteredView.TerrainFix.REPEATSLICE ? GroundFit.Mode.CUT
            : GroundFit.Mode.LEVEL;
        return new GroundFit(values, plan.groundY(), mode);
    }

    /**
     * Returns the chunk-local rectangle a share's parts fill: min X, min Z, max X, max Z, or {@code null} when they
     * fill nothing.
     */
    int[] rect(ScatteredPlan plan, ScatteredPlan.Share share) {
        Transform transform = ScatteredWriter.transform(plan.rotation());
        int[] rect = { 16, 16, -1, -1 };
        for (int f = 0; f < share.floors(); f++) {
            include(rect, part(share.part(f)), transform, false);
            include(rect, part(share.part2(f)), transform, false);
        }
        if (plan.asset()
            .terrainFix() == ScatteredView.TerrainFix.REPEATSLICE) {
            String support = plan.asset()
                .supportPart();
            include(rect, support == null ? part(share.part(0)) : part(support), transform, true);
        }
        return rect[2] < 0 ? null : rect;
    }

    private static void include(int[] rect, Part part, Transform transform, boolean bottomOnly) {
        if (part == null) {
            return;
        }
        for (int x = 0; x < part.xsize(); x++) {
            for (int z = 0; z < part.zsize(); z++) {
                if (!fills(part.column(x, z), bottomOnly)) {
                    continue;
                }
                int rx = transform.rotateX(x, z);
                int rz = transform.rotateZ(x, z);
                if (rx < 0 || rx > 15 || rz < 0 || rz > 15) {
                    continue;
                }
                rect[0] = Math.min(rect[0], rx);
                rect[1] = Math.min(rect[1], rz);
                rect[2] = Math.max(rect[2], rx);
                rect[3] = Math.max(rect[3], rz);
            }
        }
    }

    private static boolean fills(char[] column, boolean bottomOnly) {
        if (column == null) {
            return false;
        }
        if (bottomOnly) {
            return column[0] != ' ';
        }
        for (char c : column) {
            if (c != ' ') {
                return true;
            }
        }
        return false;
    }

    private Part part(String name) {
        com.tuxprogrammer.lostcities.asset.AssetId id = CityPalettes.id(name);
        return id == null ? null
            : context.assets()
                .part(id);
    }
}
