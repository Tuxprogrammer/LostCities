package com.tuxprogrammer.lostcities.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

/**
 * One subcommand of {@code /lostcities} (docs/new-spec/01-core/commands.md). The parent command checks the permission
 * level before calling {@link #execute}.
 *
 * <p>
 * Output is always a {@code ChatComponentTranslation} with a {@code lostcitiesnh.command.*} lang key sent to the
 * sender; diagnostics go to the mod logger.
 *
 * <p>
 * Side: server.
 */
public interface Subcommand {

    /** Vanilla's game-master permission level, the level every subcommand needs (P-CORE-2, decision log D31). */
    int OPERATOR_LEVEL = 2;

    /** Returns the subcommand's name, the first argument of {@code /lostcities}. */
    String name();

    /** Returns other names the subcommand answers to. */
    default List<String> aliases() {
        return Collections.emptyList();
    }

    /** Returns the permission level the sender needs. */
    default int permissionLevel() {
        return OPERATOR_LEVEL;
    }

    /**
     * Runs the subcommand.
     *
     * @param sender who typed it
     * @param args   the arguments after the subcommand's name
     * @throws CommandException with a lang key, for wrong usage; vanilla sends it to the sender
     */
    void execute(ICommandSender sender, String[] args) throws CommandException;

    /**
     * Returns completions for the last argument.
     *
     * @param sender who is typing
     * @param args   the arguments after the subcommand's name, the last one partial
     * @return the completions; empty for none
     */
    default List<String> tabComplete(ICommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
