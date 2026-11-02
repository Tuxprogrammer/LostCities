package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/ConditionContext.java
// Avoids: ASSET-04, ASSET-05, ASSET-33

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;

/**
 * The facts a condition test reads: where in a building a part or a loot chest is, which part and building it belongs
 * to, the chunk, and the biome (docs/original-spec/05-assets/conditions.md, "Condition context"). Built by the caller,
 * part selection in {@code layout} or loot and spawners in {@code worldgen}.
 *
 * <p>
 * Changes from upstream, each a bug fix:
 * <ul>
 * <li>The below part is the part on the floor below, which the caller supplies; for a {@code parts2} overlay it is the
 * main part of the floor below, not the current floor's (fixes ASSET-04 and LOOT-02).</li>
 * <li>{@link #floorOf(int, int)} uses floor division, so cellar <i>k</i> reports floor −<i>k</i> (fixes ASSET-05 and
 * LOOT-01).</li>
 * <li>Part and building names are full ids, compared as full ids (fixes ASSET-33).</li>
 * </ul>
 * Immutable; build one with {@link #builder()}. Side: both.
 */
public final class ConditionContext {

    /** Blocks per floor (upstream's {@code FLOORHEIGHT}; docs/original-spec/05-assets/index.md, "Heights"). */
    public static final int FLOOR_HEIGHT = 6;

    private final int level;
    private final int floor;
    private final int floorsBelowGround;
    private final int floorsAboveGround;
    private final AssetId part;
    private final AssetId belowPart;
    private final AssetId building;
    private final int chunkX;
    private final int chunkZ;
    private final boolean sphere;
    private final BiomeDescriptor biome;

    private ConditionContext(Builder builder) {
        this.level = builder.level;
        this.floor = builder.floor;
        this.floorsBelowGround = builder.floorsBelowGround;
        this.floorsAboveGround = builder.floorsAboveGround;
        this.part = builder.part;
        this.belowPart = builder.belowPart;
        this.building = builder.building;
        this.chunkX = builder.chunkX;
        this.chunkZ = builder.chunkZ;
        this.sphere = builder.sphere;
        this.biome = builder.biome;
    }

    /** Returns a builder with every fact at its neutral value: floor 0, no part, no building, no biome facts. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the floor a block is on, counting from a ground level: 0 for the six blocks from the ground up, −1 for
     * the six below, and so on (fixes ASSET-05: upstream truncated toward zero, so the first cellar reported floor 0).
     *
     * @param y           the block's Y
     * @param groundLevel the Y of the ground floor's lowest block
     * @return the floor
     */
    public static int floorOf(int y, int groundLevel) {
        return Math.floorDiv(y - groundLevel, FLOOR_HEIGHT);
    }

    /** Returns the level (stored for completeness; no test reads it, as upstream). */
    public int level() {
        return level;
    }

    /** Returns the floor: 0 ground, negative cellars. */
    public int floor() {
        return floor;
    }

    /** Returns the building's cellar count. */
    public int floorsBelowGround() {
        return floorsBelowGround;
    }

    /** Returns the building's floor count above ground. */
    public int floorsAboveGround() {
        return floorsAboveGround;
    }

    /** Returns the current part, or null for none (upstream's {@code <none>}). */
    public AssetId part() {
        return part;
    }

    /** Returns the part on the floor below, or null for none. */
    public AssetId belowPart() {
        return belowPart;
    }

    /** Returns the building, or null outside a building. */
    public AssetId building() {
        return building;
    }

    /** Returns the chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns the chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns whether there is a building. */
    public boolean isBuilding() {
        return building != null;
    }

    /** Returns whether the position is inside a city sphere. */
    public boolean isSphere() {
        return sphere;
    }

    /** Returns the biome facts. */
    public BiomeDescriptor biome() {
        return biome;
    }

    /** Returns whether this is the top floor or above. */
    public boolean isTopOfBuilding() {
        return floor >= floorsAboveGround;
    }

    /** Returns whether this is the ground floor. */
    public boolean isGroundFloor() {
        return floor == 0;
    }

    /** Returns whether this is a cellar. */
    public boolean isCellar() {
        return floor < 0;
    }

    /** Builds a {@link ConditionContext}. */
    public static final class Builder {

        private int level;
        private int floor;
        private int floorsBelowGround;
        private int floorsAboveGround;
        private AssetId part;
        private AssetId belowPart;
        private AssetId building;
        private int chunkX;
        private int chunkZ;
        private boolean sphere;
        private BiomeDescriptor biome = BiomeDescriptor.NONE;

        private Builder() {}

        /** Sets the level. */
        public Builder level(int value) {
            this.level = value;
            return this;
        }

        /** Sets the floor. */
        public Builder floor(int value) {
            this.floor = value;
            return this;
        }

        /** Sets the cellar count and the floor count above ground. */
        public Builder floors(int below, int above) {
            this.floorsBelowGround = below;
            this.floorsAboveGround = above;
            return this;
        }

        /** Sets the current part, null for none. */
        public Builder part(AssetId value) {
            this.part = value;
            return this;
        }

        /** Sets the part on the floor below, null for none. */
        public Builder belowPart(AssetId value) {
            this.belowPart = value;
            return this;
        }

        /** Sets the building, null for none. */
        public Builder building(AssetId value) {
            this.building = value;
            return this;
        }

        /** Sets the chunk. */
        public Builder chunk(int x, int z) {
            this.chunkX = x;
            this.chunkZ = z;
            return this;
        }

        /** Sets whether the position is in a city sphere. */
        public Builder sphere(boolean value) {
            this.sphere = value;
            return this;
        }

        /** Sets the biome facts. */
        public Builder biome(BiomeDescriptor value) {
            this.biome = value == null ? BiomeDescriptor.NONE : value;
            return this;
        }

        /** Returns the context. */
        public ConditionContext build() {
            return new ConditionContext(this);
        }
    }
}
