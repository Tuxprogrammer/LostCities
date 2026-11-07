package com.tuxprogrammer.lostcities.worldgen.damage;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/dimensions/world/terraingen/LostCitiesTerrainGenerator.java
// Avoids: TERR-28

import java.util.Arrays;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * The floating-fragment pass upstream 1.12 ran after explosion damage ({@code fixAfterExplosionNew}) and 1.20 lost
 * (TERR-28): connected blobs of solid blocks in the damaged band are found, and each poorly supported blob that
 * damage can have reached is removed or dropped onto the next solid block, so explosions leave no floating debris
 * (0004 §5).
 *
 * <p>
 * A blob's support is its count of "connections": a block next to a chunk side counts one when it is no higher than
 * that neighbor's planned building height plus 3 (as on 1.12, so the pass reads only its own chunk and the plan), and
 * a block reaching below the damaged band counts five. A blob may reach any height above the band, as on 1.12. A blob
 * is poorly supported with fewer than five connections, or
 * when connections are under {@link #DESTROY_LONE_BLOCKS_FACTOR} of its size. Such blobs, lowest first, are removed
 * when a draw is below {@link #DESTROY_OR_MOVE_CHANCE}, when they are smaller than {@link #DESTROY_SMALL_SECTIONS_SIZE}
 * blocks or have fewer than five connections, and otherwise fall.
 *
 * <p>
 * Spec: docs/new-spec/03-terrain/index.md ("Floating fragments after explosions", P03-11, D31), which keeps 1.12's
 * three numbers as constants. Changes from 1.12: only a blob with a block where damage can have reached
 * ({@link DamageArea#mayHaveDamaged}) is judged, so structure no explosion touched keeps its designed shape (AR-1: on
 * 1.12 every blob of the band was judged, and a street stair's slab steps, each its own blob by design, lost the one
 * step that fell inside a far explosion's band); falling blocks move bottom first in a fixed order instead of a hash
 * set's, so the result does not depend on hash layout; a fallen block lands on the first block that is neither air nor
 * liquid, without 1.12's check against the set's old positions, which bottom-first order makes empty already.
 *
 * <p>
 * A building is never moved (decision log D-fragment-1): in a chunk with a building, a blob with any block at or above
 * the building's lowest level is left as the writer and the damage pass made it, floors, walls and furniture alike.
 * Falling blocks land per column on the first block below, so a floor strip judged unsupported fell one level, or one
 * block onto whatever stood under it, and its furniture went with it, off the level its population job reads. The pass
 * still clears terrain and debris: blobs wholly under a building's lowest level, and every blob outside building
 * chunks.
 *
 * <p>
 * One instance per world; reuses its buffers. Not thread-safe. Side: both (plain Java).
 */
public final class FloatingFragments {

    /** 1.12's {@code destroyLoneBlocksFactor}: below this ratio of connections to blocks a blob is unsupported. */
    public static final float DESTROY_LONE_BLOCKS_FACTOR = 0.05f;
    /** 1.12's {@code destroyOrMoveChance}: the chance an unsupported blob is removed rather than dropped. */
    public static final float DESTROY_OR_MOVE_CHANCE = 0.4f;
    /** 1.12's {@code destroySmallSectionsSize}: unsupported blobs smaller than this are always removed. */
    public static final int DESTROY_SMALL_SECTIONS_SIZE = 50;
    /** Fewer connections than this make a blob unsupported, and removed. */
    static final int MIN_CONNECTIONS = 5;
    /** Connections a block reaching below the damaged band counts. */
    static final int BELOW_BAND_CONNECTIONS = 5;
    /** A side connection counts when the block is at most this far above the neighbor's planned height. */
    static final int SIDE_SUPPORT_SLACK = 3;
    /** Fallen blocks stop above this Y, as 1.12's do. */
    static final int FALL_FLOOR = 2;

    private static final int VOLUME = 16 * 16 * WorldLimits.BUILD_HEIGHT;
    /** Whether the city trace is on; the pass is plain Java, so it reads the property itself. */
    private static final boolean TRACE = Boolean.getBoolean("lostcitiesnh.cityTrace");
    /** Where trace lines go; {@code CityTraceLog.install} points it at the mod log. */
    private static volatile java.util.function.Consumer<String> traceSink = line -> {};

    private final TerrainContext context;
    /** Blob label per block, valid where {@link #stamp} equals {@link #epoch}. */
    private final int[] label = new int[VOLUME];
    private final int[] stamp = new int[VOLUME];
    private final int[] queue = new int[VOLUME];
    private int epoch;

    // Per blob, filled by scan().
    private int[] blobSize = new int[64];
    private int[] blobConnections = new int[64];
    private int[] blobLowest = new int[64];
    private boolean[] blobTouched = new boolean[64];
    private int[] blobHighest = new int[64];
    /** Per blob, its box: min X, max X, min Z, max Z, for the trace. */
    private int[] blobBox = new int[64 * 4];

    /**
     * Sends the pass's trace lines somewhere; they are written only with the city trace on.
     *
     * @param sink the line consumer
     */
    public static void traceTo(java.util.function.Consumer<String> sink) {
        traceSink = sink;
    }

    /**
     * Creates the pass of one world.
     *
     * @param context the world's terrain context
     */
    public FloatingFragments(TerrainContext context) {
        this.context = context;
    }

    /**
     * Removes or drops the poorly supported blobs of the damaged band.
     *
     * @param buffer the damaged chunk
     * @param area   the chunk's damage area
     * @param plan   the chunk's plan
     * @param sides  the plans toward −X, +X, −Z and +Z
     * @param random the damage random
     * @return the number of blocks removed or moved
     */
    public int apply(ChunkBuffer buffer, DamageArea area, ChunkPlan plan, ChunkPlan[] sides, Random random) {
        int start = area.lowestExplosionY();
        if (start < 0) {
            return 0;
        }
        int end = area.highestExplosionY();
        nextEpoch();
        BlockClassifier classes = context.classifier();
        int blobs = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = start; y < end; y++) {
                    int index = index(x, y, z);
                    if (stamp[index] == epoch || isEmpty(classes, buffer.get(x, y, z))) {
                        continue;
                    }
                    ensureCapacity(blobs + 1);
                    scan(buffer, classes, sides, area, x, y, z, start, blobs);
                    blobs++;
                }
            }
        }
        if (blobs == 0) {
            return 0;
        }
        // Unsupported blobs, lowest first; a stable sort keeps discovery order among equals, as 1.12's stream did.
        Integer[] order = new Integer[blobs];
        for (int i = 0; i < blobs; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(blobLowest[a], blobLowest[b]));
        boolean[] remove = new boolean[blobs];
        boolean[] fall = new boolean[blobs];
        boolean anyFall = false;
        int buildingBottom = plan.hasBuilding() ? plan.buildingBottom() : Integer.MAX_VALUE;
        for (Integer blob : order) {
            if (!blobTouched[blob]) {
                // AR-1: no explosion came near this blob, so how it is held up is the assets' design, not damage.
                continue;
            }
            int size = blobSize[blob];
            int connections = blobConnections[blob];
            boolean unsupported = connections < MIN_CONNECTIONS
                || ((float) connections / size) < DESTROY_LONE_BLOCKS_FACTOR;
            if (!unsupported) {
                continue;
            }
            if (blobHighest[blob] >= buildingBottom) {
                // D-fragment-1: part of a building; only the damage pass changes a building.
                trace(area, start, end, blob, "kept (building)");
                continue;
            }
            if (random.nextFloat() < DESTROY_OR_MOVE_CHANCE || size < DESTROY_SMALL_SECTIONS_SIZE
                || connections < MIN_CONNECTIONS) {
                remove[blob] = true;
                trace(area, start, end, blob, "removed");
            } else {
                fall[blob] = true;
                anyFall = true;
                trace(area, start, end, blob, "fell");
            }
        }
        int waterLevel = context.settings()
            .waterLevel();
        BlockRef liquid = plan.style()
            .liquid();
        int changed = 0;
        for (int index = 0; index < VOLUME; index++) {
            if (stamp[index] == epoch && remove[label[index]]) {
                int y = index >> 8;
                buffer.set(index & 15, y, (index >> 4) & 15, y < waterLevel ? liquid : BlockRef.AIR);
                changed++;
            }
        }
        if (anyFall) {
            // Bottom first (the index is y-major), so blocks below have already moved out of the way.
            for (int index = 0; index < VOLUME; index++) {
                if (stamp[index] != epoch || !fall[label[index]]) {
                    continue;
                }
                int x = index & 15;
                int z = (index >> 4) & 15;
                int y = index >> 8;
                BlockRef block = buffer.get(x, y, z);
                buffer.set(x, y, z, y < waterLevel ? liquid : BlockRef.AIR);
                int landing = y - 1;
                while (landing > FALL_FLOOR && isEmpty(classes, buffer.get(x, landing, z))) {
                    landing--;
                }
                buffer.set(x, landing + 1, z, block);
                changed++;
            }
        }
        return changed;
    }

    private void scan(ChunkBuffer buffer, BlockClassifier classes, ChunkPlan[] sides, DamageArea area, int startX,
        int startY, int startZ, int bandBottom, int blob) {
        int head = 0;
        int tail = 0;
        int size = 0;
        int connections = 0;
        boolean touched = false;
        int lowest = WorldLimits.BUILD_HEIGHT;
        int highest = -1;
        int minX = 15;
        int maxX = 0;
        int minZ = 15;
        int maxZ = 0;
        int first = index(startX, startY, startZ);
        stamp[first] = epoch;
        label[first] = blob;
        queue[tail++] = first;
        while (head < tail) {
            int index = queue[head++];
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = index >> 8;
            size++;
            lowest = Math.min(lowest, y);
            highest = Math.max(highest, y);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            touched = touched || area.mayHaveDamaged(x, y, z);
            for (int direction = 0; direction < 6; direction++) {
                int nx = x + (direction == 0 ? 1 : direction == 1 ? -1 : 0);
                int ny = y + (direction == 2 ? 1 : direction == 3 ? -1 : 0);
                int nz = z + (direction == 4 ? 1 : direction == 5 ? -1 : 0);
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15) {
                    ChunkPlan side = sides[nx < 0 ? 0 : nx > 15 ? 1 : nz < 0 ? 2 : 3];
                    if (ny <= side.maxHeight() + SIDE_SUPPORT_SLACK) {
                        connections++;
                    }
                    continue;
                }
                if (ny < bandBottom) {
                    connections += BELOW_BAND_CONNECTIONS;
                    continue;
                }
                if (ny > WorldLimits.MAX_Y) {
                    continue;
                }
                int next = index(nx, ny, nz);
                if (stamp[next] == epoch || isEmpty(classes, buffer.get(nx, ny, nz))) {
                    continue;
                }
                stamp[next] = epoch;
                label[next] = blob;
                queue[tail++] = next;
            }
        }
        blobSize[blob] = size;
        blobConnections[blob] = connections;
        blobLowest[blob] = lowest;
        blobTouched[blob] = touched;
        blobHighest[blob] = highest;
        blobBox[blob * 4] = minX;
        blobBox[blob * 4 + 1] = maxX;
        blobBox[blob * 4 + 2] = minZ;
        blobBox[blob * 4 + 3] = maxZ;
    }

    /** Trace (diagnostic, {@code -Dlostcitiesnh.cityTrace=true}): one line per unsupported blob and what it became. */
    private void trace(DamageArea area, int start, int end, int blob, String outcome) {
        if (TRACE) {
            traceSink.accept(
                String.format(
                    java.util.Locale.ROOT,
                    "fragments chunk=%d,%d band=%d..%d blob size=%d connections=%d y=%d..%d x=%d..%d z=%d..%d %s",
                    area.chunkX(),
                    area.chunkZ(),
                    start,
                    end,
                    blobSize[blob],
                    blobConnections[blob],
                    blobLowest[blob],
                    blobHighest[blob],
                    blobBox[blob * 4],
                    blobBox[blob * 4 + 1],
                    blobBox[blob * 4 + 2],
                    blobBox[blob * 4 + 3],
                    outcome));
        }
    }

    private static boolean isEmpty(BlockClassifier classes, BlockRef block) {
        return block.isAir() || classes.isLiquid(block);
    }

    private void ensureCapacity(int blobs) {
        if (blobs > blobSize.length) {
            int capacity = Math.max(blobs, blobSize.length * 2);
            blobSize = Arrays.copyOf(blobSize, capacity);
            blobConnections = Arrays.copyOf(blobConnections, capacity);
            blobLowest = Arrays.copyOf(blobLowest, capacity);
            blobTouched = Arrays.copyOf(blobTouched, capacity);
            blobHighest = Arrays.copyOf(blobHighest, capacity);
            blobBox = Arrays.copyOf(blobBox, capacity * 4);
        }
    }

    private void nextEpoch() {
        epoch++;
        if (epoch == Integer.MAX_VALUE) {
            Arrays.fill(stamp, 0);
            epoch = 1;
        }
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }
}
