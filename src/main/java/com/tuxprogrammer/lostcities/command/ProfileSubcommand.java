package com.tuxprogrammer.lostcities.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.config.profile.Profile;
import com.tuxprogrammer.lostcities.config.profile.ProfileOption;
import com.tuxprogrammer.lostcities.config.profile.ProfileOptions;
import com.tuxprogrammer.lostcities.config.profile.ProfileRegistry;
import com.tuxprogrammer.lostcities.config.profile.ProfileSetup;
import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;
import com.tuxprogrammer.lostcities.worldgen.overlay.ProfileSavedData;

import cpw.mods.fml.common.Loader;

/**
 * {@code /lostcities profile}: the profiles a server can pin and the one a dimension has pinned
 * (docs/design/0005-rwg-overlay.md §7, docs/new-spec/01-core/commands.md). Operator level 2.
 * <ul>
 * <li>no argument: the sender's dimension's pinned profile, its description and summary;</li>
 * <li>{@code list}: every profile that can be pinned, with its description;</li>
 * <li>{@code info <profile>}: a profile's texts and the options it sets away from the defaults (all of them to the
 * log);</li>
 * <li>{@code pin <profile> confirm}: pins a profile now, which opts an existing world in or re-pins a world; without
 * {@code confirm} it only explains that chunks generated before will not match;</li>
 * <li>{@code reload}: reads {@code config/lostcitiesnh/profiles/} again; worlds already pinned are unaffected.</li>
 * </ul>
 * Profile texts go out as translations, so a built-in's lang key is translated on the client and a file's literal text
 * shows as it is.
 *
 * <p>
 * Side: server.
 */
public final class ProfileSubcommand implements Subcommand {

    static final String PIN = "pin";
    static final String CONFIRM = "confirm";
    static final String LIST = "list";
    static final String INFO = "info";
    static final String RELOAD = "reload";

    /** Most changed options {@code info} lists in chat; the log gets them all. */
    static final int MAX_CHANGES_IN_CHAT = 12;

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            show(sender);
            return;
        }
        switch (args[0]) {
            case LIST:
                list(sender);
                return;
            case INFO:
                if (args.length != 2) {
                    throw new WrongUsageException(LangKeys.PROFILE_USAGE);
                }
                info(sender, require(args[1]));
                return;
            case RELOAD:
                reload(sender);
                return;
            case PIN:
                pin(sender, args);
                return;
            default:
                throw new WrongUsageException(LangKeys.PROFILE_USAGE);
        }
    }

    private static WorldServer world(ICommandSender sender) {
        World senderWorld = sender.getEntityWorld();
        if (!(senderWorld instanceof WorldServer world)) {
            throw new CommandException(LangKeys.PROFILE_USAGE);
        }
        return world;
    }

    private static void show(ICommandSender sender) {
        WorldServer world = world(sender);
        int dimension = world.provider.dimensionId;
        ProfileSnapshot snapshot = ProfileSavedData.get(world)
            .snapshot(dimension);
        if (snapshot == null) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_NONE, dimension));
            return;
        }
        sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_SHOW, dimension, snapshot.summary()));
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.PROFILE_TEXT,
                text(
                    snapshot.profileValues()
                        .getString("description"))));
    }

    private static void list(ICommandSender sender) {
        ProfileRegistry registry = ProfileRegistry.INSTANCE;
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.PROFILE_LIST,
                registry.names()
                    .size()));
        for (String name : registry.names()) {
            Profile profile = registry.get(name);
            sender.addChatMessage(
                new ChatComponentTranslation(
                    LangKeys.PROFILE_LIST_ENTRY,
                    name,
                    profile.builtIn() ? new ChatComponentTranslation(LangKeys.PROFILE_BUILT_IN)
                        : new ChatComponentTranslation(LangKeys.PROFILE_FILE),
                    text(
                        profile.values()
                            .getString("description"))));
        }
    }

    private static void info(ICommandSender sender, Profile profile) {
        ProfileValues values = profile.values();
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.PROFILE_INFO,
                profile.name(),
                profile.source(),
                text(values.getString("description"))));
        for (String key : new String[] { "extraDescription", "warning" }) {
            String value = values.getString(key);
            if (!value.isEmpty()) {
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "warning".equals(key) ? LangKeys.PROFILE_INFO_WARNING : LangKeys.PROFILE_TEXT,
                        text(value)));
            }
        }
        List<String> changes = changes(values);
        LostCities.LOG.info("Profile {} ({}) differs from default in: {}", profile.name(), profile.source(), changes);
        if (changes.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_INFO_DEFAULTS));
            return;
        }
        List<String> shown = changes.size() > MAX_CHANGES_IN_CHAT ? changes.subList(0, MAX_CHANGES_IN_CHAT) : changes;
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.PROFILE_INFO_CHANGES,
                changes.size(),
                String.join(", ", shown) + (shown.size() < changes.size() ? ", ..." : "")));
    }

    /** Returns {@code key=value} for every option a profile sets away from its default, texts excepted. */
    static List<String> changes(ProfileValues values) {
        Map<String, String> defaults = ProfileValues.defaults()
            .asMap();
        List<String> out = new ArrayList<>();
        for (ProfileOption option : ProfileOptions.all()) {
            String key = option.key();
            if (!option.kept() || "description".equals(key)
                || "extraDescription".equals(key)
                || "warning".equals(key)) {
                continue;
            }
            String value = values.text(key);
            if (!value.equals(defaults.get(key))) {
                out.add(key + "=" + value);
            }
        }
        return out;
    }

    private static void reload(ICommandSender sender) {
        if (!LoadedAssets.isLoaded()) {
            throw new CommandException(LangKeys.PROFILE_USAGE);
        }
        List<ProfileRegistry.Message> report = ProfileSetup.reload(
            Loader.instance()
                .getConfigDir(),
            LoadedAssets.current());
        int rejected = 0;
        for (ProfileRegistry.Message message : report) {
            if (message.error()) {
                rejected++;
            }
        }
        LostCities.LOG.info("{} reloaded the profiles", sender.getCommandSenderName());
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.PROFILE_RELOADED,
                ProfileRegistry.INSTANCE.names()
                    .size(),
                rejected,
                report.size()));
    }

    private static void pin(ICommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            throw new WrongUsageException(LangKeys.PROFILE_USAGE);
        }
        WorldServer world = world(sender);
        int dimension = world.provider.dimensionId;
        String profile = require(args[1]).name();
        if (args.length < 3 || !CONFIRM.equals(args[2])) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_CONFIRM, profile, dimension));
            return;
        }
        ProfileSnapshot snapshot = Overlays.INSTANCE.pin(world, profile);
        if (snapshot == null) {
            throw new CommandException(LangKeys.PROFILE_UNSUPPORTED, dimension, HeightOracles.worldType(world));
        }
        LostCities.LOG
            .info("{} pinned profile {} into dimension {}", sender.getCommandSenderName(), profile, dimension);
        sender.addChatMessage(new ChatComponentTranslation(LangKeys.PROFILE_PINNED, profile, dimension));
    }

    /**
     * Returns a profile that can be pinned, or throws the reason it cannot.
     *
     * @param name the name the sender typed
     * @return the profile
     * @throws CommandException naming why the profile is rejected, or the known profiles
     */
    static Profile require(String name) {
        Profile profile = ProfileRegistry.INSTANCE.get(name);
        if (profile != null) {
            return profile;
        }
        String reason = ProfileRegistry.INSTANCE.rejection(name);
        if (reason != null) {
            throw new CommandException(LangKeys.PROFILE_REJECTED, name, reason);
        }
        throw new CommandException(LangKeys.PROFILE_UNKNOWN, name, String.join(", ", ProfileRegistry.INSTANCE.names()));
    }

    /** Returns a profile text as a chat component: a lang key translates on the client; literal text shows as is. */
    static IChatComponent text(String text) {
        return new ChatComponentTranslation(text);
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        String[] options;
        if (args.length == 1) {
            options = new String[] { LIST, INFO, PIN, RELOAD };
        } else if (args.length == 2 && (PIN.equals(args[0]) || INFO.equals(args[0]))) {
            options = ProfileRegistry.INSTANCE.names()
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
