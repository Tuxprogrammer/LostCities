package com.tuxprogrammer.lostcities.client;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.MinecraftForge;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * A dev-only client probe for the live part of the client UI (docs/new-spec/09-client-ui/index.md, "What is new on the
 * client"): with {@code -Dlostcitiesnh.configScreenProbe=true}, once the title screen has settled it opens FML's Mods
 * list, selects this mod and presses its Config button, as a player would, so GTNHLib's mixin opens its config screen
 * (D31). It then walks every element of the screen, the categories' children included, and checks that each label and
 * tooltip has a translation, logs {@code [LCNH-CLIENT] config ...} lines and leaves the screen open for the client
 * run's screenshot.
 *
 * <p>
 * Registered only when the property is set and the test hooks are on. Side: client.
 */
public final class ConfigScreenProbe {

    /** JVM property that turns the probe on. */
    public static final String PROPERTY = "lostcitiesnh.configScreenProbe";

    /** The greppable prefix of the probe's lines. */
    static final String TAG = "[LCNH-CLIENT] config ";

    /** Client ticks to wait on the main menu, and between steps. */
    static final int SETTLE_TICKS = 60;

    private int step;
    private int ticks;

    /** Registers a probe on FML's bus, where client ticks are posted. */
    static void register() {
        ConfigScreenProbe probe = new ConfigScreenProbe();
        FMLCommonHandler.instance()
            .bus()
            .register(probe);
        MinecraftForge.EVENT_BUS.register(probe);
        LostCities.LOG.info("Config-screen probe on (-D{}=true)", PROPERTY);
    }

    /**
     * Opens the Mods list from the main menu, presses Config, then checks the screen.
     *
     * @param event the client tick
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || step > 2) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        // The pack replaces vanilla's main menu (CustomMainMenu), so the title screen is any screen with no world.
        if (step == 0 && (mc.theWorld != null || mc.currentScreen == null || mc.currentScreen instanceof GuiModList)) {
            ticks = 0;
            return;
        }
        if (++ticks < SETTLE_TICKS) {
            return;
        }
        ticks = 0;
        try {
            if (step == 0) {
                mc.displayGuiScreen(new GuiModList(mc.currentScreen));
            } else if (step == 1) {
                pressConfig(mc);
            } else {
                check(mc);
            }
        } catch (ReflectiveOperationException e) {
            LostCities.LOG.error("{}FAIL could not drive the Mods list", TAG, e);
            step = 3;
            return;
        }
        step++;
    }

    /** Selects this mod in the Mods list and presses its Config button. */
    private static void pressConfig(Minecraft mc) throws ReflectiveOperationException {
        if (!(mc.currentScreen instanceof GuiModList list)) {
            throw new IllegalStateException("the Mods list is not open");
        }
        ModContainer mod = Loader.instance()
            .getIndexedModList()
            .get(LostCities.MODID);
        // FML's own fields keep their names in every environment.
        set(list, "selectedMod", mod);
        GuiButton config = (GuiButton) get(list, "configModButton");
        config.enabled = true;
        // actionPerformed is a Minecraft method, renamed outside development: find it by its one GuiButton parameter.
        for (Method method : GuiModList.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && parameters[0] == GuiButton.class && method.getReturnType() == void.class) {
                method.setAccessible(true);
                try {
                    method.invoke(list, config);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException("pressing Config failed", e.getCause());
                }
                LostCities.LOG.info("{}pressed Config for {} in the Mods list", TAG, mod.getName());
                return;
            }
        }
        throw new NoSuchMethodException("GuiModList.actionPerformed");
    }

    /** Checks the open config screen: every element's label and tooltip translates. */
    private static void check(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiConfig screen)) {
            LostCities.LOG.error(
                "{}FAIL the Config button opened {}, not a config screen",
                TAG,
                mc.currentScreen == null ? "nothing"
                    : mc.currentScreen.getClass()
                        .getName());
            return;
        }
        List<String> missing = new ArrayList<>();
        int[] count = { 0 };
        for (IConfigElement element : screen.configElements) {
            walk(element, "", missing, count);
        }
        LostCities.LOG.info(
            "{}screen {} for {}: {} element(s), {} missing translation(s){}",
            TAG,
            screen.getClass()
                .getName(),
            screen.modID,
            count[0],
            missing.size(),
            missing.isEmpty() ? "" : ": " + missing);
        LostCities.LOG.info("{}{}", TAG, missing.isEmpty() ? "PASS" : "FAIL");
    }

    @SuppressWarnings("rawtypes")
    private static void walk(IConfigElement element, String path, List<String> missing, int[] count) {
        count[0]++;
        String key = element.getLanguageKey();
        String name = path + element.getName();
        boolean label = translates(key);
        boolean tooltip = translates(key + ".tooltip");
        LostCities.LOG.info(
            "{}{} '{}' label={} tooltip={}",
            TAG,
            element.isProperty() ? "option" : "category",
            name,
            label ? I18n.format(key) : "MISSING " + key,
            tooltip ? "ok" : "MISSING " + key + ".tooltip");
        if (!label) {
            missing.add(key);
        }
        if (!tooltip) {
            missing.add(key + ".tooltip");
        }
        if (!element.isProperty() && element.getChildElements() != null) {
            for (Object child : element.getChildElements()) {
                walk((IConfigElement) child, name + "/", missing, count);
            }
        }
    }

    private static boolean translates(String key) {
        return key != null && !I18n.format(key)
            .equals(key);
    }

    private static Object get(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
