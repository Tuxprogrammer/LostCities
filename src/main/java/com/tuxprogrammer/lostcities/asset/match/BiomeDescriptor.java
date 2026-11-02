package com.tuxprogrammer.lostcities.asset.match;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * The facts a biome matcher tests, as plain data: the biome's Forge {@code BiomeDictionary} types, its
 * {@code biomeName}, and, where RWG shapes the terrain, the realistic biome that generated the column
 * (docs/new-spec/05-assets/asset-format.md, "Biome matchers"; P05-6, D31). The glue builds one from the world's biome
 * provider; {@code compat.rwg} supplies the realistic biome, and leaves it null when neither RWG version is loaded.
 *
 * <p>
 * Immutable. Side: both (built on the server).
 */
public final class BiomeDescriptor {

    /** A biome with no facts at all: matches only matchers without clauses. */
    public static final BiomeDescriptor NONE = new BiomeDescriptor(Collections.<String>emptySet(), "", null);

    private final Set<String> types;
    private final String name;
    private final String realisticBiome;

    private BiomeDescriptor(Set<String> types, String name, String realisticBiome) {
        this.types = types;
        this.name = name;
        this.realisticBiome = realisticBiome;
    }

    /**
     * Creates a descriptor.
     *
     * @param types          the {@code BiomeDictionary.Type} names, any case
     * @param name           the biome's {@code biomeName}
     * @param realisticBiome the RWG realistic biome in lower snake case ({@code canyon_forest}), or null
     * @return the descriptor
     */
    public static BiomeDescriptor of(Set<String> types, String name, String realisticBiome) {
        Set<String> upper = new TreeSet<>();
        for (String type : types) {
            upper.add(type.toUpperCase(Locale.ROOT));
        }
        return new BiomeDescriptor(
            Collections.unmodifiableSet(upper),
            name == null ? "" : name,
            realisticBiome == null ? null : realisticBiome.toLowerCase(Locale.ROOT));
    }

    /** Returns the dictionary types, upper case, sorted. */
    public Set<String> types() {
        return types;
    }

    /** Returns the biome name. */
    public String name() {
        return name;
    }

    /** Returns the RWG realistic biome in lower snake case, or null when RWG did not shape this column. */
    public String realisticBiome() {
        return realisticBiome;
    }

    @Override
    public String toString() {
        return name + types + (realisticBiome == null ? "" : " rwg:" + realisticBiome);
    }
}
