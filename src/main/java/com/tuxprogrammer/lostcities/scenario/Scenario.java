package com.tuxprogrammer.lostcities.scenario;

/**
 * An in-world test: a reproducible situation on the running server, advanced for a number of server ticks and then
 * checked (CONTRIBUTING § Testing, layer 3; testing-gtnh-mods, layer 4).
 *
 * <p>
 * Lifecycle, all on the server thread: the runner calls {@link #setup}, then once per server tick {@link #tick} until
 * {@link #ticks()} ticks have run or {@link #finished} returns {@code true}, then {@link #verify}. The scenario passes
 * when none of them throws. Throw {@link ScenarioFailure} (see {@link ScenarioContext#check}) with what was observed
 * for an expected failure; any other throwable fails the scenario too and is logged with its stack trace.
 *
 * <p>
 * Work only in the scenario's own arena ({@link ScenarioContext#arenaChunkX()}), far from spawn and from other
 * scenarios, and wait in ticks, never on the wall clock. Register instances in {@link BuiltinScenarios} or, for a
 * feature's own scenarios, with {@link ScenarioRegistry#register}, and list them in docs/testing/README.md.
 *
 * <p>
 * Side: server.
 */
public interface Scenario {

    /**
     * Returns the unique name: lowercase dot-separated words, area first, for example {@code harness.trivial}. It is
     * matched exactly or by prefix and printed in report lines, so it holds no spaces ({@link ScenarioRegistry#NAME}).
     */
    String name();

    /**
     * Returns the most server ticks between {@link #setup} and {@link #verify}: 0 to {@link ScenarioRunner#MAX_TICKS}.
     * With 0, {@link #verify} runs in the same tick as {@link #setup}.
     */
    default int ticks() {
        return 1;
    }

    /**
     * Returns {@code true} for a scenario that runs only when named exactly: never as part of {@code all} or of a
     * prefix. For long runs, and for the harness's own deliberate failure.
     */
    default boolean manualOnly() {
        return false;
    }

    /**
     * Returns {@code true} to end the run early, before {@link #ticks()} have passed; checked after each {@link #tick}.
     *
     * @param context the run's context
     */
    default boolean finished(ScenarioContext context) {
        return false;
    }

    /**
     * Builds the situation.
     *
     * @param context the run's context
     * @throws Exception any failure, which fails the scenario
     */
    void setup(ScenarioContext context) throws Exception;

    /**
     * Advances the situation; called once per server tick after setup.
     *
     * @param context the run's context; {@link ScenarioContext#ticksRun()} counts the calls
     * @throws Exception any failure, which fails the scenario
     */
    default void tick(ScenarioContext context) throws Exception {}

    /**
     * Checks the outcome.
     *
     * @param context the run's context
     * @throws Exception {@link ScenarioFailure} with what was observed when the outcome is wrong
     */
    void verify(ScenarioContext context) throws Exception;
}
