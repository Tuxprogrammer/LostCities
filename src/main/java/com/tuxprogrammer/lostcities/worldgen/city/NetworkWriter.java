package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Highways.java
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Railways.java
// Avoids: HWY-02, HWY-12, CITY-19

import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.Networks;
import com.tuxprogrammer.lostcities.layout.highway.Axis;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Writes one chunk's highways, railways and rail dungeon from the layout's networks (upstream's
 * {@code Highways.generateHighways}, {@code Railways.generateRailways} and {@code generateRailwayDungeons}). The terrain
 * phase runs it last in the chunk, and the job dry run runs it too, so stations' and dungeons' chests and spawners are
 * population jobs (0005 §2, §3).
 *
 * <p>
 * Highways: each piece of {@link HighwayInfo#pieces} is a tunnel where the ground or the city stands above it, an open
 * road in a city whose neighbors across the road are city at or above its level, and a bridge elsewhere, written at
 * {@code groundLevel + 6 × level} with hard air as the water level; open roads and bridges clear 15 blocks above
 * (leaves, logs and plants stay, the clearing stops at Y 255, a drowned city fills to its water line); supports drop
 * from every piece where {@code highwaySupports} is on.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids HWY-02: a north-south piece tests its west and east neighbors, not the east one twice.</li>
 * <li>Avoids HWY-12: a support or rail block missing from the palette is skipped with one log line, never a crash.</li>
 * <li>Avoids CITY-19 (HWY-07): where the world style's {@code railwayavoidance} is {@code block_railway} and this
 * chunk's building reaches down into its rail, only this chunk's rail is left out; the layout's answer for its
 * neighbors never changes.</li>
 * <li>Ramps and plazas (design record 0007, alpha review AR-10): a ramp is the straight piece written column by column
 * along its axis a half block higher every few columns, one level across the chunk, with a bottom half slab wherever a
 * column stands a half block up, so the climb never needs a jump; a run's end is a crossing piece at the ground's
 * level, the plaza a highway ends on.</li>
 * <li>Railways under landmark cores (D30, D31): slopes along a north-south line are upstream's descent parts turned to
 * run north-south; a line or station in a core gets no clearing to the surface and no staircase.</li>
 * <li>The water test accepts still and flowing water and lava, separate blocks in 1.7.10.</li>
 * <li>Railing openings for highway-side scattered buildings belong to 07-scattered (M07).</li>
 * </ul>
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/highway-parts.md, railway-parts.md; docs/new-spec/06-highways-railways/.
 * One instance per chunk and run; not thread-safe. Side: both (plain Java).
 */
public final class NetworkWriter {

    /** Blocks cleared above an open road or a bridge (upstream's {@code clearheight}). */
    static final int CLEAR_HEIGHT = 15;
    /** Longest support column, in blocks. */
    static final int SUPPORT_LENGTH = 40;
    /** Blocks above the oracle height a surface station clears up to. */
    static final int STATION_CLEAR_MARGIN = 4;
    /** Half blocks a ramp climbs across one chunk: one city level. */
    static final int RAMP_HALF_STEPS = 2 * ChunkPlan.FLOOR_HEIGHT;
    /** The half slab on a ramp's half steps: smooth stone, bottom half. */
    static final BlockRef RAMP_SLAB = BlockRef.of("minecraft:stone_slab", 0);
    /** The rows of a three-track part's tracks, part-local Z. */
    static final int TRACK_SOUTH = 9;
    static final int TRACK_MIDDLE = 7;
    static final int TRACK_NORTH = 5;
    /** A rail dungeon tunnel's depth from the north edge (z 0 to 3) and from the south edge (z 11 to 15). */
    static final int NORTH_TUNNEL = 4;
    static final int SOUTH_TUNNEL = 5;

    private final CityWriteContext context;
    private final LayoutPlanner planner;
    private final Networks networks;
    private final ChunkPlan plan;
    private final CityTarget target;
    private final Random random;
    private final PartWriter writer;
    private final CityPalettes.ChunkPalette chunkPalette;
    private final CompiledPalette palette;
    private final WorldStyle worldStyle;
    private final CityStyle cityStyle;
    private final BlockRef liquid;
    private final int chunkX;
    private final int chunkZ;
    private final int naturalHeight;

    /**
     * Prepares the write of one chunk.
     *
     * @param context       the world's city writing context
     * @param chunkX        chunk X
     * @param chunkZ        chunk Z
     * @param target        where the blocks or jobs go
     * @param random        the chunk's network random; parts are picked from lists longer than one
     * @param sequence      the chunk's palette sequence
     * @param naturalHeight the oracle's height of the chunk, block Y
     */
    public NetworkWriter(CityWriteContext context, int chunkX, int chunkZ, CityTarget target, Random random,
        PaletteSequence sequence, int naturalHeight) {
        this.context = context;
        this.planner = context.planner();
        this.networks = planner.networks();
        this.plan = planner.plan(chunkX, chunkZ);
        this.target = target;
        this.random = random;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.naturalHeight = naturalHeight;
        this.chunkPalette = context.chunkPalette(plan);
        this.palette = chunkPalette.palette();
        this.worldStyle = context.worldStyle();
        this.cityStyle = context.assets()
            .cityStyle(CityPalettes.id(plan.cityStyle()));
        com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks style = context.styleBlocks(plan);
        this.liquid = style.liquid();
        this.writer = new PartWriter(target, sequence, context.rules(plan, style), context.log());
    }

    /**
     * Returns whether a chunk has anything for this writer: a highway, a railway, or a rail dungeon beside a railway,
     * or a railway beside a rail dungeon.
     *
     * @param planner the layout planner
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @return whether the writer would write
     */
    public static boolean hasWork(LayoutPlanner planner, int chunkX, int chunkZ) {
        Networks networks = planner.networks();
        if (networks == null) {
            return false;
        }
        if (networks.highway(chunkX, chunkZ)
            .hasHighway()
            || networks.railChunk(chunkX, chunkZ)
                .isPresent()) {
            return true;
        }
        return dungeonRail(planner, chunkX, chunkZ) != null;
    }

    /** Writes the chunk's highways, then its railway, then its rail dungeon. */
    public void write() {
        if (networks == null || worldStyle == null) {
            return;
        }
        writeHighways();
        writeRailway();
        writeRailDungeon();
    }

    // ================================================================== highways

    private void writeHighways() {
        HighwayInfo info = networks.highway(chunkX, chunkZ);
        for (HighwayInfo.Piece piece : info.pieces(chunkX, chunkZ)) {
            highwayPiece(piece);
        }
    }

    private void highwayPiece(HighwayInfo.Piece piece) {
        int level = piece.level();
        int top = piece.topLevel();
        int deckY = context.settings()
            .cityGround(level);
        boolean tunnel = plan.isTunnel(top, naturalHeight);
        String kind;
        if (tunnel) {
            kind = "tunnel";
        } else if (openRoad(piece, level)) {
            kind = "open";
        } else {
            kind = "bridge";
        }
        String slot = kind + suffix(piece.shape());
        List<Part> parts = worldStyle.parts("highways", slot);
        Part part = parts == null ? null : CityChunkWriter.pick(parts, random);
        if (part == null) {
            context.log()
                .warn("world style " + worldStyle.id() + " has no highway part for " + slot);
            return;
        }
        Transform transform = turn(piece.quarterTurns());
        int[] halfSteps = piece.shape() == HighwayInfo.Shape.RAMP ? rampSteps(piece) : null;
        PartWriter.Placement placement = placement(part, transform, deckY, HardAir.WATERLEVEL);
        int height = halfSteps == null ? writer.place(placement) : writer.placeShifted(placement, shifts(halfSteps));
        if (halfSteps != null && !target.dryRun()) {
            rampSlabs(part, transform, deckY, halfSteps);
        }
        if (!tunnel && !target.dryRun()) {
            clearAbove(part, transform, height, halfSteps);
        }
        if (networks.highways()
            .settings()
            .supports()) {
            supports(part, transform, deckY, piece, halfSteps);
        }
    }

    /**
     * Upstream's open-road test: in a city whose two neighbors across the road are city at or above the level. A
     * junction tests its east and south neighbors, an east-west piece its north and south ones, and a north-south
     * piece its west and east ones (avoids HWY-02: upstream tested the east neighbor twice).
     */
    private boolean openRoad(HighwayInfo.Piece piece, int level) {
        if (!plan.isCity()) {
            return false;
        }
        ChunkPlan a;
        ChunkPlan b;
        if (piece.junction()) {
            a = planner.plan(chunkX + 1, chunkZ);
            b = planner.plan(chunkX, chunkZ + 1);
        } else if (piece.axis() == Axis.X) {
            a = planner.plan(chunkX, chunkZ - 1);
            b = planner.plan(chunkX, chunkZ + 1);
        } else {
            a = planner.plan(chunkX - 1, chunkZ);
            b = planner.plan(chunkX + 1, chunkZ);
        }
        return a.isCity() && b.isCity() && level <= a.cityLevel() && level <= b.cityLevel();
    }

    private static String suffix(HighwayInfo.Shape shape) {
        switch (shape) {
            case CROSSING:
                return "_bi";
            case BEND:
                return "_bend";
            case T_JUNCTION:
                return "_t";
            default:
                return "";
        }
    }

    private static Transform turn(int quarterTurns) {
        switch (quarterTurns & 3) {
            case 1:
                return Transform.ROTATE_90;
            case 2:
                return Transform.ROTATE_180;
            case 3:
                return Transform.ROTATE_270;
            default:
                return Transform.ROTATE_NONE;
        }
    }

    /**
     * Returns a ramp's height above its lower level at each part-local X (along its axis), in half blocks: 0 at the
     * low end's column, {@link #RAMP_HALF_STEPS} at the high end's, at most one half block between neighbors.
     */
    static int[] rampSteps(HighwayInfo.Piece piece) {
        boolean risesEastOrSouth = piece.highLevel() > piece.lowLevel();
        int[] steps = new int[16];
        for (int i = 0; i < 16; i++) {
            int rise = Math.round(RAMP_HALF_STEPS * (i + 0.5f) / 16f);
            int column = risesEastOrSouth ? i : 15 - i;
            steps[column] = rise;
        }
        return steps;
    }

    private static int[] shifts(int[] halfSteps) {
        int[] out = new int[halfSteps.length];
        for (int i = 0; i < halfSteps.length; i++) {
            out[i] = halfSteps[i] / 2;
        }
        return out;
    }

    /** A bottom half slab on the deck of every column a half block up. */
    private void rampSlabs(Part part, Transform transform, int deckY, int[] halfSteps) {
        for (int px = 0; px < 16; px++) {
            if ((halfSteps[px] & 1) == 0) {
                continue;
            }
            for (int pz = 0; pz < part.zsize(); pz++) {
                int x = transform.rotateX(px, pz);
                int z = transform.rotateZ(px, pz);
                int y = deckY + halfSteps[px] / 2;
                if (x < 0 || x > 15 || z < 0 || z > 15 || y + 1 > WorldLimits.MAX_Y) {
                    continue;
                }
                if (!context.isEmpty(target.get(x, y, z)) && target.get(x, y + 1, z)
                    .isAir()) {
                    target.set(x, y + 1, z, RAMP_SLAB);
                }
            }
        }
    }

    /**
     * Upstream's clearing above an open road or bridge: every column from the part's top for {@link #CLEAR_HEIGHT}
     * blocks, stopping at Y 255, leaving foliage; in a drowned city the liquid up to the water level.
     */
    private void clearAbove(Part part, Transform transform, int height, int[] halfSteps) {
        boolean drowned = context.settings()
            .waterLevel()
            > context.settings()
                .groundLevel();
        int waterLevel = context.settings()
            .waterLevel();
        for (int px = 0; px < 16; px++) {
            int shift = halfSteps == null ? 0 : halfSteps[px] / 2;
            for (int pz = 0; pz < 16; pz++) {
                int x = transform.rotateX(px, pz);
                int z = transform.rotateZ(px, pz);
                if (x < 0 || x > 15 || z < 0 || z > 15) {
                    continue;
                }
                int from = height + shift;
                int to = Math.min(WorldLimits.MAX_Y, from + CLEAR_HEIGHT - 1);
                for (int y = from; y <= to; y++) {
                    BlockRef here = target.get(x, y, z);
                    if (here.isAir() || context.isFoliage(here)) {
                        continue;
                    }
                    target.set(x, y, z, drowned && y <= waterLevel ? liquid : BlockRef.AIR);
                }
            }
        }
    }

    /**
     * Upstream's supports: the part's {@code support} character, else the world style's support character and support
     * part; a support part repeats down from the deck, or columns drop from the part's west corners (and its east one
     * for a bend or T), each at most {@link #SUPPORT_LENGTH} blocks, through air and liquid only.
     */
    private void supports(Part part, Transform transform, int deckY, HighwayInfo.Piece piece, int[] halfSteps) {
        Character support = part.metaCharacter(Part.MetaKey.SUPPORT);
        Part supportPart = null;
        if (support == null) {
            support = worldStyle.highwaySupport();
            supportPart = worldStyle.highwaySupportPart();
        }
        if (supportPart != null) {
            if (!target.dryRun()) {
                BridgeWriter.supportPart(context, supportPart, chunkPalette, transform, deckY - 1, target);
            }
            return;
        }
        if (support == null) {
            return;
        }
        BlockRef block = CityPalettes.blockOf(context.palettes()
            .forPart(chunkPalette, part), support);
        if (block == null) {
            // Avoids HWY-12: upstream threw here.
            context.log()
                .warn("highway part " + part.id() + " has support '" + support + "', which its palette does not define");
            return;
        }
        column(transform, deckY, 0, 15, block, halfSteps);
        column(transform, deckY, 0, 0, block, halfSteps);
        if (piece.shape() == HighwayInfo.Shape.BEND || piece.shape() == HighwayInfo.Shape.T_JUNCTION) {
            column(transform, deckY, 15, 0, block, halfSteps);
        }
    }

    private void column(Transform transform, int deckY, int px, int pz, BlockRef block, int[] halfSteps) {
        int x = transform.rotateX(px, pz);
        int z = transform.rotateZ(px, pz);
        int y = deckY - 1 + (halfSteps == null ? 0 : halfSteps[px] / 2);
        for (int i = 0; i < SUPPORT_LENGTH && y >= WorldLimits.MIN_Y; i++, y--) {
            if (!target.dryRun() && !context.isEmpty(target.get(x, y, z))) {
                return;
            }
            target.set(x, y, z, block);
        }
    }

    // ================================================================== railways

    private void writeRailway() {
        RailChunk rail = networks.railChunk(chunkX, chunkZ);
        if (!rail.isPresent()) {
            return;
        }
        if (blockedByBuilding(rail)) {
            return;
        }
        int height = context.settings()
            .cityGround(rail.level());
        Transform transform = Transform.ROTATE_NONE;
        boolean staircase = false;
        boolean clearUpper = false;
        String slot;
        switch (rail.type()) {
            case STATION_SURFACE:
            case STATION_EXTENSION_SURFACE:
                if (rail.level() < plan.cityLevel()) {
                    slot = "stationunderground";
                } else {
                    slot = Boolean.TRUE.equals(rail.roofed()) ? "stationopenroof" : "stationopen";
                }
                clearUpper = true;
                break;
            case STATION_UNDERGROUND:
                staircase = rail.staircase();
                slot = staircase ? "stationundergroundstairs" : "stationunderground";
                break;
            case STATION_EXTENSION_UNDERGROUND:
                slot = "stationunderground";
                break;
            case RAILS_END_HERE:
                slot = "railshorizontalend";
                transform = rail.direction() == RailChunk.Direction.EAST ? Transform.MIRROR_X : Transform.ROTATE_NONE;
                break;
            case HORIZONTAL:
                slot = horizontalUnderWater(height) ? "railshorizontalwater" : "railshorizontal";
                break;
            case VERTICAL:
                slot = underWater(height) ? "railsverticalwater" : "railsvertical";
                transform = rail.direction() == RailChunk.Direction.EAST ? Transform.MIRROR_X : Transform.ROTATE_NONE;
                break;
            case THREE_SPLIT:
                slot = "rails3split";
                transform = rail.direction() == RailChunk.Direction.EAST ? Transform.MIRROR_X : Transform.ROTATE_NONE;
                break;
            case GOING_DOWN_TWO_FROM_SURFACE:
            case GOING_DOWN_FURTHER:
                slot = "railsdown2";
                transform = slopeTransform(rail);
                break;
            case GOING_DOWN_ONE_FROM_SURFACE:
                slot = "railsdown1";
                transform = slopeTransform(rail);
                break;
            case DOUBLE_BEND:
                slot = "railsbend";
                transform = rail.direction() == RailChunk.Direction.EAST ? Transform.MIRROR_X : Transform.ROTATE_NONE;
                break;
            default:
                slot = "railsflat";
        }
        Part part = railPart(slot);
        if (part == null) {
            return;
        }
        int top = writer.place(placement(part, transform, height, HardAir.AIR));
        if (clearUpper && !target.dryRun()) {
            int clearTo = Math.min(WorldLimits.MAX_Y, naturalHeight + STATION_CLEAR_MARGIN);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = top; y < clearTo; y++) {
                        target.set(x, y, z, BlockRef.AIR);
                    }
                }
            }
        }
        BlockRef railMain = railMain(rail);
        if (railMain == null) {
            return;
        }
        if (rail.type() == RailChunkType.HORIZONTAL) {
            dungeonTunnels(height, railMain);
        }
        if (rail.tracks() < 3) {
            reduceTracks(rail, part, transform, height, railMain);
        }
        if (staircase) {
            staircase(rail);
        }
    }

    /**
     * Avoids CITY-19: under {@code block_railway} avoidance a building whose lowest floor reaches below the rail
     * part's top leaves this chunk's rail out, decided from this chunk's plan alone.
     */
    private boolean blockedByBuilding(RailChunk rail) {
        if (worldStyle.railwayAvoidance() != WorldStyle.RailwayAvoidance.BLOCK_RAILWAY || !plan.hasBuilding()) {
            return false;
        }
        return plan.cityLevel() - plan.cellars() < rail.topLevel() + worldStyle.railPartHeight6();
    }

    /** A slope's transform: its part's low end faces west unmirrored; turned to run north-south along Z. */
    private static Transform slopeTransform(RailChunk rail) {
        boolean lowFirst = rail.direction() != RailChunk.Direction.EAST;
        if (rail.alongZ()) {
            // ROTATE_90 puts the part's x = 0 (its low end) at z = 0 (north); ROTATE_270 at z = 15 (south).
            return lowFirst ? Transform.ROTATE_90 : Transform.ROTATE_270;
        }
        return lowFirst ? Transform.ROTATE_NONE : Transform.MIRROR_X;
    }

    private Part railPart(String slot) {
        List<Part> parts = worldStyle.parts("railways", slot);
        Part part = parts == null ? null : CityChunkWriter.pick(parts, random);
        if (part == null) {
            context.log()
                .warn("world style " + worldStyle.id() + " has no railway part for " + slot);
        }
        return part;
    }

    /** Upstream's water test, only where neither east-west neighbor is a station. */
    private boolean horizontalUnderWater(int height) {
        if (networks.railChunk(chunkX - 1, chunkZ)
            .type()
            .isStation()
            || networks.railChunk(chunkX + 1, chunkZ)
                .type()
                .isStation()) {
            return false;
        }
        return underWater(height);
    }

    /** All six test points liquid: still or flowing water or lava, or the profile's liquid. */
    private boolean underWater(int height) {
        return liquidAt(3, height + 2, 3) && liquidAt(12, height + 2, 3)
            && liquidAt(3, height + 2, 12)
            && liquidAt(12, height + 2, 12)
            && liquidAt(3, height + 4, 7)
            && liquidAt(12, height + 4, 8);
    }

    private boolean liquidAt(int x, int y, int z) {
        BlockRef block = target.get(x, y, z);
        return block.equals(liquid) || context.isLiquid(block);
    }

    private BlockRef railMain(RailChunk rail) {
        Character c = CityPalettes.setting(cityStyle, CityStyle.Setting.RAIL_MAIN);
        BlockRef block = c == null ? null : CityPalettes.blockOf(palette, c);
        if (block == null) {
            // Avoids HWY-12: upstream threw here.
            context.log()
                .warn(
                    "city style " + plan.cityStyle()
                        + " has no rail main block in its palette; "
                        + rail.type()
                        + " rails keep every track");
        }
        return block;
    }

    /**
     * The tunnel from a horizontal line to a rail dungeon in the chunk north or south of it: 4 deep from the north
     * edge, 5 from the south, where {@code rails_horizontal}'s interior starts (HWY-13 is not a bug).
     */
    private void dungeonTunnels(int height, BlockRef rail) {
        if (hasDungeon(planner, chunkX, chunkZ - 1)) {
            tunnel(height, rail, false);
        }
        if (hasDungeon(planner, chunkX, chunkZ + 1)) {
            tunnel(height, rail, true);
        }
    }

    private void tunnel(int height, BlockRef rail, boolean south) {
        int floorDepth = south ? SOUTH_TUNNEL : NORTH_TUNNEL;
        for (int d = 0; d < floorDepth; d++) {
            int z = south ? 15 - d : d;
            for (int x = 6; x <= 7; x++) {
                target.set(x, height + 1, z, rail);
                target.set(x, height + 2, z, BlockRef.AIR);
                target.set(x, height + 3, z, BlockRef.AIR);
            }
        }
        for (int d = 0; d < floorDepth - 1; d++) {
            int z = south ? 15 - d : d;
            for (int y = height + 2; y <= height + 4; y++) {
                target.set(5, y, z, rail);
                target.set(8, y, z, rail);
            }
            target.set(6, height + 4, z, rail);
            target.set(7, height + 4, z, rail);
        }
    }

    /**
     * Upstream's track reduction: with one track the north and south rows are filled with the rail main block, with
     * two the middle row; at a station or line at the part's floor, on a slope wherever a rail is.
     */
    private void reduceTracks(RailChunk rail, Part part, Transform transform, int height, BlockRef block) {
        int[] rows = rail.tracks() == 1 ? new int[] { TRACK_NORTH, TRACK_SOUTH } : new int[] { TRACK_MIDDLE };
        switch (rail.type()) {
            case STATION_SURFACE:
            case STATION_UNDERGROUND:
            case STATION_EXTENSION_SURFACE:
            case STATION_EXTENSION_UNDERGROUND:
            case HORIZONTAL:
                for (int row : rows) {
                    for (int px = 0; px < 16; px++) {
                        target.set(transform.rotateX(px, row), height + 1, transform.rotateZ(px, row), block);
                    }
                }
                break;
            case GOING_DOWN_TWO_FROM_SURFACE:
            case GOING_DOWN_ONE_FROM_SURFACE:
            case GOING_DOWN_FURTHER:
                if (target.dryRun()) {
                    break;
                }
                for (int row : rows) {
                    for (int px = 0; px < 16; px++) {
                        int x = transform.rotateX(px, row);
                        int z = transform.rotateZ(px, row);
                        for (int y = height + 1; y < height + part.height(); y++) {
                            if (isRail(target.get(x, y, z))) {
                                target.set(x, y, z, block);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private static boolean isRail(BlockRef block) {
        return "minecraft:rail".equals(block.name()) || "minecraft:golden_rail".equals(block.name());
    }

    /** The staircase from an underground station up to the city: a staircase part per level, then its top. */
    private void staircase(RailChunk rail) {
        Part stairs = railPart("stationstaircase");
        if (stairs == null) {
            return;
        }
        for (int level = rail.level() + 1; level < plan.cityLevel(); level++) {
            writer.place(
                placement(
                    stairs,
                    Transform.ROTATE_NONE,
                    context.settings()
                        .cityGround(level),
                    HardAir.AIR));
        }
        Part surface = railPart("stationstaircasesurface");
        if (surface != null) {
            writer.place(
                placement(
                    surface,
                    Transform.ROTATE_NONE,
                    context.settings()
                        .cityGround(plan.cityLevel()),
                    HardAir.AIR));
        }
    }

    // ================================================================== rail dungeons

    /**
     * Returns whether a chunk holds a rail dungeon: the layout drew one, and the chunk is no landmark core or margin,
     * where the city builds nothing (0006 §1, E7).
     *
     * @param planner the layout planner
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @return whether a dungeon stands there
     */
    static boolean hasDungeon(LayoutPlanner planner, int chunkX, int chunkZ) {
        return planner.plan(chunkX, chunkZ)
            .railDungeonPart() != null
            && !planner.zone(chunkX, chunkZ)
                .zone()
                .forcesNonCity();
    }

    /**
     * Returns the lowest block Y of a chunk's network that keeps RWG's decoration out above it: the deck of a highway
     * piece that is no tunnel, or the floor of a railway part that reaches the surface; {@link Integer#MAX_VALUE} when
     * nothing does. A tunnel or an underground line leaves the ground above it to RWG.
     *
     * @param context       the world's city writing context
     * @param chunkX        chunk X
     * @param chunkZ        chunk Z
     * @param naturalHeight the oracle's height of the chunk
     * @return block Y, or {@link Integer#MAX_VALUE}
     */
    public static int footprintBottom(CityWriteContext context, int chunkX, int chunkZ, int naturalHeight) {
        LayoutPlanner planner = context.planner();
        Networks networks = planner.networks();
        if (networks == null) {
            return Integer.MAX_VALUE;
        }
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        int bottom = Integer.MAX_VALUE;
        HighwayInfo info = networks.highway(chunkX, chunkZ);
        for (HighwayInfo.Piece piece : info.pieces(chunkX, chunkZ)) {
            if (!plan.isTunnel(piece.topLevel(), naturalHeight)) {
                bottom = Math.min(
                    bottom,
                    context.settings()
                        .cityGround(piece.level()));
            }
        }
        RailChunk rail = networks.railChunk(chunkX, chunkZ);
        if (rail.isPresent() && rail.type()
            .isSurface() && rail.level() + 2 >= plan.cityLevel()) {
            bottom = Math.min(
                bottom,
                context.settings()
                    .cityGround(rail.level()));
        }
        return bottom;
    }

    /**
     * Returns the horizontal line a chunk's rail dungeon stands beside, north first, or {@code null} when the chunk has
     * no dungeon or no such line.
     */
    static RailChunk dungeonRail(LayoutPlanner planner, int chunkX, int chunkZ) {
        if (!hasDungeon(planner, chunkX, chunkZ)) {
            return null;
        }
        RailChunk north = planner.networks()
            .railChunk(chunkX, chunkZ - 1);
        if (north.type() == RailChunkType.HORIZONTAL) {
            return north;
        }
        RailChunk south = planner.networks()
            .railChunk(chunkX, chunkZ + 1);
        return south.type() == RailChunkType.HORIZONTAL ? south : null;
    }

    private void writeRailDungeon() {
        RailChunk rail = dungeonRail(planner, chunkX, chunkZ);
        if (rail == null) {
            return;
        }
        Part part = context.assets()
            .part(CityPalettes.id(plan.railDungeonPart()));
        if (part == null) {
            context.log()
                .warn("rail dungeon part " + plan.railDungeonPart() + " is not loaded");
            return;
        }
        writer.place(
            placement(
                part,
                Transform.ROTATE_NONE,
                context.settings()
                    .cityGround(rail.level()),
                HardAir.AIR));
    }

    private PartWriter.Placement placement(Part part, Transform transform, int oy, HardAir hardAir) {
        return new PartWriter.Placement(
            part,
            context.palettes()
                .forPart(chunkPalette, part),
            transform,
            0,
            oy,
            0,
            hardAir,
            null);
    }
}
