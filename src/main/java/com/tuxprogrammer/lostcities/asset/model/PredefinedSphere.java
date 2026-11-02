package com.tuxprogrammer.lostcities.asset.model;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A predefined city sphere (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Predefined sphere JSON").
 * Sphere landscapes are not ported before beta (D12), so the asset is parsed, validated and kept, and a load warning
 * says it has no effect. Upstream bundles none. Immutable. Side: both.
 */
public final class PredefinedSphere {

    private final AssetId id;
    private final int chunkX;
    private final int chunkZ;
    private final int centerX;
    private final int centerZ;
    private final int radius;

    private PredefinedSphere(AssetId id, int chunkX, int chunkZ, int centerX, int centerZ, int radius) {
        this.id = id;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    /**
     * Reads a predefined sphere file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the sphere, or null after reporting an error
     */
    public static PredefinedSphere parse(JsonFields root, ParseContext context) {
        boolean ok = PredefinedCity.overworld(root);
        Integer chunkX = root.integer("chunkx", true);
        Integer chunkZ = root.integer("chunkz", true);
        Integer centerX = root.integer("centerx", true);
        Integer centerZ = root.integer("centerz", true);
        Integer radius = root.integer("radius", true);
        if (!ok || chunkX == null || chunkZ == null || centerX == null || centerZ == null || radius == null) {
            return null;
        }
        root.warning("", null, "city spheres are not ported before beta (D12); this asset has no effect");
        return new PredefinedSphere(context.id(), chunkX, chunkZ, centerX, centerZ, radius);
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the chunk that owns the sphere, X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns the chunk that owns the sphere, Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns the sphere center X in blocks. */
    public int centerX() {
        return centerX;
    }

    /** Returns the sphere center Z in blocks. */
    public int centerZ() {
        return centerZ;
    }

    /** Returns the radius in blocks. */
    public int radius() {
        return radius;
    }
}
