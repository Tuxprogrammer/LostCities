package com.tuxprogrammer.lostcities.asset.block;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * The bundled block data the asset system reads besides the assets: the block-name map, the decorative-metal catalog,
 * the never-place list, the raw variety pools and the measured block colors, all under {@code data/lostcitiesnh/blockmap/} in the jar. They are
 * this mod's own data, not upstream assets, so they are never overridden from the config folder: a pack that needs a
 * different block writes it in its asset overrides as {@code modid:name:meta}.
 *
 * <p>
 * Immutable. Side: both.
 */
public final class BlockData {

    private final BlockMapping mapping;
    private final MetalCatalog metals;
    private final NeverPlaceList neverPlace;
    private final JsonElement pools;
    private final BlockColors colors;

    /**
     * Holds already-read block data, for tests that build their own; no block colors.
     *
     * @param mapping    the block-name map
     * @param metals     the decorative-metal catalog
     * @param neverPlace the never-place list
     * @param pools      the raw pools document, or an empty object
     */
    public BlockData(BlockMapping mapping, MetalCatalog metals, NeverPlaceList neverPlace, JsonElement pools) {
        this(mapping, metals, neverPlace, pools, BlockColors.EMPTY);
    }

    /**
     * Holds already-read block data.
     *
     * @param mapping    the block-name map
     * @param metals     the decorative-metal catalog
     * @param neverPlace the never-place list
     * @param pools      the raw pools document, or an empty object
     * @param colors     the measured block colors
     */
    public BlockData(BlockMapping mapping, MetalCatalog metals, NeverPlaceList neverPlace, JsonElement pools,
        BlockColors colors) {
        this.mapping = mapping;
        this.metals = metals;
        this.neverPlace = neverPlace;
        this.pools = pools;
        this.colors = colors;
    }

    /**
     * Reads the bundled block data from the class path.
     *
     * @param anchor   a class whose loader holds the resources
     * @param problems where problems go, one scope per file
     * @return the data; a missing or broken file is an error and yields an empty table
     */
    public static BlockData bundled(Class<?> anchor, Problems problems) {
        JsonElement mapping = read(anchor, BlockMapping.RESOURCE, problems);
        JsonElement metals = read(anchor, MetalCatalog.RESOURCE, problems);
        JsonElement never = read(anchor, NeverPlaceList.RESOURCE, problems);
        JsonElement pools = read(anchor, VarietyPools.RESOURCE, problems);
        return new BlockData(
            BlockMapping.parse(mapping, problems.in(origin(BlockMapping.RESOURCE))),
            MetalCatalog.parse(metals, problems.in(origin(MetalCatalog.RESOURCE))),
            NeverPlaceList.parse(never, problems.in(origin(NeverPlaceList.RESOURCE))),
            pools,
            BlockColors.bundled(anchor, problems));
    }

    /** Returns the block-name map. */
    public BlockMapping mapping() {
        return mapping;
    }

    /** Returns the decorative-metal catalog. */
    public MetalCatalog metals() {
        return metals;
    }

    /** Returns the never-place list. */
    public NeverPlaceList neverPlace() {
        return neverPlace;
    }

    /** Returns the measured block colors (docs/design/0007-facade-color-feathering.md §1). */
    public BlockColors colors() {
        return colors;
    }

    /** Returns the raw pools document, resolved by the loader against the registry. */
    public JsonElement pools() {
        return pools;
    }

    /** Returns the origin label of a bundled data file, for problem lines. */
    public static String origin(String resource) {
        return "jar:" + resource.substring(1);
    }

    private static JsonElement read(Class<?> anchor, String resource, Problems problems) {
        try (InputStream in = anchor.getResourceAsStream(resource)) {
            if (in == null) {
                problems.in(origin(resource))
                    .error("", null, "missing from the jar");
                return new JsonObject();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonFields.parseStrict(reader);
            }
        } catch (IOException | JsonParseException e) {
            problems.in(origin(resource))
                .error("", null, "not valid JSON: " + e.getMessage());
            return new JsonObject();
        }
    }
}
