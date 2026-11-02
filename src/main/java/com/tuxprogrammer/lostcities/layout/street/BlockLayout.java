package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/HierarchicalStreetPlanner.java

import java.util.Collections;
import java.util.List;

/**
 * One block of the hierarchical street field: the rectangle between two consecutive active primary roads on each axis,
 * its density and its secondary roads. Upstream's {@code HierarchicalStreetPlanner.BlockLayout} record as a Java 8
 * value class; {@link HierarchicalStreetPlanner#getBlockLayout(int, int)} builds it.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md#blocks. Side: both.
 */
public final class BlockLayout {

    private final int blockX;
    private final int blockZ;
    private final int westX;
    private final int northZ;
    private final int eastX;
    private final int southZ;
    private final double density;
    private final List<Integer> secondaryX;
    private final List<Integer> secondaryZ;

    BlockLayout(int blockX, int blockZ, int westX, int northZ, int eastX, int southZ, double density,
        List<Integer> secondaryX, List<Integer> secondaryZ) {
        this.blockX = blockX;
        this.blockZ = blockZ;
        this.westX = westX;
        this.northZ = northZ;
        this.eastX = eastX;
        this.southZ = southZ;
        this.density = density;
        this.secondaryX = Collections.unmodifiableList(secondaryX);
        this.secondaryZ = Collections.unmodifiableList(secondaryZ);
    }

    /** Returns the block's primary candidate index along X. */
    public int blockX() {
        return blockX;
    }

    /** Returns the block's primary candidate index along Z. */
    public int blockZ() {
        return blockZ;
    }

    /** Returns the chunk X of the block's west primary road. */
    public int westX() {
        return westX;
    }

    /** Returns the chunk Z of the block's north primary road. */
    public int northZ() {
        return northZ;
    }

    /** Returns the chunk X of the block's east primary road. */
    public int eastX() {
        return eastX;
    }

    /** Returns the chunk Z of the block's south primary road. */
    public int southZ() {
        return southZ;
    }

    /** Returns the block's density, in {@code [0, 1)}. */
    public double density() {
        return density;
    }

    /** Returns the chunk X of each secondary road running north-south, ascending. */
    public List<Integer> secondaryX() {
        return secondaryX;
    }

    /** Returns the chunk Z of each secondary road running east-west, ascending. */
    public List<Integer> secondaryZ() {
        return secondaryZ;
    }
}
