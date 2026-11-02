package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandLocatePart.java
// Avoids: CORE-30, CORE-31

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.editor.EditModeData;
import com.tuxprogrammer.lostcities.editor.EditRecords;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.editor.PartRecord;
import com.tuxprogrammer.lostcities.layout.ChunkSpiral;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities locatepart <part>}: where generation recorded a part near the player, from the records of an
 * {@code editMode} world, nearest chunk first over upstream's spiral of radius {@value ChunkSpiral#LOCATE_RADIUS}
 * chunks (docs/original-spec/01-core/part-editor.md, Commands, item 5). Operator level 2; players only.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids CORE-30: the recorded and the asked names are compared in one canonical form, so {@code street1} and
 * {@code lostcities:street1} both find the bundled part.</li>
 * <li>Avoids CORE-31: the search stops at {@value #MAX_HITS} hits overall, not per chunk.</li>
 * </ul>
 *
 * <p>
 * Side: server.
 */
public final class LocatePartSubcommand implements Subcommand {

    /** Upstream's intended result limit. */
    static final int MAX_HITS = 7;

    @Override
    public String name() {
        return "locatepart";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = CommandSupport.player(sender, name());
        if (args.length != 1) {
            throw new WrongUsageException(LangKeys.LOCATEPART_USAGE);
        }
        OverlayWorld overlay = CommandSupport.editWorld(sender);
        AssetId id = PartEditor.id(args[0]);
        if (id == null) {
            throw new CommandException(LangKeys.EDITOR_UNKNOWN_PART, args[0]);
        }
        List<int[]> hits = find(
            EditModeData.get(overlay.world())
                .records(),
            overlay.dimension(),
            id,
            MathHelper.floor_double(player.posX) >> 4,
            MathHelper.floor_double(player.posZ) >> 4);
        if (hits.isEmpty()) {
            sender.addChatMessage(
                new ChatComponentTranslation(LangKeys.LOCATEPART_NONE, id.toString(), ChunkSpiral.LOCATE_RADIUS));
            return;
        }
        for (int[] hit : hits) {
            sender.addChatMessage(
                new ChatComponentTranslation(LangKeys.LOCATEPART_FOUND, id.toString(), hit[0], hit[1], hit[2]));
        }
    }

    /**
     * Finds a part's records around a chunk.
     *
     * @param records   the save's records
     * @param dimension the dimension ID
     * @param id        the part, canonical
     * @param chunkX    the center chunk X
     * @param chunkZ    the center chunk Z
     * @return up to {@link #MAX_HITS} positions {@code {x, y, z}}: the chunk's center and the part's first slice
     */
    static List<int[]> find(EditRecords records, int dimension, AssetId id, int chunkX, int chunkZ) {
        List<int[]> hits = new ArrayList<>();
        ChunkSpiral.walk(chunkX, chunkZ, ChunkSpiral.LOCATE_RADIUS, (x, z) -> {
            for (PartRecord record : records.at(dimension, x, z)) {
                if (id.equals(record.part())) {
                    hits.add(new int[] { (x << 4) + 8, record.y(), (z << 4) + 8 });
                    if (hits.size() >= MAX_HITS) {
                        // Avoids CORE-31: the limit ends the whole search.
                        return false;
                    }
                }
            }
            return true;
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
                .ids(AssetType.PARTS));
    }
}
