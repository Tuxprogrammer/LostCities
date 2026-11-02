package com.tuxprogrammer.lostcities.layout.view;

import java.util.List;

/**
 * The layout's read-only view of a world style's {@code scattered} settings and the scattered assets they name: the
 * area size, the chance, the "none" weight and the weighted list, each entry with its filters and its asset's
 * placement rules (docs/original-spec/07-scattered/scattered-structures.md § 6, "Data formats"). The asset model (M02)
 * implements it over the biome provider; tests use fakes.
 *
 * <p>
 * Side: both.
 */
public interface ScatteredView {

    /** Which terrain height a scattered structure sits on (upstream's {@code terrainheight}). */
    enum TerrainHeight {
        /** The footprint's lowest sample. */
        LOWEST,
        /** The footprint's mean chunk height. */
        AVERAGE,
        /** The footprint's highest sample. */
        HIGHEST,
        /** The sea level. */
        OCEAN
    }

    /** How the terrain under a scattered structure is fixed (upstream's {@code terrainfix}). */
    enum TerrainFix {
        /** Nothing. */
        NONE,
        /** Air from the lowest level up 50 blocks over the whole chunk. */
        CLEAR,
        /** The support part's bottom slice repeated down through air and liquid. */
        REPEATSLICE
    }

    /** One scattered asset as the planner reads it. */
    interface Asset {

        /** Returns the asset's name, a full id. */
        String name();

        /** Returns the candidate buildings of a single-chunk structure; empty for a multibuilding. */
        List<String> buildings();

        /** Returns the multibuilding's name, or {@code null}. */
        String multiBuilding();

        /** Returns whether the structure may take any of the four rotations. */
        boolean rotatable();

        /** Returns whether a highway beside it leaves its railing open toward it. */
        boolean clearHighwayRailing();

        /** Returns the support part's name for {@link TerrainFix#REPEATSLICE}, or {@code null}. */
        String supportPart();

        /** Returns the height rule. */
        TerrainHeight terrainHeight();

        /** Returns the terrain fix. */
        TerrainFix terrainFix();

        /** Returns the offset from the chosen height, in blocks. */
        int heightOffset();

        /**
         * Returns the most blocks the structure can rise above its lowest level: its buildings' greatest floor count
         * times their tallest part, for the 1.7.10 build-height check.
         */
        int maxHeight();
    }

    /** One entry of the weighted list. */
    interface Entry {

        /** Returns the entry's scattered asset name, a full id. */
        String name();

        /** Returns the weight; a negative weight counts as 0. */
        int weight();

        /** Returns whether it is placed beside a highway. */
        boolean nearHighway();

        /** Returns whether it may stand over void (parsed; no effect on the default landscape, D12). */
        boolean allowVoid();

        /** Returns the most height difference allowed under it, in blocks, or {@code null}. */
        Integer maxHeightDiff();

        /**
         * Returns whether the entry's biome filter accepts a chunk's main biome.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether it is accepted; {@code true} without a filter
         */
        boolean acceptsBiome(int chunkX, int chunkZ);

        /** Returns the asset, or {@code null} when it is not loaded. */
        Asset asset();
    }

    /** Returns the area size in chunks, at least 1. */
    int areaSize();

    /** Returns the chance an area holds a structure, before the profile's multiplier. */
    float chance();

    /** Returns the weight of "none". */
    int weightNone();

    /** Returns the entries in the world style's order. */
    List<Entry> entries();
}
