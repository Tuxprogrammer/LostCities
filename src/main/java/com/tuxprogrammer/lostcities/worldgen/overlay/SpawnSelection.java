package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGeneratorBonusChest;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.model.PredefinedCity;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.worldgen.city.SpawnSearch;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * Honors the profile's spawn options in a new world (docs/new-spec/01-core/index.md, "Spawn selection";
 * docs/new-spec/00-pack-integration/spawn.md, D21).
 *
 * <ul>
 * <li>{@code WorldEvent.CreateSpawnPosition}, dimension 0, at {@code NORMAL} priority, after the overlay installs at
 * {@code HIGHEST}: when a spawn option is set, {@link SpawnSearch} picks the spot from the layout, the world's spawn is
 * set there and the event is canceled, which skips vanilla's own search. With no option set, nothing happens and the
 * pack's spawn is kept. A failed search logs one warning and leaves the event alone (CORE-14).</li>
 * <li>Canceling also skips vanilla's bonus chest; when the world asked for one, it is placed as vanilla places it, ten
 * tries within 5 blocks of the spawn (avoids CORE-17).</li>
 * <li>The spot is kept in {@code data/lostcitiesnh.dat}, never in a static map, so it cannot leak into another world
 * (CORE-12).</li>
 * <li>On a player's first join, and only for the building options, the player is placed exactly in the building, past
 * Forge's spawn fuzz, which would put them on its roof. "First join" is a flag in the player's persisted NBT, which
 * Forge copies across death; the world spawn is never rewritten on login (CORE-11).</li>
 * </ul>
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class SpawnSelection {

    /** The flag under the player's persisted NBT that marks the exact placement as done. */
    public static final String PLACED_KEY = "lostcitiesnh:spawnPlaced";

    /** Vanilla's bonus chest: tries, and the spread around the spawn. */
    static final int BONUS_TRIES = 10;

    static final int BONUS_SPREAD = 6;

    /** How far up and down from the planned floor the exact placement looks for standing room. */
    static final int[] FEET_OFFSETS = { 0, 1, -1, 2 };

    private static final SpawnSelection INSTANCE = new SpawnSelection();

    private SpawnSelection() {}

    /** Subscribes the handlers: the spawn event on Forge's bus, the login event on FML's. */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    /**
     * Chooses a new world's spawn when the profile asks for one.
     *
     * @param event Forge's spawn-position event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onCreateSpawnPosition(WorldEvent.CreateSpawnPosition event) {
        if (!(event.world instanceof WorldServer world) || world.provider.dimensionId != 0) {
            return;
        }
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null || overlay.cities() == null) {
            return;
        }
        SpawnSearch.Options options = SpawnSearch.Options.of(
            overlay.snapshot()
                .profileValues());
        if (!options.needsSearch()) {
            return;
        }
        List<String> problems = new ArrayList<>();
        SpawnSearch.Spot spot = new SpawnSearch(
            overlay.cities()
                .planner(),
            terrain(world, overlay),
            overlay.settings()
                .waterLevel(),
            cities()).find(options, problems);
        for (String problem : problems) {
            LostCities.LOG.warn("[spawn] {}", problem);
        }
        if (spot == null) {
            LostCities.LOG.warn("[spawn] no spawn matches the profile's spawn options; keeping the world's own spawn");
            return;
        }
        WorldInfo info = world.getWorldInfo();
        info.setSpawnPosition(spot.x(), spot.y(), spot.z());
        ProfileSavedData.get(world)
            .setSpawn(0, spot.x(), spot.y(), spot.z());
        event.setCanceled(true);
        LostCities.LOG.info("[spawn] spawn set at {}", spot);
        if (event.settings != null && event.settings.isBonusChestEnabled()) {
            placeBonusChest(world, info);
        }
    }

    /**
     * Places a player exactly at the chosen spawn on their first join, for the building options.
     *
     * @param event FML's login event
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP player) || player.dimension != 0) {
            return;
        }
        World world = player.worldObj;
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null) {
            return;
        }
        int[] spawn = ProfileSavedData.get(world)
            .spawn(0);
        if (spawn == null || !SpawnSearch.Options.of(
            overlay.snapshot()
                .profileValues())
            .placesExactly()) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(PLACED_KEY)) {
            return;
        }
        int[] spot = standingRoom(world, spawn);
        player.playerNetServerHandler
            .setPlayerLocation(spot[0] + 0.5, spot[1], spot[2] + 0.5, player.rotationYaw, player.rotationPitch);
        persisted.setBoolean(PLACED_KEY, true);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        LostCities.LOG.info(
            "[spawn] placed {} at {} {} {} on first join",
            player.getCommandSenderName(),
            spot[0],
            spot[1],
            spot[2]);
    }

    /**
     * Returns the two-high air space with a floor under it nearest the planned spot in its chunk, at the planned feet
     * height or just around it, reading the generated blocks; the planned spot when there is none.
     */
    static int[] standingRoom(World world, int[] spawn) {
        int baseX = spawn[0] & ~15;
        int baseZ = spawn[2] & ~15;
        for (int offset : FEET_OFFSETS) {
            int feet = spawn[1] + offset;
            int best = Integer.MAX_VALUE;
            int[] found = null;
            for (int dz = 0; dz < 16; dz++) {
                for (int dx = 0; dx < 16; dx++) {
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    if (!world.isAirBlock(x, feet, z) || !world.isAirBlock(x, feet + 1, z)
                        || world.isAirBlock(x, feet - 1, z)) {
                        continue;
                    }
                    int distance = (x - spawn[0]) * (x - spawn[0]) + (z - spawn[2]) * (z - spawn[2]);
                    if (distance < best) {
                        best = distance;
                        found = new int[] { x, feet, z };
                    }
                }
            }
            if (found != null) {
                return found;
            }
        }
        return spawn;
    }

    /** Vanilla 1.7.10's {@code WorldServer.createBonusChest}, which canceling the spawn event skips. */
    private static void placeBonusChest(WorldServer world, WorldInfo info) {
        Random random = world.rand;
        WorldGeneratorBonusChest generator = new WorldGeneratorBonusChest(
            ChestGenHooks.getItems(ChestGenHooks.BONUS_CHEST, random),
            ChestGenHooks.getCount(ChestGenHooks.BONUS_CHEST, random));
        for (int i = 0; i < BONUS_TRIES; i++) {
            int x = info.getSpawnX() + random.nextInt(BONUS_SPREAD) - random.nextInt(BONUS_SPREAD);
            int z = info.getSpawnZ() + random.nextInt(BONUS_SPREAD) - random.nextInt(BONUS_SPREAD);
            int y = world.getTopSolidOrLiquidBlock(x, z) + 1;
            if (generator.generate(world, random, x, y, z)) {
                LostCities.LOG.info("[spawn] bonus chest at {} {} {}", x, y, z);
                return;
            }
        }
    }

    private static SpawnSearch.Terrain terrain(WorldServer world, OverlayWorld overlay) {
        return new SpawnSearch.Terrain() {

            @Override
            public String biomeName(int x, int z) {
                BiomeGenBase biome = world.getWorldChunkManager()
                    .getBiomeGenAt(x, z);
                return biome == null ? null : biome.biomeName;
            }

            @Override
            public boolean biomeExists(String name) {
                for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
                    if (biome != null && name.equalsIgnoreCase(biome.biomeName)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int height(int x, int z) {
                return overlay.columns()
                    .height(x, z);
            }
        };
    }

    private static List<SpawnSearch.City> cities() {
        List<SpawnSearch.City> out = new ArrayList<>();
        if (!LoadedAssets.isLoaded()) {
            return out;
        }
        for (PredefinedCity city : LoadedAssets.current()
            .predefinedCities()) {
            out.add(
                new SpawnSearch.City(
                    city.id()
                        .toString(),
                    city.chunkX(),
                    city.chunkZ(),
                    city.radius()));
        }
        return out;
    }
}
