package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Stuff.java
// Avoids: ASSET-21, SCAT-20, SCAT-21, SCAT-22, SCAT-23

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.match.BlockMatcher;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Stuff;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ContentContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Upstream's stuff ({@code Stuff.generateStuff}): the last step of a city chunk, which scatters short columns of
 * palette characters, the bundled chains and cobwebs hanging from ceilings, into the chunk. For each tag of the
 * chunk's city style and each stuff asset with that tag, a stuff that passes its building, building-name and biome
 * filters draws a count, then tries up to {@code attempts} random positions per column: the block below and the block
 * above the column must pass their matchers, the sky test must hold, and the column must be air.
 *
 * <p>
 * Changes from upstream, each with a regression test:
 * <ul>
 * <li>ASSET-21 (SCAT-17 to SCAT-19): {@code maxcount} equal to {@code mincount} places exactly that many instead of
 * throwing; a computed height range that comes out empty skips the stuff, with no draw; an unset {@code inbuilding}
 * means "either" (the loader checks explicit ranges);</li>
 * <li>SCAT-20: the {@code buildings} filter tests the building that stands in the chunk, so a street chunk has
 * none;</li>
 * <li>SCAT-21: the random is this step's own, seeded from the world seed and the chunk;</li>
 * <li>SCAT-22: tags in the city style's order and assets in id order, as the asset set indexes them at load;</li>
 * <li>SCAT-23: {@code seesky} reads the chunk's own column above the position, never the world's height map.</li>
 * </ul>
 * Tags: the asset model keeps a city style's tags (its own, its parents' and {@value CityStyle#ALL_STUFF}) sorted, so
 * that is the order here.
 *
 * <p>
 * Reads and writes only this chunk, never a soft-edge strip column. Spec: docs/original-spec/07-scattered/stuff.md,
 * docs/new-spec/07-scattered/index.md § Stuff. Side: both (plain Java).
 */
public final class StuffWriter {

    /** Upstream's stuff seed multipliers, with the world seed mixed in (SCAT-21). */
    static final long STUFF_X = 2570174657L;
    static final long STUFF_Z = 101754695981L;
    /** Height of a stuff range above its default bottom outside a building: upstream's {@code minheight + 20}. */
    static final int DEFAULT_RANGE = 20;
    /** Blocks above a building's top floor that a stuff range reaches: upstream's margin of 10. */
    static final int ROOF_MARGIN = 10;

    /** No ore-dictionary names: what a matcher sees when the glue gives no lookup. */
    public static final Function<BlockRef, Set<String>> NO_ORES = block -> Collections.emptySet();

    private final CityWriteContext context;
    private final int chunkX;
    private final int chunkZ;
    private final Function<BlockRef, Set<String>> oreNames;

    /**
     * Prepares the stuff of one city chunk.
     *
     * @param context  the world's city writing context
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param oreNames a block's ore-dictionary names, for {@code ore:} matcher entries
     */
    public StuffWriter(CityWriteContext context, int chunkX, int chunkZ, Function<BlockRef, Set<String>> oreNames) {
        this.context = context;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.oreNames = oreNames;
    }

    /**
     * Returns the seed of a chunk's stuff random.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return the seed
     */
    public static long seed(long worldSeed, int chunkX, int chunkZ) {
        return SeedMix.chunk(worldSeed, chunkX, chunkZ, STUFF_X, STUFF_Z);
    }

    /**
     * Writes the chunk's stuff.
     *
     * @param buffer  the chunk, with its building or street, ruins and rubble written
     * @param content the chunk's content context, for its soft-edge columns
     * @return the columns placed
     */
    public int write(ChunkBuffer buffer, ContentContext content) {
        ChunkPlan plan = context.planner()
            .plan(chunkX, chunkZ);
        CityStyle style = context.assets()
            .cityStyle(CityPalettes.id(plan.cityStyle()));
        if (style == null) {
            return 0;
        }
        Random random = new Random(seed(context.worldSeed(), chunkX, chunkZ));
        BiomeDescriptor biome = context.biomes()
            .biomeOfChunk(chunkX, chunkZ);
        CompiledPalette palette = context.chunkPalette(plan)
            .palette();
        PaletteSequence sequence = new PaletteSequence(context.worldSeed() ^ STUFF_Z, chunkX, chunkZ);
        AssetId building = plan.hasBuilding() ? CityPalettes.id(plan.building()) : null;
        int placed = 0;
        for (String tag : style.stuffTags()) {
            for (Stuff stuff : context.assets()
                .stuffByTag(tag)) {
                Boolean inBuilding = stuff.inBuilding();
                // Avoids SCAT-19: unset means either.
                if (inBuilding != null && inBuilding != plan.hasBuilding()) {
                    continue;
                }
                // Avoids SCAT-20: the building that stands here, none on a street.
                if (!stuff.buildings()
                    .matches(building)) {
                    continue;
                }
                if (!stuff.biomes()
                    .isAny()
                    && !stuff.biomes()
                        .matches(biome)) {
                    continue;
                }
                placed += generate(buffer, content, stuff, plan, palette, sequence, random);
            }
        }
        return placed;
    }

    private int generate(ChunkBuffer buffer, ContentContext content, Stuff stuff, ChunkPlan plan,
        CompiledPalette palette, PaletteSequence sequence, Random random) {
        String column = stuff.column();
        for (int k = 0; k < column.length(); k++) {
            if (!palette.isDefined(column.charAt(k))) {
                context.log()
                    .warn(
                        "stuff " + stuff.id() + " uses character '" + column.charAt(k) + "', which the palette lacks");
                return 0;
            }
        }
        boolean building = plan.hasBuilding();
        int groundLevel = context.settings()
            .groundLevel();
        int cityGround = plan.cityGroundLevel();
        int min = stuff.minHeight() != null ? stuff.minHeight()
            : building ? cityGround - plan.cellars() * ChunkPlan.FLOOR_HEIGHT : groundLevel;
        int max = stuff.maxHeight() != null ? stuff.maxHeight()
            : building ? cityGround + plan.floors() * ChunkPlan.FLOOR_HEIGHT + ROOF_MARGIN : min + DEFAULT_RANGE;
        if (max <= min) {
            // Avoids SCAT-18: an empty range skips the stuff, with no draw.
            return 0;
        }
        int count = count(random, stuff.minCount(), stuff.maxCount());
        int placed = 0;
        for (int j = 0; j < count; j++) {
            for (int i = 0; i < stuff.attempts(); i++) {
                int x = random.nextInt(16);
                int y = random.nextInt(max - min) + min;
                int z = random.nextInt(16);
                if (fits(buffer, content, stuff, x, y, z)) {
                    for (int k = 0; k < column.length(); k++) {
                        char c = column.charAt(k);
                        BlockRef block = palette.isRandom(c) ? palette.get(c, sequence.next()) : palette.get(c, 0);
                        buffer.set(x, y + k, z, block);
                    }
                    placed++;
                    break;
                }
            }
        }
        return placed;
    }

    /**
     * Draws a stuff's column count: {@code mincount} to {@code maxcount − 1}, as upstream's format documents, and
     * exactly {@code mincount} when the two are equal, with no draw (avoids SCAT-17, ASSET-21).
     *
     * @param random   the stuff random
     * @param minCount the minimum
     * @param maxCount the exclusive maximum, at least the minimum
     * @return the count
     */
    static int count(Random random, int minCount, int maxCount) {
        return maxCount == minCount ? minCount : random.nextInt(maxCount - minCount) + minCount;
    }

    private boolean fits(ChunkBuffer buffer, ContentContext content, Stuff stuff, int x, int y, int z) {
        int length = stuff.column()
            .length();
        if (y < WorldLimits.MIN_Y || y + length - 1 > WorldLimits.MAX_Y || content.isSoftEdge(x, z)) {
            return false;
        }
        if (!test(stuff.blocks(), block(buffer, x, y - 1, z))
            || !test(stuff.upperBlocks(), block(buffer, x, y + length, z))) {
            return false;
        }
        Boolean seeSky = stuff.seeSky();
        if (seeSky != null && seeSky != seesSky(buffer, x, y + length - 1, z)) {
            return false;
        }
        for (int k = 0; k < length; k++) {
            if (!buffer.get(x, y + k, z)
                .isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean test(BlockMatcher matcher, BlockRef block) {
        return matcher == BlockMatcher.ANY || matcher.matches(block, oreNames.apply(block));
    }

    private static BlockRef block(ChunkBuffer buffer, int x, int y, int z) {
        return ChunkBuffer.contains(x, y, z) ? buffer.get(x, y, z) : BlockRef.AIR;
    }

    /**
     * Avoids SCAT-23: whether a position sees the sky, from this chunk's own column: no block but air above it.
     *
     * @param buffer the chunk
     * @param x      local X
     * @param y      the position's Y
     * @param z      local Z
     * @return whether only air lies above
     */
    static boolean seesSky(ChunkBuffer buffer, int x, int y, int z) {
        for (int above = Math.min(buffer.topY(), WorldLimits.MAX_Y); above > y; above--) {
            if (!buffer.get(x, above, z)
                .isAir()) {
                return false;
            }
        }
        return true;
    }
}
