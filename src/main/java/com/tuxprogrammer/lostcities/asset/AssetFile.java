package com.tuxprogrammer.lostcities.asset;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * One asset file as a source supplies it: its type, its id, where it came from and its text. The id comes from the
 * folder and file name, {@code <namespace>:<path>}, as upstream's data packs give it
 * (docs/new-spec/05-assets/asset-format.md, "Ids").
 *
 * <p>
 * Immutable. Side: both.
 */
public final class AssetFile {

    private final AssetType type;
    private final AssetId id;
    private final String origin;
    private final String text;
    private final String sha256;

    /**
     * Creates a file.
     *
     * @param type   the asset type, from the folder
     * @param id     the id, from the namespace folder and the path
     * @param origin where it came from, for messages: {@code jar:<path>} or {@code config:<path>}
     * @param text   the content
     */
    public AssetFile(AssetType type, AssetId id, String origin, String text) {
        this.type = type;
        this.id = id;
        this.origin = origin;
        this.text = text;
        this.sha256 = sha256(text);
    }

    /**
     * Derives type and id from a path relative to an asset root, {@code <namespace>/<folder>/<path>.json}.
     *
     * @param relative the path with {@code /} separators
     * @return {@code {type, id}}, or null when the path is not an asset path
     */
    public static Object[] locate(String relative) {
        if (!relative.endsWith(".json")) {
            return null;
        }
        String[] parts = relative.split("/", 3);
        if (parts.length != 3) {
            return null;
        }
        AssetType type = AssetType.byFolder(parts[1]);
        if (type == null) {
            return null;
        }
        String path = parts[2].substring(0, parts[2].length() - ".json".length());
        try {
            return new Object[] { type, AssetId.of(parts[0], path) };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Returns the type. */
    public AssetType type() {
        return type;
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns where the file came from. */
    public String origin() {
        return origin;
    }

    /** Returns the content. */
    public String text() {
        return text;
    }

    /** Returns the SHA-256 of the content, lower-case hex. */
    public String sha256() {
        return sha256;
    }

    /**
     * Returns the SHA-256 of a text, lower-case hex.
     *
     * @param text the text, hashed as UTF-8
     * @return the hash
     */
    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(Character.forDigit((b >> 4) & 0xF, 16))
                    .append(Character.forDigit(b & 0xF, 16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
