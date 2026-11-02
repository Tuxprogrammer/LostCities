package com.tuxprogrammer.lostcities.network;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;

/**
 * Length-bounded string fields for packets. Every string a packet reads has a maximum length, so a hostile peer cannot
 * make the receiver allocate without bound (networking-side-safety, item 5).
 *
 * <p>
 * Side: both.
 */
public final class PacketStrings {

    /** The default limit for a name field (a scenario, profile or part name), in characters. */
    public static final int NAME_MAX_CHARS = 64;

    private PacketStrings() {}

    /**
     * Writes a string, refusing one longer than the reader will accept.
     *
     * @param buffer   where to write
     * @param value    the string
     * @param maxChars the limit the reading side uses
     * @throws IOException when the string is longer than {@code maxChars}
     */
    public static void write(PacketBuffer buffer, String value, int maxChars) throws IOException {
        if (value.length() > maxChars) {
            throw new IOException("string of " + value.length() + " characters exceeds the limit of " + maxChars);
        }
        buffer.writeStringToBuffer(value);
    }

    /**
     * Reads a string written by {@link #write}.
     *
     * @param buffer   where to read
     * @param maxChars the most characters accepted
     * @return the string
     * @throws IOException when the encoded string is longer than {@code maxChars} or malformed
     */
    public static String read(PacketBuffer buffer, int maxChars) throws IOException {
        // Vanilla checks both the encoded byte length (at most 4 bytes per character) and the decoded length.
        return buffer.readStringFromBuffer(maxChars);
    }
}
