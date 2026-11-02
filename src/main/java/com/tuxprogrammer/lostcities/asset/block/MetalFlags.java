package com.tuxprogrammer.lostcities.asset.block;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which decorative metals a world may place (D29): an immutable set of {@link DecorativeMetal}s whose flags are on. A
 * world's flags are pinned into its profile when it is created, so palettes compiled for that world always use the
 * same set; asset loading itself never depends on them.
 *
 * <p>
 * Side: both.
 */
public final class MetalFlags {

    /** Every metal on: the default of every flag. */
    public static final MetalFlags ALL_ON = new MetalFlags(EnumSet.allOf(DecorativeMetal.class));

    /** Every metal off. */
    public static final MetalFlags ALL_OFF = new MetalFlags(EnumSet.noneOf(DecorativeMetal.class));

    private final Set<DecorativeMetal> enabled;

    private MetalFlags(EnumSet<DecorativeMetal> enabled) {
        this.enabled = Collections.unmodifiableSet(enabled);
    }

    /**
     * Returns the flags with exactly these metals on.
     *
     * @param enabled the metals whose flag is on
     * @return the flags
     */
    public static MetalFlags of(Set<DecorativeMetal> enabled) {
        EnumSet<DecorativeMetal> copy = EnumSet.noneOf(DecorativeMetal.class);
        copy.addAll(enabled);
        return new MetalFlags(copy);
    }

    /** Returns whether a metal's flag is on. */
    public boolean isOn(DecorativeMetal metal) {
        return enabled.contains(metal);
    }

    /** Returns the metals whose flag is on. */
    public Set<DecorativeMetal> enabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MetalFlags && ((MetalFlags) other).enabled.equals(enabled);
    }

    @Override
    public int hashCode() {
        return enabled.hashCode();
    }

    @Override
    public String toString() {
        return "MetalFlags" + enabled;
    }
}
