package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Jittered-grid city centers (profile option {@code cityPlacement=grid}): the world is cut into square cells of
 * {@code cell} chunks, and each cell holds exactly one city center, placed uniformly inside the cell's middle part,
 * {@code jitter} chunks in from every edge, so neighbors are at least {@code cell - 2 * jitter} chunks apart. The
 * radius is drawn the way {@link CityMap} draws it, {@code minRadius + nextInt(maxRadius - minRadius)}.
 *
 * <p>
 * Every cell's {@link Random} is seeded from the world seed and the cell coordinates alone, so a center never depends
 * on the query window, on other cells or on terrain, and lookups cost one {@code Random} per cell, never one per
 * chunk. This class is pure layout: it knows nothing of the terrain filters (ocean, connectivity, the city mask); see
 * {@link LayoutPlanner#centerYieldsCity} for those.
 *
 * <p>
 * Original code. The seed multipliers are unrelated to {@link CityMap}'s so a grid world does not echo the chance
 * layout.
 */
public final class CityGrid {

    /** Per-cell seed multipliers: large odd numbers. */
    private static final long CELL_Z = 25214903917L * 2L + 1L;
    private static final long CELL_X = 2685821657736338717L;

    /** One city center: chunk coordinates and radius in blocks. */
    public static final class Center {

        private final int chunkX;
        private final int chunkZ;
        private final int radius;

        Center(int chunkX, int chunkZ, int radius) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.radius = radius;
        }

        /** Returns the center chunk's X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the center chunk's Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the city radius in blocks. */
        public int radius() {
            return radius;
        }
    }

    private final long seed;
    private final int cell;
    private final int jitter;
    private final int span;
    private final int minRadius;
    private final int maxRadius;

    /**
     * Creates a grid.
     *
     * @param seed      the world seed
     * @param cell      cell edge in chunks, at least 1
     * @param jitter    chunks kept clear inside every cell edge; {@code cell - 2 * jitter} must be at least 1
     * @param minRadius smallest radius in blocks
     * @param maxRadius end of the radius range (exclusive), blocks
     */
    public CityGrid(long seed, int cell, int jitter, int minRadius, int maxRadius) {
        if (cell < 1 || jitter < 0 || cell - 2 * jitter < 1) {
            throw new IllegalArgumentException("bad grid: cell " + cell + ", jitter " + jitter);
        }
        this.seed = seed;
        this.cell = cell;
        this.jitter = jitter;
        this.span = cell - 2 * jitter;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
    }

    /** Returns the cell edge in chunks. */
    public int cell() {
        return cell;
    }

    /**
     * Returns the center of one cell.
     *
     * @param cellX cell X (chunk X divided by the cell size, rounded down)
     * @param cellZ cell Z
     * @return the cell's center
     */
    public Center center(int cellX, int cellZ) {
        Random random = new Random(seed + cellZ * CELL_Z + cellX * CELL_X);
        int x = cellX * cell + jitter + random.nextInt(span);
        int z = cellZ * cell + jitter + random.nextInt(span);
        int radius = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius));
        return new Center(x, z, radius);
    }

    /** Returns whether a chunk is a grid center. Costs one cell lookup. */
    public boolean isCenter(int chunkX, int chunkZ) {
        Center c = center(Math.floorDiv(chunkX, cell), Math.floorDiv(chunkZ, cell));
        return c.chunkX == chunkX && c.chunkZ == chunkZ;
    }

    /**
     * Returns the radius of the center at a chunk, or {@code -1} when the chunk is not a center.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the radius in blocks, or -1
     */
    public int radiusAt(int chunkX, int chunkZ) {
        Center c = center(Math.floorDiv(chunkX, cell), Math.floorDiv(chunkZ, cell));
        return c.chunkX == chunkX && c.chunkZ == chunkZ ? c.radius : -1;
    }

    /**
     * Returns the centers whose cells intersect a chunk window, cells in X-major then Z order. Costs one lookup per
     * cell of the window.
     *
     * @param minChunkX first chunk X of the window
     * @param minChunkZ first chunk Z
     * @param maxChunkX last chunk X (inclusive)
     * @param maxChunkZ last chunk Z (inclusive)
     * @return the centers; a center may lie outside the window, inside its cell
     */
    public List<Center> centersInCellsOf(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        List<Center> out = new ArrayList<>();
        for (int cx = Math.floorDiv(minChunkX, cell); cx <= Math.floorDiv(maxChunkX, cell); cx++) {
            for (int cz = Math.floorDiv(minChunkZ, cell); cz <= Math.floorDiv(maxChunkZ, cell); cz++) {
                out.add(center(cx, cz));
            }
        }
        return out;
    }

    /**
     * Returns the centers that lie inside the square of half-width {@code r} chunks around a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param r      half-width in chunks
     * @return the centers inside the square
     */
    public List<Center> centersNear(int chunkX, int chunkZ, int r) {
        List<Center> out = new ArrayList<>();
        for (Center c : centersInCellsOf(chunkX - r, chunkZ - r, chunkX + r, chunkZ + r)) {
            if (Math.abs(c.chunkX - chunkX) <= r && Math.abs(c.chunkZ - chunkZ) <= r) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Returns the centers whose cells lie in {@code [min, max)} on both axes, as the preview dump lists them.
     *
     * @param min first chunk of the window, a multiple of the cell size
     * @param max end of the window (exclusive), a multiple of the cell size
     * @return the centers
     */
    public List<Center> centersInWindow(int min, int max) {
        return centersInCellsOf(min, min, max - 1, max - 1);
    }
}
