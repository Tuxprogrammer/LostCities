package com.tuxprogrammer.lostcities.asset;

import java.util.Objects;

/**
 * A block as world generation names it: a registry name {@code modid:name} plus a metadata value. It is pure data,
 * with no Minecraft type, so layout code, chunk buffers and golden tests can use it without a game. The world-backed
 * chunk buffer (M04) resolves each {@code BlockRef} to a {@code Block} once per asset load, never per block written.
 *
 * <p>
 * The text form is {@code modid:name:meta}, the native reference form of docs/new-spec/05-assets (decision log D14);
 * there are no numeric block IDs anywhere (CONTRIBUTING § World generation rules). Instances are immutable and compare
 * by value.
 *
 * <p>
 * Registry names are kept verbatim, as the block catalog's prerequisites require
 * (docs/new-spec/05-assets/block-catalog/index.md, "Prerequisites for the asset system", items 2 and 3): {@code tile.}
 * prefixes, dots, pipes in mod IDs, case, and single spaces inside a name ({@code Natura:Rare Tree}). Metadata may go
 * above the vanilla nibble, up to {@link #MAX_EXTENDED_META}, for blocks such as GT frame boxes whose metadata is a
 * material ID; such values exist only where EndlessIDs stores them, and {@link #isExtendedMeta()} lets the writer tell
 * them apart.
 *
 * <p>
 * Side: both.
 */
public final class BlockRef {

    /** The highest metadata value a vanilla 1.7.10 block can store (four bits). */
    public static final int MAX_META = 15;

    /**
     * The highest metadata value a reference may carry: EndlessIDs widens block metadata to 16 bits, and GT uses
     * material IDs up to about 1000 as metadata (block catalog prerequisites, item 3).
     */
    public static final int MAX_EXTENDED_META = 32767;

    /** The most digits a metadata value is written with. */
    private static final int MAX_META_DIGITS = 5;

    /** Air, the content of an empty chunk buffer. */
    public static final BlockRef AIR = new BlockRef("minecraft:air", 0);

    private final String name;
    private final int meta;

    private BlockRef(String name, int meta) {
        this.name = name;
        this.meta = meta;
    }

    /**
     * Returns the reference to a block.
     *
     * @param name the registry name, {@code modid:name}: two non-empty parts, exactly one colon, no whitespace other
     *             than single spaces inside a part
     * @param meta the metadata, 0 to {@link #MAX_EXTENDED_META}
     * @return the reference
     * @throws IllegalArgumentException when the name or the metadata is malformed
     */
    public static BlockRef of(String name, int meta) {
        if (!isRegistryName(name)) {
            throw new IllegalArgumentException("not a modid:name block name: '" + name + "'");
        }
        if (meta < 0 || meta > MAX_EXTENDED_META) {
            throw new IllegalArgumentException("metadata " + meta + " of " + name + " not in 0.." + MAX_EXTENDED_META);
        }
        if (AIR.name.equals(name) && meta == 0) {
            return AIR;
        }
        return new BlockRef(name, meta);
    }

    /**
     * Parses the text form {@code modid:name:meta}, or {@code modid:name} for metadata 0.
     *
     * @param text the text form
     * @return the reference
     * @throws IllegalArgumentException when the text is malformed
     */
    public static BlockRef parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("block reference is null");
        }
        int last = text.lastIndexOf(':');
        int first = text.indexOf(':');
        if (first >= 0 && last > first) {
            String metaText = text.substring(last + 1);
            if (!isDecimal(metaText)) {
                throw new IllegalArgumentException("metadata of '" + text + "' is not 0.." + MAX_EXTENDED_META);
            }
            int meta = Integer.parseInt(metaText);
            if (meta > MAX_EXTENDED_META) {
                throw new IllegalArgumentException("metadata of '" + text + "' is not 0.." + MAX_EXTENDED_META);
            }
            return of(text.substring(0, last), meta);
        }
        return of(text, 0);
    }

    /**
     * Returns whether a text names a block directly with its metadata, {@code modid:name:meta}: at least two colons and
     * a decimal after the last one. A 1.20 block name never ends in {@code :<digits>}, so the two forms cannot collide
     * (docs/new-spec/05-assets/asset-format.md, "Telling the two forms apart").
     *
     * @param text the text
     * @return whether it is in the direct form; it may still be malformed in its name part
     */
    public static boolean isDirectForm(String text) {
        if (text == null) {
            return false;
        }
        int last = text.lastIndexOf(':');
        int first = text.indexOf(':');
        return first >= 0 && last > first && isDecimal(text.substring(last + 1));
    }

    /** Returns the registry name, {@code modid:name}. */
    public String name() {
        return name;
    }

    /** Returns the mod ID part of the registry name. */
    public String modId() {
        return name.substring(0, name.indexOf(':'));
    }

    /** Returns the metadata, 0 to {@link #MAX_EXTENDED_META}. */
    public int meta() {
        return meta;
    }

    /** Returns whether the metadata is above the vanilla nibble, so only an extended-metadata write can store it. */
    public boolean isExtendedMeta() {
        return meta > MAX_META;
    }

    /**
     * Returns the same block with other metadata.
     *
     * @param newMeta the metadata, 0 to {@link #MAX_EXTENDED_META}
     * @return the reference
     */
    public BlockRef withMeta(int newMeta) {
        return newMeta == meta ? this : of(name, newMeta);
    }

    /** Returns {@code true} for {@link #AIR}. */
    public boolean isAir() {
        return this == AIR;
    }

    /** Returns the text form, {@code modid:name:meta}. */
    @Override
    public String toString() {
        return name + ":" + meta;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockRef)) {
            return false;
        }
        BlockRef that = (BlockRef) other;
        return meta == that.meta && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, meta);
    }

    private static boolean isRegistryName(String name) {
        if (name == null) {
            return false;
        }
        int colon = name.indexOf(':');
        if (colon <= 0 || colon == name.length() - 1 || name.indexOf(':', colon + 1) >= 0) {
            return false;
        }
        if (name.contains("  ")) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ' ') {
                // A space is allowed only inside a part, never at either end of the mod ID or the name.
                if (i == 0 || i == name.length() - 1 || i == colon - 1 || i == colon + 1) {
                    return false;
                }
            } else if (Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDecimal(String text) {
        if (text.isEmpty() || text.length() > MAX_META_DIGITS) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }
}
