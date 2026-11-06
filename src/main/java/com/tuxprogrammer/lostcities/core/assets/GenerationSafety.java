package com.tuxprogrammer.lostcities.core.assets;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.common.registry.GameData;

/**
 * Finds blocks that are unsafe to write while a chunk is being generated: blocks whose class (a mod's, not vanilla's)
 * overrides Forge's world-aware {@code getLightOpacity(IBlockAccess, int, int, int)}. The chunk's sky light map is
 * built
 * before the chunk is registered with the chunk provider ({@code Chunk.generateSkylightMap}), and it asks every block
 * for its opacity with the world; an override that reads the world at its own position asks the provider for the chunk
 * being generated, which generates it again, without end. Thaumcraft's {@code BlockCosmeticOpaque} does (it reads its
 * metadata through the world), and Automagy's glowing amber extends it: placing it crashed a real-pack server with a
 * stack overflow in M11.
 *
 * <p>
 * An override does not always read the world, so a flagged block is a candidate to inspect, not proof; the
 * {@code assets.catalog} scenario fails on any, so each one is either removed from the assets or recorded here as
 * checked. Decompiled: Thaumcraft 4.2.3.5, {@code thaumcraft.common.blocks.BlockCosmeticOpaque.getLightOpacity};
 * Automagy 0.29.8-GTNH, {@code tuhljin.automagy.blocks.BlockTranslucent} (javap); inferred from bytecode and the crash
 * report, and may be wrong in detail. Side: server.
 */
public final class GenerationSafety {

    /** Forge's world-aware light opacity, which keeps its name in a reobfuscated pack. */
    static final String OPACITY_NAME = "getLightOpacity";
    /** Its descriptor: {@code (IBlockAccess, int, int, int) -> int}. */
    static final String OPACITY_DESC = "(Lnet/minecraft/world/IBlockAccess;III)I";

    /**
     * Blocks whose override was read and found safe for what this mod places, by registry name, with the reason. A
     * flagged block not listed here fails {@code assets.catalog} until someone reads its override.
     *
     * <p>
     * Decompiled (javap, GTNH 2.9.0 jars; inferred from bytecode and may be wrong in detail):
     * <ul>
     * <li>Extra Utilities 1.2.12 {@code BlockDecoration.getLightOpacity} returns 0 when
     * {@code World.blockExists} is false, so it never loads a chunk;</li>
     * <li>MalisisDoors 1.19.13-GTNH {@code Door.getLightOpacity} reads the world only when
     * {@code blocksLightWhenClosed()}, true for curtains, laboratory and factory doors only; {@code TrapDoor} only for
     * sliding trapdoors. The wood and iron doors and the iron trapdoor this mod places take the vanilla path.</li>
     * </ul>
     */
    static final Map<String, String> CHECKED = checked();

    private GenerationSafety() {}

    private static Map<String, String> checked() {
        Map<String, String> out = new TreeMap<>();
        String xu = "guards the world read with World.blockExists";
        out.put("ExtraUtilities:decorativeBlock1", xu);
        out.put("ExtraUtilities:decorativeBlock2", xu);
        String door = "reads the world only for curtains, laboratory and factory doors";
        out.put("minecraft:iron_door", door);
        out.put("minecraft:wooden_door", door);
        for (String wood : new String[] { "acacia", "birch", "dark_oak", "jungle", "spruce" }) {
            out.put("malisisdoors:door_" + wood, door);
        }
        out.put("malisisdoors:iron_trapdoor", "reads the world only for sliding trapdoors");
        return Collections.unmodifiableMap(out);
    }

    /**
     * Returns the blocks, of the given {@code modid:name:meta} strings, whose class overrides the world-aware light
     * opacity and which {@link #CHECKED} does not clear, each as {@code name (class)}.
     *
     * @param blocks block references as {@code modid:name:meta} or {@code modid:name}
     * @return the flagged blocks, in input order
     */
    public static List<String> worldAwareOpacity(Collection<String> blocks) {
        List<String> out = new ArrayList<>();
        for (String text : blocks) {
            int last = text.lastIndexOf(':');
            String name = last > text.indexOf(':') ? text.substring(0, last) : text;
            Object found = GameData.getBlockRegistry()
                .getObject(name);
            if (!(found instanceof Block)) {
                continue;
            }
            if (CHECKED.containsKey(name)) {
                continue;
            }
            Class<?> declaring = overrider(found.getClass());
            if (declaring != null) {
                out.add(text + " (" + declaring.getName() + ")");
            }
        }
        return out;
    }

    /**
     * Returns whether a block class, a mod's, declares the world-aware light opacity anywhere below {@link Block}.
     *
     * @param type the block class
     * @return {@code true} when a class of its own overrides the lookup
     */
    public static boolean declaresWorldOpacity(Class<?> type) {
        return overrider(type) != null;
    }

    /**
     * Returns whether a block's world-aware opacity was read and found safe to write during generation
     * ({@link #CHECKED}).
     *
     * @param name the registry name
     * @return {@code true} when cleared
     */
    public static boolean isChecked(String name) {
        return CHECKED.containsKey(name);
    }

    /**
     * Returns the first class below {@link Block} that declares the world-aware opacity, or null. Reads each class's
     * bytes with ASM rather than reflection: reflecting a block's methods resolves their signatures, and many blocks
     * name client-only classes (icon registers) that a dedicated server cannot load.
     */
    static Class<?> overrider(Class<?> type) {
        for (Class<?> c = type; c != null && c != Block.class; c = c.getSuperclass()) {
            if (c.getName()
                .startsWith("net.minecraft.")) {
                continue;
            }
            if (declaresOpacity(c)) {
                return c;
            }
        }
        return null;
    }

    private static boolean declaresOpacity(Class<?> type) {
        ClassLoader loader = type.getClassLoader() != null ? type.getClassLoader()
            : GenerationSafety.class.getClassLoader();
        String resource = type.getName()
            .replace('.', '/') + ".class";
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                LostCities.LOG.warn("Cannot read {} to check its light opacity", resource);
                return false;
            }
            boolean[] found = { false };
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                    if (OPACITY_NAME.equals(name) && OPACITY_DESC.equals(desc)) {
                        found[0] = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return found[0];
        } catch (IOException e) {
            LostCities.LOG.warn("Cannot read {} to check its light opacity: {}", resource, e.toString());
            return false;
        }
    }
}
