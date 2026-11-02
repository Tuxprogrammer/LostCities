package com.tuxprogrammer.lostcities.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.view.ScatteredView;

/**
 * One scattered area's plan (upstream's {@code ScatteredPlan} record): whether the area holds a structure, which one,
 * where its footprint lies, how it is turned, the block Y of its first floor, the ground it is set into, and each
 * footprint chunk's share: its building, floor count and the part of every floor, drawn once from the plan's replayed
 * random (docs/original-spec/07-scattered/scattered-structures.md, {@code calculatePlan} and
 * {@code generateScatteredBuilding}).
 *
 * <p>
 * Every value is a pure function of the world seed, the area, the pinned profile, the assets, the height oracle, the
 * biome provider, the city layout and the highways, so a plan never changes once made. Immutable.
 *
 * <p>
 * Spec: docs/new-spec/07-scattered/index.md. Side: server (world generation), and plain Java in tests.
 */
public final class ScatteredPlan {

    /** How a structure is turned: upstream's four rotations, clockwise seen from above. */
    public enum Rotation {

        /** As drawn; its front faces north (−Z). */
        NONE(Direction.ZMIN),
        /** A quarter turn; its front faces east (+X). */
        ROTATE_90(Direction.XMAX),
        /** A half turn; its front faces south (+Z). */
        ROTATE_180(Direction.ZMAX),
        /** Three quarter turns; its front faces west (−X). */
        ROTATE_270(Direction.XMIN);

        private final Direction front;

        Rotation(Direction front) {
            this.front = front;
        }

        /** Returns the side the structure's front faces: where its highway runs (upstream's connection direction). */
        public Direction front() {
            return front;
        }

        /** Returns whether the footprint's width and depth swap. */
        public boolean swapsDimensions() {
            return this == ROTATE_90 || this == ROTATE_270;
        }
    }

    /** A plan with no structure: scattered structures are off. */
    static final ScatteredPlan NONE = new ScatteredPlan("disabled");

    /** What one footprint chunk holds: its building and the parts of its floors, bottom up. */
    public static final class Share {

        private final int chunkX;
        private final int chunkZ;
        private final String building;
        private final String[] parts;
        private final String[] parts2;

        /**
         * Describes a share; the planner makes them, and tests.
         *
         * @param chunkX   chunk X
         * @param chunkZ   chunk Z
         * @param building the building's name
         * @param parts    each floor's part, bottom up, at least one
         * @param parts2   each floor's overlay part or {@code null}, as many as {@code parts}
         */
        public Share(int chunkX, int chunkZ, String building, String[] parts, String[] parts2) {
            if (parts.length == 0 || parts.length != parts2.length) {
                throw new IllegalArgumentException("a share has as many overlay parts as floors, at least one");
            }
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.building = building;
            this.parts = parts.clone();
            this.parts2 = parts2.clone();
        }

        /** Returns chunk X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns chunk Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the building's name, a full id. */
        public String building() {
            return building;
        }

        /** Returns the floor count, at least 1. */
        public int floors() {
            return parts.length;
        }

        /**
         * Returns a floor's part.
         *
         * @param floor 0 to {@link #floors()} − 1
         * @return the part's name
         */
        public String part(int floor) {
            return parts[floor];
        }

        /**
         * Returns a floor's overlay part ({@code parts2}).
         *
         * @param floor 0 to {@link #floors()} − 1
         * @return the part's name, or {@code null}
         */
        public String part2(int floor) {
            return parts2[floor];
        }
    }

    private final boolean valid;
    private final int areaX;
    private final int areaZ;
    private final String entry;
    private final ScatteredView.Asset asset;
    private final int minChunkX;
    private final int minChunkZ;
    private final int width;
    private final int depth;
    private final Rotation rotation;
    private final int lowestLevel;
    private final Integer highwayHeight;
    private final int minHeight;
    private final int maxHeight;
    private final List<Share> shares;
    private final String reason;

    private ScatteredPlan(String reason) {
        this.reason = reason;
        valid = false;
        areaX = 0;
        areaZ = 0;
        entry = null;
        asset = null;
        minChunkX = 0;
        minChunkZ = 0;
        width = 0;
        depth = 0;
        rotation = Rotation.NONE;
        lowestLevel = 0;
        highwayHeight = null;
        minHeight = 0;
        maxHeight = 0;
        shares = Collections.emptyList();
    }

    /**
     * Describes a planned structure; the planner makes them, and tests.
     *
     * @param areaX         the area's X index
     * @param areaZ         the area's Z index
     * @param entry         the world style entry's name
     * @param asset         the scattered asset
     * @param minChunkX     the footprint's west chunk X
     * @param minChunkZ     the footprint's north chunk Z
     * @param width         the footprint's width, in chunks
     * @param depth         the footprint's depth, in chunks
     * @param rotation      the rotation
     * @param lowestLevel   the block Y of the first floor
     * @param highwayHeight the block Y of the highway it faces, or {@code null}
     * @param minHeight     the lowest oracle sample under it
     * @param maxHeight     the highest oracle sample under it
     * @param shares        each footprint chunk's share, row by row from the north-west
     */
    public ScatteredPlan(int areaX, int areaZ, String entry, ScatteredView.Asset asset, int minChunkX, int minChunkZ,
        int width, int depth, Rotation rotation, int lowestLevel, Integer highwayHeight, int minHeight, int maxHeight,
        Share[] shares) {
        this.valid = true;
        this.reason = "";
        this.areaX = areaX;
        this.areaZ = areaZ;
        this.entry = entry;
        this.asset = asset;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.width = width;
        this.depth = depth;
        this.rotation = rotation;
        this.lowestLevel = lowestLevel;
        this.highwayHeight = highwayHeight;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.shares = Collections.unmodifiableList(Arrays.asList(shares.clone()));
    }

    /**
     * Returns an area without a structure, saying why.
     *
     * @param reason why: the chance, the draw of "none", or the check the placement failed
     * @return the plan
     */
    static ScatteredPlan none(String reason) {
        return new ScatteredPlan(reason);
    }

    /** Returns why the area holds no structure, for the rate scenario; empty for a structure. */
    public String reason() {
        return reason;
    }

    /** Returns whether the area holds a structure. */
    public boolean valid() {
        return valid;
    }

    /** Returns the area's X index. */
    public int areaX() {
        return areaX;
    }

    /** Returns the area's Z index. */
    public int areaZ() {
        return areaZ;
    }

    /** Returns the world style entry's name, a full id; {@code null} without a structure. */
    public String entry() {
        return entry;
    }

    /** Returns the scattered asset; {@code null} without a structure. */
    public ScatteredView.Asset asset() {
        return asset;
    }

    /** Returns the footprint's west chunk X. */
    public int minChunkX() {
        return minChunkX;
    }

    /** Returns the footprint's north chunk Z. */
    public int minChunkZ() {
        return minChunkZ;
    }

    /** Returns the footprint's width along X, in chunks, after the rotation. */
    public int width() {
        return width;
    }

    /** Returns the footprint's depth along Z, in chunks, after the rotation. */
    public int depth() {
        return depth;
    }

    /** Returns the rotation. */
    public Rotation rotation() {
        return rotation;
    }

    /** Returns the block Y of the first floor's bottom slice. */
    public int lowestLevel() {
        return lowestLevel;
    }

    /** Returns the block Y of the highway the structure faces, or {@code null} when it is not placed by one. */
    public Integer highwayHeight() {
        return highwayHeight;
    }

    /**
     * Returns the Y of the ground the structure is set into: the top solid block of the height it was placed at,
     * {@code lowestLevel − heightOffset − 1}. A cabin's three foundation slices end level with it, and a gas station's
     * pavement stands one block above it.
     */
    public int groundY() {
        return lowestLevel - asset.heightOffset() - 1;
    }

    /** Returns the lowest oracle height sampled under the footprint. */
    public int minHeight() {
        return minHeight;
    }

    /** Returns the highest oracle height sampled under the footprint. */
    public int maxHeight() {
        return maxHeight;
    }

    /**
     * Returns whether a chunk lies in the footprint.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code false} without a structure
     */
    public boolean covers(int chunkX, int chunkZ) {
        return valid && chunkX >= minChunkX
            && chunkZ >= minChunkZ
            && chunkX < minChunkX + width
            && chunkZ < minChunkZ + depth;
    }

    /**
     * Returns whether a chunk lies within some chunks of the footprint (Chebyshev distance).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param reach  chunks around the footprint
     * @return {@code false} without a structure
     */
    public boolean reaches(int chunkX, int chunkZ, int reach) {
        return valid && chunkX >= minChunkX - reach
            && chunkZ >= minChunkZ - reach
            && chunkX < minChunkX + width + reach
            && chunkZ < minChunkZ + depth + reach;
    }

    /**
     * Returns a footprint chunk's share.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the share, or {@code null} outside the footprint
     */
    public Share share(int chunkX, int chunkZ) {
        if (!covers(chunkX, chunkZ)) {
            return null;
        }
        return shares.get((chunkZ - minChunkZ) * width + chunkX - minChunkX);
    }

    /** Returns every footprint chunk's share, row by row from the north-west. */
    public List<Share> shares() {
        return shares;
    }

    @Override
    public String toString() {
        if (!valid) {
            return "ScatteredPlan[none]";
        }
        return "ScatteredPlan[" + entry
            + " at "
            + minChunkX
            + ","
            + minChunkZ
            + " "
            + width
            + "x"
            + depth
            + " "
            + rotation
            + " lowest="
            + lowestLevel
            + "]";
    }
}
