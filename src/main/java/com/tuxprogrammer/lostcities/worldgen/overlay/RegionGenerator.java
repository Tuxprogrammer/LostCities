package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * Generates a square of chunks one at a time in spiral order from its center, over many server ticks, and populates
 * every chunk whose 3 × 3 neighborhood exists, as the worldgen scenarios need (docs/design/0005-rwg-overlay.md §8,
 * "Method" step 3). It goes through {@code ChunkProviderServer}, as the game does, never the generator directly, and
 * with no player, so Hodgepodge's player throttle stays out of the numbers.
 *
 * <p>
 * The wall clock only paces how many chunks a tick handles; what is generated never depends on it. Server thread
 * only. Side: server.
 */
final class RegionGenerator {

    /** Most time one tick spends generating, in nanoseconds, so the server keeps ticking. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;

    private final WorldServer world;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final List<long[]> order;
    private int next;
    private int populateCursor;

    /**
     * Prepares a square.
     *
     * @param world   the world
     * @param centerX the center chunk X
     * @param centerZ the center chunk Z
     * @param radius  half the side, in chunks: the square is {@code 2 × radius + 1} chunks wide
     */
    RegionGenerator(WorldServer world, int centerX, int centerZ, int radius) {
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.order = spiral(centerX, centerZ, radius);
    }

    /** Returns the number of chunks of the square. */
    int size() {
        return order.size();
    }

    /** Returns the half-side of the square this generator populates: two chunks inside the generated one. */
    int innerRadius() {
        return Math.max(0, radius - 2);
    }

    /** Returns how many chunks have been loaded or generated so far. */
    int loaded() {
        return next;
    }

    /**
     * Loads chunks in spiral order until the tick's time budget is spent, then populates what can be.
     *
     * @return {@code true} when every chunk is loaded and every inner chunk populated
     */
    boolean tick() {
        ChunkProviderServer server = world.theChunkProviderServer;
        long start = System.nanoTime();
        while (next < order.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            long[] chunk = order.get(next++);
            server.loadChunk((int) chunk[0], (int) chunk[1]);
        }
        if (next < order.size()) {
            return false;
        }
        // Populate the inner square in a fixed order wherever vanilla's rule left a chunk unpopulated. Two chunks in
        // from the edge, so decoration that spills two chunks (RWG's big trees) still lands in generated chunks.
        int inner = innerRadius();
        int side = 2 * inner + 1;
        while (populateCursor < side * side && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            int x = centerX - inner + populateCursor % side;
            int z = centerZ - inner + populateCursor / side;
            populateCursor++;
            Chunk chunk = server.provideChunk(x, z);
            if (!chunk.isTerrainPopulated) {
                server.populate(server, x, z);
            }
        }
        return populateCursor >= side * side;
    }

    /**
     * Returns every chunk of a square in spiral order from its center: the center, then each ring outward.
     *
     * @param centerX center chunk X
     * @param centerZ center chunk Z
     * @param radius  half the side, in chunks
     * @return {x, z} pairs
     */
    static List<long[]> spiral(int centerX, int centerZ, int radius) {
        List<long[]> out = new ArrayList<>();
        out.add(new long[] { centerX, centerZ });
        for (int ring = 1; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                out.add(new long[] { centerX + dx, centerZ - ring });
            }
            for (int dz = -ring + 1; dz <= ring; dz++) {
                out.add(new long[] { centerX + ring, centerZ + dz });
            }
            for (int dx = ring - 1; dx >= -ring; dx--) {
                out.add(new long[] { centerX + dx, centerZ + ring });
            }
            for (int dz = ring - 1; dz >= -ring + 1; dz--) {
                out.add(new long[] { centerX - ring, centerZ + dz });
            }
        }
        return out;
    }
}
