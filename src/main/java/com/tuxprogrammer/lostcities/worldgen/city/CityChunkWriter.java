package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Doors.java
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Corridors.java
// Avoids: TERR-07, TERR-08, TERR-09, ASSET-32, CITY-36

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkLayout;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.DoorKind;
import com.tuxprogrammer.lostcities.layout.Doorway;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Writes one city chunk's own content from its layout: a building floor by floor with its doors, cellar fillers,
 * corridor openings and overlay parts, or a street, plaza or park with its park or fountain part, vegetation, a
 * park's greenery ({@link ParkGreenery}), the
 * fronts of neighboring buildings, the connectors of a primary road, its stair and the rail corridors under it
 * (docs/original-spec/03-terrain/building-and-street-writing.md, {@code generateBuilding} and {@code generateStreet};
 * {@code Doors} and {@code Corridors}). The terrain phase does the leveling, room for the building, street base and
 * borders, ruins, rubble and damage around it.
 *
 * <p>
 * The same code runs for the write and for the job dry run ({@link CityTarget}); steps that only read and rewrite
 * blocks (clearing above the roof, the drop of vegetation onto the ground) are skipped in the dry run. The part picks
 * draw from the content random in the same order in both runs.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids TERR-07: the street type is the plan's, never re-rolled at generation time;</li>
 * <li>avoids TERR-08: the leaves along buildings are drawn the same way on all four sides, with the 0.8 cap
 * everywhere and the outermost column used on every side;</li>
 * <li>avoids TERR-09: a neighbor's front part is written at this chunk's positions, with this chunk's jobs; only its
 * palette is the neighbor building's, so the front matches it;</li>
 * <li>avoids ASSET-32: an empty street-part list writes nothing, with one log line, instead of throwing;</li>
 * <li>avoids CITY-36: doorways come from {@code LayoutPlanner.doorway}, which tests both floors on every side;</li>
 * <li>doors are this pack's door blocks with 1.7.10 metadata (lower half: facing; upper half: 8 plus the hinge), and
 * each door half is noted for a tile-entity job, since the pack's doors keep their state in one.</li>
 * </ul>
 *
 * <p>
 * One instance per chunk and run; not thread-safe. Side: both (plain Java).
 */
public final class CityChunkWriter {

    /** Most leaves stacked on one column along a building (upstream's {@code cnt < 30}). */
    static final int MAX_VEGETATION_STACK = 30;
    /** Cap of the per-column chance of those leaves (upstream's {@code Math.min(.8f, …)}). */
    static final float MAX_VEGETATION_CHANCE = 0.8f;
    /** Upstream's vegetation seed: {@code worldSeed × 377 + chunkZ × 341873128712 + chunkX × 132897987541}. */
    private static final long VEGETATION_WORLD = 377L;
    private static final long VEGETATION_Z = 341873128712L;
    private static final long VEGETATION_X = 132897987541L;

    /** Rail metadata along X (1.7.10's east-west shape). */
    static final int RAIL_EAST_WEST = 1;
    /** Rail metadata along Z (north-south). */
    static final int RAIL_NORTH_SOUTH = 0;
    /** Upper door half metadata (1.7.10). */
    static final int DOOR_UPPER = 8;
    /** Door facing metadata, lower half: east 0, south 1, west 2, north 3. */
    static final int FACING_EAST = 0;
    static final int FACING_SOUTH = 1;
    static final int FACING_WEST = 2;
    static final int FACING_NORTH = 3;

    /** Depth of a rail corridor below the profile's ground level (upstream's {@code groundLevel − 6}). */
    static final int CORRIDOR_DEPTH = 6;

    private final CityWriteContext context;
    private final LayoutPlanner planner;
    private final ChunkLayout layout;
    private final ChunkPlan plan;
    private final AssetSet assets;
    private final CityTarget target;
    private final PartWriter writer;
    private final CompiledPalette palette;
    private final CityPalettes.ChunkPalette chunkPalette;
    private final CityStyle cityStyle;
    private final com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks style;
    private final Random random;
    private final PaletteSequence sequence;
    private final int chunkX;
    private final int chunkZ;

    /**
     * Prepares the write of one chunk.
     *
     * @param context  the world's city writing context
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param target   where the blocks or jobs go
     * @param random   the chunk's content random; street parts are picked from it
     * @param sequence the chunk's palette sequence
     */
    public CityChunkWriter(CityWriteContext context, int chunkX, int chunkZ, CityTarget target, Random random,
        PaletteSequence sequence) {
        this.context = context;
        this.planner = context.planner();
        this.layout = planner.layout(chunkX, chunkZ);
        this.plan = layout.plan();
        this.assets = context.assets();
        this.target = target;
        this.random = random;
        this.sequence = sequence;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkPalette = context.chunkPalette(plan);
        this.palette = chunkPalette.palette();
        this.cityStyle = assets.cityStyle(CityPalettes.id(plan.cityStyle()));
        this.style = context.styleBlocks(plan);
        this.writer = new PartWriter(target, sequence, context.rules(plan, style), context.log());
        PartRecorder recorder = context.recorder();
        if (!target.dryRun() && recorder != PartRecorder.NONE) {
            // Part editor (D13): the real write records its parts; the population dry run never does.
            recorder.beginChunk(chunkX, chunkZ);
            writer.onPlace(
                placement -> recorder.record(
                    chunkX,
                    chunkZ,
                    placement.part()
                        .id(),
                    placement.oy(),
                    placement.transform(),
                    placement.ox(),
                    placement.oz()));
        }
    }

    /** Returns the layout of the chunk being written. */
    public ChunkLayout layout() {
        return layout;
    }

    /** Writes a city chunk: its building, or its street with its stair. */
    public void writeCity() {
        if (plan.hasBuilding()) {
            writeBuilding();
        } else {
            writeStreet();
            if (plan.maxHighwayLevel() < 0 && layout.slope() == null && plan.lot() == LotKind.NONE) {
                writeStairs();
            }
            clearDoorSteps();
        }
    }

    /**
     * Keeps the two blocks in front of every street door of the buildings around open, at the door's height: an
     * elevated park's raised surface and border, a park or fountain part, or a stair would otherwise stand in the
     * doorway (deviation, decision log M05J rows: upstream let them, so a door into a raised park opened onto a block).
     */
    private void clearDoorSteps() {
        boolean[] approach = doorApproaches(planner, chunkX, chunkZ);
        if (approach == null) {
            return;
        }
        int ground = plan.cityGroundLevel();
        for (Direction side : Direction.values()) {
            for (int d = 0; d < DOOR_STEP; d++) {
                for (int along = 7; along <= 8; along++) {
                    int[] column = vegetationColumn(side, d, along);
                    if (!approach[(column[1] << 4) | column[0]]) {
                        continue;
                    }
                    for (int y = ground + 1; y <= ground + 2; y++) {
                        target.set(column[0], y, column[1], BlockRef.AIR);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ buildings

    private void writeBuilding() {
        int cellars = plan.cellars();
        int floors = plan.floors();
        int lowest = plan.buildingBottomHeight();
        AssetId buildingId = CityPalettes.id(plan.building());
        Building building = buildingId == null ? null : assets.building(buildingId);
        List<PartWriter.Placement> overlays = new ArrayList<>();
        int height = lowest;
        for (int f = -cellars; f <= floors; f++) {
            if (f == floors) {
                target.clearToMax(height);
            }
            Part part = part(plan.floorPart(f));
            if (part != null) {
                // Floors below the roof take the interior characters of the palette variety (AR-3, D25).
                writer.place(
                    placement(part, Transform.ROTATE_NONE, height, HardAir.AIR, buildingId).interior(f < floors));
            }
            Part overlay = part(plan.floorPart2(f));
            if (overlay != null) {
                overlays.add(placement(overlay, Transform.ROTATE_NONE, height, HardAir.AIR, buildingId));
            }
            if (f < floors && (building == null || building.allowDoors())) {
                doors(height + 1, f, building);
            }
            height += ChunkPlan.FLOOR_HEIGHT;
        }
        if (cellars > 0 && building != null && building.allowFillers()) {
            cellarFillers(lowest, building.filler());
        }
        if (cellars >= 1) {
            corridorConnections();
        }
        // Upstream commits the chunk before the overlays; writes here are direct, so they simply come last.
        for (PartWriter.Placement overlay : overlays) {
            writer.place(overlay);
        }
    }

    /**
     * Writes the doorways of one floor (upstream's {@code Doors.generateDoors}): on each side the layout decides
     * nothing, a passage or a door, framed in the building's filler from the floor up four blocks.
     */
    private void doors(int height, int floor, Building building) {
        int base = height - 1;
        BlockRef filler = building == null ? null : blockOf(building.filler());
        if (filler == null) {
            filler = BlockRef.of("minecraft:stonebrick", 0);
        }
        BlockRef door = doorBlock(plan.door());
        for (Direction side : Direction.values()) {
            Doorway doorway = planner.doorway(chunkX, chunkZ, floor, side);
            if (doorway == Doorway.NONE) {
                continue;
            }
            doorway(side, base, filler, doorway == Doorway.DOOR ? door : null);
        }
    }

    private void doorway(Direction side, int base, BlockRef filler, BlockRef door) {
        int fixed = side == Direction.XMIN || side == Direction.ZMIN ? 0 : 15;
        boolean alongZ = side == Direction.XMIN || side == Direction.XMAX;
        for (int along = 6; along <= 9; along++) {
            int x = alongZ ? fixed : along;
            int z = alongZ ? along : fixed;
            boolean frame = along == 6 || along == 9;
            for (int dy = 0; dy < 4; dy++) {
                int y = base + dy;
                if (frame || dy == 0 || dy == 3) {
                    target.set(x, y, z, filler);
                } else if (door == null) {
                    target.set(x, y, z, BlockRef.AIR);
                } else {
                    target.set(x, y, z, doorHalf(door, side, along == 7, dy == 2));
                    target.tileEntity(x, y, z, null, Transform.ROTATE_NONE);
                }
            }
        }
    }

    /**
     * Returns one half of a door: upstream's facing per side (west wall faces east, east wall west, north wall north,
     * south wall south) and hinge (left on the lower-coordinate leaf of the west and north walls, right on the east and
     * south walls), as 1.7.10 metadata.
     */
    static BlockRef doorHalf(BlockRef door, Direction side, boolean lowerLeaf, boolean upper) {
        boolean leftHinge = side == Direction.XMIN || side == Direction.ZMIN ? lowerLeaf : !lowerLeaf;
        if (upper) {
            return door.withMeta(DOOR_UPPER + (leftHinge ? 0 : 1));
        }
        switch (side) {
            case XMIN:
                return door.withMeta(FACING_EAST);
            case XMAX:
                return door.withMeta(FACING_WEST);
            case ZMIN:
                return door.withMeta(FACING_NORTH);
            default:
                return door.withMeta(FACING_SOUTH);
        }
    }

    private BlockRef doorBlock(DoorKind kind) {
        String key = "door." + kind.name()
            .toLowerCase(java.util.Locale.ROOT);
        BlockChoice choice = assets.codeBlock(key);
        if (choice == null) {
            choice = assets.codeBlock("door.oak");
        }
        return choice == null ? BlockRef.of("minecraft:wooden_door", 0)
            : choice.block(
                context.palettes()
                    .flags());
    }

    /**
     * Upstream's cellar fillers: the building's perimeter columns, from the lowest level up to the lower of this
     * chunk's and the neighbor's city ground, take the filler, hiding cellar windows underground.
     */
    private void cellarFillers(int lowest, char filler) {
        int ground = plan.cityGroundLevel();
        int north = Math.min(ground, neighbor(0, -1).cityGroundLevel());
        int south = Math.min(ground, neighbor(0, 1).cityGroundLevel());
        int west = Math.min(ground, neighbor(-1, 0).cityGroundLevel());
        int east = Math.min(ground, neighbor(1, 0).cityGroundLevel());
        for (int x = 0; x < 16; x++) {
            fillColumn(x, 0, lowest, north, filler);
            fillColumn(x, 15, lowest, south, filler);
        }
        for (int z = 1; z < 15; z++) {
            fillColumn(0, z, lowest, west, filler);
            fillColumn(15, z, lowest, east, filler);
        }
    }

    private void fillColumn(int x, int z, int from, int to, char character) {
        for (int y = from; y <= to; y++) {
            BlockRef block = drawBlock(character);
            if (block != null) {
                target.set(x, y, z, block);
            }
        }
    }

    /** Upstream's {@code generateCorridorConnections}: opens the cellar wall where a rail corridor arrives. */
    private void corridorConnections() {
        int groundLevel = context.settings()
            .groundLevel();
        for (Direction side : Direction.values()) {
            int nx = chunkX + side.stepX();
            int nz = chunkZ + side.stepZ();
            if (!planner.hasCorridor(nx, nz, side.orientation())) {
                continue;
            }
            int fixed = side == Direction.XMIN || side == Direction.ZMIN ? 0 : 15;
            for (int along = 7; along <= 10; along++) {
                for (int y = groundLevel - 5; y < groundLevel - 2; y++) {
                    if (side.orientation() == Orientation.X) {
                        target.set(fixed, y, along, BlockRef.AIR);
                    } else {
                        target.set(along, y, fixed, BlockRef.AIR);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ streets

    private void writeStreet() {
        boolean xCorridor = layout.hasCorridor(Orientation.X);
        boolean zCorridor = layout.hasCorridor(Orientation.Z);
        if (xCorridor || zCorridor) {
            corridors(xCorridor, zCorridor);
        }
        int level = plan.cityLevel();
        boolean canDoStreetOrPark = plan.highwayXLevel() != level && plan.highwayZLevel() != level;
        if (!canDoStreetOrPark) {
            return;
        }
        int height = plan.cityGroundLevel();
        if (plan.lot() != LotKind.NONE) {
            // Deviation (AR-6, docs/design/0011-city-edges-and-character.md §3): an edge lot, where nothing grows.
            new EdgeLots(target, plan, assets, context.worldSeed()).write(height);
            return;
        }
        Direction slope = layout.slope();
        // Avoids TERR-07: the plan's street type, never re-rolled.
        StreetType type = plan.streetType();
        boolean elevated = layout.isElevatedPark();
        if (elevated) {
            BlockRef elevation = drawBlock(setting(CityStyle.Setting.PARK_ELEVATION_BLOCK));
            if (elevation != null) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        target.set(x, height, z, elevation);
                    }
                }
            }
            if (plan.settings()
                .parkElevation()) {
                height++;
            }
        }
        switch (type) {
            case FULL:
                streetPart(CityStyle.StreetSlot.FULL, Transform.ROTATE_NONE, height);
                break;
            case PARK:
                parkSection(height, elevated);
                break;
            default:
                if (slope == null) {
                    normalStreetSection(height);
                } else {
                    streetPart(CityStyle.StreetSlot.STAIR, rotation(slope), height);
                }
        }
        height++;
        if (slope != null) {
            return;
        }
        String extra = type == StreetType.PARK ? plan.parkPart() : plan.fountainPart();
        Part part = part(extra);
        if (part != null) {
            writer.place(placement(part, Transform.ROTATE_NONE, height, HardAir.AIR, null));
        }
        if (!target.dryRun()) {
            vegetation(height);
            if (type == StreetType.PARK) {
                greenery(height);
            }
        }
        front(height, Direction.XMIN, Transform.ROTATE_NONE);
        front(height, Direction.ZMIN, Transform.ROTATE_90);
        front(height, Direction.XMAX, Transform.ROTATE_180);
        front(height, Direction.ZMAX, Transform.ROTATE_270);
    }

    private void normalStreetSection(int height) {
        boolean xmin = streetConnection(Direction.XMIN);
        boolean xmax = streetConnection(Direction.XMAX);
        boolean zmin = streetConnection(Direction.ZMIN);
        boolean zmax = streetConnection(Direction.ZMAX);
        int count = (xmin ? 1 : 0) + (xmax ? 1 : 0) + (zmin ? 1 : 0) + (zmax ? 1 : 0);
        CityStyle.StreetSlot slot;
        Transform transform = Transform.ROTATE_NONE;
        switch (count) {
            case 0:
                slot = CityStyle.StreetSlot.NONE;
                break;
            case 1:
                slot = CityStyle.StreetSlot.END;
                transform = xmin ? Transform.ROTATE_NONE
                    : xmax ? Transform.ROTATE_180 : zmin ? Transform.ROTATE_90 : Transform.ROTATE_270;
                break;
            case 2:
                if (xmin == xmax || zmin == zmax) {
                    slot = CityStyle.StreetSlot.STRAIGHT;
                    transform = xmin ? Transform.ROTATE_NONE
                        : xmax ? Transform.ROTATE_180 : zmin ? Transform.ROTATE_90 : Transform.ROTATE_270;
                } else {
                    slot = CityStyle.StreetSlot.BEND;
                    if (xmin && zmin) {
                        transform = Transform.ROTATE_NONE;
                    } else if (xmin) {
                        transform = Transform.ROTATE_270;
                    } else if (zmin) {
                        transform = Transform.ROTATE_90;
                    } else {
                        transform = Transform.ROTATE_180;
                    }
                }
                break;
            case 3:
                slot = CityStyle.StreetSlot.T;
                if (!xmin) {
                    transform = Transform.ROTATE_90;
                } else if (!xmax) {
                    transform = Transform.ROTATE_270;
                } else if (!zmin) {
                    transform = Transform.ROTATE_180;
                }
                break;
            default:
                slot = CityStyle.StreetSlot.ALL;
        }
        if (streetPart(slot, transform, height)) {
            minorStreetConnectors(height);
        }
    }

    /** Upstream's {@code hasStreetPartConnection}: a primary road turns only toward primary roads and bridges. */
    private boolean streetConnection(Direction side) {
        ChunkPlan adjacent = neighbor(side.stepX(), side.stepZ());
        boolean bridge = planner.bridgePart(adjacent.chunkX(), adjacent.chunkZ(), side.orientation()) != null;
        boolean road = planner.hasRoadConnection(plan, adjacent);
        if (plan.isPrimaryRoad()) {
            return road && adjacent.isPrimaryRoad() || bridge;
        }
        return road || bridge;
    }

    private void minorStreetConnectors(int height) {
        if (!plan.isPrimaryRoad() || streetParts(CityStyle.StreetSlot.CONNECTOR).isEmpty()) {
            return;
        }
        connector(Direction.XMIN, Transform.ROTATE_NONE, height);
        connector(Direction.XMAX, Transform.ROTATE_180, height);
        connector(Direction.ZMIN, Transform.ROTATE_90, height);
        connector(Direction.ZMAX, Transform.ROTATE_270, height);
    }

    private void connector(Direction side, Transform transform, int height) {
        ChunkPlan adjacent = neighbor(side.stepX(), side.stepZ());
        if (planner.hasRoadConnection(plan, adjacent) && !adjacent.isPrimaryRoad()) {
            streetPart(CityStyle.StreetSlot.CONNECTOR, transform, height);
        }
    }

    /** Writes a random part of a street slot; returns whether one was written. */
    private boolean streetPart(CityStyle.StreetSlot slot, Transform transform, int height) {
        List<Part> parts = streetParts(slot);
        if (parts.isEmpty()) {
            // Avoids ASSET-32: nothing to write, reported once.
            context.log()
                .warn("city style " + plan.cityStyle() + " has no " + slot + " street parts");
            return false;
        }
        writer.place(placement(pick(parts, random), transform, height, HardAir.VOID, null));
        return true;
    }

    /**
     * Upstream's {@code getRandomPart}: the only part of a one-part list without a draw, else one {@code nextInt}.
     *
     * @param parts  the list
     * @param random the content random
     * @return the part, or {@code null} for an empty list (avoids ASSET-32), drawing nothing
     */
    static Part pick(List<Part> parts, Random random) {
        if (parts.isEmpty()) {
            return null;
        }
        return parts.size() == 1 ? parts.get(0) : parts.get(random.nextInt(parts.size()));
    }

    private List<Part> streetParts(CityStyle.StreetSlot slot) {
        if (cityStyle == null) {
            return new ArrayList<>();
        }
        CityStyle.StreetSet set = plan.plannedRoad() == PlannedRoadType.PRIMARY ? CityStyle.StreetSet.LARGE_PARTS
            : plan.plannedRoad() == PlannedRoadType.TERTIARY ? CityStyle.StreetSet.TERTIARY_PARTS
                : CityStyle.StreetSet.PARTS;
        return cityStyle.streetParts(set, slot);
    }

    /**
     * Upstream's {@code generateParkSection}: grass inside, and on the edge the street block where the city has park
     * borders, except where an elevated park continues into its elevated neighbors.
     */
    private void parkSection(int height, boolean elevated) {
        Character grassChar = setting(CityStyle.Setting.PARK_GRASS);
        BlockRef grassFallback = code("grass", BlockRef.of("minecraft:grass", 0));
        Character streetChar = setting(CityStyle.Setting.STREET);
        boolean parkBorder = plan.settings()
            .parkBorder();
        boolean el00 = elevated && planner.isElevatedParkSection(chunkX - 1, chunkZ - 1);
        boolean el10 = elevated && planner.isElevatedParkSection(chunkX, chunkZ - 1);
        boolean el20 = elevated && planner.isElevatedParkSection(chunkX + 1, chunkZ - 1);
        boolean el01 = elevated && planner.isElevatedParkSection(chunkX - 1, chunkZ);
        boolean el21 = elevated && planner.isElevatedParkSection(chunkX + 1, chunkZ);
        boolean el02 = elevated && planner.isElevatedParkSection(chunkX - 1, chunkZ + 1);
        boolean el12 = elevated && planner.isElevatedParkSection(chunkX, chunkZ + 1);
        boolean el22 = elevated && planner.isElevatedParkSection(chunkX + 1, chunkZ + 1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean edge = x == 0 || x == 15 || z == 0 || z == 15;
                boolean grass = !edge;
                if (edge && elevated) {
                    if (x == 0 && z == 0) {
                        grass = el01 && el00 && el10;
                    } else if (x == 15 && z == 0) {
                        grass = el21 && el20 && el10;
                    } else if (x == 0 && z == 15) {
                        grass = el01 && el02 && el12;
                    } else if (x == 15 && z == 15) {
                        grass = el12 && el22 && el21;
                    } else if (x == 0) {
                        grass = el01;
                    } else if (x == 15) {
                        grass = el21;
                    } else if (z == 0) {
                        grass = el10;
                    } else {
                        grass = el12;
                    }
                }
                BlockRef block;
                if (grass || !parkBorder) {
                    block = grassChar == null ? grassFallback : drawBlock(grassChar);
                } else {
                    block = drawBlock(streetChar);
                }
                if (block != null) {
                    target.set(x, height, z, block);
                }
            }
        }
    }

    /**
     * Fills the park's free grass with tall grass, flowers, ferns and shrubs for its biome, like bonemeal (deviation,
     * AR-18: upstream's parks keep only their part's few flowers and saplings). Nothing where the profile or city style
     * avoids foliage.
     */
    private void greenery(int height) {
        if (plan.settings()
            .avoidFoliage()) {
            return;
        }
        ParkGreenery.Flora flora = ParkGreenery.floraOf(
            context.biomes()
                .biomeOfChunk(chunkX, chunkZ));
        ParkGreenery.plant(target, height, flora, palette, ParkGreenery.random(context.worldSeed(), chunkX, chunkZ));
    }

    /**
     * Upstream's {@code generateRandomVegetation}: along each side whose neighbor has a building, leaves stacked on the
     * ground in the outer columns, more often nearer the building.
     *
     * <p>
     * Avoids TERR-08: every side uses the same weights, {@code T + 1 − d} for the column {@code d} blocks from the
     * edge (d from 0 to {@code T − 1}), capped at 0.8, so the outermost column is used on all four sides.
     */
    private void vegetation(int height) {
        Random vegetation = new Random(
            context.worldSeed() * VEGETATION_WORLD + chunkZ * VEGETATION_Z + chunkX * VEGETATION_X);
        boolean[] clear = doorApproaches(planner, chunkX, chunkZ);
        int thickness = context.settings()
            .randomLeafBlockThickness();
        float chance = context.settings()
            .randomLeafBlockChance();
        for (Direction side : Direction.values()) {
            if (!neighbor(side.stepX(), side.stepZ()).hasBuilding()) {
                continue;
            }
            for (int d = 0; d < thickness; d++) {
                float p = vegetationChance(d, thickness, chance);
                for (int along = 0; along < 16; along++) {
                    int[] column = vegetationColumn(side, d, along);
                    int x = column[0];
                    int z = column[1];
                    if (clear != null && clear[(z << 4) | x]) {
                        // A door's approach stays clear (decision log, M05J rows).
                        continue;
                    }
                    int y = height;
                    while (y > WorldLimits.MIN_Y && target.get(x, y - 1, z)
                        .isAir()) {
                        y--;
                    }
                    int count = 0;
                    while (vegetation.nextFloat() < p && count < MAX_VEGETATION_STACK && y <= WorldLimits.MAX_Y) {
                        target.set(x, y, z, leaf());
                        y++;
                        count++;
                    }
                }
            }
        }
    }

    /** How deep into a street a door's approach reaches, in blocks from the building's side. */
    static final int DOOR_APPROACH = 3;
    /** How deep in front of a door the doorway's height is kept open, in blocks. */
    static final int DOOR_STEP = 2;

    /**
     * Returns the columns in front of the street doors of the buildings around a street chunk: the two door columns,
     * {@value #DOOR_APPROACH} blocks deep, for every neighbor whose floor at this street's level has a door on the side
     * facing it. Vegetation and the rubble layer leave them clear (deviation, decision log M05J rows: upstream's leaves
     * and rubble could block a door).
     *
     * @param planner the layout planner
     * @param chunkX  the street chunk's X
     * @param chunkZ  the street chunk's Z
     * @return 256 flags, index {@code z × 16 + x}; {@code null} when no door faces the chunk
     */
    static boolean[] doorApproaches(LayoutPlanner planner, int chunkX, int chunkZ) {
        ChunkPlan street = planner.plan(chunkX, chunkZ);
        if (!street.isCity() || street.hasBuilding()) {
            return null;
        }
        boolean[] columns = null;
        for (Direction side : Direction.values()) {
            int nx = chunkX + side.stepX();
            int nz = chunkZ + side.stepZ();
            ChunkPlan building = planner.plan(nx, nz);
            if (!building.hasBuilding()) {
                continue;
            }
            int floor = street.cityLevel() - building.cityLevel();
            if (floor < 0 || floor >= building.floors()
                || planner.doorway(nx, nz, floor, side.opposite()) != Doorway.DOOR) {
                continue;
            }
            if (columns == null) {
                columns = new boolean[256];
            }
            for (int d = 0; d < DOOR_APPROACH; d++) {
                for (int along = 7; along <= 8; along++) {
                    int[] column = vegetationColumn(side, d, along);
                    columns[(column[1] << 4) | column[0]] = true;
                }
            }
        }
        return columns;
    }

    /**
     * Returns the chance of leaves on a column {@code d} blocks from a building's side: {@code chance × (T + 1 − d)},
     * capped at 0.8 on every side (avoids TERR-08).
     *
     * @param d         the distance from the side, 0 to {@code thickness − 1}
     * @param thickness the profile's {@code randomLeafBlockThickness}
     * @param chance    the profile's {@code randomLeafBlockChance}
     * @return the chance
     */
    static float vegetationChance(int d, int thickness, float chance) {
        return Math.min(MAX_VEGETATION_CHANCE, chance * (thickness + 1 - d));
    }

    /**
     * Returns the column {@code d} blocks in from a side at a position along it, the same rule on every side (avoids
     * TERR-08: upstream's +X and +Z sides started one column in and never used column 15).
     *
     * @param side  the side the building is on
     * @param d     the distance from that side
     * @param along the position along the side
     * @return {x, z}
     */
    static int[] vegetationColumn(Direction side, int d, int along) {
        switch (side) {
            case XMIN:
                return new int[] { d, along };
            case XMAX:
                return new int[] { 15 - d, along };
            case ZMIN:
                return new int[] { along, d };
            default:
                return new int[] { along, 15 - d };
        }
    }

    /**
     * Upstream's {@code generateFrontPart}, without TERR-09: the neighbor building's front part, written here with this
     * chunk's positions and jobs and the neighbor's palette.
     */
    private void front(int height, Direction side, Transform transform) {
        if (!layout.hasFrontPartFrom(side)) {
            return;
        }
        ChunkPlan building = neighbor(side.stepX(), side.stepZ());
        Part part = part(building.frontPart());
        if (part == null) {
            return;
        }
        CityPalettes.ChunkPalette buildingPalette = context.chunkPalette(building);
        CompiledPalette partPalette = context.palettes()
            .forPart(buildingPalette, part);
        writer.place(
            new PartWriter.Placement(
                part,
                partPalette,
                transform,
                0,
                height,
                0,
                HardAir.AIR,
                CityPalettes.id(building.building())));
    }

    /** Upstream's {@code generateStreetDecorations}: the stair part, toward the higher street. */
    private void writeStairs() {
        Direction stair = layout.stair();
        Part part = stair == null ? null : part(plan.stairPart());
        if (part != null) {
            writer.place(placement(part, rotation(stair), plan.cityGroundLevel() + 1, HardAir.AIR, null));
        }
    }

    /**
     * Upstream's {@code Corridors.generateCorridors}: a rail corridor under the street, 4 wide, at the profile's ground
     * level minus 6, with rails on its outer track, glass and glowstone in its roof at the chunk's middle, and the base
     * block around it up to the street.
     */
    private void corridors(boolean xRail, boolean zRail) {
        int groundLevel = context.settings()
            .groundLevel();
        int bottom = groundLevel - CORRIDOR_DEPTH;
        Character roofChar = setting(CityStyle.Setting.CORRIDOR_ROOF);
        Character glassChar = setting(CityStyle.Setting.CORRIDOR_GLASS);
        BlockRef rail = code("rail", BlockRef.of("minecraft:rail", 0));
        BlockRef glowstone = blockOrDefault(
            setting(CityStyle.Setting.GLOWSTONE),
            code("glowstone", BlockRef.of("minecraft:glowstone", 0)));
        BlockRef base = style.base();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (xRail && z >= 7 && z <= 10 || zRail && x >= 7 && x <= 10) {
                    BlockRef track = BlockRef.AIR;
                    if (xRail && z == 10) {
                        track = rail.withMeta(RAIL_EAST_WEST);
                    } else if (zRail && x == 10) {
                        track = rail.withMeta(RAIL_NORTH_SOUTH);
                    }
                    setDrawn(x, bottom, z, roofChar);
                    target.set(x, bottom + 1, z, track);
                    target.set(x, bottom + 2, z, BlockRef.AIR);
                    target.set(x, bottom + 3, z, BlockRef.AIR);
                    if (xRail && x == 7 && (z == 8 || z == 9) || zRail && z == 7 && (x == 8 || x == 9)) {
                        setDrawn(x, bottom + 4, z, glassChar);
                        target.set(x, bottom + 5, z, glowstone);
                    } else {
                        setDrawn(x, bottom + 4, z, roofChar);
                        setDrawn(x, bottom + 5, z, roofChar);
                    }
                } else {
                    for (int y = groundLevel - 5; y < plan.cityGroundLevel(); y++) {
                        target.set(x, y, z, base);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ bridges

    /**
     * Writes the bridge span over a non-city chunk (upstream's {@code Bridges.generateBridges}), with its supports.
     *
     * @return whether a bridge was written
     */
    public boolean writeBridge() {
        return BridgeWriter.write(context, layout, plan, palette, chunkPalette, target, sequence);
    }

    // ------------------------------------------------------------------ helpers

    private PartWriter.Placement placement(Part part, Transform transform, int oy, HardAir hardAir, AssetId building) {
        return new PartWriter.Placement(
            part,
            context.palettes()
                .forPart(chunkPalette, part),
            transform,
            0,
            oy,
            0,
            hardAir,
            building);
    }

    private Part part(String name) {
        AssetId id = CityPalettes.id(name);
        if (id == null) {
            return null;
        }
        Part part = assets.part(id);
        if (part == null) {
            context.log()
                .warn("part " + name + " is not loaded; skipped");
        }
        return part;
    }

    /** Returns the next leaf of the chunk's leaf table, a palette-sequence slot per leaf (upstream's getRandomLeaf). */
    private BlockRef leaf() {
        List<BlockRef> leaves = style.leaves();
        return leaves.get(sequence.next() % leaves.size());
    }

    private ChunkPlan neighbor(int dx, int dz) {
        return planner.plan(chunkX + dx, chunkZ + dz);
    }

    private Character setting(CityStyle.Setting setting) {
        return CityPalettes.setting(cityStyle, setting);
    }

    /** Draws one block of a character from the chunk palette, a slot per block for a random character. */
    BlockRef drawBlock(Character c) {
        if (c == null || !palette.isDefined(c)) {
            return null;
        }
        BlockChoice choice = palette.isRandom(c) ? palette.choice(c, sequence.next()) : palette.choice(c, 0);
        if (choice == null || choice.isHardAir()) {
            return null;
        }
        return choice.block(palette.flags());
    }

    private BlockRef blockOf(char c) {
        return CityPalettes.blockOf(palette, c);
    }

    private BlockRef blockOrDefault(Character c, BlockRef fallback) {
        BlockRef block = CityPalettes.blockOf(palette, c);
        return block == null ? fallback : block;
    }

    private void setDrawn(int x, int y, int z, Character c) {
        BlockRef block = drawBlock(c);
        if (block != null) {
            target.set(x, y, z, block);
        }
    }

    private BlockRef code(String key, BlockRef fallback) {
        BlockRef block = context.palettes()
            .code(key);
        return block == null ? fallback : block;
    }

    /**
     * Returns upstream's rotation for a side ({@code Direction.getRotation}): parts are authored facing −X.
     *
     * @param side the side
     * @return the transform
     */
    static Transform rotation(Direction side) {
        switch (side) {
            case XMAX:
                return Transform.ROTATE_180;
            case ZMIN:
                return Transform.ROTATE_90;
            case ZMAX:
                return Transform.ROTATE_270;
            default:
                return Transform.ROTATE_NONE;
        }
    }
}
