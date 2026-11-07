package com.tuxprogrammer.lostcities.layout;

import java.util.function.Consumer;

/**
 * A diagnostic trace for the layout: one line per decision worth explaining (why a city got a beltway or none, where a
 * city's connected component starts, which link chunks an end's radius swallowed). Off unless the glue installs a
 * sink, which it does only when {@code -Dlostcitiesnh.cityTrace=true} is set, so a normal run pays one volatile read
 * per traced decision and builds no strings. The layout is pure Java, so the sink is a plain {@link Consumer}; the
 * glue writes each line to the mod log with an {@code [LCNH-TRACE]} prefix.
 *
 * <p>
 * Tracing never changes a result: callers compute what they would anyway and only describe it.
 *
 * <p>
 * Side: server.
 */
public final class LayoutTrace {

    private static volatile Consumer<String> sink;

    private LayoutTrace() {}

    /**
     * Installs the sink, or removes it with {@code null}.
     *
     * @param consumer receives each formatted line
     */
    public static void install(Consumer<String> consumer) {
        sink = consumer;
    }

    /** Returns whether a sink is installed; callers test this before building a line. */
    public static boolean on() {
        return sink != null;
    }

    /**
     * Writes one line, formatted with {@link String#format}, when a sink is installed.
     *
     * @param format the format
     * @param args   its arguments
     */
    public static void log(String format, Object... args) {
        Consumer<String> current = sink;
        if (current != null) {
            current.accept(String.format(java.util.Locale.ROOT, format, args));
        }
    }
}
