package com.tuxprogrammer.lostcities.asset.match;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * One biome matcher entry in the 1.7.10 vocabulary (P05-6, decision log D31; docs/new-spec/05-assets/asset-format.md,
 * "Biome matchers"):
 * <ul>
 * <li>{@code type:<Type>}: the biome is registered with that Forge {@code BiomeDictionary.Type};</li>
 * <li>{@code name:<biome name>}: the biome's {@code biomeName} equals the text, case-insensitively;</li>
 * <li>{@code rwg:<realistic biome>}: through {@code compat.rwg}, the RWG realistic biome that generated the column,
 * named by its class without the {@code RealisticBiome} prefix in lower snake case; with neither RWG version loaded it
 * matches nothing.</li>
 * </ul>
 * 1.20 biome ids and tags ({@code minecraft:desert}, {@code #minecraft:is_ocean}) have no 1.7.10 meaning and are
 * rejected at parse with a hint at the conversion. Side: both.
 */
public final class BiomeEntry {

    /** What an entry tests. */
    public enum Kind {
        /** A {@code BiomeDictionary} type. */
        TYPE,
        /** A biome name. */
        NAME,
        /** An RWG realistic biome. */
        RWG
    }

    /**
     * Forge 10.13.4's {@code BiomeDictionary.Type} constants, so a typo in a type is caught at load in plain Java. The
     * dev self-check compares this list with the running Forge's enum.
     */
    public static final Set<String> KNOWN_TYPES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "HOT",
                "COLD",
                "SPARSE",
                "DENSE",
                "WET",
                "DRY",
                "SAVANNA",
                "CONIFEROUS",
                "JUNGLE",
                "SPOOKY",
                "DEAD",
                "LUSH",
                "NETHER",
                "END",
                "MUSHROOM",
                "MAGICAL",
                "OCEAN",
                "RIVER",
                "WATER",
                "MESA",
                "FOREST",
                "PLAINS",
                "MOUNTAIN",
                "HILLS",
                "SWAMP",
                "SANDY",
                "SNOWY",
                "WASTELAND",
                "BEACH",
                "DESERT",
                "FROZEN")));

    private final Kind kind;
    private final String value;

    private BiomeEntry(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    /**
     * Parses an entry.
     *
     * @param text the entry text
     * @return the entry
     * @throws IllegalArgumentException with the reason when the entry is not in the 1.7.10 vocabulary
     */
    public static BiomeEntry parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("empty biome entry");
        }
        if (text.startsWith("type:")) {
            String type = text.substring(5)
                .toUpperCase(Locale.ROOT);
            if (!KNOWN_TYPES.contains(type)) {
                throw new IllegalArgumentException("'" + type + "' is not a Forge BiomeDictionary type");
            }
            return new BiomeEntry(Kind.TYPE, type);
        }
        if (text.startsWith("name:") && text.length() > 5) {
            return new BiomeEntry(Kind.NAME, text.substring(5));
        }
        if (text.startsWith("rwg:") && text.length() > 4) {
            String biome = text.substring(4);
            if (!biome.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException(
                    "an rwg: biome is written in lower snake case, such as canyon_forest");
            }
            return new BiomeEntry(Kind.RWG, biome);
        }
        throw new IllegalArgumentException(
            "not a 1.7.10 biome entry; write type:<BiomeDictionary type>, name:<biome name> or rwg:<realistic biome>"
                + " (1.20 ids and #tags have no meaning here: #minecraft:is_ocean becomes type:OCEAN)");
    }

    /** Returns what the entry tests. */
    public Kind kind() {
        return kind;
    }

    /** Returns the type (upper case), name or realistic biome. */
    public String value() {
        return value;
    }

    /** Returns whether a biome matches this entry. */
    public boolean matches(BiomeDescriptor biome) {
        switch (kind) {
            case TYPE:
                return biome.types()
                    .contains(value);
            case NAME:
                return biome.name()
                    .equalsIgnoreCase(value);
            default:
                return value.equals(biome.realisticBiome());
        }
    }

    @Override
    public String toString() {
        return kind.name()
            .toLowerCase(Locale.ROOT) + ":"
            + value;
    }
}
