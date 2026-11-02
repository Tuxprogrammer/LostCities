package com.tuxprogrammer.lostcities;

import com.tuxprogrammer.lostcities.command.LostCitiesCommand;
import com.tuxprogrammer.lostcities.compat.villagenames.VillageNamesOracles;
import com.tuxprogrammer.lostcities.compat.villagenames.VillageNamesScenario;
import com.tuxprogrammer.lostcities.config.LostCitiesConfig;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.core.assets.AssetScenarios;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.core.selfcheck.DevSelfCheck;
import com.tuxprogrammer.lostcities.network.LostCitiesNetwork;
import com.tuxprogrammer.lostcities.scenario.BuiltinScenarios;
import com.tuxprogrammer.lostcities.scenario.ScenarioAutorun;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.scenario.ScenarioRunner;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayScenarios;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

/**
 * Lifecycle work shared by both sides, and all of it on a dedicated server. The client proxy
 * ({@code client.ClientProxy}) extends this class and adds only client-side setup.
 *
 * <p>
 * Phases follow CONTRIBUTING § Architecture: {@code preInit} loads the config and opens the network channel;
 * {@code init} registers event handlers; {@code postInit} loads the assets and runs the dev self-check. Nothing is
 * registered with a game
 * registry: the mod adds no blocks, items or entities (docs/new-spec/01-core/index.md), so module flags can never
 * change what a client and server must agree on.
 *
 * <p>
 * Side: both.
 */
public class CommonProxy {

    /**
     * Loads the GTNHLib config and opens the network channel.
     *
     * @param event FML's pre-initialization event
     */
    public void preInit(FMLPreInitializationEvent event) {
        LostCitiesConfig.register();
        LostCitiesNetwork.init();
        LostCities.LOG.info("{} {}: modules {}", LostCities.NAME, Tags.VERSION, ModuleState.configured());
    }

    /**
     * Registers event handlers: the overlay's installer and population vetoes (docs/design/0005-rwg-overlay.md §1),
     * then the scenarios. The built-in and overlay scenarios are always registered, since a registry entry does nothing
     * on
     * its own; the scenario runner listens to server ticks only where the test hooks are on.
     *
     * @param event FML's initialization event
     */
    public void init(FMLInitializationEvent event) {
        Overlays.register();
        BuiltinScenarios.registerAll(ScenarioRegistry.INSTANCE);
        AssetScenarios.registerAll(ScenarioRegistry.INSTANCE);
        OverlayScenarios.registerAll(ScenarioRegistry.INSTANCE);
        VillageNamesScenario.registerAll(ScenarioRegistry.INSTANCE);
        if (DevEnvironment.testHooksEnabled()) {
            ScenarioRunner.subscribe();
        }
    }

    /**
     * Loads the assets against the complete block registry, pins the order of FML's village-creation handlers so
     * VillageNames villages are reproducible (decision log D86), then runs the dev self-check, which throws and
     * so stops the dev server when it fails.
     *
     * @param event FML's post-initialization event
     */
    public void postInit(FMLPostInitializationEvent event) {
        LoadedAssets.load(
            Loader.instance()
                .getConfigDir());
        VillageNamesOracles.init();
        DevSelfCheck.runIfEnabled();
    }

    /**
     * Registers the {@code /lostcities} command.
     *
     * @param event FML's server-starting event
     */
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new LostCitiesCommand());
    }

    /**
     * Starts the scenarios named by {@code -Dlostcitiesnh.scenarios}, when the test hooks are on.
     *
     * @param event FML's server-started event
     */
    public void serverStarted(FMLServerStartedEvent event) {
        ScenarioAutorun.startIfRequested();
    }

    /**
     * Forgets any scenario run and every wrapped world of the server that stopped, so an integrated server started
     * next starts clean.
     *
     * @param event FML's server-stopped event
     */
    public void serverStopped(FMLServerStoppedEvent event) {
        ScenarioRunner.INSTANCE.reset();
        Overlays.INSTANCE.reset();
    }
}
