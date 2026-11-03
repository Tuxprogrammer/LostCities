package com.tuxprogrammer.lostcities.worldgen.height;

/**
 * A bounded per-column cache in front of a height oracle that computes one column per call (RWG2's), so the plan
 * prefetcher's probe ({@link HeightProbe}) can fetch a burst of columns one at a time, as
 * {@link ChunkColumnCache} lets it fetch one chunk at a time on classic RWG (docs/design/0005-rwg-overlay.md §8).
 *
 * <p>
 * A {@link #WAYS}-way set-associative table of {@link #CAPACITY} columns owned by the world; eviction never changes an
 * answer, since the oracle is pure. While the probe is active, a miss throws instead of computing. Not thread-safe;
 * server thread only. Side: server, and plain Java in tests.
 */
public final class ColumnHeightCache implements TerrainHeightOracle {

    /** Columns cached: 8,192, about 100 KB; a burst of fetches reads a few hundred. */
    public static final int CAPACITY = 8192;

    /** Entries per set. */
    public static final int WAYS = 8;

    /** Bytes the cache holds at most. */
    public static final long MAX_BYTES = CAPACITY * (long) (Long.BYTES + Integer.BYTES + 1) + CAPACITY / WAYS;

    private static final int SETS = CAPACITY / WAYS;
    private static final int SET_SHIFT = Integer.SIZE - Integer.numberOfTrailingZeros(SETS);

    private final TerrainHeightOracle oracle;
    private final HeightProbe probe;
    private final long[] keys = new long[CAPACITY];
    private final int[] heights = new int[CAPACITY];
    private final boolean[] filled = new boolean[CAPACITY];
    private final byte[] victims = new byte[SETS];
    private long computations;

    /**
     * Wraps an oracle.
     *
     * @param oracle the per-column oracle
     * @param probe  the world's probe: while it is active, a miss throws {@link HeightProbe.NotCached}
     */
    public ColumnHeightCache(TerrainHeightOracle oracle, HeightProbe probe) {
        this.oracle = oracle;
        this.probe = probe;
    }

    @Override
    public int height(int blockX, int blockZ) {
        long key = key(blockX, blockZ);
        int entry = find(key, blockX, blockZ);
        if (entry >= 0) {
            return heights[entry];
        }
        if (probe.active()) {
            throw probe.miss(() -> height(blockX, blockZ));
        }
        int height = oracle.height(blockX, blockZ);
        computations++;
        int set = set(blockX, blockZ);
        int slot = set * WAYS + victim(set);
        keys[slot] = key;
        heights[slot] = height;
        filled[slot] = true;
        return height;
    }

    @Override
    public boolean isCached(int blockX, int blockZ) {
        return find(key(blockX, blockZ), blockX, blockZ) >= 0;
    }

    @Override
    public long computations() {
        return computations;
    }

    @Override
    public HeightProbe probe() {
        return probe;
    }

    private int find(long key, int blockX, int blockZ) {
        int base = set(blockX, blockZ) * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (filled[base + way] && keys[base + way] == key) {
                return base + way;
            }
        }
        return -1;
    }

    /** Picks the way a new column of a set takes: an empty one, else the oldest (round robin). */
    private int victim(int set) {
        int base = set * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (!filled[base + way]) {
                return way;
            }
        }
        int way = victims[set];
        victims[set] = (byte) ((way + 1) % WAYS);
        return way;
    }

    private static long key(int blockX, int blockZ) {
        return ((long) blockX << 32) | (blockZ & 0xFFFFFFFFL);
    }

    private static int set(int blockX, int blockZ) {
        return ((blockX * 0x9E3779B1) ^ (blockZ * 0x85EBCA77)) >>> SET_SHIFT;
    }
}
