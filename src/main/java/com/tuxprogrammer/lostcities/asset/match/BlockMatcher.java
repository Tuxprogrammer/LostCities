// spotless:off
package com.tuxprogrammer.lostcities.asset.match;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/regassets/data/BlockMatcher.java
// Avoids: CORE-20
// spotless:on

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A stuff asset's test on the block below or above its column: {@code if_all}, {@code if_any} and {@code excluding},
 * each a list of entries (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Block matcher"). No clauses
 * means any block. Blocks compare by registry name, not metadata, as upstream compares blocks.
 *
 * <p>
 * Deviation: an entry is a block reference (a direct {@code modid:name:meta} or a mapped 1.20 name, which matches every
 * block of its mapping chain that exists) or an ore-dictionary name {@code ore:<name>} in place of a block tag;
 * {@code #forge:stone} converts to {@code ore:stone} (D19; P05-4, D31). An unknown block is reported at load and
 * matches nothing, never air (fixes CORE-20, which upstream also files as ASSET-13). Immutable. Side: both.
 */
public final class BlockMatcher {

    /** The matcher that accepts every block. */
    public static final BlockMatcher ANY = new BlockMatcher(
        Collections.<Entry>emptyList(),
        Collections.<Entry>emptyList(),
        Collections.<Entry>emptyList());

    /** One entry: a set of registry names, or an ore-dictionary name. */
    public static final class Entry {

        private final Set<String> blocks;
        private final String oreName;

        Entry(Set<String> blocks, String oreName) {
            this.blocks = blocks;
            this.oreName = oreName;
        }

        boolean matches(BlockRef block, Set<String> oreNames) {
            if (oreName != null) {
                return oreNames.contains(oreName);
            }
            return blocks.contains(block.name());
        }

        @Override
        public String toString() {
            return oreName != null ? "ore:" + oreName : blocks.toString();
        }
    }

    private final List<Entry> ifAll;
    private final List<Entry> ifAny;
    private final List<Entry> excluding;

    private BlockMatcher(List<Entry> ifAll, List<Entry> ifAny, List<Entry> excluding) {
        this.ifAll = ifAll;
        this.ifAny = ifAny;
        this.excluding = excluding;
    }

    /**
     * Reads a matcher object.
     *
     * @param fields   the object
     * @param resolver the block resolver
     * @return the matcher; unknown entries are reported and match nothing
     */
    public static BlockMatcher parse(JsonFields fields, BlockResolver resolver) {
        return new BlockMatcher(
            clause(fields, "if_all", resolver),
            clause(fields, "if_any", resolver),
            clause(fields, "excluding", resolver));
    }

    /**
     * Tests a block.
     *
     * @param block    the block
     * @param oreNames the block's ore-dictionary names (the glue looks them up)
     * @return whether the block matches every {@code if_all} entry, at least one {@code if_any} entry when there are
     *         any, and no {@code excluding} entry
     */
    public boolean matches(BlockRef block, Set<String> oreNames) {
        for (Entry entry : ifAll) {
            if (!entry.matches(block, oreNames)) {
                return false;
            }
        }
        if (!ifAny.isEmpty()) {
            boolean any = false;
            for (Entry entry : ifAny) {
                if (entry.matches(block, oreNames)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        for (Entry entry : excluding) {
            if (entry.matches(block, oreNames)) {
                return false;
            }
        }
        return true;
    }

    private static List<Entry> clause(JsonFields fields, String key, BlockResolver resolver) {
        List<String> entries = fields.stringList(key, false);
        if (entries == null) {
            return Collections.emptyList();
        }
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String text = entries.get(i);
            String path = fields.field(key) + "[" + i + "]";
            if (text.startsWith("ore:") && text.length() > 4) {
                out.add(new Entry(Collections.<String>emptySet(), text.substring(4)));
                continue;
            }
            if (text.startsWith("#")) {
                fields.scope()
                    .error(
                        path,
                        text,
                        "block tags have no 1.7.10 meaning; write an ore-dictionary name, such as ore:stone for #forge:stone");
                out.add(new Entry(Collections.<String>emptySet(), null));
                continue;
            }
            Set<String> names = resolver.resolveForMatch(text, fields.scope(), path);
            out.add(
                new Entry(
                    names == null ? Collections.<String>emptySet()
                        : Collections.unmodifiableSet(new LinkedHashSet<>(names)),
                    null));
        }
        return Collections.unmodifiableList(out);
    }
}
