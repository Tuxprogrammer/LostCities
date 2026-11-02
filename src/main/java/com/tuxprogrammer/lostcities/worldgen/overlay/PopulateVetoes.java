package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Vetoes RWG's lakes, lava lakes and dungeons, NewHorizonsCoreMod's oil deposits and its Railcraft quarried-stone
 * populator, where a population area touches
 * a city chunk, so they never cut into cellars and streets (docs/design/0005-rwg-overlay.md §3; upstream 1.12's
 * {@code preventLakesRavinesInCities}, a profile option on by default). RWG honors these {@code Populate} results;
 * NHCore's oil generation gates on the {@code CUSTOM} type (OilGeneratorFix.java#L139-L171 at 2.9.99). Landmark cores
 * are not city chunks and get no veto (0006 §1).
 *
 * <p>
 * Registered on {@code MinecraftForge.TERRAIN_GEN_BUS}. Server thread only. Side: server.
 */
public final class PopulateVetoes {

    /** The name of Railcraft's quarried-stone populate type, added to the enum at runtime. */
    static final String RAILCRAFT_QUARRY = "RAILCRAFT_QUARRY";

    private final Overlays overlays;
    private long vetoed;

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
        if (!denied(type)) {
            return;
        }
        OverlayWorld overlay = overlays.get(event.world);
        if (overlay != null && overlay.populationAreaTouchesCity(event.chunkX, event.chunkZ)) {
            event.setResult(Event.Result.DENY);
            vetoed++;
        }
    }

    /**
     * Returns whether a populate type is one the city denies: lakes, lava lakes, dungeons, the {@code CUSTOM} type
     * NHCore's oil uses, and Railcraft's quarried-stone populator, which Railcraft adds to the enum at runtime and
     * NHCore's replacement fires (other-worldgen.md T2; compared by name, as RWG2 compares Railcraft's types).
     * Source: NH_QuarryPopulator.java#L38-L68 at NewHorizonsCoreMod 2.9.99,
     * https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.99/src/main/java/com/dreammaster/railcraftStones/NH_QuarryPopulator.java#L38-L68
     *
     * @param type the populate event's type
     * @return {@code true} when the city denies it
     */
    static boolean denied(PopulateChunkEvent.Populate.EventType type) {
        return type == PopulateChunkEvent.Populate.EventType.LAKE || type == PopulateChunkEvent.Populate.EventType.LAVA
            || type == PopulateChunkEvent.Populate.EventType.DUNGEON
            || type == PopulateChunkEvent.Populate.EventType.CUSTOM
            || RAILCRAFT_QUARRY.equals(type.name());
    }

    /** Returns how many populate steps were denied. */
    public long vetoed() {
        return vetoed;
    }
}
