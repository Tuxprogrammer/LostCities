package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.LangKeys;

/** {@link ListPartsSubcommand}: a player's subcommand at operator level 2. */
public class ListPartsSubcommandTest {

    @Test
    public void theConsoleIsToldItNeedsAPlayer() {
        assertEquals(
            LangKeys.COMMAND_PLAYER_ONLY,
            TestSenders.errorKey(
                () -> new ListPartsSubcommand().execute(TestSenders.console(4, new ArrayList<>()), new String[0])));
        assertEquals(Subcommand.OPERATOR_LEVEL, new ListPartsSubcommand().permissionLevel());
    }
}
