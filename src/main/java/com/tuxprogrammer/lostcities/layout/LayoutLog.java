package com.tuxprogrammer.lostcities.layout;

/**
 * Where the layout reports what it had to skip or fall back from (a predefined building on a landmark core, an unknown
 * asset name). The layout is pure Java and cannot use the mod logger; the glue passes one that writes to it. Each
 * problem is reported once per planner, so a recomputed plan does not repeat its line.
 *
 * <p>
 * Side: server.
 */
public interface LayoutLog {

    /** A log that drops everything, for tests that do not look. */
    LayoutLog SILENT = message -> {};

    /**
     * Reports a problem.
     *
     * @param message one line, naming the asset or position involved
     */
    void warn(String message);
}
