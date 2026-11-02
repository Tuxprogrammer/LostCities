package com.tuxprogrammer.lostcities.asset.block;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * One block string of an asset, resolved at load against the block-name map and the live registry: the 1.7.10 block it
 * becomes, and, for a decorative metal (D29), the block it becomes while that metal's flag is off. Resolution happens
 * once, at {@code postInit}, so generation never looks a name up (docs/new-spec/05-assets/asset-format.md, "Block
 * references").
 *
 * <p>
 * The hard-air marker ({@code minecraft:structure_void}) resolves to {@link #HARD_AIR}, which places nothing: "air,
 * water below the water level, or keep, by the part's hard-air setting" (docs/original-spec/05-assets/
 * parts-and-placement.md, step 6). Immutable. Side: both.
 */
public final class BlockChoice {

    /** The internal hard-air marker; never written as a block. */
    public static final BlockRef HARD_AIR_MARKER = BlockRef.of("lostcitiesnh:hard_air", 0);

    /** The choice for {@code minecraft:structure_void}. */
    public static final BlockChoice HARD_AIR = new BlockChoice(
        "minecraft:structure_void",
        HARD_AIR_MARKER,
        null,
        null,
        false,
        null,
        false,
        false,
        RotationFamily.NONE);

    private final String source;
    private final BlockRef block;
    private final DecorativeMetal metal;
    private final BlockRef metalOff;
    private final boolean fallback;
    private final TileTemplate tag;
    private final boolean capExempt;
    private final boolean treasure;
    private final RotationFamily family;

    BlockChoice(String source, BlockRef block, DecorativeMetal metal, BlockRef metalOff, boolean fallback,
        TileTemplate tag, boolean capExempt, boolean treasure, RotationFamily family) {
        this.source = source;
        this.block = block;
        this.metal = metal;
        this.metalOff = metalOff;
        this.fallback = fallback;
        this.tag = tag;
        this.capExempt = capExempt;
        this.treasure = treasure;
        this.family = family;
    }

    /**
     * Returns a choice for a block that needs no mapping, for code-named blocks and tests.
     *
     * @param block the block
     * @return the choice
     */
    public static BlockChoice of(BlockRef block) {
        return new BlockChoice(block.toString(), block, null, null, false, null, false, false, RotationFamily.NONE);
    }

    /** Returns the block string as the asset wrote it. */
    public String source() {
        return source;
    }

    /** Returns the block placed while every flag is on, or when the block is no decorative metal. */
    public BlockRef block() {
        return block;
    }

    /**
     * Returns the block a world places.
     *
     * @param flags the world's decorative-metal flags
     * @return {@link #block()}, or the metal-off block when this is a decorative metal whose flag is off
     */
    public BlockRef block(MetalFlags flags) {
        if (metal != null && metalOff != null && !flags.isOn(metal)) {
            return metalOff;
        }
        return block;
    }

    /** Returns the decorative metal governing this block, or null. Treasure is loot and has none. */
    public DecorativeMetal metal() {
        return metal;
    }

    /** Returns the block used while {@link #metal()}'s flag is off, or null. */
    public BlockRef metalOff() {
        return metalOff;
    }

    /** Returns whether a fallback was taken because the primary block's mod is absent. */
    public boolean isFallback() {
        return fallback;
    }

    /** Returns whether this is the hard-air marker. */
    public boolean isHardAir() {
        return this == HARD_AIR;
    }

    /** Returns the tile-entity template the block needs, or null. */
    public TileTemplate tag() {
        return tag;
    }

    /** Returns whether this block is exempt from D29's per-building accent cap (the cabin, the gas station). */
    public boolean capExempt() {
        return capExempt;
    }

    /** Returns whether the block is a treasure character's, which is loot. */
    public boolean treasure() {
        return treasure;
    }

    /** Returns how the block's metadata turns with a part's transform. */
    public RotationFamily family() {
        return family;
    }

    @Override
    public String toString() {
        return source + " -> " + block + (metal == null ? "" : " [" + metal.key() + " off: " + metalOff + "]");
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BlockChoice)) {
            return false;
        }
        BlockChoice that = (BlockChoice) other;
        return block.equals(that.block) && metal == that.metal
            && java.util.Objects.equals(metalOff, that.metalOff)
            && java.util.Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(block, metal, metalOff, tag);
    }
}
