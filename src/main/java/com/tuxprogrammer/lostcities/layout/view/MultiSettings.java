package com.tuxprogrammer.lostcities.layout.view;

/**
 * A world style's {@code multisettings}: how multibuildings are placed per multichunk. The asset loader validates the
 * values (CITY-25: {@code maximum >= minimum}, every multibuilding fits the area), so the layout never throws on them.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/multibuildings.md#3-numbers. Side: both.
 */
public final class MultiSettings {

    /** The {@code standard} world style's values, which are also the code defaults. */
    public static final MultiSettings DEFAULTS = new MultiSettings(10, 1, 5, 0.8f, 50);

    private final int areaSize;
    private final int minimum;
    private final int maximum;
    private final float correctStyleFactor;
    private final int attempts;

    /**
     * Creates settings.
     *
     * @param areaSize           {@code areasize}: chunks per multichunk side
     * @param minimum            {@code minimum}: fewest multibuildings attempted per multichunk
     * @param maximum            {@code maximum}: most attempted
     * @param correctStyleFactor {@code correctstylefactor}: share of footprint chunks that must have the building's
     *                           city style
     * @param attempts           {@code attempts}: random positions tried per multibuilding
     */
    public MultiSettings(int areaSize, int minimum, int maximum, float correctStyleFactor, int attempts) {
        this.areaSize = areaSize;
        this.minimum = minimum;
        this.maximum = maximum;
        this.correctStyleFactor = correctStyleFactor;
        this.attempts = attempts;
    }

    /** Returns {@code areasize}, chunks. */
    public int areaSize() {
        return areaSize;
    }

    /** Returns {@code minimum}. */
    public int minimum() {
        return minimum;
    }

    /** Returns {@code maximum}. */
    public int maximum() {
        return maximum;
    }

    /** Returns {@code correctstylefactor}. */
    public float correctStyleFactor() {
        return correctStyleFactor;
    }

    /** Returns {@code attempts}. */
    public int attempts() {
        return attempts;
    }
}
