package com.tuxprogrammer.lostcities.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * {@code /lostcities timers [reset]}: prints the overlay's generation timers of the sender's dimension (p50, p95, p99,
 * p99.9 and max per series and chunk class), the budget verdicts B1 to B5 and the count of failed chunks, to the
 * sender and the log; {@code reset} clears them (docs/design/0005-rwg-overlay.md §8, P03-12). Operator level 2. It
 * replaces upstream's {@code /lostcities stats}.
 *
 * <p>
 * Side: server.
 */
public final class TimersSubcommand implements Subcommand {

    static final String RESET = "reset";

    @Override
    public String name() {
        return "timers";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 1 || (args.length == 1 && !RESET.equals(args[0]))) {
            throw new WrongUsageException(LangKeys.TIMERS_USAGE);
        }
        int dimension = sender.getEntityWorld().provider.dimensionId;
        OverlayWorld overlay = Overlays.INSTANCE.get(sender.getEntityWorld());
        if (overlay == null) {
            throw new CommandException(LangKeys.TIMERS_NONE, dimension);
        }
        GenerationTimers timers = overlay.timers();
        if (args.length == 1) {
            timers.reset();
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.TIMERS_RESET, dimension));
            return;
        }
        for (String line : timers.report()) {
            LostCities.LOG.info("[timers dim {}] {}", dimension, line);
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.TIMERS_LINE, line));
        }
        for (GenerationTimers.Verdict verdict : timers.budgets(overlay.cacheBytes())) {
            LostCities.LOG.info("[timers dim {}] {}", dimension, verdict);
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.TIMERS_LINE, verdict.toString()));
        }
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<String> matches = CommandBase.getListOfStringsMatchingLastWord(args, RESET);
        return matches;
    }
}
