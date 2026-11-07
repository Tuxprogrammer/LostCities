package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.BlockView;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalette;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPass;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/**
 * Finds the spawners, chests and GT machines of a populated chunk that no population job completed, and says what
 * each becomes (decision log D-popsync-1).
 *
 * <p>
 * Jobs are planned without blocks, at the positions the terrain phase writes. Passes that run after the building, in
 * the terrain phase, can move a block straight down: the floating-fragment pass drops a poorly supported blob after
 * explosion damage, furniture and part blocks with it. The job then finds nothing at its position, and the block
 * below kept vanilla's defaults: a pig spawner, an empty chest, a GT machine stand-in with no machine. This sweep
 * finds such a block and gives it what its job would have:
 * <ol>
 * <li>a chest, spawner or tile job in the same column, above the block, that found nothing (a fall keeps the
 * column);</li>
 * <li>else the first furnished floor at or above it whose palette would have placed this block there
 * ({@link FurnishPass#completionAt});</li>
 * <li>else nothing: a spawner and a GT machine become air (a spawner nothing can give a mob is air, per
 * {@link SpawnerMobs}); a chest with no source is left as it is, since it is not this mod's.</li>
 * </ol>
 * The caller still checks the tile entity: one that already holds a mob or items (a vanilla dungeon's) is left alone.
 *
 * <p>
 * Plain Java, side: both. Pure: reads only the view.
 */
public final class OrphanTiles {

    /** What an orphan becomes. */
    public enum Action {
        /** Complete it: mob, loot or template from {@link Orphan}. */
        COMPLETE,
        /** Turn it into air. */
        CLEAR
    }

    /** A block no job completed, and what it becomes. */
    public static final class Orphan {

        private final int x;
        private final int y;
        private final int z;
        private final String block;
        private final Action action;
        private final String mob;
        private final String loot;
        private final String template;

        Orphan(int x, int y, int z, String block, Action action, String mob, String loot, String template) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.action = action;
            this.mob = mob;
            this.loot = loot;
            this.template = template == null ? "" : template;
        }

        /** Returns local X. */
        public int x() {
            return x;
        }

        /** Returns the block Y. */
        public int y() {
            return y;
        }

        /** Returns local Z. */
        public int z() {
            return z;
        }

        /** Returns the block's registry name, as the view reads it (a stand-in reads as the GT machine). */
        public String block() {
            return block;
        }

        /** Returns what the block becomes. */
        public Action action() {
            return action;
        }

        /** Returns the spawner's drawn mob, unresolved, or {@code null} (a spawner with none becomes air). */
        public String mob() {
            return mob;
        }

        /** Returns the chest's loot category, or {@code null} or empty for none. */
        public String loot() {
            return loot;
        }

        /** Returns the template as JSON text, empty for none. */
        public String template() {
            return template;
        }

        @Override
        public String toString() {
            return block + "@"
                + x
                + ","
                + y
                + ","
                + z
                + " "
                + action
                + (mob == null ? "" : " mob=" + mob)
                + (loot == null ? "" : " loot=" + loot)
                + (template.isEmpty() ? "" : " " + template);
        }
    }

    private OrphanTiles() {}

    /**
     * Returns the block index {@link #find} uses for a position.
     *
     * @param x local X
     * @param y block Y, 0 to 255
     * @param z local Z
     * @return the index
     */
    public static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /**
     * Returns whether a chunk's jobs can leave orphans, so the sweep is worth running.
     *
     * @param jobs the chunk's jobs
     * @return whether any job completes a chest, spawner, tile entity or furnished floor
     */
    public static boolean worthSweeping(List<PopulationJob> jobs) {
        for (PopulationJob job : jobs) {
            switch (job.kind()) {
                case CHEST, SPAWNER, TILE_ENTITY, FURNISH -> {
                    return true;
                }
                default -> {}
            }
        }
        return false;
    }

    /**
     * Finds the orphans of a chunk, lowest first.
     *
     * @param view     the populated chunk
     * @param jobs     the chunk's jobs
     * @param handled  whether a position, by {@link #index}, was completed by a job of this run
     * @param palettes the furnish palettes
     * @param minY     the lowest level to look at, inclusive
     * @param maxY     the highest level to look at, inclusive
     * @return the orphans
     */
    public static List<Orphan> find(BlockView view, List<PopulationJob> jobs, IntPredicate handled,
        FurnishPalettes palettes, int minY, int maxY) {
        List<Orphan> out = new ArrayList<>();
        for (int y = Math.max(minY, 0); y <= Math.min(maxY, 255); y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (handled.test(index(x, y, z))) {
                        continue;
                    }
                    BlockRef block = view.get(x, y, z);
                    String name = block.name();
                    boolean spawner = FurnishPalette.SPAWNER.equals(name);
                    boolean chest = FurnishPalette.CHEST.equals(name);
                    boolean machine = FurnishItem.GT_MACHINES.equals(name);
                    if (!spawner && !chest && !machine) {
                        continue;
                    }
                    Orphan orphan = resolve(view, jobs, handled, palettes, x, y, z, name, spawner, chest);
                    if (orphan != null) {
                        out.add(orphan);
                    }
                }
            }
        }
        return out;
    }

    private static Orphan resolve(BlockView view, List<PopulationJob> jobs, IntPredicate handled,
        FurnishPalettes palettes, int x, int y, int z, String name, boolean spawner, boolean chest) {
        // 1. A job of the same column, above, that found nothing at its own position.
        PopulationJob column = null;
        for (PopulationJob job : jobs) {
            if (job.x() != x || job.z() != z || job.y() <= y || job.y() > 255 || handled.test(index(x, job.y(), z))) {
                continue;
            }
            boolean fits = switch (job.kind()) {
                case SPAWNER -> spawner;
                case CHEST -> chest;
                case TILE_ENTITY -> !spawner && !chest;
                default -> false;
            };
            if (fits && (column == null || job.y() < column.y())) {
                column = job;
            }
        }
        if (column != null) {
            return switch (column.kind()) {
                case SPAWNER -> new Orphan(x, y, z, name, Action.COMPLETE, column.argument(), null, "");
                case CHEST -> new Orphan(x, y, z, name, Action.COMPLETE, null, column.argument(), column.template());
                default -> new Orphan(x, y, z, name, Action.COMPLETE, null, null, column.template());
            };
        }
        // 2. The lowest furnished floor at or above it whose palette placed this block in this column.
        PopulationJob best = null;
        FurnishPass.Completion bestCompletion = null;
        for (PopulationJob job : jobs) {
            if (job.kind() != PopulationJob.Kind.FURNISH || job.y() < y || best != null && job.y() >= best.y()) {
                continue;
            }
            FurnishPass.Completion completion = FurnishPass
                .completionAt(view, x, y, z, job.y(), job.argument(), palettes);
            if (completion != null) {
                best = job;
                bestCompletion = completion;
            }
        }
        if (bestCompletion != null) {
            return new Orphan(
                x,
                y,
                z,
                name,
                Action.COMPLETE,
                spawner ? bestCompletion.mob() : null,
                bestCompletion.loot(),
                bestCompletion.template());
        }
        // 3. No source: never a pig spawner or a machine without a machine; a chest is not ours to touch.
        return chest ? null : new Orphan(x, y, z, name, Action.CLEAR, null, null, "");
    }
}
