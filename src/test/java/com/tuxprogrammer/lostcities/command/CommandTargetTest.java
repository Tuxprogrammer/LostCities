package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ChunkCoordinates;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.LangKeys;

/** {@link CommandSupport#parseTarget}: the optional block coordinates of {@code debug} and {@code map}. */
public class CommandTargetTest {

    private static final ChunkCoordinates PLAYER = new ChunkCoordinates(40, 70, -90);

    private static String error(String[] args, ChunkCoordinates at) {
        return TestSenders.errorKey(() -> CommandSupport.parseTarget(args, at, "debug"));
    }

    @Test
    public void noArgumentsUseTheSendersPosition() {
        assertArrayEquals(new int[] { 40, -90 }, CommandSupport.parseTarget(new String[0], PLAYER, "debug"));
    }

    @Test
    public void validCoordinatesWinOverThePosition() {
        assertArrayEquals(
            new int[] { 100, -200 },
            CommandSupport.parseTarget(new String[] { "100", "-200" }, PLAYER, "map"));
        assertArrayEquals(new int[] { 100, -200 }, CommandSupport.parseTarget(new String[] { "100", "-200" }, null, "map"));
        assertArrayEquals(
            new int[] { 30_000_000, -30_000_000 },
            CommandSupport.parseTarget(new String[] { "30000000", "-30000000" }, null, "map"));
    }

    @Test
    public void nonIntegersAreRejected() {
        assertEquals(LangKeys.COMMAND_COORDS_INVALID, error(new String[] { "1.5", "2" }, PLAYER));
        assertEquals(LangKeys.COMMAND_COORDS_INVALID, error(new String[] { "1", "abc" }, PLAYER));
        assertEquals(LangKeys.COMMAND_COORDS_INVALID, error(new String[] { "~", "~" }, PLAYER));
        assertEquals(LangKeys.COMMAND_COORDS_INVALID, error(new String[] { "99999999999999999999", "0" }, PLAYER));
    }

    @Test
    public void coordinatesBeyondTheLimitAreRejected() {
        assertEquals(LangKeys.COMMAND_COORDS_RANGE, error(new String[] { "30000001", "0" }, PLAYER));
        assertEquals(LangKeys.COMMAND_COORDS_RANGE, error(new String[] { "0", "-30000001" }, null));
    }

    @Test
    public void theConsoleWithoutCoordinatesGetsTheUsage() {
        assertEquals(LangKeys.COMMAND_COORDS_USAGE, error(new String[0], null));
    }

    @Test
    public void aSingleOrExtraArgumentGetsTheUsage() {
        assertEquals(LangKeys.COMMAND_COORDS_USAGE, error(new String[] { "5" }, PLAYER));
        assertEquals(LangKeys.COMMAND_COORDS_USAGE, error(new String[] { "1", "2", "3" }, PLAYER));
    }
}
