package com.tuxprogrammer.lostcities.worldgen.height;

/**
 * A bounded per-chunk cache in front of a height oracle that computes whole chunks anyway (classic RWG): the first
 * column asked of a chunk fills all 256, and the rest of that chunk is an array read. The layout's chunk samples, the
 * scattered planner's columns and the structure oracles' site tests then share one computation per chunk.
 *
 * <p>
 * A {@link #WAYS}-way set-associative table of {@link #CAPACITY} chunks owned by the world; eviction never changes an
 * answer, since the oracle is pure. It holds the few hundred chunks a burst of fetches reads, so the plan prefetcher
 * can fetch them one at a time without a later one evicting an earlier one. While the world's
 * {@link HeightProbe} is active, a miss throws instead of computing. Not thread-safe; server thread only. Side:
 * server, and plain Java in tests.
 */
public final class ChunkColumnCache implements TerrainHeightOracle {

    /** Chunks cached: 1,024, about 1 MB of heights. */
    public static final int CAPACITY = 1024;

    /** Entries per set: eight, so a working set of a few hundred chunks never overflows a set. */
    public static final int WAYS = 8;

    /** Bytes the cache holds at most. */
    public static final long MAX_BYTES = CAPACITY * (256L * Integer.BYTES + Long.BYTES) + CAPACITY / WAYS;

    private static final int SETS = CAPACITY / WAYS;
    private static final int SET_SHIFT = Integer.SIZE - Integer.numberOfTrailingZeros(SETS);

    private final TerrainHeightOracle oracle;
    private final HeightProbe probe;
    private final long[] keys = new long[CAPACITY];
    private final int[][] columns = new int[CAPACITY][];
    private final byte[] victims = new byte[SETS];
    private long computations;

    /**
     * Wraps an oracle, with a probe that is never active.
     *
     * @param oracle the whole-chunk oracle
     */
    public ChunkColumnCache(TerrainHeightOracle oracle) {
        this(oracle, new HeightProbe());
    }

    /**
     * Wraps an oracle.
     *
     * @param oracle the whole-chunk oracle
     * @param probe  the world's probe: while it is active, a miss throws {@link HeightProbe.NotCached}
     */
    public ChunkColumnCache(TerrainHeightOracle oracle, HeightProbe probe) {
        this.oracle = oracle;
        this.probe = probe;
    }

    /**
     * Returns an oracle with the world's cache in front: this one when the oracle computes whole chunks, a
     * {@link ColumnHeightCache} when it computes columns, none for a constant one, which costs nothing.
     *
     * @param oracle the oracle
     * @param probe  the world's probe
     * @return the oracle to use
     */
    public static TerrainHeightOracle wrap(TerrainHeightOracle oracle, HeightProbe probe) {
        if (oracle.computesWholeChunks()) {
            return new ChunkColumnCache(oracle, probe);
        }
        return oracle instanceof ConstantHeightOracle ? oracle : new ColumnHeightCache(oracle, probe);
    }

    @Override
    public int height(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int entry = find(chunkX, chunkZ);
        int[] heights;
        if (entry >= 0) {
            heights = columns[entry];
        } else {
            if (probe.active()) {
                throw probe.miss(() -> height(blockX, blockZ));
            }
            int set = set(chunkX, chunkZ);
            int slot = set * WAYS + victim(set);
            heights = columns[slot];
            if (heights == null) {
                heights = new int[256];
                columns[slot] = heights;
            }
            // The key goes in after the heights, so a failing oracle leaves the slot unmatched.
            keys[slot] = Long.MIN_VALUE;
            oracle.chunkHeights(chunkX, chunkZ, heights);
            computations++;
            keys[slot] = key(chunkX, chunkZ);
        }
        return heights[(blockX & 15) * 16 + (blockZ & 15)];
    }

    @Override
    public boolean isCached(int blockX, int blockZ) {
        return find(blockX >> 4, blockZ >> 4) >= 0;
    }

    @Override
    public HeightProbe probe() {
        return probe;
    }

    @Override
    public long computations() {
        return computations;
    }

    @Override
    public boolean computesWholeChunks() {
        return true;
    }

    @Override
    public void chunkHeights(int chunkX, int chunkZ, int[] out) {
        for (int i = 0; i < 256; i++) {
            out[i] = height((chunkX << 4) + (i >> 4), (chunkZ << 4) + (i & 15));
        }
    }

    private int find(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        int base = set(chunkX, chunkZ) * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (columns[base + way] != null && keys[base + way] == key) {
                return base + way;
            }
        }
        return -1;
    }

    /** Picks the way a new chunk of a set takes: an empty one, else the oldest (round robin). */
    private int victim(int set) {
        int base = set * WAYS;
        for (int way = 0; way < WAYS; way++) {
            if (columns[base + way] == null) {
                return way;
            }
        }
        int way = victims[set];
        victims[set] = (byte) ((way + 1) % WAYS);
        return way;
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static int set(int chunkX, int chunkZ) {
        return ((chunkX * 0x9E3779B1) ^ (chunkZ * 0x85EBCA77)) >>> SET_SHIFT;
    }
}
