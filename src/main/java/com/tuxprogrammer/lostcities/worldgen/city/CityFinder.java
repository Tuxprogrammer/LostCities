package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Locale;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/**
 * Finds things to look at and check in a world's cities, from the plan alone (no block is read and no chunk is
 * loaded), for the test-hook command {@code /lostcities find} and the screenshot tooling: the nearest city center, a
 * primary street, an enterable building, the edge where a city meets RWG terrain, a reserved lot (a landmark core or
 * its edge ring) and a village sewn into a city (a gate, lane or link chunk). Each result has a target block and a
 * suggested viewpoint with a view direction.
 *
 * <p>
 * The search walks square rings of chunks outward from a start chunk and keeps the target nearest the start by
 * block distance (ties: smaller Z, then smaller X), stopping once no farther ring can hold a nearer one. The reserved
 * and village kinds find something only where a structure oracle knows a landmark.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class CityFinder {

    /** What to find. */
    public enum Kind {

        /** A city center: the chunk a city sphere is centered on. */
        CENTER,
        /** A primary street chunk. */
        STREET,
        /** A building of at least two floors, not ruined, with a door onto a street. */
        INTERIOR,
        /** A city chunk on land beside a non-city chunk: the blend into RWG terrain. */
        EDGE,
        /** A landmark core or a city chunk of its edge ring. */
        RESERVED,
        /** A chunk of a village's sewing: a gate, lane or link chunk. */
        VILLAGE;

        /** Returns the lower-case name the command takes. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * Returns the kind of a name.
         *
         * @param key the name, any case
         * @return the kind, or {@code null}
         */
        public static Kind of(String key) {
            for (Kind kind : values()) {
                if (kind.key()
                    .equalsIgnoreCase(key)) {
                    return kind;
                }
            }
            return null;
        }
    }

    /** One find: the target block and a viewpoint. */
    public static final class Result {

        private final Kind kind;
        private final int chunkX;
        private final int chunkZ;
        private final int x;
        private final int y;
        private final int z;
        private final double viewX;
        private final double viewY;
        private final double viewZ;
        private final float yaw;
        private final float pitch;
        private final String label;

        Result(Kind kind, int chunkX, int chunkZ, int x, int y, int z, double viewX, double viewY, double viewZ,
            float yaw, float pitch, String label) {
            this.kind = kind;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.x = x;
            this.y = y;
            this.z = z;
            this.viewX = viewX;
            this.viewY = viewY;
            this.viewZ = viewZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.label = label;
        }

        /** Returns what was found. */
        public Kind kind() {
            return kind;
        }

        /** Returns the target's chunk X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the target's chunk Z. */
        public int chunkZ() {
            return chunkZ;
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

        /** Returns the viewpoint's X (a player's feet). */
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

        /** Returns the view's yaw, degrees (0 south, 90 west, 180 north, 270 east). */
        public float yaw() {
            return yaw;
        }

        /** Returns the view's pitch, degrees (positive looks down). */
        public float pitch() {
            return pitch;
        }

        /** Returns what stands at the target. */
        public String label() {
            return label;
        }

        /**
         * Returns this result with another viewpoint, for the command's adjustment to open air.
         *
         * @param newX the viewpoint's X
         * @param newY the viewpoint's Y
         * @param newZ the viewpoint's Z
         * @return the result
         */
        public Result withView(double newX, double newY, double newZ) {
            return new Result(kind, chunkX, chunkZ, x, y, z, newX, newY, newZ, yaw, pitch, label);
        }

        /**
         * Returns the one-line machine-readable form the command logs and automation reads:
         * {@code kind=street block=X,Y,Z chunk=CX,CZ view=X,Y,Z,YAW,PITCH label=...}.
         *
         * @return the line
         */
        public String format() {
            return String.format(
                Locale.ROOT,
                "kind=%s block=%d,%d,%d chunk=%d,%d view=%.1f,%.1f,%.1f,%.0f,%.0f label=%s",
                kind.key(),
                x,
                y,
                z,
                chunkX,
                chunkZ,
                viewX,
                viewY,
                viewZ,
                yaw,
                pitch,
                label);
        }

        @Override
        public String toString() {
            return format();
        }
    }

    /** Height of a player's feet above the walking surface's block. */
    static final int FEET = 1;
    /** How far above a city's ground the overview hovers, in blocks. */
    static final int OVERVIEW_HEIGHT = 70;
    /** How far south of the target the overview hovers, in blocks. */
    static final int OVERVIEW_BACK = 60;
    /** Pitch of the overview, degrees. */
    static final float OVERVIEW_PITCH = 40f;
    /** How far outside the city the edge view stands, in blocks. */
    static final int EDGE_BACK = 28;
    /** How far above the natural ground the edge view stands. */
    static final int EDGE_HEIGHT = 18;
    /** Pitch of the edge view, degrees. */
    static final float EDGE_PITCH = 20f;
    /** Height above a landmark the reserved-lot and village views hover. */
    static final int LANDMARK_HEIGHT = 40;
    /** Distance south of the target those views hover. */
    static final int LANDMARK_BACK = 36;
    /** Pitch of those views. */
    static final float LANDMARK_PITCH = 45f;
    /** Fewest floors of a building worth entering. */
    static final int INTERIOR_MIN_FLOORS = 2;

    private final LayoutPlanner planner;
    private final CityPlan plan;

    /**
     * Creates the finder of one world.
     *
     * @param planner the world's layout planner
     * @param plan    the world's plan source, for grounds and natural heights
     */
    public CityFinder(LayoutPlanner planner, CityPlan plan) {
        this.planner = planner;
        this.plan = plan;
    }

    /**
     * Finds the nearest thing of a kind.
     *
     * @param kind   what to find
     * @param blockX the start's block X
     * @param blockZ the start's block Z
     * @param radius how far to look, in chunks
     * @return the nearest find, or {@code null} when there is none within the radius
     */
    public Result find(Kind kind, int blockX, int blockZ, int radius) {
        return find(kind, blockX, blockZ, radius, result -> true);
    }

    /**
     * Finds the nearest thing of a kind that a filter accepts.
     *
     * @param kind   what to find
     * @param blockX the start's block X
     * @param blockZ the start's block Z
     * @param radius how far to look, in chunks
     * @param accept which finds count
     * @return the nearest accepted find, or {@code null}
     */
    public Result find(Kind kind, int blockX, int blockZ, int radius, java.util.function.Predicate<Result> accept) {
        int startX = blockX >> 4;
        int startZ = blockZ >> 4;
        Result best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int ring = 0; ring <= radius; ring++) {
            long nearestInRing = (long) Math.max(0, ring - 1) * 16;
            if (best != null && nearestInRing * nearestInRing > bestDistance) {
                break;
            }
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    Result found = at(kind, startX + dx, startZ + dz);
                    if (found == null || !accept.test(found)) {
                        continue;
                    }
                    long ddx = found.x - blockX;
                    long ddz = found.z - blockZ;
                    long distance = ddx * ddx + ddz * ddz;
                    if (distance < bestDistance
                        || distance == bestDistance && (found.z < best.z || found.z == best.z && found.x < best.x)) {
                        best = found;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Returns what a chunk offers for a kind, or {@code null}.
     *
     * @param kind   the kind
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the result
     */
    Result at(Kind kind, int chunkX, int chunkZ) {
        switch (kind) {
            case CENTER:
                return center(chunkX, chunkZ);
            case STREET:
                return street(chunkX, chunkZ);
            case INTERIOR:
                return interior(chunkX, chunkZ);
            case EDGE:
                return edge(chunkX, chunkZ);
            case RESERVED:
                return reserved(chunkX, chunkZ);
            default:
                return village(chunkX, chunkZ);
        }
    }

    private Result center(int chunkX, int chunkZ) {
        if (!planner.isCityCenter(chunkX, chunkZ) || !planner.plan(chunkX, chunkZ)
            .isCity()) {
            return null;
        }
        int ground = plan.chunk(chunkX, chunkZ)
            .groundY();
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        return new Result(
            Kind.CENTER,
            chunkX,
            chunkZ,
            x,
            ground,
            z,
            x + 0.5,
            ground + OVERVIEW_HEIGHT,
            z + OVERVIEW_BACK + 0.5,
            180f,
            OVERVIEW_PITCH,
            "city center " + planner.plan(chunkX, chunkZ)
                .cityStyle());
    }

    private Result street(int chunkX, int chunkZ) {
        ChunkPlan layout = planner.plan(chunkX, chunkZ);
        if (!layout.isCity() || layout.hasBuilding()
            || !layout.isPrimaryRoad()
            || planner.streetSlope(chunkX, chunkZ) != null) {
            return null;
        }
        int ground = layout.cityGroundLevel();
        boolean alongX = planner.plan(chunkX - 1, chunkZ)
            .isPrimaryRoad()
            || planner.plan(chunkX + 1, chunkZ)
                .isPrimaryRoad();
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        // Stand at the street's middle and look along it: east along X, south along Z.
        return new Result(
            Kind.STREET,
            chunkX,
            chunkZ,
            x,
            ground,
            z,
            x + 0.5,
            ground + FEET,
            z + 0.5,
            alongX ? 270f : 0f,
            5f,
            "primary street");
    }

    private Result interior(int chunkX, int chunkZ) {
        ChunkPlan layout = planner.plan(chunkX, chunkZ);
        if (!layout.hasBuilding() || layout.floors() < INTERIOR_MIN_FLOORS
            || layout.ruinHeight() >= 0
            || !layout.multiPos()
                .isSingle()) {
            return null;
        }
        String door = null;
        float yaw = 0f;
        for (com.tuxprogrammer.lostcities.layout.Direction side : com.tuxprogrammer.lostcities.layout.Direction
            .values()) {
            ChunkPlan neighbor = planner.plan(chunkX + side.stepX(), chunkZ + side.stepZ());
            if (planner.doorway(chunkX, chunkZ, 0, side) == com.tuxprogrammer.lostcities.layout.Doorway.DOOR
                && neighbor.isCity()
                && !neighbor.hasBuilding()) {
                door = side.name();
                yaw = doorYaw(side);
                break;
            }
        }
        if (door == null) {
            return null;
        }
        int ground = layout.cityGroundLevel();
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        return new Result(
            Kind.INTERIOR,
            chunkX,
            chunkZ,
            x,
            ground,
            z,
            x + 0.5,
            ground + FEET,
            z + 0.5,
            yaw,
            0f,
            "building " + layout.building() + ", " + layout.floors() + " floors, door " + door);
    }

    private Result edge(int chunkX, int chunkZ) {
        ChunkPlan layout = planner.plan(chunkX, chunkZ);
        if (!layout.isCity() || layout.hasBuilding()) {
            return null;
        }
        for (com.tuxprogrammer.lostcities.layout.Direction side : com.tuxprogrammer.lostcities.layout.Direction
            .values()) {
            int nx = chunkX + side.stepX();
            int nz = chunkZ + side.stepZ();
            com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan outside = plan.chunk(nx, nz);
            if (outside.kind() != com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind.NONE || outside.ocean()) {
                continue;
            }
            int x = (chunkX << 4) + 8 + side.stepX() * 8;
            int z = (chunkZ << 4) + 8 + side.stepZ() * 8;
            int ground = layout.cityGroundLevel();
            double viewX = x + side.stepX() * EDGE_BACK + 0.5;
            double viewZ = z + side.stepZ() * EDGE_BACK + 0.5;
            return new Result(
                Kind.EDGE,
                chunkX,
                chunkZ,
                x,
                ground,
                z,
                viewX,
                Math.max(ground, outside.naturalHeight()) + EDGE_HEIGHT,
                viewZ,
                yawToward(viewX, viewZ, x, z),
                EDGE_PITCH,
                "city edge toward " + side.name());
        }
        return null;
    }

    private Result reserved(int chunkX, int chunkZ) {
        Zone zone = planner.zone(chunkX, chunkZ)
            .zone();
        if (zone != Zone.CORE && !(zone == Zone.EDGE_RING && planner.plan(chunkX, chunkZ)
            .isCity())) {
            return null;
        }
        return landmarkView(
            Kind.RESERVED,
            chunkX,
            chunkZ,
            (zone == Zone.CORE ? "landmark core " : "edge ring of ") + planner.zone(chunkX, chunkZ)
                .landmarkId());
    }

    private Result village(int chunkX, int chunkZ) {
        ChunkPlan layout = planner.plan(chunkX, chunkZ);
        LaneRole role = layout.lane()
            .role();
        if (role != LaneRole.GATE && role != LaneRole.LANE && role != LaneRole.LINK) {
            return null;
        }
        return landmarkView(
            Kind.VILLAGE,
            chunkX,
            chunkZ,
            role.name()
                .toLowerCase(Locale.ROOT) + " of "
                + layout.lane()
                    .landmarkId());
    }

    private Result landmarkView(Kind kind, int chunkX, int chunkZ, String label) {
        com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan chunk = plan.chunk(chunkX, chunkZ);
        int ground = chunk.isCity() ? chunk.groundY() : chunk.naturalHeight();
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        return new Result(
            kind,
            chunkX,
            chunkZ,
            x,
            ground,
            z,
            x + 0.5,
            ground + LANDMARK_HEIGHT,
            z + LANDMARK_BACK + 0.5,
            180f,
            LANDMARK_PITCH,
            label);
    }

    /** Returns the yaw that looks from a building's middle toward its door on a side. */
    static float doorYaw(com.tuxprogrammer.lostcities.layout.Direction side) {
        switch (side) {
            case XMIN:
                return 90f;
            case XMAX:
                return 270f;
            case ZMIN:
                return 180f;
            default:
                return 0f;
        }
    }

    /**
     * Returns the yaw that looks from one point toward another (Minecraft's: 0 south, 90 west, 180 north, 270 east).
     *
     * @param fromX the viewer's X
     * @param fromZ the viewer's Z
     * @param toX   the target's X
     * @param toZ   the target's Z
     * @return degrees, 0 to 360
     */
    static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        double degrees = Math.toDegrees(Math.atan2(-(toX - fromX), toZ - fromZ));
        return (float) ((degrees + 360.0) % 360.0);
    }
}
