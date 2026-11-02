package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/**
 * The finder's {@code best} mode for the landmark shots of a review set (decision log D124 and its follow-ups): of
 * every village gate, reserved lot or soft edge within a radius, the one that shows the sewing most clearly, with a
 * viewpoint that frames it.
 *
 * <ul>
 * <li>Candidates come from the structure oracle's landmarks, not a scan of every chunk, so a search 200 chunks out
 * asks the layout for a few chunks around each landmark only.</li>
 * <li>Score, lower is better: the relief of the ground around the target (city ground in city chunks, the oracle
 * height elsewhere), plus how far the city's ground and the landmark's lie apart; a soft edge also prefers a step a
 * strip visibly ramps (3 to 10 blocks). Ties go to the nearer.</li>
 * <li>Views stand beside what they show, above the highest ground on the line of sight plus room for trees, and look
 * down at the target: a village gate from the side, so the city lies on one half of the frame, the village on the
 * other and the lane between; a reserved lot from inside the city across its edge ring; a soft edge along the
 * strip, so its ramp shows in profile.</li>
 * </ul>
 *
 * <p>
 * Pure: reads plans and the oracle, never blocks. Side: both (plain Java).
 */
final class LandmarkViews {

    /** Chunks around a target whose ground counts toward its relief. */
    static final int RELIEF_RADIUS = 2;
    /** How far beside a village gate the view stands, in blocks. */
    static final int GATE_SIDE = 40;
    /** How far toward the city from the gate chunk's middle the gate view stands, in blocks. */
    static final int GATE_TOWARD_CITY = 8;
    /** How far into the city from the edge ring the reserved-lot view stands, in blocks. */
    static final int RESERVED_BACK = 44;
    /** How far along a soft edge the view stands from the strip's middle, in blocks. */
    static final int STRIP_ALONG = 26;
    /** How far into the city from the strip the soft-edge view stands, in blocks. */
    static final int STRIP_INWARD = 10;
    /** Height of a view above the highest ground on its line of sight, room for trees, in blocks. */
    static final int LIFT = 26;
    /** Height of a soft-edge view above the highest ground on its line of sight, in blocks. */
    static final int STRIP_LIFT = 16;
    /** Blocks a building's rooftop parts reach above its top floor. */
    static final int ROOF = 4;
    /** Points sampled on a line of sight. */
    static final int SIGHT_SAMPLES = 8;
    /** Height of a player's eyes above the feet. */
    static final double EYE = 1.62;
    /** Steps a soft edge's strip ramps visibly between, in blocks. */
    static final int STRIP_STEP_MIN = 3;
    /** See {@link #STRIP_STEP_MIN}. */
    static final int STRIP_STEP_MAX = 10;
    /** Score added to a gate whose chunk is outside a city (a village near a city, not sewn into one). */
    static final int OUTSIDE_CITY_PENALTY = 40;
    /** How far a gate's chunk may be stepped outward from the gate before it leaves the core (the layout's limit). */
    static final int MAX_GATE_STEPS = 8;

    private final LayoutPlanner planner;
    private final CityPlan plan;
    private final StructureOracle structures;

    LandmarkViews(LayoutPlanner planner, CityPlan plan, StructureOracle structures) {
        this.planner = planner;
        this.plan = plan;
        this.structures = structures;
    }

    /** A candidate view and its score. */
    private static final class Candidate {

        final CityFinder.Result result;
        final int score;
        final long distance;

        Candidate(CityFinder.Result result, int score, long distance) {
            this.result = result;
            this.score = score;
            this.distance = distance;
        }

        boolean beats(Candidate other) {
            return other == null || score < other.score
                || score == other.score
                    && (distance < other.distance || distance == other.distance && (result.z() < other.result.z()
                        || result.z() == other.result.z() && result.x() < other.result.x()));
        }
    }

    /**
     * Returns the clearest landmark view of a kind within a radius.
     *
     * @param kind   {@code SOFTEDGE}, {@code RESERVED} or {@code VILLAGE}
     * @param blockX the start's block X
     * @param blockZ the start's block Z
     * @param radius how far to look, in chunks
     * @return the best view, or {@code null} when the oracle knows no landmark that offers one
     */
    CityFinder.Result best(CityFinder.Kind kind, int blockX, int blockZ, int radius) {
        int startX = blockX >> 4;
        int startZ = blockZ >> 4;
        List<Landmark> landmarks = structures
            .landmarksTouching(startX - radius, startZ - radius, startX + radius, startZ + radius);
        if (kind == CityFinder.Kind.RESERVED) {
            // The reserved lot of the village whose gate shows best: a flat core can still be a forest of tall
            // trees, but a gate that shows its village from beside the lane promises a core that can be seen.
            Landmark shown = null;
            Candidate gate = null;
            for (Landmark landmark : landmarks) {
                Candidate found = village(landmark, blockX, blockZ, startX, startZ, radius);
                if (found != null && found.beats(gate)) {
                    gate = found;
                    shown = landmark;
                }
            }
            if (shown != null) {
                landmarks = Collections.singletonList(shown);
            }
        }
        Candidate best = null;
        for (Landmark landmark : landmarks) {
            Candidate found = switch (kind) {
                case VILLAGE -> village(landmark, blockX, blockZ, startX, startZ, radius);
                case RESERVED -> reserved(landmark, blockX, blockZ, startX, startZ, radius, false);
                case SOFTEDGE -> reserved(landmark, blockX, blockZ, startX, startZ, radius, true);
                default -> null;
            };
            if (found != null && found.beats(best)) {
                best = found;
            }
        }
        return best == null ? null : best.result;
    }

    private Candidate village(Landmark landmark, int blockX, int blockZ, int startX, int startZ, int radius) {
        if (landmark.kind() != LandmarkKind.VILLAGE) {
            return null;
        }
        Candidate best = null;
        for (Gate gate : landmark.gates()) {
            RoadDirection out = gate.facing();
            int x = gate.chunkX();
            int z = gate.chunkZ();
            for (int step = 0; step < MAX_GATE_STEPS && isCoreOf(x, z, landmark); step++) {
                x += out.stepX();
                z += out.stepZ();
            }
            if (!within(x, z, startX, startZ, radius) || planner.plan(x, z)
                .lane()
                .role() != LaneRole.GATE) {
                continue;
            }
            ChunkPlan gateChunk = plan.chunk(x, z);
            int ground = surface(x, z);
            int score = relief(x, z) + Math.abs(ground - landmark.groundEstimate())
                + (gateChunk.isCity() ? 0 : OUTSIDE_CITY_PENALTY);
            int targetX = (x << 4) + 8;
            int targetZ = (z << 4) + 8;
            // Beside the lane: the city on one half of the frame, the village on the other.
            double towardX = out.stepX() * GATE_TOWARD_CITY;
            double towardZ = out.stepZ() * GATE_TOWARD_CITY;
            CityFinder.Result view = null;
            for (int sign = -1; sign <= 1; sign += 2) {
                double viewX = targetX + 0.5 + towardX - out.stepZ() * sign * GATE_SIDE;
                double viewZ = targetZ + 0.5 + towardZ + out.stepX() * sign * GATE_SIDE;
                CityFinder.Result candidate = look(
                    CityFinder.Kind.VILLAGE,
                    x,
                    z,
                    targetX,
                    ground,
                    targetZ,
                    viewX,
                    viewZ,
                    LIFT,
                    String.format(
                        Locale.ROOT,
                        "gate of %s facing %s, %s, relief %d, ground %d to the village's %d",
                        landmark.id(),
                        out.name(),
                        gateChunk.isCity() ? "in the city" : "outside a city",
                        relief(x, z),
                        ground,
                        landmark.groundEstimate()));
                if (view == null || candidate.viewY() < view.viewY()) {
                    view = candidate;
                }
            }
            Candidate found = new Candidate(view, score, distance(view, blockX, blockZ));
            if (found.beats(best)) {
                best = found;
            }
        }
        return best;
    }

    /**
     * Returns a landmark's best reserved-lot or soft-edge view: a city chunk of its edge ring beside one of its core
     * chunks.
     */
    private Candidate reserved(Landmark landmark, int blockX, int blockZ, int startX, int startZ, int radius,
        boolean strip) {
        Candidate best = null;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockRect rect : landmark.footprint()) {
            minX = Math.min(minX, rect.minX() >> 4);
            minZ = Math.min(minZ, rect.minZ() >> 4);
            maxX = Math.max(maxX, rect.maxX() >> 4);
            maxZ = Math.max(maxZ, rect.maxZ() >> 4);
        }
        for (int cz = minZ - 1; cz <= maxZ + 1; cz++) {
            for (int cx = minX - 1; cx <= maxX + 1; cx++) {
                if (!isCoreOf(cx, cz, landmark)) {
                    continue;
                }
                for (RoadDirection side : RoadDirection.values()) {
                    // The city chunk E beside core chunk C, and the direction from E to C.
                    int ex = cx - side.stepX();
                    int ez = cz - side.stepZ();
                    if (!within(ex, ez, startX, startZ, radius) || !plan.chunk(ex, ez)
                        .isCity()) {
                        continue;
                    }
                    Candidate found = strip ? softEdge(landmark, ex, ez, cx, cz, side, blockX, blockZ)
                        : lot(landmark, ex, ez, cx, cz, side, blockX, blockZ);
                    if (found.beats(best)) {
                        best = found;
                    }
                }
            }
        }
        return best;
    }

    private Candidate lot(Landmark landmark, int ex, int ez, int cx, int cz, RoadDirection side, int blockX,
        int blockZ) {
        int ground = surface(ex, ez);
        int score = relief(cx, cz) + Math.abs(ground - landmark.groundEstimate());
        int targetX = (cx << 4) + 8;
        int targetZ = (cz << 4) + 8;
        // From inside the city, across the edge ring's park to the core.
        double viewX = targetX + 0.5 - side.stepX() * RESERVED_BACK;
        double viewZ = targetZ + 0.5 - side.stepZ() * RESERVED_BACK;
        CityFinder.Result view = look(
            CityFinder.Kind.RESERVED,
            cx,
            cz,
            targetX,
            surface(cx, cz),
            targetZ,
            viewX,
            viewZ,
            LIFT,
            String.format(
                Locale.ROOT,
                "landmark core %s seen across its edge ring from %s, relief %d",
                landmark.id(),
                opposite(side),
                relief(cx, cz)));
        return new Candidate(view, score, distance(view, blockX, blockZ));
    }

    private Candidate softEdge(Landmark landmark, int ex, int ez, int cx, int cz, RoadDirection side, int blockX,
        int blockZ) {
        int ground = surface(ex, ez);
        int step = Math.abs(ground - surface(cx, cz));
        int score = relief(cx, cz) + Math.max(0, STRIP_STEP_MIN - step) * 2 + Math.max(0, step - STRIP_STEP_MAX);
        // The strip's middle, on the city chunk's side that faces the core.
        int targetX = (ex << 4) + 8 + side.stepX() * 7;
        int targetZ = (ez << 4) + 8 + side.stepZ() * 7;
        CityFinder.Result view = null;
        for (int sign = -1; sign <= 1; sign += 2) {
            double viewX = targetX + 0.5 - side.stepZ() * sign * STRIP_ALONG - side.stepX() * STRIP_INWARD;
            double viewZ = targetZ + 0.5 + side.stepX() * sign * STRIP_ALONG - side.stepZ() * STRIP_INWARD;
            CityFinder.Result candidate = look(
                CityFinder.Kind.SOFTEDGE,
                ex,
                ez,
                targetX,
                ground,
                targetZ,
                viewX,
                viewZ,
                STRIP_LIFT,
                String.format(
                    Locale.ROOT,
                    "soft edge toward %s (%s), step %d, relief %d",
                    side.name(),
                    landmark.id(),
                    step,
                    relief(cx, cz)));
            if (view == null || candidate.viewY() < view.viewY()) {
                view = candidate;
            }
        }
        return new Candidate(view, score, distance(view, blockX, blockZ));
    }

    /**
     * Builds a view from a point toward a target: above the highest ground or roof on the line of sight by
     * {@code lift}, the yaw and pitch that center the target.
     */
    private CityFinder.Result look(CityFinder.Kind kind, int chunkX, int chunkZ, int targetX, int targetY, int targetZ,
        double viewX, double viewZ, int lift, String label) {
        int highest = targetY;
        for (int i = 0; i <= SIGHT_SAMPLES; i++) {
            double t = i / (double) SIGHT_SAMPLES;
            int x = (int) Math.floor(viewX + (targetX - viewX) * t);
            int z = (int) Math.floor(viewZ + (targetZ - viewZ) * t);
            highest = Math.max(highest, obstacle(x >> 4, z >> 4));
        }
        double viewY = highest + lift;
        double horizontal = Math.hypot(targetX + 0.5 - viewX, targetZ + 0.5 - viewZ);
        float pitch = (float) Math.toDegrees(Math.atan2(viewY + EYE - (targetY + 1), horizontal));
        return new CityFinder.Result(
            kind,
            chunkX,
            chunkZ,
            targetX,
            targetY,
            targetZ,
            viewX,
            viewY,
            viewZ,
            Math.round(CityFinder.yawToward(viewX, viewZ, targetX + 0.5, targetZ + 0.5)),
            Math.round(pitch),
            label);
    }

    /** Returns the ground a chunk shows: the city ground in a city chunk, the oracle height elsewhere. */
    int surface(int chunkX, int chunkZ) {
        ChunkPlan chunk = plan.chunk(chunkX, chunkZ);
        return chunk.isCity() ? chunk.groundY() : chunk.naturalHeight();
    }

    /** Returns the top of what stands in a chunk: a building's roof (and its rooftop parts), else the ground. */
    int obstacle(int chunkX, int chunkZ) {
        ChunkPlan chunk = plan.chunk(chunkX, chunkZ);
        return chunk.isCity() ? chunk.maxHeight() + ROOF : chunk.naturalHeight();
    }

    /** Returns the difference between the highest and lowest ground within {@link #RELIEF_RADIUS} of a chunk. */
    int relief(int chunkX, int chunkZ) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int dz = -RELIEF_RADIUS; dz <= RELIEF_RADIUS; dz++) {
            for (int dx = -RELIEF_RADIUS; dx <= RELIEF_RADIUS; dx++) {
                int height = surface(chunkX + dx, chunkZ + dz);
                low = Math.min(low, height);
                high = Math.max(high, height);
            }
        }
        return high - low;
    }

    private boolean isCoreOf(int chunkX, int chunkZ, Landmark landmark) {
        ZoneInfo zone = planner.zone(chunkX, chunkZ);
        return zone.zone() == Zone.CORE && landmark.id()
            .equals(zone.landmarkId());
    }

    private static boolean within(int chunkX, int chunkZ, int startX, int startZ, int radius) {
        return Math.max(Math.abs(chunkX - startX), Math.abs(chunkZ - startZ)) <= radius;
    }

    private static long distance(CityFinder.Result result, int blockX, int blockZ) {
        long dx = result.x() - blockX;
        long dz = result.z() - blockZ;
        return dx * dx + dz * dz;
    }

    private static String opposite(RoadDirection side) {
        return switch (side) {
            case NORTH -> "the south";
            case SOUTH -> "the north";
            case WEST -> "the east";
            default -> "the west";
        };
    }
}
