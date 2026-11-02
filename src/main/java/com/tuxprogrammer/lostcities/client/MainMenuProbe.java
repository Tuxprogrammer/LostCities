package com.tuxprogrammer.lostcities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * A dev-only client probe for headless checks: with {@code -Dlostcitiesnh.quitAtMainMenu=true}, once the main menu
 * has been open for {@link #SETTLE_TICKS} client ticks it logs {@code [LCNH-CLIENT] main menu reached}, saves a
 * screenshot, and closes the game. So {@code runClient} under Xvfb proves the client boots to the menu with the mod
 * loaded, and exits by itself instead of being killed (testing-gtnh-mods: drive clients from inside the game).
 *
 * <p>
 * Registered only when the property is set and the test hooks are on. Side: client.
 */
public final class MainMenuProbe {

    /** JVM property that turns the probe on. */
    public static final String PROPERTY = "lostcitiesnh.quitAtMainMenu";

    /** The greppable line the probe logs. */
    public static final String REACHED_LINE = "[LCNH-CLIENT] main menu reached";

    /** Client ticks to wait after the menu opens, so its first frames are drawn before the screenshot. */
    static final int SETTLE_TICKS = 60;

    private boolean menuOpen;
    private int ticksOnMenu;
    private boolean done;

    /** Registers a probe on both event buses. */
    static void register() {
        MainMenuProbe probe = new MainMenuProbe();
        MinecraftForge.EVENT_BUS.register(probe);
        FMLCommonHandler.instance()
            .bus()
            .register(probe);
        LostCities.LOG.info("Main-menu probe on (-D{}=true): the client closes itself at the main menu", PROPERTY);
    }

    /**
     * Notes when the main menu opens or closes.
     *
     * @param event a screen opening
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        menuOpen = event.gui instanceof GuiMainMenu;
        ticksOnMenu = 0;
    }

    /**
     * Counts ticks on the main menu, then reports, takes the screenshot and closes the game.
     *
     * @param event the client tick
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !menuOpen || done || ++ticksOnMenu < SETTLE_TICKS) {
            return;
        }
        done = true;
        Minecraft mc = Minecraft.getMinecraft();
        LostCities.LOG.info(REACHED_LINE);
        IChatComponent saved = ScreenShotHelper
            .saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        LostCities.LOG.info("[LCNH-CLIENT] screenshot: {}", saved == null ? "none" : saved.getUnformattedText());
        mc.shutdown();
    }
}
