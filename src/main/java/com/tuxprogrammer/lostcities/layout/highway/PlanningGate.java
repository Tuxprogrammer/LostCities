package com.tuxprogrammer.lostcities.layout.highway;

/**
 * Lets the world glue decide when an expensive planner value is computed: a hub cell reads up to 64 city potentials
 * and their heights. Before the planner computes a hub it is missing, it calls {@link #enter} with the computation;
 * the gate may return, and the planner computes inline, or throw, abandoning the caller's computation. The world's
 * plan prefetcher throws while it probes, then runs the computation a step at a time inside its tick budget
 * (new-spec 06 P06-8, 0005 §8, B4).
 *
 * <p>
 * Determinism: a throw only abandons a computation; every planner cache stores a value only once it is complete, so
 * nothing the gate does changes a result. Side: both.
 */
@FunctionalInterface
public interface PlanningGate {

    /** Always computes inline. */
    PlanningGate NONE = compute -> {};

    /**
     * Called before a missing hub cell is computed.
     *
     * @param compute computes and caches that hub; safe to run later, any number of times
     */
    void enter(Runnable compute);
}
