package com.tuxprogrammer.lostcities.editor;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/editor/Editor.java

import java.util.LinkedHashMap;
import java.util.Map;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.MetaRotation;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.worldgen.city.PaletteSequence;

/**
 * The cells of a part as the part editor and {@code createbuilding} see them: where each lands in its chunk, which
 * block placing it writes, and the reverse palette read back from the world (docs/original-spec/01-core/part-editor.md,
 * "Starting a session"; docs/original-spec/01-core/commands.md, {@code createbuilding} step 5).
 *
 * <p>
 * Placing is upstream's raw placement, not generation's: every cell is written, a space or a character the palette
 * does not define as air, hard air as air, so the author sees exactly the part. A random character takes the next slot
 * of the chunk's palette sequence, as generation does. A part with a transform and an offset (a rotated street part)
 * lands where generation put it, and its directional blocks turn with it (deviation: upstream recorded neither and
 * re-placed such parts unrotated at the chunk corner).
 *
 * <p>
 * Side: both (plain Java).
 */
public final class PartCells {

    /** Visits one cell of a part. */
    @FunctionalInterface
    public interface Visitor {

        /**
         * Visits a cell.
         *
         * @param partX  the part's local X
         * @param y      the slice
         * @param partZ  the part's local Z
         * @param localX the chunk-local X it lands on
         * @param localZ the chunk-local Z it lands on
         * @param c      the cell's character
         */
        void visit(int partX, int y, int partZ, int localX, int localZ, char c);
    }

    /** Writes or reads one block at a chunk-local position. */
    @FunctionalInterface
    public interface Sink {

        /**
         * Handles a block.
         *
         * @param localX chunk-local X
         * @param y      block Y
         * @param localZ chunk-local Z
         * @param block  the block
         */
        void accept(int localX, int y, int localZ, BlockRef block);
    }

    /** Reads the world's block at a chunk-local position. */
    @FunctionalInterface
    public interface Source {

        /**
         * Returns a block.
         *
         * @param localX chunk-local X
         * @param y      block Y
         * @param localZ chunk-local Z
         * @return the block
         */
        BlockRef get(int localX, int y, int localZ);
    }

    private PartCells() {}

    /**
     * Visits every cell of a part that lands inside its chunk, spaces included, slice by slice.
     *
     * @param part      the part
     * @param transform its transform
     * @param offsetX   local X of its origin
     * @param offsetZ   local Z of its origin
     * @param visitor   what to do with each cell
     */
    public static void forEach(Part part, Transform transform, int offsetX, int offsetZ, Visitor visitor) {
        for (int y = 0; y < part.height(); y++) {
            for (int z = 0; z < part.zsize(); z++) {
                for (int x = 0; x < part.xsize(); x++) {
                    int localX = offsetX + transform.rotateX(x, z);
                    int localZ = offsetZ + transform.rotateZ(x, z);
                    if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
                        continue;
                    }
                    visitor.visit(x, y, z, localX, localZ, part.get(x, y, z));
                }
            }
        }
    }

    /**
     * Returns the block placing a character writes.
     *
     * @param c         the character
     * @param palette   the part's palette (the chunk's, with the building's and the part's own on top)
     * @param sequence  the chunk's palette sequence, for random characters
     * @param transform the part's transform, which turns directional blocks
     * @return the block; air for a space, an undefined character or hard air
     */
    public static BlockRef blockFor(char c, CompiledPalette palette, PaletteSequence sequence, Transform transform) {
        if (c == ' ' || !palette.isDefined(c)) {
            return BlockRef.AIR;
        }
        BlockChoice choice = palette.isRandom(c) ? palette.choice(c, sequence.next()) : palette.choice(c, 0);
        if (choice.isHardAir()) {
            return BlockRef.AIR;
        }
        BlockRef block = choice.block(palette.flags());
        return transform == Transform.ROTATE_NONE ? block : MetaRotation.rotate(block, choice.family(), transform);
    }

    /**
     * Places a part: every cell, from block Y {@code bottom} up.
     *
     * @param part      the part
     * @param palette   its palette
     * @param sequence  the chunk's palette sequence
     * @param transform its transform
     * @param offsetX   local X of its origin
     * @param offsetZ   local Z of its origin
     * @param bottom    block Y of its first slice
     * @param sink      where the blocks go
     * @return how many cells were written
     */
    public static int place(Part part, CompiledPalette palette, PaletteSequence sequence, Transform transform,
        int offsetX, int offsetZ, int bottom, Sink sink) {
        int[] written = { 0 };
        forEach(part, transform, offsetX, offsetZ, (px, y, pz, lx, lz, c) -> {
            sink.accept(lx, bottom + y, lz, blockFor(c, palette, sequence, transform));
            written[0]++;
        });
        return written[0];
    }

    /**
     * Builds a session's reverse palette: for every cell whose character the palette defines, the world's block there
     * stands for that character; when several characters give the same block, the last one read wins, as upstream.
     *
     * @param part      the part
     * @param palette   its palette
     * @param transform its transform
     * @param offsetX   local X of its origin
     * @param offsetZ   local Z of its origin
     * @param bottom    block Y of its first slice
     * @param world     the world's blocks
     * @return the reverse palette, in reading order
     */
    public static Map<BlockRef, Character> reversePalette(Part part, CompiledPalette palette, Transform transform,
        int offsetX, int offsetZ, int bottom, Source world) {
        Map<BlockRef, Character> reverse = new LinkedHashMap<>();
        forEach(part, transform, offsetX, offsetZ, (px, y, pz, lx, lz, c) -> {
            if (c != ' ' && palette.isDefined(c)) {
                reverse.put(world.get(lx, bottom + y, lz), c);
            }
        });
        return reverse;
    }

    /**
     * Returns the export grid of a part placed in the world: the block at each part cell's chunk position.
     *
     * @param transform the part's transform
     * @param offsetX   local X of its origin
     * @param offsetZ   local Z of its origin
     * @param bottom    block Y of its first slice
     * @param world     the world's blocks
     * @return the grid; a cell outside the chunk reads as air
     */
    public static PartExport.Grid grid(Transform transform, int offsetX, int offsetZ, int bottom, Source world) {
        return (x, y, z) -> {
            int localX = offsetX + transform.rotateX(x, z);
            int localZ = offsetZ + transform.rotateZ(x, z);
            if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
                return BlockRef.AIR;
            }
            return world.get(localX, bottom + y, localZ);
        };
    }
}
