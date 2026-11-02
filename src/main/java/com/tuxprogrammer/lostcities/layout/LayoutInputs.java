package com.tuxprogrammer.lostcities.layout;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedIndex;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.view.AssetView;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;

/**
 * Everything one dimension's city layout is a function of: the world seed, the dimension, the pinned profile, the
 * loaded assets, the terrain oracle, the structure oracle, the highway and railway layout and the predefined content.
 * The glue builds one per world when the overlay is installed and drops it with the world (new-spec
 * 02-profiles-dimensions, {@code DefaultDimensionInfo} construction).
 *
 * <p>
 * Side: server.
 */
public final class LayoutInputs {

    /** Default bound on each planner cache, in entries; about a 64-chunk view radius of chunks. */
    public static final int DEFAULT_CACHE_SIZE = 16384;

    final long seed;
    final DimensionKey dimension;
    final LayoutProfile profile;
    final AssetView assets;
    final TerrainView terrain;
    final StructureOracle structures;
    final InfrastructureView infrastructure;
    final NetworkSettings networks;
    final PredefinedIndex predefined;
    final LayoutLog log;
    final int cacheSize;

    private LayoutInputs(Builder b) {
        seed = b.seed;
        dimension = b.dimension;
        profile = b.profile;
        assets = b.assets;
        terrain = b.terrain;
        structures = b.structures;
        infrastructure = b.infrastructure;
        networks = b.networks;
        predefined = b.predefined;
        log = b.log;
        cacheSize = b.cacheSize;
    }

    /**
     * Starts the inputs of a layout.
     *
     * @param seed    the world seed
     * @param profile the pinned profile's layout options
     * @param assets  the loaded assets
     * @param terrain the terrain oracle
     * @return a builder with the overworld, no structures, no infrastructure, no predefined content and a silent log
     */
    public static Builder builder(long seed, LayoutProfile profile, AssetView assets, TerrainView terrain) {
        return new Builder(seed, profile, assets, terrain);
    }

    /** Returns the world seed. */
    public long seed() {
        return seed;
    }

    /** Returns the dimension. */
    public DimensionKey dimension() {
        return dimension;
    }

    /** Returns the profile. */
    public LayoutProfile profile() {
        return profile;
    }

    /** Builds {@link LayoutInputs}. */
    public static final class Builder {

        private final long seed;
        private final LayoutProfile profile;
        private final AssetView assets;
        private final TerrainView terrain;
        private DimensionKey dimension = DimensionKey.OVERWORLD;
        private StructureOracle structures = StructureOracle.NONE;
        private InfrastructureView infrastructure = InfrastructureView.NONE;
        private NetworkSettings networks;
        private PredefinedIndex predefined = PredefinedIndex.EMPTY;
        private LayoutLog log = LayoutLog.SILENT;
        private int cacheSize = DEFAULT_CACHE_SIZE;

        private Builder(long seed, LayoutProfile profile, AssetView assets, TerrainView terrain) {
            this.seed = seed;
            this.profile = profile;
            this.assets = assets;
            this.terrain = terrain;
        }

        /** @param v the dimension (default the overworld) @return this builder */
        public Builder dimension(DimensionKey v) {
            dimension = v;
            return this;
        }

        /** @param v the aggregate structure oracle (default none) @return this builder */
        public Builder structures(StructureOracle v) {
            structures = v;
            return this;
        }

        /** @param v the highway and railway layout (default none) @return this builder */
        public Builder infrastructure(InfrastructureView v) {
            infrastructure = v;
            return this;
        }

        /**
         * Makes the layout build its own highway and railway networks ({@link Networks}) from these settings, in place
         * of the {@link #infrastructure(InfrastructureView)} view (M06).
         *
         * @param v the network settings, or {@code null} for none (the default)
         * @return this builder
         */
        public Builder networks(NetworkSettings v) {
            networks = v;
            return this;
        }

        /** @param v the dimension's predefined content (default none) @return this builder */
        public Builder predefined(PredefinedIndex v) {
            predefined = v;
            return this;
        }

        /** @param v where problems are reported (default silent) @return this builder */
        public Builder log(LayoutLog v) {
            log = v;
            return this;
        }

        /** @param v the bound on each planner cache, in entries, at least 64 @return this builder */
        public Builder cacheSize(int v) {
            cacheSize = Math.max(64, v);
            return this;
        }

        /** @return the inputs */
        public LayoutInputs build() {
            return new LayoutInputs(this);
        }
    }
}
