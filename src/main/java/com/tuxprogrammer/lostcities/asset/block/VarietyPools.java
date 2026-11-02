package com.tuxprogrammer.lostcities.asset.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.match.WeightedPick;

/**
 * The block catalog's variety pools as data: for each upstream palette role, weighted candidates drawn from the pack's
 * decorative blocks, each falling back to the D19 baseline when its mod is absent (D25, D28, D29;
 * docs/new-spec/05-assets/block-catalog/index.md, "Variety pools"). Read from
 * {@code data/lostcitiesnh/blockmap/pools.json}, which {@code tools/asset-convert/pools_from_catalog.py} builds from
 * the catalog's tables, so the catalog stays the one place the pools are written.
 *
 * <p>
 * M02 loads, validates and resolves the pools against the live registry, so the real-pack report names every
 * candidate whose blocks do not exist; turning pools into palettes, per building by weight, is M11's
 * ({@link Pool#pick(Random)} is the draw it will use). Rubble sets and set-piece rows are kept as data with their
 * blocks.
 * Immutable once resolved. Side: both.
 */
public final class VarietyPools {

    /** The bundled data file, as a class-path resource. */
    public static final String RESOURCE = "/data/lostcitiesnh/blockmap/pools.json";

    /** The catalog's gameplay flags (block catalog, "How the pools are written"). */
    public enum Flag {
        /** Spawn-proof. */
        SP,
        /** Speed block. */
        SPD,
        /** GT boulder host. */
        BH,
        /** Tile entity: the entry may need tile-entity data. */
        TE,
        /** Emits light. */
        L,
        /** Falls like sand. */
        G,
        /** Slow to mine. */
        H,
        /** Extended metadata above 15. */
        XM,
        /** Tinted at render time. */
        T,
        /** Decorative metal (D29). */
        DM
    }

    /** One weighted candidate of a pool. */
    public static final class Candidate {

        private final String label;
        private final int weight;
        private final boolean baseline;
        private final List<BlockRef> blocks;
        private final List<BlockRef> fallback;
        private final boolean fallbackToBaseline;
        private final Set<Flag> flags;
        private final List<String> missing;

        Candidate(String label, int weight, boolean baseline, List<BlockRef> blocks, List<BlockRef> fallback,
            boolean fallbackToBaseline, Set<Flag> flags, List<String> missing) {
            this.label = label;
            this.weight = weight;
            this.baseline = baseline;
            this.blocks = blocks;
            this.fallback = fallback;
            this.fallbackToBaseline = fallbackToBaseline;
            this.flags = flags;
            this.missing = missing;
        }

        /** Returns the catalog's description of the candidate. */
        public String label() {
            return label;
        }

        /** Returns the weight, the share of buildings (or slots) that take it. */
        public int weight() {
            return weight;
        }

        /** Returns whether this is the D19 baseline. */
        public boolean baseline() {
            return baseline;
        }

        /** Returns the blocks the candidate names, in catalog order. */
        public List<BlockRef> blocks() {
            return blocks;
        }

        /** Returns the named fallback blocks; empty when it falls back to the baseline. */
        public List<BlockRef> fallback() {
            return fallback;
        }

        /** Returns whether an unavailable candidate falls back to the pool's baseline. */
        public boolean fallbackToBaseline() {
            return fallbackToBaseline;
        }

        /** Returns the flags. */
        public Set<Flag> flags() {
            return flags;
        }

        /** Returns the registry names of the blocks that are not registered; empty when the candidate is available. */
        public List<String> missing() {
            return missing;
        }

        /** Returns whether every block of the candidate is registered. */
        public boolean available() {
            return missing.isEmpty();
        }
    }

    /** One pool: a role's weighted candidates, or a table of rubble sets or set-piece characters. */
    public static final class Pool {

        private final String name;
        private final String title;
        private final List<Candidate> candidates;

        Pool(String name, String title, List<Candidate> candidates) {
            this.name = name;
            this.title = title;
            this.candidates = candidates;
        }

        /** Returns the pool's key, the catalog heading in lower kebab case. */
        public String name() {
            return name;
        }

        /** Returns the catalog heading. */
        public String title() {
            return title;
        }

        /** Returns the candidates, baseline first, in catalog order. */
        public List<Candidate> candidates() {
            return candidates;
        }

        /**
         * Draws one candidate by weight among the available ones, as a building picks its variety.
         *
         * @param random the building random
         * @return the candidate, or null when the pool has no weighted candidates
         */
        public Candidate pick(Random random) {
            return WeightedPick.pick(random, candidates, c -> c.available() ? c.weight : 0);
        }
    }

    private final Map<String, Pool> pools;

    private VarietyPools(Map<String, Pool> pools) {
        this.pools = pools;
    }

    /**
     * Reads and resolves the pools.
     *
     * @param json     the parsed {@code pools.json}
     * @param registry the live registry
     * @param scope    where problems go; an unregistered block is a note, not a fault
     * @return the pools
     */
    public static VarietyPools parse(JsonElement json, BlockRegistryView registry, Problems.Scope scope) {
        Map<String, Pool> pools = new LinkedHashMap<>();
        if (!json.isJsonObject()) {
            scope.error("", null, "must be an object");
            return new VarietyPools(pools);
        }
        JsonFields root = new JsonFields(json.getAsJsonObject(), "", scope);
        for (JsonFields pool : root.objects("pools", true)) {
            String name = pool.string("name", true);
            String title = pool.string("title", true);
            if (name == null || title == null) {
                continue;
            }
            List<Candidate> candidates = new ArrayList<>();
            for (JsonFields candidate : pool.objects("candidates", false)) {
                Candidate parsed = candidate(candidate, registry);
                if (parsed != null) {
                    candidates.add(parsed);
                }
            }
            for (String key : new String[] { "sets", "characters" }) {
                for (JsonFields row : pool.objects(key, false)) {
                    Candidate parsed = candidate(row, registry);
                    if (parsed != null) {
                        candidates.add(parsed);
                    }
                }
            }
            if (pools.put(name, new Pool(name, title, Collections.unmodifiableList(candidates))) != null) {
                pool.error("name", name, "is used by two pools");
            }
        }
        return new VarietyPools(Collections.unmodifiableMap(pools));
    }

    /** Returns a pool by name, or null. */
    public Pool pool(String name) {
        return pools.get(name);
    }

    /** Returns every pool, in catalog order. */
    public Map<String, Pool> pools() {
        return pools;
    }

    /** Returns every candidate block that is not registered, as {@code pool: label: name}, for the report. */
    public List<String> unresolved() {
        List<String> out = new ArrayList<>();
        for (Pool pool : pools.values()) {
            for (Candidate candidate : pool.candidates) {
                for (String name : candidate.missing) {
                    out.add(pool.name + ": " + name);
                }
            }
        }
        return out;
    }

    /** Returns how many distinct candidate blocks the pools name. */
    public int blockCount() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (Pool pool : pools.values()) {
            for (Candidate candidate : pool.candidates) {
                for (BlockRef block : candidate.blocks) {
                    names.add(block.name());
                }
            }
        }
        return names.size();
    }

    private static Candidate candidate(JsonFields fields, BlockRegistryView registry) {
        String label = fields.string("label", true);
        if (label == null) {
            return null;
        }
        int weight = fields.integer("weight", 0);
        boolean baseline = fields.flag("baseline", false);
        List<BlockRef> blocks = refs(fields, "blocks");
        List<BlockRef> fallback = refs(fields, "fallback");
        boolean toBaseline = fields.flag("fallbackToBaseline", false);
        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        List<String> flagNames = fields.stringList("flags", false);
        if (flagNames != null) {
            for (String flag : flagNames) {
                String key = flag.startsWith("L") && flag.length() > 1 && Character.isDigit(flag.charAt(1)) ? "L"
                    : flag;
                try {
                    flags.add(Flag.valueOf(key));
                } catch (IllegalArgumentException e) {
                    fields.error("flags", flag, "is not a catalog flag");
                }
            }
        }
        List<String> missing = new ArrayList<>();
        for (BlockRef block : blocks) {
            if (!registry.exists(block.name()) && !missing.contains(block.name())) {
                missing.add(block.name());
            }
        }
        return new Candidate(
            label,
            weight,
            baseline,
            Collections.unmodifiableList(blocks),
            Collections.unmodifiableList(fallback),
            toBaseline,
            Collections.unmodifiableSet(flags),
            Collections.unmodifiableList(missing));
    }

    private static List<BlockRef> refs(JsonFields fields, String key) {
        List<BlockRef> out = new ArrayList<>();
        List<String> texts = fields.stringList(key, false);
        if (texts == null) {
            return out;
        }
        for (String text : texts) {
            try {
                out.add(BlockRef.parse(text));
            } catch (IllegalArgumentException e) {
                fields.error(key, text, e.getMessage());
            }
        }
        return out;
    }
}
