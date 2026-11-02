package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandLocate.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ChunkSpiral;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities locate <building>}: where the building stands near the sender, from the layout alone (no chunk is
 * generated). It walks upstream's spiral of radius {@value ChunkSpiral#LOCATE_RADIUS} chunks and reports up to
 * {@value #MAX_HITS} chunks as {@code x, groundLevel, z} at the chunk's center (docs/original-spec/01-core/commands.md,
 * {@code locate}). A name is matched in its canonical form, so {@code building1} and {@code lostcities:building1} are
 * the same (ASSET-33). Operator level 2.
 *
 * <p>
 * Side: server.
 */
public final class LocateSubcommand implements Subcommand {

    /** Upstream's result limit. */
    static final int MAX_HITS = 7;

    @Override
    public String name() {
        return "locate";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(LangKeys.LOCATE_USAGE);
        }
        OverlayWorld overlay = CommandSupport.cities(sender);
        AssetId id = PartEditor.id(args[0]);
        if (id == null || overlay.cities()
            .context()
            .assets()
            .building(id) == null) {
            throw new CommandException(LangKeys.LOCATE_UNKNOWN, args[0]);
        }
        ChunkCoordinates at = sender.getPlayerCoordinates();
        List<int[]> hits = find(
            overlay.cities()
                .planner(),
            id,
            at.posX >> 4,
            at.posZ >> 4);
        if (hits.isEmpty()) {
            sender.addChatMessage(
                new ChatComponentTranslation(LangKeys.LOCATE_NONE, id.toString(), ChunkSpiral.LOCATE_RADIUS));
            return;
        }
        for (int[] hit : hits) {
            sender.addChatMessage(
                new ChatComponentTranslation(LangKeys.LOCATE_FOUND, id.toString(), hit[0], hit[1], hit[2]));
        }
    }

    /**
     * Finds a building's chunks around a chunk, nearest first.
     *
     * @param planner the world's layout planner
     * @param id      the building
     * @param chunkX  the center chunk X
     * @param chunkZ  the center chunk Z
     * @return up to {@link #MAX_HITS} positions {@code {x, groundLevel, z}}
     */
    static List<int[]> find(LayoutPlanner planner, AssetId id, int chunkX, int chunkZ) {
        List<int[]> hits = new ArrayList<>();
        ChunkSpiral.walk(chunkX, chunkZ, ChunkSpiral.LOCATE_RADIUS, (x, z) -> {
            ChunkPlan plan = planner.plan(x, z);
            if (plan.hasBuilding() && id.equals(PartEditor.id(plan.building()))) {
                hits.add(new int[] { (x << 4) + 8, plan.cityGroundLevel(), (z << 4) + 8 });
            }
            return hits.size() < MAX_HITS;
        });
        return hits;
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
