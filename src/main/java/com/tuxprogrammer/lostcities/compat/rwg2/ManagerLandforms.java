package com.tuxprogrammer.lostcities.compat.rwg2;

import java.lang.reflect.Field;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.overlay.PlanThreads;

import rwg2.biomes.realistic.land.RealisticBiomeMountainChain;
import rwg2.config.ConfigRWG;
import rwg2.util.ContinentalNoise;
import rwg2.world.ChunkManagerRealistic;

/**
 * The RWG2 predicates of a world, answered by its own {@code ChunkManagerRealistic}: the calls RWG2's
 * {@code WitcheryWorldgenCompat} makes to keep Witchery off its volcanoes, lava-cave openings and mountain-chain river
 * junctions, plus the centers of the first two.
 *
 * <ul>
 * <li>Volcano: {@code getVolcanoVicinityCoordinates} is set within the volcano's island radius (160 blocks) when RWG2
 * lets the volcano generate. Its center comes from the manager's {@code ContinentalNoise} (a private field, read once
 * by reflection), {@code getVolcanoCenterCoordinates}, rounded to whole blocks there and so the same from every column;
 * if the field cannot be read, the center is the column minus the local offset RWG2 returns, rounded.</li>
 * <li>Lava cave: {@code getLavaCaveCenterCoordinates}, set within the cave's influence when the cave may generate.</li>
 * <li>Junction strength: {@code getRiverJunctionStrength}; mountain chain: {@code getBiomeDataAt} is a
 * {@code RealisticBiomeMountainChain}.</li>
 * </ul>
 * The manager's own memo maps (volcano and lava-cave eligibility, biome data) only remember answers, so asking it does
 * not change the world it generates.
 *
 * <p>
 * Source: RWG2 2.0.6,
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L407-L428
 * (volcano and lava-cave coordinates),
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L912-L916
 * (junction strength),
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/util/ContinentalNoise.java#L373-L377
 * (volcano center), and
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/support/WitcheryWorldgenCompat.java#L23-L95
 * (the protection rule this mirrors).
 *
 * <p>
 * Load only when {@code Loader.isModLoaded("rwg2")}. The manager's caches are not thread-safe, so one instance is used by one thread;
 * {@link #forWorld} routes the plan worker to its own.
 * Side: server.
 */
final class ManagerLandforms implements Rwg2Landforms {

    private final ChunkManagerRealistic manager;
    private final ContinentalNoise continents;

    /**
     * Wraps a world's chunk manager.
     *
     * @param manager the manager
     */
    ManagerLandforms(ChunkManagerRealistic manager) {
        this.manager = manager;
        this.continents = continents(manager);
    }

    /**
     * Returns the landforms of a world for whichever thread asks: over the world's manager on the server thread, over
     * the plan worker's own copy on the worker (D-plan-worker-2), since the manager's memo maps are not thread-safe.
     *
     * @param world   the world
     * @param manager the world's manager
     * @return the routed landforms
     */
    static Rwg2Landforms forWorld(World world, ChunkManagerRealistic manager) {
        ManagerLandforms server = new ManagerLandforms(manager);
        return new Rwg2Landforms() {

            private Rwg2Landforms current() {
                return PlanThreads.route(world, server, copy -> new ManagerLandforms((ChunkManagerRealistic) copy));
            }

            @Override
            public long volcanoCenter(int blockX, int blockZ) {
                return current().volcanoCenter(blockX, blockZ);
            }

            @Override
            public long lavaCaveCenter(int blockX, int blockZ) {
                return current().lavaCaveCenter(blockX, blockZ);
            }

            @Override
            public float junctionStrength(int blockX, int blockZ) {
                return current().junctionStrength(blockX, blockZ);
            }

            @Override
            public boolean mountainChain(int blockX, int blockZ) {
                return current().mountainChain(blockX, blockZ);
            }
        };
    }

    private static ContinentalNoise continents(ChunkManagerRealistic manager) {
        try {
            Field field = ChunkManagerRealistic.class.getDeclaredField("continents");
            field.setAccessible(true);
            Object value = field.get(manager);
            return value instanceof ContinentalNoise ? (ContinentalNoise) value : null;
        } catch (ReflectiveOperationException | SecurityException e) {
            LostCities.LOG
                .warn("[LCNH-RWG2] cannot read RWG2's continental noise ({}); volcano centers rounded", e.toString());
            return null;
        }
    }

    @Override
    public long volcanoCenter(int blockX, int blockZ) {
        long local = manager.getVolcanoVicinityCoordinates(blockX, blockZ);
        if (local == Long.MIN_VALUE) {
            return NONE;
        }
        if (continents != null) {
            long center = continents
                .getVolcanoCenterCoordinates(blockX + ConfigRWG.landmassOffsetX, blockZ + ConfigRWG.landmassOffsetZ);
            if (center != Long.MIN_VALUE) {
                int x = (int) (center >> 32) - ConfigRWG.landmassOffsetX;
                int z = (int) center - ConfigRWG.landmassOffsetZ;
                return Rwg2Landforms.pack(x, z);
            }
        }
        int x = Math.round(blockX - ContinentalNoise.unpackVolcanoX(local));
        int z = Math.round(blockZ - ContinentalNoise.unpackVolcanoY(local));
        return Rwg2Landforms.pack(x, z);
    }

    @Override
    public long lavaCaveCenter(int blockX, int blockZ) {
        long center = manager.getLavaCaveCenterCoordinates(blockX, blockZ);
        if (center == Long.MIN_VALUE) {
            return NONE;
        }
        return Rwg2Landforms.pack((int) (center >> 32), (int) center);
    }

    @Override
    public float junctionStrength(int blockX, int blockZ) {
        return manager.getRiverJunctionStrength(blockX, blockZ);
    }

    @Override
    public boolean mountainChain(int blockX, int blockZ) {
        return manager.getBiomeDataAt(blockX, blockZ) instanceof RealisticBiomeMountainChain;
    }

    /**
     * Returns whether RWG2's own rule protects a column (its {@code WitcheryWorldgenCompat.isProtectedLandmark} and
     * junction test, without the 8-block margin), from the manager directly: the scenario's exact reference.
     *
     * @param x block X
     * @param z block Z
     * @return whether the column is a volcano vicinity, a lava-cave opening or a mountain-chain river junction
     */
    boolean protectedByRwg2(int x, int z) {
        if (manager.getVolcanoVicinityCoordinates(x, z) != Long.MIN_VALUE) {
            return true;
        }
        long cave = manager.getLavaCaveCoordinates(x, z);
        if (cave != Long.MIN_VALUE) {
            float caveX = ContinentalNoise.unpackVolcanoX(cave);
            float caveZ = ContinentalNoise.unpackVolcanoY(cave);
            if (caveX * caveX + caveZ * caveZ
                <= Rwg2LandmarkOracle.LAVA_CAVE_OPENING * Rwg2LandmarkOracle.LAVA_CAVE_OPENING) {
                return true;
            }
        }
        if (manager.getRiverJunctionStrength(x, z) < Rwg2LandmarkOracle.JUNCTION_THRESHOLD) {
            return false;
        }
        for (int dz = -Rwg2LandmarkOracle.CHAIN_REACH; dz <= Rwg2LandmarkOracle.CHAIN_REACH; dz += 16) {
            for (int dx = -Rwg2LandmarkOracle.CHAIN_REACH; dx <= Rwg2LandmarkOracle.CHAIN_REACH; dx += 16) {
                if (mountainChain(x + dx, z + dz)) {
                    return true;
                }
            }
        }
        return false;
    }
}
