package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

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
    private final ChunkContent network;
    private final boolean highway;
    private final short[] footprint;
    private final int guardBottom;
    private final int guardTop;
    private final LanePath lane;
    private final List<PopulationJob> fixedJobs;
    private final Supplier<List<PopulationJob>> jobSource;
    private List<PopulationJob> jobs;
    private int[] borderOpenings;
    private long[] keepClear;
    private Supplier<Edges> edgeSource;
    private final String label;
    private final boolean lot;
    private final boolean scattered;
    private final List<GroundFit> groundFits;
    private final ChunkContent finish;

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
        this.network = b.network;
        this.highway = b.highway;
        this.footprint = b.footprint == null ? null : b.footprint.clone();
        this.guardBottom = b.guardBottom;
        this.guardTop = b.guardTop;
        this.lane = b.lane;
        this.fixedJobs = Collections.unmodifiableList(new ArrayList<>(b.jobs));
        this.jobSource = b.jobSource;
        this.jobs = b.jobSource == null ? fixedJobs : null;
        this.borderOpenings = b.borderOpenings.clone();
        this.keepClear = b.keepClear;
        this.edgeSource = b.edgeSource;
        this.label = b.label;
        this.lot = b.lot;
        this.scattered = b.scattered;
        this.groundFits = b.groundFits.isEmpty() ? Collections.<GroundFit>emptyList()
            : Collections.unmodifiableList(new ArrayList<>(b.groundFits));
        this.finish = b.finish;
    }

    /**
     * Returns whether the chunk is an edge lot (docs/design/0011-city-edges-and-character.md §3): its writer digs below
     * city ground, so the street base is filled before it writes, and no rubble or leaves are laid on it.
     */
    public boolean isLot() {
        return lot;
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

    /**
     * Returns the writer of the chunk's own content: a city chunk's building or street, or the bridge span over a
     * non-city chunk ({@link ChunkContent#NONE} otherwise). A landmark core never has content.
     */
    public ChunkContent content() {
        return content;
    }

    /**
     * Returns the writer of the chunk's highways, railways and rail dungeon (M06), which the terrain phase runs after
     * the chunk's own content, its street borders, ruins and rubble, or after the blend outside a city;
     * {@link ChunkContent#NONE} when none passes. In a landmark core it is only ever a railway deep enough to pass
     * under the core (D30, D31).
     *
     * @return the network writer
     */
    public ChunkContent network() {
        return network;
    }

    /**
     * Returns whether a highway passes through the chunk, so lakes, lava and dungeons stay out of its population area
     * as they stay out of a city's (M06: no slope or pit eats a highway, alpha review AR-10).
     *
     * @return whether a highway passes
     */
    public boolean hasHighway() {
        return highway;
    }

    /**
     * Returns whether a block Y lies in the chunk's guarded band: the Y range of its highway tunnels and its railway
     * parts (M06). The write guard keeps RWG's decoration out of the band in every column, as it does the footprint,
     * and leaves the ground above to RWG. Without it, BOP's lakes, which no populate event announces, flood a tunnel's
     * deck.
     *
     * @param y block Y
     * @return whether it is in the band; {@code false} in a chunk with none
     */
    public boolean inGuardBand(int y) {
        return y >= guardBottom && y <= guardTop;
    }

    /**
     * Returns whether a scattered structure stands in this chunk (07-scattered): lakes, lava, dungeons and oil are
     * denied where a population window touches it (P07-4, D31), and other mods' steering keeps out of it (P07-5).
     */
    public boolean isScattered() {
        return scattered;
    }

    /**
     * Returns the ground fits that reach this chunk, in the order they apply: the scattered structures whose ground
     * the terrain phase sets into RWG's terrain here, before the chunk's content (design record 0009).
     */
    public List<GroundFit> groundFits() {
        return groundFits;
    }

    /**
     * Returns the writer of a city chunk's last step, after its ruins and rubble: upstream's stuff columns
     * ({@code Stuff.generateStuff}); {@link ChunkContent#NONE} for none.
     */
    public ChunkContent finish() {
        return finish;
    }

    /**
     * Returns the lowest Y of a column's footprint, the volume the write guard keeps RWG's decoration out of: every
     * column the city writes in a building, street, bridge, highway (a neighbor's diagonal deck spilling in included) or railway, from its lowest written block up to
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

    /**
     * Returns the population jobs, in the order they run: the jobs added to the builder, then those of its job
     * source. A job source is asked once, on the first call, so a plan read only for its neighbors' terrain never pays
     * for enumerating its parts (budget B1). The source is pure, so the list is the same whenever it is made.
     *
     * @return the jobs
     */
    public List<PopulationJob> jobs() {
        List<PopulationJob> list = jobs;
        if (list == null) {
            List<PopulationJob> all = new ArrayList<>(fixedJobs);
            all.addAll(jobSource.get());
            list = Collections.unmodifiableList(all);
            jobs = list;
        }
        return list;
    }

    /**
     * Returns whether a column is a door's approach, which the terrain phase's rubble layer leaves clear so a
     * building's street door can be walked through (decision log, M05J rows).
     *
     * @param x local X, 0 to 15
     * @param z local Z, 0 to 15
     * @return whether the column stays clear
     */
    public boolean keepsClear(int x, int z) {
        resolveEdges();
        if (keepClear == null) {
            return false;
        }
        int index = (z << 4) | x;
        return (keepClear[index >> 6] & (1L << (index & 63))) != 0;
    }

    /** Asks the edge source once, the first time a border opening or a kept-clear column is read. */
    private void resolveEdges() {
        Supplier<Edges> source = edgeSource;
        if (source == null) {
            return;
        }
        Edges edges = source.get();
        for (Side side : Side.values()) {
            borderOpenings[side.ordinal()] = edges.openings[side.ordinal()] & 0xFFFF;
        }
        keepClear = pack(edges.keepClear);
        edgeSource = null;
    }

    /**
     * What a chunk's own terrain phase reads at its edges, made lazily because a plan is asked for far more often as a
     * neighbor than for its own chunk: the border openings of each side and the door approaches.
     */
    public static final class Edges {

        final int[] openings;
        final boolean[] keepClear;

        /**
         * Collects the edges.
         *
         * @param openings  4 masks in {@link Side} order
         * @param keepClear 256 flags, or {@code null}
         */
        public Edges(int[] openings, boolean[] keepClear) {
            this.openings = openings.clone();
            this.keepClear = keepClear;
        }
    }

    private static long[] pack(boolean[] columns) {
        if (columns == null) {
            return null;
        }
        long[] bits = new long[4];
        boolean any = false;
        for (int i = 0; i < 256; i++) {
            if (columns[i]) {
                bits[i >> 6] |= 1L << (i & 63);
                any = true;
            }
        }
        return any ? bits : null;
    }

    /**
     * Returns a short description of what stands here, for the finder and logs: the building's or street's asset name,
     * or the empty string.
     */
    public String label() {
        return label;
    }

    /**
     * Returns the openings on one side, where a street border is one block lower so a slope, stairs or a bridge from
     * the neighbor can come through: bit {@code i} for position {@code i} along the side.
     *
     * @param side the side
     * @return a 16-bit mask
     */
    public int borderOpenings(Side side) {
        resolveEdges();
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
        private ChunkContent network = ChunkContent.NONE;
        private boolean highway;
        private short[] footprint;
        private int guardBottom = Integer.MAX_VALUE;
        private int guardTop = Integer.MIN_VALUE;
        private LanePath lane;
        private final List<PopulationJob> jobs = new ArrayList<>();
        private Supplier<List<PopulationJob>> jobSource;
        private final int[] borderOpenings = new int[4];
        private long[] keepClear;
        private Supplier<Edges> edgeSource;
        private String label = "";
        private boolean lot;
        private boolean scattered;
        private final List<GroundFit> groundFits = new ArrayList<>();
        private ChunkContent finish = ChunkContent.NONE;

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

        /** Sets the network writer (highways, railways, rail dungeon). */
        public Builder network(ChunkContent value) {
            this.network = Objects.requireNonNull(value);
            return this;
        }

        /** Marks the chunk as one a highway passes through. */
        public Builder highway(boolean value) {
            this.highway = value;
            return this;
        }

        /**
         * Adds a block Y range to the guarded band ({@link ChunkPlan#inGuardBand}); the band spans every range added.
         *
         * @param bottom lowest block Y, inclusive
         * @param top    highest block Y, inclusive; below {@code bottom} adds nothing
         * @return this builder
         */
        public Builder guardBand(int bottom, int top) {
            if (bottom <= top) {
                this.guardBottom = Math.min(this.guardBottom, bottom);
                this.guardTop = Math.max(this.guardTop, top);
            }
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

        /**
         * Sets a pure, lazily asked source of further jobs, which run after the jobs added with {@link #job}.
         *
         * @param source the source; asked at most once per plan
         * @return this
         */
        public Builder jobSource(Supplier<List<PopulationJob>> source) {
            this.jobSource = Objects.requireNonNull(source);
            return this;
        }

        /**
         * Adds a pure, lazily asked source of jobs that run after those of the source already set: a chunk's highways
         * and railways after its city, bridge or scattered structure (M06).
         *
         * @param more the source; asked at most once per plan
         * @return this
         */
        public Builder addJobSource(Supplier<List<PopulationJob>> more) {
            Supplier<List<PopulationJob>> first = jobSource;
            if (first == null) {
                return jobSource(more);
            }
            jobSource = () -> {
                List<PopulationJob> all = new ArrayList<>(first.get());
                all.addAll(more.get());
                return all;
            };
            return this;
        }

        /**
         * Marks the columns of door approaches, which the rubble layer leaves clear.
         *
         * @param columns 256 flags, index {@code z × 16 + x}; {@code null} for none
         * @return this
         */
        public Builder keepClear(boolean[] columns) {
            this.keepClear = pack(columns);
            return this;
        }

        /**
         * Sets a pure, lazily asked source of the border openings and door approaches, replacing what
         * {@link #borderOpenings} and {@link #keepClear} set.
         *
         * @param source the source; asked at most once per plan
         * @return this
         */
        public Builder edgeSource(Supplier<Edges> source) {
            this.edgeSource = Objects.requireNonNull(source);
            return this;
        }

        /** Marks the chunk as an edge lot (0011 §3). */
        public Builder lot(boolean value) {
            this.lot = value;
            return this;
        }

        /** Marks the chunk as part of a scattered structure's footprint. */
        public Builder scattered(boolean value) {
            this.scattered = value;
            return this;
        }

        /** Adds a ground fit that reaches this chunk; fits apply in the order added. */
        public Builder groundFit(GroundFit value) {
            groundFits.add(Objects.requireNonNull(value));
            return this;
        }

        /** Sets the writer of a city chunk's last step. */
        public Builder finish(ChunkContent value) {
            this.finish = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the label: what stands here, for the finder and logs. */
        public Builder label(String value) {
            this.label = Objects.requireNonNull(value);
            return this;
        }

        /** Sets the border openings of one side (a 16-bit mask). */
        public Builder borderOpenings(Side side, int mask) {
            borderOpenings[side.ordinal()] = mask & 0xFFFF;
            return this;
        }

        /** Returns the immutable plan. */
        public ChunkPlan build() {
            if (kind != ChunkKind.CITY && building) {
                throw new IllegalStateException("only a city chunk has a building: " + chunkX + "," + chunkZ);
            }
            if (kind == ChunkKind.LANDMARK_CORE && content != ChunkContent.NONE) {
                // 0006 §1: the city writes nothing in a core but a village lane's surface path.
                throw new IllegalStateException("a landmark core has no content: " + chunkX + "," + chunkZ);
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
