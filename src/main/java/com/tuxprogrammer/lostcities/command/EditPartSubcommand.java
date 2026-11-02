package com.tuxprogrammer.lostcities.command;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandEditPart.java
// Avoids: CORE-34, CORE-35

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;

import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.editor.EditModeData;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.editor.PartRecord;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities editpart} and {@code /lostcities resumeedit}: start editing the recorded part the player stands
 * in (docs/original-spec/01-core/part-editor.md, Commands, items 2 and 3). The chunk's records are walked in the order
 * generation wrote them; the first part whose slices span the player's Y wins. {@code editpart} places the part's
 * blocks again first; {@code resumeedit} keeps the world's blocks, the player's edits, and only reads the reverse
 * palette again. Needs an {@code editMode} world. Operator level 2; players only.
 *
 * <p>
 * Avoids CORE-34 (the records were written on the server thread, {@code EditRecords}) and CORE-35 (the session belongs
 * to the world and ends at logout, {@code EditSessions}).
 *
 * <p>
 * Side: server.
 */
public final class EditPartSubcommand implements Subcommand {

    private final boolean resume;

    /**
     * Creates {@code editpart} or {@code resumeedit}.
     *
     * @param resume {@code true} for {@code resumeedit}, which does not place the part again
     */
    public EditPartSubcommand(boolean resume) {
        this.resume = resume;
    }

    @Override
    public String name() {
        return resume ? "resumeedit" : "editpart";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = CommandSupport.player(sender, name());
        if (args.length != 0) {
            throw new WrongUsageException(LangKeys.COMMAND_USAGE);
        }
        OverlayWorld overlay = CommandSupport.editWorld(sender);
        PartEditor editor = new PartEditor(overlay);
        int chunkX = MathHelper.floor_double(player.posX) >> 4;
        int chunkZ = MathHelper.floor_double(player.posZ) >> 4;
        int y = MathHelper.floor_double(player.posY);
        List<PartRecord> records = EditModeData.get(overlay.world())
            .records()
            .at(overlay.dimension(), chunkX, chunkZ);
        PartRecord record = spanning(records, y, editor);
        if (record == null) {
            throw new CommandException(LangKeys.EDITOR_NOTHING_HERE);
        }
        Part part = editor.part(
            record.part()
                .toString());
        sender.addChatMessage(
            new ChatComponentTranslation(
                resume ? LangKeys.RESUMEEDIT_STARTED : LangKeys.EDITPART_STARTED,
                record.part()
                    .toString(),
                record.y()));
        editor.start(player.getUniqueID(), chunkX, chunkZ, part, record, !resume);
    }

    /**
     * Returns the first record, in writing order, whose part spans a Y.
     *
     * @throws CommandException naming a recorded part that is no longer loaded, as upstream
     */
    private static PartRecord spanning(List<PartRecord> records, int y, PartEditor editor) {
        for (PartRecord record : records) {
            Part part = editor.part(
                record.part()
                    .toString());
            if (part == null) {
                throw new CommandException(
                    LangKeys.EDITOR_UNKNOWN_IN_CHUNK,
                    record.part()
                        .toString());
            }
            if (record.spans(y, part.height())) {
                return record;
            }
        }
        return null;
    }
}
