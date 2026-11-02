package com.tuxprogrammer.lostcities.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.LangKeys;

/**
 * {@code /lostcities view <player> <x> <y> <z> <yaw> <pitch>}: moves a player to a position with a view direction,
 * which vanilla 1.7.10's {@code tp} cannot set, so a scripted real-pack client can take screenshots from fixed
 * viewpoints (tools/remote/README.md). Test hooks only, operator level 2; the server sends the move to the client the
 * way a teleport does.
 *
 * <p>
 * Side: server.
 */
public final class ViewSubcommand implements Subcommand {

    @Override
    public String name() {
        return "view";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (!DevEnvironment.testHooksEnabled()) {
            throw new CommandException(LangKeys.TEST_DISABLED);
        }
        if (args.length != 6) {
            throw new WrongUsageException(LangKeys.VIEW_USAGE);
        }
        EntityPlayerMP player = CommandBase.getPlayer(sender, args[0]);
        double x = CommandBase.parseDouble(sender, args[1]);
        double y = CommandBase.parseDoubleBounded(sender, args[2], 0, 512);
        double z = CommandBase.parseDouble(sender, args[3]);
        float yaw = (float) CommandBase.parseDoubleBounded(sender, args[4], -360, 360);
        float pitch = (float) CommandBase.parseDoubleBounded(sender, args[5], -90, 90);
        player.mountEntity(null);
        player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
        sender.addChatMessage(
            new ChatComponentTranslation(LangKeys.VIEW_DONE, player.getCommandSenderName(), (int) x, (int) y, (int) z));
    }
}
