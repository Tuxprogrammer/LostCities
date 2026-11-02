package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Bridges.java
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Supports.java
// Avoids: CITY-30, CITY-38, HWY-12

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.MetaRotation;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkLayout;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Upstream's {@code Bridges.generateBridges} with {@code Supports}: the bridge span over a non-city chunk, written
 * from the profile's ground level (the hierarchical street mode's bridge level), with block supports or a support part
 * down to the ground where the span continues on both sides, and a support edge where it lands
 * (docs/original-spec/04-city-layout/bridges.md, docs/original-spec/03-terrain/building-and-street-writing.md).
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids CITY-30: a span along Z is the X part transposed, and its blocks' metadata is turned by the same map
 * ({@link Transform#MIRROR_90_X}), so stairs, slabs and rails face along the span;</li>
 * <li>avoids CITY-38: a bridge is skipped when any highway's level lies within the bridge part's height, not only a
 * level-0 highway, so a level-1 highway never cuts a covered bridge's roof;</li>
 * <li>avoids HWY-12: an undefined character or support block is skipped with one log line, never a crash.</li>
 * </ul>
 *
 * <p>
 * Side: both (plain Java).
 */
final class BridgeWriter {

    /** How far below the water level block supports start (upstream's {@code waterLevel − 10}). */
    static final int SUPPORT_DEPTH = 10;

    private BridgeWriter() {}

    /**
     * Writes a chunk's bridge, if it carries one.
     *
     * @return whether a bridge was written
     */
    static boolean write(CityWriteContext context, ChunkLayout layout, ChunkPlan plan, CompiledPalette chunkPalette,
        CityPalettes.ChunkPalette palettes, CityTarget target, PaletteSequence sequence) {
        Orientation orientation = Orientation.X;
        String name = layout.bridgePart(Orientation.X);
        if (name == null) {
            orientation = Orientation.Z;
            name = layout.bridgePart(Orientation.Z);
        }
        if (name == null) {
            return false;
        }
        AssetId id = CityPalettes.id(name);
        Part part = id == null ? null
            : context.assets()
                .part(id);
        if (part == null) {
            return false;
        }
        int level = context.settings()
            .groundLevel();
        if (blocksHighway(plan.highwayXLevel(), level, part.height(), context)
            || blocksHighway(plan.highwayZLevel(), level, part.height(), context)) {
            return false;
        }
        CompiledPalette palette = context.palettes()
            .forPart(palettes, part);
        // Avoids CITY-30: positions and blocks transformed by one map.
        Transform transform = orientation == Orientation.X ? Transform.ROTATE_NONE : Transform.MIRROR_90_X;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int px = orientation == Orientation.X ? x : z;
                int pz = orientation == Orientation.X ? z : x;
                if (px >= part.xsize() || pz >= part.zsize()) {
                    continue;
                }
                for (int l = 0; l < part.height(); l++) {
                    char c = part.get(px, l, pz);
                    int y = level + l;
                    if (c == ' ' || y > WorldLimits.MAX_Y) {
                        continue;
                    }
                    deck(context, palette, part, c, x, y, z, transform, target, sequence);
                }
            }
        }
        supports(context, plan, part, palette, palettes, orientation, transform, level, target);
        return true;
    }

    private static void deck(CityWriteContext context, CompiledPalette palette, Part part, char c, int x, int y,
        int z, Transform transform, CityTarget target, PaletteSequence sequence) {
        if (!palette.isDefined(c)) {
            context.log()
                .warn("bridge part " + part.id() + " uses character '" + c + "', which its palette does not define");
            return;
        }
        BlockChoice choice = palette.isRandom(c) ? palette.choice(c, sequence.next()) : palette.choice(c, 0);
        if (choice.isHardAir()) {
            // Upstream writes the structure void, which keeps whatever is there.
            return;
        }
        BlockRef block = MetaRotation.rotate(choice.block(palette.flags()), choice.family(), transform);
        PaletteEntry info = palette.info(c);
        if (info != null && info.torch()) {
            if (context.settings()
                .generateLighting()) {
                target.set(x, y, z, block);
                target.torch(x, y, z);
            } else {
                target.set(x, y, z, BlockRef.AIR);
            }
            return;
        }
        // Upstream writes every character of a bridge, air included, so the deck's clearance is clear.
        target.set(x, y, z, block);
    }

    /**
     * Returns whether a highway at a city level would cut the bridge part: its deck Y lies within the part's height
     * above the bridge level (avoids CITY-38).
     */
    static boolean blocksHighway(int highwayLevel, int bridgeLevel, int partHeight, CityWriteContext context) {
        if (highwayLevel < 0) {
            return false;
        }
        int deck = context.settings()
            .cityGround(highwayLevel);
        return deck < bridgeLevel + partHeight;
    }

    private static void supports(CityWriteContext context, ChunkPlan plan, Part part, CompiledPalette partPalette,
        CityPalettes.ChunkPalette chunkPalette, Orientation orientation, Transform transform, int level,
        CityTarget target) {
        if (!context.settings()
            .bridgeSupports()) {
            return;
        }
        CityStyle cityStyle = context.assets()
            .cityStyle(CityPalettes.id(plan.cityStyle()));
        WorldStyle worldStyle = context.worldStyle();
        Character support = part.metaCharacter(Part.MetaKey.SUPPORT);
        Part supportPart = null;
        CompiledPalette supportPalette = partPalette;
        if (support == null) {
            Character citySupport = CityPalettes.setting(cityStyle, CityStyle.Setting.BRIDGE_SUPPORT);
            support = citySupport != null ? citySupport : worldStyle == null ? null : worldStyle.bridgeSupport();
            supportPart = cityStyle == null ? null : cityStyle.bridgeSupportPart();
            if (supportPart == null && citySupport == null && worldStyle != null) {
                supportPart = worldStyle.bridgeSupportPart();
            }
            supportPalette = chunkPalette.palette();
        }
        if (support == null && supportPart == null) {
            return;
        }
        BlockRef block = support == null ? null : CityPalettes.blockOf(supportPalette, support);
        if (support != null && block == null) {
            context.log()
                .warn("bridge part " + part.id() + " has support '" + support + "', which its palette does not define");
        }
        int minX = plan.chunkX() - (orientation == Orientation.X ? 1 : 0);
        int minZ = plan.chunkZ() - (orientation == Orientation.Z ? 1 : 0);
        int maxX = plan.chunkX() + (orientation == Orientation.X ? 1 : 0);
        int maxZ = plan.chunkZ() + (orientation == Orientation.Z ? 1 : 0);
        boolean minBridge = context.planner()
            .bridgePart(minX, minZ, orientation) != null;
        boolean maxBridge = context.planner()
            .bridgePart(maxX, maxZ, orientation) != null;
        if (minBridge && maxBridge) {
            if (supportPart != null) {
                if (!target.dryRun()) {
                    supportPart(context, supportPart, chunkPalette, transform, level - 1, target);
                }
            } else if (block != null) {
                int waterLevel = context.settings()
                    .waterLevel();
                for (int y = Math.max(WorldLimits.MIN_Y, waterLevel - SUPPORT_DEPTH); y <= context.settings()
                    .groundLevel(); y++) {
                    target.set(7, y, 7, block);
                    target.set(7, y, 8, block);
                    target.set(8, y, 7, block);
                    target.set(8, y, 8, block);
                }
            }
        }
        if (block == null) {
            return;
        }
        int ground = context.settings()
            .groundLevel();
        if (!minBridge) {
            edge(target, orientation, 0, ground, block);
        }
        if (!maxBridge) {
            edge(target, orientation, 15, ground, block);
        }
    }

    /** The support edge where a span lands: four blocks across the middle of the chunk's side. */
    private static void edge(CityTarget target, Orientation orientation, int fixed, int y, BlockRef block) {
        for (int along = 6; along <= 9; along++) {
            if (orientation == Orientation.X) {
                target.set(fixed, y, along, block);
            } else {
                target.set(along, y, fixed, block);
            }
        }
    }

    /**
     * Upstream's {@code Supports.generatePart}: repeats a support part's columns down from {@code topY}, each column
     * stopping at the first block that is not air or liquid.
     */
    private static void supportPart(CityWriteContext context, Part part, CityPalettes.ChunkPalette chunkPalette,
        Transform transform, int topY, CityTarget target) {
        if (part.height() == 0) {
            return;
        }
        CompiledPalette palette = context.palettes()
            .forPart(chunkPalette, part);
        for (int x = 0; x < part.xsize(); x++) {
            for (int z = 0; z < part.zsize(); z++) {
                char[] column = part.column(x, z);
                if (column == null) {
                    continue;
                }
                int rx = transform.rotateX(x, z);
                int rz = transform.rotateZ(x, z);
                if (rx < 0 || rx > 15 || rz < 0 || rz > 15) {
                    continue;
                }
                int index = column.length - 1;
                for (int y = topY; y >= WorldLimits.MIN_Y && context.isEmpty(target.get(rx, y, rz)); y--) {
                    char c = column[index];
                    BlockRef block = CityPalettes.blockOf(palette, c);
                    if (block != null && !block.isAir()) {
                        BlockChoice choice = palette.choice(c, 0);
                        target.set(rx, y, rz, MetaRotation.rotate(block, choice.family(), transform));
                    }
                    if (--index < 0) {
                        index = column.length - 1;
                    }
                }
            }
        }
    }
}
