package com.tuxprogrammer.lostcities.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.network.validation.PacketOutcome;
import com.tuxprogrammer.lostcities.network.validation.RecordingSink;
import com.tuxprogrammer.lostcities.network.validation.Rejection;
import com.tuxprogrammer.lostcities.network.validation.Rules;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** The client-bound base class validates what the server sent before applying it, and drops malformed bytes. */
public class ClientboundPacketTest {

    /** The kinds of notice the test packet carries. */
    enum Notice {
        INFO,
        WARNING
    }

    /** A client-bound packet carrying an enum ordinal, as a later milestone might send one. */
    public static final class NoticePacket extends ClientboundPacket {

        int notice;

        public NoticePacket() {}

        NoticePacket(int notice) {
            this.notice = notice;
        }

        @Override
        public void encode(PacketBuffer buf) {
            buf.writeByte(notice);
        }

        @Override
        public void decode(PacketBuffer buf) {
            notice = buf.readByte();
        }

        @Override
        protected void validate(Rules rules) {
            rules.ordinalInRange("notice", notice, Notice.class);
        }

        @Override
        protected void handleClient() {
            throw new AssertionError("tests drive dispatch() with their own handler");
        }
    }

    @Test
    public void validOrdinalIsApplied() {
        AtomicBoolean handled = new AtomicBoolean();
        PacketOutcome outcome = new NoticePacket(1).dispatch(() -> handled.set(true), new RecordingSink());
        assertEquals(PacketOutcome.Kind.ACCEPTED, outcome.kind());
        assertTrue(handled.get());
    }

    @Test
    public void outOfRangeOrdinalIsRejected() {
        AtomicBoolean handled = new AtomicBoolean();
        PacketOutcome outcome = new NoticePacket(5).dispatch(() -> handled.set(true), new RecordingSink());
        assertEquals(Rejection.OUT_OF_RANGE, outcome.reason());
        assertFalse(handled.get());
    }

    @Test
    public void emptyBytesAreDroppedAsMalformed() {
        NoticePacket packet = new NoticePacket();
        ByteBuf empty = Unpooled.buffer();
        packet.fromBytes(empty);
        AtomicBoolean handled = new AtomicBoolean();
        PacketOutcome outcome = packet.dispatch(() -> handled.set(true), new RecordingSink());
        assertEquals(Rejection.MALFORMED, outcome.reason());
        assertFalse(handled.get());
    }
}
