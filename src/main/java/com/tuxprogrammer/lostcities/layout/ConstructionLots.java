package com.tuxprogrammer.lostcities.layout;

import com.tuxprogrammer.lostcities.layout.DensityRings.BuildingClass;
import com.tuxprogrammer.lostcities.layout.DensityRings.Ring;
import com.tuxprogrammer.lostcities.layout.random.QualityRandom;

/**
 * Construction lots at real sizes (city-realism phase 3, decisions D-p3-1 to D-p3-4): a ring construction lot stands in
 * for a building of its ring's class, so it takes that class's footprint (1 × 1, 2 × 1 or 2 × 2 chunks, like the
 * multibuildings), digs as deep as that class's cellars and raises its frame a random part of that class's floors.
 * Original to this mod; upstream has no lots.
 *
 * <p>
 * Lots are drawn per <em>cell</em>, an aligned {@value #CELL} × {@value #CELL} square of chunks, the way multibuildings
 * are drawn per multichunk: a footprint never crosses a cell border, so two lots never overlap and every chunk of a
 * footprint reaches the same draw from the cell's coordinates alone, whatever order the chunks are planned in. The
 * draw is a pure function of the world seed, the cell and the ring distance at the cell's corner; whether the chunks
 * can hold it is the planner's business ({@link LayoutPlanner}).
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ConstructionLots {

    /** Side of a cell, in chunks: the largest footprint. */
    public static final int CELL = 2;

    /** Salt of a cell's random. */
    static final long CELL_SALT = 0x434f4e5354524354L;

    /** Footprint sizes, in chunks: 1 × 1, 2 × 1 (either way round) and 2 × 2. */
    private static final int[] SIZE_AREA = { 1, 2, 4 };

    /**
     * Weights of the sizes per class, rows by {@link BuildingClass}, columns by {@link #SIZE_AREA}. From the 2b audit:
     * the towers are building1 and building8 (single chunks) and multi1, huge1 and huge2 (2 × 2 and larger); every
     * high-rise is a 2 × 2 multibuilding; mid-rises are single chunks but for the two 2 × 2 shopping centers;
     * low-rises and houses are single chunks. The 2 × 1 share stands for the long buildings (highway_restaurant) and
     * for a half-built 2 × 2.
     */
    private static final float[][] SIZE_WEIGHTS = { { 0.4f, 0.2f, 0.4f }, // tower
        { 0.1f, 0.3f, 0.6f }, // high-rise
        { 0.7f, 0.2f, 0.1f }, // mid-rise
        { 0.85f, 0.15f, 0f }, // low-rise
        { 1f, 0f, 0f } }; // house

    /** Cellars per class, {@code {min, max}}: the five originals and the cabin have none, towers dig deepest. */
    private static final int[][] CELLARS = { { 2, 3 }, { 1, 3 }, { 1, 2 }, { 0, 1 }, { 0, 0 } };

    /**
     * Floors per class, {@code {min, max}}; a maximum of 0 stands for {@code buildingMaxFloors}. Houses are 1 to 2
     * floors and low-rises 2 to 3 (the originals' JSON), mid-rises 3 to 6 (the shopping centers' 4 to 6 and the
     * plain singles), high-rises and towers reach the profile's cap.
     */
    private static final int[][] FLOORS = { { 7, 0 }, { 5, 0 }, { 3, 6 }, { 2, 3 }, { 1, 2 } };

    private ConstructionLots() {}

    /** A drawn lot: its class, footprint, kind, cellars and frame. */
    public static final class Draw {

        /** The building class the lot stands in for. */
        public final BuildingClass cls;
        /** Chunk X and Z of the footprint's top-left chunk. */
        public final int x;
        public final int z;
        /** Footprint size, in chunks. */
        public final int dimX;
        public final int dimZ;
        /** The lot's kind before the planner's checks. */
        public final LotKind kind;
        /** Cellars of the class, before the planner fits them above the world's floor. */
        public final int cellars;
        /** Floors of the class building the frame belongs to. */
        public final int classFloors;
        /** Floors the frame rises: 1 to {@link #classFloors}. */
        public final int frameFloors;

        Draw(BuildingClass cls, int x, int z, int dimX, int dimZ, LotKind kind, int cellars, int classFloors,
            int frameFloors) {
            this.cls = cls;
            this.x = x;
            this.z = z;
            this.dimX = dimX;
            this.dimZ = dimZ;
            this.kind = kind;
            this.cellars = cellars;
            this.classFloors = classFloors;
            this.frameFloors = frameFloors;
        }

        /** Whether a chunk lies in the footprint. */
        public boolean contains(int chunkX, int chunkZ) {
            return chunkX >= x && chunkX < x + dimX && chunkZ >= z && chunkZ < z + dimZ;
        }

        /** The same lot shrunk to its top-left chunk. */
        Draw single() {
            return new Draw(cls, x, z, 1, 1, kind, cellars, classFloors, frameFloors);
        }
    }

    /** Returns the cell a chunk coordinate falls in. */
    public static int cell(int chunk) {
        return Math.floorDiv(chunk, CELL);
    }

    /**
     * Returns the chance that a cell holds a construction lot, so lots cover as much of a ring as D308's per-chunk
     * chance did: {@code lotChance × CELL² / mean area}, the mean area over the ring's class mix.
     *
     * @param d        ring distance at the cell's corner
     * @param strength {@code densityRings}
     * @return the chance, 0 to 1
     */
    public static float cellChance(float d, float strength) {
        float area = meanArea(Ring.of(d), strength);
        return area <= 0f ? 0f : Math.min(1f, DensityRings.lotChance(d, strength) * CELL * CELL / area);
    }

    /** The mean footprint area, in chunks, of a lot in a ring. */
    static float meanArea(Ring ring, float strength) {
        float total = 0f;
        float area = 0f;
        for (BuildingClass cls : BuildingClass.values()) {
            float w = DensityRings.acceptance(cls, ring, strength);
            total += w;
            float[] sizes = SIZE_WEIGHTS[cls.ordinal()];
            for (int i = 0; i < sizes.length; i++) {
                area += w * sizes[i] * SIZE_AREA[i];
            }
        }
        return total <= 0f ? 0f : area / total;
    }

    /**
     * Draws a cell's lot. Every chunk of the cell gets the same answer: the draw reads only the seed, the cell and the
     * ring distance at the cell's top-left chunk, with a fixed number of draws.
     *
     * @param seed      the world seed
     * @param cellX     cell X
     * @param cellZ     cell Z
     * @param d         ring distance at the cell's top-left chunk
     * @param strength  {@code densityRings}
     * @param maxFloors {@code buildingMaxFloors}
     * @return the lot, or {@code null} when the cell holds none
     */
    public static Draw draw(long seed, int cellX, int cellZ, float d, float strength, int maxFloors) {
        QualityRandom r = new QualityRandom((seed + cellZ * 341873128712L + cellX * 132897987541L) ^ CELL_SALT);
        float roll = r.nextFloat();
        float classRoll = r.nextFloat();
        float sizeRoll = r.nextFloat();
        float posRoll = r.nextFloat();
        float kindRoll = r.nextFloat();
        float cellarRoll = r.nextFloat();
        float floorRoll = r.nextFloat();
        float frameRoll = r.nextFloat();
        if (strength <= 0f || roll >= cellChance(d, strength)) {
            return null;
        }
        Ring ring = Ring.of(d);
        BuildingClass cls = pickClass(ring, strength, classRoll);
        int size = pick(SIZE_WEIGHTS[cls.ordinal()], sizeRoll);
        int baseX = cellX * CELL;
        int baseZ = cellZ * CELL;
        int dimX;
        int dimZ;
        int x = baseX;
        int z = baseZ;
        int which = Math.min(3, (int) (posRoll * 4));
        if (SIZE_AREA[size] == 4) {
            dimX = 2;
            dimZ = 2;
        } else if (SIZE_AREA[size] == 2) {
            // Long way along X or along Z, in either half of the cell.
            boolean alongX = (which & 1) == 0;
            dimX = alongX ? 2 : 1;
            dimZ = alongX ? 1 : 2;
            if (alongX) {
                z += which >> 1;
            } else {
                x += which >> 1;
            }
        } else {
            dimX = 1;
            dimZ = 1;
            x += which & 1;
            z += which >> 1;
        }
        LotKind kind = kind(kindRoll);
        int[] cellars = CELLARS[cls.ordinal()];
        int cellarCount = cellars[0]
            + Math.min(cellars[1] - cellars[0], (int) (cellarRoll * (cellars[1] - cellars[0] + 1)));
        int floors = classFloors(cls, maxFloors, floorRoll);
        int frame = 1 + Math.min(floors - 1, (int) (frameRoll * floors));
        return new Draw(cls, x, z, dimX, dimZ, kind, cellarCount, floors, frame);
    }

    /**
     * Returns a class's floors for a roll: uniform between the class's bounds, both clamped to {@code maxFloors}.
     *
     * @param cls       the class
     * @param maxFloors {@code buildingMaxFloors}
     * @param roll      a roll in [0, 1)
     * @return the floors, at least 1
     */
    static int classFloors(BuildingClass cls, int maxFloors, float roll) {
        int[] bounds = FLOORS[cls.ordinal()];
        int cap = Math.max(1, maxFloors);
        int hi = bounds[1] == 0 ? cap : Math.min(bounds[1], cap);
        int lo = Math.min(bounds[0], hi);
        return lo + Math.min(hi - lo, (int) (roll * (hi - lo + 1)));
    }

    /** The cellar bounds of a class, {@code {min, max}}. */
    static int[] cellarBounds(BuildingClass cls) {
        return CELLARS[cls.ordinal()].clone();
    }

    /** The floor bounds of a class at a profile maximum, {@code {min, max}}. */
    static int[] floorBounds(BuildingClass cls, int maxFloors) {
        return new int[] { classFloors(cls, maxFloors, 0f), classFloors(cls, maxFloors, 0.999999f) };
    }

    /** The class a lot stands in for: weighted by the ring's acceptance of each class. */
    static BuildingClass pickClass(Ring ring, float strength, float roll) {
        BuildingClass[] classes = BuildingClass.values();
        float[] w = new float[classes.length];
        for (int i = 0; i < classes.length; i++) {
            w[i] = DensityRings.acceptance(classes[i], ring, strength);
        }
        return classes[pick(w, roll)];
    }

    /** The share of a size among a class's footprints: 0 for 1 × 1, 1 for 2 × 1, 2 for 2 × 2. */
    static float sizeWeight(BuildingClass cls, int size) {
        float[] w = SIZE_WEIGHTS[cls.ordinal()];
        float total = 0f;
        for (float v : w) {
            total += v;
        }
        return w[size] / total;
    }

    private static LotKind kind(float roll) {
        float[] weights = LayoutPlanner.LOT_KIND_WEIGHTS;
        return roll < weights[0] ? LotKind.BARE : roll < weights[0] + weights[1] ? LotKind.PIT : LotKind.FOUNDATION;
    }

    private static int pick(float[] weights, float roll) {
        float total = 0f;
        for (float w : weights) {
            total += w;
        }
        float target = roll * total;
        float running = 0f;
        int last = 0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] <= 0f) {
                continue;
            }
            last = i;
            running += weights[i];
            if (target < running) {
                return i;
            }
        }
        return last;
    }
}
