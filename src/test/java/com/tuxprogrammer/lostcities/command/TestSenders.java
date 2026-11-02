package com.tuxprogrammer.lostcities.command;

import java.lang.reflect.Proxy;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;

/**
 * Command senders for unit tests: a console-like sender with a permission level that records its chat, built as a
 * dynamic proxy so the test names no obfuscated member of {@code ICommandSender}.
 */
final class TestSenders {

    private TestSenders() {}

    /**
     * Returns a sender that is not a player.
     *
     * @param level the highest permission level it has
     * @param chat  where its chat goes
     * @return the sender
     */
    static ICommandSender console(int level, List<IChatComponent> chat) {
        return (ICommandSender) Proxy.newProxyInstance(
            TestSenders.class.getClassLoader(),
            new Class<?>[] { ICommandSender.class },
            (proxy, method, args) -> {
                Class<?> type = method.getReturnType();
                if (type == boolean.class) {
                    return args != null && args.length == 2 && (Integer) args[0] <= level;
                }
                if (type == void.class) {
                    if (args != null && args.length == 1 && args[0] instanceof IChatComponent message) {
                        chat.add(message);
                    }
                    return null;
                }
                if (type == String.class) {
                    return "tester";
                }
                if (type == ChunkCoordinates.class) {
                    return new ChunkCoordinates(0, 64, 0);
                }
                if (type == int.class) {
                    // hashCode
                    return System.identityHashCode(proxy);
                }
                return null;
            });
    }

    /**
     * Runs a subcommand and returns the lang key of the error it threw.
     *
     * @param action the subcommand's run
     * @return the key, or {@code null} when it did not throw
     */
    static String errorKey(Runnable action) {
        try {
            action.run();
            return null;
        } catch (CommandException e) {
            return e.getMessage();
        }
    }
}
