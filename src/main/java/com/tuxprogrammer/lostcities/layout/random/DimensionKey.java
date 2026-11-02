package com.tuxprogrammer.lostcities.layout.random;

/**
 * The dimension identity the layout hashes. 1.7.10 has integer dimension IDs, upstream 1.20 namespaced
 * {@code ResourceKey}s; the layout hashes upstream 1.20's ID for the same dimension, so that its pure planners give
 * upstream's output for the same seed: {@code minecraft:overworld} for dimension 0 and {@code lostcitiesnh:dim<id>} for
 * any other dimension a profile is mapped to (new-spec 04-city-layout, "Determinism and the 1.7.10 world", P04-1,
 * decision log D31).
 *
 * <p>
 * Upstream hashes the ID two ways, and both are here:
 * <ul>
 * <li>{@link #stableHash()}: the street planner's {@code dimensionSalt}, the bridge planner's span hash and the
 * intercity highway planner;</li>
 * <li>{@link #resourceLocationHash()}: {@code ResourceLocation.hashCode()}, which the per-chunk generation context and
 * the terrain-transition seed fold into their seeds (03-terrain, owned by the terrain code).</li>
 * </ul>
 *
 * <p>
 * Side: both.
 */
public final class DimensionKey {

    /** The overworld, dimension 0. */
    public static final DimensionKey OVERWORLD = new DimensionKey("minecraft", "overworld");

    /** Java's string-hash multiplier, which 1.20's {@code ResourceLocation.hashCode()} uses to combine its parts. */
    private static final int HASH_COMBINER = 31;

    private final String namespace;
    private final String path;
    private final long stableHash;

    private DimensionKey(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
        this.stableHash = StableHash.stableStringHash(namespace + ":" + path);
    }

    /**
     * The key for a 1.7.10 dimension ID.
     *
     * @param dimensionId the integer dimension ID
     * @return {@link #OVERWORLD} for 0, {@code lostcitiesnh:dim<id>} otherwise
     */
    public static DimensionKey forDimension(int dimensionId) {
        return dimensionId == 0 ? OVERWORLD : new DimensionKey("lostcitiesnh", "dim" + dimensionId);
    }

    /**
     * Parses a {@code namespace:path} ID, as predefined content's {@code dimension} field names it.
     *
     * @param id the ID
     * @return the key
     * @throws IllegalArgumentException when the ID has no single {@code :} separating two non-empty parts
     */
    public static DimensionKey parse(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1 || id.indexOf(':', colon + 1) >= 0) {
            throw new IllegalArgumentException("not a namespace:path dimension ID: '" + id + "'");
        }
        DimensionKey key = new DimensionKey(id.substring(0, colon), id.substring(colon + 1));
        return key.equals(OVERWORLD) ? OVERWORLD : key;
    }

    /** Returns the ID as upstream writes it, {@code namespace:path}. */
    public String id() {
        return namespace + ":" + path;
    }

    /** Returns {@link StableHash#stableStringHash(String)} of {@link #id()}. */
    public long stableHash() {
        return stableHash;
    }

    /**
     * Returns what 1.20's {@code new ResourceLocation(namespace, path).hashCode()} returns:
     * {@code 31 * namespace.hashCode() + path.hashCode()}. The recorded values in
     * {@code src/test/resources/parity/resource_location_hash.txt} confirm the formula.
     */
    public int resourceLocationHash() {
        return HASH_COMBINER * namespace.hashCode() + path.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DimensionKey && ((DimensionKey) other).id()
            .equals(id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public String toString() {
        return id();
    }
}
