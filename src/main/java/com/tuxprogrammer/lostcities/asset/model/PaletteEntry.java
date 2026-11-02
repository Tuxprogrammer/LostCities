package com.tuxprogrammer.lostcities.asset.model;

import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;

/**
 * One character of a palette: what it places, the block it turns into when damaged, and its information (loot, mob,
 * torch, tile-entity template) (docs/original-spec/05-assets/palettes-and-variants.md, "Palette JSON").
 *
 * <p>
 * Exactly one of the four kinds holds: a single block, a random list (an inline {@code blocks} list or a variant), or
 * a reference to another character ({@code frompalette}). Immutable. Side: both.
 */
public final class PaletteEntry {

    /** What an entry places. */
    public enum Kind {
        /** One block. */
        BLOCK,
        /** A 128-slot random list, from {@code blocks} or a variant. */
        RANDOM,
        /** Whatever another character resolves to after every layer ({@code frompalette}). */
        REFERENCE
    }

    private final char character;
    private final Kind kind;
    private final BlockChoice block;
    private final RandomList random;
    private final char reference;
    private final BlockChoice damaged;
    private final Condition loot;
    private final Condition mob;
    private final boolean torch;
    private final TileTemplate tag;

    PaletteEntry(char character, Kind kind, BlockChoice block, RandomList random, char reference, BlockChoice damaged,
        Condition loot, Condition mob, boolean torch, TileTemplate tag) {
        this.character = character;
        this.kind = kind;
        this.block = block;
        this.random = random;
        this.reference = reference;
        this.damaged = damaged;
        this.loot = loot;
        this.mob = mob;
        this.torch = torch;
        this.tag = tag;
    }

    /** Returns the character. */
    public char character() {
        return character;
    }

    /** Returns what the entry places. */
    public Kind kind() {
        return kind;
    }

    /** Returns the block of a {@link Kind#BLOCK} entry, else null. */
    public BlockChoice block() {
        return block;
    }

    /** Returns the list of a {@link Kind#RANDOM} entry, else null. */
    public RandomList random() {
        return random;
    }

    /** Returns the character a {@link Kind#REFERENCE} entry copies. */
    public char reference() {
        return reference;
    }

    /** Returns the block this character turns into when damaged, or null (it belongs to this character only). */
    public BlockChoice damaged() {
        return damaged;
    }

    /** Returns the loot condition, or null. */
    public Condition loot() {
        return loot;
    }

    /** Returns the spawner mob condition, or null. */
    public Condition mob() {
        return mob;
    }

    /** Returns whether the block is a torch re-oriented after generation. */
    public boolean torch() {
        return torch;
    }

    /** Returns the tile-entity template, or null. */
    public TileTemplate tag() {
        return tag;
    }

    /** Returns whether the entry carries any information (loot, mob, torch or a template). */
    public boolean hasInfo() {
        return loot != null || mob != null || torch || tag != null;
    }
}
