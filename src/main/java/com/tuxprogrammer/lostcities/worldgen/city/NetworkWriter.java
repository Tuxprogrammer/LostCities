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
import com.tuxprogrammer.lostcities.layout.link.LinkGeometry;
import com.tuxprogrammer.lostcities.layout.link.LinkRailGeometry;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Writes one chunk's highways, railways and rail dungeon from the layout's networks (upstream's
 * {@code Highways.generateHighways}, {@code Railways.generateRailways} and {@code generateRailwayDungeons}). The
 * terrain phase runs it last in the chunk, and the job dry run runs it too, so stations' and dungeons' chests and
 * spawners are population jobs (0005 §2, §3).
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
 * <li>Ramps and plazas (design record 0013, alpha review AR-10): a ramp is the straight piece written column by column
 * along its axis a half block higher every few columns, one level across the chunk, with a bottom half slab wherever a
 * column stands a half block up, so the climb never needs a jump; a run's end is a crossing piece at the ground's
 * level, the plaza a highway ends on.</li>
 * <li>Railways under landmark cores (D30, D31): slopes along a north-south line are upstream's descent parts turned to
 * run north-south; a line or station in a core gets no clearing to the surface and no staircase.</li>
 * <li>The water test accepts still and flowing water and lava, separate blocks in 1.7.10.</li>
 * <li>Railing openings: where a highway-side scattered building (a gas station, a restaurant) faces a flat piece at
 * its deck's height, the iron bars along that edge are taken out, as upstream's {@code clearRailing}; the scattered
 * planner (07-scattered) decides where.</li>
 * </ul>
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/highway-parts.md, railway-parts.md;
 * docs/new-spec/06-highways-railways/.
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
    static final int RAMP_HALF_STEPS = 2 * ChunkPlan.LEVEL_HEIGHT;
    /** The half slab on a ramp's half steps: smooth stone, bottom half. */
    static final BlockRef RAMP_SLAB = BlockRef.of("minecraft:stone_slab", 0);
    /** Powers a link line's mid-arm powered rail (0015 §3). */
    static final BlockRef REDSTONE_BLOCK = BlockRef.of("minecraft:redstone_block", 0);
    /** The rows of a three-track part's tracks, part-local Z. */
    static final int TRACK_SOUTH = 9;
    static final int TRACK_MIDDLE = 7;
    static final int TRACK_NORTH = 5;
    /**
     * Blocks a tunnel, rail or rail-dungeon part reaches above the city ground of its top level: one level, as the
     * bundled parts are (a two-level slope is 18 high from its lower level).
     */
    static final int PART_LEVEL_HEIGHT = 6;

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
    private com.tuxprogrammer.lostcities.layout.ScatteredPlanner scattered;

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
        this.cityStyle = context.cityStyle(plan.cityStyle());
        com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks style = context.styleBlocks(plan, chunkPalette);
        this.liquid = style.liquid();
        this.writer = new PartWriter(target, sequence, context.rules(plan, style), context.log());
        PartRecorder recorder = context.recorder();
        if (!target.dryRun() && recorder != PartRecorder.NONE) {
            // Part editor (D13): the network's parts are recorded after the city's own; a chunk the city writer
            // did not start (no city, no bridge) starts its list here.
            boolean started = plan.isCity()
                || planner.bridgePart(chunkX, chunkZ, com.tuxprogrammer.lostcities.layout.street.Orientation.X) != null
                || planner.bridgePart(chunkX, chunkZ, com.tuxprogrammer.lostcities.layout.street.Orientation.Z) != null;
            if (!started) {
                recorder.beginChunk(chunkX, chunkZ);
            }
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

    /**
     * Sets the world's scattered planner, which says where a highway-side building opens a highway's railing.
     *
     * @param planner the scattered planner, or {@code null} for none
     * @return this writer
     */
    public NetworkWriter railings(com.tuxprogrammer.lostcities.layout.ScatteredPlanner planner) {
        this.scattered = planner;
        return this;
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
            if (!piece.coded()) {
                highwayPiece(piece);
            }
        }
        if (info.linked()) {
            linkColumns();
        }
    }

    /**
     * Writes the link pieces made in code (0015 §3): every column of this chunk that a diagonal, a transition or a
     * plaza's corner opening in this chunk or one of its eight neighbors covers gets the straight part's cross-section
     * at that column's deck height, with the ramp slab, the clearing above and supports as for a straight piece. Reads
     * only the planner; writes only this chunk, so no cascade. The part is the slot's first, so neighbors that write
     * the
     * same strip agree.
     */
    private void linkColumns() {
        int[] xs = new int[16];
        int[] zs = new int[16];
        HighwayInfo.Piece[] pieces = new HighwayInfo.Piece[16];
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (HighwayInfo.Piece piece : networks.highway(chunkX + dx, chunkZ + dz)
                    .pieces(chunkX + dx, chunkZ + dz)) {
                    if (!LinkGeometry.coded(piece)) {
                        continue;
                    }
                    if (count == pieces.length) {
                        xs = java.util.Arrays.copyOf(xs, count * 2);
                        zs = java.util.Arrays.copyOf(zs, count * 2);
                        pieces = java.util.Arrays.copyOf(pieces, count * 2);
                    }
                    xs[count] = chunkX + dx;
                    zs[count] = chunkZ + dz;
                    pieces[count++] = piece;
                }
            }
        }
        if (count == 0) {
            return;
        }
        boolean supports = networks.highways()
            .settings()
            .supports();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                LinkGeometry.Column c = LinkGeometry.highest(xs, zs, pieces, count, chunkX * 16 + x, chunkZ * 16 + z);
                if (c != null) {
                    linkColumn(c, x, z, supports);
                }
            }
        }
    }

    private void linkColumn(LinkGeometry.Column c, int x, int z, boolean supports) {
        int deckY = context.settings()
            .cityGround(c.level());
        int shift = c.half() / 2;
        boolean tunnel = plan.isTunnel(c.level() + (c.half() > 0 ? 1 : 0), naturalHeight);
        List<Part> parts = worldStyle.parts("highways", tunnel ? "tunnel" : "bridge");
        if (parts == null || parts.isEmpty()) {
            return;
        }
        Part part = parts.get(0);
        int px = Math.floorMod((int) Math.floor(c.along()), part.xsize());
        int pz = Math.min(part.zsize() - 1, c.row());
        PartWriter.Placement placement = placement(part, Transform.ROTATE_NONE, deckY, HardAir.WATERLEVEL);
        writer.placeColumn(placement, px, pz, x, z, shift);
        if (target.dryRun()) {
            return;
        }
        int y = deckY + shift;
        if ((c.half() & 1) == 1 && y + 1 <= WorldLimits.MAX_Y
            && !context.isEmpty(target.get(x, y, z))
            && target.get(x, y + 1, z)
                .isAir()) {
            target.set(x, y + 1, z, RAMP_SLAB);
        }
        if (!tunnel) {
            clearColumn(x, z, deckY + part.height() + shift);
        }
        if (supports && (pz == 0 || pz == part.zsize() - 1) && Math.floorMod((int) Math.floor(c.along()), 8) == 0) {
            Character support = part.metaCharacter(Part.MetaKey.SUPPORT);
            if (support == null) {
                support = worldStyle.highwaySupport();
            }
            BlockRef block = support == null ? null
                : supportBlock(
                    context,
                    context.palettes()
                        .forPart(chunkPalette, part),
                    support,
                    part);
            if (block != null) {
                column(Transform.ROTATE_NONE, y, x, z, block, null);
            }
        }
    }

    private void clearColumn(int x, int z, int from) {
        boolean drowned = context.settings()
            .waterLevel()
            > context.settings()
                .groundLevel();
        int waterLevel = context.settings()
            .waterLevel();
        int to = Math.min(WorldLimits.MAX_Y, from + CLEAR_HEIGHT - 1);
        for (int y = from; y <= to; y++) {
            BlockRef here = target.get(x, y, z);
            if (here.isAir() || context.isFoliage(here)) {
                continue;
            }
            target.set(x, y, z, drowned && y <= waterLevel ? liquid : BlockRef.AIR);
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
        if (scattered != null && halfSteps == null && !target.dryRun()) {
            openRailings(deckY, part.height());
        }
    }

    /**
     * Upstream's {@code clearRailing}: toward each side where a highway-side scattered building faces this piece at its
     * deck's height, the iron bars along that chunk edge, from the deck up through the part, are taken out.
     */
    private void openRailings(int deckY, int partHeight) {
        for (com.tuxprogrammer.lostcities.layout.Direction side : com.tuxprogrammer.lostcities.layout.Direction
            .values()) {
            if (!scattered.opensRailing(chunkX, chunkZ, side, deckY)) {
                continue;
            }
            for (int i = 0; i < 16; i++) {
                int x = side == com.tuxprogrammer.lostcities.layout.Direction.XMIN ? 0
                    : side == com.tuxprogrammer.lostcities.layout.Direction.XMAX ? 15 : i;
                int z = side == com.tuxprogrammer.lostcities.layout.Direction.ZMIN ? 0
                    : side == com.tuxprogrammer.lostcities.layout.Direction.ZMAX ? 15 : i;
                for (int y = deckY + 1; y < deckY + partHeight && y <= WorldLimits.MAX_Y; y++) {
                    if ("minecraft:iron_bars".equals(
                        target.get(x, y, z)
                            .name())) {
                        target.set(x, y, z, BlockRef.AIR);
                    }
                }
            }
        }
    }

    /**
     * Upstream's open-road test: in a city whose two neighbors across the road are city at or above the level. A
     * junction tests its east and south neighbors, an east-west piece its north and south ones, and a north-south
     * piece its west and east ones (avoids HWY-02: upstream tested the east neighbor twice).
     */
    private boolean openRoad(HighwayInfo.Piece piece, int level) {
        return openRoad(piece, level, plan.isCity(), (dx, dz) -> {
            ChunkPlan neighbor = planner.plan(chunkX + dx, chunkZ + dz);
            return neighbor.isCity() ? neighbor.cityLevel() : -1;
        });
    }

    /** A neighbor's city level, or −1 when it is not city. */
    @FunctionalInterface
    interface NeighborLevel {

        /**
         * @param dx chunks east
         * @param dz chunks south
         * @return the neighbor's city level, or −1 when it is not city
         */
        int level(int dx, int dz);
    }

    /**
     * Returns whether a highway piece is an open road: the chunk is city, and both neighbors across the road are city
     * at or above the piece's level. A junction tests east and south, an east-west piece north and south, a
     * north-south piece west and east (avoids HWY-02).
     *
     * @param piece     the piece
     * @param level     its level
     * @param city      whether the chunk is city
     * @param neighbors the neighbors' city levels
     * @return whether it is open
     */
    static boolean openRoad(HighwayInfo.Piece piece, int level, boolean city, NeighborLevel neighbors) {
        if (!city) {
            return false;
        }
        int a;
        int b;
        if (piece.junction()) {
            a = neighbors.level(1, 0);
            b = neighbors.level(0, 1);
        } else if (piece.axis() == Axis.X) {
            a = neighbors.level(0, -1);
            b = neighbors.level(0, 1);
        } else {
            a = neighbors.level(-1, 0);
            b = neighbors.level(1, 0);
        }
        return a >= 0 && b >= 0 && level <= a && level <= b;
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
     * low end's column, {@link #RAMP_HALF_STEPS} at the high end's, at most one half block between neighbors. A
     * north-south ramp is the part turned 90°, which puts part-local X on chunk-local Z.
     *
     * @param piece the ramp
     * @return 16 heights in half blocks, indexed by the chunk-local coordinate along its axis
     */
    public static int[] rampSteps(HighwayInfo.Piece piece) {
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

    /** The top of a ramp's whole-block columns under {@code rampSurface}: the half slab's material, a full block. */
    static final BlockRef RAMP_SURFACE = BlockRef.of("minecraft:double_stone_slab", 0);

    /**
     * A bottom half slab on the deck of every column a half block up. Under {@code rampSurface} (KI-2), the deck's top
     * block of every other raised column becomes {@link #RAMP_SURFACE}, so the whole ramp shows the slab's material
     * instead of stripes of slab and road.
     */
    private void rampSlabs(Part part, Transform transform, int deckY, int[] halfSteps) {
        boolean surface = context.settings()
            .rampSurface();
        for (int px = 0; px < 16; px++) {
            if ((halfSteps[px] & 1) == 0) {
                if (surface && halfSteps[px] > 0) {
                    rampSurface(part, transform, deckY + halfSteps[px] / 2, px);
                }
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

    /** Tops one whole-block ramp column's deck with {@link #RAMP_SURFACE} where a half slab would sit on it. */
    private void rampSurface(Part part, Transform transform, int y, int px) {
        if (y + 1 > WorldLimits.MAX_Y) {
            return;
        }
        for (int pz = 0; pz < part.zsize(); pz++) {
            int x = transform.rotateX(px, pz);
            int z = transform.rotateZ(px, pz);
            if (x < 0 || x > 15 || z < 0 || z > 15) {
                continue;
            }
            if (!context.isEmpty(target.get(x, y, z)) && target.get(x, y + 1, z)
                .isAir()) {
                target.set(x, y, z, RAMP_SURFACE);
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
        BlockRef block = supportBlock(
            context,
            context.palettes()
                .forPart(chunkPalette, part),
            support,
            part);
        if (block == null) {
            return;
        }
        column(transform, deckY, 0, 15, block, halfSteps);
        column(transform, deckY, 0, 0, block, halfSteps);
        if (piece.shape() == HighwayInfo.Shape.BEND || piece.shape() == HighwayInfo.Shape.T_JUNCTION) {
            column(transform, deckY, 15, 0, block, halfSteps);
        }
    }

    /**
     * Returns a highway part's support block, or {@code null} with one log line when its palette lacks the character
     * (avoids HWY-12: upstream threw a {@code RuntimeException} mid-generation).
     *
     * @param context the world's city writing context
     * @param palette the part's palette
     * @param support the support character
     * @param part    the part, for the log line
     * @return the block, or {@code null}
     */
    static BlockRef supportBlock(CityWriteContext context, CompiledPalette palette, char support, Part part) {
        BlockRef block = CityPalettes.blockOf(palette, support);
        if (block == null) {
            context.log()
                .warn(
                    "highway part " + part.id()
                        + " has support '"
                        + support
                        + "', which its palette does not define; no supports");
        }
        return block;
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
        if (rail.coded()) {
            linkRail(rail, height);
            return;
        }
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
                if (rail.buffer() != null) {
                    // railwayCoreEnds: upstream's end part, its buffer toward the core chunk left out (0013 §5).
                    slot = "railshorizontalend";
                    transform = rail.buffer() == RailChunk.Direction.EAST ? Transform.ROTATE_NONE : Transform.MIRROR_X;
                    break;
                }
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
        if (rail.type() == RailChunkType.VERTICAL && rail.buffer() != null && !target.dryRun()) {
            verticalBuffer(rail.buffer(), height, railMain);
        }
        if (staircase) {
            staircase(rail);
        }
    }

    /** The first row past a north-south line's buffer stop, counted from the chunk edge it faces. */
    static final int BUFFER_ROWS = 4;

    /**
     * {@code railwayCoreEnds} on a north-south line (0013 §5): upstream has no north-south end part, so the track
     * stops {@link #BUFFER_ROWS} blocks from the edge facing the core chunk left out, on a block of the rail's own
     * material; the rails past it are taken up.
     *
     * @param side     the side the buffer faces: west for north, east for south
     * @param height   the part's floor Y
     * @param railMain the rail's support block, the stop
     */
    private void verticalBuffer(RailChunk.Direction side, int height, BlockRef railMain) {
        boolean south = side == RailChunk.Direction.EAST;
        int stop = south ? 15 - BUFFER_ROWS : BUFFER_ROWS;
        for (int x = 0; x < 16; x++) {
            for (int y = height + 1; y <= height + 2; y++) {
                if (isRail(target.get(x, y, stop))) {
                    target.set(x, y, stop, railMain);
                }
                for (int i = 0; i < BUFFER_ROWS; i++) {
                    int z = south ? 15 - i : i;
                    if (isRail(target.get(x, y, z))) {
                        target.set(x, y, z, BlockRef.AIR);
                    }
                }
            }
        }
    }

    /**
     * Writes a link-network line in code (0015 §3): every tunnel column copies the slot's first
     * {@code railshorizontal} part's cross-section (its interior row 4 inside, its wall row 3 for walls), turned to
     * run along Z on north and south arms; then the track, one vanilla rail per column shaped to join its neighbors,
     * with a powered rail over a redstone block mid-arm, and at a T a lever that switches the center curve. Writes only
     * this chunk.
     */
    private void linkRail(RailChunk rail, int height) {
        List<Part> parts = worldStyle.parts("railways", "railshorizontal");
        if (parts == null || parts.isEmpty()) {
            return;
        }
        Part part = parts.get(0);
        PartWriter.Placement placement = placement(part, Transform.ROTATE_NONE, height, HardAir.AIR);
        int sides = rail.openings();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int role = LinkRailGeometry.role(sides, x, z);
                if (role == LinkRailGeometry.NONE) {
                    continue;
                }
                int along = LinkRailGeometry.alongZ(x, z) ? z : x;
                writer.placeColumn(placement, along, role == LinkRailGeometry.WALL ? 3 : 4, x, z, 0);
            }
        }
        if (target.dryRun()) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int shape = LinkRailGeometry.shape(sides, x, z);
                if (shape < 0) {
                    continue;
                }
                if (LinkRailGeometry.powered(sides, x, z)) {
                    target.set(x, height, z, REDSTONE_BLOCK);
                    target.set(x, height + 1, z, BlockRef.of("minecraft:golden_rail", shape | 8));
                } else {
                    target.set(x, height + 1, z, BlockRef.of("minecraft:rail", shape));
                }
            }
        }
        int[] lever = LinkRailGeometry.lever(sides);
        if (lever != null) {
            // A T's switch (D356): a floor lever beside the curve, as a player builds one; meta 5 stands on the
            // floor.
            target.set(lever[0], height + 1, lever[1], BlockRef.of("minecraft:lever", 5));
        }
        BlockRef railMain = railMain(rail);
        if (railMain != null && rail.type() == RailChunkType.HORIZONTAL) {
            dungeonTunnels(height, railMain);
        }
    }

    /**
     * Avoids CITY-19: under {@code block_railway} avoidance a building whose lowest floor reaches below the rail
     * part's top leaves this chunk's rail out, decided from this chunk's plan alone.
     */
    private boolean blockedByBuilding(RailChunk rail) {
        return blockedByBuilding(
            worldStyle.railwayAvoidance(),
            plan.hasBuilding(),
            plan.cityLevel(),
            plan.cellars(),
            rail.topLevel(),
            worldStyle.railPartHeight6());
    }

    /**
     * Returns whether a building leaves a chunk's rail out: under {@code block_railway}, when the building's lowest
     * floor reaches below the rail part's top. Decided from the chunk alone, so no neighbor's rail changes (avoids
     * CITY-19, HWY-07).
     *
     * @param avoidance   the world style's {@code railwayavoidance}
     * @param building    whether the chunk has a building
     * @param cityLevel   its city level
     * @param cellars     its cellars
     * @param railTop     the highest level the rail reaches
     * @param partHeight6 the world style's {@code railpartheight6}
     * @return whether the rail is left out
     */
    static boolean blockedByBuilding(WorldStyle.RailwayAvoidance avoidance, boolean building, int cityLevel,
        int cellars, int railTop, int partHeight6) {
        if (avoidance != WorldStyle.RailwayAvoidance.BLOCK_RAILWAY || !building) {
            return false;
        }
        return cityLevel - cellars < railTop + partHeight6;
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
        return railMainBlock(
            context,
            palette,
            CityPalettes.setting(cityStyle, CityStyle.Setting.RAIL_MAIN),
            plan.cityStyle(),
            rail.type());
    }

    /**
     * Returns a city style's rail main block, or {@code null} with one log line when it names none or its palette lacks
     * it (avoids HWY-12: upstream threw a {@code RuntimeException} mid-generation). Without it the rails keep every
     * track and a rail dungeon's tunnel is not dug.
     *
     * @param context   the world's city writing context
     * @param palette   the chunk's palette
     * @param character the city style's {@code railmain} character, or {@code null}
     * @param style     the city style's name, for the log line
     * @param type      the rail type, for the log line
     * @return the block, or {@code null}
     */
    static BlockRef railMainBlock(CityWriteContext context, CompiledPalette palette, Character character, String style,
        RailChunkType type) {
        BlockRef block = character == null ? null : CityPalettes.blockOf(palette, character);
        if (block == null) {
            context.log()
                .warn(
                    "city style " + style
                        + " has no rail main block in its palette; "
                        + type
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
        // A neighbor's diagonal deck spills into this chunk's side columns: guard them too (0015 §3), or RWG's trees
        // and other mods' decoration grow up through the deck.
        if (info.spill() && !plan.isTunnel(info.spillHigh(), naturalHeight)) {
            bottom = Math.min(
                bottom,
                context.settings()
                    .cityGround(info.spillLow()));
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
     * Returns the block Y range the write guard keeps RWG's decoration out of in every column of a chunk, beside the
     * footprint: from one block under the floor of a highway tunnel, a railway part or a rail dungeon to the top of
     * the highest of them. The ground above a tunnel or an underground line stays RWG's; only its lakes, which BOP
     * writes with no populate event to veto, and the rest of its decoration stay out of the tunnels (M06, AR-10).
     *
     * @param context       the world's city writing context
     * @param chunkX        chunk X
     * @param chunkZ        chunk Z
     * @param naturalHeight the oracle's height of the chunk
     * @return {bottom, top}, inclusive, or {@code null} for none
     */
    public static int[] guardBand(CityWriteContext context, int chunkX, int chunkZ, int naturalHeight) {
        LayoutPlanner planner = context.planner();
        Networks networks = planner.networks();
        if (networks == null) {
            return null;
        }
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        TerrainSettings settings = context.settings();
        int bottom = Integer.MAX_VALUE;
        int top = Integer.MIN_VALUE;
        HighwayInfo info = networks.highway(chunkX, chunkZ);
        for (HighwayInfo.Piece piece : info.pieces(chunkX, chunkZ)) {
            if (plan.isTunnel(piece.topLevel(), naturalHeight)) {
                bottom = Math.min(bottom, settings.cityGround(piece.level()) - 1);
                top = Math.max(top, settings.cityGround(piece.topLevel()) + PART_LEVEL_HEIGHT);
            }
        }
        // A diagonal deck spilling in underground needs its band here as well (0015 §3).
        if (info.spill() && plan.isTunnel(info.spillHigh(), naturalHeight)) {
            bottom = Math.min(bottom, settings.cityGround(info.spillLow()) - 1);
            top = Math.max(top, settings.cityGround(info.spillHigh()) + PART_LEVEL_HEIGHT);
        }
        RailChunk rail = networks.railChunk(chunkX, chunkZ);
        if (rail.isPresent()) {
            bottom = Math.min(bottom, settings.cityGround(rail.level()) - 1);
            top = Math.max(top, settings.cityGround(rail.topLevel()) + PART_LEVEL_HEIGHT);
        }
        RailChunk dungeon = dungeonRail(planner, chunkX, chunkZ);
        if (dungeon != null) {
            bottom = Math.min(bottom, settings.cityGround(dungeon.level()) - 1);
            top = Math.max(top, settings.cityGround(dungeon.level()) + PART_LEVEL_HEIGHT);
        }
        return bottom > top ? null : new int[] { bottom, top };
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
