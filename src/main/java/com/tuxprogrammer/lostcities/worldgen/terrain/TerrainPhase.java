package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Arrays;
import java.util.Random;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.damage.ExplosionDamage;
import com.tuxprogrammer.lostcities.worldgen.damage.FloatingFragments;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkContent;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ContentContext;

/**
 * The overlay's terrain phase for one chunk, as pure Java over a {@link ChunkBuffer}: what runs after RWG's
 * {@code provideChunk} returns and before the chunk joins the world (docs/design/0005-rwg-overlay.md §2,
 * docs/new-spec/03-terrain/pipeline-on-rwg.md "Stage map"). The world glue backs the buffer with the chunk's sections
 * and recomputes the height map and sky light afterwards; tests back it with an array.
 *
 * <p>
 * Order, for a chunk C:
 * <ol>
 * <li>Every input is read first: C's plan, its neighbors' plans and the damage areas. A chunk with no city or landmark
 * within three chunks passes through untouched.</li>
 * <li>A landmark core gets only its village lane's surface path, and nothing else (0006 §1).</li>
 * <li>A city chunk: high water, leveling to the city ground (soft-edge strips toward cores), room for the building,
 * the plan's content, the street base and borders, the ruin of a ruined building, the street rubble layer, the
 * retaining walls of steep strips, a lane.</li>
 * <li>Any other chunk: the blend toward nearby cities and cores, then, beside a core (the city's margin), the strip
 * that meets the core's own edge columns; the plan's content (a bridge span), and a village lane with its steps.</li>
 * <li>In each of those, the highways, railways and rail dungeon of the chunk last (M06), so nothing the city or the
 * blend writes cuts them; in a core only a railway deep enough to pass under it.</li>
 * <li>Explosion damage, the floating-fragment pass, debris from damaged neighbors, then vines (stages 10 and
 * 13a).</li>
 * </ol>
 *
 * <p>
 * One instance per world; not thread-safe. Side: both (plain Java).
 */
public final class TerrainPhase {

    /** How the phase treated a chunk, for the timers' split (0005 §8). */
    public enum ChunkClass {
        /** A city chunk. */
        CITY,
        /** A chunk the overlay changed that is not a city chunk: blended, damaged, given debris, vines or a lane. */
        BLEND,
        /** A chunk the overlay left exactly as RWG made it. */
        UNTOUCHED
    }

    /** What one call did. */
    public static final class Result {

        private final ChunkClass chunkClass;
        private final int damaged;
        private final int fragments;
        private final int debris;
        private final int vines;
        private final int lane;

        Result(ChunkClass chunkClass, int damaged, int fragments, int debris, int vines, int lane) {
            this.chunkClass = chunkClass;
            this.damaged = damaged;
            this.fragments = fragments;
            this.debris = debris;
            this.vines = vines;
            this.lane = lane;
        }

        /** Returns how the chunk was treated. */
        public ChunkClass chunkClass() {
            return chunkClass;
        }

        /** Returns the blocks explosion damage changed. */
        public int damaged() {
            return damaged;
        }

        /** Returns the blocks the floating-fragment pass removed or moved. */
        public int fragments() {
            return fragments;
        }

        /** Returns the debris blocks placed. */
        public int debris() {
            return debris;
        }

        /** Returns the vine blocks placed. */
        public int vines() {
            return vines;
        }

        /** Returns the lane columns paved. */
        public int lane() {
            return lane;
        }
    }

    private static final Result UNTOUCHED = new Result(ChunkClass.UNTOUCHED, 0, 0, 0, 0, 0);

    /** How far the phase looks for a city or core before deciding a chunk is untouched. */
    static final int REACH = TerrainBlend.REACH;

    /** Size of the damage-area cache: neighbors share most of their areas' inputs. */
    private static final int AREA_CACHE = 256;

    private static final long CONTENT_X = 341873128712L;
    private static final long CONTENT_Z = 132897987541L;
    /** "NETWORKS": the network random's salt, so its picks never follow the content random's. */
    private static final long NETWORK_SALT = 0x4e4554574f524b53L;

    private final TerrainContext context;
    private final CityPlan cityPlan;
    private final TerrainBlend blend;
    private final ExplosionDamage damage;
    private final FloatingFragments fragments;
    private final Ruins ruins;
    private final int damageReach;

    private final long[] areaKeys = new long[AREA_CACHE];
    private final DamageArea[] areas = new DamageArea[AREA_CACHE];

    private final int[] targets = new int[256];
    private final int[] depth = new int[256];
    private final int[] inner = new int[256];
    private final boolean[] skip = new boolean[256];
    private final int[][] coreEdges = new int[4][16];

    /**
     * Creates the terrain phase of one world.
     *
     * @param context  the world's terrain context
     * @param cityPlan the plan source, cached
     */
    public TerrainPhase(TerrainContext context, CityPlan cityPlan) {
        this.context = context;
        this.cityPlan = cityPlan;
        this.blend = new TerrainBlend(context);
        this.damage = new ExplosionDamage(context);
        this.fragments = new FloatingFragments(context);
        this.ruins = new Ruins(context);
        this.damageReach = DamageArea.searchRange(context.settings());
    }

    /** Returns the plan source. */
    public CityPlan cityPlan() {
        return cityPlan;
    }

    /**
     * Runs the terrain phase on one chunk.
     *
     * @param buffer the chunk, as RWG finished it
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return what was done
     */
    public Result run(ChunkBuffer buffer, int chunkX, int chunkZ) {
        ChunkPlan plan = cityPlan.chunk(chunkX, chunkZ);
        if (plan.isCore()) {
            int paved = plan.lane() == null ? 0 : VillageLanes.lay(buffer, context, chunkX, chunkZ, plan.lane());
            // A railway deep enough to pass under the core (D30, D31); nothing else the city makes.
            boolean rail = network(buffer, chunkX, chunkZ, plan);
            return paved > 0 || rail ? new Result(ChunkClass.BLEND, 0, 0, 0, 0, paved) : UNTOUCHED;
        }
        if (!plan.isCity() && plan.lane() == null
            && plan.network() == ChunkContent.NONE
            && !nearCityOrCore(chunkX, chunkZ)) {
            return UNTOUCHED;
        }
        ChunkPlan[] sides = sides(chunkX, chunkZ);
        DamageArea area = damageArea(chunkX, chunkZ);
        DamageArea[] sideAreas = new DamageArea[4];
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            sideAreas[side.ordinal()] = damageArea(chunkX + dx(side), chunkZ + dz(side));
        }
        ChunkPlan[] debrisNeighbors = new ChunkPlan[8];
        DamageArea[] debrisAreas = new DamageArea[8];
        for (int i = 0; i < 8; i++) {
            int[] offset = ExplosionDamage.NEIGHBOR_OFFSETS[i];
            debrisNeighbors[i] = cityPlan.chunk(chunkX + offset[0], chunkZ + offset[1]);
            debrisAreas[i] = debrisNeighbors[i].hasBuilding() ? damageArea(chunkX + offset[0], chunkZ + offset[1])
                : null;
        }

        boolean changed = false;
        int paved = 0;
        if (plan.isCity()) {
            cityChunk(buffer, chunkX, chunkZ, plan, sides);
            changed = true;
        } else {
            boolean facesCore = SoftEdges.recordCoreEdges(buffer, context.classifier(), sides, coreEdges);
            changed = blend.apply(buffer, chunkX, chunkZ, cityPlan);
            if (changed && facesCore) {
                // The margin: the blend sloped it from the city toward the core's oracle height; the strip meets the
                // core's real edge columns (AR-10, AR-13).
                SoftEdges.stitchToCores(buffer, context, chunkX, chunkZ, coreEdges);
            }
            if (plan.content() != ChunkContent.NONE) {
                plan.content()
                    .write(
                        buffer,
                        new ContentContext(
                            chunkX,
                            chunkZ,
                            plan,
                            cityPlan,
                            context,
                            contentRandom(chunkX, chunkZ),
                            null));
                changed = true;
            }
            changed |= network(buffer, chunkX, chunkZ, plan);
        }
        if (plan.lane() != null) {
            if (!plan.isCity()) {
                changed |= VillageLanes.grade(buffer, context, plan.lane(), coreEdges) > 0;
            }
            paved = VillageLanes.lay(buffer, context, chunkX, chunkZ, plan.lane());
            changed |= paved > 0;
        }

        Random random = damage.damageRandom(chunkX, chunkZ);
        int damaged = 0;
        int moved = 0;
        if (area.hasExplosions()) {
            damaged = damage.breakBlocks(buffer, area, plan, random);
            moved = fragments.apply(buffer, area, plan, sides, random);
        }
        int debris = damage.debris(buffer, plan, debrisNeighbors, debrisAreas, random);
        int vines = Vines
            .grow(buffer, context, plan, sides, sideAreas, Vines.random(context.worldSeed(), chunkX, chunkZ));
        changed |= damaged > 0 || moved > 0 || debris > 0 || vines > 0;
        if (!changed) {
            return UNTOUCHED;
        }
        return new Result(plan.isCity() ? ChunkClass.CITY : ChunkClass.BLEND, damaged, moved, debris, vines, paved);
    }

    private void cityChunk(ChunkBuffer buffer, int chunkX, int chunkZ, ChunkPlan plan, ChunkPlan[] sides) {
        CityTerrain.highWater(buffer, context, plan);
        boolean strips = SoftEdges.anyCoreSide(sides[0], sides[1], sides[2], sides[3]);
        if (strips) {
            SoftEdges.targets(buffer, context, chunkX, chunkZ, plan, sides, targets, depth, inner);
            for (int i = 0; i < 256; i++) {
                skip[i] = depth[i] != SoftEdges.NOT_IN_STRIP;
            }
        } else {
            Arrays.fill(targets, plan.groundY());
        }
        CityTerrain.level(buffer, context, targets);
        boolean[] skipMask = strips ? skip : null;
        if (plan.hasBuilding()) {
            CityTerrain.makeRoomForBuilding(buffer, context, plan, sides, skipMask);
        }
        plan.content()
            .write(
                buffer,
                new ContentContext(chunkX, chunkZ, plan, cityPlan, context, contentRandom(chunkX, chunkZ), skipMask));
        if (!plan.hasBuilding()) {
            CityTerrain.streetBaseAndBorders(buffer, context, plan, sides, true, skipMask);
        }
        Random ruinRandom = ruins.random(chunkX, chunkZ);
        ruins.ruin(buffer, chunkX, chunkZ, plan, ruinRandom, skipMask);
        if (!plan.hasBuilding() || plan.ruinHeight() >= 0) {
            ruins.rubble(buffer, chunkX, chunkZ, plan, around(chunkX, chunkZ), ruinRandom, skipMask);
        }
        if (strips) {
            SoftEdges.retainingWalls(buffer, plan, targets, depth, inner);
        }
        network(buffer, chunkX, chunkZ, plan);
    }

    /**
     * Writes the chunk's highways, railways and rail dungeon (M06), after everything the city or the blend wrote, so
     * no later step cuts them.
     *
     * @return whether anything was written
     */
    private boolean network(ChunkBuffer buffer, int chunkX, int chunkZ, ChunkPlan plan) {
        if (plan.network() == ChunkContent.NONE) {
            return false;
        }
        plan.network()
            .write(
                buffer,
                new ContentContext(chunkX, chunkZ, plan, cityPlan, context, networkRandom(chunkX, chunkZ), null));
        return true;
    }

    /**
     * Returns whether any city or landmark core lies within reach, so the blend, damage or debris may touch the
     * chunk. With explosions allowed outside cities, every chunk may be damaged.
     */
    private boolean nearCityOrCore(int chunkX, int chunkZ) {
        if (!context.settings()
            .explosionsInCitiesOnly()) {
            return true;
        }
        int reach = Math.max(REACH, damageReach);
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                if (cityPlan.chunk(chunkX + dx, chunkZ + dz)
                    .kind() != ChunkKind.NONE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the seed of a chunk's content random: the random a {@link ContentContext} carries, from which a writer
     * picks its street parts. Exposed so a plan's job dry run draws the same picks (TERR-27: it mixes in the world
     * seed).
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return the seed
     */
    public static long contentSeed(long worldSeed, int chunkX, int chunkZ) {
        return SeedMix.chunk(worldSeed, chunkX, chunkZ, CONTENT_X, CONTENT_Z);
    }

    /**
     * Returns the seed of a chunk's network random, from which highway and railway parts are picked out of lists
     * longer than one; the bundled lists have one part each, so the bundled assets draw nothing from it.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return the seed
     */
    public static long networkSeed(long worldSeed, int chunkX, int chunkZ) {
        return SeedMix.chunk(worldSeed ^ NETWORK_SALT, chunkX, chunkZ, CONTENT_X, CONTENT_Z);
    }

    private Random networkRandom(int chunkX, int chunkZ) {
        return new Random(networkSeed(context.worldSeed(), chunkX, chunkZ));
    }

    private Random contentRandom(int chunkX, int chunkZ) {
        return new Random(contentSeed(context.worldSeed(), chunkX, chunkZ));
    }

    private ChunkPlan[] around(int chunkX, int chunkZ) {
        ChunkPlan[] around = new ChunkPlan[9];
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                around[(dz + 1) * 3 + dx + 1] = cityPlan.chunk(chunkX + dx, chunkZ + dz);
            }
        }
        return around;
    }

    private ChunkPlan[] sides(int chunkX, int chunkZ) {
        ChunkPlan[] sides = new ChunkPlan[4];
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            sides[side.ordinal()] = cityPlan.chunk(chunkX + dx(side), chunkZ + dz(side));
        }
        return sides;
    }

    /**
     * Returns the damage area of a chunk, from a small cache: neighbors' areas are read again by the next chunks.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the area
     */
    public DamageArea damageArea(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        int slot = ((chunkX * 31) ^ (chunkZ * 17)) & (AREA_CACHE - 1);
        DamageArea area = areas[slot];
        if (area != null && areaKeys[slot] == key) {
            return area;
        }
        area = DamageArea.of(context.worldSeed(), chunkX, chunkZ, context.settings(), cityPlan);
        areas[slot] = area;
        areaKeys[slot] = key;
        return area;
    }

    static int dx(ChunkPlan.Side side) {
        return side == ChunkPlan.Side.WEST ? -1 : side == ChunkPlan.Side.EAST ? 1 : 0;
    }

    static int dz(ChunkPlan.Side side) {
        return side == ChunkPlan.Side.NORTH ? -1 : side == ChunkPlan.Side.SOUTH ? 1 : 0;
    }
}
