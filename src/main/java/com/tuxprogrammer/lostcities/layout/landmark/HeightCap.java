package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * The paraboloid an AE2 meteorite's crater removes everything above: at horizontal distance {@code d} blocks from the
 * impact, Y {@code centerY + curvature * d²}, out to {@code reach} blocks. A city building's roof must stay below it,
 * so the city steps down toward the impact instead of being sliced (0006 §1 and §3; coexistence.md § AE2 meteorites:
 * {@code y - r + 1 + 0.02 d²} within 200 blocks). The compat oracle ({@code compat.ae2.CraterBasin}) fills in the
 * lowest center the crater can have, {@code Tmin - 10 - r + 1} less a block of slack, where {@code Tmin} is the lowest
 * natural ground in the meteor chunk's sky-count box: AE2 digs a crater only when more than 10 blocks of that box,
 * which reaches 10 blocks above the meteor, see the sky. The layout only evaluates the surface.
 *
 * <p>
 * Side: both.
 */
public final class HeightCap {

    /** AE2's crater curvature, per block². */
    public static final double AE2_CURVATURE = 0.02D;
    /** How far AE2's crater pass reaches from the impact, in blocks. */
    public static final int AE2_REACH = 200;
    /**
     * Blocks above a chunk's city ground the cap must clear for the chunk to stay city: one city level, for its
     * street, park furniture and lamps. A chunk whose cap is lower leaves the city, so the crater cuts natural ground
     * there, not a street (D216).
     */
    public static final int CITY_HEADROOM = 6;

    private final int centerX;
    private final int centerZ;
    private final int centerY;
    private final double curvature;
    private final int reach;

    /**
     * Creates a cap.
     *
     * @param centerX   the impact's block X
     * @param centerZ   the impact's block Z
     * @param centerY   the paraboloid's lowest Y, at the impact
     * @param curvature how fast the cap rises, per block² of horizontal distance
     * @param reach     the horizontal distance in blocks beyond which nothing is removed
     */
    public HeightCap(int centerX, int centerZ, int centerY, double curvature, int reach) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.centerY = centerY;
        this.curvature = curvature;
        this.reach = reach;
    }

    /**
     * An AE2 crater cap.
     *
     * @param centerX the impact's block X
     * @param centerZ the impact's block Z
     * @param centerY the conservative center height
     * @return the cap
     */
    public static HeightCap ae2(int centerX, int centerZ, int centerY) {
        return new HeightCap(centerX, centerZ, centerY, AE2_CURVATURE, AE2_REACH);
    }

    /** Returns the impact's block X. */
    public int centerX() {
        return centerX;
    }

    /** Returns the impact's block Z. */
    public int centerZ() {
        return centerZ;
    }

    /** Returns the cap's lowest Y. */
    public int centerY() {
        return centerY;
    }

    /**
     * Returns the lowest cap over a chunk: the cap at the chunk's column nearest the impact, rounded down, or
     * {@link Integer#MAX_VALUE} when even that column is out of reach.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the highest Y a roof in this chunk may reach, or {@code Integer.MAX_VALUE}
     */
    public int lowestCapOver(int chunkX, int chunkZ) {
        int nearestX = clamp(centerX, chunkX << 4, (chunkX << 4) + 15);
        int nearestZ = clamp(centerZ, chunkZ << 4, (chunkZ << 4) + 15);
        double dx = nearestX - centerX;
        double dz = nearestZ - centerZ;
        double squared = dx * dx + dz * dz;
        if (squared > (double) reach * reach) {
            return Integer.MAX_VALUE;
        }
        double cap = centerY + curvature * squared;
        return cap >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.floor(cap);
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    @Override
    public String toString() {
        return "cap(" + centerX + "," + centerZ + " y" + centerY + " k" + curvature + " r" + reach + ")";
    }
}
