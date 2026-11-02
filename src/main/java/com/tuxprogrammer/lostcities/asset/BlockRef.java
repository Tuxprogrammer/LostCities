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
 * Side: both.
 */
public final class BlockRef {

    /** The highest metadata value a 1.7.10 block can store (four bits). */
    public static final int MAX_META = 15;

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
     * @param name the registry name, {@code modid:name}: two non-empty parts, no whitespace, exactly one colon
     * @param meta the metadata, 0 to {@link #MAX_META}
     * @return the reference
     * @throws IllegalArgumentException when the name or the metadata is malformed
     */
    public static BlockRef of(String name, int meta) {
        if (!isRegistryName(name)) {
            throw new IllegalArgumentException("not a modid:name block name: '" + name + "'");
        }
        if (meta < 0 || meta > MAX_META) {
            throw new IllegalArgumentException("metadata " + meta + " of " + name + " not in 0.." + MAX_META);
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
                throw new IllegalArgumentException("metadata of '" + text + "' is not 0.." + MAX_META);
            }
            return of(text.substring(0, last), Integer.parseInt(metaText));
        }
        return of(text, 0);
    }

    /** Returns the registry name, {@code modid:name}. */
    public String name() {
        return name;
    }

    /** Returns the mod ID part of the registry name. */
    public String modId() {
        return name.substring(0, name.indexOf(':'));
    }

    /** Returns the metadata, 0 to {@link #MAX_META}. */
    public int meta() {
        return meta;
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
        for (int i = 0; i < name.length(); i++) {
            if (Character.isWhitespace(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDecimal(String text) {
        if (text.isEmpty() || text.length() > 2) {
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
