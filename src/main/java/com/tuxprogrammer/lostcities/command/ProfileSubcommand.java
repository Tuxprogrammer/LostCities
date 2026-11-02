package com.tuxprogrammer.lostcities.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;
import com.tuxprogrammer.lostcities.worldgen.overlay.ProfileSavedData;

/**
 * {@code /lostcities profile} shows the profile pinned into the sender's dimension; {@code /lostcities profile pin
 * <profile> confirm} pins one now, which opts an existing world in or re-pins a world (docs/design/0005-rwg-overlay.md
 * §7, docs/new-spec/01-core/commands.md). Without {@code confirm} it only explains that chunks generated before will
 * not match. Operator level 2.
 *
 * <p>
 * Side: server.
 */
public final class ProfileSubcommand implements Subcommand {

    static final String PIN = "pin";
    static final String CONFIRM = "confirm";

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        World senderWorld = sender.getEntityWorld();
        if (!(senderWorld instanceof WorldServer world)) {
            throw new CommandException(LangKeys.PROFILE_USAGE);
        }
        int dimension = world.provider.dimensionId;
        if (args.length == 0) {
            ProfileSnapshot snapshot = ProfileSavedData.get(world)
                .snapshot(dimension);
            if (snapshot == null) {
                sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_NONE, dimension));
            } else {
                sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_SHOW, dimension, snapshot.summary()));
            }
            return;
        }
        if (!PIN.equals(args[0]) || args.length < 2 || args.length > 3) {
            throw new WrongUsageException(LangKeys.PROFILE_USAGE);
        }
        String profile = args[1];
        if (!BuiltinProfiles.names()
            .contains(profile)) {
            throw new CommandException(LangKeys.PROFILE_UNKNOWN, profile, String.join(", ", BuiltinProfiles.names()));
        }
        if (args.length < 3 || !CONFIRM.equals(args[2])) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_CONFIRM, profile, dimension));
            return;
        }
        ProfileSnapshot snapshot = Overlays.INSTANCE.pin(world, profile);
        if (snapshot == null) {
            throw new CommandException(LangKeys.PROFILE_UNSUPPORTED, dimension, HeightOracles.worldType(world));
        }
        LostCities.LOG.info("{} pinned profile {} into dimension {}", sender.getCommandSenderName(), profile, dimension);
        sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_PINNED, profile, dimension));
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        String[] options;
        if (args.length == 1) {
            options = new String[] { PIN };
        } else if (args.length == 2 && PIN.equals(args[0])) {
            options = BuiltinProfiles.names()
                .toArray(new String[0]);
        } else if (args.length == 3 && PIN.equals(args[0])) {
            options = new String[] { CONFIRM };
        } else {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<String> matches = CommandBase.getListOfStringsMatchingLastWord(args, options);
        return matches;
    }
}
