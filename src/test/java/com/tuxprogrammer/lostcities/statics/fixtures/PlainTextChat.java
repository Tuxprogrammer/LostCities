package com.tuxprogrammer.lostcities.statics.fixtures;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/** Fixture for {@code LangKeysTest}: sends hard-coded English instead of a lang key. Never run. */
public class PlainTextChat {

    /** Sends untranslated text. */
    public void tell(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText("Hello"));
    }
}
