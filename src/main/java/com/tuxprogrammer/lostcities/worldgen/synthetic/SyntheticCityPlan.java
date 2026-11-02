package com.tuxprogrammer.lostcities.worldgen.synthetic;

import java.util.Arrays;
import java.util.Random;

import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * The synthetic test city: a fixed, flat plan of streets and boxy buildings that drives the overlay in M04 before
 * M03's layout and M02's assets exist (the S2 prototype's approach). It is a test fixture, selected only by a world
 * pinned with the {@code synthetic} plan source, which needs the test hooks (docs/testing/README.md).
 *
 * <p>
 * Layout, in chunks relative to each city center at chunk ({@code 64 × i}, {@code 64 × j}):
 * <ul>
 * <li>a square of radius {@link #RADIUS}: a chunk inside it is a city chunk when its oracle height is within
 * {@code cityMinHeight} … {@code cityMaxHeight}, at the city level its height gives (upstream's thresholds);</li>
 * <li>streets on every third row and column, buildings of 1 to 6 floors and 0 to 2 cellars between them, and a park
 * where a seeded draw says so;</li>
 * <li>two fake landmarks: a 2 × 2-chunk village core inside the city at (4..5, −5..−4) with a lane from the street at
 * x = 3 across the soft-edge strip to the core's center, and a 2 × 2 core straddling the city's west edge at
 * (−7..−6, −1..0), so the blend meets a core outside the city (rule E1). City chunks among a core's eight neighbors
 * form its edge ring: no buildings, parks unless a street passes.</li>
 * </ul>
 *
 * <p>
 * Pure: a function of the world seed, the settings and the height oracle. Side: both (plain Java).
 */
public final class SyntheticCityPlan implements CityPlan {

    /** Chunks between city centers along X and Z. */
    public static final int PERIOD = 64;
    /** Half-width of a city's square, in chunks: cities are 13 × 13 chunks. */
    public static final int RADIUS = 6;
    /** Every third row and column of a city is a street. */
    static final int STREET_EVERY = 3;
    /** One building lot in this many is a park. */
    static final int PARK_ONE_IN = 7;
    /** Most floors of a synthetic building. */
    static final int MAX_FLOORS = 6;
    /** Most cellars of a synthetic building. */
    static final int MAX_CELLARS = 2;

    /** The village core inside the city: chunk offsets {minX, maxX, minZ, maxZ} from the center. */
    static final int[] VILLAGE_CORE = { 4, 5, -5, -4 };
    /** The core straddling the city's west edge. */
    static final int[] EDGE_CORE = { -7, -6, -1, 0 };

    /** The lane runs along this chunk Z boundary (offset from the center): the middle of the village core's side. */
    static final int LANE_Z_BOUNDARY = -4;
    /** Half-width of the lane, in columns, around its center line: 3 columns wide. */
    static final int LANE_HALF_WIDTH = 1;

    private static final long LOT_X = 341873128712L;
    private static final long LOT_Z = 132897987541L;

    private final long worldSeed;
    private final TerrainSettings settings;
    private final ChunkHeights heights;
    private final StyleBlocks style;

    /**
     * Creates the plan.
     *
     * @param worldSeed the world seed
     * @param settings  the pinned settings
     * @param heights   the oracle's chunk heights
     */
    public SyntheticCityPlan(long worldSeed, TerrainSettings settings, ChunkHeights heights) {
        this.worldSeed = worldSeed;
        this.settings = settings;
        this.heights = heights;
        this.style = StyleBlocks.builder()
            .damaged(SyntheticContent.WALL, SyntheticContent.IRON_BARS)
            .damaged(SyntheticContent.WINDOW, SyntheticContent.IRON_BARS)
            .build();
    }

    /** Returns the chunk X of the city center nearest a chunk X. */
    public static int centerOf(int chunk) {
        return Math.floorDiv(chunk + PERIOD / 2, PERIOD) * PERIOD;
    }

    @Override
    public ChunkPlan chunk(int chunkX, int chunkZ) {
        int dx = chunkX - centerOf(chunkX);
        int dz = chunkZ - centerOf(chunkZ);
        int natural = heights.height(chunkX, chunkZ);
        int level = settings.levelForHeight(natural);
        boolean ocean = natural < settings.waterLevel();
        if (inRect(VILLAGE_CORE, dx, dz) || inRect(EDGE_CORE, dx, dz)) {
            return ChunkPlan.builder(chunkX, chunkZ, ChunkKind.LANDMARK_CORE)
                .cityLevel(level)
                .groundY(natural)
                .naturalHeight(natural)
                .ocean(ocean)
                .lane(inRect(VILLAGE_CORE, dx, dz) ? coreLane(dx, dz) : null)
                .build();
        }
        boolean inCity = Math.abs(dx) <= RADIUS && Math.abs(dz) <= RADIUS
            && natural >= settings.cityMinHeight()
            && natural <= settings.cityMaxHeight();
        if (!inCity) {
            return ChunkPlan.none(chunkX, chunkZ, level, settings.cityGround(level), natural, ocean);
        }
        int ground = settings.cityGround(level);
        boolean street = Math.floorMod(dx, STREET_EVERY) == 0 || Math.floorMod(dz, STREET_EVERY) == 0;
        boolean edgeRing = nearCore(VILLAGE_CORE, dx, dz) || nearCore(EDGE_CORE, dx, dz);
        Random lot = new Random(SeedMix.chunk(worldSeed, chunkX, chunkZ, LOT_X, LOT_Z));
        boolean park = !street && (edgeRing || lot.nextInt(PARK_ONE_IN) == 0);
        ChunkPlan.Builder builder = ChunkPlan.builder(chunkX, chunkZ, ChunkKind.CITY)
            .cityLevel(level)
            .groundY(ground)
            .naturalHeight(natural)
            .ocean(ocean)
            .style(style);
        int[] footprint = new int[256];
        if (street) {
            builder.content(SyntheticContent.street());
            Arrays.fill(footprint, ground - 3);
        } else if (park) {
            builder.content(SyntheticContent.park());
            Arrays.fill(footprint, ChunkPlan.NO_FOOTPRINT);
        } else {
            int floors = 1 + lot.nextInt(MAX_FLOORS);
            int cellars = lot.nextInt(MAX_CELLARS + 1);
            while (cellars > 0 && ground - WorldLimits.FLOOR_HEIGHT * cellars < WorldLimits.BUILDING_BOTTOM_MIN) {
                cellars--;
            }
            while (floors > 1 && ground + WorldLimits.FLOOR_HEIGHT * floors >= WorldLimits.BUILDING_TOP_CAP) {
                floors--;
            }
            builder.building(floors, cellars)
                .content(SyntheticContent.building());
            Arrays.fill(footprint, ground - WorldLimits.FLOOR_HEIGHT * cellars - 1);
            builder.job(PopulationJob.of(PopulationJob.Kind.CHEST, 2, ground + 1, 2, ""));
            for (int floor = 0; floor < floors; floor++) {
                int y = ground + WorldLimits.FLOOR_HEIGHT * floor + SyntheticContent.TORCH_HEIGHT;
                builder.job(PopulationJob.of(PopulationJob.Kind.TORCH, 1, y, 7, ""));
            }
        }
        LanePath lane = cityLane(dx, dz);
        if (lane != null) {
            builder.lane(lane);
            for (int i = 0; i < 256; i++) {
                if (lane.contains(i & 15, i >> 4)) {
                    footprint[i] = Math.min(ground, natural) - 4;
                }
            }
        }
        if (edgeRing && !street) {
            // Edge-ring parks keep RWG's decoration (0006 §1); only a lane would be in the footprint.
            for (int i = 0; i < 256; i++) {
                if (lane == null || !lane.contains(i & 15, i >> 4)) {
                    footprint[i] = ChunkPlan.NO_FOOTPRINT;
                }
            }
        }
        return builder.footprint(footprint)
            .build();
    }

    /** The lane in the street chunk west of the village core: its strip columns, toward the core's edge. */
    private static LanePath cityLane(int dx, int dz) {
        if (dx != VILLAGE_CORE[0] - 1) {
            return null;
        }
        boolean[] columns = new boolean[256];
        boolean any = false;
        for (int x = 8; x < 16; x++) {
            any |= laneRow(columns, x, dz);
        }
        return any ? LanePath.of(columns, LanePath.Material.PLAIN) : null;
    }

    /** The lane across the village core, from its west edge to its center. */
    private static LanePath coreLane(int dx, int dz) {
        if (dx != VILLAGE_CORE[0]) {
            return null;
        }
        boolean[] columns = new boolean[256];
        boolean any = false;
        for (int x = 0; x < 16; x++) {
            any |= laneRow(columns, x, dz);
        }
        return any ? LanePath.of(columns, LanePath.Material.PLAIN) : null;
    }

    private static boolean laneRow(boolean[] columns, int x, int dz) {
        boolean any = false;
        for (int offset = -LANE_HALF_WIDTH; offset <= LANE_HALF_WIDTH; offset++) {
            // The lane's center line is the first column of chunk row LANE_Z_BOUNDARY.
            int relativeZ = LANE_Z_BOUNDARY * 16 + offset;
            int chunkDz = Math.floorDiv(relativeZ, 16);
            if (chunkDz == dz) {
                columns[(Math.floorMod(relativeZ, 16) << 4) | x] = true;
                any = true;
            }
        }
        return any;
    }

    private static boolean inRect(int[] rect, int dx, int dz) {
        return dx >= rect[0] && dx <= rect[1] && dz >= rect[2] && dz <= rect[3];
    }

    private static boolean nearCore(int[] rect, int dx, int dz) {
        return dx >= rect[0] - 1 && dx <= rect[1] + 1 && dz >= rect[2] - 1 && dz <= rect[3] + 1 && !inRect(rect, dx, dz);
    }
}
