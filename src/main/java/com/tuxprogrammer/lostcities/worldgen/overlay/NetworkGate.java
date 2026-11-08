package com.tuxprogrammer.lostcities.worldgen.overlay;

import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;

/**
 * Schedules the highway planner's hub cells through the plan prefetcher (new-spec 06 P06-8, 0005 §8, B4). A hub
 * cell samples up to 64 city potentials, each of which may need a height, so computing one inside a chunk's phase
 * could cost tens of milliseconds. While the prefetcher probes a plan, a missing hub is a miss like a missing height:
 * the gate throws the probe's exception with a step that computes the hub with the probe on and, when that stops at
 * a height not yet cached, fetches that one height. Each step costs at most one potential sweep and one height, so it
 * fits the prefetcher's tick; the prefetcher asks again until the hub is cached. Outside a probe the hub is computed
 * inline, as before.
 *
 * <p>
 * Nothing it does changes a result: a hub is pure, and the planner caches it only once complete. One per world.
 * Server thread only. Side: server.
 */
final class NetworkGate implements PlanningGate {

    private final HeightProbe probe;
    private boolean stepping;

    /**
     * Creates the gate of one world.
     *
     * @param probe the world's probe, shared with its height caches
     */
    NetworkGate(HeightProbe probe) {
        this.probe = probe;
    }

    @Override
    public void enter(Runnable compute) {
        if (!probe.active() || stepping) {
            return;
        }
        throw probe.miss(() -> step(compute));
    }

    /**
     * A city link's route (D-perf2-worker-1): while a probe is active, the plan is abandoned with a step that advances
     * the route by one bounded step ({@code CityLinkGraph.STEP_WORK}) with the probe on, or, when the step stops at an
     * input not yet cached before any progress, fetches that one input. So each worker unit holds the plan lock for one
     * route step or one fetch, never a whole route; the prefetcher asks again until the route is cached.
     */
    @Override
    public void enterSteps(java.util.function.BooleanSupplier step) {
        if (!probe.active() || stepping) {
            return;
        }
        throw probe.miss(() -> step(step::getAsBoolean));
    }

    /** Computes the hub with the probe on; a height it misses is fetched, and the next step tries again. */
    private void step(Runnable compute) {
        stepping = true;
        probe.setActive(true);
        try {
            compute.run();
        } catch (HeightProbe.NotCached missing) {
            probe.setActive(false);
            probe.fetchMissing();
        } finally {
            stepping = false;
            probe.setActive(false);
        }
    }
}
