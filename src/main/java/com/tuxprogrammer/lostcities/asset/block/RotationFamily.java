package com.tuxprogrammer.lostcities.asset.block;

/**
 * How a 1.7.10 block stores its orientation in metadata, so the writer can turn it with a part's transform. Every
 * directional block turns with the part, not only stairs, doors and rails (docs/new-spec/05-assets/index.md,
 * "Transforms: rotations"; fixes ASSET-18 and LOOT-09). The encodings are those of {@link MetaRule}.
 *
 * <p>
 * Side: both.
 */
public enum RotationFamily {
    /** No orientation in metadata. */
    NONE,
    /** Stairs: bits 0 to 1 east, west, south, north; bit 2 upside down. */
    STAIRS,
    /** Doors: lower half bits 0 to 1 east, south, west, north; upper half bit 0 the hinge side. */
    DOOR,
    /** Torches: 1 to 4 east, west, south, north on a wall; 5 standing. */
    TORCH,
    /** Ladders, furnaces, chests: 2 to 5 north, south, west, east. */
    FACING,
    /** Levers: wall 1 to 4 as torches, floor 5 and 6, ceiling 7 and 0, by axis. */
    LEVER,
    /** Rails: shapes 0 to 9. */
    RAIL,
    /** Powered, detector and activator rails: straight shapes 0 to 5, bit 3 powered. */
    POWERED_RAIL,
    /** Trapdoors: bits 0 to 1 north, south, west, east. */
    TRAPDOOR,
    /** Six-direction blocks: down, up, north, south, west, east (barrels, pistons). */
    PISTON,
    /** Axis blocks: y 0, x 1, z 2 (chains). */
    AXIS,
    /** Anvils: bits 0 to 1 a horizontal index south, west, north, east. */
    ANVIL,
    /** Vines: one bit per side, south 1, west 2, north 4, east 8. */
    VINE
}
