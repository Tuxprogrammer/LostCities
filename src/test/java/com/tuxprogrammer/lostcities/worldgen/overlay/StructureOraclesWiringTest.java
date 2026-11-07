package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** LootGames rooms and AE2 meteorites are not landmarks; the other oracles stay wired (D-landmark-policy-1). */
class StructureOraclesWiringTest {

    @Test
    void lootGamesAndMeteoritesAreNotWired() {
        List<String> names = StructureOracles.factoryNames();
        assertEquals(
            StructureOracles.factories()
                .size(),
            names.size(),
            "one name per factory");
        assertFalse(names.contains("lootgames"), names.toString());
        assertFalse(names.contains("ae2"), names.toString());
        assertTrue(names.contains("witchery"), "the Witchery veto reads its predicted sites");
        assertTrue(names.contains("roguelike"), "the steering reads its predicted sites");
        assertTrue(names.contains("villagenames"));
        assertTrue(names.contains("rwg2"));
    }
}
