package com.tuxprogrammer.lostcities.layout.highway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An accepted connection's highway: its two hubs, its shape (straight or one L), its level from the hubs' city levels,
 * and its runs, one from hub to hub, or several where a landmark's core and margin cut it; a detour around one is part
 * of a run (new-spec 06, "R: route geometry"). Immutable.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md (step R). Side: both.
 */
public final class Route {

    /** Upstream's route shapes. */
    public enum Shape {
        /** The hubs share a row or a column. */
        STRAIGHT,
        /** Along the first hub's row, then down the second hub's column. */
        HORIZONTAL_THEN_VERTICAL,
        /** Down the first hub's column, then along the second hub's row. */
        VERTICAL_THEN_HORIZONTAL
    }

    private final ConnectionKey key;
    private final Hub first;
    private final Hub second;
    private final int level;
    private final Shape shape;
    private final int length;
    private final long penalty;
    private final List<Run> runs;
    private final int detours;

    /**
     * Creates a route.
     *
     * @param key     its connection
     * @param first   the hub of the key's first cell
     * @param second  the hub of the key's second cell
     * @param level   its city level from the hubs (before overlapping routes share theirs)
     * @param shape   its shape
     * @param length  the Manhattan distance between the hubs
     * @param penalty the chosen shape's city penalty (0 when not computed)
     * @param runs    its runs; empty when an obstacle sits right beside both hubs
     * @param detours how many detours its runs take around obstacles
     */
    public Route(ConnectionKey key, Hub first, Hub second, int level, Shape shape, int length, long penalty,
        List<Run> runs, int detours) {
        this.key = key;
        this.first = first;
        this.second = second;
        this.level = level;
        this.shape = shape;
        this.length = length;
        this.penalty = penalty;
        this.runs = Collections.unmodifiableList(new ArrayList<>(runs));
        this.detours = detours;
    }

    /** Returns its connection. */
    public ConnectionKey key() {
        return key;
    }

    /** Returns the first cell's hub. */
    public Hub first() {
        return first;
    }

    /** Returns the second cell's hub. */
    public Hub second() {
        return second;
    }

    /** Returns its own city level, before overlapping routes share their lowest. */
    public int level() {
        return level;
    }

    /** Returns its shape. */
    public Shape shape() {
        return shape;
    }

    /** Returns the Manhattan distance between the hubs. */
    public int length() {
        return length;
    }

    /** Returns the chosen shape's city penalty, for the debug output. */
    public long penalty() {
        return penalty;
    }

    /** Returns its runs. */
    public List<Run> runs() {
        return runs;
    }

    /** Returns how many detours it takes. */
    public int detours() {
        return detours;
    }

    /** Returns how many dead ends cut it: one fewer than its runs, or 1 when an obstacle cut it to nothing. */
    public int deadEnds() {
        return runs.isEmpty() ? 1 : runs.size() - 1;
    }

    /** Returns every segment of every run. */
    public List<Segment> segments() {
        List<Segment> out = new ArrayList<>();
        for (Run run : runs) {
            out.addAll(run.segments());
        }
        return out;
    }

    /**
     * Returns whether a chunk lies on the route.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether a run passes through it
     */
    public boolean contains(int chunkX, int chunkZ) {
        for (Run run : runs) {
            if (run.contains(chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "route " + key + " " + shape + " level " + level + " " + runs;
    }
}
