package com.tuxprogrammer.lostcities.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.scenario.TestSubcommand;

/**
 * {@code /lostcities}, with the aliases {@code /lost} and {@code /lostcitiesnh}: one 1.7.10 command that dispatches on
 * its first argument (docs/new-spec/01-core/commands.md, "Shape"). M04 adds {@code profile} and {@code timers} to M01's
 * {@code test} (alias {@code scenario}), and the test-hook-only {@code view}; M05 adds the test-hook-only {@code find};
 * M09 adds {@code saveprofile} and the profile listing; M10 adds {@code debug}, {@code map}, {@code locate},
 * {@code createbuilding} and the part editor's {@code createpart}, {@code editpart}, {@code resumeedit},
 * {@code listparts}, {@code locatepart} and {@code exportpart}.
 *
 * <p>
 * Every subcommand needs operator level 2, checked per subcommand before it runs. Errors are {@link CommandException}s
 * with lang keys, which vanilla translates on the client.
 *
 * <p>
 * Side: server.
 */
public final class LostCitiesCommand extends CommandBase {

    /** The command's name. */
    public static final String NAME = "lostcities";

    private static final List<String> ALIASES = Collections.unmodifiableList(Arrays.asList("lost", "lostcitiesnh"));

    private final Map<String, Subcommand> byName = new TreeMap<>();
    private final List<String> primaryNames = new ArrayList<>();

    /** Creates the command with every subcommand. */
    public LostCitiesCommand() {
        add(new ProfileSubcommand());
        add(new SaveProfileSubcommand());
        add(new TimersSubcommand());
        add(new TestSubcommand());
        add(new ViewSubcommand());
        add(new FindSubcommand());
        add(new DebugSubcommand());
        add(new MapSubcommand());
        add(new LocateSubcommand());
        add(new CreateBuildingSubcommand());
        add(new CreatePartSubcommand());
        add(new EditPartSubcommand(false));
        add(new EditPartSubcommand(true));
        add(new ListPartsSubcommand());
        add(new LocatePartSubcommand());
        add(new ExportPartSubcommand());
    }

    private void add(Subcommand subcommand) {
        primaryNames.add(subcommand.name());
        byName.put(subcommand.name(), subcommand);
        for (String alias : subcommand.aliases()) {
            byName.put(alias, subcommand);
        }
    }

    @Override
    public String getCommandName() {
        return NAME;
    }

    @Override
    public List<String> getCommandAliases() {
        return ALIASES;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return LangKeys.COMMAND_USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return Subcommand.OPERATOR_LEVEL;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(LangKeys.COMMAND_USAGE);
        }
        Subcommand subcommand = byName.get(args[0]);
        if (subcommand == null) {
            throw new CommandException(LangKeys.COMMAND_UNKNOWN, args[0]);
        }
        if (!sender.canCommandSenderUseCommand(subcommand.permissionLevel(), getCommandName())) {
            throw new CommandException(LangKeys.COMMAND_NO_PERMISSION);
        }
        subcommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, primaryNames.toArray(new String[0]));
        }
        Subcommand subcommand = args.length > 1 ? byName.get(args[0]) : null;
        if (subcommand == null) {
            return Collections.emptyList();
        }
        return subcommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
