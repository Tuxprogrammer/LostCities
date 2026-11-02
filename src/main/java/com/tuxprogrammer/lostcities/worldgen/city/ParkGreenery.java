package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.model.RandomList;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.variety.PaletteVariety;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * Fills a park's bare grass the way bonemeal would (AR-18, the owner's alpha review): tall grass on most free grass,
 * with flowers, ferns and small leaf shrubs mixed in, chosen for the chunk's biome. Upstream's parks carry only the
 * few flowers and saplings of their park part, which left the grass bare.
 *
 * <p>
 * A plant goes only on a grass block of the park's surface with air above it, so park parts, paths, ponds, benches
 * and the border are left as they are; it is one block high, so nothing it places can be cut in half later, and the
 * rubble layer covers a plant rather than stacking on top of it. Every plant stays on grass in every biome (no dead
 * bush or cactus, which drop off grass on the next update). Shrubs are leaves with the no-decay bit, like the leaves
 * upstream piles along buildings. The tables here are vanilla; where the chunk palette's variety defines a flora list
 * (D25, M11), the plants come from it instead, with the pack's flowers, grasses and bushes beside the vanilla ones.
 *
 * <p>
 * Deterministic: one {@link Random} per chunk from the world seed and the chunk coordinates, two draws per free grass
 * block, in a fixed order. Cheap: one pass over at most 256 columns at one height.
 *
 * <p>
 * Spec: docs/new-spec/03-terrain/index.md, "Park greenery" (a deviation). Immutable tables. Side: both (plain Java).
 */
final class ParkGreenery {

    /** The plant tables a biome can use. */
    enum Flora {

        /** Plains, forests and anything not listed below: tall grass, many kinds of flowers, oak and birch shrubs. */
        TEMPERATE(0.55f),
        /** Deserts, savannas, mesas and other hot dry land: sparse dry grass, rare flowers, acacia shrubs. */
        ARID(0.35f),
        /** Taiga and cold or snowy land: grass and ferns, few flowers, spruce shrubs. */
        COLD(0.5f),
        /** Jungles: dense grass and ferns, jungle shrubs. */
        JUNGLE(0.65f),
        /** Swamps: grass, ferns and blue orchids, oak shrubs. */
        SWAMP(0.5f);

        private final float cover;

        Flora(float cover) {
            this.cover = cover;
        }

        /** Returns the chance a free grass block gets a plant. */
        float cover() {
            return cover;
        }

        /** Returns the palette character whose list holds this flora's plants with the pack's flora mixed in (D25). */
        char character() {
            switch (this) {
                case ARID:
                    return PaletteVariety.FLORA_ARID;
                case COLD:
                    return PaletteVariety.FLORA_COLD;
                case JUNGLE:
                    return PaletteVariety.FLORA_JUNGLE;
                case SWAMP:
                    return PaletteVariety.FLORA_SWAMP;
                default:
                    return PaletteVariety.FLORA_TEMPERATE;
            }
        }
    }

    /** Seed factors of the greenery random, distinct from every other per-chunk random of the writer. */
    static final long SEED_X = 618039887L;
    static final long SEED_Z = 1442695040L;

    /** The block a plant stands on: the park's grass, whatever its metadata. */
    static final String GRASS_BLOCK = "minecraft:grass";

    private static final BlockRef GRASS = BlockRef.of("minecraft:tallgrass", 1);
    private static final BlockRef FERN = BlockRef.of("minecraft:tallgrass", 2);
    private static final BlockRef DRY_SHRUB = BlockRef.of("minecraft:tallgrass", 0);
    private static final BlockRef DANDELION = BlockRef.of("minecraft:yellow_flower", 0);
    private static final BlockRef POPPY = BlockRef.of("minecraft:red_flower", 0);
    private static final BlockRef BLUE_ORCHID = BlockRef.of("minecraft:red_flower", 1);
    private static final BlockRef ALLIUM = BlockRef.of("minecraft:red_flower", 2);
    private static final BlockRef AZURE_BLUET = BlockRef.of("minecraft:red_flower", 3);
    private static final BlockRef RED_TULIP = BlockRef.of("minecraft:red_flower", 4);
    private static final BlockRef ORANGE_TULIP = BlockRef.of("minecraft:red_flower", 5);
    private static final BlockRef WHITE_TULIP = BlockRef.of("minecraft:red_flower", 6);
    private static final BlockRef PINK_TULIP = BlockRef.of("minecraft:red_flower", 7);
    private static final BlockRef OXEYE_DAISY = BlockRef.of("minecraft:red_flower", 8);
    /** Leaves with the no-decay bit (4): oak, spruce, birch, jungle; acacia in {@code leaves2}. */
    private static final BlockRef OAK_SHRUB = BlockRef.of("minecraft:leaves", 4);
    private static final BlockRef SPRUCE_SHRUB = BlockRef.of("minecraft:leaves", 5);
    private static final BlockRef BIRCH_SHRUB = BlockRef.of("minecraft:leaves", 6);
    private static final BlockRef JUNGLE_SHRUB = BlockRef.of("minecraft:leaves", 7);
    private static final BlockRef ACACIA_SHRUB = BlockRef.of("minecraft:leaves2", 4);

    private static final List<BlockRef> TEMPERATE = new Table().add(GRASS, 62)
        .add(FERN, 3)
        .add(DANDELION, 8)
        .add(POPPY, 7)
        .add(OXEYE_DAISY, 4)
        .add(AZURE_BLUET, 4)
        .add(RED_TULIP, 1)
        .add(ORANGE_TULIP, 1)
        .add(WHITE_TULIP, 1)
        .add(PINK_TULIP, 1)
        .add(ALLIUM, 1)
        .add(OAK_SHRUB, 5)
        .add(BIRCH_SHRUB, 2)
        .build();
    private static final List<BlockRef> ARID = new Table().add(GRASS, 78)
        .add(DRY_SHRUB, 8)
        .add(DANDELION, 2)
        .add(POPPY, 2)
        .add(ORANGE_TULIP, 1)
        .add(ACACIA_SHRUB, 7)
        .add(OAK_SHRUB, 2)
        .build();
    private static final List<BlockRef> COLD = new Table().add(GRASS, 48)
        .add(FERN, 34)
        .add(POPPY, 3)
        .add(DANDELION, 3)
        .add(AZURE_BLUET, 2)
        .add(SPRUCE_SHRUB, 10)
        .build();
    private static final List<BlockRef> JUNGLE = new Table().add(GRASS, 46)
        .add(FERN, 34)
        .add(POPPY, 3)
        .add(DANDELION, 2)
        .add(BLUE_ORCHID, 1)
        .add(JUNGLE_SHRUB, 14)
        .build();
    private static final List<BlockRef> SWAMP = new Table().add(GRASS, 56)
        .add(FERN, 16)
        .add(BLUE_ORCHID, 12)
        .add(POPPY, 2)
        .add(DANDELION, 2)
        .add(OAK_SHRUB, 12)
        .build();

    private ParkGreenery() {}

    /**
     * Returns the plant table for a biome, from its Forge {@code BiomeDictionary} types: swamp, then jungle, then hot
     * and dry land, then cold land, else temperate.
     *
     * @param biome the chunk's main biome
     * @return the flora
     */
    static Flora floraOf(BiomeDescriptor biome) {
        Set<String> types = biome.types();
        if (types.contains("SWAMP")) {
            return Flora.SWAMP;
        }
        if (types.contains("JUNGLE")) {
            return Flora.JUNGLE;
        }
        if (types.contains("SANDY") || types.contains("SAVANNA")
            || types.contains("MESA")
            || types.contains("WASTELAND")
            || types.contains("HOT") && types.contains("DRY")) {
            return Flora.ARID;
        }
        if (types.contains("CONIFEROUS") || types.contains("COLD") || types.contains("SNOWY")) {
            return Flora.COLD;
        }
        return Flora.TEMPERATE;
    }

    /**
     * Returns the weighted table of a flora, one entry per unit of weight, so a uniform draw picks by weight.
     *
     * @param flora the flora
     * @return the table, unmodifiable
     */
    static List<BlockRef> table(Flora flora) {
        switch (flora) {
            case ARID:
                return ARID;
            case COLD:
                return COLD;
            case JUNGLE:
                return JUNGLE;
            case SWAMP:
                return SWAMP;
            default:
                return TEMPERATE;
        }
    }

    /**
     * Returns the greenery random of a chunk.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return a fresh random
     */
    static Random random(long worldSeed, int chunkX, int chunkZ) {
        return new Random(SeedMix.chunk(worldSeed, chunkX, chunkZ, SEED_X, SEED_Z));
    }

    /**
     * Plants a park: every grass block at {@code height − 1} with air at {@code height} draws against the flora's
     * cover and, on a hit, takes a plant from its table.
     *
     * @param target the chunk being written
     * @param height block Y of the plants, one above the park's grass
     * @param flora  the chunk's flora
     * @param random the chunk's greenery random
     * @return the number of plants placed
     */
    static int plant(CityTarget target, int height, Flora flora, Random random) {
        return plant(target, height, flora, null, random);
    }

    /**
     * Plants a park, drawing from the chunk palette's flora list where the palette variety defines one: the pack's
     * flowers, grasses and bushes beside the vanilla plants (D25). Same draws per grass block as the vanilla tables:
     * one float against the cover, one integer for the plant (a slot of 128 for a palette list).
     *
     * @param target  the chunk being written
     * @param height  block Y of the plants, one above the park's grass
     * @param flora   the chunk's flora
     * @param palette the chunk palette, or {@code null} for the vanilla tables
     * @param random  the chunk's greenery random
     * @return the number of plants placed
     */
    static int plant(CityTarget target, int height, Flora flora, CompiledPalette palette, Random random) {
        char c = flora.character();
        if (palette != null && palette.isDefined(c)) {
            return plantFromPalette(target, height, flora, palette, c, random);
        }
        List<BlockRef> table = table(flora);
        int placed = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (!GRASS_BLOCK.equals(
                    target.get(x, height - 1, z)
                        .name())
                    || !target.get(x, height, z)
                        .isAir()) {
                    continue;
                }
                float draw = random.nextFloat();
                int pick = random.nextInt(table.size());
                if (draw < flora.cover()) {
                    target.set(x, height, z, table.get(pick));
                    placed++;
                }
            }
        }
        return placed;
    }

    private static int plantFromPalette(CityTarget target, int height, Flora flora, CompiledPalette palette, char c,
        Random random) {
        int placed = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (!GRASS_BLOCK.equals(
                    target.get(x, height - 1, z)
                        .name())
                    || !target.get(x, height, z)
                        .isAir()) {
                    continue;
                }
                float draw = random.nextFloat();
                int slot = random.nextInt(RandomList.SLOTS);
                if (draw < flora.cover()) {
                    BlockRef plant = palette.get(c, slot);
                    if (plant != null && !plant.isAir()) {
                        target.set(x, height, z, plant);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }

    /** Builds a weighted table: each block listed as many times as its weight. */
    private static final class Table {

        private final List<BlockRef> entries = new ArrayList<>();

        Table add(BlockRef block, int weight) {
            for (int n = 0; n < weight; n++) {
                entries.add(block);
            }
            return this;
        }

        List<BlockRef> build() {
            return Collections.unmodifiableList(entries);
        }
    }
}
