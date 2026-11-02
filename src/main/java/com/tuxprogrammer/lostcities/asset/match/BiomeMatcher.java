package com.tuxprogrammer.lostcities.asset.match;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/regassets/data/BiomeMatcher.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * Where a world style's city style, a scattered structure or a stuff column may appear, by the chunk's biome
 * (docs/original-spec/05-assets/world-styles.md, "Biome matcher"): {@code if_all}, {@code if_any} and
 * {@code excluding}, each a list of biome sets, keep upstream's meaning. Each set is one entry or a list of entries,
 * and a biome is in a set when it matches any of its entries.
 *
 * <p>
 * Deviation: entries are the 1.7.10 vocabulary of {@link BiomeEntry} instead of 1.20 biome ids and tags
 * (docs/new-spec/05-assets/index.md, "Biome matcher"; P05-6, decision log D31), because on RWG the provider reports
 * RWG's own base biomes and 1.20 selectors would never match. A matcher without clauses matches every biome.
 * Immutable. Side: both.
 */
public final class BiomeMatcher {

    /** The matcher that accepts every biome, used where an asset gives none. */
    public static final BiomeMatcher ANY = new BiomeMatcher(
        Collections.<List<BiomeEntry>>emptyList(),
        Collections.<List<BiomeEntry>>emptyList(),
        Collections.<List<BiomeEntry>>emptyList());

    private final List<List<BiomeEntry>> ifAll;
    private final List<List<BiomeEntry>> ifAny;
    private final List<List<BiomeEntry>> excluding;

    private BiomeMatcher(List<List<BiomeEntry>> ifAll, List<List<BiomeEntry>> ifAny, List<List<BiomeEntry>> excluding) {
        this.ifAll = ifAll;
        this.ifAny = ifAny;
        this.excluding = excluding;
    }

    /**
     * Reads a matcher object.
     *
     * @param fields the object; problems go to its scope
     * @return the matcher; clauses with bad entries keep their valid entries, and each bad entry is an error
     */
    public static BiomeMatcher parse(JsonFields fields) {
        return new BiomeMatcher(clause(fields, "if_all"), clause(fields, "if_any"), clause(fields, "excluding"));
    }

    /**
     * Builds a matcher from entry lists, one set per entry, for code and tests.
     *
     * @param ifAll     entries the biome must all match
     * @param ifAny     entries of which the biome must match one
     * @param excluding entries the biome must match none of
     * @return the matcher
     */
    public static BiomeMatcher of(List<BiomeEntry> ifAll, List<BiomeEntry> ifAny, List<BiomeEntry> excluding) {
        return new BiomeMatcher(singletons(ifAll), singletons(ifAny), singletons(excluding));
    }

    /** Returns whether the matcher has no clauses, so it accepts every biome. */
    public boolean isAny() {
        return ifAll.isEmpty() && ifAny.isEmpty() && excluding.isEmpty();
    }

    /**
     * Tests a biome.
     *
     * @param biome the chunk's biome facts
     * @return whether the biome is in every {@code if_all} set, in at least one {@code if_any} set (when there are
     *         any), and in no {@code excluding} set
     */
    public boolean matches(BiomeDescriptor biome) {
        for (List<BiomeEntry> set : ifAll) {
            if (!inSet(biome, set)) {
                return false;
            }
        }
        if (!ifAny.isEmpty()) {
            boolean any = false;
            for (List<BiomeEntry> set : ifAny) {
                if (inSet(biome, set)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        for (List<BiomeEntry> set : excluding) {
            if (inSet(biome, set)) {
                return false;
            }
        }
        return true;
    }

    /** Returns every entry the matcher names, for reports. */
    public List<BiomeEntry> entries() {
        List<BiomeEntry> out = new ArrayList<>();
        for (List<List<BiomeEntry>> clause : java.util.Arrays.asList(ifAll, ifAny, excluding)) {
            for (List<BiomeEntry> set : clause) {
                out.addAll(set);
            }
        }
        return out;
    }

    private static boolean inSet(BiomeDescriptor biome, List<BiomeEntry> set) {
        for (BiomeEntry entry : set) {
            if (entry.matches(biome)) {
                return true;
            }
        }
        return false;
    }

    private static List<List<BiomeEntry>> singletons(List<BiomeEntry> entries) {
        List<List<BiomeEntry>> out = new ArrayList<>();
        for (BiomeEntry entry : entries) {
            out.add(Collections.singletonList(entry));
        }
        return Collections.unmodifiableList(out);
    }

    private static List<List<BiomeEntry>> clause(JsonFields fields, String key) {
        JsonArray array = fields.array(key, false);
        if (array == null) {
            return Collections.emptyList();
        }
        List<List<BiomeEntry>> sets = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String path = fields.field(key) + "[" + i + "]";
            JsonElement element = array.get(i);
            List<BiomeEntry> set = new ArrayList<>();
            if (element.isJsonArray()) {
                JsonArray inner = element.getAsJsonArray();
                for (int j = 0; j < inner.size(); j++) {
                    add(fields, path + "[" + j + "]", inner.get(j), set);
                }
            } else {
                add(fields, path, element, set);
            }
            if (!set.isEmpty()) {
                sets.add(Collections.unmodifiableList(set));
            }
        }
        return Collections.unmodifiableList(sets);
    }

    private static void add(JsonFields fields, String path, JsonElement element, List<BiomeEntry> set) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive()
            .isString()) {
            fields.scope()
                .error(path, element.toString(), "must be a biome entry string");
            return;
        }
        try {
            set.add(BiomeEntry.parse(element.getAsString()));
        } catch (IllegalArgumentException e) {
            fields.scope()
                .error(path, element.getAsString(), e.getMessage());
        }
    }
}
