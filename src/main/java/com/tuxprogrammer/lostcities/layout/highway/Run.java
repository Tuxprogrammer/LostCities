package com.tuxprogrammer.lostcities.layout.highway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One continuous stretch of a route: its segments in order from its start to its end, and what lies at each end, a
 * hub or a dead end before an obstacle. A route is one run, or several where a landmark's core and margin cut it
 * (new-spec 06, "R: route geometry" (3)).
 *
 * <p>
 * A run also knows its level profile (deviation, alpha review AR-10, design record 0013): the highway reaches each end
 * at that end's own level, a hub's city level or the ground's level at a dead end, by ramps of one city level per
 * chunk next to it, and keeps the route's level between. Ramps never fall on a corner or an end chunk, which stay
 * flat, so bends, junctions and plazas keep their parts. Upstream keeps a route's level from end to end, so a highway
 * could end in the air or inside a hill.
 *
 * <p>
 * Indices count chunks along the run: 0 is the start chunk, {@link #length()} the end chunk. Boundary {@code j}, for
 * {@code j} from 1 to the length, lies between chunks {@code j − 1} and {@code j}. Side: both.
 */
public final class Run {

    /** What lies at one end of a run. */
    public enum EndKind {
        /** A hub: the city the route connects. */
        HUB,
        /** A dead end one chunk before a landmark's core and margin that no detour gets around. */
        DEAD_END
    }

    /** One end of a run: its chunk, its kind and the city level the highway arrives at there. */
    public static final class End {

        private final EndKind kind;
        private final int chunkX;
        private final int chunkZ;
        private final int level;

        /**
         * Creates an end.
         *
         * @param kind   hub or dead end
         * @param chunkX its chunk X
         * @param chunkZ its chunk Z
         * @param level  the city level the highway arrives at: the hub's, or the ground's at a dead end
         */
        public End(EndKind kind, int chunkX, int chunkZ, int level) {
            this.kind = kind;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.level = Math.max(0, level);
        }

        /** Returns the kind. */
        public EndKind kind() {
            return kind;
        }

        /** Returns its chunk X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns its chunk Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the city level the highway arrives at. */
        public int level() {
            return level;
        }

        @Override
        public String toString() {
            return kind + "@" + chunkX + "," + chunkZ + " level " + level;
        }
    }

    private final List<Segment> segments;
    private final End start;
    private final End end;
    private final int length;
    private final int[] corners;

    /**
     * Creates a run.
     *
     * @param segments its segments in order, each starting where the one before ended; at least one
     * @param start    its start, on the first segment's first chunk
     * @param end      its end, on the last segment's last chunk
     */
    public Run(List<Segment> segments, End start, End end) {
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("A run needs a segment");
        }
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        this.start = start;
        this.end = end;
        int total = 0;
        int[] cornerIndex = new int[segments.size() - 1];
        for (int i = 0; i < segments.size(); i++) {
            total += segments.get(i)
                .length();
            if (i < segments.size() - 1) {
                cornerIndex[i] = total;
            }
        }
        this.length = total;
        this.corners = cornerIndex;
    }

    /** Returns the segments in order. */
    public List<Segment> segments() {
        return segments;
    }

    /** Returns the start. */
    public End start() {
        return start;
    }

    /** Returns the end. */
    public End end() {
        return end;
    }

    /** Returns its length: the steps from the start chunk to the end chunk. */
    public int length() {
        return length;
    }

    /**
     * Returns a chunk's index along the run, or −1 when it is not on it. A corner lies on two segments and has one
     * index.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the index, 0 to {@link #length()}, or −1
     */
    public int indexOf(int chunkX, int chunkZ) {
        int before = 0;
        for (Segment segment : segments) {
            if (segment.contains(chunkX, chunkZ)) {
                return before + segment.offsetOf(chunkX, chunkZ);
            }
            before += segment.length();
        }
        return -1;
    }

    /** Returns whether a chunk lies on the run. */
    public boolean contains(int chunkX, int chunkZ) {
        for (Segment segment : segments) {
            if (segment.contains(chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether an index is a corner, where one segment turns into the next. */
    public boolean isCorner(int index) {
        for (int corner : corners) {
            if (corner == index) {
                return true;
            }
        }
        return false;
    }

    /** Returns the number of chunks that may hold a ramp: neither an end nor a corner. */
    int rampChunks() {
        return rampChunks(NO_HOLDS);
    }

    /** No held chunks: the profile as before {@code highwayRampHolds}. */
    static final int[] NO_HOLDS = new int[0];

    /**
     * Returns the number of chunks that may hold a ramp: neither an end, a corner nor a held chunk.
     *
     * @param holds indices kept flat for other routes (see {@link #holds}), each strictly between the ends and no
     *              corner of this run
     */
    int rampChunks(int[] holds) {
        return Math.max(0, length - 1 - corners.length - holds.length);
    }

    /**
     * Returns the indices of this run, sorted and distinct, among the given ones that it may keep flat for another
     * route: strictly between its ends and not its own corner ({@code highwayRampHolds}, 0013 section 2).
     *
     * @param indices candidate indices, in any order, out-of-range ones allowed
     * @return the held indices
     */
    public int[] holds(int[] indices) {
        java.util.TreeSet<Integer> out = new java.util.TreeSet<>();
        for (int index : indices) {
            if (index > 0 && index < length && !isCorner(index)) {
                out.add(index);
            }
        }
        int[] held = new int[out.size()];
        int i = 0;
        for (int index : out) {
            held[i++] = index;
        }
        return held;
    }

    /** Returns the number of corners with an index of at most {@code index}. */
    private int cornersUpTo(int index) {
        int count = 0;
        for (int corner : corners) {
            if (corner <= index) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the end level the profile can reach: the end's own level, or as near it as the run's ramp chunks allow
     * from the start's level when the run is too short to make up the difference.
     */
    int reachableEndLevel() {
        return reachableEndLevel(NO_HOLDS);
    }

    private int reachableEndLevel(int[] holds) {
        int ramps = rampChunks(holds);
        int difference = end.level - start.level;
        if (Math.abs(difference) <= ramps) {
            return end.level;
        }
        return start.level + Integer.signum(difference) * ramps;
    }

    /**
     * Returns the deck's city level at a boundary: the cruise level, held within the reach of each end's level by one
     * level per ramp chunk.
     *
     * @param boundary the boundary, 1 to {@link #length()}
     * @param cruise   the route's level
     * @return the level
     */
    public int boundaryLevel(int boundary, int cruise) {
        return boundaryLevel(boundary, cruise, NO_HOLDS);
    }

    /**
     * Returns the deck's city level at a boundary, with some chunks held flat: a held chunk takes no ramp, as a corner.
     *
     * @param boundary the boundary, 1 to {@link #length()}
     * @param cruise   the route's level
     * @param holds    held indices, from {@link #holds}
     * @return the level
     */
    public int boundaryLevel(int boundary, int cruise, int[] holds) {
        int fromStart = (boundary - 1) - cornersUpTo(boundary - 1) - heldUpTo(holds, boundary - 1);
        int toEnd = rampChunks(holds) - fromStart;
        int startLevel = start.level;
        int endLevel = reachableEndLevel(holds);
        int low = Math.max(startLevel - fromStart, endLevel - toEnd);
        int high = Math.min(startLevel + fromStart, endLevel + toEnd);
        return Math.max(low, Math.min(high, cruise));
    }

    /**
     * Returns the deck's level where a chunk is entered from the start's side.
     *
     * @param index  the chunk's index
     * @param cruise the route's level
     * @return the level
     */
    public int entryLevel(int index, int cruise) {
        return entryLevel(index, cruise, NO_HOLDS);
    }

    /** As {@link #entryLevel(int, int)}, with held chunks ({@link #holds}). */
    public int entryLevel(int index, int cruise, int[] holds) {
        if (length == 0) {
            return start.level;
        }
        return boundaryLevel(Math.max(1, index), cruise, holds);
    }

    private static int heldUpTo(int[] holds, int index) {
        int count = 0;
        for (int held : holds) {
            if (held <= index) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the deck's level where a chunk is left toward the end's side.
     *
     * @param index  the chunk's index
     * @param cruise the route's level
     * @return the level
     */
    public int exitLevel(int index, int cruise) {
        return exitLevel(index, cruise, NO_HOLDS);
    }

    /** As {@link #exitLevel(int, int)}, with held chunks ({@link #holds}). */
    public int exitLevel(int index, int cruise, int[] holds) {
        if (length == 0) {
            return start.level;
        }
        return boundaryLevel(Math.min(length, index + 1), cruise, holds);
    }

    @Override
    public String toString() {
        return "run " + start + " " + segments + " " + end;
    }
}
