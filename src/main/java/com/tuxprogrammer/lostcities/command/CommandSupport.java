package com.tuxprogrammer.lostcities.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

/**
 * Checks and helpers the layout and part-editor subcommands share (docs/new-spec/01-core/commands.md, "Shape" and
 * "Edit-mode gate"): the sender's world must have real cities, some subcommands need a player, the editor's need an
 * {@code editMode} world, and names complete from the asset registries. Every failure is a {@link CommandException}
 * with a lang key.
 *
 * <p>
 * Side: server.
 */
final class CommandSupport {

    private CommandSupport() {}

    /**
     * Returns the overlay of the sender's world, which must generate real cities (upstream's "This dimension doesn't
     * support Lost Cities").
     *
     * @param sender who typed the command
     * @return the overlay, whose {@link OverlayWorld#cities()} is not {@code null}
     * @throws CommandException when the world has no real cities
     */
    static OverlayWorld cities(ICommandSender sender) {
        OverlayWorld overlay = Overlays.INSTANCE.get(sender.getEntityWorld());
        if (overlay == null || overlay.cities() == null) {
            throw new CommandException(LangKeys.COMMAND_NO_CITIES, sender.getEntityWorld().provider.dimensionId);
        }
        return overlay;
    }

    /**
     * Returns the overlay of a part-editing world (upstream's edit-mode gate).
     *
     * @param sender who typed the command
     * @return the overlay
     * @throws CommandException when the world has no cities or is not an {@code editMode} world
     */
    static OverlayWorld editWorld(ICommandSender sender) {
        OverlayWorld overlay = cities(sender);
        if (!overlay.editMode()) {
            throw new CommandException(LangKeys.EDITOR_NOT_EDIT_MODE);
        }
        return overlay;
    }

    /**
     * Returns the sender as a player; the console is told the subcommand needs one, instead of an exception trace.
     *
     * @param sender     who typed the command
     * @param subcommand the subcommand's name, for the message
     * @return the player
     * @throws CommandException when the sender is not a player
     */
    static EntityPlayerMP player(ICommandSender sender, String subcommand) {
        if (!(sender instanceof EntityPlayerMP player)) {
            throw new CommandException(LangKeys.COMMAND_PLAYER_ONLY, subcommand);
        }
        return player;
    }

    /**
     * Parses a block coordinate with vanilla's relative {@code ~} syntax ({@code CommandBase.clamp_coord}, named
     * {@code func_110666_a} in 1.7.10's mappings): {@code ~} and {@code ~N} are relative to the base, a number is
     * absolute.
     *
     * @param sender who typed the command
     * @param base   the sender's coordinate on that axis
     * @param arg    the argument
     * @return the block coordinate
     * @throws CommandException for a malformed number, with vanilla's lang key
     */
    static int coordinate(ICommandSender sender, double base, String arg) {
        return MathHelper.floor_double(CommandBase.func_110666_a(sender, base, arg));
    }

    /**
     * Returns completions of an asset name: each id in its short form for {@code lostcities} assets and in full for
     * others, matched against what was typed.
     *
     * @param args the arguments, the last one partial
     * @param ids  the registry's ids
     * @return the matches
     */
    @SuppressWarnings("unchecked")
    static List<String> completeIds(String[] args, Collection<AssetId> ids) {
        List<String> names = new ArrayList<>();
        for (AssetId id : ids) {
            names.add(shortName(id));
        }
        return CommandBase.getListOfStringsMatchingLastWord(args, names.toArray(new String[0]));
    }

    /**
     * Returns an id's short form: the path alone in the {@code lostcities} namespace, {@code namespace:path}
     * otherwise.
     *
     * @param id the id
     * @return the short form
     */
    static String shortName(AssetId id) {
        return AssetId.DEFAULT_NAMESPACE.equals(id.namespace()) ? id.path() : id.toString();
    }
}
