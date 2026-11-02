package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * {@link SpawnSelection}'s server glue (docs/new-spec/01-core/index.md, "Spawn selection"). The spawn search itself is
 * pure and tested in {@code SpawnSearchTest}; the handlers need a live world, so these tests read their compiled code
 * and check the saved data they keep.
 */
public class SpawnSelectionTest {

    /** Every method name that sets a world spawn or the chosen spawn. */
    private static final String[] SPAWN_WRITERS = { "setSpawnPosition", "setSpawnLocation", "setSpawn" };

    private static ClassNode spawnSelection() throws IOException {
        try (InputStream in = SpawnSelection.class.getClassLoader()
            .getResourceAsStream(
                SpawnSelection.class.getName()
                    .replace('.', '/') + ".class")) {
            assertNotNull(in, "SpawnSelection's class file is on the test classpath");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            ClassNode node = new ClassNode();
            new ClassReader(out.toByteArray()).accept(node, ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) {
                return method;
            }
        }
        throw new AssertionError("SpawnSelection has no method " + name);
    }

    /** Returns the names of the methods a method calls, in order. */
    private static List<String> calls(MethodNode method) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                out.add(((MethodInsnNode) insn).name);
            }
        }
        return out;
    }

    private static boolean loads(MethodNode method, String constant) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LdcInsnNode && constant.equals(((LdcInsnNode) insn).cst)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void doesNotReproduceCORE11_theLoginHandlerNeverRewritesASpawnAndPlacesOnlyOnce() throws IOException {
        ClassNode node = spawnSelection();
        for (MethodNode method : node.methods) {
            if (method.name.equals("onCreateSpawnPosition")) {
                continue;
            }
            for (String writer : SPAWN_WRITERS) {
                assertFalse(
                    calls(method).contains(writer),
                    method.name + " calls " + writer + ": only a new world's spawn event may choose a spawn");
            }
        }
        MethodNode login = method(node, "onPlayerLoggedIn");
        List<String> calls = calls(login);
        assertTrue(loads(login, SpawnSelection.PLACED_KEY), "the login handler reads the player's placed flag");
        assertTrue(calls.contains("getBoolean"), "a player already placed is left alone");
        assertTrue(calls.contains("setBoolean"), "placing a player marks them as placed");
        assertTrue(
            calls.indexOf("getBoolean") < calls.indexOf("setPlayerLocation"),
            "the flag is checked before the player is moved");
    }

    @Test
    public void doesNotReproduceCORE12_theChosenSpawnBelongsToItsWorldsSavedData() {
        for (Field field : SpawnSelection.class.getDeclaredFields()) {
            if (field.isSynthetic()) {
                // A coverage tool's probes, not the class's own state.
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                assertTrue(Modifier.isFinal(field.getModifiers()), field.getName() + " is mutable static state");
                assertFalse(
                    Map.class.isAssignableFrom(field.getType()) || Collection.class.isAssignableFrom(field.getType()),
                    field.getName() + " would keep spawns past the world");
            } else {
                throw new AssertionError("SpawnSelection keeps state in " + field.getName());
            }
        }
        ProfileSavedData first = new ProfileSavedData(ProfileSavedData.NAME);
        first.setSpawn(0, 120, 72, -40);
        NBTTagCompound saved = new NBTTagCompound();
        first.writeToNBT(saved);
        ProfileSavedData reloaded = new ProfileSavedData(ProfileSavedData.NAME);
        reloaded.readFromNBT(saved);
        assertArrayEquals(new int[] { 120, 72, -40 }, reloaded.spawn(0), "the spot survives a reload of its world");
        ProfileSavedData secondWorld = new ProfileSavedData(ProfileSavedData.NAME);
        secondWorld.readFromNBT(new NBTTagCompound());
        assertNull(secondWorld.spawn(0), "another world starts with no chosen spawn");
    }

    @Test
    public void doesNotReproduceCORE17_aCanceledSpawnEventStillPlacesTheBonusChestAsVanillaDoes() throws IOException {
        ClassNode node = spawnSelection();
        List<String> spawn = calls(method(node, "onCreateSpawnPosition"));
        assertTrue(spawn.contains("setCanceled"));
        assertTrue(spawn.contains("isBonusChestEnabled"), "the world's bonus-chest setting is honored");
        assertTrue(spawn.contains("placeBonusChest"));
        MethodNode chest = method(node, "placeBonusChest");
        boolean generator = false;
        for (AbstractInsnNode insn = chest.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.NEW && insn instanceof TypeInsnNode
                && "net/minecraft/world/gen/feature/WorldGeneratorBonusChest".equals(((TypeInsnNode) insn).desc)) {
                generator = true;
            }
        }
        assertTrue(generator, "vanilla's bonus-chest generator");
        assertTrue(loads(chest, "bonusChest"), "filled from ChestGenHooks' bonus-chest category");
        // Vanilla 1.7.10's WorldServer.createBonusChest: ten tries at spawn + nextInt(6) - nextInt(6).
        assertEquals(10, SpawnSelection.BONUS_TRIES);
        assertEquals(6, SpawnSelection.BONUS_SPREAD);
    }
}
