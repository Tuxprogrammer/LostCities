package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandListParts.java

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;

import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.editor.EditModeData;
import com.tuxprogrammer.lostcities.editor.PartRecord;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities listparts}: one line per part generation recorded in the player's chunk, in writing order
 * (docs/original-spec/01-core/part-editor.md, Commands, item 4). Needs an {@code editMode} world. Operator level 2;
 * players only.
 *
 * <p>
 * Side: server.
 */
public final class ListPartsSubcommand implements Subcommand {

    @Override
    public String name() {
        return "listparts";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = CommandSupport.player(sender, name());
        if (args.length != 0) {
            throw new WrongUsageException(LangKeys.COMMAND_USAGE);
        }
        OverlayWorld overlay = CommandSupport.editWorld(sender);
        int chunkX = MathHelper.floor_double(player.posX) >> 4;
        int chunkZ = MathHelper.floor_double(player.posZ) >> 4;
        List<PartRecord> records = EditModeData.get(overlay.world())
            .records()
            .at(overlay.dimension(), chunkX, chunkZ);
        if (records.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.LISTPARTS_NONE, chunkX, chunkZ));
            return;
        }
        for (PartRecord record : records) {
            sender.addChatMessage(
                new ChatComponentTranslation(
                    LangKeys.LISTPARTS_ENTRY,
                    record.part()
                        .toString(),
                    record.y()));
        }
    }
}
