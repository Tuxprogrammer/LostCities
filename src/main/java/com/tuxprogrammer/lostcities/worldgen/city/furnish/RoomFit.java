package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.util.Random;

import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * Which rooms may hold loot chests and spawners, and how rare they are (design record 0016, decisions 10 and 11).
 * Loot belongs in storage-like rooms (cellars, offices, kitchens, industrial floors); spawners belong in cellars and
 * industrial floors only, never in homes, shops or on streets. Both are thinned by a fixed factor so they are rarer
 * than the legacy placement. Pure functions of a seed and a position. Side: both (plain Java).
 */
public final class RoomFit {

    /** Share of the parts' loot chests that survive in a fitting room (legacy: all of them). */
    public static final float CHEST_KEEP = 0.5f;
    /** Share of the parts' spawners that survive in a fitting room (legacy: all of them). */
    public static final float SPAWNER_KEEP = 0.4f;

    private static final long POS_X = 341873128712L;
    private static final long POS_Z = 132897987541L;
    private static final long POS_Y = 2654435761L;
    private static final long KEEP_STREAM = 0x6B656570L;

    private RoomFit() {}

    /** Returns whether loot chests fit a room kind. */
    public static boolean lootFits(RoomKind kind) {
        return kind == RoomKind.BASEMENT || kind == RoomKind.OFFICE
            || kind == RoomKind.KITCHEN
            || kind == RoomKind.INDUSTRIAL;
    }

    /** Returns whether spawners fit a room kind. */
    public static boolean spawnerFits(RoomKind kind) {
        return kind == RoomKind.BASEMENT || kind == RoomKind.INDUSTRIAL;
    }

    /**
     * Returns whether a part's chest or spawner stays, by its own deterministic roll (it draws from no other random).
     *
     * @param worldSeed the world seed
     * @param x         world X
     * @param y         Y
     * @param z         world Z
     * @param keep      {@link #CHEST_KEEP} or {@link #SPAWNER_KEEP}
     * @return whether it survives the thinning
     */
    public static boolean survives(long worldSeed, long x, int y, long z, float keep) {
        long seed = SeedMix.mix(worldSeed ^ KEEP_STREAM, x * POS_X + z * POS_Z + y * POS_Y);
        return new Random(seed).nextFloat() < keep;
    }
}
