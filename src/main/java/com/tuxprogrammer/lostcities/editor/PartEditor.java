package com.tuxprogrammer.lostcities.editor;

import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ForcedBuilding;
import com.tuxprogrammer.lostcities.worldgen.city.CityPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.CityWriteContext;
import com.tuxprogrammer.lostcities.worldgen.city.PaletteSequence;
import com.tuxprogrammer.lostcities.worldgen.overlay.BlockRefs;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * The part editor's and {@code createbuilding}'s work in a live world: placing parts with a chunk's palette, reading
 * them back into a session's reverse palette, and exporting them (docs/original-spec/01-core/part-editor.md;
 * docs/new-spec/01-core/index.md, Part editor). The palette of a chunk is the one generation writes it with: the
 * layout's palette choices, the building's palette and the part's own (upstream's combined palette).
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class PartEditor {

    /**
     * Upstream's {@code UPDATE_ALL}: neighbors and clients are told, as for {@code createpart} and {@code editpart}.
     */
    public static final int UPDATE_ALL = 3;
    /** Upstream's {@code UPDATE_CLIENTS}: no neighbor updates, as for {@code createbuilding}. */
    public static final int UPDATE_CLIENTS = 2;

    private final OverlayWorld overlay;
    private final WorldServer world;
    private final CityWriteContext context;
    private final BlockRefs refs;

    /**
     * Creates the editor of a world that has real cities.
     *
     * @param overlay the world's overlay; {@link OverlayWorld#cities()} must not be {@code null}
     */
    public PartEditor(OverlayWorld overlay) {
        this.overlay = overlay;
        this.world = overlay.world();
        this.context = overlay.cities()
            .context();
        this.refs = overlay.refs();
    }

    /**
     * Looks up a part by any name form: {@code name} or {@code namespace:name} (one canonical form, ASSET-33).
     *
     * @param name the name
     * @return the part, or {@code null}
     */
    public Part part(String name) {
        AssetId id = id(name);
        return id == null ? null
            : context.assets()
                .part(id);
    }

    /**
     * Returns the canonical id of a name, or {@code null} when it is malformed.
     *
     * @param name the name as typed
     * @return the id
     */
    public static AssetId id(String name) {
        try {
            return AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the palette a part is written with in a chunk.
     *
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param building the building whose palette joins in, or {@code null} for the chunk's own
     * @param part     the part
     * @return the palette
     */
    public CompiledPalette palette(int chunkX, int chunkZ, String building, Part part) {
        ChunkPlan plan = context.planner()
            .plan(chunkX, chunkZ);
        CityPalettes.ChunkPalette chunk = context.palettes()
            .chunk(plan.palettes(), building != null ? building : plan.building());
        return context.palettes()
            .forPart(chunk, part);
    }

    /**
     * Starts a player's session on a part, placing it first when asked (upstream's {@code Editor.startEditing}).
     *
     * @param player    the player's UUID
     * @param chunkX    the part's chunk X
     * @param chunkZ    the part's chunk Z
     * @param part      the part
     * @param placement where it stands: its first slice's Y, its transform and offset
     * @param clear     {@code true} to place the part's blocks first ({@code createpart}, {@code editpart}),
     *                  {@code false} to keep the world's ({@code resumeedit})
     * @return the session
     */
    public EditSession start(UUID player, int chunkX, int chunkZ, Part part, PartRecord placement, boolean clear) {
        CompiledPalette palette = palette(chunkX, chunkZ, null, part);
        load(chunkX, chunkZ);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        if (clear) {
            PartCells.place(
                part,
                palette,
                new PaletteSequence(context.worldSeed(), chunkX, chunkZ),
                placement.transform(),
                placement.offsetX(),
                placement.offsetZ(),
                placement.y(),
                (x, y, z, block) -> set(baseX + x, y, baseZ + z, block, UPDATE_ALL));
        }
        Map<BlockRef, Character> reverse = PartCells.reversePalette(
            part,
            palette,
            placement.transform(),
            placement.offsetX(),
            placement.offsetZ(),
            placement.y(),
            (x, y, z) -> get(baseX + x, y, baseZ + z));
        EditSession session = new EditSession(chunkX, chunkZ, placement, reverse);
        overlay.editSessions()
            .start(player, session);
        return session;
    }

    /**
     * Exports a session's part as it now stands in the world.
     *
     * @param session the session
     * @return the export, or {@code null} when the part is no longer loaded
     */
    public PartExport.Result export(EditSession session) {
        Part part = context.assets()
            .part(session.part());
        if (part == null) {
            return null;
        }
        CompiledPalette palette = palette(session.chunkX(), session.chunkZ(), null, part);
        load(session.chunkX(), session.chunkZ());
        int baseX = session.chunkX() << 4;
        int baseZ = session.chunkZ() << 4;
        PartRecord placement = session.placement();
        return PartExport.export(
            part.xsize(),
            part.zsize(),
            part.height(),
            session.reverse(),
            palette.characters(),
            refPalette(part),
            PartCells.grid(
                placement.transform(),
                placement.offsetX(),
                placement.offsetZ(),
                placement.y(),
                (x, y, z) -> get(baseX + x, y, baseZ + z)));
    }

    /**
     * Returns the reference palette a part names, written back as {@code refpalette}; {@code null} for an inline
     * palette or none.
     */
    static String refPalette(Part part) {
        Palette palette = part.palette();
        if (palette == null || palette.id()
            .path()
            .endsWith("/__local__")) {
            return null;
        }
        return palette.id()
            .toString();
    }

    /**
     * Builds a forced building into a chunk (upstream's {@code createbuilding}): clears the chunk from the lowest
     * cellar's Y to the top of the world, then writes each level's part and overlay part, 6 blocks apart, with the
     * chunk's palette and the building's.
     *
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param building the levels to write
     * @param bottom   block Y of the lowest cellar
     * @return how many levels were written; a level whose part is not loaded is skipped
     */
    public int build(int chunkX, int chunkZ, ForcedBuilding building, int bottom) {
        load(chunkX, chunkZ);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int y = bottom; y <= WorldLimits.MAX_Y; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    set(baseX + x, y, baseZ + z, BlockRef.AIR, UPDATE_CLIENTS);
                }
            }
        }
        PaletteSequence sequence = new PaletteSequence(context.worldSeed(), chunkX, chunkZ);
        int height = bottom;
        int written = 0;
        for (int floor = -building.cellars(); floor <= building.floors(); floor++) {
            for (String name : new String[] { building.part(floor), building.part2(floor) }) {
                Part part = name == null ? null : part(name);
                if (part == null) {
                    continue;
                }
                CompiledPalette palette = palette(chunkX, chunkZ, building.building(), part);
                PartCells.place(
                    part,
                    palette,
                    sequence,
                    com.tuxprogrammer.lostcities.asset.transform.Transform.ROTATE_NONE,
                    0,
                    0,
                    height,
                    (x, y, z, block) -> {
                        if (!block.isAir()) {
                            set(baseX + x, y, baseZ + z, block, UPDATE_CLIENTS);
                        }
                    });
                written++;
            }
            height += ChunkPlan.FLOOR_HEIGHT;
        }
        return written;
    }

    /**
     * Loads, or generates, a chunk before it is read or written: a provide request for an unloaded chunk may answer an
     * empty chunk in the pack, whose writes are lost.
     */
    private void load(int chunkX, int chunkZ) {
        world.theChunkProviderServer.loadChunk(chunkX, chunkZ);
    }

    private void set(int x, int y, int z, BlockRef block, int flags) {
        if (y < WorldLimits.MIN_Y || y > WorldLimits.MAX_Y) {
            return;
        }
        Block target = refs.block(block);
        world.setBlock(x, y, z, target, block.meta(), flags);
    }

    private BlockRef get(int x, int y, int z) {
        if (y < WorldLimits.MIN_Y || y > WorldLimits.MAX_Y) {
            return BlockRef.AIR;
        }
        return refs.ref(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }
}
