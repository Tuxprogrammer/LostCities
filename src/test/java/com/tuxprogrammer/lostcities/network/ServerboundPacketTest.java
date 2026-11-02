package com.tuxprogrammer.lostcities.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.network.validation.FakeSender;
import com.tuxprogrammer.lostcities.network.validation.PacketOutcome;
import com.tuxprogrammer.lostcities.network.validation.RecordingSink;
import com.tuxprogrammer.lostcities.network.validation.Rejection;
import com.tuxprogrammer.lostcities.network.validation.Rules;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * The server-bound base class rejects out-of-range, wrong-dimension and unauthorized requests before the handler
 * runs, drops malformed packets, and contains handler failures (docs/ROADMAP.md, M01 exit gate).
 */
public class ServerboundPacketTest {

    /** The number of palette slots the test packet's index must address. */
    static final int SLOTS = 4;

    /**
     * A world-tool packet as a later milestone would write one: operator-only, naming a dimension, a block and a
     * slot index.
     */
    public static final class EditSlotPacket extends ServerboundPacket {

        int dimension;
        int x;
        int y;
        int z;
        int slot;

        public EditSlotPacket() {}

        EditSlotPacket(int dimension, int x, int y, int z, int slot) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.slot = slot;
        }

        @Override
        protected int requiredPermissionLevel() {
            return 2;
        }

        @Override
        public void encode(PacketBuffer buf) {
            buf.writeInt(dimension);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeByte(slot);
        }

        @Override
        public void decode(PacketBuffer buf) {
            dimension = buf.readInt();
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
            slot = buf.readByte();
        }

        @Override
        protected void validate(Rules rules) {
            rules.sameDimension(dimension)
                .inRange("y", y, 0, 255)
                .withinReachOfBlock(x, y, z)
                .indexInRange("slot", slot, SLOTS);
        }

        @Override
        protected void handle(EntityPlayerMP player) {
            throw new AssertionError("tests drive dispatch() with their own handler");
        }
    }

    @Test
    public void validRequestFromAnOperatorIsHandled() {
        Run run = dispatch(new EditSlotPacket(0, 2, 64, 2, 3), FakeSender.operator());
        assertEquals(PacketOutcome.Kind.ACCEPTED, run.outcome.kind());
        assertTrue(run.handled.get());
        assertTrue(run.sink.rejected.isEmpty());
    }

    @Test
    public void outOfRangeIndexIsRejected() {
        assertRejected(Rejection.OUT_OF_RANGE, dispatch(new EditSlotPacket(0, 2, 64, 2, SLOTS), FakeSender.operator()));
        assertRejected(Rejection.OUT_OF_RANGE, dispatch(new EditSlotPacket(0, 2, 64, 2, -1), FakeSender.operator()));
        assertRejected(Rejection.OUT_OF_RANGE, dispatch(new EditSlotPacket(0, 2, 300, 2, 0), FakeSender.operator()));
    }

    @Test
    public void wrongDimensionIsRejected() {
        assertRejected(Rejection.WRONG_DIMENSION, dispatch(new EditSlotPacket(-1, 2, 64, 2, 0), FakeSender.operator()));
    }

    @Test
    public void unauthorizedSenderIsRejectedBeforeTheFieldsAreLookedAt() {
        // Every field is also bad; the permission rule runs first, so the reason is UNAUTHORIZED.
        assertRejected(Rejection.UNAUTHORIZED, dispatch(new EditSlotPacket(7, 900, 900, 900, 99), FakeSender.player()));
    }

    @Test
    public void outOfReachTargetIsRejected() {
        assertRejected(Rejection.OUT_OF_REACH, dispatch(new EditSlotPacket(0, 100, 64, 100, 0), FakeSender.operator()));
    }

    @Test
    public void deadSenderIsRejected() {
        FakeSender sender = FakeSender.operator();
        sender.alive = false;
        assertRejected(Rejection.SENDER_INACTIVE, dispatch(new EditSlotPacket(0, 2, 64, 2, 0), sender));
    }

    @Test
    public void truncatedBytesAreDroppedAsMalformed() {
        EditSlotPacket packet = new EditSlotPacket();
        ByteBuf truncated = Unpooled.buffer();
        truncated.writeInt(0);
        packet.fromBytes(truncated);
        assertRejected(Rejection.MALFORMED, dispatch(packet, FakeSender.operator()));
    }

    @Test
    public void roundTripThroughBytesKeepsTheFields() throws IOException {
        ByteBuf bytes = Unpooled.buffer();
        new EditSlotPacket(0, 2, 64, 2, 3).encode(new PacketBuffer(bytes));
        EditSlotPacket decoded = new EditSlotPacket();
        decoded.fromBytes(bytes);
        assertEquals(PacketOutcome.Kind.ACCEPTED, dispatch(decoded, FakeSender.operator()).outcome.kind());
        assertEquals(3, decoded.slot);
    }

    @Test
    public void handlerExceptionIsContained() {
        RecordingSink sink = new RecordingSink();
        PacketOutcome outcome = new EditSlotPacket(0, 2, 64, 2, 0)
            .dispatch(FakeSender.operator(), () -> { throw new IllegalStateException("handler bug"); }, sink);
        assertEquals(PacketOutcome.Kind.FAILED, outcome.kind());
        assertEquals(1, sink.failures.size());
    }

    private static void assertRejected(Rejection reason, Run run) {
        assertEquals(PacketOutcome.Kind.REJECTED, run.outcome.kind(), run.outcome.toString());
        assertEquals(reason, run.outcome.reason(), run.outcome.toString());
        assertFalse(run.handled.get(), "the handler must not run for a rejected packet");
        assertEquals(1, run.sink.rejected.size());
    }

    private static Run dispatch(ServerboundPacket packet, FakeSender sender) {
        Run run = new Run();
        run.outcome = packet.dispatch(sender, () -> run.handled.set(true), run.sink);
        return run;
    }

    /** One dispatch's observations. */
    private static final class Run {

        final AtomicBoolean handled = new AtomicBoolean();
        final RecordingSink sink = new RecordingSink();
        PacketOutcome outcome;
    }

    @Test
    public void noRealPacketIsRegisteredYet() {
        // docs/new-spec/01-core/index.md: no packet is defined until a feature needs one.
        assertTrue(
            LostCitiesNetwork.serverboundPackets()
                .isEmpty());
        assertTrue(
            LostCitiesNetwork.clientboundPackets()
                .isEmpty());
    }
}
