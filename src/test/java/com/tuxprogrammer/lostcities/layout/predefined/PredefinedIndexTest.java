package com.tuxprogrammer.lostcities.layout.predefined;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;

/** {@link PredefinedIndex}: predefined content by chunk, with whole footprints and no silent overlaps. */
public class PredefinedIndexTest {

    private static MultiBuildingView multi(String name, int dimX, int dimZ) {
        return new MultiBuildingView() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public int dimX() {
                return dimX;
            }

            @Override
            public int dimZ() {
                return dimZ;
            }

            @Override
            public String buildingAt(int x, int z) {
                return "b";
            }
        };
    }

    private static PredefinedCity city(String source, int x, int z, List<PredefinedBuilding> buildings,
        List<int[]> streets) {
        return new PredefinedCity(source, "minecraft:overworld", x, z, 100, "style", buildings, streets);
    }

    @Test
    public void indexesCitiesBuildingsAndStreets() {
        PredefinedIndex index = PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                city(
                    "a",
                    10,
                    10,
                    Collections.singletonList(new PredefinedBuilding("house", 2, 0, false, true)),
                    Collections.singletonList(new int[] { -1, 0 }))),
            name -> null);
        assertNotNull(index.cityAt(10, 10));
        assertEquals(
            "house",
            index.buildingAtTopLeft(12, 10)
                .building());
        assertTrue(
            index.occupant(9, 10)
                .isStreet());
        assertNull(index.occupant(11, 10));
        assertTrue(
            index.problems()
                .isEmpty());
    }

    @Test
    public void otherDimensionsAreLeftOut() {
        PredefinedIndex index = PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                new PredefinedCity(
                    "n",
                    "minecraft:the_nether",
                    0,
                    0,
                    50,
                    "s",
                    Collections.emptyList(),
                    Collections.emptyList())),
            name -> null);
        assertTrue(index.isEmpty());
    }

    @Test
    public void doesNotReproduceCITY07_theWholeMultibuildingFootprintIsOccupied() {
        PredefinedIndex index = PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                city(
                    "a",
                    0,
                    0,
                    Collections.singletonList(new PredefinedBuilding("big", 0, 0, true, false)),
                    Collections.<int[]>emptyList())),
            name -> multi(name, 3, 4));
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 4; z++) {
                PredefinedIndex.Occupant o = index.occupant(x, z);
                assertNotNull(o, x + "," + z);
                assertEquals(x, o.offsetX());
                assertEquals(z, o.offsetZ());
            }
        }
        assertNull(index.occupant(3, 0));
        assertNull(index.occupant(0, 4));
    }

    @Test
    public void doesNotReproduceCITY08_overlapsAreRejectedNamingBothAssets() {
        PredefinedCity first = city(
            "a_first",
            0,
            0,
            Collections.singletonList(new PredefinedBuilding("one", 0, 0, false, false)),
            Collections.<int[]>emptyList());
        PredefinedCity second = city(
            "b_second",
            5,
            0,
            Collections.singletonList(new PredefinedBuilding("two", -5, 0, false, false)),
            Collections.<int[]>emptyList());
        // Whatever order the loader hands them over in, the first by source name keeps the chunk.
        for (List<PredefinedCity> order : Arrays.asList(Arrays.asList(first, second), Arrays.asList(second, first))) {
            PredefinedIndex index = PredefinedIndex.build("minecraft:overworld", order, name -> null);
            assertEquals(
                "one",
                index.buildingAtTopLeft(0, 0)
                    .building());
            assertEquals(
                1,
                index.problems()
                    .size());
            String problem = index.problems()
                .get(0);
            assertTrue(problem.contains("a_first") && problem.contains("b_second"), problem);
        }
    }
}
