package com.tuxprogrammer.lostcities.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Clean screenshot capture for the alpha review set (tools/remote/README.md, "Shot lists"): a dev-only client mode,
 * on with {@code -Dlostcitiesnh.testHooks=true -Dlostcitiesnh.capture=true} on the client JVM, that keeps the frame
 * free of everything but the world and tells the shot runner when a viewpoint is ready.
 *
 * <ul>
 * <li>The GUI is hidden as F1 hides it ({@code hideGUI}). That also hides the hand and held item, the crosshair, the
 * chat and every overlay drawn through {@code RenderGameOverlayEvent}, which vanilla does not fire while the GUI is
 * hidden: the pack's InGame Info XML panel and JourneyMap's minimap among them. The overlay event is also cancelled, in
 * case a screen re-enables it.</li>
 * <li>The "Press 'E' to open your inventory" toast is vanilla's achievement window, which draws even with the GUI
 * hidden; the hint is switched off and the window cleared every tick.</li>
 * <li>Any screen that opens in the world (other than terrain download) is closed.</li>
 * <li>Field of view, render distance and clouds come from {@link CaptureSettings}. They are set in memory only and
 * never saved by this class; the shot runner also restores the client's options files after the run.</li>
 * <li>Each tick a {@link SettleTracker} watches the camera, the chunks around it and the terrain build queue
 * ({@link TerrainProbe}), and the log gets {@code [LCNH-CAPTURE] settled ...} once the viewpoint is complete and still,
 * and {@code [LCNH-CAPTURE] moving} when it changes. The runner screenshots only after a settled line at its
 * viewpoint, so no shot shows half-loaded terrain.</li>
 * </ul>
 *
 * <p>
 * Weather and time are the server's: the runner locks them with console commands. Registered by {@link ClientProxy}
 * only with the test hooks and the property on, so a normal client never runs it. Side: client.
 */
public final class CleanCapture {

    /** The greppable tag on every line this class logs. */
    public static final String TAG = "[LCNH-CAPTURE]";

    /** Ticks between progress lines while a viewpoint is still loading: ten seconds. */
    static final int PROGRESS_TICKS = 200;

    private final CaptureSettings settings;
    private final TerrainProbe probe;
    private final SettleTracker tracker = new SettleTracker();
    private boolean announced;

    private CleanCapture(CaptureSettings settings, TerrainProbe probe) {
        this.settings = settings;
        this.probe = probe;
    }

    /** Applies the view settings and registers the capture on both event buses. */
    static void register() {
        CleanCapture capture = new CleanCapture(CaptureSettings.from(System.getProperties()), TerrainProbe.create());
        capture.applyViewSettings(Minecraft.getMinecraft().gameSettings);
        MinecraftForge.EVENT_BUS.register(capture);
        FMLCommonHandler.instance()
            .bus()
            .register(capture);
        LostCities.LOG.info(
            "{} clean capture on (-D{}=true): {} terrain={}",
            TAG,
            CaptureSettings.PROPERTY,
            capture.settings,
            capture.probe.kind());
    }

    /** Sets the view options before any world loads, so the renderer starts at the capture's render distance. */
    private void applyViewSettings(GameSettings options) {
        options.fovSetting = settings.fov();
        options.renderDistanceChunks = settings.renderDistance();
        options.clouds = settings.clouds();
        options.showInventoryAchievementHint = false;
    }

    /**
     * Cancels the in-game overlay as a whole while capturing, so nothing drawn through it reaches a shot.
     *
     * @param event the overlay's first event of a frame
     */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL && Minecraft.getMinecraft().theWorld != null) {
            event.setCanceled(true);
        }
    }

    /**
     * Keeps the frame clean and tracks whether the viewpoint has settled.
     *
     * @param event the client tick
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.theWorld;
        EntityClientPlayerMP player = mc.thePlayer;
        if (world == null || player == null) {
            tracker.reset();
            announced = false;
            return;
        }
        keepFrameClean(mc);
        if (!announced) {
            announced = true;
            LostCities.LOG.info("{} active in world: {}", TAG, settings);
        }
        int chunkX = MathHelper.floor_double(player.posX) >> 4;
        int chunkZ = MathHelper.floor_double(player.posZ) >> 4;
        int missing = missingChunks(world, chunkX, chunkZ, settings.chunkRadius());
        boolean built = probe.built(mc);
        SettleTracker.Event change = tracker
            .tick(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch, missing, built);
        if (change == SettleTracker.Event.SETTLED) {
            int columnX = MathHelper.floor_double(player.posX);
            int columnZ = MathHelper.floor_double(player.posZ);
            // y is the feet, as /tp and /lostcities view take it: a 1.7.10 client player's posY includes eye height.
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "%s settled x=%.2f y=%.2f z=%.2f yaw=%.1f pitch=%.1f top=%d ticks=%d",
                    TAG,
                    player.posX,
                    player.boundingBox.minY,
                    player.posZ,
                    player.rotationYaw,
                    player.rotationPitch,
                    world.getHeightValue(columnX, columnZ) - 1,
                    tracker.ticksSinceMove()));
        } else if (change == SettleTracker.Event.MOVED) {
            LostCities.LOG.info("{} moving", TAG);
        } else if (loadingForAWhile()) {
            LostCities.LOG.info(
                "{} waiting {} s: {} chunks missing, terrain {}",
                TAG,
                tracker.ticksSinceMove() / 20,
                missing,
                built ? "built" : "building");
        }
    }

    /** Returns whether a progress line is due: the viewpoint is unsettled, every {@link #PROGRESS_TICKS} ticks. */
    private boolean loadingForAWhile() {
        int ticks = tracker.ticksSinceMove();
        return !tracker.settled() && ticks > 0 && ticks % PROGRESS_TICKS == 0;
    }

    /** Hides the GUI, the hand and the toast, keeps first person, and closes any screen but terrain download. */
    private static void keepFrameClean(Minecraft mc) {
        GameSettings options = mc.gameSettings;
        options.hideGUI = true;
        options.showDebugInfo = false;
        options.thirdPersonView = 0;
        options.showInventoryAchievementHint = false;
        mc.guiAchievement.func_146257_b(); // clearAchievements: the toast draws even with the GUI hidden
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiDownloadTerrain)) {
            LostCities.LOG.info(
                "{} closing screen {}",
                TAG,
                mc.currentScreen.getClass()
                    .getName());
            mc.displayGuiScreen(null);
        }
    }

    /** Counts the chunks within a radius of a chunk that the client has not received yet. */
    private static int missingChunks(WorldClient world, int chunkX, int chunkZ, int radius) {
        int missing = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (world.getChunkFromChunkCoords(chunkX + dx, chunkZ + dz)
                    .isEmpty()) {
                    missing++;
                }
            }
        }
        return missing;
    }
}
