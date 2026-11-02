// spotless:off
package com.tuxprogrammer.lostcities.asset.transform;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Transform.java
// Avoids: CITY-27, CITY-28, ASSET-22
// spotless:on

/**
 * How a part is turned or mirrored when it is placed: the four rotations and three mirrors upstream uses, mapping a
 * part's local {@code (x, z)} to chunk-local coordinates (docs/original-spec/05-assets/parts-and-placement.md,
 * "Transforms"). The formulas assume a 16 × 16 footprint, as upstream's do: smaller parts turn about the chunk.
 *
 * <p>
 * Changes from upstream: block facings are transformed by the same map as positions, so a mirror reflects a facing
 * instead of rotating it by 180° or 90° (fixes CITY-28, which upstream also files as ASSET-17), and every rail shape
 * has an image under every transform (fixes CITY-27, ASSET-16). Upstream's unseeded {@code randomRotation} is not
 * ported (ASSET-22). Coordinates: X grows east, Z grows south. Side: both.
 */
public enum Transform {

    /** Identity. */
    ROTATE_NONE(1, 0, 0, 1),
    /** 90° clockwise seen from above: {@code (15 − z, x)}. */
    ROTATE_90(0, -1, 1, 0),
    /** 180°: {@code (15 − x, 15 − z)}. */
    ROTATE_180(-1, 0, 0, -1),
    /** 90° counter-clockwise: {@code (z, 15 − x)}. */
    ROTATE_270(0, 1, -1, 0),
    /** Mirror across the X midline: {@code (15 − x, z)}. */
    MIRROR_X(-1, 0, 0, 1),
    /** Mirror across the Z midline: {@code (x, 15 − z)}. */
    MIRROR_Z(1, 0, 0, -1),
    /** Transpose, swapping X and Z: {@code (z, x)}. */
    MIRROR_90_X(0, 1, 1, 0);

    /** The largest chunk-local coordinate, upstream's constant 15. */
    private static final int MAX = 15;

    // The linear part of the map on direction vectors: (dx, dz) -> (xx·dx + xz·dz, zx·dx + zz·dz).
    private final int xx;
    private final int xz;
    private final int zx;
    private final int zz;

    Transform(int xx, int xz, int zx, int zz) {
        this.xx = xx;
        this.xz = xz;
        this.zx = zx;
        this.zz = zz;
    }

    /**
     * Returns the chunk-local X a part's {@code (x, z)} lands on.
     *
     * @param x the part's local X
     * @param z the part's local Z
     * @return the chunk-local X
     */
    public int rotateX(int x, int z) {
        return xx * x + xz * z + (xx + xz < 0 ? MAX : 0);
    }

    /**
     * Returns the chunk-local Z a part's {@code (x, z)} lands on.
     *
     * @param x the part's local X
     * @param z the part's local Z
     * @return the chunk-local Z
     */
    public int rotateZ(int x, int z) {
        return zx * x + zz * z + (zx + zz < 0 ? MAX : 0);
    }

    /** Returns whether this transform reflects (reverses handedness), so door hinges swap sides. */
    public boolean isMirror() {
        return xx * zz - xz * zx < 0;
    }

    /**
     * Returns the transform that undoes this one: rotations by 90° swap; the 180° rotation and the mirrors are their
     * own inverse, as upstream's {@code getOpposite}.
     *
     * @return the inverse
     */
    public Transform opposite() {
        switch (this) {
            case ROTATE_90:
                return ROTATE_270;
            case ROTATE_270:
                return ROTATE_90;
            default:
                return this;
        }
    }

    /**
     * Returns where a horizontal direction points after this transform.
     *
     * @param direction the direction in the part
     * @return the direction in the chunk
     */
    public Direction apply(Direction direction) {
        if (!direction.isHorizontal()) {
            return direction;
        }
        int dx = xx * direction.dx() + xz * direction.dz();
        int dz = zx * direction.dx() + zz * direction.dz();
        return Direction.of(dx, dz);
    }
}
