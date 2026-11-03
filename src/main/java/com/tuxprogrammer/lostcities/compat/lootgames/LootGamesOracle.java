package com.tuxprogrammer.lostcities.compat.lootgames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The structure oracle for LootGames puzzle dungeons (0006 §1 and §2): every chunk {@link RhombGrid} says LootGames
 * tries is a landmark, so the city leaves the buried room and the mouth of its entrance natural ground.
 *
 * <p>
 * LootGames digs a 21 × 21 room centered on block {@code (16 cx + 8, 16 cz + 8)}, from 2 to 10 blocks under the
 * highest level where all its columns are solid ground, then a 1-wide tunnel from the room's +Z wall rising one block
 * per block toward +Z until it opens to the sky, at most 15 blocks past the wall
 * ({@code StructureGenerator.doGenDungeon}). The landmark is {@code lootgames@<cx>,<cz>} of kind
 * {@link LandmarkKind#LOOTGAMES}, certainty {@link Certainty#EXACT}, footprint the room
 * {@code [16 cx − 2, 16 cx + 18] × [16 cz − 2, 16 cz + 18]} and the tunnel {@code x = 16 cx + 8},
 * {@code z = 16 cz + 19 … 16 cz + 33}, ground estimate the median oracle height over the room, computed when first
 * read (only the finder and the scenarios read it). A site where LootGames
 * finds no level or meets a tile entity in the room stays natural ground (0006 §5); a city chest or spawner could
 * never stop it, since the core has none.
 *
 * <p>
 * Pure: the world seed, the rhomb size and the height oracle only. The site test is a {@code Random} reseed and two
 * draws, so it is not cached; the ground estimate is, per site, in a bounded cache this object owns.
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/eu/usrv/legacylootgames/StructureGenerator.java#L18-L21,
 * #L107-L240.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § LootGames. Side: server, and plain Java in tests.
 */
public final class LootGamesOracle implements StructureOracle {

    /** Sites whose ground estimate is cached. */
    public static final int CACHE_SIZE = 256;
    /** The room's half-width, blocks ({@code PUZZLEROOM_CENTER_TO_BORDER}). */
    public static final int ROOM_HALF = 10;
    /** The room center's offset from the chunk's corner, blocks. */
    static final int CENTER_OFFSET = 8;
    /** How far the tunnel runs past the room's +Z wall, at most, blocks. */
    public static final int TUNNEL_LENGTH = 15;
    /** Height used without an oracle. */
    static final int FALLBACK_Y = 64;

    private final RhombGrid grid;
    private final TerrainHeightOracle heights;
    private final GroundCache grounds;

    /**
     * Creates the oracle of a world.
     *
     * @param worldSeed the world seed
     * @param settings  the dimension's settings, enabled
     * @param heights   the height oracle, or {@code null} for Y {@value #FALLBACK_Y}
     * @param cacheSize the most sites whose ground is cached
     */
    public LootGamesOracle(long worldSeed, LootGamesSettings settings, TerrainHeightOracle heights, int cacheSize) {
        if (!settings.enabled()) {
            throw new IllegalArgumentException("LootGames dungeons are off: " + settings);
        }
        this.grid = new RhombGrid(worldSeed, settings.rhombSize());
        this.heights = heights;
        this.grounds = new GroundCache(cacheSize);
    }

    /** Returns the grid this oracle reads. */
    public RhombGrid grid() {
        return grid;
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        BlockRect query = new BlockRect(minChunkX << 4, minChunkZ << 4, (maxChunkX << 4) + 15, (maxChunkZ << 4) + 15);
        List<Landmark> out = new ArrayList<>();
        // A room reaches one chunk west, east and north of its chunk; its tunnel two chunks south.
        for (int chunkX = minChunkX - 1; chunkX <= maxChunkX + 1; chunkX++) {
            for (int chunkZ = minChunkZ - 2; chunkZ <= maxChunkZ + 1; chunkZ++) {
                if (!grid.spawnsIn(chunkX, chunkZ)) {
                    continue;
                }
                Landmark landmark = landmark(chunkX, chunkZ);
                if (touches(landmark, query)) {
                    out.add(landmark);
                }
            }
        }
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }

    /**
     * Returns the dungeon LootGames tries in a chunk, or {@code null}.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the landmark, or {@code null}
     */
    public synchronized Landmark dungeonAt(int chunkX, int chunkZ) {
        return grid.spawnsIn(chunkX, chunkZ) ? landmark(chunkX, chunkZ) : null;
    }

    private Landmark landmark(int chunkX, int chunkZ) {
        BlockRect room = room(chunkX, chunkZ);
        int centerX = (chunkX << 4) + CENTER_OFFSET;
        int centerZ = (chunkZ << 4) + CENTER_OFFSET;
        BlockRect tunnel = new BlockRect(
            centerX,
            centerZ + ROOM_HALF + 1,
            centerX,
            centerZ + ROOM_HALF + TUNNEL_LENGTH);
        return Landmark.builder(id(chunkX, chunkZ), LandmarkKind.LOOTGAMES)
            .footprint(room)
            .footprint(tunnel)
            .certainty(Certainty.EXACT)
            // Read only by the finder and the scenarios: computed on first read (D263).
            .groundEstimate(() -> ground(chunkX, chunkZ, room))
            .build();
    }

    /**
     * Returns the room of a site, block rectangle.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the 21 × 21 room
     */
    public static BlockRect room(int chunkX, int chunkZ) {
        int centerX = (chunkX << 4) + CENTER_OFFSET;
        int centerZ = (chunkZ << 4) + CENTER_OFFSET;
        return new BlockRect(centerX - ROOM_HALF, centerZ - ROOM_HALF, centerX + ROOM_HALF, centerZ + ROOM_HALF);
    }

    /** The median oracle height at the room's center and corners. */
    private synchronized int ground(int chunkX, int chunkZ, BlockRect room) {
        if (heights == null) {
            return FALLBACK_Y;
        }
        long key = GroundCache.key(chunkX, chunkZ);
        Integer cached = grounds.get(key);
        if (cached != null) {
            return cached;
        }
        int[] samples = { heights.height(room.minX() + ROOM_HALF, room.minZ() + ROOM_HALF),
            heights.height(room.minX(), room.minZ()), heights.height(room.maxX(), room.minZ()),
            heights.height(room.minX(), room.maxZ()), heights.height(room.maxX(), room.maxZ()) };
        Arrays.sort(samples);
        return grounds.put(key, samples[samples.length / 2]);
    }

    /**
     * Returns a LootGames landmark's identity.
     *
     * @param chunkX the site's chunk X
     * @param chunkZ the site's chunk Z
     * @return {@code lootgames@<chunkX>,<chunkZ>}
     */
    public static String id(int chunkX, int chunkZ) {
        return "lootgames@" + chunkX + "," + chunkZ;
    }

    private static boolean touches(Landmark landmark, BlockRect query) {
        for (BlockRect rect : landmark.footprint()) {
            if (rect.minX() <= query.maxX() && rect.maxX() >= query.minX()
                && rect.minZ() <= query.maxZ()
                && rect.maxZ() >= query.minZ()) {
                return true;
            }
        }
        return false;
    }
}
