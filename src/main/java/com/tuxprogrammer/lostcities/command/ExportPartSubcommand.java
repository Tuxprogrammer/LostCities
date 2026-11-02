package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandExportPart.java
// Avoids: CORE-28, CORE-29

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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.editor.EditSession;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.editor.PartExport;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

import cpw.mods.fml.common.Loader;

/**
 * {@code /lostcities exportpart <name> [confirm]}: writes the player's edited part, read back from the world, to
 * {@code config/lostcitiesnh/export/parts/<name>.json} (docs/original-spec/01-core/part-editor.md, Commands, item 6;
 * docs/new-spec/01-core/index.md, Part editor). The file is kept out of the live assets, so an export never goes live
 * on the next load by accident; the author moves it into {@code config/lostcitiesnh/assets/} (P-CORE-4, D31).
 * Operator level 2; players only.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids CORE-28: the name must match {@code [a-z0-9_]+} and the folder is fixed, so no other file can be
 * written; an existing file is replaced only with {@code confirm}.</li>
 * <li>Avoids CORE-29: {@link PartExport} sorts {@code missingpalette} and fails cleanly when the character pool runs
 * out; the file is written in one call that closes it on every path.</li>
 * </ul>
 *
 * <p>
 * Side: server.
 */
public final class ExportPartSubcommand implements Subcommand {

    /** What an export file's name may be: lower-case letters, digits and underscores. */
    static final Pattern NAME = Pattern.compile("[a-z0-9_]+");
    /** The export folder, under the game's config folder. */
    public static final String FOLDER = "lostcitiesnh/export/parts";
    /** The extension the command adds. */
    static final String EXTENSION = ".json";

    @Override
    public String name() {
        return "exportpart";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = CommandSupport.player(sender, name());
        if (args.length < 1 || args.length > 2 || args.length == 2 && !ProfileSubcommand.CONFIRM.equals(args[1])) {
            throw new WrongUsageException(LangKeys.EXPORTPART_USAGE);
        }
        String name = args[0];
        if (!validName(name)) {
            throw new CommandException(LangKeys.EXPORTPART_NAME, name);
        }
        OverlayWorld overlay = CommandSupport.cities(sender);
        EditSession session = overlay.editSessions()
            .get(player.getUniqueID());
        if (session == null) {
            throw new CommandException(LangKeys.EDITOR_NOT_EDITING);
        }
        Path file = file(
            Loader.instance()
                .getConfigDir(),
            name);
        String shown = FOLDER + "/" + name + EXTENSION;
        if (Files.exists(file) && args.length < 2) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.EXPORTPART_EXISTS, shown));
            return;
        }
        PartExport.Result result = new PartEditor(overlay).export(session);
        if (result == null) {
            throw new CommandException(
                LangKeys.EDITOR_UNKNOWN_PART,
                session.part()
                    .toString());
        }
        if (result.poolExhausted()) {
            throw new CommandException(LangKeys.EXPORTPART_POOL, PartExport.POOL.size());
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(
                file,
                result.json()
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LostCities.LOG.error("exportpart could not write {}", file, e);
            throw new CommandException(LangKeys.EXPORTPART_FAILED, shown, e.getMessage());
        }
        LostCities.LOG.info("{} exported part {} to {}", sender.getCommandSenderName(), session.part(), file);
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.EXPORTPART_DONE,
                session.part()
                    .toString(),
                shown,
                result.newBlocks()));
    }

    /**
     * Returns whether an export name is allowed (avoids CORE-28).
     *
     * @param name the name as typed
     * @return whether it matches {@code [a-z0-9_]+}
     */
    static boolean validName(String name) {
        return NAME.matcher(name)
            .matches();
    }

    /**
     * Returns the file an export name writes.
     *
     * @param configDir the game's config folder
     * @param name      a valid name
     * @return {@code <config>/lostcitiesnh/export/parts/<name>.json}
     */
    static Path file(File configDir, String name) {
        return new File(new File(configDir, FOLDER), name + EXTENSION).toPath();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandBase.getListOfStringsMatchingLastWord(args, ProfileSubcommand.CONFIRM);
        }
        return Collections.emptyList();
    }
}
