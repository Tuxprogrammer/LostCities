package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: LOOT-05, ASSET-05, TERR-09

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.model.ConditionContext;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPass;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomFit;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomKind;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * The dry run's {@link CityTarget}: writes nothing and lists the chunk's population jobs: torch fix-ups, chests with
 * their loot category, spawners with their mob, and tile entities with their templates (upstream's
 * {@code handleLoot}, {@code handleSpawner}, {@code handleBlockEntity} and torch todos, docs/original-spec/03-terrain/
 * chunk-driver-and-parts.md step 4.6).
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids LOOT-05: each chest's and spawner's draws come from a random seeded by the world seed and the block's own
 * position, so no other job, vine or map order can change them;</li>
 * <li>avoids ASSET-05: the condition's floor counts with floor division, so a cellar's chest is on floor −1 and
 * below;</li>
 * <li>avoids TERR-09: every job is this chunk's, at this chunk's positions, front parts included.</li>
 * </ul>
 * The loot condition and the chest-without-loot draw are taken here, at plan time, so the population phase only
 * applies the result.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class JobTarget implements CityTarget {

    /** Mixes a block position into the chest and spawner seeds. */
    private static final long POS_X = 341873128712L;
    private static final long POS_Z = 132897987541L;
    private static final long POS_Y = 2654435761L;

    /** What the jobs of one chunk depend on beyond the parts. */
    public static final class Facts {

        final long worldSeed;
        final int chunkX;
        final int chunkZ;
        final int groundLevel;
        final int cityGround;
        final int cellars;
        final int floors;
        final BiomeDescriptor biome;
        final boolean noLoot;
        final boolean generateLoot;
        final float chestWithoutLootChance;
        final boolean vanillaLoot;
        final boolean enderZooHardMobs;
        final int floorPitch;
        final boolean roomRules;

        /**
         * Collects the facts.
         *
         * @param worldSeed              the world seed
         * @param chunkX                 chunk X
         * @param chunkZ                 chunk Z
         * @param groundLevel            the profile's ground level, block Y
         * @param cityGround             the chunk's city ground, block Y
         * @param cellars                the building's cellars, 0 without one
         * @param floors                 the building's floors, 0 without one
         * @param biome                  the chunk's main biome
         * @param noLoot                 whether the building has no loot
         * @param generateLoot           the profile's {@code generateLoot}
         * @param chestWithoutLootChance the profile's {@code chestWithoutLootChance}
         */
        public Facts(long worldSeed, int chunkX, int chunkZ, int groundLevel, int cityGround, int cellars, int floors,
            BiomeDescriptor biome, boolean noLoot, boolean generateLoot, float chestWithoutLootChance) {
            this.worldSeed = worldSeed;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.groundLevel = groundLevel;
            this.cityGround = cityGround;
            this.cellars = cellars;
            this.floors = floors;
            this.biome = biome;
            this.noLoot = noLoot;
            this.generateLoot = generateLoot;
            this.chestWithoutLootChance = chestWithoutLootChance;
            this.vanillaLoot = false;
            this.enderZooHardMobs = false;
            this.floorPitch = ConditionContext.FLOOR_HEIGHT;
            this.roomRules = false;
        }

        private Facts(Facts base, boolean vanillaLoot, boolean enderZooHardMobs, int floorPitch, boolean roomRules) {
            this.worldSeed = base.worldSeed;
            this.chunkX = base.chunkX;
            this.chunkZ = base.chunkZ;
            this.groundLevel = base.groundLevel;
            this.cityGround = base.cityGround;
            this.cellars = base.cellars;
            this.floors = base.floors;
            this.biome = base.biome;
            this.noLoot = base.noLoot;
            this.generateLoot = base.generateLoot;
            this.chestWithoutLootChance = base.chestWithoutLootChance;
            this.vanillaLoot = vanillaLoot;
            this.enderZooHardMobs = enderZooHardMobs;
            this.floorPitch = floorPitch;
            this.roomRules = roomRules;
        }

        /**
         * Returns these facts with the profile's loot and mob options (docs/new-spec/08-loot-mobs-blocks/index.md).
         *
         * @param vanillaLoot      the profile's {@code vanillaLootCategories}: whether chests may draw the 1.7.10
         *                         vanilla categories
         * @param enderZooHardMobs the profile's {@code enderZooHardMobs}: whether {@code hardmobs} spawners take
         *                         EnderZoo's overworld mobs
         * @return the facts with the options
         */
        public Facts withOptions(boolean vanillaLoot, boolean enderZooHardMobs) {
            return new Facts(this, vanillaLoot, enderZooHardMobs, floorPitch, roomRules);
        }

        /**
         * Returns these facts with the world's floor pitch, which the {@code floor} condition counts in (0014); 6, the
         * legacy pitch, unless set.
         *
         * @param pitch blocks per building floor
         * @return the facts with the pitch
         */
        public Facts withFloorPitch(int pitch) {
            return new Facts(this, vanillaLoot, enderZooHardMobs, pitch, roomRules);
        }

        /**
         * Returns these facts with the world's {@code roomRules}: chests and spawners keep to fitting rooms and are
         * thinned (design record 0016).
         *
         * @param on whether the room rules apply
         * @return the facts with the rules
         */
        public Facts withRoomRules(boolean on) {
            return new Facts(this, vanillaLoot, enderZooHardMobs, floorPitch, on);
        }
    }

    private final Facts facts;
    private final List<PopulationJob> jobs = new ArrayList<>();

    /**
     * Creates the dry run of one chunk.
     *
     * @param facts what the jobs depend on
     */
    public JobTarget(Facts facts) {
        this.facts = facts;
    }

    /** Returns the jobs listed, in order. */
    public List<PopulationJob> jobs() {
        return Collections.unmodifiableList(jobs);
    }

    @Override
    public boolean dryRun() {
        return true;
    }

    @Override
    public BlockRef get(int x, int y, int z) {
        return BlockRef.AIR;
    }

    @Override
    public void set(int x, int y, int z, BlockRef block) {}

    @Override
    public void clearToMax(int fromY) {}

    @Override
    public void torch(int x, int y, int z) {
        if (inChunk(x, y, z)) {
            jobs.add(PopulationJob.of(PopulationJob.Kind.TORCH, x, y, z, ""));
        }
    }

    @Override
    public void chest(int x, int y, int z, PaletteEntry info, TileTemplate template, Transform transform,
        PartWriter.Placement placement) {
        if (!inChunk(x, y, z)) {
            return;
        }
        String category = "";
        // A barrel is storage clutter, not a room's loot chest: the room rules thin chests only (D-barrel-loot-1).
        boolean stays = isBarrel(info) || !facts.roomRules || roomAllows(x, y, z, placement, true);
        if (!stays) {
            // The terrain phase wrote the chest block; the population phase takes it out again (0016, decision 11).
            jobs.add(PopulationJob.of(PopulationJob.Kind.CLEAR_CHEST, x, y, z, ""));
            return;
        }
        if (!facts.noLoot && facts.generateLoot) {
            Random random = positionRandom(x, y, z);
            if (random.nextFloat() >= facts.chestWithoutLootChance) {
                String value = info.loot()
                    .randomValue(random, context(y, placement), v -> LootRules.allowed(v, facts.vanillaLoot));
                category = LootRules.category(value, facts.vanillaLoot, random);
            }
        }
        jobs.add(
            PopulationJob.of(PopulationJob.Kind.CHEST, x, y, z, category)
                .withTemplate(TileTemplates.rotated(template, transform)));
    }

    /** Whether a palette entry is upstream's barrel, which keeps its loot but is not thinned by the room rules. */
    private static boolean isBarrel(PaletteEntry info) {
        return info.block() != null && info.block()
            .source()
            .startsWith("minecraft:barrel");
    }

    @Override
    public void spawner(int x, int y, int z, PaletteEntry info, PartWriter.Placement placement) {
        if (!inChunk(x, y, z)) {
            return;
        }
        if (facts.roomRules && !roomAllows(x, y, z, placement, false)) {
            // An empty mob makes the population phase clear the spawner block.
            jobs.add(PopulationJob.of(PopulationJob.Kind.SPAWNER, x, y, z, ""));
            return;
        }
        Random random = positionRandom(x, y, z);
        String mob = LootRules.entityName(
            info.mob()
                .randomValue(random, context(y, placement)));
        if (facts.enderZooHardMobs && isHardMobs(info)) {
            // Drawn after the condition, from the spawner's own random, so the option moves no other draw.
            mob = LootRules.ENDERZOO_HARDMOBS.get(random.nextInt(LootRules.ENDERZOO_HARDMOBS.size()));
        }
        jobs.add(PopulationJob.of(PopulationJob.Kind.SPAWNER, x, y, z, mob));
    }

    @Override
    public void tileEntity(int x, int y, int z, TileTemplate template, Transform transform) {
        if (inChunk(x, y, z)) {
            jobs.add(
                PopulationJob.of(PopulationJob.Kind.TILE_ENTITY, x, y, z, "")
                    .withTemplate(TileTemplates.rotated(template, transform)));
        }
    }

    @Override
    public void furnish(int y, String argument) {
        if (inChunk(0, y, 0)) {
            jobs.add(PopulationJob.of(PopulationJob.Kind.FURNISH, 0, y, 0, argument));
        }
    }

    /**
     * Whether the room rules let a part's chest or spawner stay (design record 0016, decision 11): the floor must be a
     * fitting room kind and the position must survive the thinning. Below the city ground, outside a building, the
     * cellar and corridor rule applies. Rail dungeon chests are not part of any room and stay unfiltered.
     */
    private boolean roomAllows(int x, int y, int z, PartWriter.Placement placement, boolean chest) {
        long worldX = (facts.chunkX << 4) + x;
        long worldZ = (facts.chunkZ << 4) + z;
        float keep = chest ? RoomFit.CHEST_KEEP : RoomFit.SPAWNER_KEEP;
        if (!RoomFit.survives(facts.worldSeed, worldX, y, worldZ, keep)) {
            return false;
        }
        int floor = ConditionContext.floorOf(y, facts.cityGround, facts.floorPitch);
        AssetId building = placement.building();
        RoomKind kind;
        if (building == null || facts.floors + facts.cellars == 0) {
            kind = floor < 0 ? RoomKind.BASEMENT : null;
        } else if (floor >= facts.floors) {
            kind = null;
        } else {
            long salt = FurnishPass.floorSalt(facts.worldSeed, facts.chunkX, facts.chunkZ, floor);
            kind = RoomKind.forFloor(building.path(), floor, salt, true);
        }
        if (kind == null) {
            return false;
        }
        return chest ? RoomFit.lootFits(kind) : RoomFit.spawnerFits(kind);
    }

    /** Whether a spawner's condition is upstream's {@code hardmobs}, which the EnderZoo option replaces. */
    private static boolean isHardMobs(PaletteEntry info) {
        return "hardmobs".equals(
            info.mob()
                .id()
                .path());
    }

    private ConditionContext context(int y, PartWriter.Placement placement) {
        return ConditionContext.builder()
            .level(Math.floorDiv(y - facts.groundLevel, ConditionContext.LEVEL_HEIGHT))
            .floor(ConditionContext.floorOf(y, facts.cityGround, facts.floorPitch))
            .floors(facts.cellars, facts.floors)
            .part(
                placement.part()
                    .id())
            .building(placement.building())
            .chunk(facts.chunkX, facts.chunkZ)
            .biome(facts.biome)
            .build();
    }

    private Random positionRandom(int x, int y, int z) {
        long worldX = (facts.chunkX << 4) + x;
        long worldZ = (facts.chunkZ << 4) + z;
        return new Random(SeedMix.mix(facts.worldSeed, worldX * POS_X + worldZ * POS_Z + y * POS_Y));
    }

    private static boolean inChunk(int x, int y, int z) {
        return x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 256;
    }
}
