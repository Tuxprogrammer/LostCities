package com.tuxprogrammer.lostcities.asset.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.Problems;

/**
 * Turns the block strings of assets into {@link BlockChoice}s, once, at load: through the block-name map for a 1.20
 * name, directly for a {@code modid:name:meta} reference, then against the live registry, the never-place list and the
 * decorative-metal catalog (docs/new-spec/05-assets/asset-format.md, "Block references"; D14, D19, D28, D29, D31).
 *
 * <p>
 * Rules it enforces:
 * <ul>
 * <li>An unknown name, in neither the map nor the registry, is an error with file and field, never air (fixes
 * CORE-20, which upstream calls ASSET-13 too). So is 1.12's {@code name@meta} form (fixes ASSET-03).</li>
 * <li>A mapped name takes the first block of its chain that exists; each fallback taken is recorded, with the block
 * that was missing, for the dev self-check and the real-pack report.</li>
 * <li>A block on the never-place list is rejected; in a chain the next block is tried, and a direct reference has none,
 * so the entry fails. Treasure characters may hold valuable storage blocks, which are loot.</li>
 * <li>A decorative-metal block records its metal and the block to use while that flag is off: the entry's own
 * {@code metalOff} chain, or the map's generic metal fallback. Treasure ignores the flags.</li>
 * </ul>
 * Results are cached per string and context, so a name used in a hundred parts is resolved, and its fallback recorded,
 * once. Side: both; runs where the registry is complete, never needs a world (fixes ASSET-14).
 */
public final class BlockResolver {

    private final BlockMapping mapping;
    private final MetalCatalog metals;
    private final NeverPlaceList neverPlace;
    private final BlockRegistryView registry;
    private final Map<String, Outcome> cache = new HashMap<>();
    private final Map<String, Fallback> fallbacks = new TreeMap<>();

    /**
     * Creates a resolver.
     *
     * @param mapping    the block-name map
     * @param metals     the decorative-metal catalog
     * @param neverPlace the never-place list
     * @param registry   the live registry
     */
    public BlockResolver(BlockMapping mapping, MetalCatalog metals, NeverPlaceList neverPlace,
        BlockRegistryView registry) {
        this.mapping = mapping;
        this.metals = metals;
        this.neverPlace = neverPlace;
        this.registry = registry;
    }

    /** Returns the block-name map. */
    public BlockMapping mapping() {
        return mapping;
    }

    /** Returns the live registry view. */
    public BlockRegistryView registry() {
        return registry;
    }

    /**
     * Resolves a block string that will be placed.
     *
     * @param text      the string as the asset writes it
     * @param asset     the asset naming it, for scoped rows and treasure; null outside assets
     * @param character the palette character it belongs to, for treasure; null when not a palette entry
     * @param scope     where problems go
     * @param field     the JSON path of the field, for messages
     * @return the choice, or null when the string cannot be resolved (an error was reported)
     */
    public BlockChoice resolve(String text, AssetId asset, Character character, Problems.Scope scope, String field) {
        boolean treasure = asset != null && character != null && mapping.isTreasure(asset, character);
        String key = text + "\u0000" + asset + "\u0000" + treasure;
        Outcome outcome = cache.get(key);
        if (outcome == null) {
            outcome = compute(text, asset, treasure);
            cache.put(key, outcome);
        }
        for (String warning : outcome.warnings) {
            scope.warning(field, text, warning);
        }
        if (outcome.error != null) {
            scope.error(field, text, outcome.error);
            return null;
        }
        return outcome.choice;
    }

    /**
     * Resolves a block-matcher entry: a direct reference or a 1.20 name, which matches every block of its chain that
     * exists (block matchers compare blocks, not metadata, as upstream's do).
     *
     * @param text  the entry
     * @param scope where problems go
     * @param field the JSON path, for messages
     * @return the registry names that match; empty when none exists, and null when the entry is unknown (an error was
     *         reported, and the entry matches nothing)
     */
    public Set<String> resolveForMatch(String text, Problems.Scope scope, String field) {
        Set<String> out = new LinkedHashSet<>();
        if (BlockRef.isDirectForm(text) || !text.contains(":")) {
            BlockRef ref;
            try {
                ref = BlockRef.isDirectForm(text) ? BlockRef.parse(text) : null;
            } catch (IllegalArgumentException e) {
                ref = null;
            }
            if (ref == null) {
                scope.error(field, text, "not a block reference");
                return null;
            }
            if (!registry.exists(ref.name())) {
                scope.error(field, text, "block is not registered; the entry matches nothing");
                return null;
            }
            out.add(ref.name());
            return out;
        }
        BlockName name;
        try {
            name = BlockName.parse(text);
        } catch (IllegalArgumentException e) {
            scope.error(field, text, e.getMessage());
            return null;
        }
        MappingRow row = mapping.find(name.id(), null);
        if (row == null) {
            if (registry.exists(name.id())) {
                out.add(name.id());
                return out;
            }
            scope.error(field, text, "not in the block-name map and not registered; the entry matches nothing");
            return null;
        }
        for (MappingRow.Candidate candidate : row.chain()) {
            if (registry.exists(
                candidate.base()
                    .name())) {
                out.add(
                    candidate.base()
                        .name());
            }
        }
        if (out.isEmpty()) {
            scope.note(field, text, "no block of its mapping exists here, so the entry matches nothing");
        }
        return out;
    }

    /** Returns every fallback taken so far, one per mapped name and missing block, sorted. */
    public List<Fallback> fallbacks() {
        return Collections.unmodifiableList(new ArrayList<>(fallbacks.values()));
    }

    private Outcome compute(String text, AssetId asset, boolean treasure) {
        if (text == null || text.isEmpty()) {
            return Outcome.error("empty block name");
        }
        if (BlockRef.isDirectForm(text)) {
            return direct(text, treasure);
        }
        BlockName name;
        try {
            name = BlockName.parse(text);
        } catch (IllegalArgumentException e) {
            return Outcome.error("not a block name: " + e.getMessage() + "; it takes no block, never air");
        }
        MappingRow row = mapping.find(name.id(), asset);
        if (row == null) {
            return Outcome.error(
                "'" + name.id()
                    + "' is not in the block-name map; write a 1.7.10 block as modid:name:meta, metadata included");
        }
        if (row.hardAir()) {
            return Outcome.of(BlockChoice.HARD_AIR, Collections.<String>emptyList());
        }
        if (row.matchOnly()) {
            return Outcome.error("'" + name.id() + "' is mapped for block matchers only and cannot be placed");
        }
        List<String> warnings = new ArrayList<>();
        List<MappingRow.Candidate> chain = row.chain();
        if ("double".equals(
            name.properties()
                .get("type"))) {
            if (row.doubleChain()
                .isEmpty()) {
                return Outcome.error("'" + name.id() + "' has no double-slab block in the block-name map");
            }
            chain = row.doubleChain();
        }
        Picked picked = pick(row.id(), chain, name, treasure, warnings);
        if (picked.error != null) {
            return Outcome.error(picked.error);
        }
        DecorativeMetal metal = treasure ? null : metals.metalOf(picked.block);
        BlockRef metalOff = null;
        if (metal != null) {
            if (row.metal() != null && row.metal() == metal
                && !row.metalOff()
                    .isEmpty()) {
                Picked off = pick(row.id() + " (" + metal.key() + " off)", row.metalOff(), name, false, warnings);
                if (off.error != null) {
                    return Outcome.error(off.error);
                }
                metalOff = off.block;
            } else {
                metalOff = genericMetalFallback();
            }
        }
        BlockChoice choice = new BlockChoice(
            text,
            picked.block,
            metal,
            metalOff,
            picked.fallback,
            row.tag(),
            row.capExempt() && metal != null,
            treasure,
            picked.family);
        return Outcome.of(choice, warnings);
    }

    private Outcome direct(String text, boolean treasure) {
        BlockRef ref;
        try {
            ref = BlockRef.parse(text);
        } catch (IllegalArgumentException e) {
            return Outcome.error("not a modid:name:meta reference: " + e.getMessage());
        }
        if (!registry.exists(ref.name())) {
            return Outcome.error(
                "block '" + ref.name()
                    + "' is not registered (is its mod absent?); a direct reference has no fallback");
        }
        String reason = neverPlace.reason(ref, treasure);
        if (reason != null) {
            return Outcome.error("block " + ref + " is never placed (" + reason + ")");
        }
        DecorativeMetal metal = treasure ? null : metals.metalOf(ref);
        BlockRef metalOff = metal == null ? null : genericMetalFallback();
        BlockChoice choice = new BlockChoice(
            text,
            ref,
            metal,
            metalOff,
            false,
            null,
            false,
            treasure,
            mapping.familyOf(ref.name()));
        return Outcome.of(choice, Collections.<String>emptyList());
    }

    private BlockRef genericMetalFallback() {
        for (BlockRef ref : mapping.metalFallback()) {
            if (registry.exists(ref.name())) {
                return ref;
            }
        }
        return BlockRef.AIR;
    }

    private Picked pick(String rowId, List<MappingRow.Candidate> chain, BlockName name, boolean treasure,
        List<String> warnings) {
        MappingRow.Candidate primary = chain.get(0);
        for (int i = 0; i < chain.size(); i++) {
            MappingRow.Candidate candidate = chain.get(i);
            MetaRule.Result result = candidate.rule()
                .apply(name.properties());
            if (!result.errors()
                .isEmpty()) {
                return Picked.error(String.join("; ", result.errors()));
            }
            if (i == 0) {
                warnings.addAll(result.warnings());
            }
            BlockRef block;
            try {
                block = candidate.base()
                    .withMeta(
                        candidate.base()
                            .meta() + result.bits());
            } catch (IllegalArgumentException e) {
                return Picked.error(e.getMessage());
            }
            if (!registry.exists(block.name())) {
                continue;
            }
            String reason = neverPlace.reason(block, treasure);
            if (reason != null) {
                warnings.add("mapped block " + block + " is never placed (" + reason + "); the next block is used");
                continue;
            }
            if (i > 0) {
                String key = rowId + " "
                    + primary.base()
                        .name();
                fallbacks.computeIfAbsent(
                    key,
                    k -> new Fallback(
                        rowId,
                        primary.base()
                            .name(),
                        block.name()))
                    .count();
            }
            return Picked.of(
                block,
                i > 0,
                candidate.rule()
                    .family());
        }
        return Picked.error("no block of '" + rowId + "' exists or may be placed: " + chain);
    }

    /** A fallback the resolver took: a mapped name whose primary block is not registered. */
    public static final class Fallback {

        private final String mapped;
        private final String missing;
        private final String used;
        private int uses;

        Fallback(String mapped, String missing, String used) {
            this.mapped = mapped;
            this.missing = missing;
            this.used = used;
        }

        void count() {
            uses++;
        }

        /** Returns the 1.20 name (or the row label) whose primary was missing. */
        public String mapped() {
            return mapped;
        }

        /** Returns the registry name of the primary block that is not registered. */
        public String missing() {
            return missing;
        }

        /** Returns the registry name of the block used instead. */
        public String used() {
            return used;
        }

        /** Returns how many distinct block strings took this fallback. */
        public int uses() {
            return uses;
        }

        @Override
        public String toString() {
            return mapped + ": " + missing + " is not registered, using " + used;
        }
    }

    private static final class Picked {

        final BlockRef block;
        final boolean fallback;
        final RotationFamily family;
        final String error;

        private Picked(BlockRef block, boolean fallback, RotationFamily family, String error) {
            this.block = block;
            this.fallback = fallback;
            this.family = family;
            this.error = error;
        }

        static Picked of(BlockRef block, boolean fallback, RotationFamily family) {
            return new Picked(block, fallback, family, null);
        }

        static Picked error(String error) {
            return new Picked(null, false, RotationFamily.NONE, error);
        }
    }

    private static final class Outcome {

        final BlockChoice choice;
        final List<String> warnings;
        final String error;

        private Outcome(BlockChoice choice, List<String> warnings, String error) {
            this.choice = choice;
            this.warnings = warnings;
            this.error = error;
        }

        static Outcome of(BlockChoice choice, List<String> warnings) {
            return new Outcome(choice, warnings, null);
        }

        static Outcome error(String error) {
            return new Outcome(null, Collections.<String>emptyList(), error);
        }
    }
}
