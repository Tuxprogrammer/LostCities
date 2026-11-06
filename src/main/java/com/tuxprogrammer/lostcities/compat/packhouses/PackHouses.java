package com.tuxprogrammer.lostcities.compat.packhouses;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.random.StableHash;
import com.tuxprogrammer.lostcities.worldgen.plan.PackHouseSelection;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.VillagerRegistry;

/**
 * Builds a pack house: a house another mod's own village-component generator makes, placed on an empty edge lot
 * (docs/design/0011-city-edges-and-character.md, pack houses). The piece is created the way a village creates it
 * (Forge's {@code VillagerRegistry.getVillageComponent} for a mod, the piece class's own factory for vanilla's), from a
 * {@code Random} seeded by the world seed and the chunk, at a box inside the lot chunk with its ground level already
 * set ({@link PieceFields}), and written with {@code addComponentParts} clipped to the chunk's own box, so it writes
 * nothing outside the chunk and is the same whatever order chunks generate in. Pieces are stateful, so each chunk
 * builds its own.
 *
 * <p>
 * Villagers never spawn from these pieces (their counters are raised), and a house wider or deeper than
 * {@value #MAX_SPAN} blocks is skipped so a two-block margin keeps its walls, chests and doors off the chunk's edge,
 * where placing a tile entity would read the next chunk.
 *
 * <p>
 * Side: server.
 */
public final class PackHouses {

    /** The widest and deepest box built: leaves a two-block margin in a chunk. */
    public static final int MAX_SPAN = 12;

    /** A house kind: a vanilla piece class, or a mod's piece class registered with Forge's villager registry. */
    static final class Kind {

        final String id;
        final String modId;
        final String pieceClass;
        final Class<?> vanilla;

        Kind(String id, String modId, String pieceClass, Class<?> vanilla) {
            this.id = id;
            this.modId = modId;
            this.pieceClass = pieceClass;
            this.vanilla = vanilla;
        }
    }

    /** A kind that is usable in this game: with its Forge piece weight, or vanilla's factory. */
    static final class Usable {

        final Kind kind;
        final StructureVillagePieces.PieceWeight weight;
        final Method factory;

        Usable(Kind kind, StructureVillagePieces.PieceWeight weight, Method factory) {
            this.kind = kind;
            this.weight = weight;
            this.factory = factory;
        }
    }

    /** Every kind, in a fixed order; the order is what makes the pick stable. */
    static final Kind[] KINDS = { new Kind("vanilla.house1", "", "", StructureVillagePieces.House1.class),
        new Kind("vanilla.smith", "", "", StructureVillagePieces.House2.class),
        new Kind("vanilla.house3", "", "", StructureVillagePieces.House3.class),
        new Kind("vanilla.garden", "", "", StructureVillagePieces.House4Garden.class),
        new Kind("vanilla.woodhut", "", "", StructureVillagePieces.WoodHut.class),
        new Kind("vanilla.hall", "", "", StructureVillagePieces.Hall.class),
        new Kind("vanilla.church", "", "", StructureVillagePieces.Church.class),
        new Kind("railcraft.workshop", "Railcraft", "mods.railcraft.common.worldgen.ComponentWorkshop", null),
        new Kind("tconstruct.smeltery", "TConstruct", "tconstruct.world.village.ComponentSmeltery", null),
        new Kind("tconstruct.toolworkshop", "TConstruct", "tconstruct.world.village.ComponentToolWorkshop", null),
        new Kind("forestry.beehouse", "Forestry", "forestry.apiculture.worldgen.ComponentVillageBeeHouse", null),
        new Kind("witchery.apothecary", "witchery", "com.emoniph.witchery.worldgen.ComponentVillageApothecary", null),
        new Kind("witchery.bookshop", "witchery", "com.emoniph.witchery.worldgen.ComponentVillageBookShop", null),
        new Kind("witchery.hut", "witchery", "com.emoniph.witchery.worldgen.ComponentVillageWitchHut", null) };

    private static List<Usable> usable;

    private PackHouses() {}

    /**
     * Returns the kinds usable in this game, in {@link #KINDS} order: vanilla's, and each mod's that is loaded and has
     * registered its village piece (its own config leaves it out otherwise). Resolved once, on first use, after every
     * mod has registered.
     */
    static synchronized List<Usable> usable() {
        if (usable != null) {
            return usable;
        }
        List<Usable> out = new ArrayList<>();
        ArrayList<Object> weights = new ArrayList<>();
        try {
            VillagerRegistry.addExtraVillageComponents(weights, new Random(0L), 1);
        } catch (RuntimeException | LinkageError e) {
            LostCities.LOG.warn("Pack houses: the villager registry could not list its pieces", e);
        }
        for (Kind kind : KINDS) {
            if (kind.vanilla != null) {
                Method factory = factory(kind.vanilla);
                if (factory != null) {
                    out.add(new Usable(kind, null, factory));
                }
                continue;
            }
            if (!Loader.isModLoaded(kind.modId)) {
                continue;
            }
            for (Object entry : weights) {
                if (entry instanceof StructureVillagePieces.PieceWeight weight && weight.villagePieceClass != null
                    && kind.pieceClass.equals(weight.villagePieceClass.getName())) {
                    out.add(new Usable(kind, weight, null));
                    break;
                }
            }
        }
        StringBuilder names = new StringBuilder();
        for (Usable u : out) {
            names.append(u.kind.id)
                .append(' ');
        }
        LostCities.LOG.info(
            "Pack houses available: {}",
            names.toString()
                .trim());
        usable = out;
        return out;
    }

    /** Returns the ids of the usable kinds, for the scenario's log. */
    public static List<String> usableIds() {
        List<String> ids = new ArrayList<>();
        for (Usable u : usable()) {
            ids.add(u.kind.id);
        }
        return ids;
    }

    /** Finds a vanilla piece class's static factory by its shape (Start, List, Random, five ints), not its name. */
    private static Method factory(Class<?> piece) {
        for (Method method : piece.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) && p.length == 8
                && p[0] == StructureVillagePieces.Start.class
                && p[1] == List.class
                && p[2] == Random.class
                && method.getReturnType() == piece) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static StructureComponent create(Usable use, Random random, int x, int y, int z, int mode) {
        try {
            Object made;
            if (use.factory != null) {
                made = use.factory.invoke(null, null, new ArrayList<Object>(), random, x, y, z, mode, 0);
            } else {
                made = VillagerRegistry
                    .getVillageComponent(use.weight, null, new ArrayList<Object>(), random, x, y, z, mode, 0);
            }
            return made instanceof StructureComponent component ? component : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LostCities.LOG.warn("Pack house {} could not be created", use.kind.id, e);
            return null;
        }
    }

    /**
     * Returns the kind a lot holds, or {@code null} when none is usable.
     *
     * @param worldSeed the world seed
     * @param chunkX    the lot's chunk X
     * @param chunkZ    the lot's chunk Z
     * @return the kind's id
     */
    public static String kindAt(long worldSeed, int chunkX, int chunkZ) {
        List<Usable> list = usable();
        return list.isEmpty() ? null
            : list.get(PackHouseSelection.kind(worldSeed, chunkX, chunkZ, list.size())).kind.id;
    }

    /**
     * Builds the house of a lot inside its chunk.
     *
     * @param world    the world, whose chunk is loaded
     * @param chunkX   the lot's chunk X
     * @param chunkZ   the lot's chunk Z
     * @param groundY  the block Y of the lot's ground surface
     * @param doorSide the side the door faces, 0 north, 1 east, 2 south, 3 west
     * @return the id of the kind built, or {@code null} when nothing was built
     */
    public static String build(World world, int chunkX, int chunkZ, int groundY, int doorSide) {
        List<Usable> list = usable();
        if (list.isEmpty()) {
            return null;
        }
        long seed = world.getSeed();
        Usable use = list.get(PackHouseSelection.kind(seed, chunkX, chunkZ, list.size()));
        long pieceSeed = StableHash
            .mix64(seed ^ PackHouseSelection.SEED_SALT ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
        int mode = Math.floorMod(doorSide, 4);
        int y = groundY + 1;
        // Measure the box at the origin, then rebuild it centered in the chunk. A factory's box is a pure translation
        // of its inputs, so the second build has the measured size.
        StructureComponent trial = create(use, new Random(pieceSeed), 0, y, 0, mode);
        if (trial == null) {
            return null;
        }
        StructureBoundingBox measured = trial.getBoundingBox();
        int width = measured.getXSize();
        int depth = measured.getZSize();
        if (width > MAX_SPAN || depth > MAX_SPAN) {
            return null;
        }
        int minX = (chunkX << 4) + (16 - width) / 2;
        int minZ = (chunkZ << 4) + (16 - depth) / 2;
        Random random = new Random(pieceSeed);
        StructureComponent piece = create(use, random, minX - measured.minX, y, minZ - measured.minZ, mode);
        if (piece == null) {
            return null;
        }
        StructureBoundingBox box = piece.getBoundingBox();
        if (box.minX != minX || box.minZ != minZ || box.minY != y) {
            LostCities.LOG.warn("Pack house {} built at an unexpected box {}; skipped", use.kind.id, box);
            return null;
        }
        PieceFields.apply(piece, StructureVillagePieces.Village.class, y);
        StructureBoundingBox chunkBox = new StructureBoundingBox(
            chunkX << 4,
            0,
            chunkZ << 4,
            (chunkX << 4) + 15,
            255,
            (chunkZ << 4) + 15);
        try {
            piece.addComponentParts(world, random, chunkBox);
        } catch (RuntimeException e) {
            LostCities.LOG.warn("Pack house {} failed at chunk ({}, {})", use.kind.id, chunkX, chunkZ, e);
            return null;
        }
        return use.kind.id;
    }
}
