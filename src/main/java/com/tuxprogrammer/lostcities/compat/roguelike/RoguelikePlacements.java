package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import com.tuxprogrammer.lostcities.core.DevEnvironment;

/**
 * What the steering mixin saw, for the {@code oracle.roguelike} scenario: each site Roguelike's test accepted (the
 * point its dungeon is built at), each point its test rejected and each point the steering refused, with the dungeon
 * chunk's origin they belong to.
 * Roguelike saves nothing that would let another mod find its dungeons, so this is how the scenario learns where they
 * went.
 *
 * <p>
 * Test instrumentation: it records only while the test hooks are on ({@link DevEnvironment#testHooksEnabled()}), keeps
 * at most {@value #CAPACITY} events (the oldest dropped first), and generation never reads it, so it cannot change a
 * world. The scenario clears it before it generates.
 *
 * <p>
 * Server thread; synchronized. Side: server.
 */
public final class RoguelikePlacements {

    /** Most events kept. */
    public static final int CAPACITY = 4096;

    /** One event. */
    public static final class Event {

        private final int dimension;
        private final int originX;
        private final int originZ;
        private final int x;
        private final int z;
        private final SteeringRule.Outcome outcome;
        private final boolean accepted;

        Event(int dimension, int originX, int originZ, int x, int z, SteeringRule.Outcome outcome, boolean accepted) {
            this.dimension = dimension;
            this.originX = originX;
            this.originZ = originZ;
            this.x = x;
            this.z = z;
            this.outcome = outcome;
            this.accepted = accepted;
        }

        /** Returns the dimension ID. */
        public int dimension() {
            return dimension;
        }

        /** Returns the block X {@code generateNear} started from (the dungeon chunk's X × 16 + 4). */
        public int originX() {
            return originX;
        }

        /** Returns the block Z {@code generateNear} started from. */
        public int originZ() {
            return originZ;
        }

        /** Returns the point's block X. */
        public int x() {
            return x;
        }

        /** Returns the point's block Z. */
        public int z() {
            return z;
        }

        /** Returns the steering outcome that refused the point, or {@code null} when the steering let it through. */
        public SteeringRule.Outcome refusedBy() {
            return outcome;
        }

        /** Returns whether Roguelike accepted the point: its dungeon stands there. */
        public boolean accepted() {
            return accepted;
        }

        @Override
        public String toString() {
            String what = accepted ? "site " : outcome != null ? "refused(" + outcome + ") " : "rejected ";
            return what + x + "," + z + " from " + originX + "," + originZ;
        }
    }

    private static final Deque<Event> EVENTS = new ArrayDeque<>();

    private RoguelikePlacements() {}

    /**
     * Records the site Roguelike accepted.
     *
     * @param dimension the dimension ID
     * @param originX   the block X the dungeon searched from
     * @param originZ   the block Z the dungeon searched from
     * @param x         the site's block X
     * @param z         the site's block Z
     */
    public static void accepted(int dimension, int originX, int originZ, int x, int z) {
        add(new Event(dimension, originX, originZ, x, z, null, true));
    }

    /**
     * Records a point the steering let through and Roguelike's own test rejected.
     *
     * @param dimension the dimension ID
     * @param originX   the block X the dungeon searched from
     * @param originZ   the block Z the dungeon searched from
     * @param x         the point's block X
     * @param z         the point's block Z
     */
    public static void rejected(int dimension, int originX, int originZ, int x, int z) {
        add(new Event(dimension, originX, originZ, x, z, null, false));
    }

    /**
     * Records a point the steering refused.
     *
     * @param dimension the dimension ID
     * @param originX   the block X the dungeon searched from
     * @param originZ   the block Z the dungeon searched from
     * @param x         the point's block X
     * @param z         the point's block Z
     * @param outcome   why
     */
    public static void refused(int dimension, int originX, int originZ, int x, int z, SteeringRule.Outcome outcome) {
        add(new Event(dimension, originX, originZ, x, z, outcome, false));
    }

    private static synchronized void add(Event event) {
        if (!DevEnvironment.testHooksEnabled()) {
            return;
        }
        EVENTS.addLast(event);
        while (EVENTS.size() > CAPACITY) {
            EVENTS.removeFirst();
        }
    }

    /** Returns the events, oldest first. */
    public static synchronized List<Event> events() {
        return Collections.unmodifiableList(new ArrayList<>(EVENTS));
    }

    /** Forgets every event. */
    public static synchronized void clear() {
        EVENTS.clear();
    }
}
