package com.tuxprogrammer.lostcities.asset.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.RotationFamily;
import com.tuxprogrammer.lostcities.asset.model.Palette;

/** Metadata turned with a part's transform, per rotation family. */
public class MetaRotationTest {

    @Test
    public void stairsTurnAndKeepTheirUpsideDownBit() {
        // East 0 turns 90 degrees clockwise to south 2; north top 7 turns to east top 4.
        assertEquals(2, MetaRotation.rotate(0, RotationFamily.STAIRS, Transform.ROTATE_90));
        assertEquals(4, MetaRotation.rotate(7, RotationFamily.STAIRS, Transform.ROTATE_90));
        assertEquals(1, MetaRotation.rotate(0, RotationFamily.STAIRS, Transform.ROTATE_180));
        assertEquals(3, MetaRotation.rotate(0, RotationFamily.STAIRS, Transform.ROTATE_270));
    }

    @Test
    public void doesNotReproduceCITY28_mirrorsReflectAndSwapDoorHinges() {
        // A north-facing stair stays north under MIRROR_X; upstream turned it south.
        assertEquals(3, MetaRotation.rotate(3, RotationFamily.STAIRS, Transform.MIRROR_X));
        assertEquals(1, MetaRotation.rotate(0, RotationFamily.STAIRS, Transform.MIRROR_X));
        // Upper door half: a mirror swaps the hinge; a rotation leaves it.
        assertEquals(9, MetaRotation.rotate(8, RotationFamily.DOOR, Transform.MIRROR_Z));
        assertEquals(8, MetaRotation.rotate(8, RotationFamily.DOOR, Transform.ROTATE_90));
        // Lower door half facing east (0), open (4): under ROTATE_90 it faces south (1), still open.
        assertEquals(5, MetaRotation.rotate(4, RotationFamily.DOOR, Transform.ROTATE_90));
    }

    @Test
    public void doesNotReproduceCITY27_everyRailShapeHasAnImageUnderEveryTransform() {
        for (Transform transform : Transform.values()) {
            for (int shape = 0; shape < 10; shape++) {
                int image = MetaRotation.rotate(shape, RotationFamily.RAIL, transform);
                assertEquals(
                    shape,
                    MetaRotation.rotate(image, RotationFamily.RAIL, transform.opposite()),
                    transform + " " + shape);
            }
        }
        // MIRROR_Z on ascending_east stays ascending_east (upstream threw); MIRROR_90_X turns east_west north_south.
        assertEquals(2, MetaRotation.rotate(2, RotationFamily.RAIL, Transform.MIRROR_Z));
        assertEquals(0, MetaRotation.rotate(1, RotationFamily.RAIL, Transform.MIRROR_90_X));
        // A south_east curve (6) mirrored across X becomes south_west (7); powered rails keep their powered bit.
        assertEquals(7, MetaRotation.rotate(6, RotationFamily.RAIL, Transform.MIRROR_X));
        assertEquals(9, MetaRotation.rotate(8, RotationFamily.POWERED_RAIL, Transform.ROTATE_90));
    }

    @Test
    public void doesNotReproduceASSET18_everyDirectionalBlockTurns() {
        // Chests, furnaces, ladders: north 2 turns east 5 under ROTATE_90.
        assertEquals(5, MetaRotation.rotate(2, RotationFamily.FACING, Transform.ROTATE_90));
        // Wall torches: east 1 turns south 3; standing 5 stays.
        assertEquals(3, MetaRotation.rotate(1, RotationFamily.TORCH, Transform.ROTATE_90));
        assertEquals(5, MetaRotation.rotate(5, RotationFamily.TORCH, Transform.ROTATE_90));
        // Levers: wall north 4 powered 8 turns east 1, powered; floor axis swaps.
        assertEquals(9, MetaRotation.rotate(12, RotationFamily.LEVER, Transform.ROTATE_90));
        assertEquals(6, MetaRotation.rotate(5, RotationFamily.LEVER, Transform.ROTATE_90));
        // Trapdoors: north 0 turns east 3, open and top bits kept.
        assertEquals(15, MetaRotation.rotate(12, RotationFamily.TRAPDOOR, Transform.ROTATE_90));
        // Barrels: north 2 turns east 5; up 1 stays.
        assertEquals(5, MetaRotation.rotate(2, RotationFamily.PISTON, Transform.ROTATE_90));
        assertEquals(1, MetaRotation.rotate(1, RotationFamily.PISTON, Transform.ROTATE_90));
        // Chains: x 1 turns z 2.
        assertEquals(2, MetaRotation.rotate(1, RotationFamily.AXIS, Transform.ROTATE_90));
        // Anvils: north 2 turns east 3.
        assertEquals(3, MetaRotation.rotate(2, RotationFamily.ANVIL, Transform.ROTATE_90));
        // Vines: a west vine (2) turns north (4).
        assertEquals(4, MetaRotation.rotate(2, RotationFamily.VINE, Transform.ROTATE_90));
    }

    @Test
    public void theMapGivesBundledBlocksTheirFamilies() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        Palette oilrig = set.palette(AssetId.parse("oilrig"));
        assertEquals(
            RotationFamily.STAIRS,
            set.mapping()
                .familyOf("minecraft:quartz_stairs"));
        assertEquals(
            RotationFamily.FACING,
            set.mapping()
                .familyOf("minecraft:ladder"));
        assertEquals(
            RotationFamily.PISTON,
            set.mapping()
                .familyOf("etfuturum:barrel"));
        for (com.tuxprogrammer.lostcities.asset.model.PaletteEntry entry : oilrig.entries()
            .values()) {
            BlockChoice choice = entry.block();
            if (choice != null && choice.block()
                .name()
                .equals("minecraft:ladder")) {
                assertEquals(RotationFamily.FACING, choice.family());
                assertEquals(
                    BlockRef.parse("minecraft:ladder:3"),
                    MetaRotation.rotate(choice.block(), choice.family(), Transform.ROTATE_90),
                    "a ladder facing east (5) turns to face south (3)");
            }
        }
    }
}
