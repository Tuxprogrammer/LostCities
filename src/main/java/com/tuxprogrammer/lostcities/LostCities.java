package com.tuxprogrammer.lostcities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

/**
 * The {@code @Mod} entry point of Lost Cities: New Horizons. It only forwards FML's lifecycle events to the side's
 * proxy, which does the work in the order CONTRIBUTING § Architecture sets: config and network in {@code preInit},
 * event handlers in {@code init}, assets and the dev self-check in {@code postInit}.
 *
 * <p>
 * Spec: docs/new-spec/01-core/index.md (Lifecycle and registration). Side: both.
 */
@Mod(
    modid = LostCities.MODID,
    version = Tags.VERSION,
    name = LostCities.NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = LostCities.DEPENDENCIES)
public class LostCities {

    /** The mod ID: also the config file name, the network channel, the lang namespace and the command prefix. */
    public static final String MODID = "lostcitiesnh";

    /** The mod's display name. */
    public static final String NAME = "Lost Cities: New Horizons";

    /**
     * Hard dependencies, at the versions GTNH 2.9.0 ships (docs/design/0002-target-pack.md). Kept in step with
     * {@code mcmod.info} and {@code dependencies.gradle}.
     */
    public static final String DEPENDENCIES = "required-after:gtnhlib@[0.11.54,)";

    /** The mod logger. Every diagnostic goes here, never to standard output. */
    public static final Logger LOG = LogManager.getLogger(MODID);

    /** The side's proxy; FML picks the class by name, so common code never names the client proxy. */
    @SidedProxy(
        clientSide = "com.tuxprogrammer.lostcities.client.ClientProxy",
        serverSide = "com.tuxprogrammer.lostcities.CommonProxy")
    public static CommonProxy proxy;

    /**
     * Loads the config and opens the network channel.
     *
     * @param event FML's pre-initialization event
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    /**
     * Registers event handlers.
     *
     * @param event FML's initialization event
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    /**
     * Runs the dev self-check once every mod has registered its blocks.
     *
     * @param event FML's post-initialization event
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    /**
     * Freezes the API's profile callbacks and adds their profiles, before any world of the server is pinned.
     *
     * @param event FML's server-about-to-start event
     */
    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.serverAboutToStart(event);
    }

    /**
     * Registers the {@code /lostcities} command.
     *
     * @param event FML's server-starting event
     */
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    /**
     * Starts the scenarios requested on the command line, if any.
     *
     * @param event FML's server-started event
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    /**
     * Drops any scenario run left over from the server that stopped.
     *
     * @param event FML's server-stopped event
     */
    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }
}
