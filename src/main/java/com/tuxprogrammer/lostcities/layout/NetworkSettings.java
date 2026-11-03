package com.tuxprogrammer.lostcities.layout;

import com.tuxprogrammer.lostcities.layout.highway.HighwaySettings;
import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;
import com.tuxprogrammer.lostcities.layout.rail.RailwaySettings;

/**
 * What a world's highway and railway networks are built from (M06): the pinned profile's highway and railway
 * settings, with the module switches folded in, and the gate that schedules the highway planner's hub cells. Given to
 * {@link LayoutInputs}, it makes the layout build its own {@link Networks}.
 *
 * <p>
 * Side: both.
 */
public final class NetworkSettings {

    /** Upstream's defaults: highways and railways on. */
    public static final NetworkSettings DEFAULTS = new NetworkSettings(
        HighwaySettings.DEFAULTS,
        RailwaySettings.DEFAULTS,
        PlanningGate.NONE);

    private final HighwaySettings highways;
    private final RailwaySettings railways;
    private final PlanningGate gate;
    private final boolean subways;

    /**
     * Creates the settings, with the cities' subways on.
     *
     * @param highways the highway settings
     * @param railways the railway settings
     * @param gate     schedules the highway planner's hub cells
     */
    public NetworkSettings(HighwaySettings highways, RailwaySettings railways, PlanningGate gate) {
        this(highways, railways, gate, true);
    }

    /**
     * Creates the settings.
     *
     * @param highways the highway settings
     * @param railways the railway settings
     * @param gate     schedules the highway planner's hub cells
     * @param subways  whether the cities' rail corridors under their streets generate: the {@code railways} module,
     *                 which upstream's corridors know nothing of
     */
    public NetworkSettings(HighwaySettings highways, RailwaySettings railways, PlanningGate gate, boolean subways) {
        this.highways = highways;
        this.railways = railways;
        this.gate = gate;
        this.subways = subways;
    }

    /** Returns the highway settings. */
    public HighwaySettings highways() {
        return highways;
    }

    /** Returns the railway settings. */
    public RailwaySettings railways() {
        return railways;
    }

    /** Returns whether the cities' subways (rail corridors under their streets) generate. */
    public boolean subways() {
        return subways;
    }

    /** Returns the gate. */
    public PlanningGate gate() {
        return gate;
    }

    /**
     * Returns these settings with another gate.
     *
     * @param newGate the gate
     * @return the settings
     */
    public NetworkSettings withGate(PlanningGate newGate) {
        return new NetworkSettings(highways, railways, newGate, subways);
    }
}
