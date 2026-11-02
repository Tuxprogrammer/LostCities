// spotless:off
package com.tuxprogrammer.lostcities.asset.transform;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Transform.java
// Avoids: CITY-27, CITY-28, ASSET-18
// spotless:on

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.RotationFamily;

/**
 * Turns a block's metadata with a part's {@link Transform}, by its {@link RotationFamily}: the 1.7.10 counterpart of
 * {@code BlockState.rotate} and upstream's rail-shape table (docs/original-spec/05-assets/parts-and-placement.md,
 * "Transforms"; D19, "Blockstate properties to metadata").
 *
 * <p>
 * Changes from upstream: every directional block turns with the part, not only stairs, doors and rails (fixes
 * ASSET-18 and LOOT-09); mirrors reflect facings and swap door hinges (fixes CITY-28, upstream's ASSET-17); and the
 * rail table is complete, computed from each shape's two ends (fixes CITY-27, upstream's ASSET-16). Bits that are not
 * orientation (open, powered, upside down, damage) are kept. Side: both.
 */
public final class MetaRotation {

    /** Stairs: facing 0 east, 1 west, 2 south, 3 north in bits 0 to 1. */
    private static final Direction[] STAIRS = { Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };
    /** Lower door halves: facing 0 east, 1 south, 2 west, 3 north in bits 0 to 1. */
    private static final Direction[] DOOR = { Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };
    /** Wall torches and levers: 1 east, 2 west, 3 south, 4 north. */
    private static final Direction[] WALL = { null, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };
    /** Ladders, furnaces, chests, and the horizontal half of six-direction blocks: 2 north to 5 east. */
    private static final Direction[] SIX = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
        Direction.WEST, Direction.EAST };
    /** Trapdoors: 0 north, 1 south, 2 west, 3 east in bits 0 to 1. */
    private static final Direction[] TRAPDOOR = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    /** Anvils: a horizontal index, 0 south, 1 west, 2 north, 3 east, in bits 0 to 1. */
    private static final Direction[] ANVIL = { Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST };
    /** Vines: one bit per side. */
    private static final Direction[] VINE = { Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST };
    /**
     * Rail shapes 0 to 9 by their two ends; for an ascending shape the first end is the raised one: north_south,
     * east_west, ascending_east, ascending_west, ascending_north, ascending_south, south_east, south_west, north_west,
     * north_east.
     */
    private static final Direction[][] RAIL = { { Direction.NORTH, Direction.SOUTH },
        { Direction.EAST, Direction.WEST }, { Direction.EAST, Direction.WEST }, { Direction.WEST, Direction.EAST },
        { Direction.NORTH, Direction.SOUTH }, { Direction.SOUTH, Direction.NORTH }, { Direction.SOUTH, Direction.EAST },
        { Direction.SOUTH, Direction.WEST }, { Direction.NORTH, Direction.WEST }, { Direction.NORTH, Direction.EAST } };
    /** The first ascending rail shape. */
    private static final int FIRST_ASCENDING = 2;
    /** The last ascending rail shape. */
    private static final int LAST_ASCENDING = 5;
    /** The powered bit of powered rails and levers. */
    private static final int BIT_8 = 8;

    private MetaRotation() {}

    /**
     * Turns a block.
     *
     * @param block     the block as the palette gives it
     * @param family    how its metadata stores its orientation
     * @param transform the part's transform
     * @return the block with turned metadata; the same block for {@link RotationFamily#NONE} or the identity
     */
    public static BlockRef rotate(BlockRef block, RotationFamily family, Transform transform) {
        if (transform == Transform.ROTATE_NONE || family == RotationFamily.NONE) {
            return block;
        }
        return block.withMeta(rotate(block.meta(), family, transform));
    }

    /**
     * Turns metadata.
     *
     * @param meta      the metadata
     * @param family    how it stores the orientation
     * @param transform the transform
     * @return the turned metadata
     */
    public static int rotate(int meta, RotationFamily family, Transform transform) {
        switch (family) {
            case STAIRS:
                return (meta & ~3) | index(STAIRS, transform.apply(STAIRS[meta & 3]));
            case DOOR:
                return door(meta, transform);
            case TORCH:
                return wall(meta, transform);
            case FACING:
            case PISTON:
                return six(meta, transform);
            case LEVER:
                return lever(meta, transform);
            case RAIL:
                return rail(meta, transform);
            case POWERED_RAIL:
                return (meta & BIT_8) | rail(meta & 7, transform);
            case TRAPDOOR:
                return (meta & ~3) | index(TRAPDOOR, transform.apply(TRAPDOOR[meta & 3]));
            case AXIS:
                return axis(meta, transform);
            case ANVIL:
                return (meta & ~3) | index(ANVIL, transform.apply(ANVIL[meta & 3]));
            case VINE:
                return vine(meta, transform);
            default:
                return meta;
        }
    }

    private static int door(int meta, Transform transform) {
        if ((meta & BIT_8) != 0) {
            // Upper half: bit 0 is the hinge side, which a mirror swaps; rotations leave it.
            return transform.isMirror() ? meta ^ 1 : meta;
        }
        return (meta & ~3) | index(DOOR, transform.apply(DOOR[meta & 3]));
    }

    private static int wall(int meta, Transform transform) {
        int orientation = meta & 7;
        if (orientation < 1 || orientation > 4) {
            return meta;
        }
        return (meta & ~7) | index(WALL, transform.apply(WALL[orientation]));
    }

    private static int six(int meta, Transform transform) {
        int orientation = meta & 7;
        if (orientation > 5) {
            return meta;
        }
        return (meta & ~7) | index(SIX, transform.apply(SIX[orientation]));
    }

    private static int lever(int meta, Transform transform) {
        int orientation = meta & 7;
        if (orientation >= 1 && orientation <= 4) {
            return wall(meta, transform);
        }
        boolean swapsAxes = transform.apply(Direction.EAST)
            .dx() == 0;
        if (!swapsAxes) {
            return meta;
        }
        int powered = meta & BIT_8;
        switch (orientation) {
            case 5:
                return powered | 6;
            case 6:
                return powered | 5;
            case 7:
                return powered;
            default:
                return powered | 7;
        }
    }

    private static int rail(int shape, Transform transform) {
        if (shape < 0 || shape >= RAIL.length) {
            return shape;
        }
        Direction first = transform.apply(RAIL[shape][0]);
        Direction second = transform.apply(RAIL[shape][1]);
        boolean ascending = shape >= FIRST_ASCENDING && shape <= LAST_ASCENDING;
        for (int candidate = 0; candidate < RAIL.length; candidate++) {
            boolean candidateAscending = candidate >= FIRST_ASCENDING && candidate <= LAST_ASCENDING;
            if (candidateAscending != ascending) {
                continue;
            }
            Direction[] ends = RAIL[candidate];
            if (ascending ? ends[0] == first
                : (ends[0] == first && ends[1] == second) || (ends[0] == second && ends[1] == first)) {
                return candidate;
            }
        }
        return shape;
    }

    private static int axis(int meta, Transform transform) {
        int axis = meta & 3;
        boolean swapsAxes = transform.apply(Direction.EAST)
            .dx() == 0;
        if (!swapsAxes || axis == 0) {
            return meta;
        }
        return (meta & ~3) | (axis == 1 ? 2 : 1);
    }

    private static int vine(int meta, Transform transform) {
        int out = meta & ~15;
        for (int bit = 0; bit < VINE.length; bit++) {
            if ((meta & (1 << bit)) != 0) {
                out |= 1 << index(VINE, transform.apply(VINE[bit]));
            }
        }
        return out;
    }

    private static int index(Direction[] table, Direction direction) {
        for (int i = 0; i < table.length; i++) {
            if (table[i] == direction) {
                return i;
            }
        }
        throw new IllegalStateException(direction + " is not in the table");
    }
}
