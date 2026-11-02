// @formatter:off
package com.tuxprogrammer.lostcities.worldgen.damage;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Explosion.java
// @formatter:on

/**
 * One explosion of the abandoned-city look: a center block and a radius in blocks
 * (docs/original-spec/03-terrain/damage-ruins-rubble.md). Distances are measured from the center block's center,
 * as upstream's {@code BlockPos.distToCenterSqr} does.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class Explosion {

    private final int x;
    private final int y;
    private final int z;
    private final int radius;
    private final int squaredRadius;

    /**
     * Creates an explosion.
     *
     * @param x      center block X
     * @param y      center block Y
     * @param z      center block Z
     * @param radius radius in blocks, at least 1
     */
    public Explosion(int x, int y, int z, int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException("explosion radius " + radius + " is below 1");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.squaredRadius = radius * radius;
    }

    /** Returns the center's block X. */
    public int x() {
        return x;
    }

    /** Returns the center's block Y. */
    public int y() {
        return y;
    }

    /** Returns the center's block Z. */
    public int z() {
        return z;
    }

    /** Returns the radius in blocks. */
    public int radius() {
        return radius;
    }

    /** Returns the radius squared. */
    public int squaredRadius() {
        return squaredRadius;
    }

    /**
     * Returns the squared distance from the center block's center to a point, as upstream's
     * {@code distToCenterSqr(px, py, pz)}.
     *
     * @param px point X
     * @param py point Y
     * @param pz point Z
     * @return squared distance in blocks²
     */
    public double squaredDistanceTo(double px, double py, double pz) {
        double dx = x + 0.5 - px;
        double dy = y + 0.5 - py;
        double dz = z + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return "Explosion[" + x + "," + y + "," + z + " r=" + radius + "]";
    }
}
