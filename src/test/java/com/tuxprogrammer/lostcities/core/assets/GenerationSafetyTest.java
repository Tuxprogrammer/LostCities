package com.tuxprogrammer.lostcities.core.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import org.junit.jupiter.api.Test;

/**
 * D165: a block class that overrides the world-aware light opacity is found by reading its bytes, through its
 * superclasses, without reflecting on (and so loading) the classes its methods name.
 */
public class GenerationSafetyTest {

    /** Reads the world for its opacity, as Thaumcraft's {@code BlockCosmeticOpaque} does. */
    static class ReadsTheWorld extends Block {

        ReadsTheWorld() {
            super(Material.rock);
        }

        @Override
        public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
            return world.getBlockMetadata(x, y, z) == 2 ? 0 : 255;
        }
    }

    /** Inherits the override, as Automagy's glowing amber does. */
    static class Inherits extends ReadsTheWorld {
    }

    /** A plain block. */
    static class Plain extends Block {

        Plain() {
            super(Material.rock);
        }
    }

    @Test
    public void theOverrideIsFoundInTheClassOrItsSuperclasses() {
        assertEquals(ReadsTheWorld.class, GenerationSafety.overrider(ReadsTheWorld.class));
        assertEquals(ReadsTheWorld.class, GenerationSafety.overrider(Inherits.class));
        assertNull(GenerationSafety.overrider(Plain.class));
        assertNull(GenerationSafety.overrider(Block.class));
    }
}
