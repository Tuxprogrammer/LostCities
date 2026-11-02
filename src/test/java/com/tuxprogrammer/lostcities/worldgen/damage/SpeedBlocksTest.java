package com.tuxprogrammer.lostcities.worldgen.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/** The speed-block swap of decision log D28 rule 3. */
public class SpeedBlocksTest {

    @Test
    public void everyCatalogSpeedBlockHasANormalPartner() {
        String[] speed = { "chisel:concrete", "chisel:road", "chisel:road2", "chisel:concrete_slab",
            "gregtech:gt.blockconcretes", "TConstruct:SpeedBlock", "TConstruct:SpeedSlab" };
        for (String name : speed) {
            for (int meta = 0; meta < 16; meta++) {
                BlockRef normal = SpeedBlocks.normalFor(BlockRef.of(name, meta));
                assertTrue(normal != null && "minecraft".equals(normal.modId()), name + ":" + meta);
                assertFalse(SpeedBlocks.isSpeedBlock(normal));
            }
        }
    }

    @Test
    public void slabsKeepTheirHalf() {
        assertEquals(
            BlockRef.of("minecraft:stone_slab", 8),
            SpeedBlocks.normalFor(BlockRef.of("chisel:concrete_slab", 9)));
        assertEquals(
            BlockRef.of("minecraft:stone_slab", 1),
            SpeedBlocks.normalFor(BlockRef.of("TConstruct:SpeedSlab", 3)));
    }

    @Test
    public void ordinaryBlocksAreLeftAlone() {
        BlockRef brick = BlockRef.of("minecraft:stonebrick", 0);
        assertNull(SpeedBlocks.normalFor(brick));
        assertEquals(brick, SpeedBlocks.withoutSpeed(brick));
    }
}
