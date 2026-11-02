package com.tuxprogrammer.lostcities.layout;

import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedBuilding;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedCity;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedIndex;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;

/**
 * The predefined content the layout honors: the dimension's {@link PredefinedIndex} minus every predefined building,
 * multibuilding or street that lands on a landmark core. The core wins, because the city never writes in a core
 * (0006 §1; P04-6, decision log D31): such an entry is skipped as a whole, with one log line naming the asset.
 *
 * <p>
 * Spec: docs/new-spec/04-city-layout/index.md ("Predefined content on a landmark core"). Side: server.
 */
final class ActivePredefined {

    private final PredefinedIndex index;
    private final LandmarkZones zones;
    private final LayoutPlanner planner;

    ActivePredefined(PredefinedIndex index, LandmarkZones zones, LayoutPlanner planner) {
        this.index = index;
        this.zones = zones;
        this.planner = planner;
    }

    /** Returns the predefined city centered on a chunk, or {@code null}. */
    PredefinedCity cityAt(int chunkX, int chunkZ) {
        return index.isEmpty() ? null : index.cityAt(chunkX, chunkZ);
    }

    /** Returns every predefined city of the dimension, sorted by source name. */
    java.util.List<PredefinedCity> cities() {
        return index.cities();
    }

    /** Returns what predefined content occupies a chunk, or {@code null} when none does or it is skipped. */
    PredefinedIndex.Occupant occupant(int chunkX, int chunkZ) {
        if (index.isEmpty()) {
            return null;
        }
        PredefinedIndex.Occupant occupant = index.occupant(chunkX, chunkZ);
        if (occupant == null || !onCore(occupant, chunkX, chunkZ)) {
            return occupant;
        }
        String what = occupant.isStreet() ? "street"
            : "building " + occupant.building()
                .building();
        int topX = chunkX - occupant.offsetX();
        int topZ = chunkZ - occupant.offsetZ();
        planner.warnOnce(
            "predefined-core:" + occupant.source() + ":" + what + ":" + topX + ":" + topZ,
            "predefined " + what
                + " of "
                + occupant.source()
                + " at chunk "
                + topX
                + ","
                + topZ
                + " lies on a landmark core and is skipped");
        return null;
    }

    /** Returns the predefined building whose top-left is this chunk, or {@code null} when none is or it is skipped. */
    PredefinedBuilding buildingAtTopLeft(int chunkX, int chunkZ) {
        if (index.isEmpty()) {
            return null;
        }
        PredefinedBuilding building = index.buildingAtTopLeft(chunkX, chunkZ);
        return building != null && occupant(chunkX, chunkZ) != null ? building : null;
    }

    /** Whether any chunk of the occupant's building (or the street chunk itself) is a landmark core. */
    private boolean onCore(PredefinedIndex.Occupant occupant, int chunkX, int chunkZ) {
        int dimX = 1;
        int dimZ = 1;
        if (!occupant.isStreet() && occupant.building()
            .multi()) {
            MultiBuildingView multi = planner.assets.multiBuilding(
                occupant.building()
                    .building());
            if (multi != null) {
                dimX = multi.dimX();
                dimZ = multi.dimZ();
            }
        }
        int topX = chunkX - occupant.offsetX();
        int topZ = chunkZ - occupant.offsetZ();
        for (int x = 0; x < dimX; x++) {
            for (int z = 0; z < dimZ; z++) {
                if (zones.zone(topX + x, topZ + z)
                    .zone() == Zone.CORE) {
                    return true;
                }
            }
        }
        return false;
    }
}
