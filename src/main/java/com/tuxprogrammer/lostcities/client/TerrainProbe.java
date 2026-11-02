package com.tuxprogrammer.lostcities.client;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Tells the clean capture whether the terrain renderer has finished building the chunks in view.
 *
 * <p>
 * The GTNH pack renders terrain with Angelica's Celeritas renderer, which replaces vanilla's chunk updates. Angelica
 * 2.2.43 exposes it as
 * {@code com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer.getInstanceOrNull()} (Angelica's
 * source at tag 2.2.43, {@code src/main/java/com/gtnewhorizons/angelica/rendering/celeritas/CeleritasWorldRenderer.java}),
 * whose superclass {@code org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer} has, in the pack's
 * {@code angelica-2.2.43.jar} (read with javap):
 * <ul>
 * <li>{@code isTerrainRenderComplete()}, which is only {@code ChunkBuilder.isBuildQueueEmpty()}: the queue empties
 * between scheduling rounds while sections still wait in the rebuild lists, and on classic RWG whole chunks near the
 * camera were still unbuilt after it had stayed true for a second;</li>
 * <li>{@code getDebugStrings()}, whose {@code Chunk Queues: U=.. (P0=.. | P1=.. | P2=..)} line counts the build results
 * waiting for upload and the sections waiting in each rebuild list ({@link #pendingFromDebug}).</li>
 * </ul>
 * The probe reports "built" only when the queue is empty and that line counts nothing pending. Both are reached by
 * reflection, so the mod has no Angelica dependency. Without Angelica, or with its renderer off, vanilla's
 * {@code RenderGlobal.worldRenderersToUpdate} list is the queue. If neither can be read, the probe says "built" and the
 * shot runner's frame check is what remains.
 *
 * <p>
 * Side: client.
 */
final class TerrainProbe {

    private static final String CELERITAS = "com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer";
    private static final Pattern QUEUES = Pattern
        .compile("Chunk Queues: U=(\\d+) \\(P0=(\\d+) \\| P1=(\\d+) \\| P2=(\\d+)\\)");

    private final Method instance;
    private final Method complete;
    private final Method debug;
    private boolean warned;
    private int lastPending;

    private TerrainProbe(Method instance, Method complete, Method debug) {
        this.instance = instance;
        this.complete = complete;
        this.debug = debug;
    }

    /** Finds Angelica's renderer if the pack has it, and falls back on vanilla's queue otherwise. */
    static TerrainProbe create() {
        try {
            Class<?> renderer = Class.forName(CELERITAS);
            Method debug = null;
            try {
                debug = renderer.getMethod("getDebugStrings");
            } catch (NoSuchMethodException e) {
                // Only the queue, then.
            }
            return new TerrainProbe(
                renderer.getMethod("getInstanceOrNull"),
                renderer.getMethod("isTerrainRenderComplete"),
                debug);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return new TerrainProbe(null, null, null);
        }
    }

    /** Returns which renderer the probe reads, for the log. */
    String kind() {
        return instance == null ? "vanilla" : debug == null ? "angelica-queue" : "angelica";
    }

    /** Returns the sections pending at the last {@link #built} call, or -1 when unknown, for the log. */
    int lastPending() {
        return lastPending;
    }

    /**
     * Returns whether the terrain renderer has nothing left to build.
     *
     * @param mc the client
     * @return {@code true} when the queue is empty and nothing is pending, or when it cannot be read
     */
    boolean built(Minecraft mc) {
        lastPending = -1;
        try {
            if (instance != null) {
                Object renderer = instance.invoke(null);
                if (renderer != null) {
                    boolean queueEmpty = (Boolean) complete.invoke(renderer);
                    if (debug != null) {
                        lastPending = pendingFromDebug((Collection<?>) debug.invoke(renderer));
                    }
                    return queueEmpty && lastPending <= 0;
                }
            }
            List<?> queue = ReflectionHelper
                .getPrivateValue(RenderGlobal.class, mc.renderGlobal, "worldRenderersToUpdate", "field_72767_j");
            return queue == null || queue.isEmpty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LostCities.LOG
                    .warn("{} cannot read the terrain build queue; shots rely on their waits", CleanCapture.TAG, e);
            }
            return true;
        }
    }

    /**
     * Sums the pending uploads and rebuild-list sections in Celeritas's {@code Chunk Queues} debug line.
     *
     * @param lines the renderer's debug strings
     * @return the sum, or -1 when no line has that form
     */
    static int pendingFromDebug(Collection<?> lines) {
        if (lines == null) {
            return -1;
        }
        for (Object line : lines) {
            Matcher m = QUEUES.matcher(String.valueOf(line));
            if (m.find()) {
                return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2))
                    + Integer.parseInt(m.group(3))
                    + Integer.parseInt(m.group(4));
            }
        }
        return -1;
    }
}
