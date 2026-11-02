package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Locale;

import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.ScatteredPlanner;

/**
 * Finds the scattered structure nearest a point, from the plans alone (no block is read and no chunk is loaded), for
 * {@code /lostcities find scattered[:<name>]} and the review shot lists: optionally only one asset ({@code cabin},
 * {@code radiotower}, {@code oilrig}, {@code highway_gas_station}, {@code highway_restaurant}, with or without the
 * {@code lostcities:} prefix). The result has a target block, the structure's middle, and a viewpoint south-east of it
 * looking at it from a little above.
 *
 * <p>
 * The search walks square rings of areas outward and keeps the structure whose middle is nearest the start (ties:
 * smaller Z, then smaller X), stopping once no farther ring can hold a nearer one.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class ScatteredFinder {

    /** The find kind. */
    public static final String KIND = "scattered";
    /** Separates the kind from an asset name. */
    static final char NAME_SEPARATOR = ':';
    /** Blocks from a structure's middle to the viewpoint, plus this many per chunk of its larger side. */
    static final int VIEW_BASE = 14;
    static final int VIEW_PER_CHUNK = 10;
    /** Blocks above the ground the viewpoint rises, plus this share of the structure's height. */
    static final int VIEW_RISE = 6;
    static final double VIEW_HEIGHT_SHARE = 0.6;
    /** Most blocks of a structure's height the target aims at: its lower part, where its door and ground are. */
    static final int TARGET_HEIGHT = 12;

    /** One find. */
    public static final class Result {

        private final ScatteredPlan plan;
        private final int x;
        private final int y;
        private final int z;
        private final double viewX;
        private final double viewY;
        private final double viewZ;
        private final float yaw;
        private final float pitch;

        Result(ScatteredPlan plan, int x, int y, int z, double viewX, double viewY, double viewZ, float yaw,
            float pitch) {
            this.plan = plan;
            this.x = x;
            this.y = y;
            this.z = z;
            this.viewX = viewX;
            this.viewY = viewY;
            this.viewZ = viewZ;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        /** Returns the structure's plan. */
        public ScatteredPlan plan() {
            return plan;
        }

        /** Returns the target block's X. */
        public int x() {
            return x;
        }

        /** Returns the target block's Y. */
        public int y() {
            return y;
        }

        /** Returns the target block's Z. */
        public int z() {
            return z;
        }

        /** Returns the label: the structure's asset name. */
        public String label() {
            return ScatteredContent.label(plan);
        }

        /**
         * Returns the machine-readable line, as {@code CityFinder}'s:
         * {@code kind=scattered block=X,Y,Z chunk=CX,CZ view=X,Y,Z,YAW,PITCH label=...}.
         *
         * @return the line
         */
        public String format() {
            return String.format(
                Locale.ROOT,
                "kind=%s block=%d,%d,%d chunk=%d,%d view=%.1f,%.1f,%.1f,%.0f,%.0f label=%s",
                KIND,
                x,
                y,
                z,
                x >> 4,
                z >> 4,
                viewX,
                viewY,
                viewZ,
                yaw,
                pitch,
                label());
        }

        /** Returns the viewpoint's X. */
        public double viewX() {
            return viewX;
        }

        /** Returns the viewpoint's Y. */
        public double viewY() {
            return viewY;
        }

        /** Returns the viewpoint's Z. */
        public double viewZ() {
            return viewZ;
        }

        /** Returns the view's yaw, degrees. */
        public float yaw() {
            return yaw;
        }

        /** Returns the view's pitch, degrees. */
        public float pitch() {
            return pitch;
        }
    }

    private final ScatteredPlanner planner;

    /**
     * Creates the finder of one world.
     *
     * @param planner the world's scattered planner
     */
    public ScatteredFinder(ScatteredPlanner planner) {
        this.planner = planner;
    }

    /**
     * Returns whether a find argument names this kind: {@code scattered} or {@code scattered:<name>}.
     *
     * @param argument the command's kind argument
     * @return whether this finder handles it
     */
    public static boolean handles(String argument) {
        String lower = argument.toLowerCase(Locale.ROOT);
        return lower.equals(KIND) || lower.startsWith(KIND + NAME_SEPARATOR);
    }

    /**
     * Returns the asset name a find argument asks for.
     *
     * @param argument {@code scattered} or {@code scattered:<name>}
     * @return the name, or {@code null} for any
     */
    public static String nameOf(String argument) {
        int at = argument.indexOf(NAME_SEPARATOR);
        return at < 0 || at == argument.length() - 1 ? null
            : argument.substring(at + 1)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Finds the nearest structure.
     *
     * @param name   the asset to find, with or without its namespace; {@code null} for any
     * @param blockX the start's block X
     * @param blockZ the start's block Z
     * @param radius how far to look, in chunks
     * @return the nearest, or {@code null} when there is none within the radius
     */
    public Result find(String name, int blockX, int blockZ, int radius) {
        if (!planner.enabled()) {
            return null;
        }
        int size = planner.areaSize();
        int startX = planner.area(blockX >> 4);
        int startZ = planner.area(blockZ >> 4);
        int rings = radius / size + 1;
        ScatteredPlan best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int ring = 0; ring <= rings; ring++) {
            long nearest = (long) Math.max(0, ring - 1) * size * 16;
            if (best != null && nearest * nearest > bestDistance) {
                break;
            }
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    ScatteredPlan plan = planner.planOfArea(startX + dx, startZ + dz);
                    if (!plan.valid() || !matches(plan, name)) {
                        continue;
                    }
                    long ddx = middleX(plan) - blockX;
                    long ddz = middleZ(plan) - blockZ;
                    long distance = ddx * ddx + ddz * ddz;
                    if (distance > (long) radius * 16 * radius * 16) {
                        continue;
                    }
                    if (distance < bestDistance || distance == bestDistance && (middleZ(plan) < middleZ(best)
                        || middleZ(plan) == middleZ(best) && middleX(plan) < middleX(best))) {
                        best = plan;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best == null ? null : view(best);
    }

    /** Whether a plan's asset is the one asked for. */
    static boolean matches(ScatteredPlan plan, String name) {
        if (name == null) {
            return true;
        }
        String entry = plan.entry()
            .toLowerCase(Locale.ROOT);
        return entry.equals(name) || entry.endsWith(":" + name);
    }

    private static int middleX(ScatteredPlan plan) {
        return (plan.minChunkX() << 4) + plan.width() * 8;
    }

    private static int middleZ(ScatteredPlan plan) {
        return (plan.minChunkZ() << 4) + plan.depth() * 8;
    }

    /** The viewpoint: south-east of the middle, above the ground, looking at the structure's lower part. */
    static Result view(ScatteredPlan plan) {
        int x = middleX(plan);
        int z = middleZ(plan);
        int height = plan.asset()
            .maxHeight();
        int ground = plan.groundY() + 1;
        int y = Math.max(ground, plan.lowestLevel()) + Math.min(height, TARGET_HEIGHT) / 2;
        double back = VIEW_BASE + VIEW_PER_CHUNK * Math.max(plan.width(), plan.depth());
        double viewX = x + back * 0.6;
        double viewZ = z + back * 0.8;
        double viewY = Math.max(ground, plan.lowestLevel()) + VIEW_RISE + height * VIEW_HEIGHT_SHARE;
        float yaw = CityFinder.yawToward(viewX, viewZ, x, z);
        float pitch = (float) Math.toDegrees(Math.atan2(viewY - y, back));
        return new Result(plan, x, y, z, viewX, viewY, viewZ, yaw, pitch);
    }
}
