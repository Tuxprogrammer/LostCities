package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.chunk.Chunk;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.packhouses.PackHouses;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.worldgen.city.CityFinder;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/**
 * {@code packhouse.real} (real pack, fresh world): generates the region around the nearest edge lot that holds a
 * pack house and checks that
 * <ul>
 * <li>the lot chunk holds the house's blocks above its ground, and none (apart from the terrain's own trees and plants) in the
 * outermost columns of the chunk;</li>
 * <li>no chunk was generated while a population ran (no cascade);</li>
 * <li>building the house a second time gives the same blocks (the piece is a pure function of the seed and chunk).</li>
 * </ul>
 * The log line {@code [LCNH-PACKHOUSE]} carries the kind and the block hash, so two fresh runs of one seed compare.
 *
 * <p>
 * Side: server.
 */
final class PackHouseScenario implements Scenario {

    /** Chunks searched each way around the city center for a lot with a house. */
    static final int SEARCH = 64;
    /** Radius of the region generated around the lot, in chunks. */
    static final int REGION_RADIUS = 4;
    /** Least blocks above ground a house must have placed. */
    static final int MIN_BLOCKS = 30;
    /** Height above ground that is hashed. */
    static final int HASH_HEIGHT = 20;

    private RegionGenerator generator;
    private int lotX;
    private int lotZ;
    private ChunkPlan lot;
    private long cascadesBefore;
    private boolean done;

    @Override
    public String name() {
        return "packhouse.real";
    }

    @Override
    public int ticks() {
        return 20 * 60 * 8;
    }

    @Override
    public boolean finished(ScenarioContext context) {
        return done;
    }

    @Override
    public void setup(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(overlay != null && overlay.cities() != null, "expected the overlay with the layout plan source");
        CityFinder.Result center = CityScenarios.freshCenter(context, overlay);
        ChunkCoordinates spawn = context.world()
            .getSpawnPoint();
        int min = CityScenarios.SPAWN_AREA + REGION_RADIUS + 1;
        for (int ring = 0; ring <= SEARCH && lot == null; ring++) {
            for (int dz = -ring; dz <= ring && lot == null; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = center.chunkX() + dx;
                    int z = center.chunkZ() + dz;
                    if (Math.max(Math.abs(x - (spawn.posX >> 4)), Math.abs(z - (spawn.posZ >> 4))) < min) {
                        continue;
                    }
                    ChunkPlan plan = overlay.plan()
                        .chunk(x, z);
                    if (plan.packHouseSide() >= 0) {
                        lot = plan;
                        lotX = x;
                        lotZ = z;
                        break;
                    }
                }
            }
        }
        context.check(
            lot != null,
            "expected an edge lot with a pack house within " + SEARCH + " chunks of the city center");
        if (lot == null) {
            done = true;
            return;
        }
        List<String> kinds = PackHouses.usableIds();
        LostCities.LOG.info("[LCNH-PACKHOUSE] lot ({}, {}) side {} kinds {}", lotX, lotZ, lot.packHouseSide(), kinds);
        context.check(!kinds.isEmpty(), "expected at least vanilla's houses to be usable");
        cascadesBefore = overlay.cascades();
        generator = new RegionGenerator(context.world(), lotX, lotZ, REGION_RADIUS);
    }

    @Override
    public void tick(ScenarioContext context) {
        if (generator != null) {
            done = generator.tick();
        }
    }

    @Override
    public void verify(ScenarioContext context) {
        if (lot == null) {
            return;
        }
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(done, "expected the region generated");
        context.check(
            overlay.cascades() == cascadesBefore,
            "expected no chunk generated during a population, found " + (overlay.cascades() - cascadesBefore));
        Chunk chunk = context.world()
            .getChunkFromChunkCoords(lotX, lotZ);
        int ground = lot.groundY();
        int blocks = 0;
        int edge = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = ground + 1; y <= ground + HASH_HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) != Blocks.air) {
                        blocks++;
                        if ((x == 0 || x == 15 || z == 0 || z == 15) && !natural(chunk.getBlock(x, y, z))) {
                            edge++;
                            if (edge <= 6) {
                                LostCities.LOG.info(
                                    "[LCNH-PACKHOUSE] edge block {} at ({}, {}, {}) ground {}",
                                    Block.blockRegistry.getNameForObject(chunk.getBlock(x, y, z)),
                                    x,
                                    y,
                                    z,
                                    ground);
                            }
                        }
                    }
                }
            }
        }
        context.check(blocks >= MIN_BLOCKS, "expected the house's blocks above the lot's ground, found " + blocks);
        context.check(edge == 0, "expected no house block in the chunk's outermost columns, found " + edge);
        long first = hash(chunk, ground);
        String kind = overlay.packHouse(lotX, lotZ, lot);
        long second = hash(chunk, ground);
        context.check(kind != null, "expected the second build to place a house");
        context.check(
            first == second,
            "expected the same blocks from a second build, got " + Long.toHexString(first)
                + " then "
                + Long.toHexString(second));
        LostCities.LOG.info(
            "[LCNH-PACKHOUSE] chunk ({}, {}) kind {} blocks {} hash {}",
            lotX,
            lotZ,
            kind,
            blocks,
            Long.toHexString(first));
    }

    /** Trees, plants and snow that the terrain's own population puts at the chunk edge; not part of any house. */
    private static boolean natural(Block block) {
        String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
        return name.contains("log") || name.contains("leaves")
            || name.contains("grass")
            || name.contains("flower")
            || name.contains("snow")
            || name.contains("vine")
            || name.contains("wall");
    }

    private static long hash(Chunk chunk, int ground) {
        long h = 1125899906842597L;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = ground + 1; y <= ground + HASH_HEIGHT; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    h = 31 * h + Block.getIdFromBlock(block);
                    h = 31 * h + chunk.getBlockMetadata(x, y, z);
                }
            }
        }
        return h;
    }
}
