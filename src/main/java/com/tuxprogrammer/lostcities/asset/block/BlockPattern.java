package com.tuxprogrammer.lostcities.asset.block;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * A pattern over block references, as the block-name map's lists write them: {@code modid:name} matches every
 * metadata of that block, {@code modid:name:meta} one metadata, and a trailing {@code *} every registry name with that
 * prefix ({@code gregtech:gt.blockcasings*}).
 *
 * <p>
 * Side: both.
 */
public final class BlockPattern {

    private final String name;
    private final int meta;
    private final boolean prefix;

    private BlockPattern(String name, int meta, boolean prefix) {
        this.name = name;
        this.meta = meta;
        this.prefix = prefix;
    }

    /**
     * Parses a pattern.
     *
     * @param text the pattern text
     * @return the pattern
     * @throws IllegalArgumentException when the text is neither form
     */
    public static BlockPattern parse(String text) {
        if (text == null || text.indexOf(':') <= 0) {
            throw new IllegalArgumentException("not a block pattern: '" + text + "'");
        }
        if (text.endsWith("*")) {
            return new BlockPattern(text.substring(0, text.length() - 1), -1, true);
        }
        if (BlockRef.isDirectForm(text)) {
            BlockRef ref = BlockRef.parse(text);
            return new BlockPattern(ref.name(), ref.meta(), false);
        }
        return new BlockPattern(
            BlockRef.of(text, 0)
                .name(),
            -1,
            false);
    }

    /** Returns whether a block reference matches. */
    public boolean matches(BlockRef ref) {
        if (prefix) {
            return ref.name()
                .startsWith(name);
        }
        return ref.name()
            .equals(name) && (meta < 0 || meta == ref.meta());
    }

    @Override
    public String toString() {
        return prefix ? name + "*" : meta < 0 ? name : name + ":" + meta;
    }
}
