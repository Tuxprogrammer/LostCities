package com.tuxprogrammer.lostcities.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.command.Subcommand;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.LangKeys;

/**
 * {@code /lostcities test <scenario|prefix|all|list>}, alias {@code scenario}: runs scenarios on the server and reports
 * PASS or FAIL to the sender's chat and to the log (docs/new-spec/01-core/commands.md, CONTRIBUTING § Testing).
 * Operator level 2, and only where the test hooks are on: the development environment, or a release jar started with
 * {@code -Dlostcitiesnh.testHooks=true}.
 *
 * <p>
 * Side: server.
 */
public final class TestSubcommand implements Subcommand {

    /** The selector that lists the scenarios instead of running them. */
    static final String LIST = "list";

    @Override
    public String name() {
        return "test";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("scenario");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (!DevEnvironment.testHooksEnabled()) {
            throw new CommandException(LangKeys.TEST_DISABLED);
        }
        if (args.length != 1) {
            throw new WrongUsageException(LangKeys.TEST_USAGE);
        }
        String selector = args[0];
        if (LIST.equals(selector)) {
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.TEST_LIST, String.join(", ", names())));
            return;
        }
        List<Scenario> scenarios = ScenarioRegistry.INSTANCE.select(selector);
        if (scenarios.isEmpty()) {
            LostCities.LOG.info(ScenarioReport.errorLine("no scenario matches '" + selector + "'"));
            throw new CommandException(LangKeys.TEST_NO_MATCH, selector);
        }
        if (!ScenarioRunner.INSTANCE.start(scenarios, sender, null)) {
            LostCities.LOG.info(ScenarioReport.errorLine("a scenario run is already in progress"));
            throw new CommandException(LangKeys.TEST_BUSY);
        }
        LostCities.LOG
            .info("{} started {} scenario(s) for '{}'", sender.getCommandSenderName(), scenarios.size(), selector);
        sender.addChatMessage(new ChatComponentTranslation(LangKeys.TEST_STARTED, scenarios.size()));
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<>();
        options.add(ScenarioRegistry.ALL);
        options.add(LIST);
        options.addAll(names());
        @SuppressWarnings("unchecked")
        List<String> matches = CommandBase.getListOfStringsMatchingLastWord(args, options.toArray(new String[0]));
        return matches;
    }

    private static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Scenario scenario : ScenarioRegistry.INSTANCE.all()) {
            names.add(scenario.name());
        }
        return names;
    }
}
