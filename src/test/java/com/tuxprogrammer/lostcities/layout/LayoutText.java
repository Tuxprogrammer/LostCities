package com.tuxprogrammer.lostcities.layout;

import java.util.Arrays;

import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.street.Orientation;

/**
 * Text forms of a layout, for goldens and comparisons: three character maps a reviewer can read (what each chunk is,
 * its landmark zone, its building's floors) and a one-line description of every decision of a chunk.
 */
public final class LayoutText {

    private LayoutText() {}

    /**
     * The map legend, written into every golden.
     */
    public static final String LEGEND = String.join(
        "\n",
        "# chunks: . not city   C core   a avoid ring   L link path   G gate   l lane   D detour",
        "#         P/S/T primary/secondary/tertiary road   F plaza   p park   o open lot without a park part",
        "#         B building   M multibuilding   = street without a road (predefined or capped)",
        "#         x empty lot   X excavation   K foundation (edge lots, 0011)",
        "# zones:  C core   E edge ring   1 2 transition rings   a avoid ring   ^ crater cap   . none",
        "# floors: building floors 1-9, + for 10 or more, 0 for a roof-only building, . no building");

    /** The character of a chunk on the chunk map. */
    static char chunkChar(LayoutPlanner p, int x, int z) {
        ChunkPlan plan = p.plan(x, z);
        Zone zone = plan.zone()
            .zone();
        LaneRole role = plan.lane()
            .role();
        if (zone == Zone.CORE) {
            return 'C';
        }
        if (zone == Zone.AVOID_RING) {
            return 'a';
        }
        if (!plan.isCity()) {
            return role == LaneRole.LINK ? 'L' : role == LaneRole.GATE ? 'G' : '.';
        }
        if (plan.hasBuilding()) {
            return plan.multiPos()
                .isMulti() ? 'M' : 'B';
        }
        switch (role) {
            case GATE:
                return 'G';
            case LANE:
                return 'l';
            case DETOUR:
                return 'D';
            default:
                break;
        }
        if (plan.lot() != LotKind.NONE) {
            return plan.lot() == LotKind.BARE ? 'x' : plan.lot() == LotKind.PIT ? 'X' : 'K';
        }
        if (plan.streetType() == StreetType.FULL) {
            return 'F';
        }
        switch (plan.plannedRoad()) {
            case PRIMARY:
                return 'P';
            case SECONDARY:
                return 'S';
            case TERTIARY:
                return 'T';
            default:
                break;
        }
        if (plan.streetType() == StreetType.PARK) {
            return plan.parkPart() != null ? 'p' : 'o';
        }
        return '=';
    }

    static char zoneChar(LayoutPlanner p, int x, int z) {
        ChunkPlan plan = p.plan(x, z);
        switch (plan.zone()
            .zone()) {
            case CORE:
                return 'C';
            case EDGE_RING:
                return 'E';
            case TRANSITION_1:
                return '1';
            case TRANSITION_2:
                return '2';
            case AVOID_RING:
                return 'a';
            default:
                return plan.zone()
                    .inCraterBasin() ? '^' : '.';
        }
    }

    static char floorChar(LayoutPlanner p, int x, int z) {
        ChunkPlan plan = p.plan(x, z);
        if (!plan.hasBuilding()) {
            return '.';
        }
        return plan.floors() >= 10 ? '+' : (char) ('0' + plan.floors());
    }

    /** Draws the three maps of a rectangle of chunks, north at the top. */
    public static String maps(LayoutPlanner p, int minX, int minZ, int maxX, int maxZ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# region x ")
            .append(minX)
            .append("..")
            .append(maxX)
            .append(", z ")
            .append(minZ)
            .append("..")
            .append(maxZ)
            .append('\n');
        String[] titles = { "chunks", "zones", "floors" };
        for (int map = 0; map < 3; map++) {
            sb.append(titles[map])
                .append('\n');
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    sb.append(map == 0 ? chunkChar(p, x, z) : map == 1 ? zoneChar(p, x, z) : floorChar(p, x, z));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /** Every decision of a chunk's plan and layout, on one line. */
    public static String describe(LayoutPlanner p, int x, int z) {
        ChunkPlan c = p.plan(x, z);
        ChunkLayout l = p.layout(x, z);
        StringBuilder sb = new StringBuilder();
        sb.append(x)
            .append(',')
            .append(z)
            .append(c.isCity() ? " city" : " -")
            .append(" lvl")
            .append(c.cityLevel())
            .append(' ')
            .append(c.cityStyle())
            .append(" b=")
            .append(c.building())
            .append(' ')
            .append(c.multiPos())
            .append(" f")
            .append(c.floors())
            .append(" c")
            .append(c.cellars())
            .append(" parts=")
            .append(Arrays.toString(c.floorParts))
            .append(Arrays.toString(c.floorParts2))
            .append(" road=")
            .append(c.rawRoad())
            .append('/')
            .append(c.plannedRoad())
            .append(' ')
            .append(c.streetType())
            .append(" park=")
            .append(c.parkPart())
            .append(" fountain=")
            .append(c.fountainPart())
            .append(" door=")
            .append(c.door())
            .append(" bridge=")
            .append(c.bridgePart())
            .append('/')
            .append(c.largeBridgePart())
            .append(" stair=")
            .append(c.stairPart())
            .append('@')
            .append(Float.floatToIntBits(c.stairPriority()))
            .append(" pal=")
            .append(c.palettes())
            .append(" urb=")
            .append(Float.floatToIntBits(c.intensity()))
            .append(" lot=")
            .append(c.lot())
            .append('/')
            .append(c.lotDepth())
            .append('/')
            .append(c.lotColumnFloors())
            .append(" noLoot=")
            .append(c.noLoot())
            .append(" ruin=")
            .append(Float.floatToIntBits(c.ruinHeight()))
            .append(" corr=")
            .append(c.xRailCorridor())
            .append(c.zRailCorridor())
            .append(" dungeon=")
            .append(c.railDungeonPart())
            .append(" front=")
            .append(c.frontPart())
            .append(" conn=")
            .append(Arrays.toString(c.connectionAtX))
            .append(Arrays.toString(c.connectionAtZ))
            .append(" zone=")
            .append(c.zone())
            .append(" lane=")
            .append(c.lane())
            .append(" | slope=")
            .append(l.slope())
            .append(" stair=")
            .append(l.stair())
            .append(" elevated=")
            .append(l.isElevatedPark())
            .append(" bridges=")
            .append(l.bridgePart(Orientation.X))
            .append('/')
            .append(l.bridgePart(Orientation.Z))
            .append(" corridors=")
            .append(l.hasCorridor(Orientation.X))
            .append(l.hasCorridor(Orientation.Z))
            .append(" fronts=");
        for (Direction d : Direction.values()) {
            sb.append(l.hasFrontPartFrom(d) ? '1' : '0');
        }
        sb.append(" core=");
        for (Direction d : Direction.values()) {
            sb.append(l.facesCore(d) ? '1' : '0');
        }
        sb.append(' ')
            .append(l.corner());
        return sb.toString();
    }

    /** Describes every chunk of a rectangle, one line each, row by row. */
    public static String details(LayoutPlanner p, int minX, int minZ, int maxX, int maxZ) {
        StringBuilder sb = new StringBuilder();
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                sb.append(describe(p, x, z))
                    .append('\n');
            }
        }
        return sb.toString();
    }
}
