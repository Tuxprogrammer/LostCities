// spotless:off
package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/CompiledPalette.java
// spotless:on

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A random block list as variants and inline {@code blocks} entries write it: weighted entries filled into exactly
 * {@value #SLOTS} slots in list order (docs/original-spec/05-assets/palettes-and-variants.md, "Random lists"). So
 * {@code [32 A, 32 B, 1000 C]} is 32/128 A, 32/128 B, 64/128 C. A list that fills fewer than 128 slots is rejected at
 * load; entries past slot 128 are dropped, as upstream.
 *
 * <p>
 * Immutable. Side: both.
 */
public final class RandomList {

    /** The number of slots of every random list (upstream's 128). */
    public static final int SLOTS = 128;

    private final BlockChoice[] slots;

    private RandomList(BlockChoice[] slots) {
        this.slots = slots;
    }

    /**
     * Reads a {@code [{random, block}, ...]} list.
     *
     * @param fields    the object holding the list
     * @param key       the field ({@code blocks})
     * @param context   the asset being read
     * @param character the palette character the list belongs to, or null for a variant
     * @return the list, or null after reporting an error
     */
    public static RandomList parse(JsonFields fields, String key, ParseContext context, Character character) {
        List<JsonFields> entries = fields.objects(key, true);
        List<Integer> weights = new ArrayList<>();
        List<BlockChoice> blocks = new ArrayList<>();
        boolean ok = true;
        for (JsonFields entry : entries) {
            Integer weight = entry.integer("random", true);
            String text = entry.string("block", true);
            if (weight == null || text == null) {
                ok = false;
                continue;
            }
            if (weight < 0) {
                entry.error("random", Integer.toString(weight), "must not be negative");
                ok = false;
                continue;
            }
            BlockChoice choice = context.block(text, character, entry.field("block"));
            if (choice == null) {
                ok = false;
                continue;
            }
            weights.add(weight);
            blocks.add(choice);
        }
        if (!ok) {
            return null;
        }
        RandomList list = fill(weights, blocks);
        if (list == null) {
            fields.error(key, null, "the weights fill fewer than " + SLOTS + " slots; the last entry is usually 1000");
        }
        return list;
    }

    /**
     * Fills the slots from weights and blocks, in order.
     *
     * @param weights the slot count of each entry
     * @param blocks  the block of each entry
     * @return the list, or null when fewer than {@value #SLOTS} slots are filled
     */
    public static RandomList fill(List<Integer> weights, List<BlockChoice> blocks) {
        BlockChoice[] slots = new BlockChoice[SLOTS];
        int index = 0;
        for (int i = 0; i < weights.size() && index < SLOTS; i++) {
            for (int n = 0; n < weights.get(i) && index < SLOTS; n++) {
                slots[index++] = blocks.get(i);
            }
        }
        return index < SLOTS ? null : new RandomList(slots);
    }

    /**
     * Returns a slot.
     *
     * @param index 0 to {@value #SLOTS} − 1
     * @return the block in that slot
     */
    public BlockChoice slot(int index) {
        return slots[index];
    }

    /** Returns the distinct blocks of the list, in slot order (fixes ASSET-02's duplicate crash for callers). */
    public Set<BlockChoice> distinct() {
        Set<BlockChoice> out = new LinkedHashSet<>();
        Collections.addAll(out, slots);
        return out;
    }

    /** Returns how many slots hold each distinct block, in slot order, for reports and goldens. */
    public List<String> describe() {
        List<String> out = new ArrayList<>();
        BlockChoice current = null;
        int run = 0;
        for (BlockChoice slot : slots) {
            if (slot.equals(current)) {
                run++;
                continue;
            }
            if (current != null) {
                out.add(run + "x" + current.block());
            }
            current = slot;
            run = 1;
        }
        out.add(run + "x" + current.block());
        return out;
    }
}
