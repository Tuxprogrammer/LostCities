package com.tuxprogrammer.lostcities.compat.packhouses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The reflection helper sets a piece's ground and villager counters by class and value, and copes with absence. */
public class PieceFieldsTest {

    /** Stands in for the village base class: a ground field at -1 and a villager counter at 0. */
    static class FakeVillage {
        protected int hpos = -1;
        private int villagers;
        private boolean desert;
    }

    /** Stands in for a mod's piece: its own private ground field, a counter of its own and unrelated ints. */
    static class FakeHouse extends FakeVillage {
        private int averageGroundLevel = -1;
        private int witchesSpawned;
        private int width = 9;
        private int unrelatedMinusOne = -1;
        private static int staticGround = -1;
    }

    /** A piece with none of the fields. */
    static class Bare {
        private final String name = "x";
    }

    @Test
    public void setsTheBaseAndSubclassFieldsAndLeavesOthersAlone() throws Exception {
        FakeHouse house = new FakeHouse();
        PieceFields.Result result = PieceFields.apply(house, FakeVillage.class, 64);
        assertEquals(2, result.groundFields());
        assertEquals(2, result.counterFields());
        assertEquals(64, house.hpos);
        assertEquals(64, field(house, "averageGroundLevel"));
        assertEquals(PieceFields.NO_VILLAGERS, field(house, "witchesSpawned"));
        assertEquals(PieceFields.NO_VILLAGERS, field(house, "villagers", FakeVillage.class));
        assertEquals(9, field(house, "width"));
        assertEquals(-1, field(house, "unrelatedMinusOne"));
        assertEquals(-1, FakeHouse.staticGround);
    }

    @Test
    public void aMissingFieldIsNotAnError() {
        PieceFields.Result result = PieceFields.apply(new Bare(), FakeVillage.class, 64);
        assertEquals(0, result.groundFields());
        assertEquals(0, result.counterFields());
    }

    @Test
    public void aPieceAlreadyGroundedKeepsItsGround() {
        FakeVillage village = new FakeVillage();
        village.hpos = 70;
        PieceFields.Result result = PieceFields.apply(village, FakeVillage.class, 64);
        assertEquals(0, result.groundFields());
        assertEquals(70, village.hpos);
    }

    private static int field(Object o, String name) throws Exception {
        return field(o, name, o.getClass());
    }

    private static int field(Object o, String name, Class<?> type) throws Exception {
        java.lang.reflect.Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(o);
    }
}
