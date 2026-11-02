package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * VillageNames' villages and ocean monuments as one oracle: every landmark either knows, sorted by identity. Pure
 * because both parts are. {@link VillageNamesOracles#forWorld} returns it; the {@code oracle.villagenames} scenario
 * reads the parts.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class VillageNamesStructureOracle implements StructureOracle {

    private final VillageOracle villages;
    private final MonumentOracle monuments;

    /**
     * Joins the two oracles.
     *
     * @param villages  the village oracle
     * @param monuments the monument oracle
     */
    public VillageNamesStructureOracle(VillageOracle villages, MonumentOracle monuments) {
        this.villages = villages;
        this.monuments = monuments;
    }

    /** Returns the village oracle. */
    public VillageOracle villages() {
        return villages;
    }

    /** Returns the monument oracle. */
    public MonumentOracle monuments() {
        return monuments;
    }

    @Override
    public List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        List<Landmark> out = new ArrayList<>(villages.landmarksTouching(minChunkX, minChunkZ, maxChunkX, maxChunkZ));
        out.addAll(monuments.landmarksTouching(minChunkX, minChunkZ, maxChunkX, maxChunkZ));
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }
}
