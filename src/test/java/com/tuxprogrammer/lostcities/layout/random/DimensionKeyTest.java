package com.tuxprogrammer.lostcities.layout.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ParityData;

/**
 * {@link DimensionKey}: the 1.7.10 dimension ID to upstream 1.20 ID mapping of P04-1, and the {@code ResourceLocation}
 * hash helper, checked against values recorded from Minecraft 1.20.1 (settling the "medium confidence" formula of
 * new-spec 04-city-layout).
 */
public class DimensionKeyTest {

    @Test
    public void dimensionZeroIsTheOverworld() {
        assertSame(DimensionKey.OVERWORLD, DimensionKey.forDimension(0));
        assertEquals("minecraft:overworld", DimensionKey.OVERWORLD.id());
        assertEquals(
            "lostcitiesnh:dim7",
            DimensionKey.forDimension(7)
                .id());
        assertEquals(
            "lostcitiesnh:dim-1",
            DimensionKey.forDimension(-1)
                .id());
    }

    @Test
    public void resourceLocationHashMatchesMinecraft120() throws Exception {
        List<String[]> records = ParityData.records("resource_location_hash.txt");
        assertTrue(records.size() >= 5);
        for (String[] r : records) {
            assertEquals(
                Integer.parseInt(r[1]),
                DimensionKey.parse(r[0])
                    .resourceLocationHash(),
                r[0]);
        }
    }

    @Test
    public void stableHashIsTheStreetPlannersDimensionSalt() {
        assertEquals(StableHash.stableStringHash("minecraft:overworld"), DimensionKey.OVERWORLD.stableHash());
    }

    @Test
    public void parseRejectsMalformedIds() {
        assertThrows(IllegalArgumentException.class, () -> DimensionKey.parse("overworld"));
        assertThrows(IllegalArgumentException.class, () -> DimensionKey.parse(":x"));
        assertThrows(IllegalArgumentException.class, () -> DimensionKey.parse("a:b:c"));
        assertSame(DimensionKey.OVERWORLD, DimensionKey.parse("minecraft:overworld"));
    }
}
