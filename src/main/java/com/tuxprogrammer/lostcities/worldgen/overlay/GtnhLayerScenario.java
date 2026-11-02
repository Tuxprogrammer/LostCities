package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.loot.LootTier;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.city.CityFinder;
import com.tuxprogrammer.lostcities.worldgen.city.LootRules;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/**
 * {@code gtnh.layer}: the GTNH layer of a real city on a fresh world (ROADMAP M08 exit gate). It generates the region
 * around the first city center outside the spawn area, as {@code city.walk} does, then checks every city chunk of it:
 * <ul>
 * <li><b>Loot:</b> every chest the plan fills holds only items from this mod's categories, each in the tier map at EV
 * or below (docs/design/0004 §3); the categories drawn are tallied;</li>
 * <li><b>Spawners:</b> every spawner the plan sets names the mob the plan drew (after the registry fallback), from
 * upstream's sets (or EnderZoo's when the profile says so), never a pig, and each name creates an entity;</li>
 * <li><b>Ore veins:</b> GregTech ore blocks stand in the natural stone under the city (veins pass through, D18), and
 * none above the city ground in a building chunk, where only building blocks, which no GT ore replaces, stand. GT's
 * boulders replace terracotta and GT stones in buildings (ore-veins.md, the residual conflict option A accepts), and
 * ore can then form in a boulder: those are counted apart, with the boulder stone in buildings, and do not fail.</li>
 * </ul>
 * One {@code [LCNH-GTNH]} summary line per check goes to the log.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class GtnhLayerScenario implements Scenario {

    /** The mobs upstream's {@code easymobs} and {@code hardmobs} give, as 1.7.10 names (D20). */
    static final Set<String> UPSTREAM_MOBS = new HashSet<>(
        java.util.Arrays.asList("Zombie", "Skeleton", "Spider", "Blaze", "Witch"));

    private RegionGenerator generator;
    private CityFinder.Result center;
    private boolean done;
    private int waited;

    /** Ticks to wait after the region is generated, for GT's deferred ore work to land. */
    static final int ORE_SETTLE_TICKS = 100;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new GtnhLayerScenario());
    }

    @Override
    public String name() {
        return "gtnh.layer";
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
        context.check(overlay != null && overlay.cities() != null, "expected the overlay with the layout plan");
        center = CityScenarios.freshCenter(context, overlay);
        generator = new RegionGenerator(context.world(), center.chunkX(), center.chunkZ(), CityScenarios.REGION_RADIUS);
    }

    @Override
    public void tick(ScenarioContext context) {
        if (generator.tick()) {
            waited++;
            done = waited >= ORE_SETTLE_TICKS;
        }
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected the region generated, loaded " + generator.loaded() + "/" + generator.size());
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        WorldServer world = context.world();
        boolean enderZoo = overlay.settings()
            .enderZooHardMobs();
        Tally tally = new Tally();
        int inner = generator.innerRadius() - 1;
        for (int cz = center.chunkZ() - inner; cz <= center.chunkZ() + inner; cz++) {
            for (int cx = center.chunkX() - inner; cx <= center.chunkX() + inner; cx++) {
                ChunkPlan plan = overlay.plan()
                    .chunk(cx, cz);
                if (!plan.isCity()) {
                    continue;
                }
                tally.cityChunks++;
                for (PopulationJob job : plan.jobs()) {
                    int x = (cx << 4) + job.x();
                    int z = (cz << 4) + job.z();
                    if (job.kind() == PopulationJob.Kind.CHEST) {
                        chest(world, x, job.y(), z, job.argument(), tally);
                    } else if (job.kind() == PopulationJob.Kind.SPAWNER) {
                        spawner(world, x, job.y(), z, job.argument(), enderZoo, tally);
                    }
                }
                ores(world.getChunkFromChunkCoords(cx, cz), plan, tally);
            }
        }
        for (String line : tally.report()) {
            LostCities.LOG.info("[LCNH-GTNH] {}", line);
        }
        context.check(tally.cityChunks > 0, "expected city chunks in the region");
        context.check(tally.filledChests > 0, "expected filled city chests; " + tally.chestLine());
        context.check(tally.badItems.isEmpty(), "chest items not in the tier map or above EV: " + tally.badItems);
        context.check(tally.spawners > 0, "expected spawners; " + tally.spawnerLine());
        context.check(tally.badSpawners.isEmpty(), "spawners with the wrong mob: " + tally.badSpawners);
        context.check(tally.oresBelow > 0, "expected GT ore under the city; " + tally.oreLine());
        context.check(tally.oresInBuildings == 0, "GT ore inside buildings: " + tally.oreLine());
    }

    private static void chest(WorldServer world, int x, int y, int z, String category, Tally tally) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IInventory inventory)) {
            tally.missingChests++;
            return;
        }
        if (category.isEmpty()) {
            tally.emptyChests++;
            return;
        }
        tally.categories.merge(category, 1, Integer::sum);
        boolean any = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) {
                continue;
            }
            any = true;
            tally.items++;
            LootTier tier = CityLoot.tierOf(stack);
            if (tier == null || !tier.withinCeiling()) {
                if (tally.badItems.size() < CityScenarios.EXAMPLES) {
                    tally.badItems.add(CityLoot.key(stack) + "=" + tier + " at " + x + "," + y + "," + z);
                }
            } else {
                tally.tiers.merge(tier, 1, Integer::sum);
            }
        }
        if (any) {
            tally.filledChests++;
        }
    }

    private static void spawner(WorldServer world, int x, int y, int z, String drawn, boolean enderZoo, Tally tally) {
        if (world.getBlock(x, y, z) != Blocks.mob_spawner) {
            tally.missingSpawners++;
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityMobSpawner spawner)) {
            tally.missingSpawners++;
            return;
        }
        String name = spawner.func_145881_a()
            .getEntityNameToSpawn();
        tally.spawners++;
        tally.mobs.merge(name, 1, Integer::sum);
        String expected = SpawnerMobs.resolve(drawn);
        boolean allowed = UPSTREAM_MOBS.contains(name) || enderZoo && LootRules.ENDERZOO_HARDMOBS.contains(name);
        Entity entity = EntityList.createEntityByName(name, world);
        if (!name.equals(expected) || !allowed || entity == null) {
            if (tally.badSpawners.size() < CityScenarios.EXAMPLES) {
                tally.badSpawners.add(
                    name + " (planned "
                        + drawn
                        + ", creates "
                        + (entity == null ? "nothing"
                            : entity.getClass()
                                .getSimpleName())
                        + ") at "
                        + x
                        + ","
                        + y
                        + ","
                        + z);
            }
        }
    }

    private static void ores(Chunk chunk, ChunkPlan plan, Tally tally) {
        int ground = plan.groundY();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 1; y < 256; y++) {
                    Block block = chunk.getBlock(lx, y, lz);
                    if (y >= ground && plan.hasBuilding() && isRawGtStone(block, chunk.getBlockMetadata(lx, y, lz))) {
                        tally.boulderBlocksInBuildings++;
                        continue;
                    }
                    if (!isGtOre(block)) {
                        continue;
                    }
                    if (y < ground) {
                        tally.oresBelow++;
                    } else if (plan.hasBuilding()) {
                        if (besideRawGtStone(chunk, lx, y, lz)) {
                            // Ore in a GT boulder that cut into the building: boulder damage (ore-veins.md option A).
                            tally.oresInBoulders++;
                            continue;
                        }
                        tally.oresInBuildings++;
                        if (tally.oreExamples.size() < CityScenarios.EXAMPLES) {
                            tally.oreExamples.add(oreExample(chunk, plan, lx, y, lz));
                        }
                    } else {
                        tally.oresAboveOpen++;
                    }
                }
            }
        }
    }

    /**
     * Whether a block is raw GT granite, marble or basalt (metas 0 and 8 of {@code gt.blockgranites} and
     * {@code gt.blockstones}): the stone GT's boulders are made of, which no palette places (D28 rule 7, ore-veins.md).
     */
    static boolean isRawGtStone(Block block, int meta) {
        if (meta != 0 && meta != 8) {
            return false;
        }
        Object name = Block.blockRegistry.getNameForObject(block);
        return "gregtech:gt.blockgranites".equals(String.valueOf(name))
            || "gregtech:gt.blockstones".equals(String.valueOf(name));
    }

    private static boolean besideRawGtStone(Chunk chunk, int lx, int y, int lz) {
        int[][] offsets = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int[] offset : offsets) {
            int x = lx + offset[0];
            int yy = y + offset[1];
            int z = lz + offset[2];
            if (x >= 0 && x < 16
                && z >= 0
                && z < 16
                && yy > 0
                && yy < 256
                && isRawGtStone(chunk.getBlock(x, yy, z), chunk.getBlockMetadata(x, yy, z))) {
                return true;
            }
        }
        return false;
    }

    /** Describes an ore found above the city ground: where, the plan's facts there, and the blocks around it. */
    private static String oreExample(Chunk chunk, ChunkPlan plan, int lx, int y, int lz) {
        StringBuilder out = new StringBuilder();
        out.append((chunk.xPosition << 4) + lx)
            .append(',')
            .append(y)
            .append(',')
            .append((chunk.zPosition << 4) + lz)
            .append(" ground ")
            .append(plan.groundY())
            .append(" floors ")
            .append(plan.floors())
            .append(" footprintBottom ")
            .append(plan.hasFootprint() ? plan.footprintBottom(lx, lz) : -1)
            .append(" ruin ")
            .append(plan.ruinHeight())
            .append(" around");
        int[][] offsets = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int[] offset : offsets) {
            int x = lx + offset[0];
            int z = lz + offset[2];
            int yy = y + offset[1];
            if (x < 0 || x > 15 || z < 0 || z > 15 || yy < 0 || yy > 255) {
                continue;
            }
            out.append(' ')
                .append(Block.blockRegistry.getNameForObject(chunk.getBlock(x, yy, z)))
                .append(':')
                .append(chunk.getBlockMetadata(x, yy, z));
        }
        return out.toString();
    }

    /** Whether a block is a GregTech ore block: GT's and bartworks' ore families ({@code gt.blockores…}). */
    static boolean isGtOre(Block block) {
        if (block == Blocks.air) {
            return false;
        }
        Object name = Block.blockRegistry.getNameForObject(block);
        if (name == null) {
            return false;
        }
        String text = name.toString();
        return (text.startsWith("gregtech:") || text.startsWith("bartworks:")) && text.contains("blockores");
    }

    /** What the scenario counted. */
    static final class Tally {

        int cityChunks;
        int filledChests;
        int emptyChests;
        int missingChests;
        int items;
        int spawners;
        int missingSpawners;
        long oresBelow;
        long oresInBuildings;
        long oresAboveOpen;
        long oresInBoulders;
        long boulderBlocksInBuildings;
        final Map<String, Integer> categories = new TreeMap<>();
        final Map<LootTier, Integer> tiers = new TreeMap<>();
        final Map<String, Integer> mobs = new TreeMap<>();
        final List<String> badItems = new ArrayList<>();
        final List<String> badSpawners = new ArrayList<>();
        final List<String> oreExamples = new ArrayList<>();

        String chestLine() {
            return "chests filled " + filledChests
                + ", empty by draw "
                + emptyChests
                + ", gone "
                + missingChests
                + ", items "
                + items
                + ", categories "
                + categories
                + ", item tiers "
                + tiers;
        }

        String spawnerLine() {
            return "spawners " + spawners + " (gone " + missingSpawners + "), mobs " + mobs;
        }

        String oreLine() {
            return "GT ore blocks under the city ground " + oresBelow
                + ", above it in building chunks "
                + oresInBuildings
                + " (and "
                + oresInBoulders
                + " in GT boulders there), above it in open chunks "
                + oresAboveOpen
                + "; raw GT boulder stone above the ground in building chunks "
                + boulderBlocksInBuildings;
        }

        List<String> report() {
            List<String> out = new ArrayList<>();
            out.add("city chunks " + cityChunks);
            out.add("loot: " + chestLine() + "; bad " + badItems);
            out.add("spawners: " + spawnerLine() + "; bad " + badSpawners);
            out.add("ores: " + oreLine() + "; in buildings " + oreExamples);
            return out;
        }
    }
}
