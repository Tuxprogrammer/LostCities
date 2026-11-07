package com.tuxprogrammer.lostcities.worldgen.overlay;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.LayoutTrace;

/**
 * The city-generation trace (diagnostic only): with {@code -Dlostcitiesnh.cityTrace=true}, the layout's
 * {@link LayoutTrace} decisions and the world-generation side's per-chunk lines (what the terrain phase wrote, what the
 * furnish pass placed, where a district monument stands) go to the mod log as {@code [LCNH-TRACE]} lines. Off by
 * default; nothing it does changes what is generated.
 *
 * <p>
 * Side: server.
 */
public final class CityTraceLog {

    /** The system property that turns the trace on. */
    public static final String PROPERTY = "lostcitiesnh.cityTrace";
    /** Prefix of every trace line. */
    public static final String TAG = "[LCNH-TRACE] ";
    /** Whether the trace is on for this run. */
    public static final boolean ENABLED = Boolean.getBoolean(PROPERTY);

    private CityTraceLog() {}

    /** Routes the layout's trace to the mod log when the trace is on; called once in pre-init. */
    public static void install() {
        if (ENABLED) {
            LayoutTrace.install(line -> LostCities.LOG.info("{}{}", TAG, line));
            LostCities.LOG.info("{}city trace on ({})", TAG, PROPERTY);
        }
    }

    /**
     * Writes one trace line, formatted with {@link String#format}, when the trace is on.
     *
     * @param format the format
     * @param args   its arguments
     */
    public static void log(String format, Object... args) {
        if (ENABLED) {
            LostCities.LOG.info("{}{}", TAG, String.format(java.util.Locale.ROOT, format, args));
        }
    }
}
