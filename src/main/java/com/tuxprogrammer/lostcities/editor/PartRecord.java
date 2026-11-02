package com.tuxprogrammer.lostcities.editor;

import java.util.Objects;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * One part an {@code editMode} world's generation wrote into a chunk: its id, the block Y of its first slice, and its
 * transform and local offset, which upstream did not record (docs/new-spec/01-core/index.md, Part editor). The part
 * spans {@code y} to {@code y + height - 1}.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class PartRecord {

    private final AssetId part;
    private final int y;
    private final Transform transform;
    private final int offsetX;
    private final int offsetZ;

    /**
     * Creates a record.
     *
     * @param part      the part's id
     * @param y         block Y of its first slice
     * @param transform its transform
     * @param offsetX   local X of its origin, 0 to 15
     * @param offsetZ   local Z of its origin, 0 to 15
     */
    public PartRecord(AssetId part, int y, Transform transform, int offsetX, int offsetZ) {
        this.part = Objects.requireNonNull(part);
        this.y = y;
        this.transform = Objects.requireNonNull(transform);
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    /** Returns the part's id. */
    public AssetId part() {
        return part;
    }

    /** Returns the block Y of the part's first slice. */
    public int y() {
        return y;
    }

    /** Returns the part's transform. */
    public Transform transform() {
        return transform;
    }

    /** Returns the local X of the part's origin. */
    public int offsetX() {
        return offsetX;
    }

    /** Returns the local Z of the part's origin. */
    public int offsetZ() {
        return offsetZ;
    }

    /**
     * Returns whether a block Y lies inside the part: upstream's {@code recordedY <= y < recordedY + sliceCount}.
     *
     * @param blockY the Y
     * @param height the part's slice count
     * @return whether it is inside
     */
    public boolean spans(int blockY, int height) {
        return blockY >= y && blockY < y + height;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PartRecord that)) {
            return false;
        }
        return y == that.y && offsetX == that.offsetX
            && offsetZ == that.offsetZ
            && part.equals(that.part)
            && transform == that.transform;
    }

    @Override
    public int hashCode() {
        return Objects.hash(part, y, transform, offsetX, offsetZ);
    }

    @Override
    public String toString() {
        return part + "@"
            + y
            + (transform == Transform.ROTATE_NONE ? "" : " " + transform)
            + (offsetX == 0 && offsetZ == 0 ? "" : " +" + offsetX + "," + offsetZ);
    }
}
