package com.tuxprogrammer.lostcities.layout;

/**
 * The level-0 terrain-correction target at a chunk corner (upstream's {@code getLowestCityHeightAtChunkCorner}): a
 * block Y the blend must pass through, or no constraint. The terrain blend (03-terrain, M04) builds its L1 and L2
 * meshes and random offsets on it.
 *
 * <p>
 * Avoids CITY-22: "no constraint" is an explicit flag ({@link #isConstrained()}) instead of upstream's sentinel 100000
 * tested against a hard-coded build limit of 256.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/building-info.md § Terrain-correction targets. Side: both.
 */
public final class CornerHeight {

    /** A corner the city does not constrain. */
    public static final CornerHeight UNCONSTRAINED = new CornerHeight(false, 0);

    private final boolean constrained;
    private final int height;

    private CornerHeight(boolean constrained, int height) {
        this.constrained = constrained;
        this.height = height;
    }

    /**
     * A corner fixed at a height.
     *
     * @param height block Y
     * @return the constrained corner
     */
    public static CornerHeight at(int height) {
        return new CornerHeight(true, height);
    }

    /** Returns whether the corner is fixed. */
    public boolean isConstrained() {
        return constrained;
    }

    /** Returns the corner's block Y; meaningful only when {@link #isConstrained()}. */
    public int height() {
        return height;
    }

    @Override
    public String toString() {
        return constrained ? "corner@" + height : "corner(free)";
    }
}
