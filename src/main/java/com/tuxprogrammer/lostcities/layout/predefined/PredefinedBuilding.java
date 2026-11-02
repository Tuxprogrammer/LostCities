package com.tuxprogrammer.lostcities.layout.predefined;

/**
 * A building a predefined city pins to a chunk, relative to the city's center chunk: a single building, or with
 * {@code multi} a multibuilding whose top-left chunk is there.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/predefined-content.md#6-data-formats. Side: both.
 */
public final class PredefinedBuilding {

    private final String building;
    private final int relChunkX;
    private final int relChunkZ;
    private final boolean multi;
    private final boolean preventRuins;

    /**
     * Creates an entry.
     *
     * @param building     the building's name, or the multibuilding's when {@code multi}
     * @param relChunkX    chunk X of its top-left chunk, relative to the city center
     * @param relChunkZ    chunk Z, relative to the city center
     * @param multi        whether {@code building} names a multibuilding
     * @param preventRuins whether the building is never a ruin
     */
    public PredefinedBuilding(String building, int relChunkX, int relChunkZ, boolean multi, boolean preventRuins) {
        this.building = building;
        this.relChunkX = relChunkX;
        this.relChunkZ = relChunkZ;
        this.multi = multi;
        this.preventRuins = preventRuins;
    }

    /** Returns the building's (or multibuilding's) name. */
    public String building() {
        return building;
    }

    /** Returns its top-left chunk X relative to the city center. */
    public int relChunkX() {
        return relChunkX;
    }

    /** Returns its top-left chunk Z relative to the city center. */
    public int relChunkZ() {
        return relChunkZ;
    }

    /** Returns whether it is a multibuilding. */
    public boolean multi() {
        return multi;
    }

    /** Returns whether it is never a ruin. */
    public boolean preventRuins() {
        return preventRuins;
    }
}
