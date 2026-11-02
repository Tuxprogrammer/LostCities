package com.tuxprogrammer.lostcities.asset.block;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * One row of the block-name map: a 1.20 block id and the 1.7.10 blocks it becomes, as a fallback chain whose first
 * existing block is used (docs/new-spec/00-pack-integration/block-mapping.md, "How the mapping works"). A row may be
 * scoped to some assets, as the cabin's copper and the gas station's iron are, and may carry a decorative-metal flag
 * with the chain to use when that flag is off (D29, D31).
 *
 * <p>
 * Immutable. Side: both.
 */
public final class MappingRow {

    /** One block of a fallback chain: a 1.7.10 block with its base metadata, and the rule that adds state bits. */
    public static final class Candidate {

        private final BlockRef base;
        private final MetaRule rule;

        /**
         * Creates a candidate.
         *
         * @param base the block with its base metadata (the type bits)
         * @param rule how 1.20 properties add to the metadata
         */
        public Candidate(BlockRef base, MetaRule rule) {
            this.base = base;
            this.rule = rule;
        }

        /** Returns the block with its base metadata. */
        public BlockRef base() {
            return base;
        }

        /** Returns the rule that turns properties into metadata bits for this block. */
        public MetaRule rule() {
            return rule;
        }

        @Override
        public String toString() {
            return base + (rule == MetaRule.PLAIN ? ""
                : " (" + rule.name()
                    .toLowerCase(Locale.ROOT) + ")");
        }
    }

    private final String id;
    private final List<Candidate> chain;
    private final List<Candidate> doubleChain;
    private final DecorativeMetal metal;
    private final List<Candidate> metalOff;
    private final boolean capExempt;
    private final boolean hardAir;
    private final boolean matchOnly;
    private final TileTemplate tag;
    private final Set<AssetId> scope;

    MappingRow(String id, List<Candidate> chain, List<Candidate> doubleChain, DecorativeMetal metal,
        List<Candidate> metalOff, boolean capExempt, boolean hardAir, boolean matchOnly, TileTemplate tag,
        Set<AssetId> scope) {
        this.id = id;
        this.chain = Collections.unmodifiableList(chain);
        this.doubleChain = Collections.unmodifiableList(doubleChain);
        this.metal = metal;
        this.metalOff = Collections.unmodifiableList(metalOff);
        this.capExempt = capExempt;
        this.hardAir = hardAir;
        this.matchOnly = matchOnly;
        this.tag = tag;
        this.scope = Collections.unmodifiableSet(scope);
    }

    /** Returns the 1.20 id, {@code minecraft:stone_bricks}. */
    public String id() {
        return id;
    }

    /** Returns the fallback chain, primary first; the last block is vanilla unless {@link #matchOnly()}. */
    public List<Candidate> chain() {
        return chain;
    }

    /** Returns the chain for {@code type=double} of a slab row, empty for other rows. */
    public List<Candidate> doubleChain() {
        return doubleChain;
    }

    /** Returns the decorative metal whose flag governs this row, or null. */
    public DecorativeMetal metal() {
        return metal;
    }

    /** Returns the chain used while {@link #metal()}'s flag is off, empty when the row has no flag. */
    public List<Candidate> metalOff() {
        return metalOff;
    }

    /** Returns whether this row is one of the two exceptions to D29's per-building accent cap. */
    public boolean capExempt() {
        return capExempt;
    }

    /** Returns whether the row is the hard-air marker ({@code minecraft:structure_void}), which places nothing. */
    public boolean hardAir() {
        return hardAir;
    }

    /** Returns whether the row may be used only in block matchers, where every existing candidate matches. */
    public boolean matchOnly() {
        return matchOnly;
    }

    /** Returns the tile-entity template the 1.7.10 block needs, or null (the potted cactus's pot contents). */
    public TileTemplate tag() {
        return tag;
    }

    /** Returns the assets this row applies to, empty for a global row. */
    public Set<AssetId> scope() {
        return scope;
    }

    @Override
    public String toString() {
        return id + " -> " + chain;
    }
}
