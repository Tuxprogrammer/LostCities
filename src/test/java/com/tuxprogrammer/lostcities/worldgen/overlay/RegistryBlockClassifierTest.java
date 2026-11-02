package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;

/**
 * The registry rules that replace upstream's {@code easybreakable} and {@code lights} tags
 * (docs/new-spec/08-loot-mobs-blocks/index.md, "Block tags"), over blocks made here: a block is judged by its own
 * material and light value, whatever mod it comes from. The world is not needed, so nothing is registered.
 */
public class RegistryBlockClassifierTest {

    /** A block of a material with no light, as a pack mod would register it. */
    private static Block block(Material material) {
        return new Block(material) {};
    }

    /** A block that emits light, as a pack lamp or one of 1.7.10's separate lit blocks does. */
    private static Block light(Material material, int value) {
        return new Block(material) {

            @Override
            public int getLightValue() {
                return value;
            }
        };
    }

    /**
     * LOOT-07: upstream shipped an {@code easybreakable} file that disagreed with its own generator. Here the rule is
     * the only source: every glass-material block, a pack glass included, takes the ×2.5 explosion damage, and no
     * other material does.
     */
    @Test
    public void doesNotReproduceLOOT07_everyGlassMaterialBlockIsEasilyBroken() {
        assertEquals(
            BlockClassifier.EASY_BREAKABLE,
            RegistryBlockClassifier.propertyFlags(block(Material.glass)) & BlockClassifier.EASY_BREAKABLE,
            "a pack glass block");
        assertEquals(
            BlockClassifier.EASY_BREAKABLE,
            RegistryBlockClassifier.propertyFlags(light(Material.glass, 15)) & BlockClassifier.EASY_BREAKABLE,
            "a glowing glass, as glowstone and Botania's sea lamp are");
        for (Material material : new Material[] { Material.rock, Material.wood, Material.iron, Material.clay,
            Material.redstoneLight }) {
            assertEquals(
                0,
                RegistryBlockClassifier.propertyFlags(block(material)) & BlockClassifier.EASY_BREAKABLE,
                "not glass: " + material);
        }
    }

    /**
     * LOOT-07, the test classifier's side: the vanilla names the unit and golden tests use are exactly the glass
     * blocks of vanilla 1.7.10 the rule gives (glass, stained glass, both panes, glowstone and the beacon), so the two
     * classifiers cannot drift apart for vanilla blocks.
     */
    @Test
    public void doesNotReproduceLOOT07_theVanillaListMatchesTheGlassRule() {
        for (String name : new String[] { "minecraft:glass", "minecraft:stained_glass", "minecraft:glass_pane",
            "minecraft:stained_glass_pane", "minecraft:glowstone", "minecraft:beacon" }) {
            assertTrue(
                (NameBlockClassifier.classify(BlockRef.of(name, 0)) & BlockClassifier.EASY_BREAKABLE) != 0,
                name);
        }
        for (String name : new String[] { "minecraft:stone", "minecraft:ice", "minecraft:redstone_lamp",
            "minecraft:quartz_block", "minecraft:iron_bars" }) {
            assertEquals(0, NameBlockClassifier.classify(BlockRef.of(name, 0)) & BlockClassifier.EASY_BREAKABLE, name);
        }
    }

    /**
     * LOOT-08: upstream's {@code lights} tag was built once from vanilla blocks' default states, so modded lights and
     * conditional ones (a lit furnace, a lit redstone lamp) were missing. Here any block with a light value counts,
     * whatever its mod or material, and a block without one does not.
     */
    @Test
    public void doesNotReproduceLOOT08_everyBlockThatEmitsLightIsALight() {
        assertEquals(
            BlockClassifier.LIGHT,
            RegistryBlockClassifier.propertyFlags(light(Material.redstoneLight, 15)) & BlockClassifier.LIGHT,
            "a modded lamp");
        assertEquals(
            BlockClassifier.LIGHT,
            RegistryBlockClassifier.propertyFlags(light(Material.rock, 13)) & BlockClassifier.LIGHT,
            "a lit furnace, its own block in 1.7.10");
        assertEquals(
            BlockClassifier.LIGHT,
            RegistryBlockClassifier.propertyFlags(light(Material.circuits, 1)) & BlockClassifier.LIGHT,
            "a dim light still counts");
        assertEquals(
            0,
            RegistryBlockClassifier.propertyFlags(block(Material.redstoneLight)) & BlockClassifier.LIGHT,
            "an unlit lamp");
        assertTrue(
            (NameBlockClassifier.classify(BlockRef.of("minecraft:lit_furnace", 0)) & BlockClassifier.LIGHT) != 0,
            "the vanilla list keeps the lit furnace");
        assertTrue(
            (NameBlockClassifier.classify(BlockRef.of("minecraft:lit_redstone_lamp", 0)) & BlockClassifier.LIGHT) != 0,
            "and the lit redstone lamp");
    }
}
