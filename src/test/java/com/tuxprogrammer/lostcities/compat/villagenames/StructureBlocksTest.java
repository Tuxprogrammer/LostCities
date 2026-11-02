package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The scenario's evidence blocks and the oracles' bounded cache. */
class StructureBlocksTest {

    @Test
    void builtBlocksCountNaturalOnesDoNot() {
        assertTrue(StructureBlocks.isVillageBlock("minecraft:planks"));
        assertTrue(StructureBlocks.isVillageBlock("minecraft:torch"));
        assertTrue(StructureBlocks.isVillageBlock("minecraft:farmland"));
        assertTrue(StructureBlocks.isVillageBlock("VillageNames:concrete"));
        assertTrue(StructureBlocks.isVillageBlock("etfuturum:lantern"));
        assertFalse(StructureBlocks.isVillageBlock("etfuturum:deepslate"));
        assertTrue(StructureBlocks.isVillageBlock("witchery:snowstairs"));
        assertFalse(StructureBlocks.isVillageBlock("gregtech:gt.blockores2"));
        assertFalse(StructureBlocks.isVillageBlock("minecraft:snow_layer"));
        assertFalse(StructureBlocks.isVillageBlock("minecraft:log"));
        assertFalse(StructureBlocks.isVillageBlock("minecraft:cobblestone"));
        assertFalse(StructureBlocks.isVillageBlock("minecraft:sandstone"));
        assertFalse(StructureBlocks.isVillageBlock("minecraft:grass"));
        assertFalse(StructureBlocks.isVillageBlock(null));
    }

    @Test
    void monumentBlocks() {
        assertTrue(StructureBlocks.isMonumentBlock("etfuturum:prismarine_block"));
        assertTrue(StructureBlocks.isMonumentBlock("VillageNames:sea_lantern"));
        assertFalse(StructureBlocks.isMonumentBlock("minecraft:water"));
        assertFalse(StructureBlocks.isMonumentBlock(null));
    }

    @Test
    void cacheKeepsTheMostRecent() {
        BoundedCache<String> cache = new BoundedCache<>(2);
        cache.put(BoundedCache.key(0, 0), "a");
        cache.put(BoundedCache.key(1, 0), "b");
        cache.get(BoundedCache.key(0, 0));
        cache.put(BoundedCache.key(-1, -1), "c");
        assertEquals(2, cache.size());
        assertEquals("a", cache.get(BoundedCache.key(0, 0)));
        assertNull(cache.get(BoundedCache.key(1, 0)));
        assertEquals("c", cache.get(BoundedCache.key(-1, -1)));
    }

    @Test
    void keysAreUniquePerChunk() {
        assertFalse(BoundedCache.key(1, -1) == BoundedCache.key(-1, 1));
        assertFalse(BoundedCache.key(0, -1) == BoundedCache.key(-1, -1));
    }
}
