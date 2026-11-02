package com.tuxprogrammer.lostcities.asset.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/** Part transforms (docs/original-spec/05-assets/parts-and-placement.md, "Transforms"). */
public class TransformTest {

    @Test
    public void positionsFollowUpstreamsTable() {
        int x = 3;
        int z = 5;
        assertEquals(3, Transform.ROTATE_NONE.rotateX(x, z));
        assertEquals(5, Transform.ROTATE_NONE.rotateZ(x, z));
        assertEquals(10, Transform.ROTATE_90.rotateX(x, z));
        assertEquals(3, Transform.ROTATE_90.rotateZ(x, z));
        assertEquals(12, Transform.ROTATE_180.rotateX(x, z));
        assertEquals(10, Transform.ROTATE_180.rotateZ(x, z));
        assertEquals(5, Transform.ROTATE_270.rotateX(x, z));
        assertEquals(12, Transform.ROTATE_270.rotateZ(x, z));
        assertEquals(12, Transform.MIRROR_X.rotateX(x, z));
        assertEquals(5, Transform.MIRROR_X.rotateZ(x, z));
        assertEquals(3, Transform.MIRROR_Z.rotateX(x, z));
        assertEquals(10, Transform.MIRROR_Z.rotateZ(x, z));
        assertEquals(5, Transform.MIRROR_90_X.rotateX(x, z));
        assertEquals(3, Transform.MIRROR_90_X.rotateZ(x, z));
    }

    @Test
    public void rotationsTurnClockwiseSeenFromAbove() {
        assertEquals(Direction.SOUTH, Transform.ROTATE_90.apply(Direction.EAST));
        assertEquals(Direction.EAST, Transform.ROTATE_90.apply(Direction.NORTH));
        assertEquals(Direction.WEST, Transform.ROTATE_180.apply(Direction.EAST));
        assertEquals(Direction.NORTH, Transform.ROTATE_270.apply(Direction.EAST));
        assertEquals(Direction.UP, Transform.ROTATE_90.apply(Direction.UP));
        assertEquals(Transform.ROTATE_270, Transform.ROTATE_90.opposite());
        assertEquals(Transform.MIRROR_X, Transform.MIRROR_X.opposite());
    }

    @Test
    public void doesNotReproduceCITY28_mirrorsReflectFacings() {
        // Upstream rotated stairs and doors by 180 degrees under MIRROR_X, turning a north-facing stair south.
        assertEquals(Direction.NORTH, Transform.MIRROR_X.apply(Direction.NORTH));
        assertEquals(Direction.WEST, Transform.MIRROR_X.apply(Direction.EAST));
        assertEquals(Direction.EAST, Transform.MIRROR_Z.apply(Direction.EAST));
        assertEquals(Direction.NORTH, Transform.MIRROR_Z.apply(Direction.SOUTH));
        assertEquals(Direction.SOUTH, Transform.MIRROR_90_X.apply(Direction.EAST));
        assertEquals(Direction.WEST, Transform.MIRROR_90_X.apply(Direction.NORTH));
        assertTrue(Transform.MIRROR_X.isMirror());
        assertTrue(Transform.MIRROR_90_X.isMirror());
        assertFalse(Transform.ROTATE_90.isMirror());
    }

    @Test
    public void doesNotReproduceCITY27_everyDirectionHasAnImageUnderEveryTransform() {
        for (Transform transform : Transform.values()) {
            for (Direction direction : Direction.values()) {
                Direction image = transform.apply(direction);
                assertEquals(
                    direction,
                    transform.opposite()
                        .apply(image),
                    transform + " " + direction);
            }
        }
    }

    @Test
    public void doesNotReproduceASSET22_noUnseededRandomRotation() {
        for (Method method : Transform.class.getDeclaredMethods()) {
            assertFalse(
                method.getName()
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains("random"),
                method.toString());
        }
    }
}
