package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Scattered.java
// Avoids: SCAT-08, SCAT-13

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.MetaRotation;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Writes one footprint chunk's share of a scattered structure (upstream's {@code generateScatteredBuilding}): the
 * terrain fix on the first floor, then each floor's part and its overlay part, turned with the plan's rotation, with
 * hard air as air. The floor count and the parts were drawn in the plan from its replayed random
 * ({@link ScatteredPlan.Share}), so every chunk of a multibuilding agrees.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids SCAT-13: a floor's overlay part is placed at the floor's own base, as a city building's, not on top of
 * the floor's part;</li>
 * <li>avoids SCAT-08: the {@code repeatslice} support stops at Y {@value #SUPPORT_BOTTOM}, never running off the world;
 * a support character with variants takes its first variant, so the support draws nothing from the palette sequence
 * and the job dry run, which skips the support, draws the parts' blocks exactly as the write does.</li>
 * </ul>
 * The ground under the structure is set by the terrain phase before this runs (design record 0009).
 *
 * <p>
 * The same code runs for the write and for the dry run that lists the population jobs ({@link CityTarget}). One
 * instance per chunk and run. Side: both (plain Java).
 */
public final class ScatteredWriter {

    /** Lowest Y a {@code repeatslice} support reaches (SCAT-08). */
    static final int SUPPORT_BOTTOM = 1;
    /** Blocks above the lowest level that {@code clear} empties: upstream's {@code lowestLevel + 50}. */
    static final int CLEAR_HEIGHT = 50;

    private final CityWriteContext context;
    private final ScatteredPlan plan;
    private final ScatteredPlan.Share share;
    private final CityTarget target;
    private final CityPalettes.ChunkPalette chunkPalette;
    private final PartWriter writer;
    private final BlockRef liquid;
    private final Transform transform;
    private final AssetId buildingId;

    /**
     * Prepares the write of one footprint chunk.
     *
     * @param context  the world's city writing context
     * @param plan     the structure's plan
     * @param share    this chunk's share of it
     * @param target   where the blocks or jobs go
     * @param sequence the chunk's palette sequence
     */
    public ScatteredWriter(CityWriteContext context, ScatteredPlan plan, ScatteredPlan.Share share, CityTarget target,
        PaletteSequence sequence) {
        this.context = context;
        this.plan = plan;
        this.share = share;
        this.target = target;
        ChunkPlan layout = context.planner()
            .plan(share.chunkX(), share.chunkZ());
        // The chunk's palettes with the scattered building's own palette, as a city building's chunk has.
        this.chunkPalette = context.palettes()
            .chunk(layout.palettes(), share.building());
        com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks style = context.styleBlocks(layout);
        this.liquid = style.liquid();
        this.writer = new PartWriter(target, sequence, context.rules(layout, style), context.log());
        this.transform = transform(plan.rotation());
        this.buildingId = CityPalettes.id(share.building());
    }

    /** Returns the part transform of a plan's rotation. */
    public static Transform transform(ScatteredPlan.Rotation rotation) {
        switch (rotation) {
            case ROTATE_90:
                return Transform.ROTATE_90;
            case ROTATE_180:
                return Transform.ROTATE_180;
            case ROTATE_270:
                return Transform.ROTATE_270;
            default:
                return Transform.ROTATE_NONE;
        }
    }

    /** Writes the share: the terrain fix, then each floor bottom up. */
    public void write() {
        int lowest = plan.lowestLevel();
        int height = lowest;
        for (int f = 0; f < share.floors(); f++) {
            Part part = part(share.part(f));
            if (f == 0) {
                terrainFix(lowest, part);
            }
            int base = height;
            if (part != null) {
                height = writer.place(placement(part, base));
            }
            Part overlay = part(share.part2(f));
            if (overlay != null) {
                // Avoids SCAT-13: the overlay starts at its floor's base, as for city buildings.
                writer.place(placement(overlay, base));
            }
        }
    }

    private void terrainFix(int lowest, Part floorPart) {
        ScatteredView.TerrainFix fix = plan.asset()
            .terrainFix();
        if (target.dryRun() || fix == ScatteredView.TerrainFix.NONE) {
            return;
        }
        if (fix == ScatteredView.TerrainFix.CLEAR) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    for (int y = lowest; y < lowest + CLEAR_HEIGHT; y++) {
                        target.set(x, y, z, BlockRef.AIR);
                    }
                }
            }
            return;
        }
        String supportName = plan.asset()
            .supportPart();
        Part support = supportName == null ? floorPart : part(supportName);
        if (support == null) {
            return;
        }
        CompiledPalette palette = context.palettes()
            .forPart(chunkPalette, support);
        for (int x = 0; x < support.xsize(); x++) {
            for (int z = 0; z < support.zsize(); z++) {
                char[] column = support.column(x, z);
                if (column == null || column[0] == ' ' || !palette.isDefined(column[0])) {
                    continue;
                }
                BlockChoice choice = palette.choice(column[0], 0);
                if (choice.isHardAir()) {
                    continue;
                }
                BlockRef block = MetaRotation.rotate(choice.block(palette.flags()), choice.family(), transform);
                if (block.isAir()) {
                    continue;
                }
                int rx = transform.rotateX(x, z);
                int rz = transform.rotateZ(x, z);
                for (int y = lowest - 1; y >= SUPPORT_BOTTOM; y--) {
                    BlockRef here = target.get(rx, y, rz);
                    // Upstream's test: exactly air or the profile's liquid; any other block ends the support.
                    if (!here.isAir() && !here.equals(liquid)) {
                        break;
                    }
                    target.set(rx, y, rz, block);
                }
            }
        }
    }

    private PartWriter.Placement placement(Part part, int oy) {
        return new PartWriter.Placement(
            part,
            context.palettes()
                .forPart(chunkPalette, part),
            transform,
            0,
            oy,
            0,
            HardAir.AIR,
            buildingId);
    }

    private Part part(String name) {
        AssetId id = CityPalettes.id(name);
        if (id == null) {
            return null;
        }
        Part part = context.assets()
            .part(id);
        if (part == null) {
            context.log()
                .warn("part " + name + " of a scattered structure is not loaded; skipped");
        }
        return part;
    }

    /** Returns the lowest block Y the support may reach, for tests. */
    static int supportBottom() {
        return Math.max(SUPPORT_BOTTOM, WorldLimits.MIN_Y + 1);
    }
}
