package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Vetoes RWG's lakes, lava lakes and dungeons, and NewHorizonsCoreMod's oil deposits, where a population area touches
 * a city chunk or a scattered structure's footprint (P07-4, D31), so they never cut into cellars, streets, cabins or
 * gas stations (docs/design/0005-rwg-overlay.md §3; upstream 1.12's
 * {@code preventLakesRavinesInCities}, a profile option on by default). RWG honors these {@code Populate} results;
 * NHCore's oil generation gates on the {@code CUSTOM} type (OilGeneratorFix.java#L139-L171 at 2.9.99). Landmark cores
 * are not city chunks and get no veto (0006 §1).
 *
 * <p>
 * Registered on {@code MinecraftForge.TERRAIN_GEN_BUS}. Server thread only. Side: server.
 */
public final class PopulateVetoes {

    private final Overlays overlays;
    private long vetoed;
    private long vetoedScattered;

    PopulateVetoes(Overlays overlays) {
        this.overlays = overlays;
    }

    /**
     * Denies a lake, lava lake, dungeon or custom feature in a city's population area.
     *
     * @param event Forge's populate event
     */
    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event) {
        // Not a switch: mods add values to this enum at runtime (Forestry's hives do), and a switch's ordinal table,
        // sized when it is first used, would be too short for them.
        PopulateChunkEvent.Populate.EventType type = event.type;
        if (type != PopulateChunkEvent.Populate.EventType.LAKE && type != PopulateChunkEvent.Populate.EventType.LAVA
            && type != PopulateChunkEvent.Populate.EventType.DUNGEON
            && type != PopulateChunkEvent.Populate.EventType.CUSTOM) {
            return;
        }
        OverlayWorld overlay = overlays.get(event.world);
        if (overlay == null) {
            return;
        }
        if (overlay.populationAreaTouchesCity(event.chunkX, event.chunkZ)) {
            event.setResult(Event.Result.DENY);
            vetoed++;
        } else if (overlay.populationAreaTouchesScattered(event.chunkX, event.chunkZ)) {
            // P07-4 (D31): a scattered structure is defended like a city building.
            event.setResult(Event.Result.DENY);
            vetoed++;
            vetoedScattered++;
        }
    }

    /** Returns how many populate steps were denied because their area touched a scattered structure, not a city. */
    public long vetoedScattered() {
        return vetoedScattered;
    }

    /** Returns how many populate steps were denied. */
    public long vetoed() {
        return vetoed;
    }
}
