package com.tuxprogrammer.lostcities.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.tuxprogrammer.lostcities.network.validation.PacketOutcome;
import com.tuxprogrammer.lostcities.network.validation.PacketPipeline;
import com.tuxprogrammer.lostcities.network.validation.Rejection;
import com.tuxprogrammer.lostcities.network.validation.RejectionSink;
import com.tuxprogrammer.lostcities.network.validation.Rules;
import com.tuxprogrammer.lostcities.network.validation.Sender;
import com.tuxprogrammer.lostcities.network.validation.Violation;

import io.netty.buffer.ByteBuf;

/**
 * The base of every client-to-server packet. For every subclass at once it guarantees:
 * <ul>
 * <li>the handler runs on the server thread, never on Netty's: GTNHLib's {@code IPacket.onMessage} reschedules before
 * it calls {@link #executeServer};</li>
 * <li>validation runs first, in this order: the sender is alive and connected; the sender holds
 * {@link #requiredPermissionLevel()}; then the packet's own {@link #validate} rules (dimension, reach, every index and
 * enum against its bounds, target type). The first failure drops the packet and is logged at debug level, rate-limited
 * (CONTRIBUTING § Code standards);</li>
 * <li>nothing is thrown into Netty or the server tick: a decoding error marks the packet malformed and drops it, and an
 * exception in validation or in {@link #handle} is contained and logged.</li>
 * </ul>
 *
 * <p>
 * A subclass needs a public no-argument constructor, since the channel creates instances to decode into. Register it
 * in {@link LostCitiesNetwork#serverboundPackets()}. Every M01 test packet lives in the tests; no real packet exists
 * until a feature needs one (docs/new-spec/01-core/index.md, "Network").
 *
 * <p>
 * Source: GTNHLib 0.11.54,
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/network/base/IPacket.java#L26-L51
 * ({@code onMessage} runs {@code executeServer} on the server thread).
 *
 * <p>
 * Side: server (loaded on both sides; only the server runs it).
 */
public abstract class ServerboundPacket implements IPacket {

    /** Why decoding failed, or {@code null}; written on the Netty thread, read on the server thread. */
    private volatile String decodeError;

    /**
     * Returns the permission level the sender needs: 0 for any player, 2 for operator-only world tools. Abstract so
     * that no packet can forget to decide.
     */
    protected abstract int requiredPermissionLevel();

    /**
     * Declares this packet's rules for its own fields. Called on the server thread after decoding, after the sender
     * and permission rules. Declare cheap range checks before target lookups.
     *
     * @param rules where to declare; {@link Rules#sender()} is the sending player
     */
    protected abstract void validate(Rules rules);

    /**
     * Does the packet's work, on the server thread, only after every rule held. Validated targets may be cast without
     * further checks.
     *
     * @param player the sending player
     */
    protected abstract void handle(EntityPlayerMP player);

    /** Decodes without ever throwing into Netty; a malformed packet is remembered and dropped when it is executed. */
    @Override
    public final void fromBytes(ByteBuf buf) {
        try {
            decode(new PacketBuffer(buf));
        } catch (IOException | RuntimeException e) {
            decodeError = e.toString();
        }
    }

    /** Entry point GTNHLib calls on the server thread. */
    @Override
    public final IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP player = handler.playerEntity;
        if (player != null) {
            dispatch(new PlayerSender(player), () -> handle(player), LostCitiesNetwork.rejections());
        }
        return null;
    }

    /**
     * The whole server-side pipeline without the Minecraft glue, so unit tests can drive it.
     *
     * @param sender  the sender
     * @param handler what {@link #handle} would do
     * @param sink    where drops are reported
     * @return what happened to the packet
     */
    final PacketOutcome dispatch(Sender sender, Runnable handler, RejectionSink sink) {
        String name = getClass().getSimpleName();
        if (decodeError != null) {
            Violation malformed = new Violation(Rejection.MALFORMED, decodeError);
            sink.rejected(name, sender.name(), malformed);
            return PacketOutcome.rejected(malformed);
        }
        return PacketPipeline.run(name, sender.name(), Rules.forSender(sender), rules -> {
            rules.senderActive();
            rules.permissionLevel(requiredPermissionLevel());
            validate(rules);
        }, handler, sink);
    }
}
