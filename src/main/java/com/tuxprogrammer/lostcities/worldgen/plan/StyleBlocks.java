package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * The blocks the terrain phase itself writes for one chunk, already resolved from the city style and palette: the
 * base under streets, the liquid, border and wall blocks, the rubble, filler, iron bars and leaves of damage and
 * ruins, and each block's damaged form. Upstream reads these from {@code CityStyle} and {@code CompiledPalette}
 * characters ({@code borderBlock}, {@code wallBlock}, {@code rubbleBlock}, {@code fillerBlock},
 * {@code ironbarsBlock}, {@code leavesBlock}, {@code rubbleDirtBlock}, palette {@code damaged} entries); here the plan
 * hands them over as plain {@link BlockRef}s so the terrain code needs no asset model (docs/new-spec/03-terrain).
 *
 * <p>
 * M05 builds one per city style and palette from M02's assets; the synthetic test city (M04) builds one by hand.
 * Immutable.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class StyleBlocks {

    /** Upstream's default leaf table (20 jungle, 20 spruce, 88 oak), persistent: 1.7.10 leaves with the no-decay bit. */
    public static final List<BlockRef> DEFAULT_LEAVES = Collections.unmodifiableList(defaultLeaves());

    /**
     * Upstream's default rubble table (20 mossy stone bricks, 40 mossy cobblestone, 68 moss) mapped to 1.7.10 blocks
     * (docs/new-spec/03-terrain/index.md, P03-7): moss is {@code chisel:moss:0}, falling back to mossy cobblestone.
     */
    public static final List<BlockRef> DEFAULT_RUBBLE_DIRT = Collections.unmodifiableList(defaultRubbleDirt());

    private final BlockRef base;
    private final BlockRef liquid;
    private final BlockRef border;
    private final BlockRef wall;
    private final BlockRef rubble;
    private final BlockRef filler;
    private final BlockRef ironBars;
    private final List<BlockRef> leaves;
    private final List<BlockRef> rubbleDirt;
    private final Map<BlockRef, BlockRef> damaged;

    private StyleBlocks(Builder builder) {
        this.base = builder.base;
        this.liquid = builder.liquid;
        this.border = builder.border;
        this.wall = builder.wall;
        this.rubble = builder.rubble;
        this.filler = builder.filler;
        this.ironBars = builder.ironBars;
        this.leaves = Collections.unmodifiableList(new ArrayList<>(builder.leaves));
        this.rubbleDirt = Collections.unmodifiableList(new ArrayList<>(builder.rubbleDirt));
        this.damaged = Collections.unmodifiableMap(new HashMap<>(builder.damaged));
    }

    /** Returns a builder preset to upstream's defaults with vanilla 1.7.10 blocks. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns the defaults: stone base, water, cobblestone border, cobblestone wall, cobblestone rubble, stone-brick filler. */
    public static StyleBlocks defaults() {
        return builder().build();
    }

    /** Returns the base block filled under streets: {@code minecraft:stone}, an ore host on purpose (D18). */
    public BlockRef base() {
        return base;
    }

    /** Returns the liquid used below the water level: {@code minecraft:water} by default. */
    public BlockRef liquid() {
        return liquid;
    }

    /** Returns the city style's border block, written along street edges and building skirts. */
    public BlockRef border() {
        return border;
    }

    /** Returns the city style's wall block, the low wall on top of a street border. */
    public BlockRef wall() {
        return wall;
    }

    /** Returns the building's rubble block (its {@code rubbleBlock}, else its {@code fillerBlock}). */
    public BlockRef rubble() {
        return rubble;
    }

    /** Returns the building's filler block, put at the lowest level where it would float over air. */
    public BlockRef filler() {
        return filler;
    }

    /** Returns the iron-bars block of the city style. */
    public BlockRef ironBars() {
        return ironBars;
    }

    /** Returns the leaf blocks ruins and the rubble layer draw from. */
    public List<BlockRef> leaves() {
        return leaves;
    }

    /** Returns the rubble blocks the street rubble layer draws from. */
    public List<BlockRef> rubbleDirt() {
        return rubbleDirt;
    }

    /**
     * Returns the damaged form of a block (upstream's palette {@code damaged} entry, usually iron bars), or
     * {@code null} when it has none.
     *
     * @param block the intact block
     * @return its damaged form, or {@code null}
     */
    public BlockRef damagedForm(BlockRef block) {
        return damaged.get(block);
    }

    /** Mutable builder for {@link StyleBlocks}. */
    public static final class Builder {

        private BlockRef base = BlockRef.of("minecraft:stone", 0);
        private BlockRef liquid = BlockRef.of("minecraft:water", 0);
        private BlockRef border = BlockRef.of("minecraft:cobblestone", 0);
        private BlockRef wall = BlockRef.of("minecraft:cobblestone_wall", 0);
        private BlockRef rubble = BlockRef.of("minecraft:cobblestone", 0);
        private BlockRef filler = BlockRef.of("minecraft:stonebrick", 0);
        private BlockRef ironBars = BlockRef.of("minecraft:iron_bars", 0);
        private List<BlockRef> leaves = DEFAULT_LEAVES;
        private List<BlockRef> rubbleDirt = DEFAULT_RUBBLE_DIRT;
        private final Map<BlockRef, BlockRef> damaged = new HashMap<>();

        private Builder() {}

        /** Sets the base block. */
        public Builder base(BlockRef value) {
            this.base = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the liquid. */
        public Builder liquid(BlockRef value) {
            this.liquid = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the border block. */
        public Builder border(BlockRef value) {
            this.border = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the wall block. */
        public Builder wall(BlockRef value) {
            this.wall = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the rubble block. */
        public Builder rubble(BlockRef value) {
            this.rubble = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the filler block. */
        public Builder filler(BlockRef value) {
            this.filler = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the iron-bars block. */
        public Builder ironBars(BlockRef value) {
            this.ironBars = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the leaf table; at least one entry. */
        public Builder leaves(List<BlockRef> value) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("leaf table is empty");
            }
            this.leaves = value;
            return this;
        }

        /** Sets the rubble table; at least one entry. */
        public Builder rubbleDirt(List<BlockRef> value) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("rubble table is empty");
            }
            this.rubbleDirt = value;
            return this;
        }

        /** Declares that {@code block} breaks into {@code damagedForm} under light damage. */
        public Builder damaged(BlockRef block, BlockRef damagedForm) {
            damaged.put(Objects.requireNonNull(block), Objects.requireNonNull(damagedForm));
            return this;
        }

        /** Returns the immutable value. */
        public StyleBlocks build() {
            return new StyleBlocks(this);
        }
    }

    private static List<BlockRef> defaultLeaves() {
        // 1.7.10 leaves: meta 0 oak, 1 spruce, 3 jungle; +4 is the no-decay bit (D19).
        List<BlockRef> out = new ArrayList<>();
        addCopies(out, BlockRef.of("minecraft:leaves", 3 | 4), 20);
        addCopies(out, BlockRef.of("minecraft:leaves", 1 | 4), 20);
        addCopies(out, BlockRef.of("minecraft:leaves", 4), 88);
        return out;
    }

    private static List<BlockRef> defaultRubbleDirt() {
        List<BlockRef> out = new ArrayList<>();
        addCopies(out, BlockRef.of("minecraft:stonebrick", 1), 20);
        addCopies(out, BlockRef.of("minecraft:mossy_cobblestone", 0), 40);
        addCopies(out, BlockRef.of("chisel:moss", 0), 68);
        return out;
    }

    private static void addCopies(List<BlockRef> out, BlockRef block, int count) {
        for (int i = 0; i < count; i++) {
            out.add(block);
        }
    }
}
