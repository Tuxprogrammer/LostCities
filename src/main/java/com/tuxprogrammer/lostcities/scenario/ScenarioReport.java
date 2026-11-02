package com.tuxprogrammer.lostcities.scenario;

/**
 * The machine-readable lines the scenario runner writes to the server log. Automation greps for them:
 *
 * <pre>
 * [LCNH-SCENARIO] PASS|FAIL &lt;name&gt; &lt;ms&gt; &lt;message&gt;
 * [LCNH-SCENARIO] Scenarios: N passed, M failed
 * [LCNH-SCENARIO] ERROR &lt;message&gt;
 * </pre>
 *
 * One result line per scenario, then one summary line per run, written only after every scenario of the run has
 * finished; {@code M > 0} is a failed run. {@code ERROR} means the run did not start (no match, a run already going,
 * hooks off) and counts as a failure too. {@code ms} is wall time from setup to verify, reported for information and
 * never used to decide a result. Every message is collapsed onto one line.
 *
 * <p>
 * Side: server (plain Java).
 */
public final class ScenarioReport {

    /** The prefix of every line. */
    public static final String TAG = "[LCNH-SCENARIO]";

    private ScenarioReport() {}

    /**
     * Returns the result line of one scenario.
     *
     * @param passed  whether it passed
     * @param name    its name
     * @param millis  wall time it took, in milliseconds
     * @param message what happened; {@code ok} or {@code failed} when empty
     * @return the line
     */
    public static String resultLine(boolean passed, String name, long millis, String message) {
        return TAG + " " + verdict(passed) + " " + name + " " + millis + " " + messageOrDefault(passed, message);
    }

    /**
     * Returns the summary line of a run.
     *
     * @param passed how many scenarios passed
     * @param failed how many failed
     * @return the line
     */
    public static String summaryLine(int passed, int failed) {
        return TAG + " Scenarios: " + passed + " passed, " + failed + " failed";
    }

    /**
     * Returns the line for a run that did not start.
     *
     * @param message why
     * @return the line
     */
    public static String errorLine(String message) {
        String text = oneLine(message);
        return TAG + " ERROR " + (text.isEmpty() ? "error" : text);
    }

    /** Returns {@code PASS} or {@code FAIL}. */
    static String verdict(boolean passed) {
        return passed ? "PASS" : "FAIL";
    }

    /** Returns the one-line message, or {@code ok} / {@code failed} when there is none. */
    static String messageOrDefault(boolean passed, String message) {
        String text = oneLine(message);
        if (text.isEmpty()) {
            return passed ? "ok" : "failed";
        }
        return text;
    }

    /** Collapses every run of whitespace, line breaks included, into one space. */
    static String oneLine(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("\\s+", " ")
            .trim();
    }
}
