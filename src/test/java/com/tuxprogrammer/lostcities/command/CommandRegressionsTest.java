package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.LangKeys;

/**
 * Regression tests for upstream's command bugs that no single subcommand's test covers
 * (docs/new-spec/01-core/commands.md).
 */
public class CommandRegressionsTest {

    @Test
    public void doesNotReproduceCORE27_saveprofileNeedsOperatorLevel2() {
        assertEquals(Subcommand.OPERATOR_LEVEL, new SaveProfileSubcommand().permissionLevel());
        List<IChatComponent> chat = new ArrayList<>();
        String error = TestSenders.errorKey(
            () -> new LostCitiesCommand()
                .processCommand(TestSenders.console(1, chat), new String[] { "saveprofile", "default", "mine" }));
        assertEquals(LangKeys.COMMAND_NO_PERMISSION, error, "upstream let every player write files");
        assertTrue(chat.isEmpty(), "nothing is sent or written before the check");
    }

    @Test
    public void doesNotReproduceCORE27_saveprofileWritesOnlyPlainNamesAndReportsRefusalsAsErrors() {
        SaveProfileSubcommand saveprofile = new SaveProfileSubcommand();
        for (String name : new String[] { "../ops", "server.properties", "Mine", "a/b", "" }) {
            List<IChatComponent> chat = new ArrayList<>();
            String error = TestSenders
                .errorKey(() -> saveprofile.execute(TestSenders.console(4, chat), new String[] { "default", name }));
            assertEquals(LangKeys.SAVEPROFILE_NAME, error, "'" + name + "' is refused as an error");
            assertTrue(chat.isEmpty(), "no success message for '" + name + "'");
        }
        List<IChatComponent> chat = new ArrayList<>();
        String error = TestSenders
            .errorKey(() -> saveprofile.execute(TestSenders.console(4, chat), new String[] { "default", "atlantis" }));
        assertEquals(LangKeys.SAVEPROFILE_BUILT_IN, error, "a built-in's name is refused");
    }

    @Test
    public void doesNotReproduceCORE33_noChunkResetCommandIsRegistered() {
        List<IChatComponent> chat = new ArrayList<>();
        String error = TestSenders.errorKey(
            () -> new LostCitiesCommand().processCommand(TestSenders.console(4, chat), new String[] { "resetchunks" }));
        assertEquals(LangKeys.COMMAND_UNKNOWN, error);
        @SuppressWarnings("unchecked")
        List<String> names = new LostCitiesCommand()
            .addTabCompletionOptions(TestSenders.console(4, new ArrayList<IChatComponent>()), new String[] { "" });
        assertFalse(names.isEmpty());
        for (String name : names) {
            assertFalse(name.contains("reset"), "no subcommand regenerates chunks: " + names);
        }
    }
}
