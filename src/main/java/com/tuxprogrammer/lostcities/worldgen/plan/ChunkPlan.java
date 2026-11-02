package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Everything the overlay reads about one chunk from the city plan: what the chunk is, its city level and ground, its
 * oracle height, its building's floors and cellars, the city style's blocks, the writer of its own content, the
 * footprint the write guard protects, a village lane's columns and the population jobs. It is the narrow interface
 * between the layout (M03, pure Java) and the overlay (M04): M05 builds these from M03's chunk plans, and the
 * synthetic test city builds them by hand (docs/design/decision-log.md, the M04 rows).
 *
 * <p>
 * Every value is a pure function of the world seed, the chunk coordinates, the pinned profile, the assets and the
 * oracles, so a plan never changes once made (D9). Immutable.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ChunkPlan {

    /** {@link #footprintBottom} of a column outside the footprint. */
    public static final int NO_FOOTPRINT = Integer.MAX_VALUE;

    /** {@link #ruinHeight()} of a building that is not ruined. */
    public static final float NOT_RUINED = -1f;

    /** Sides of a chunk, in the order {@link #borderOpenings} indexes them. */
    public enum Side {
        /** Toward −X (local x = 0). */
        WEST,
        /** Toward +X (local x = 15). */
        EAST,
        /** Toward −Z (local z = 0). */
        NORTH,
        /** Toward +Z (local z = 15). */
        SOUTH
    }

    private final int chunkX;
    private final int chunkZ;
    private final ChunkKind kind;
    private final int cityLevel;
    private final int groundY;
    private final int naturalHeight;
    private final boolean ocean;
    private final boolean building;
    private final int floors;
    private final int cellars;
    private final float ruinHeight;
    private final float explosionChance;
    private final StyleBlocks style;
    private final ChunkContent content;
    private final short[] footprint;
    private final LanePath lane;
    private final List<PopulationJob> jobs;
    private final int[] borderOpenings;

    private ChunkPlan(Builder b) {
        this.chunkX = b.chunkX;
        this.chunkZ = b.chunkZ;
        this.kind = b.kind;
        this.cityLevel = b.cityLevel;
        this.groundY = b.groundY;
        this.naturalHeight = b.naturalHeight;
        this.ocean = b.ocean;
        this.building = b.building;
        this.floors = b.floors;
        this.cellars = b.cellars;
        this.ruinHeight = b.ruinHeight;
        this.explosionChance = b.explosionChance;
        this.style = b.style;
        this.content = b.content;
        this.footprint = b.footprint == null ? null : b.footprint.clone();
        this.lane = b.lane;
        this.jobs = Collections.unmodifiableList(new ArrayList<>(b.jobs));
        this.borderOpenings = b.borderOpenings.clone();
    }

    /**
     * Returns a builder.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param kind   what the chunk is
     * @return the builder, with no building, no footprint, no lane and no jobs
     */
    public static Builder builder(int chunkX, int chunkZ, ChunkKind kind) {
        return new Builder(chunkX, chunkZ, kind);
    }

    /**
     * Returns the plan of a chunk the city does not touch.
     *
     * @param chunkX        chunk X
     * @param chunkZ        chunk Z
     * @param cityLevel     the level upstream gives every chunk from its height (used by street borders)
     * @param groundY       that level's ground Y
     * @param naturalHeight the oracle height
     * @param ocean         whether the chunk's biome is an ocean
     * @return the plan
     */
    public static ChunkPlan none(int chunkX, int chunkZ, int cityLevel, int groundY, int naturalHeight, boolean ocean) {
        return builder(chunkX, chunkZ, ChunkKind.NONE).cityLevel(cityLevel)
            .groundY(groundY)
            .naturalHeight(naturalHeight)
            .ocean(ocean)
            .build();
    }

    /** Returns chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns what the chunk is. */
    public ChunkKind kind() {
        return kind;
    }

    /** Returns {@code true} for a city chunk. */
    public boolean isCity() {
        return kind == ChunkKind.CITY;
    }

    /** Returns {@code true} for a landmark core. */
    public boolean isCore() {
        return kind == ChunkKind.LANDMARK_CORE;
    }

    /** Returns the city level, 0 to 8; for a non-city chunk the level its height gives (upstream's rule). */
    public int cityLevel() {
        return cityLevel;
    }

    /**
     * Returns the chunk's ground Y: a city chunk's city ground ({@code groundLevel + 6 × level}); a landmark core's
     * natural height from the oracle, which fixes the blend corners next to it (rule E1); the level's ground for any
     * other chunk.
     */
    public int groundY() {
        return groundY;
    }

    /** Returns the chunk's oracle height, block Y: the base terrain height at its sample point. */
    public int naturalHeight() {
        return naturalHeight;
    }

    /** Returns whether the chunk's biome is an ocean ({@code BiomeDictionary} type {@code OCEAN}). */
    public boolean ocean() {
        return ocean;
    }

    /** Returns whether a building stands here. */
    public boolean hasBuilding() {
        return building;
    }

    /** Returns the building's floors above ground; 0 without a building. */
    public int floors() {
        return floors;
    }

    /** Returns the building's cellars; 0 without a building. */
    public int cellars() {
        return cellars;
    }

    /**
     * Returns the Y of the building's lowest level: {@code groundY − 6 × cellars}. The layout keeps it at or above
     * {@link WorldLimits#BUILDING_BOTTOM_MIN}, dropping cellars one at a time (TERR-31).
     */
    public int buildingBottom() {
        return groundY - WorldLimits.FLOOR_HEIGHT * cellars;
    }

    /** Returns upstream's {@code getMaxHeight}: the building's top, {@code groundY + 6 × floors}, or the ground. */
    public int maxHeight() {
        return building ? groundY + WorldLimits.FLOOR_HEIGHT * floors : groundY;
    }

    /** Returns the fraction of floors a ruined building keeps, or {@link #NOT_RUINED}. */
    public float ruinHeight() {
        return ruinHeight;
    }

    /**
     * Returns the city style's explosion chance, the chance an explosion reaching this chunk is kept, or
     * {@link Float#NaN} when the style sets none (every explosion is kept).
     */
    public float explosionChance() {
        return explosionChance;
    }

    /** Returns the city style's terrain blocks. */
    public StyleBlocks style() {
        return style;
    }

    /** Returns the writer of the chunk's own content. */
    public ChunkContent content() {
        return content;
    }

    /**
     * Returns the lowest Y of a column's footprint, the volume the write guard keeps RWG's decoration out of: every
     * column the city writes in a building, street, bridge, highway or railway, from its lowest written block up to
     * Y 255 (0005 §3a, P03-1). Parks, yards, edge-ring parks, soft-edge strips and landmark cores are outside it.
     *
     * @param x local X, 0 to 15
     * @param z local Z, 0 to 15
     * @return the bottom Y, or {@link #NO_FOOTPRINT}
     */
    public int footprintBottom(int x, int z) {
        return footprint == null ? NO_FOOTPRINT : unpack(footprint[(z << 4) | x]);
    }

    /** Returns whether any column of this chunk is in the footprint. */
    public boolean hasFootprint() {
        return footprint != null;
    }

    /** Returns the village lane crossing this chunk, or {@code null}. */
    public LanePath lane() {
        return lane;
    }

    /** Returns the population jobs, in the order they run. */
    public List<PopulationJob> jobs() {
        return jobs;
    }

    /**
     * Returns the openings on one side, where a street border is one block lower so a slope, stairs or a bridge from
     * the neighbor can come through: bit {@code i} for position {@code i} along the side.
     *
     * @param side the side
     * @return a 16-bit mask
     */
    public int borderOpenings(Side side) {
        return borderOpenings[side.ordinal()];
    }

    @Override
    public String toString() {
        return "ChunkPlan[" + chunkX
            + ","
            + chunkZ
            + " "
            + kind
            + " level="
            + cityLevel
            + " ground="
            + groundY
            + (building ? " floors=" + floors + " cellars=" + cellars : "")
            + "]";
    }

    /** Mutable builder for {@link ChunkPlan}. */
    public static final class Builder {

        private final int chunkX;
        private final int chunkZ;
        private final ChunkKind kind;
        private int cityLevel;
        private int groundY = 71;
        private int naturalHeight = 71;
        private boolean ocean;
        private boolean building;
        private int floors;
        private int cellars;
        private float ruinHeight = NOT_RUINED;
        private float explosionChance = Float.NaN;
        private StyleBlocks style = StyleBlocks.defaults();
        private ChunkContent content = ChunkContent.NONE;
        private short[] footprint;
        private LanePath lane;
        private final List<PopulationJob> jobs = new ArrayList<>();
        private final int[] borderOpenings = new int[4];

        private Builder(int chunkX, int chunkZ, ChunkKind kind) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.kind = Objects.requireNonNull(kind);
        }

        /** Sets the city level, 0 to 8. */
        public Builder cityLevel(int value) {
            this.cityLevel = value;
            return this;
        }

        /** Sets the ground Y. */
        public Builder groundY(int value) {
            this.groundY = WorldLimits.clampY(value);
            return this;
        }

        /** Sets the oracle height. */
        public Builder naturalHeight(int value) {
            this.naturalHeight = WorldLimits.clampY(value);
            return this;
        }

        /** Sets whether the chunk's biome is an ocean. */
        public Builder ocean(boolean value) {
            this.ocean = value;
            return this;
        }

        /**
         * Puts a building here.
         *
         * @param floorCount  floors above ground, at least 1
         * @param cellarCount cellars, at least 0
         * @return this
         */
        public Builder building(int floorCount, int cellarCount) {
            if (floorCount < 1 || cellarCount < 0) {
                throw new IllegalArgumentException("building needs floors >= 1 and cellars >= 0");
            }
            this.building = true;
            this.floors = floorCount;
            this.cellars = cellarCount;
            return this;
        }

        /** Sets the fraction of floors a ruined building keeps, 0 to 1. */
        public Builder ruinHeight(float value) {
            this.ruinHeight = value;
            return this;
        }

        /** Sets the city style's explosion chance; {@link Float#NaN} for none. */
        public Builder explosionChance(float value) {
            this.explosionChance = value;
            return this;
        }

        /** Sets the terrain blocks. */
        public Builder style(StyleBlocks value) {
            this.style = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the content writer. */
        public Builder content(ChunkContent value) {
            this.content = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Puts columns in the footprint.
         *
         * @param bottoms 256 bottom Ys, index {@code z × 16 + x}, {@link #NO_FOOTPRINT} for columns outside it;
         *                {@code null} for none
         * @return this
         */
        public Builder footprint(int[] bottoms) {
            if (bottoms == null) {
                this.footprint = null;
                return this;
            }
            if (bottoms.length != 256) {
                throw new IllegalArgumentException("a footprint has 256 columns, got " + bottoms.length);
            }
            short[] packed = new short[256];
            boolean any = false;
            for (int i = 0; i < 256; i++) {
                int bottom = bottoms[i];
                if (bottom == NO_FOOTPRINT) {
                    packed[i] = Short.MAX_VALUE;
                } else {
                    packed[i] = (short) WorldLimits.clampY(bottom);
                    any = true;
                }
            }
            this.footprint = any ? packed : null;
            return this;
        }

        /** Sets the lane path; {@code null} for none. */
        public Builder lane(LanePath value) {
            this.lane = value;
            return this;
        }

        /** Adds a population job. */
        public Builder job(PopulationJob value) {
            jobs.add(Objects.requireNonNull(value));
            return this;
        }

        /** Sets the border openings of one side (a 16-bit mask). */
        public Builder borderOpenings(Side side, int mask) {
            borderOpenings[side.ordinal()] = mask & 0xFFFF;
            return this;
        }

        /** Returns the immutable plan. */
        public ChunkPlan build() {
            if (kind != ChunkKind.CITY && (building || content != ChunkContent.NONE)) {
                throw new IllegalStateException(
                    "only a city chunk has a building or content: " + chunkX + "," + chunkZ);
            }
            if (building && groundY - WorldLimits.FLOOR_HEIGHT * cellars < WorldLimits.BUILDING_BOTTOM_MIN) {
                // TERR-31: a building that cannot fit is the layout's problem, settled before generation.
                throw new IllegalStateException(
                    "building bottom " + (groundY - WorldLimits.FLOOR_HEIGHT * cellars)
                        + " is below Y "
                        + WorldLimits.BUILDING_BOTTOM_MIN
                        + " at "
                        + chunkX
                        + ","
                        + chunkZ
                        + "; drop cellars in the layout");
            }
            return new ChunkPlan(this);
        }
    }

    /**
     * Returns the footprint bottom stored for a column, translating the packed "none" value.
     *
     * @param packed a stored value
     * @return the bottom Y or {@link #NO_FOOTPRINT}
     */
    private static int unpack(short packed) {
        return packed == Short.MAX_VALUE ? NO_FOOTPRINT : packed;
    }
}
