package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandCreatePart.java

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.editor.PartRecord;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities createpart <part> <x> <y> <z>}: places a part in the chunk that holds the position, its first
 * slice at the position's Y, with that chunk's palette, and starts the player's editing session on it
 * (docs/original-spec/01-core/part-editor.md, Commands, item 1). The world must have real cities; it need not be an
 * {@code editMode} world, as upstream. Coordinates take vanilla's {@code ~} syntax. Operator level 2; players only,
 * since a session is a player's.
 *
 * <p>
 * Side: server.
 */
public final class CreatePartSubcommand implements Subcommand {

    @Override
    public String name() {
        return "createpart";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = CommandSupport.player(sender, name());
        if (args.length != 4) {
            throw new WrongUsageException(LangKeys.CREATEPART_USAGE);
        }
        OverlayWorld overlay = CommandSupport.cities(sender);
        PartEditor editor = new PartEditor(overlay);
        Part part = editor.part(args[0]);
        if (part == null) {
            throw new CommandException(LangKeys.EDITOR_UNKNOWN_PART, args[0]);
        }
        int x = CommandSupport.coordinate(sender, player.posX, args[1]);
        int y = CommandSupport.coordinate(sender, player.posY, args[2]);
        int z = CommandSupport.coordinate(sender, player.posZ, args[3]);
        editor.start(
            player.getUniqueID(),
            x >> 4,
            z >> 4,
            part,
            new PartRecord(part.id(), y, Transform.ROTATE_NONE, 0, 0),
            true);
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.CREATEPART_DONE,
                part.id()
                    .toString(),
                x,
                y,
                z));
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
