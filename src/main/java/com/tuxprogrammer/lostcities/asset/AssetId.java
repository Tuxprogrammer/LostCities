package com.tuxprogrammer.lostcities.asset;

import java.util.Locale;

/**
 * The full id of an asset, {@code namespace:path}. Every name an asset gives is normalized to this form at load, and
 * every comparison uses it, so a bare name and its full id always match each other (docs/new-spec/05-assets/
 * asset-format.md, "Names"; P05-2, decision log D31; fixes ASSET-33).
 *
 * <p>
 * A bare name means {@code lostcities:<name>}, as upstream's assets are written. Ids compare by value and sort by
 * their text form. Side: both.
 */
public final class AssetId implements Comparable<AssetId> {

    /** The namespace of upstream's bundled assets, and of every bare name. */
    public static final String DEFAULT_NAMESPACE = "lostcities";

    private final String namespace;
    private final String path;

    private AssetId(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    /**
     * Returns the id for a namespace and a path.
     *
     * @param namespace the namespace: lower case letters, digits, {@code _ - .}
     * @param path      the path: the same characters plus {@code /}
     * @return the id
     * @throws IllegalArgumentException when either part is empty or holds another character
     */
    public static AssetId of(String namespace, String path) {
        if (!valid(namespace, false) || !valid(path, true)) {
            throw new IllegalArgumentException("not a namespace:path asset id: '" + namespace + ":" + path + "'");
        }
        return new AssetId(namespace, path);
    }

    /**
     * Normalizes a name to a full id: a name with a colon is parsed as {@code namespace:path}; a bare name means
     * {@code lostcities:<name>}.
     *
     * @param name the name as an asset writes it
     * @return the id
     * @throws IllegalArgumentException when the name is null, empty or malformed
     */
    public static AssetId parse(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("empty asset name");
        }
        int colon = name.indexOf(':');
        if (colon < 0) {
            return of(DEFAULT_NAMESPACE, name);
        }
        if (name.indexOf(':', colon + 1) >= 0) {
            throw new IllegalArgumentException("asset name '" + name + "' has more than one ':'");
        }
        return of(name.substring(0, colon), name.substring(colon + 1));
    }

    /** Returns the namespace. */
    public String namespace() {
        return namespace;
    }

    /** Returns the path. */
    public String path() {
        return path;
    }

    /** Returns the text form, always {@code namespace:path}. */
    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssetId)) {
            return false;
        }
        AssetId that = (AssetId) other;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public int compareTo(AssetId other) {
        return toString().compareTo(other.toString());
    }

    private static boolean valid(String part, boolean allowSlash) {
        if (part == null || part.isEmpty()) {
            return false;
        }
        String lower = part.toLowerCase(Locale.ROOT);
        if (!lower.equals(part)) {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '_'
                || c == '-'
                || c == '.'
                || (allowSlash && c == '/');
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
