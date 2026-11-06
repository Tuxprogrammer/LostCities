package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandMap.java
// Avoids: CORE-26

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities map}: upstream's 41 × 41 chunk map around the sender, north at the top, to the log as
 * {@code [LCNH-MAP]} lines, with one summary line to the sender (docs/original-spec/01-core/commands.md, {@code map};
 * docs/new-spec/01-core/commands.md). {@code B} is a city chunk with a building, {@code +} another city chunk,
 * {@code #} a landmark core (new), {@code .} a non-city chunk with a highway, a space anything else. Operator level 2,
 * unlike upstream's level 0 (avoids CORE-26); upstream printed to standard output only.
 *
 * <p>
 * Side: server.
 */
public final class MapSubcommand implements Subcommand {

    /** Chunks from the center to the map's edge: upstream's −20 to +20. */
    static final int RADIUS = 20;
    /** Prefix of the log lines. */
    static final String TAG = "[LCNH-MAP] ";

    /** A map and what it counted. */
    static final class Map {

        final List<String> rows = new ArrayList<>();
        int buildings;
        int city;
        int highways;
        int cores;
    }

    @Override
    public String name() {
        return "map";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        int[] block = CommandSupport.blockTarget(sender, args, name());
        OverlayWorld overlay = CommandSupport.cities(sender);
        int chunkX = block[0] >> 4;
        int chunkZ = block[1] >> 4;
        Map map = draw(
            overlay.cities()
                .planner(),
            chunkX,
            chunkZ,
            RADIUS);
        LostCities.LOG.info(
            "{}dim {} around chunk {},{} (block {},{}): {} buildings, {} city, {} highway, {} core",
            TAG,
            overlay.dimension(),
            chunkX,
            chunkZ,
            block[0],
            block[1],
            map.buildings,
            map.city,
            map.highways,
            map.cores);
        for (String row : map.rows) {
            LostCities.LOG.info("{}|{}|", TAG, row);
        }
        int size = 2 * RADIUS + 1;
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.MAP_DONE,
                size,
                size,
                chunkX,
                chunkZ,
                map.buildings,
                map.city,
                map.highways,
                map.cores));
    }

    /**
     * Draws the map from the layout alone.
     *
     * @param planner the world's layout planner
     * @param centerX center chunk X
     * @param centerZ center chunk Z
     * @param radius  chunks from the center to the edge
     * @return the rows, north to south, each west to east, and the counts
     */
    static Map draw(LayoutPlanner planner, int centerX, int centerZ, int radius) {
        Map map = new Map();
        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            StringBuilder row = new StringBuilder(2 * radius + 1);
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                row.append(symbol(planner.plan(x, z), map));
            }
            map.rows.add(row.toString());
        }
        return map;
    }

    private static char symbol(ChunkPlan plan, Map map) {
        if (plan.zone()
            .zone() == Zone.CORE) {
            map.cores++;
            return '#';
        }
        if (plan.isCity()) {
            if (plan.hasBuilding()) {
                map.buildings++;
                return 'B';
            }
            map.city++;
            return '+';
        }
        if (plan.maxHighwayLevel() >= 0) {
            map.highways++;
            return '.';
        }
        return ' ';
    }
}
