package com.tuxprogrammer.lostcities.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Block references: the {@code modid:name:meta} text form, validation and value equality. */
public class BlockRefTest {

    @Test
    public void parsesTheNativeTextForm() {
        BlockRef ref = BlockRef.parse("chisel:moss:0");
        assertEquals("chisel:moss", ref.name());
        assertEquals("chisel", ref.modId());
        assertEquals(0, ref.meta());
        assertEquals("chisel:moss:0", ref.toString());
    }

    @Test
    public void metadataDefaultsToZero() {
        assertEquals(BlockRef.of("minecraft:stone", 0), BlockRef.parse("minecraft:stone"));
    }

    @Test
    public void keepsDotsAndCaseInPackNames() {
        // Ztones registers names such as tile.iszmBlock (docs/new-spec/00-pack-integration/block-mapping.md, D30).
        BlockRef ref = BlockRef.parse("Ztones:tile.iszmBlock:8");
        assertEquals("Ztones:tile.iszmBlock", ref.name());
        assertEquals(8, ref.meta());
    }

    @Test
    public void airIsOneInstance() {
        assertSame(BlockRef.AIR, BlockRef.parse("minecraft:air"));
        assertTrue(
            BlockRef.parse("minecraft:air:0")
                .isAir());
    }

    @Test
    public void comparesByValue() {
        assertEquals(BlockRef.of("minecraft:wool", 3), BlockRef.parse("minecraft:wool:3"));
        assertEquals(
            BlockRef.of("minecraft:wool", 3)
                .hashCode(),
            BlockRef.parse("minecraft:wool:3")
                .hashCode());
        assertNotEquals(BlockRef.of("minecraft:wool", 3), BlockRef.of("minecraft:wool", 4));
    }

    @Test
    public void rejectsMalformedReferences() {
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse("stone"));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse(":stone"));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse("minecraft:"));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse("minecraft:stone:16"));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse("minecraft:stone:x"));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse("minecraft:st one"));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.of("minecraft:stone", -1));
        assertThrows(IllegalArgumentException.class, () -> BlockRef.parse(null));
    }
}
