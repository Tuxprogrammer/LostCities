package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;

import astrotibs.villagenames.village.MapGenVillageVN;

/**
 * Builds a village's plan with VillageNames' own code, exactly as its generator does when a chunk near the start is
 * provided, but on a generator of our own: VillageNames' {@code getStructureStart} on a private
 * {@code MapGenVillageVN} subclass whose {@code Random} is seeded as {@link MapGenSeeds} says (decision log
 * D84). Nothing here loads or generates a chunk, reads saved data or touches {@code world.rand}:
 * {@code getStructureStart} reads the generator's {@code Random}, VillageNames' config and the world's biome provider
 * ({@code world.getWorldChunkManager()}), and its start's constructor builds every piece from those alone.
 *
 * <p>
 * Calling VillageNames rather than reimplementing it keeps the hundreds of piece classes, their weights and the
 * draw order exactly VillageNames'. Its side effects are the ones its generator has anyway: {@code BiomeManager}
 * learns the village biomes again, and every village-creation handler is asked for a piece weight with our
 * {@code Random}. The handlers are asked in the order {@link VillageHandlerOrder} pins.
 *
 * <p>
 * Touches VillageNames classes, so only {@link VillageNamesOracles} creates it, after {@code Loader.isModLoaded}.
 * Server thread only; the caller synchronizes.
 *
 * <p>
 * Source: VillageNames 4.5.21-GTNH,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/MapGenVillageVN.java#L157-L454
 * ({@code getStructureStart} and {@code Start}); vanilla 1.7.10 {@code MapGenStructure.func_151538_a} for the
 * {@code nextInt()} before it, and {@code StructureStart.isSizeableStructure}, which decides whether a start is built.
 *
 * <p>
 * Side: server.
 */
final class VillageNamesPlans implements VillagePlans {

    private final World world;
    private final OracleVillageGenerator generator = new OracleVillageGenerator();

    /**
     * Creates the plan builder of a world.
     *
     * @param world the world, whose biome provider and seed the plans read
     */
    VillageNamesPlans(World world) {
        this.world = world;
    }

    /** Returns the class of {@code Random} the generator uses, for the log. */
    String randomClass() {
        return generator.random()
            .getClass()
            .getName();
    }

    @Override
    public VillagePlan plan(int chunkX, int chunkZ) {
        StructureStart start = generator.start(world, chunkX, chunkZ);
        List<VillagePlan.Piece> pieces = new ArrayList<>();
        for (Object component : start.getComponents()) {
            StructureComponent piece = (StructureComponent) component;
            StructureBoundingBox box = piece.getBoundingBox();
            pieces.add(
                new VillagePlan.Piece(
                    new BlockRect(box.minX, box.minZ, box.maxX, box.maxZ),
                    piece instanceof StructureVillagePieces.Road));
        }
        return new VillagePlan(pieces, start.isSizeableStructure());
    }

    /**
     * VillageNames' village generator with its protected start factory opened to the oracle. Never registered with
     * anything and never asked to generate, so it holds no saved data. Hodgepodge's {@code fastRandom} gives it the
     * same {@code Random} class it gives the live generator, because both are built by {@code MapGenBase}'s
     * constructor (decision log D85).
     */
    static final class OracleVillageGenerator extends MapGenVillageVN {

        /** Returns the generator's {@code Random}. */
        Random random() {
            return this.rand;
        }

        /**
         * Builds the start of a chunk as VillageNames' live generator would: its {@code Random} in the state
         * {@code MapGenBase} and {@code MapGenStructure} leave, then {@code getStructureStart}.
         *
         * @param world  the world
         * @param chunkX the start chunk X
         * @param chunkZ the start chunk Z
         * @return VillageNames' start, its plan complete
         */
        StructureStart start(World world, int chunkX, int chunkZ) {
            this.worldObj = world;
            MapGenSeeds.prepareForStart(this.rand, world.getSeed(), chunkX, chunkZ);
            return getStructureStart(chunkX, chunkZ);
        }
    }
}
