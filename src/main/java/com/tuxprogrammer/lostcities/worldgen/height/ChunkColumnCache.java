package com.tuxprogrammer.lostcities.worldgen.height;

/**
 * A bounded per-chunk cache in front of a height oracle that computes whole chunks anyway (classic RWG): the first
 * column asked of a chunk fills all 256, and the rest of that chunk is an array read. The layout's chunk samples and
 * the structure oracles' ground estimates (several columns per village) then share one computation per chunk.
 *
 * <p>
 * A direct-mapped table of {@link #CAPACITY} chunks owned by the world; eviction never changes an answer, since the
 * oracle is pure. Not thread-safe; server thread only. Side: server, and plain Java in tests.
 */
public final class ChunkColumnCache implements TerrainHeightOracle {

    /** Chunks cached: 256 KB of heights. */
    public static final int CAPACITY = 256;

    /** Bytes the cache holds at most. */
    public static final long MAX_BYTES = CAPACITY * (256L * Integer.BYTES + Long.BYTES);

    private static final int MASK = CAPACITY - 1;

    private final TerrainHeightOracle oracle;
    private final long[] keys = new long[CAPACITY];
    private final int[][] columns = new int[CAPACITY][];

    /**
     * Wraps an oracle.
     *
     * @param oracle the whole-chunk oracle
     */
    public ChunkColumnCache(TerrainHeightOracle oracle) {
        this.oracle = oracle;
    }

    /**
     * Returns an oracle with this cache in front when it computes whole chunks, else the oracle itself.
     *
     * @param oracle the oracle
     * @return the oracle to use
     */
    public static TerrainHeightOracle wrap(TerrainHeightOracle oracle) {
        return oracle.computesWholeChunks() ? new ChunkColumnCache(oracle) : oracle;
    }

    @Override
    public int height(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        int slot = ((chunkX * 0x9E3779B1) ^ (chunkZ * 0x85EBCA77)) >>> 7 & MASK;
        int[] heights = columns[slot];
        if (heights == null || keys[slot] != key) {
            if (heights == null) {
                heights = new int[256];
                columns[slot] = heights;
            }
            oracle.chunkHeights(chunkX, chunkZ, heights);
            keys[slot] = key;
        }
        return heights[(blockX & 15) * 16 + (blockZ & 15)];
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
}
