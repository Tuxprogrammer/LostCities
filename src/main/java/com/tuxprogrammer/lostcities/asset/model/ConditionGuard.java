package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/regassets/data/ConditionGuard.java
// Avoids: ASSET-04, ASSET-33

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.match.BiomeEntry;

/**
 * The guard fields shared by condition entries and building part references: every present field adds an AND clause,
 * and no fields means "always" (docs/original-spec/05-assets/conditions.md, "Condition tests").
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>{@code belowpart} tests the part below, not the current part (fixes ASSET-04).</li>
 * <li>{@code inpart}, {@code belowpart} and {@code inbuilding} names are normalized to full ids at load and compared as
 * full ids, so {@code rail_dungeon1} and {@code lostcities:rail_dungeon1} match each other (fixes ASSET-33).</li>
 * <li>{@code range} must parse at load; a bad range is an error with its field.</li>
 * <li>{@code inbiome} takes the 1.7.10 biome vocabulary of {@link BiomeEntry} (P05-6, D31); the biome is in the set
 * when it matches any entry.</li>
 * </ul>
 * Immutable. Side: both.
 */
public final class ConditionGuard {

    /** The test that always holds. */
    public static final ConditionGuard ALWAYS = new ConditionGuard();

    private Boolean top;
    private Boolean ground;
    private Boolean cellar;
    private Boolean isBuilding;
    private Boolean isSphere;
    private Integer floor;
    private int[] range;
    private Integer chunkX;
    private Integer chunkZ;
    private Set<AssetId> inPart;
    private Set<AssetId> belowPart;
    private Set<AssetId> inBuilding;
    private List<BiomeEntry> inBiome;

    private ConditionGuard() {}

    /**
     * Reads the test fields of an object.
     *
     * @param fields  the object
     * @param context the asset being read
     * @return the test; fields with errors are reported and left out
     */
    public static ConditionGuard parse(JsonFields fields, ParseContext context) {
        ConditionGuard test = new ConditionGuard();
        test.top = fields.bool("top", false);
        test.ground = fields.bool("ground", false);
        test.cellar = fields.bool("cellar", false);
        test.isBuilding = fields.bool("isbuilding", false);
        test.isSphere = fields.bool("issphere", false);
        test.floor = fields.integer("floor", false);
        test.chunkX = fields.integer("chunkx", false);
        test.chunkZ = fields.integer("chunkz", false);
        String range = fields.string("range", false);
        if (range != null) {
            test.range = parseRange(range);
            if (test.range == null) {
                fields.error("range", range, "must be '<from>,<to>', two whole floors");
            }
        }
        test.inPart = names(fields, "inpart", context);
        test.belowPart = names(fields, "belowpart", context);
        test.inBuilding = names(fields, "inbuilding", context);
        List<String> biomes = fields.stringOrList("inbiome", false);
        if (biomes != null) {
            List<BiomeEntry> entries = new ArrayList<>();
            for (int i = 0; i < biomes.size(); i++) {
                try {
                    entries.add(BiomeEntry.parse(biomes.get(i)));
                } catch (IllegalArgumentException e) {
                    context.scope()
                        .error(fields.field("inbiome") + "[" + i + "]", biomes.get(i), e.getMessage());
                }
            }
            test.inBiome = Collections.unmodifiableList(entries);
        }
        return test;
    }

    /**
     * Parses a {@code range} value.
     *
     * @param text the value, {@code "l1,l2"}
     * @return the two bounds, or null when the text is not two integers
     */
    static int[] parseRange(String text) {
        String[] parts = text.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Evaluates the test.
     *
     * @param context the facts
     * @return whether every present clause holds
     */
    public boolean test(ConditionContext context) {
        if (top != null && top != context.isTopOfBuilding()) {
            return false;
        }
        if (ground != null && ground != context.isGroundFloor()) {
            return false;
        }
        if (cellar != null && cellar != context.isCellar()) {
            return false;
        }
        if (isBuilding != null && isBuilding != context.isBuilding()) {
            return false;
        }
        if (isSphere != null && isSphere != context.isSphere()) {
            return false;
        }
        if (floor != null && floor != context.floor()) {
            return false;
        }
        if (range != null && (context.floor() < range[0] || context.floor() > range[1])) {
            return false;
        }
        if (chunkX != null && chunkX != context.chunkX()) {
            return false;
        }
        if (chunkZ != null && chunkZ != context.chunkZ()) {
            return false;
        }
        if (inPart != null && !inPart.contains(context.part())) {
            return false;
        }
        // Avoids ASSET-04: the part below, not the current part.
        if (belowPart != null && !belowPart.contains(context.belowPart())) {
            return false;
        }
        if (inBuilding != null && !inBuilding.contains(context.building())) {
            return false;
        }
        if (inBiome != null) {
            boolean any = false;
            for (BiomeEntry entry : inBiome) {
                if (entry.matches(context.biome())) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether the test has no clauses. */
    public boolean isAlways() {
        return top == null && ground == null
            && cellar == null
            && isBuilding == null
            && isSphere == null
            && floor == null
            && range == null
            && chunkX == null
            && chunkZ == null
            && inPart == null
            && belowPart == null
            && inBuilding == null
            && inBiome == null;
    }

    /** Returns the {@code top} clause, or null. */
    public Boolean top() {
        return top;
    }

    /** Returns the {@code cellar} clause, or null. */
    public Boolean cellar() {
        return cellar;
    }

    private static Set<AssetId> names(JsonFields fields, String key, ParseContext context) {
        List<String> names = fields.stringOrList(key, false);
        if (names == null) {
            return null;
        }
        Set<AssetId> out = new LinkedHashSet<>();
        for (int i = 0; i < names.size(); i++) {
            AssetId id = context.name(names.get(i), fields.field(key) + "[" + i + "]");
            if (id != null) {
                out.add(id);
            }
        }
        return Collections.unmodifiableSet(out);
    }
}
