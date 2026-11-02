package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The VillageNames settings the structure oracles read: whether its village generator and its ocean monuments are on,
 * the village grid's spacing, the village-free radius and the spawn-biome list, plus the world-side switches (map
 * features, RWG's own "Generate Villages"). Immutable and plain Java, so the grid math is testable without
 * VillageNames; {@code VillageNamesConfig} fills it from VillageNames' loaded config at runtime.
 *
 * <p>
 * Source (VillageNames 4.5.21-GTNH, the version GTNH 2.9.0 ships):
 * <ul>
 * <li>spacing: maximum {@code median + range}, minimum {@code median - range} clamped to 1,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/MapGenVillageVN.java#L66-L76;</li>
 * <li>the config keys and defaults,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/config/village/VillageGeneratorConfigHandler.java#L593-L625;</li>
 * <li>a spawn-biome entry is {@code name|villageType|materialType|nosub}, split on {@code |} and trimmed, an entry
 * with an empty name skipped,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/config/village/VillageGeneratorConfigHandler.java#L1822-L1862;</li>
 * <li>monuments: {@code GeneralConfig.addOceanMonuments} registers the monument world generator,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/VillageNames.java#L174-L180.</li>
 * </ul>
 * The pack's values ({@code config/VillageNames4/newvillages/village_gen.cfg} of GTNH 2.9.0): generator on, median 40,
 * range 10, village-free radius 0, monuments on.
 *
 * <p>
 * Side: both (plain data).
 */
public final class VillageNamesSettings {

    /** VillageNames' default median village spacing, in chunks (the pack sets 40). */
    public static final int DEFAULT_SPACING_MEDIAN = 20;
    /** VillageNames' default spacing range, in chunks (the pack sets 10). */
    public static final int DEFAULT_SPACING_RANGE = 12;

    private final boolean villagesOn;
    private final boolean monumentsOn;
    private final int spacingMedian;
    private final int spacingRange;
    private final int noVillagesRadius;
    private final Set<String> spawnBiomeNames;

    /**
     * Creates the settings.
     *
     * @param villagesOn       whether villages generate: VillageNames' new generator is on, and the world's generator
     *                         generates villages
     * @param monumentsOn      whether ocean monuments generate: VillageNames' "Add Monuments" and the world's map
     *                         features
     * @param spacingMedian    "Village Spacing: Median", in chunks
     * @param spacingRange     "Village Spacing: Range", in chunks
     * @param noVillagesRadius "Village Spacing: Village-Free Radius", in chunks
     * @param spawnBiomeNames  the biome names of "Spawn Biome Names", exact and case-sensitive, in config order
     */
    public VillageNamesSettings(boolean villagesOn, boolean monumentsOn, int spacingMedian, int spacingRange,
        int noVillagesRadius, Set<String> spawnBiomeNames) {
        this.villagesOn = villagesOn;
        this.monumentsOn = monumentsOn;
        this.spacingMedian = spacingMedian;
        this.spacingRange = spacingRange;
        this.noVillagesRadius = noVillagesRadius;
        this.spawnBiomeNames = Collections.unmodifiableSet(new LinkedHashSet<>(spawnBiomeNames));
    }

    /** Settings under which VillageNames generates nothing: what the oracles use when it is absent. */
    public static VillageNamesSettings off() {
        return new VillageNamesSettings(
            false,
            false,
            DEFAULT_SPACING_MEDIAN,
            DEFAULT_SPACING_RANGE,
            0,
            Collections.<String>emptySet());
    }

    /**
     * Returns the biome names of VillageNames' "Spawn Biome Names" entries, as its {@code unpackBiomes} reads them: the
     * part before the first {@code |}, trimmed; entries whose name is empty are skipped.
     *
     * @param entries the raw entries, or {@code null} when the list is unset
     * @return the names, in config order, without duplicates
     */
    public static Set<String> parseSpawnBiomeNames(String[] entries) {
        Set<String> names = new LinkedHashSet<>();
        if (entries == null) {
            return names;
        }
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String name = entry.split("\\|")[0].trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /** Returns whether villages generate in this world. */
    public boolean villagesOn() {
        return villagesOn;
    }

    /** Returns whether ocean monuments generate in this world. */
    public boolean monumentsOn() {
        return monumentsOn;
    }

    /** Returns the median spacing, in chunks. */
    public int spacingMedian() {
        return spacingMedian;
    }

    /** Returns the spacing range, in chunks. */
    public int spacingRange() {
        return spacingRange;
    }

    /** Returns the grid cell's side, VillageNames' maximum distance between villages: median + range, in chunks. */
    public int maxSpacing() {
        return spacingMedian + spacingRange;
    }

    /** Returns VillageNames' minimum distance between villages: median − range, at least 1, in chunks. */
    public int minSpacing() {
        return Math.max(1, spacingMedian - spacingRange);
    }

    /** Returns the village-free radius around chunk (0, 0), in chunks. */
    public int noVillagesRadius() {
        return noVillagesRadius;
    }

    /** Returns the spawn-biome names, exact and case-sensitive. */
    public Set<String> spawnBiomeNames() {
        return spawnBiomeNames;
    }

    /**
     * Returns whether a biome name is in the spawn-biome list, by VillageNames' exact, case-sensitive comparison.
     *
     * @param biomeName the biome's {@code biomeName}
     * @return whether a village may start there
     */
    public boolean isSpawnBiome(String biomeName) {
        return biomeName != null && spawnBiomeNames.contains(biomeName);
    }

    /**
     * Returns a one-line summary of every value the oracles read, for the log and for the world's pinned profile
     * snapshot (0006 §2: a change moves structure sites for new chunks).
     *
     * @return the summary
     */
    public String summary() {
        return String.format(
            Locale.ROOT,
            "villages=%s monuments=%s spacing=%d+-%d villageFreeRadius=%d spawnBiomes=%d(%08x)",
            villagesOn,
            monumentsOn,
            spacingMedian,
            spacingRange,
            noVillagesRadius,
            spawnBiomeNames.size(),
            spawnBiomeNames.hashCode());
    }

    @Override
    public String toString() {
        return summary();
    }
}
