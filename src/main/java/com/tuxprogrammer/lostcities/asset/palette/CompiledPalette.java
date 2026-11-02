package com.tuxprogrammer.lostcities.asset.palette;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/CompiledPalette.java
// Avoids: ASSET-02, ASSET-11, ASSET-12, ASSET-30

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.RandomList;

/**
 * The palette of one chunk, or of one part placed in a chunk: the layers in order (style palettes group by group, then
 * the building's palette, then the part's), compiled into one lookup from character to block
 * (docs/original-spec/05-assets/palettes-and-variants.md, "Compiling a palette"). Later layers override earlier ones
 * character by character.
 *
 * <p>
 * Changes from upstream, each a bug fix (docs/new-spec/05-assets/index.md, "Palettes and variants"):
 * <ul>
 * <li>A {@code frompalette} reference is resolved after every layer is applied, so it copies its target's final
 * value: a building that redefines {@code #} changes the {@code @} columns that copy it (fixes ASSET-11). A reference
 * that a later layer redefines with a block is replaced like any other entry. References follow chains; a reference
 * whose target is never defined, or that loops, leaves its character undefined.</li>
 * <li>A character's information (loot, mob, torch, template) always comes from the layer that last defined the
 * character (fixes ASSET-12).</li>
 * <li>A damaged target belongs to the character whose entry declares it; a reference copies its target's damaged
 * target unless it declares its own (fixes ASSET-30).</li>
 * <li>{@link #getAll(char)} returns the distinct blocks of a random character instead of throwing on duplicates (fixes
 * ASSET-02).</li>
 * </ul>
 * Blocks are resolved for the world's decorative-metal flags when the palette is compiled, so a lookup is one array
 * read. Immutable; a part's palette is {@link #withLayer(Palette)}, which callers cache per (chunk palette, part)
 * (fixes ASSET-26 for them). Side: both.
 */
public final class CompiledPalette {

    private final List<Palette> layers;
    private final MetalFlags flags;
    private final Map<Character, BlockChoice[]> blocks;
    private final Map<Character, BlockChoice> damaged;
    private final Map<Character, PaletteEntry> info;

    private CompiledPalette(List<Palette> layers, MetalFlags flags, Map<Character, BlockChoice[]> blocks,
        Map<Character, BlockChoice> damaged, Map<Character, PaletteEntry> info) {
        this.layers = layers;
        this.flags = flags;
        this.blocks = blocks;
        this.damaged = damaged;
        this.info = info;
    }

    /**
     * Compiles layers.
     *
     * @param flags  the world's decorative-metal flags
     * @param layers the palettes, lowest first; null entries are skipped
     * @return the compiled palette
     */
    public static CompiledPalette compile(MetalFlags flags, List<Palette> layers) {
        List<Palette> kept = new ArrayList<>();
        Map<Character, PaletteEntry> last = new LinkedHashMap<>();
        for (Palette layer : layers) {
            if (layer == null) {
                continue;
            }
            kept.add(layer);
            last.putAll(layer.entries());
        }
        Map<Character, BlockChoice[]> blocks = new TreeMap<>();
        Map<Character, BlockChoice> damaged = new TreeMap<>();
        Map<Character, PaletteEntry> info = new TreeMap<>();
        for (Map.Entry<Character, PaletteEntry> entry : last.entrySet()) {
            PaletteEntry defining = entry.getValue();
            if (defining.hasInfo()) {
                info.put(entry.getKey(), defining);
            }
            PaletteEntry target = follow(defining, last);
            if (target == null) {
                continue;
            }
            blocks.put(entry.getKey(), slots(target));
            BlockChoice damage = defining.damaged() != null ? defining.damaged() : target.damaged();
            if (damage != null) {
                damaged.put(entry.getKey(), damage);
            }
        }
        return new CompiledPalette(Collections.unmodifiableList(kept), flags, blocks, damaged, info);
    }

    /**
     * Compiles layers.
     *
     * @param flags  the world's decorative-metal flags
     * @param layers the palettes, lowest first
     * @return the compiled palette
     */
    public static CompiledPalette compile(MetalFlags flags, Palette... layers) {
        List<Palette> list = new ArrayList<>();
        Collections.addAll(list, layers);
        return compile(flags, list);
    }

    /**
     * Returns this palette with one more layer on top, compiled from every layer so references see it (a part's local
     * or referenced palette over the chunk palette).
     *
     * @param layer the top layer, or null for this palette itself
     * @return the compiled palette
     */
    public CompiledPalette withLayer(Palette layer) {
        if (layer == null) {
            return this;
        }
        List<Palette> list = new ArrayList<>(layers);
        list.add(layer);
        return compile(flags, list);
    }

    /** Returns the layers, lowest first. */
    public List<Palette> layers() {
        return layers;
    }

    /** Returns the decorative-metal flags the palette was compiled for. */
    public MetalFlags flags() {
        return flags;
    }

    /** Returns whether a character has a block. */
    public boolean isDefined(char c) {
        return blocks.containsKey(c);
    }

    /** Returns whether a character is a random list, which draws a slot per block placed. */
    public boolean isRandom(char c) {
        BlockChoice[] slots = blocks.get(c);
        return slots != null && slots.length > 1;
    }

    /**
     * Returns the block of a character, drawing a slot for a random character.
     *
     * @param c      the character
     * @param random the caller's random; drawn only for a random character ({@code nextInt(128)}, as upstream)
     * @return the block, or null when the character is undefined
     */
    public BlockRef get(char c, Random random) {
        BlockChoice choice = choice(c, random);
        return choice == null ? null : choice.block(flags);
    }

    /**
     * Returns the block of a character at a given slot, for callers that draw the slot themselves (upstream's
     * per-chunk palette index).
     *
     * @param c    the character
     * @param slot 0 to 127; ignored for a single-block character
     * @return the block, or null when the character is undefined
     */
    public BlockRef get(char c, int slot) {
        BlockChoice[] slots = blocks.get(c);
        if (slots == null) {
            return null;
        }
        return slots[slots.length == 1 ? 0 : slot].block(flags);
    }

    /**
     * Returns the resolved choice behind a character, for the writer's tile-entity template, rotation family and
     * hard-air test.
     *
     * @param c      the character
     * @param random the caller's random; drawn only for a random character
     * @return the choice, or null when the character is undefined
     */
    public BlockChoice choice(char c, Random random) {
        BlockChoice[] slots = blocks.get(c);
        if (slots == null) {
            return null;
        }
        return slots.length == 1 ? slots[0] : slots[random.nextInt(RandomList.SLOTS)];
    }

    /**
     * Returns every block a character can place, each once (fixes ASSET-02).
     *
     * @param c the character
     * @return the distinct blocks in slot order; empty when undefined
     */
    public Set<BlockRef> getAll(char c) {
        BlockChoice[] slots = blocks.get(c);
        if (slots == null) {
            return Collections.emptySet();
        }
        Set<BlockRef> out = new LinkedHashSet<>();
        for (BlockChoice slot : slots) {
            out.add(slot.block(flags));
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Returns the block a character turns into when damaged by explosions or ruins.
     *
     * @param c the character that placed the block
     * @return the damaged block, or null when this character declares none (fixes ASSET-30)
     */
    public BlockRef damaged(char c) {
        BlockChoice choice = damaged.get(c);
        return choice == null ? null : choice.block(flags);
    }

    /**
     * Returns the entry whose information applies to a character: loot, mob, torch and template.
     *
     * @param c the character
     * @return the entry of the layer that last defined the character, when it carries information; else null
     */
    public PaletteEntry info(char c) {
        return info.get(c);
    }

    /**
     * Returns the tile-entity template a character's block takes: the entry's {@code tag}, else the block-name map's
     * (the potted cactus's contents).
     *
     * @param c      the character
     * @param choice the block placed, from {@link #choice(char, Random)}
     * @return the template, or null
     */
    public TileTemplate template(char c, BlockChoice choice) {
        PaletteEntry entry = info.get(c);
        if (entry != null && entry.tag() != null) {
            return entry.tag();
        }
        return choice == null ? null : choice.tag();
    }

    /** Returns every defined character, sorted. */
    public Set<Character> characters() {
        return Collections.unmodifiableSet(blocks.keySet());
    }

    /**
     * Returns a canonical text form: one line per character, sorted, with its blocks as slot runs, its damaged block
     * and its information. Golden tests hash it.
     *
     * @return the text
     */
    public String describe() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Character, BlockChoice[]> entry : blocks.entrySet()) {
            char c = entry.getKey();
            out.append('\'')
                .append(c)
                .append("' ");
            BlockChoice[] slots = entry.getValue();
            BlockRef previous = null;
            int run = 0;
            List<String> runs = new ArrayList<>();
            for (BlockChoice slot : slots) {
                BlockRef block = slot.block(flags);
                if (block.equals(previous)) {
                    run++;
                    continue;
                }
                if (previous != null) {
                    runs.add(run + "x" + previous);
                }
                previous = block;
                run = 1;
            }
            runs.add(run + "x" + previous);
            out.append(String.join(",", runs));
            BlockRef damage = damaged(c);
            if (damage != null) {
                out.append(" damaged=")
                    .append(damage);
            }
            PaletteEntry information = info.get(c);
            if (information != null) {
                if (information.loot() != null) {
                    out.append(" loot=")
                        .append(
                            information.loot()
                                .id());
                }
                if (information.mob() != null) {
                    out.append(" mob=")
                        .append(
                            information.mob()
                                .id());
                }
                if (information.torch()) {
                    out.append(" torch");
                }
                if (information.tag() != null) {
                    out.append(" tag=")
                        .append(information.tag());
                }
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static PaletteEntry follow(PaletteEntry start, Map<Character, PaletteEntry> entries) {
        PaletteEntry current = start;
        Set<Character> seen = new LinkedHashSet<>();
        while (current.kind() == PaletteEntry.Kind.REFERENCE) {
            if (!seen.add(current.character())) {
                return null;
            }
            current = entries.get(current.reference());
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static BlockChoice[] slots(PaletteEntry entry) {
        if (entry.kind() == PaletteEntry.Kind.BLOCK) {
            return new BlockChoice[] { entry.block() };
        }
        RandomList random = entry.random();
        BlockChoice[] slots = new BlockChoice[RandomList.SLOTS];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = random.slot(i);
        }
        return slots;
    }
}
