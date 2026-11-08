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

    /**
     * Called before a missing value that can be computed a bounded step at a time is computed inline: a city link's
     * route (D-perf2-worker-1). The gate may return, and the caller finishes the computation inline (continuing from
     * any steps already run), or throw, abandoning the caller's computation; it then runs {@code step} later, any
     * number of times. The default returns: always inline.
     *
     * @param step advances the computation by a bounded amount and returns {@code true} once the value is cached; a
     *             miss of an input it reads is thrown only from a call that made no progress, so a gate that fetches
     *             that input and steps again always gets further
     */
    default void enterSteps(java.util.function.BooleanSupplier step) {}
}
