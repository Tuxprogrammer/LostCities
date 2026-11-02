package com.tuxprogrammer.lostcities.worldgen.buffer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Turns a chunk buffer into a stable text form and a SHA-256 hash of it. Golden-chunk tests compare these with
 * checked-in values (CONTRIBUTING § Testing), and the worldgen scenarios compare two fresh worlds the same way.
 *
 * <p>
 * The text form, version 1, is:
 *
 * <pre>
 * lostcitiesnh-chunk-buffer v1
 * palette &lt;n&gt;
 * 0 minecraft:air:0
 * 1 &lt;block&gt;
 * ...
 * layer &lt;y&gt; &lt;count&gt;*&lt;index&gt; &lt;count&gt;*&lt;index&gt; ...
 * </pre>
 *
 * Index 0 is always air. Other blocks are numbered in the order they first appear, scanning Y upward, then Z, then X.
 * Each {@code layer} line run-length encodes one horizontal layer in Z-then-X order (256 blocks); layers that hold only
 * air are left out. Every line ends with a line feed and the text is hashed as UTF-8. The result depends on nothing
 * but the buffer's content: no hash-map iteration order and no platform line ending, so it is identical on every JVM
 * and machine.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ChunkBufferDigest {

    /** The first line of the text form; it changes whenever the format does. */
    public static final String HEADER = "lostcitiesnh-chunk-buffer v1";

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private ChunkBufferDigest() {}

    /**
     * Returns the text form of a buffer.
     *
     * @param buffer the buffer to describe
     * @return the version-1 text form described in the class comment
     */
    public static String text(ChunkBuffer buffer) {
        // Pass 1: number the blocks in first-appearance order. LinkedHashMap iterates in insertion order, so the
        // palette order is the scan order and never the hash order.
        Map<BlockRef, Integer> palette = new LinkedHashMap<>();
        palette.put(BlockRef.AIR, 0);
        for (int y = 0; y < ChunkBuffer.HEIGHT; y++) {
            for (int z = 0; z < ChunkBuffer.WIDTH; z++) {
                for (int x = 0; x < ChunkBuffer.WIDTH; x++) {
                    BlockRef block = buffer.get(x, y, z);
                    if (!palette.containsKey(block)) {
                        palette.put(block, palette.size());
                    }
                }
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(HEADER)
            .append('\n');
        out.append("palette ")
            .append(palette.size())
            .append('\n');
        for (Map.Entry<BlockRef, Integer> entry : palette.entrySet()) {
            out.append(entry.getValue())
                .append(' ')
                .append(entry.getKey())
                .append('\n');
        }
        // Pass 2: one run-length line per layer that is not all air.
        for (int y = 0; y < ChunkBuffer.HEIGHT; y++) {
            List<int[]> runs = layerRuns(buffer, y, palette);
            if (runs.size() == 1 && runs.get(0)[1] == 0) {
                continue;
            }
            out.append("layer ")
                .append(y);
            for (int[] run : runs) {
                out.append(' ')
                    .append(run[0])
                    .append('*')
                    .append(run[1]);
            }
            out.append('\n');
        }
        return out.toString();
    }

    /**
     * Returns the SHA-256 hash of a buffer's text form, as 64 lowercase hex digits.
     *
     * @param buffer the buffer to hash
     * @return the hash
     */
    public static String sha256(ChunkBuffer buffer) {
        return sha256(text(buffer));
    }

    /**
     * Returns the SHA-256 hash of a text, as UTF-8 bytes, in 64 lowercase hex digits.
     *
     * @param text the text to hash
     * @return the hash
     */
    public static String sha256(String text) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform must provide SHA-256 (MessageDigest's Javadoc), so this cannot happen.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        char[] hex = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            hex[2 * i] = HEX[(digest[i] >> 4) & 0xF];
            hex[2 * i + 1] = HEX[digest[i] & 0xF];
        }
        return new String(hex);
    }

    /** Returns the runs of one layer as {count, palette index} pairs, in Z-then-X order. */
    private static List<int[]> layerRuns(ChunkBuffer buffer, int y, Map<BlockRef, Integer> palette) {
        List<int[]> runs = new ArrayList<>();
        int current = -1;
        int count = 0;
        for (int z = 0; z < ChunkBuffer.WIDTH; z++) {
            for (int x = 0; x < ChunkBuffer.WIDTH; x++) {
                int index = palette.get(buffer.get(x, y, z));
                if (index == current) {
                    count++;
                } else {
                    if (count > 0) {
                        runs.add(new int[] { count, current });
                    }
                    current = index;
                    count = 1;
                }
            }
        }
        runs.add(new int[] { count, current });
        return runs;
    }
}
