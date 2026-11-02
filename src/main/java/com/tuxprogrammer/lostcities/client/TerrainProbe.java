package com.tuxprogrammer.lostcities.client;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Tells the clean capture whether the terrain renderer has finished building the chunks in view.
 *
 * <p>
 * The GTNH pack renders terrain with Angelica's Celeritas renderer, which replaces vanilla's chunk updates. Angelica
 * 2.2.43 exposes it as {@code com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer.getInstanceOrNull()},
 * whose superclass {@code org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer} has a public
 * {@code isTerrainRenderComplete()}: the build queue is empty (checked with javap on the pack's
 * {@code angelica-2.2.43.jar}; the class is in Angelica's source at tag 2.2.43,
 * {@code src/main/java/com/gtnewhorizons/angelica/rendering/celeritas/CeleritasWorldRenderer.java}). It is reached by
 * reflection, so the mod has no Angelica dependency. Without Angelica, or with its renderer off, vanilla's
 * {@code RenderGlobal.worldRenderersToUpdate} list is the queue. If neither can be read, the probe says "built" and the
 * capture falls back on the shot's own wait.
 *
 * <p>
 * Side: client.
 */
final class TerrainProbe {

    private static final String CELERITAS = "com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer";

    private final Method instance;
    private final Method complete;
    private boolean warned;

    private TerrainProbe(Method instance, Method complete) {
        this.instance = instance;
        this.complete = complete;
    }

    /** Finds Angelica's renderer if the pack has it, and falls back on vanilla's queue otherwise. */
    static TerrainProbe create() {
        try {
            Class<?> renderer = Class.forName(CELERITAS);
            return new TerrainProbe(renderer.getMethod("getInstanceOrNull"), renderer.getMethod("isTerrainRenderComplete"));
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return new TerrainProbe(null, null);
        }
    }

    /** Returns which renderer the probe reads, for the log. */
    String kind() {
        return instance == null ? "vanilla" : "angelica";
    }

    /**
     * Returns whether the terrain renderer has nothing left to build.
     *
     * @param mc the client
     * @return {@code true} when the queue is empty, or when it cannot be read
     */
    boolean built(Minecraft mc) {
        try {
            if (instance != null) {
                Object renderer = instance.invoke(null);
                if (renderer != null) {
                    return (Boolean) complete.invoke(renderer);
                }
            }
            List<?> queue = ReflectionHelper
                .getPrivateValue(RenderGlobal.class, mc.renderGlobal, "worldRenderersToUpdate", "field_72767_j");
            return queue == null || queue.isEmpty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LostCities.LOG.warn("{} cannot read the terrain build queue; shots rely on their waits", CleanCapture.TAG, e);
            }
            return true;
        }
    }
}
