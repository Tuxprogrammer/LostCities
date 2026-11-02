package com.tuxprogrammer.lostcities.editor;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/editor/EditorInfo.java
// Avoids: CORE-35

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * One player's part-editing session: which part, where it stands (its chunk, the Y of its first slice, its transform
 * and offset) and the reverse palette read back from the world when the session started
 * (docs/original-spec/01-core/part-editor.md, "Starting a session"). {@link EditSessions} keeps it for its world only
 * (avoids CORE-35).
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class EditSession {

    private final AssetId part;
    private final int chunkX;
    private final int chunkZ;
    private final PartRecord placement;
    private final Map<BlockRef, Character> reverse;

    /**
     * Creates a session.
     *
     * @param chunkX    the chunk the part stands in
     * @param chunkZ    the chunk the part stands in
     * @param placement the part, its first slice's Y, its transform and offset
     * @param reverse   the reverse palette
     */
    public EditSession(int chunkX, int chunkZ, PartRecord placement, Map<BlockRef, Character> reverse) {
        this.part = placement.part();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.placement = Objects.requireNonNull(placement);
        this.reverse = Collections.unmodifiableMap(new LinkedHashMap<>(reverse));
    }

    /** Returns the part being edited. */
    public AssetId part() {
        return part;
    }

    /** Returns the chunk X the part stands in. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns the chunk Z the part stands in. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns the block Y of the part's first slice. */
    public int bottom() {
        return placement.y();
    }

    /** Returns the part's transform. */
    public Transform transform() {
        return placement.transform();
    }

    /** Returns where the part stands, as a record. */
    public PartRecord placement() {
        return placement;
    }

    /** Returns the reverse palette: the character each block read at the session's start stands for. */
    public Map<BlockRef, Character> reverse() {
        return reverse;
    }
}
