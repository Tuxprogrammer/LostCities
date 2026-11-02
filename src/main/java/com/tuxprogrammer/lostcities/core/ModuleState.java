package com.tuxprogrammer.lostcities.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.tuxprogrammer.lostcities.config.WorldgenConfig;

/**
 * Which modules are active: the one authority every use site asks, instead of reading config flags itself
 * (config-safe-registration). Instances are immutable snapshots.
 *
 * <p>
 * A new world copies {@link #configured()} into its pinned profile snapshot (docs/design/0005-rwg-overlay.md §7), and
 * its generation reads the pinned state ({@code ProfileSnapshot.moduleState()}), so a later config edit cannot change
 * a world that already exists.
 *
 * <p>
 * Side: both; only the server's state decides generation. No module flag is synced to clients, because no client
 * feature reads one (docs/new-spec/01-core/index.md, "Config sync").
 */
public final class ModuleState {

    private final Set<Module> active;

    private ModuleState(Set<Module> active) {
        this.active = Collections.unmodifiableSet(active);
    }

    /**
     * Returns a snapshot with exactly the given modules active.
     *
     * @param active the active modules
     * @return the snapshot
     */
    public static ModuleState of(Set<Module> active) {
        EnumSet<Module> copy = EnumSet.noneOf(Module.class);
        copy.addAll(active);
        return new ModuleState(copy);
    }

    /** Returns a snapshot with every module active, the default configuration. */
    public static ModuleState allActive() {
        return new ModuleState(EnumSet.allOf(Module.class));
    }

    /** Returns a snapshot of the flags as the config currently holds them. */
    public static ModuleState configured() {
        EnumSet<Module> active = EnumSet.noneOf(Module.class);
        addIf(active, Module.CITIES, WorldgenConfig.cities);
        addIf(active, Module.HIGHWAYS, WorldgenConfig.highways);
        addIf(active, Module.RAILWAYS, WorldgenConfig.railways);
        addIf(active, Module.SCATTERED_STRUCTURES, WorldgenConfig.scatteredStructures);
        addIf(active, Module.PALETTE_VARIETY, WorldgenConfig.paletteVariety);
        return new ModuleState(active);
    }

    /**
     * Returns whether a module is active.
     *
     * @param module the module
     * @return {@code true} when its generation runs
     */
    public boolean isActive(Module module) {
        return active.contains(module);
    }

    /** Returns the active modules, in declaration order. */
    public Set<Module> active() {
        return active;
    }

    /** Returns the active modules for the log, for example {@code [CITIES, HIGHWAYS]}. */
    @Override
    public String toString() {
        return active.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ModuleState && ((ModuleState) other).active.equals(active);
    }

    @Override
    public int hashCode() {
        return active.hashCode();
    }

    private static void addIf(Set<Module> active, Module module, boolean on) {
        if (on) {
            active.add(module);
        }
    }
}
