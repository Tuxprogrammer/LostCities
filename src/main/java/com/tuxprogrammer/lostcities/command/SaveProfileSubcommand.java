package com.tuxprogrammer.lostcities.command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.Profile;
import com.tuxprogrammer.lostcities.config.profile.ProfileJson;
import com.tuxprogrammer.lostcities.config.profile.ProfileRegistry;
import com.tuxprogrammer.lostcities.config.profile.ProfileSetup;
import com.tuxprogrammer.lostcities.config.profile.ProfileTexts;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;

import cpw.mods.fml.common.Loader;

/**
 * {@code /lostcities saveprofile <profile> <newName> [confirm]}: writes a profile in upstream's JSON format to
 * {@code config/lostcitiesnh/profiles/<newName>.json}, ready to edit and name in {@code dimensionProfiles}, then reads
 * the profiles again so the copy can be pinned at once (docs/new-spec/01-core/commands.md). Operator level 2, unlike
 * upstream's level 0 (avoids CORE-27).
 * <ul>
 * <li>The new name matches {@code [a-z0-9_]+}, so the write stays in the profiles folder, and is not a built-in's
 * (P-PROF-2, D31).</li>
 * <li>An existing file is replaced only with {@code confirm}.</li>
 * <li>A built-in's texts are written as their English words, for the owner to edit.</li>
 * <li>Errors are reported as errors, never as success.</li>
 * </ul>
 *
 * <p>
 * Side: server.
 */
public final class SaveProfileSubcommand implements Subcommand {

    /** What a new profile name may be: lower-case letters, digits and underscores. */
    static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

    @Override
    public String name() {
        return "saveprofile";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || args.length > 3 || args.length == 3 && !ProfileSubcommand.CONFIRM.equals(args[2])) {
            throw new WrongUsageException(LangKeys.SAVEPROFILE_USAGE);
        }
        Profile profile = ProfileSubcommand.require(args[0]);
        String newName = args[1];
        if (!NAME.matcher(newName)
            .matches()) {
            throw new CommandException(LangKeys.SAVEPROFILE_NAME, newName);
        }
        if (BuiltinProfiles.get(newName) != null) {
            throw new CommandException(LangKeys.SAVEPROFILE_BUILT_IN, newName);
        }
        File configDir = Loader.instance()
            .getConfigDir();
        Path file = new File(new File(configDir, ProfileRegistry.FOLDER), newName + ProfileJson.EXTENSION).toPath();
        String shown = ProfileRegistry.FOLDER + "/" + newName + ProfileJson.EXTENSION;
        if (Files.exists(file) && args.length < 3) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.SAVEPROFILE_EXISTS, shown));
            return;
        }
        String json = ProfileJson.write(profile, profile.builtIn() ? ProfileTexts::english : text -> text);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LostCities.LOG.error("saveprofile could not write {}", file, e);
            throw new CommandException(LangKeys.SAVEPROFILE_FAILED, shown, e.getMessage());
        }
        LostCities.LOG.info("{} saved profile {} as {}", sender.getCommandSenderName(), profile.name(), file);
        if (LoadedAssets.isLoaded()) {
            ProfileSetup.reload(configDir, LoadedAssets.current());
        }
        sender.addChatMessage(new ChatComponentTranslation(LangKeys.SAVEPROFILE_SAVED, profile.name(), shown, newName));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(
                args,
                ProfileRegistry.INSTANCE.names()
                    .toArray(new String[0]));
        }
        if (args.length == 3) {
            return CommandBase.getListOfStringsMatchingLastWord(args, ProfileSubcommand.CONFIRM);
        }
        return Collections.emptyList();
    }
}
