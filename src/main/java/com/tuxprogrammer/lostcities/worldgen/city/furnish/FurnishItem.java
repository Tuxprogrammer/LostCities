package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * One weighted entry of a room palette (design record 0016, decision 5): either a block by registry name, or a GT
 * machine by its MetaTileEntity ID. A GT machine is always the block {@value #GT_MACHINES} at its base-type metadata,
 * with a tile entity the population phase fills with {@code mID} and {@code mFacing}.
 *
 * <p>
 * Facings use the horizontal order of {@link #NORTH}, {@link #SOUTH}, {@link #WEST}, {@link #EAST}: the way the front
 * points, which is away from the wall the item stands against. Immutable. Side: both (plain Java).
 */
public final class FurnishItem {

    /** GregTech's machine block (research-gt-machines.md §1). */
    public static final String GT_MACHINES = "gregtech:gt.blockmachines";

    /** Facing index: the front points to -Z. */
    public static final int NORTH = 0;
    /** Facing index: the front points to +Z. */
    public static final int SOUTH = 1;
    /** Facing index: the front points to -X. */
    public static final int WEST = 2;
    /** Facing index: the front points to +X. */
    public static final int EAST = 3;

    private final String block;
    private final int meta;
    private final boolean tile;
    private final int[] facingMeta;
    private final int gtId;
    private final boolean controller;
    private final int weight;

    private FurnishItem(String block, int meta, boolean tile, int[] facingMeta, int gtId, boolean controller,
        int weight) {
        this.block = block;
        this.meta = meta;
        this.tile = tile;
        this.facingMeta = facingMeta == null ? null : facingMeta.clone();
        this.gtId = gtId;
        this.controller = controller;
        this.weight = weight;
    }

    /**
     * Returns a block item.
     *
     * @param block      registry name
     * @param meta       metadata when the item has no facing table
     * @param tile       whether the block has a tile entity, which counts against the caps
     * @param facingMeta metadata per facing index, or {@code null}
     * @param weight     relative weight, at least 1
     * @return the item
     */
    public static FurnishItem block(String block, int meta, boolean tile, int[] facingMeta, int weight) {
        return new FurnishItem(block, meta, tile, facingMeta, 0, false, weight);
    }

    /**
     * Returns a GT machine item.
     *
     * @param gtId       the MetaTileEntity ID
     * @param baseMeta   the block metadata of the machine's tile-entity base type (0 ULV, 1 LV to HV, 2 EV and
     *                   controllers)
     * @param controller whether it is a multiblock controller, kept two blocks from the chunk's edges
     * @param weight     relative weight, at least 1
     * @return the item
     */
    public static FurnishItem machine(int gtId, int baseMeta, boolean controller, int weight) {
        return new FurnishItem(GT_MACHINES, baseMeta, true, null, gtId, controller, weight);
    }

    /** Returns the block's registry name. */
    public String block() {
        return block;
    }

    /** Returns whether the item places a tile entity: counted against the per-room and per-chunk caps. */
    public boolean tile() {
        return tile;
    }

    /** Returns whether the item is a GT machine. */
    public boolean isMachine() {
        return gtId > 0;
    }

    /** Returns the GT MetaTileEntity ID, or 0 for a block item. */
    public int gtId() {
        return gtId;
    }

    /** Returns whether the item is a multiblock controller. */
    public boolean controller() {
        return controller;
    }

    /** Returns the relative weight. */
    public int weight() {
        return weight;
    }

    /** Returns the base metadata: the block's own, or a GT machine's base type. */
    public int meta() {
        return meta;
    }

    /**
     * Returns the block written for a facing.
     *
     * @param facing the facing index, {@link #NORTH} to {@link #EAST}
     * @return the block reference
     */
    public BlockRef blockFor(int facing) {
        int m = facingMeta == null ? meta : facingMeta[facing];
        return BlockRef.of(block, m);
    }

    @Override
    public String toString() {
        return isMachine() ? "gt:" + gtId + "@" + meta : block + "@" + meta;
    }
}
