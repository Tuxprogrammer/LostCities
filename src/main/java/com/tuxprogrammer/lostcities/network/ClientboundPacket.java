package com.tuxprogrammer.lostcities.network;

import java.io.IOException;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.tuxprogrammer.lostcities.network.validation.PacketOutcome;
import com.tuxprogrammer.lostcities.network.validation.PacketPipeline;
import com.tuxprogrammer.lostcities.network.validation.Rejection;
import com.tuxprogrammer.lostcities.network.validation.RejectionSink;
import com.tuxprogrammer.lostcities.network.validation.Rules;
import com.tuxprogrammer.lostcities.network.validation.Violation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * The base of every server-to-client packet. Clients decode defensively too: an unchecked enum index from the network
 * crashes a client as surely as a server (networking-side-safety). For every subclass it guarantees:
 * <ul>
 * <li>decoding never throws into Netty; a malformed packet is dropped;</li>
 * <li>{@link #validate} rules (indices, enums, finite numbers) run before {@link #handleClient}, on the client
 * thread;</li>
 * <li>a client-bound packet that reaches the server, because a hostile client sent its ID, is dropped as
 * {@link Rejection#WRONG_SIDE}.</li>
 * </ul>
 *
 * <p>
 * {@link #handleClient} must reach client code only through the {@code client} package, from a method marked
 * {@code @SideOnly(Side.CLIENT)}, so a dedicated server never loads a client class ({@code ClientIsolationTest}).
 * No real client-bound packet exists until a feature needs one (docs/new-spec/01-core/index.md, "Network").
 *
 * <p>
 * Side: client (loaded on both sides; only the client runs it).
 */
public abstract class ClientboundPacket implements IPacket {

    /** Why decoding failed, or {@code null}; written on the Netty thread, read on the client thread. */
    private volatile String decodeError;

    /**
     * Declares this packet's rules for its own fields.
     *
     * @param rules where to declare; it has no sender
     */
    protected abstract void validate(Rules rules);

    /** Applies the packet on the client thread, only after every rule held. */
    protected abstract void handleClient();

    /** Decodes without ever throwing into Netty. */
    @Override
    public final void fromBytes(ByteBuf buf) {
        try {
            decode(new PacketBuffer(buf));
        } catch (IOException | RuntimeException e) {
            decodeError = e.toString();
        }
    }

    /** Entry point GTNHLib calls on the client thread. */
    @Override
    @SideOnly(Side.CLIENT)
    public final IPacket executeClient(NetHandlerPlayClient handler) {
        dispatch(this::handleClient, LostCitiesNetwork.rejections());
        return null;
    }

    /** A client-bound packet on the server: a client sent a server-to-client ID. Dropped. */
    @Override
    public final IPacket executeServer(NetHandlerPlayServer handler) {
        String origin = handler.playerEntity == null ? "unknown" : handler.playerEntity.getCommandSenderName();
        LostCitiesNetwork.rejections()
            .rejected(
                getClass().getSimpleName(),
                origin,
                new Violation(Rejection.WRONG_SIDE, "client-bound packet received by the server"));
        return null;
    }

    /**
     * The client-side pipeline without the Minecraft glue, so unit tests can drive it.
     *
     * @param handler what {@link #handleClient} would do
     * @param sink    where drops are reported
     * @return what happened to the packet
     */
    final PacketOutcome dispatch(Runnable handler, RejectionSink sink) {
        String name = getClass().getSimpleName();
        if (decodeError != null) {
            Violation malformed = new Violation(Rejection.MALFORMED, decodeError);
            sink.rejected(name, "server", malformed);
            return PacketOutcome.rejected(malformed);
        }
        return PacketPipeline.run(name, "server", Rules.withoutSender(), this::validate, handler, sink);
    }
}
