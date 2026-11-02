package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/**
 * The columns a village lane crosses in one chunk, from the layout's sewing (0006 §3, docs/new-spec/03-terrain/
 * pipeline-on-rwg.md "Village lanes"): a path {@value #WIDTH} blocks wide.
 * <ul>
 * <li>In a link chunk (a village outside a city) the path runs from the chunk's middle to each side it connects
 * to.</li>
 * <li>In a gate chunk, the margin chunk in front of the gate, it does the same, except that toward the core it runs in
 * line with the gate, with a jog along the chunk's middle to join the rest; beside a city it runs down (or up) the
 * margin's slope from the city's street to the core, graded and stepped by the terrain phase (AR-7, AR-11).</li>
 * <li>In a core chunk it runs from the gate straight out to the core's edge, so it never comes nearer the village's
 * pieces than the gate itself.</li>
 * </ul>
 * Lane chunks inside a city are tertiary roads (D50); their street is the lane, so they get no path.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class LaneShapes {

    /** Width of a lane, in blocks: VillageNames' paths are 3 wide. */
    public static final int WIDTH = 3;
    /** The column a lane's middle runs along in a link chunk. */
    static final int MIDDLE = 8;
    /** Most chunks a gate's path may cross inside a core before it is given up (layout's gate step bound). */
    static final int MAX_CORE_STEPS = 8 * 16;

    private LaneShapes() {}

    /**
     * Returns the path of a gate or link chunk.
     *
     * @param cell   the chunk's sewing cell
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return 256 flags, index {@code z × 16 + x}; {@code null} when the chunk has no path
     */
    public static boolean[] forCell(LaneCell cell, int chunkX, int chunkZ) {
        if (cell.role() != LaneRole.GATE && cell.role() != LaneRole.LINK) {
            return null;
        }
        boolean[] columns = new boolean[256];
        Gate gate = cell.gate();
        RoadDirection toCore = gate == null ? null
            : gate.facing()
                .opposite();
        for (RoadDirection direction : RoadDirection.values()) {
            if (!cell.connects(direction)) {
                continue;
            }
            int line = MIDDLE;
            if (direction == toCore) {
                // In line with the gate, then along the middle to join the other connections.
                line = clampLine(direction.isNorthSouth() ? gate.x() - (chunkX << 4) : gate.z() - (chunkZ << 4));
                jog(columns, direction, line);
            }
            ray(columns, direction, line);
        }
        return columns;
    }

    /**
     * Returns the path inside a core chunk: from each gate whose lane the layout laid, straight outward to the first
     * chunk that is not core.
     *
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param landmarks the landmarks near the chunk
     * @param margin    the profile's {@code landmarkMargin}, blocks
     * @param sewn      tells whether a gate's lane was laid (its gate chunk carries it)
     * @return 256 flags; {@code null} when no path crosses the chunk
     */
    public static boolean[] forCore(int chunkX, int chunkZ, List<Landmark> landmarks, int margin, GateCheck sewn) {
        boolean[] columns = null;
        for (Landmark landmark : landmarks) {
            for (Gate gate : landmark.gates()) {
                columns = coreRun(columns, chunkX, chunkZ, landmark, gate, margin, sewn);
            }
        }
        return columns;
    }

    /** Tells whether a gate's lane was laid. */
    @FunctionalInterface
    public interface GateCheck {

        /**
         * Returns whether the lane of a gate was laid.
         *
         * @param gate       the gate
         * @param gateChunkX the gate chunk's X: the first chunk outside the core along the gate's facing
         * @param gateChunkZ the gate chunk's Z
         * @return whether the layout laid it
         */
        boolean laid(Gate gate, int gateChunkX, int gateChunkZ);
    }

    private static boolean[] coreRun(boolean[] columns, int chunkX, int chunkZ, Landmark landmark, Gate gate,
        int margin, GateCheck sewn) {
        RoadDirection out = gate.facing();
        int x = gate.x();
        int z = gate.z();
        int steps = 0;
        while (landmark.coreDistance(x >> 4, z >> 4, margin) == 0) {
            if (++steps > MAX_CORE_STEPS) {
                return columns;
            }
            x += out.stepX();
            z += out.stepZ();
        }
        if (!sewn.laid(gate, x >> 4, z >> 4)) {
            return columns;
        }
        boolean[] out2 = columns;
        int bx = gate.x();
        int bz = gate.z();
        while (bx != x || bz != z) {
            if (bx >> 4 == chunkX && bz >> 4 == chunkZ) {
                if (out2 == null) {
                    out2 = new boolean[256];
                }
                for (int offset = -WIDTH / 2; offset <= WIDTH / 2; offset++) {
                    int px = (bx & 15) + (out.isNorthSouth() ? offset : 0);
                    int pz = (bz & 15) + (out.isNorthSouth() ? 0 : offset);
                    if (px >= 0 && px < 16 && pz >= 0 && pz < 16) {
                        out2[(pz << 4) | px] = true;
                    }
                }
            }
            bx += out.stepX();
            bz += out.stepZ();
        }
        return out2;
    }

    /**
     * Returns the lane path of a chunk's columns, or {@code null}.
     *
     * @param columns  the flags, or {@code null}
     * @param material the village's material
     * @return the path
     */
    public static LanePath path(boolean[] columns, LanePath.Material material) {
        return columns == null ? null : LanePath.of(columns, material);
    }

    /** A band from the line's middle point to the chunk's edge in one direction. */
    private static void ray(boolean[] columns, RoadDirection direction, int line) {
        for (int step = 0; step < 16; step++) {
            int along = direction.stepX() + direction.stepZ() > 0 ? MIDDLE + step : MIDDLE - step;
            if (along < 0 || along > 15) {
                break;
            }
            for (int offset = -WIDTH / 2; offset <= WIDTH / 2; offset++) {
                int across = line + offset;
                if (across < 0 || across > 15) {
                    continue;
                }
                int x = direction.isNorthSouth() ? across : along;
                int z = direction.isNorthSouth() ? along : across;
                columns[(z << 4) | x] = true;
            }
        }
    }

    /** A band along the chunk's middle from {@link #MIDDLE} to an offset line, joining it to the other rays. */
    private static void jog(boolean[] columns, RoadDirection direction, int line) {
        int from = Math.min(line, MIDDLE);
        int to = Math.max(line, MIDDLE);
        for (int across = from; across <= to; across++) {
            for (int offset = -WIDTH / 2; offset <= WIDTH / 2; offset++) {
                int along = MIDDLE + offset;
                int x = direction.isNorthSouth() ? across : along;
                int z = direction.isNorthSouth() ? along : across;
                columns[(z << 4) | x] = true;
            }
        }
    }

    private static int clampLine(int local) {
        return Math.max(WIDTH / 2, Math.min(15 - WIDTH / 2, local));
    }
}
