package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandDebug.java
// Avoids: CORE-26

import java.util.ArrayList;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.ChunkCharacteristics;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;

/**
 * The full dump of {@code /lostcities debug}: one {@code name = value} line per field of a chunk's layout state, in
 * upstream's order, minus legacy streets, legacy highways and the planner's parallelism (D11), plus the chunk's
 * landmark zone and village lane (docs/original-spec/01-core/commands.md, {@code debug};
 * docs/new-spec/01-core/commands.md). It reads the immutable layout only (D9).
 *
 * <p>
 * Changes from upstream: avoids CORE-26 at the command (operator level 2, sent to the sender and the log, never to
 * standard output); the intercity highway planner's dump waits for M06's planner, so the highway lines are its levels.
 *
 * <p>
 * Side: both (plain Java).
 */
final class ChunkReport {

    private ChunkReport() {}

    /**
     * Returns a chunk's layout state as lines.
     *
     * @param planner    the world's layout planner
     * @param profile    the pinned profile's name
     * @param chunkX     chunk X
     * @param chunkZ     chunk Z
     * @param kind       the overlay's chunk kind
     * @param explosions how many explosions reach the chunk
     * @param ruinChance the profile's ruin chance, for the ruin level
     * @param terrainY   the chunk's terrain height, for the tunnel answers
     * @return the lines
     */
    static List<String> lines(LayoutPlanner planner, String profile, int chunkX, int chunkZ, String kind,
        int explosions, float ruinChance, int terrainY) {
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        ChunkCharacteristics c = plan.characteristics();
        RailInfo rail = planner.infrastructure()
            .rail(chunkX, chunkZ);
        List<String> out = new ArrayList<>();
        add(out, "profile", profile);
        add(out, "buildingType", plan.hasBuilding() ? plan.building() : null);
        add(out, "multiBuilding", plan.multiBuilding());
        add(out, "multiPos", plan.multiPos());
        add(out, "floors", plan.floors());
        add(out, "floorsBelowGround", plan.cellars());
        add(out, "cityLevel", plan.cityLevel());
        add(out, "cityGroundLevel", plan.cityGroundLevel());
        add(out, "isCity", plan.isCity());
        add(out, "chunkX", chunkX);
        add(out, "chunkZ", chunkZ);
        add(out, "cityStyle", plan.cityStyle());
        add(out, "streetType", plan.streetType());
        add(out, "rawPlannedRoadType", plan.rawRoad());
        add(out, "plannedRoadType", plan.plannedRoad());
        add(out, "isPrimaryRoad", plan.isPrimaryRoad());
        add(out, "hierarchicalOpen", plan.isHierarchicalOpen());
        add(out, "predefinedStreet", plan.isPredefinedStreet());
        add(out, "couldHaveBuilding", c.couldHaveBuilding());
        add(out, "fountainPart", plan.fountainPart());
        add(out, "parkPart", plan.parkPart());
        add(out, "bridgePart", plan.bridgePart());
        add(out, "largeBridgePart", plan.largeBridgePart());
        add(out, "stairPart", plan.stairPart());
        add(out, "palettes", plan.palettes());
        add(out, "noLoot", plan.noLoot());
        add(out, "ruinHeight", plan.ruinHeight());
        add(out, "ruinLevel", plan.ruinLevel(ruinChance));
        add(out, "tunnel0", plan.isTunnel(0, terrainY));
        add(out, "tunnel1", plan.isTunnel(1, terrainY));
        add(out, "highwayXLevel", plan.highwayXLevel());
        add(out, "highwayZLevel", plan.highwayZLevel());
        add(out, "maxHighwayLevel", plan.maxHighwayLevel());
        add(out, "relativeDistanceToCityCenter", planner.cityFactor(chunkX, chunkZ));
        add(out, "isCityCenter", planner.isCityCenter(chunkX, chunkZ));
        add(out, "cityRadius", planner.cityRadius(chunkX, chunkZ));
        add(out, "railType", rail.type());
        add(out, "railLevel", rail.level());
        add(out, "xRailCorridor", plan.xRailCorridor());
        add(out, "zRailCorridor", plan.zRailCorridor());
        add(out, "explosions", explosions);
        add(out, "terrainHeight", terrainY);
        add(out, "landmarkZone", plan.zone());
        add(out, "lane", plan.lane());
        add(out, "chunkKind", kind);
        for (int floor = -plan.cellars(); plan.hasBuilding() && floor <= plan.floors(); floor++) {
            add(
                out,
                "floor" + floor,
                plan.floorPart(floor) + (plan.floorPart2(floor) == null ? "" : " + " + plan.floorPart2(floor)));
        }
        return out;
    }

    private static void add(List<String> out, String name, Object value) {
        out.add(name + " = " + value);
    }
}
