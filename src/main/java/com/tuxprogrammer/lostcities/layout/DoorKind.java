package com.tuxprogrammer.lostcities.layout;

/**
 * The door a building's doorways get, in the order of upstream's {@code nextInt(7)} draw. Which block each becomes is
 * the block mapping's: the woods are MalisisDoors' doors with {@code minecraft:wooden_door} as the fallback, oak and
 * iron the vanilla doors (decision log D28 rule 5, block-mapping.md § Doors and trapdoors).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/building-info.md, step 12.8. Side: both.
 */
public enum DoorKind {

    /** Draw 0. */
    BIRCH,
    /** Draw 1. */
    ACACIA,
    /** Draw 2. */
    DARK_OAK,
    /** Draw 3. */
    SPRUCE,
    /** Draw 4. */
    OAK,
    /** Draw 5. */
    JUNGLE,
    /** Draw 6. */
    IRON;

    /** How many kinds the draw chooses among. */
    public static final int COUNT = 7;

    /**
     * Returns the door for a draw.
     *
     * @param draw the {@code nextInt(7)} result
     * @return the door
     */
    public static DoorKind forDraw(int draw) {
        return values()[draw];
    }
}
