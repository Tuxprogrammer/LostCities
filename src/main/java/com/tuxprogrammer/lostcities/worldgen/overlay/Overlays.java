package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.Tags;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.config.ProfilesConfig;
import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Installs the overlay's chunk-provider wrapper and keeps one {@link OverlayWorld} per wrapped world
 * (docs/design/0005-rwg-overlay.md §1 and §7).
 *
 * <ul>
 * <li>New world: in {@code WorldEvent.CreateSpawnPosition}, at priority {@code HIGHEST} and receiving canceled events,
 * after the chunk provider exists and before the spawn search generates the first chunk. The configured profile is
 * resolved and pinned into {@code data/lostcitiesnh.dat} first.</li>
 * <li>Existing world or another dimension: in {@code WorldEvent.Load}, if not already wrapped, when the world has a
 * pinned snapshot, or {@code enableOnExistingWorlds} pins one now.</li>
 * <li>World types without a height oracle are skipped with one info line; so are dimensions with no profile.</li>
 * <li>Never a wrapper of a wrapper; the state is dropped on {@code WorldEvent.Unload}.</li>
 * </ul>
 * It also ends the footprint guard's decoration window at {@code PopulateChunkEvent.Post}, so RWG's snow passes.
 *
 * <p>
 * The world map is owned by the worlds' lifecycle: entries come with an installation and go with the unload.
 * Server thread only. Side: server.
 */
public final class Overlays {

    /** The installer; registered on the Forge bus in {@code init}. */
    public static final Overlays INSTANCE = new Overlays();

    /** JVM property that pins the synthetic test city into new worlds, honored only with the test hooks. */
    public static final String PLAN_SOURCE_PROPERTY = "lostcitiesnh.planSource";

    /** Hash pinned for the asset set while the overlay loads no assets (M02 joins in M05). */
    static final String NO_ASSETS = "none";

    private final Map<World, OverlayWorld> worlds = new IdentityHashMap<>();

    private Overlays() {}

    /** Subscribes the installer and the population vetoes. */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        MinecraftForge.TERRAIN_GEN_BUS.register(new PopulateVetoes(INSTANCE));
    }

    /**
     * Installs the wrapper on a new world before its first chunk.
     *
     * @param event Forge's spawn-position event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onCreateSpawnPosition(WorldEvent.CreateSpawnPosition event) {
        if (!(event.world instanceof WorldServer world) || worlds.containsKey(world)) {
            return;
        }
        int dimension = world.provider.dimensionId;
        String profile = profileFor(dimension);
        if (profile == null) {
            return;
        }
        ProfileSavedData data = ProfileSavedData.get(world);
        ProfileSnapshot snapshot = data.snapshot(dimension);
        if (snapshot == null) {
            snapshot = pinNew(world, data, profile);
            if (snapshot == null) {
                return;
            }
        }
        install(world, snapshot);
    }

    /**
     * Installs the wrapper on an existing world, or on a dimension loaded after the overworld.
     *
     * @param event Forge's world-load event
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!(event.world instanceof WorldServer world) || worlds.containsKey(world)) {
            return;
        }
        int dimension = world.provider.dimensionId;
        String profile = profileFor(dimension);
        if (profile == null) {
            return;
        }
        ProfileSavedData data = ProfileSavedData.get(world);
        ProfileSnapshot snapshot = data.snapshot(dimension);
        if (snapshot == null) {
            if (!ProfilesConfig.enableOnExistingWorlds) {
                LostCities.LOG.info(
                    "Dimension {} has no pinned profile, so no cities generate there; "
                        + "'/lostcities profile pin <profile> confirm' or enableOnExistingWorlds opts it in",
                    dimension);
                return;
            }
            snapshot = pinNew(world, data, profile);
            if (snapshot == null) {
                return;
            }
            LostCities.LOG.warn(
                "Dimension {} was created before {} and now has cities; chunks generated before will not match",
                dimension,
                LostCities.NAME);
        }
        install(world, snapshot);
    }

    /**
     * Drops a world's state when it unloads.
     *
     * @param event Forge's world-unload event
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        OverlayWorld overlay = worlds.remove(event.world);
        if (overlay != null) {
            for (String line : overlay.timers()
                .report()) {
                LostCities.LOG.info("[timers dim {}] {}", overlay.dimension(), line);
            }
            overlay.close();
        }
    }

    /**
     * Ends the footprint guard's decoration window: RWG adds snow and ice after this event, and they pass.
     *
     * @param event Forge's post-population event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPopulatePost(PopulateChunkEvent.Post event) {
        if (worlds.containsKey(event.world)) {
            FootprintGuard.endOfDecoration(event.world);
        }
    }

    /**
     * Returns a world's overlay state.
     *
     * @param world the world
     * @return the state, or {@code null} when the world is not wrapped
     */
    public OverlayWorld get(World world) {
        return worlds.get(world);
    }

    /** Returns every wrapped world's state. */
    public List<OverlayWorld> all() {
        return new ArrayList<>(worlds.values());
    }

    /**
     * Pins a profile into a world now and installs (or reinstalls) the wrapper: the operator command's opt-in for an
     * existing world, and its explicit re-pin (0005 §7).
     *
     * @param world   the world
     * @param profile the profile's name
     * @return the snapshot pinned, or {@code null} when the world type has no oracle
     * @throws IllegalArgumentException for an unknown profile
     */
    public ProfileSnapshot pin(WorldServer world, String profile) {
        ProfileSnapshot snapshot = ProfileSnapshot.resolve(
            profile,
            planSource(),
            ModuleState.configured(),
            NO_ASSETS,
            HeightOracles.worldType(world),
            Tags.VERSION);
        if (!HeightOracles.supports(world)) {
            return null;
        }
        uninstall(world);
        ProfileSavedData.get(world)
            .pin(world.provider.dimensionId, snapshot);
        LostCities.LOG.warn(
            "Pinned profile {} into dimension {}; chunks generated before will not match new ones",
            snapshot.summary(),
            world.provider.dimensionId);
        install(world, snapshot);
        return snapshot;
    }

    /** Clears every world's state; the server stopped. */
    public void reset() {
        for (OverlayWorld overlay : worlds.values()) {
            overlay.close();
        }
        worlds.clear();
        FootprintGuard.reset();
    }

    private ProfileSnapshot pinNew(WorldServer world, ProfileSavedData data, String profile) {
        if (!HeightOracles.supports(world)) {
            LostCities.LOG.info(
                "World type {} of dimension {} has no height oracle; no cities generate there",
                HeightOracles.worldType(world),
                world.provider.dimensionId);
            return null;
        }
        ProfileSnapshot snapshot;
        try {
            snapshot = ProfileSnapshot.resolve(
                profile,
                planSource(),
                ModuleState.configured(),
                NO_ASSETS,
                HeightOracles.worldType(world),
                Tags.VERSION);
        } catch (IllegalArgumentException e) {
            LostCities.LOG.error(
                "dimensionProfiles names {} for dimension {}: {}",
                profile,
                world.provider.dimensionId,
                e.getMessage());
            return null;
        }
        data.pin(world.provider.dimensionId, snapshot);
        LostCities.LOG.info("Pinned profile {} into dimension {}", snapshot.summary(), world.provider.dimensionId);
        return snapshot;
    }

    private void install(WorldServer world, ProfileSnapshot snapshot) {
        ChunkProviderServer server = world.theChunkProviderServer;
        if (server.currentChunkProvider instanceof OverlayChunkProvider) {
            return;
        }
        TerrainHeightOracle oracle = HeightOracles.create(world);
        if (oracle == null) {
            LostCities.LOG.info(
                "World type {} of dimension {} has no height oracle; the overlay is not installed",
                HeightOracles.worldType(world),
                world.provider.dimensionId);
            return;
        }
        if (snapshot.layoutVersion() != ProfileSnapshot.LAYOUT_VERSION) {
            LostCities.LOG.warn(
                "Dimension {} was pinned with layout version {}, this version has {}: new chunks may not match",
                world.provider.dimensionId,
                snapshot.layoutVersion(),
                ProfileSnapshot.LAYOUT_VERSION);
        }
        int before = server.getLoadedChunkCount();
        IChunkProvider delegate = server.currentChunkProvider;
        OverlayWorld overlay = new OverlayWorld(world, snapshot, oracle, before);
        server.currentChunkProvider = new OverlayChunkProvider(delegate, overlay);
        worlds.put(world, overlay);
        LostCities.LOG.info(
            "Overlay installed in dimension {} over {} with {}; {} chunk(s) were loaded before",
            world.provider.dimensionId,
            OverlayWorld.describe(delegate),
            snapshot.summary(),
            before);
    }

    private void uninstall(WorldServer world) {
        OverlayWorld overlay = worlds.remove(world);
        ChunkProviderServer server = world.theChunkProviderServer;
        if (server.currentChunkProvider instanceof OverlayChunkProvider wrapper) {
            server.currentChunkProvider = wrapper.delegate();
        }
        if (overlay != null) {
            overlay.close();
        }
    }

    /** The plan source to pin: the synthetic test city only when asked for with the test hooks on. */
    static String planSource() {
        if (DevEnvironment.testHooksEnabled()
            && ProfileSnapshot.PLAN_SYNTHETIC.equals(System.getProperty(PLAN_SOURCE_PROPERTY))) {
            return ProfileSnapshot.PLAN_SYNTHETIC;
        }
        return ProfileSnapshot.PLAN_LAYOUT;
    }

    /**
     * Returns the profile {@code dimensionProfiles} names for a dimension.
     *
     * @param dimension the dimension ID
     * @return the profile's name, or {@code null} when the dimension is not listed
     */
    static String profileFor(int dimension) {
        for (String entry : ProfilesConfig.dimensionProfiles) {
            int equals = entry == null ? -1 : entry.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            try {
                if (Integer.parseInt(
                    entry.substring(0, equals)
                        .trim())
                    == dimension) {
                    String profile = entry.substring(equals + 1)
                        .trim();
                    return profile.isEmpty() ? BuiltinProfiles.DEFAULT : profile;
                }
            } catch (NumberFormatException e) {
                LostCities.LOG.warn("dimensionProfiles entry '{}' has no dimension number; ignored", entry);
            }
        }
        return null;
    }
}
