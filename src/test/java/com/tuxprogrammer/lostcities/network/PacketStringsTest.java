package com.tuxprogrammer.lostcities.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

/** String fields are bounded on both ends: the writer refuses a long string, the reader refuses a long encoding. */
public class PacketStringsTest {

    @Test
    public void roundTripsAStringWithinTheLimit() throws IOException {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        PacketStrings.write(buffer, "harness.trivial", PacketStrings.NAME_MAX_CHARS);
        assertEquals("harness.trivial", PacketStrings.read(buffer, PacketStrings.NAME_MAX_CHARS));
    }

    @Test
    public void writerRefusesAStringOverTheLimit() {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        assertThrows(IOException.class, () -> PacketStrings.write(buffer, "abcdef", 5));
    }

    @Test
    public void readerRefusesAnEncodingOverTheLimit() throws IOException {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        PacketStrings.write(buffer, "abcdefghij", 10);
        assertThrows(IOException.class, () -> PacketStrings.read(buffer, 2));
    }
}
