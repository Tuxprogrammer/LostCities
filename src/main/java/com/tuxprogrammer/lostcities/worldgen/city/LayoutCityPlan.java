package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: TERR-25, TERR-31

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkContent;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * The real plan source (decision log D71): the overlay's {@link CityPlan} over M03's {@link LayoutPlanner} and M02's
 * assets. For every chunk it answers what the terrain and population phases need: the chunk's kind and city ground,
 * its building's floors and cellars, its ruin fraction and its city style's explosion chance and terrain blocks, the
 * writer of its building, street, park or bridge ({@link CityChunkWriter}), its footprint, its village lane, the
 * openings in its street borders, and its population jobs, listed by a dry run of the same writer.
 *
 * <p>
 * Every value is a pure function of the world seed, the chunk, the pinned settings, the assets, the height oracle and
 * the structure oracle (D9), so neighbors agree and existing worlds stay valid. Avoids TERR-25: the plan is built and
 * validated before the terrain phase writes anything; avoids TERR-31: the layout fits every building above Y 2.
 *
 * <p>
 * Not thread-safe; wrap in {@code CachedCityPlan}. Side: both (plain Java).
 */
public final class LayoutCityPlan implements CityPlan {

    /** Depth of a street's footprint below its ground (the border fallback depth, as M04's streets). */
    static final int STREET_FOOTPRINT_DEPTH = 3;
    /** Depth of a lane column's footprint below the lower of the city ground and the natural height. */
    static final int LANE_FOOTPRINT_DEPTH = 4;
    /** Reach of the structure oracle around a core chunk when its lanes are drawn, in chunks. */
    static final int CORE_LANE_REACH = 1;

    /**
     * Entries of each layout planner cache (D57) in a live world: smaller than the planner's default so the world's
     * caches stay within budget B5 (16 MB); the overlay's plan cache shields the planner from most repeats.
     */
    public static final int LAYOUT_CACHE_SIZE = 2048;
    /** Layout planner caches of that size (city factor, level, zone, sewing, characteristics, plan, derived). */
    static final int LAYOUT_CACHES = 7;
    /** Estimated bytes of one layout cache entry, its key and its value. */
    static final int LAYOUT_ENTRY_BYTES = 320;
    /** Estimated bytes of one compiled palette: about 100 characters, a tenth of them 128-slot random lists. */
    static final int PALETTE_BYTES = 16 * 1024;
    /** Estimated bytes of one style blocks value, with its two 128-slot tables. */
    static final int STYLE_BYTES = 3 * 1024;

    private final CityWriteContext context;
    private final StructureOracle structures;
    private final TerrainView terrain;
    private final LayoutPlanner planner;
    private final TerrainSettings settings;

    /**
     * Creates the plan source of one world.
     *
     * @param context    the world's city writing context
     * @param structures the structure oracle the planner was built with
     * @param terrain    the terrain view the planner was built with
     */
    public LayoutCityPlan(CityWriteContext context, StructureOracle structures, TerrainView terrain) {
        this.context = context;
        this.structures = structures;
        this.terrain = terrain;
        this.planner = context.planner();
        this.settings = context.settings();
    }

    /** Returns the world's city writing context. */
    public CityWriteContext context() {
        return context;
    }

    /** Returns the structure oracle the layout builds around, for scenarios. */
    public StructureOracle structures() {
        return structures;
    }

    /** Returns the layout planner, for the finder and scenarios. */
    public LayoutPlanner planner() {
        return planner;
    }

    /**
     * Returns the most memory the layout's and the palettes' caches can hold, estimated, in bytes, for budget B5: the
     * caches are bounded, so this is a ceiling, not a measurement.
     *
     * @return bytes
     */
    public long estimatedCacheBytes() {
        long layout = (long) LAYOUT_CACHE_SIZE * LAYOUT_CACHES * LAYOUT_ENTRY_BYTES;
        long palettes = (long) (CityPalettes.CHUNK_CAPACITY + CityPalettes.PART_CAPACITY) * PALETTE_BYTES;
        long styles = (long) CityPalettes.STYLE_CAPACITY * STYLE_BYTES;
        return layout + palettes + styles;
    }

    @Override
    public com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan chunk(int chunkX, int chunkZ) {
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        int natural = terrain.height(chunkX, chunkZ);
        boolean ocean = terrain.isOcean(chunkX, chunkZ);
        if (plan.zone()
            .zone() == Zone.CORE) {
            return core(chunkX, chunkZ, plan, natural, ocean);
        }
        if (!plan.isCity()) {
            return outside(chunkX, chunkZ, plan, natural, ocean);
        }
        return city(chunkX, chunkZ, plan, natural, ocean);
    }

    private com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan core(int chunkX, int chunkZ, ChunkPlan plan,
        int natural, boolean ocean) {
        List<Landmark> landmarks = structures.landmarksTouching(
            chunkX - CORE_LANE_REACH,
            chunkZ - CORE_LANE_REACH,
            chunkX + CORE_LANE_REACH,
            chunkZ + CORE_LANE_REACH);
        boolean[] lane = LaneShapes
            .forCore(chunkX, chunkZ, sorted(landmarks), settings.landmarkMargin(), (gate, gx, gz) -> {
                LaneCell cell = planner.plan(gx, gz)
                    .lane();
                return cell.gate() != null && cell.gate()
                    .equals(gate);
            });
        return com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.builder(chunkX, chunkZ, ChunkKind.LANDMARK_CORE)
            .cityLevel(plan.cityLevel())
            .groundY(natural)
            .naturalHeight(natural)
            .ocean(ocean)
            .lane(LaneShapes.path(lane, material(chunkX, chunkZ)))
            .label(
                "landmark core " + plan.zone()
                    .landmarkId())
            .build();
    }

    private com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan outside(int chunkX, int chunkZ, ChunkPlan plan,
        int natural, boolean ocean) {
        int level = plan.cityLevel();
        com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Builder builder = com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan
            .builder(chunkX, chunkZ, ChunkKind.NONE)
            .cityLevel(level)
            .groundY(settings.cityGround(level))
            .naturalHeight(natural)
            .ocean(ocean);
        int[] footprint = null;
        boolean bridge = planner.bridgePart(chunkX, chunkZ, Orientation.X) != null
            || planner.bridgePart(chunkX, chunkZ, Orientation.Z) != null;
        if (bridge) {
            builder.content(bridgeContent(chunkX, chunkZ))
                .jobSource(() -> dryRun(chunkX, chunkZ, false))
                .label("bridge");
            footprint = new int[256];
            Arrays.fill(footprint, settings.groundLevel() - 1);
        }
        boolean[] lane = LaneShapes.forCell(plan.lane(), chunkX, chunkZ);
        if (lane != null) {
            builder.lane(LaneShapes.path(lane, material(chunkX, chunkZ)));
            if (footprint == null) {
                footprint = new int[256];
                Arrays.fill(footprint, com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.NO_FOOTPRINT);
            }
            int bottom = natural - LANE_FOOTPRINT_DEPTH;
            for (int i = 0; i < 256; i++) {
                if (lane[i]) {
                    footprint[i] = Math.min(footprint[i], bottom);
                }
            }
        }
        return builder.footprint(footprint)
            .build();
    }

    private com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan city(int chunkX, int chunkZ, ChunkPlan plan,
        int natural, boolean ocean) {
        int ground = plan.cityGroundLevel();
        CityStyle cityStyle = context.assets()
            .cityStyle(CityPalettes.id(plan.cityStyle()));
        Float explosion = cityStyle == null ? null : cityStyle.number(CityStyle.Setting.EXPLOSION_CHANCE);
        com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Builder builder = com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan
            .builder(chunkX, chunkZ, ChunkKind.CITY)
            .cityLevel(plan.cityLevel())
            .groundY(ground)
            .naturalHeight(natural)
            .ocean(ocean)
            .style(context.styleBlocks(plan))
            .explosionChance(explosion == null ? Float.NaN : explosion)
            .content(cityContent(chunkX, chunkZ))
            .jobSource(() -> dryRun(chunkX, chunkZ, true))
            .label(label(plan));
        if (plan.hasBuilding()) {
            builder.building(plan.floors(), plan.cellars());
            if (plan.ruinHeight() >= 0) {
                builder.ruinHeight(plan.ruinHeight());
            }
        }
        int[] footprint = new int[256];
        if (plan.hasBuilding()) {
            Arrays.fill(footprint, plan.buildingBottomHeight() - 1);
        } else if (isPark(plan, chunkX, chunkZ)) {
            // Parks, open lots and edge-ring parks keep RWG's decoration (0006 §1, pipeline-on-rwg.md).
            Arrays.fill(footprint, com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.NO_FOOTPRINT);
        } else {
            Arrays.fill(footprint, ground - STREET_FOOTPRINT_DEPTH);
        }
        LaneCell cell = plan.lane();
        if (cell.role() == LaneRole.GATE) {
            boolean[] lane = LaneShapes.forCell(cell, chunkX, chunkZ);
            if (lane != null) {
                builder.lane(LaneShapes.path(lane, material(chunkX, chunkZ)));
                int bottom = Math.min(ground, natural) - LANE_FOOTPRINT_DEPTH;
                for (int i = 0; i < 256; i++) {
                    if (lane[i]) {
                        footprint[i] = Math.min(footprint[i], bottom);
                    }
                }
            }
        }
        if (!plan.hasBuilding()) {
            builder.edgeSource(() -> edges(chunkX, chunkZ));
        }

        return builder.footprint(footprint)
            .build();
    }

    /** Returns whether a street chunk is a park or open lot, whose columns stay outside the footprint. */
    private boolean isPark(ChunkPlan plan, int chunkX, int chunkZ) {
        // An elevated park is a park too (the planner raises only PARK chunks), so no neighbor need be asked.
        return plan.streetType() == StreetType.PARK;
    }

    private ChunkContent cityContent(int chunkX, int chunkZ) {
        return (buffer,
            contentContext) -> new CityChunkWriter(
                context,
                chunkX,
                chunkZ,
                new BufferTarget(buffer, contentContext),
                contentContext.random(),
                new PaletteSequence(context.worldSeed(), chunkX, chunkZ)).writeCity();
    }

    private ChunkContent bridgeContent(int chunkX, int chunkZ) {
        return (buffer,
            contentContext) -> new CityChunkWriter(
                context,
                chunkX,
                chunkZ,
                new BufferTarget(buffer, contentContext),
                contentContext.random(),
                new PaletteSequence(context.worldSeed(), chunkX, chunkZ)).writeBridge();
    }

    /**
     * Lists a chunk's population jobs by running its writer against a {@link JobTarget}, with the same content random
     * and palette sequence the terrain phase uses.
     */
    private List<PopulationJob> dryRun(int chunkX, int chunkZ, boolean city) {
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        BiomeDescriptor biome = context.biomes()
            .biomeOfChunk(chunkX, chunkZ);
        JobTarget target = new JobTarget(
            new JobTarget.Facts(
                context.worldSeed(),
                chunkX,
                chunkZ,
                settings.groundLevel(),
                plan.cityGroundLevel(),
                plan.hasBuilding() ? plan.cellars() : 0,
                plan.hasBuilding() ? plan.floors() : 0,
                biome,
                plan.noLoot(),
                settings.generateLoot(),
                settings.chestWithoutLootChance()));
        CityChunkWriter writer = new CityChunkWriter(
            context,
            chunkX,
            chunkZ,
            target,
            new Random(TerrainPhase.contentSeed(context.worldSeed(), chunkX, chunkZ)),
            new PaletteSequence(context.worldSeed(), chunkX, chunkZ));
        if (city) {
            writer.writeCity();
        } else {
            writer.writeBridge();
        }
        return target.jobs();
    }

    /** The border openings and door approaches of a street chunk, asked only by its own terrain phase. */
    private com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Edges edges(int chunkX, int chunkZ) {
        com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Side[] sides = com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Side
            .values();
        int[] openings = new int[sides.length];
        for (com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Side side : sides) {
            openings[side.ordinal()] = borderOpenings(chunkX, chunkZ, side);
        }
        return new com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Edges(
            openings,
            CityChunkWriter.doorApproaches(planner, chunkX, chunkZ));
    }

    /**
     * Upstream's {@code borderNeedsConnectionToAdjacentChunk}, per side: the columns of a street border that open for
     * a slope or a stair coming up from the neighbor (between its part's {@code z1} and {@code z2}, turned to face this
     * chunk), and the whole side where the neighbor carries a bridge along that axis.
     */
    int borderOpenings(int chunkX, int chunkZ, com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Side side) {
        Direction direction = direction(side);
        int nx = chunkX + direction.stepX();
        int nz = chunkZ + direction.stepZ();
        if (planner.bridgePart(nx, nz, direction.orientation()) != null) {
            return 0xFFFF;
        }
        int mask = 0;
        if (planner.streetSlope(nx, nz) == direction.opposite()) {
            mask |= openingOf(slopePart(nx, nz), direction);
        }
        if (planner.stairDirection(nx, nz) == direction.opposite()) {
            mask |= openingOf(
                part(
                    planner.plan(nx, nz)
                        .stairPart()),
                direction);
        }
        return mask;
    }

    private Part slopePart(int chunkX, int chunkZ) {
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        CityStyle style = context.assets()
            .cityStyle(CityPalettes.id(plan.cityStyle()));
        if (style == null) {
            return null;
        }
        CityStyle.StreetSet set = plan.plannedRoad() == PlannedRoadType.PRIMARY ? CityStyle.StreetSet.LARGE_PARTS
            : plan.plannedRoad() == PlannedRoadType.TERTIARY ? CityStyle.StreetSet.TERTIARY_PARTS
                : CityStyle.StreetSet.PARTS;
        List<Part> parts = style.streetParts(set, CityStyle.StreetSlot.STAIR);
        return parts.isEmpty() ? null : parts.get(0);
    }

    /** The border bits a neighbor's part opens: its {@code z1}..{@code z2} run on its far side, turned toward us. */
    static int openingOf(Part part, Direction side) {
        if (part == null) {
            return 0;
        }
        Integer z1 = part.metaInteger(Part.MetaKey.Z1);
        Integer z2 = part.metaInteger(Part.MetaKey.Z2);
        if (z1 == null || z2 == null) {
            return 0;
        }
        com.tuxprogrammer.lostcities.asset.transform.Transform transform = CityChunkWriter.rotation(side.opposite());
        int x1 = transform.rotateX(15, z1);
        int zz1 = transform.rotateZ(15, z1);
        int x2 = transform.rotateX(15, z2);
        int zz2 = transform.rotateZ(15, z2);
        boolean alongZ = side.orientation() == Orientation.X;
        int from = alongZ ? Math.min(zz1, zz2) : Math.min(x1, x2);
        int to = alongZ ? Math.max(zz1, zz2) : Math.max(x1, x2);
        int mask = 0;
        for (int along = Math.max(0, from); along <= Math.min(15, to); along++) {
            mask |= 1 << along;
        }
        return mask;
    }

    private static Direction direction(com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.Side side) {
        switch (side) {
            case WEST:
                return Direction.XMIN;
            case EAST:
                return Direction.XMAX;
            case NORTH:
                return Direction.ZMIN;
            default:
                return Direction.ZMAX;
        }
    }

    private Part part(String name) {
        return name == null ? null
            : context.assets()
                .part(CityPalettes.id(name));
    }

    /**
     * Returns the village material of a lane in a chunk, from the chunk's biome as VillageNames picks its village type
     * (desert and mesa villages swap their path blocks, StructureVillageVN.java#L945-L1003 at 4.5.21-GTNH).
     */
    LanePath.Material material(int chunkX, int chunkZ) {
        BiomeDescriptor biome = context.biomes()
            .biomeOfChunk(chunkX, chunkZ);
        if (biome.types()
            .contains("MESA")) {
            return LanePath.Material.MESA;
        }
        if (biome.types()
            .contains("SANDY")
            && biome.types()
                .contains("HOT")) {
            return LanePath.Material.SAND;
        }
        return LanePath.Material.PLAIN;
    }

    private static String label(ChunkPlan plan) {
        if (plan.hasBuilding()) {
            String multi = plan.multiBuilding();
            return "building " + plan.building() + (multi == null ? "" : " in " + multi);
        }
        if (plan.isPlannedRoad()) {
            return plan.plannedRoad()
                .name()
                .toLowerCase(java.util.Locale.ROOT) + " street";
        }
        switch (plan.streetType()) {
            case PARK:
                return plan.parkPart() == null ? "open lot" : "park " + plan.parkPart();
            case FULL:
                return "plaza";
            default:
                return "street";
        }
    }

    private static List<Landmark> sorted(List<Landmark> landmarks) {
        if (landmarks.size() < 2) {
            return landmarks;
        }
        Landmark[] array = landmarks.toArray(new Landmark[0]);
        Arrays.sort(
            array,
            (a, b) -> a.id()
                .compareTo(b.id()));
        return Collections.unmodifiableList(Arrays.asList(array));
    }

    /** Returns the building-bottom bound the layout keeps, for tests: Y 2 (TERR-31). */
    static int buildingBottomMin() {
        return WorldLimits.BUILDING_BOTTOM_MIN;
    }
}
