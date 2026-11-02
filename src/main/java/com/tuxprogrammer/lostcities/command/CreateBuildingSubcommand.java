package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandCreateBuilding.java
// Avoids: CORE-32

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ForcedBuilding;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * {@code /lostcities createbuilding <building> <floors> <cellars> <x> <y> <z>}: builds a building into the chunk that
 * holds the position, its lowest cellar at the position's Y, for asset authors (docs/original-spec/01-core/commands.md,
 * {@code createbuilding}; docs/new-spec/01-core/commands.md). Floors 1 to 20 and cellars 0 to 10 as upstream, further
 * limited so the roof part stays below Y {@value #TOP_Y} (0005 §6). Coordinates take vanilla's {@code ~} syntax.
 * Operator level 2; the console may run it too.
 *
 * <p>
 * Avoids CORE-32: the levels are drawn from a copy of the chunk's plan ({@code LayoutPlanner.forcedBuilding}); the
 * cached layout that generation and every query read is never changed.
 *
 * <p>
 * Side: server.
 */
public final class CreateBuildingSubcommand implements Subcommand {

    /** Upstream's floor range. */
    static final int MIN_FLOORS = 1;
    static final int MAX_FLOORS = 20;
    /** Upstream's cellar range. */
    static final int MAX_CELLARS = 10;
    /** The highest Y a building's top level may reach (0005 §6). */
    static final int TOP_Y = 254;

    @Override
    public String name() {
        return "createbuilding";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 6) {
            throw new WrongUsageException(LangKeys.CREATEBUILDING_USAGE);
        }
        OverlayWorld overlay = CommandSupport.cities(sender);
        AssetId id = PartEditor.id(args[0]);
        if (id == null || overlay.cities()
            .context()
            .assets()
            .building(id) == null) {
            throw new CommandException(LangKeys.CREATEBUILDING_UNKNOWN, args[0]);
        }
        int floors = CommandBase.parseIntBounded(sender, args[1], MIN_FLOORS, MAX_FLOORS);
        int cellars = CommandBase.parseIntBounded(sender, args[2], 0, MAX_CELLARS);
        ChunkCoordinates at = sender.getPlayerCoordinates();
        int x = CommandSupport.coordinate(sender, at.posX, args[3]);
        int y = CommandSupport.coordinate(sender, at.posY, args[4]);
        int z = CommandSupport.coordinate(sender, at.posZ, args[5]);
        int fit = floorsThatFit(y, cellars);
        if (y < WorldLimits.MIN_Y + 1 || floors > fit) {
            throw new CommandException(LangKeys.CREATEBUILDING_TOO_HIGH, floors, cellars, y, TOP_Y, Math.max(0, fit));
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ForcedBuilding building = overlay.cities()
            .planner()
            .forcedBuilding(chunkX, chunkZ, id.toString(), floors, cellars);
        if (building == null) {
            throw new CommandException(LangKeys.CREATEBUILDING_NO_PARTS, id.toString());
        }
        new PartEditor(overlay).build(chunkX, chunkZ, building, y);
        LostCities.LOG.info(
            "{} built {} in chunk {},{} of dimension {} from Y {}",
            sender.getCommandSenderName(),
            building,
            chunkX,
            chunkZ,
            overlay.dimension(),
            y);
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.CREATEBUILDING_DONE,
                id.toString(),
                chunkX,
                chunkZ,
                floors,
                cellars,
                y));
    }

    /**
     * Returns how many floors fit above a lowest cellar at a Y: the roof level's top must stay at or below
     * {@link #TOP_Y}.
     *
     * @param bottom  block Y of the lowest cellar
     * @param cellars the cellars
     * @return the most floors, which may be below {@link #MIN_FLOORS}
     */
    static int floorsThatFit(int bottom, int cellars) {
        // Levels -cellars .. floors, each FLOOR_HEIGHT high: the top level ends at bottom + (cellars + floors + 1) * 6
        // - 1.
        return (TOP_Y + 1 - bottom) / ChunkPlan.FLOOR_HEIGHT - cellars - 1;
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        if (args.length != 1 || !LoadedAssets.isLoaded()) {
            return Collections.emptyList();
        }
        return CommandSupport.completeIds(
            args,
            LoadedAssets.current()
                .ids(AssetType.BUILDINGS));
    }
}
