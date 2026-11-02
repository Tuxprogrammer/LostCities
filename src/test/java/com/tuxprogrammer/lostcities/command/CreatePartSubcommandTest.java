package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.LangKeys;

/**
 * The part editor's player-only subcommands: the console is told so with a lang key, and a non-operator is refused
 * before anything runs.
 */
public class CreatePartSubcommandTest {

    private static final Subcommand[] PLAYER_ONLY = { new CreatePartSubcommand(), new EditPartSubcommand(false),
        new EditPartSubcommand(true), new ListPartsSubcommand(), new LocatePartSubcommand(),
        new ExportPartSubcommand() };

    @Test
    public void theConsoleIsToldTheSubcommandNeedsAPlayer() {
        for (Subcommand subcommand : PLAYER_ONLY) {
            List<IChatComponent> chat = new ArrayList<>();
            String error = TestSenders
                .errorKey(() -> subcommand.execute(TestSenders.console(4, chat), new String[] { "a", "1", "2", "3" }));
            assertEquals(LangKeys.COMMAND_PLAYER_ONLY, error, subcommand.name());
        }
    }

    @Test
    public void aNonOperatorIsRefused() {
        LostCitiesCommand command = new LostCitiesCommand();
        for (Subcommand subcommand : PLAYER_ONLY) {
            assertEquals(Subcommand.OPERATOR_LEVEL, subcommand.permissionLevel());
            List<IChatComponent> chat = new ArrayList<>();
            String error = TestSenders.errorKey(
                () -> command.processCommand(TestSenders.console(1, chat), new String[] { subcommand.name() }));
            assertEquals(LangKeys.COMMAND_NO_PERMISSION, error, subcommand.name());
            assertTrue(chat.isEmpty());
        }
    }

    @Test
    public void everySubcommandOfTheSpecIsRegistered() {
        LostCitiesCommand command = new LostCitiesCommand();
        for (String name : new String[] { "profile", "saveprofile", "timers", "timings", "test", "scenario", "view",
            "find", "debug", "map", "locate", "createbuilding", "createpart", "editpart", "resumeedit", "listparts",
            "locatepart", "exportpart" }) {
            List<IChatComponent> chat = new ArrayList<>();
            String error = TestSenders
                .errorKey(() -> command.processCommand(TestSenders.console(0, chat), new String[] { name }));
            assertEquals(LangKeys.COMMAND_NO_PERMISSION, error, name + " is known (an unknown one says so instead)");
        }
    }
}
