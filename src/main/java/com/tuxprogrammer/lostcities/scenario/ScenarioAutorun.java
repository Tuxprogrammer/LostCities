package com.tuxprogrammer.lostcities.scenario;

import java.util.List;

import net.minecraft.server.MinecraftServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.DevEnvironment;

/**
 * Starts scenarios from the command line, so automation needs no console input: {@code ./gradlew runServer
 * --mcJvmArgs=-Dlostcitiesnh.scenarios=all --mcJvmArgs=-Dlostcitiesnh.stopAfterScenarios=true} boots the server, runs
 * every scenario once the server has started, writes the summary line and stops the server (docs/testing/README.md).
 *
 * <p>
 * Only where the test hooks are on ({@link DevEnvironment#testHooksEnabled()}). Side: server.
 */
public final class ScenarioAutorun {

    /** JVM property naming the scenarios to run at server start: a selector as {@link ScenarioRegistry#select} takes. */
    public static final String SCENARIOS_PROPERTY = "lostcitiesnh.scenarios";

    /** JVM property that stops the server after the started run's summary line: {@code true} or {@code false}. */
    public static final String STOP_PROPERTY = "lostcitiesnh.stopAfterScenarios";

    private ScenarioAutorun() {}

    /** Starts the requested run, if a selector is set and the test hooks are on; called once the server has started. */
    public static void startIfRequested() {
        String selector = System.getProperty(SCENARIOS_PROPERTY);
        if (selector == null || selector.trim()
            .isEmpty()) {
            return;
        }
        boolean stop = Boolean.getBoolean(STOP_PROPERTY);
        if (!DevEnvironment.testHooksEnabled()) {
            LostCities.LOG.error(ScenarioReport.errorLine("-D" + SCENARIOS_PROPERTY + " set but the test hooks are off"));
            stopIf(stop);
            return;
        }
        List<Scenario> scenarios = ScenarioRegistry.INSTANCE.select(selector.trim());
        if (scenarios.isEmpty()) {
            LostCities.LOG.error(ScenarioReport.errorLine("no scenario matches '" + selector.trim() + "'"));
            stopIf(stop);
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        LostCities.LOG.info("Running {} scenario(s) for -D{}={}", scenarios.size(), SCENARIOS_PROPERTY, selector);
        if (!ScenarioRunner.INSTANCE.start(scenarios, server, stop ? () -> stopIf(true) : null)) {
            LostCities.LOG.error(ScenarioReport.errorLine("the scenario runner could not start"));
            stopIf(stop);
        }
    }

    private static void stopIf(boolean stop) {
        if (stop) {
            LostCities.LOG.info("Stopping the server after the scenario run (-D{}=true)", STOP_PROPERTY);
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }
}
