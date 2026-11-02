package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * No main class writes to the standard streams: every message goes through the mod logger (CONTRIBUTING § Code
 * standards, "Never use {@code System.out}"). It reads the compiled classes and fails on any use of
 * {@code System.out}, {@code System.err} or {@code printStackTrace()}, the last of which prints to standard error.
 *
 * <p>
 * This is the regression test for TERR-33: upstream's generation error path printed every failure to standard error
 * as well as logging it, behind a throttle kept in an unsynchronized static. Here a failed chunk is logged once with
 * the mod logger and counted for the timers command (docs/new-spec/03-terrain/index.md, "Exceptions during
 * generation", P03-12, D31); generation runs on the server thread, so there is no throttle to share.
 */
public class StandardStreamsTest {

    @Test
    public void doesNotReproduceTERR33_noMainClassPrintsToTheStandardStreams() throws Exception {
        List<String> violations = violations(MainClasses.all());
        assertTrue(
            violations.isEmpty(),
            "standard stream use in main classes; log with LostCities.LOG instead:\n" + String.join("\n", violations));
    }

    @Test
    public void fixtureReadingSystemErrFails() {
        ClassNode fixture = fixture("PrintsToErr");
        MethodNode method = method(fixture);
        method.instructions
            .add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;"));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        assertEquals(
            Collections.singletonList("fixtures.PrintsToErr -> java.lang.System.err"),
            violations(Collections.singletonList(fixture)));
    }

    @Test
    public void fixturePrintingAStackTraceFails() {
        ClassNode fixture = fixture("PrintsStackTrace");
        MethodNode method = method(fixture);
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions
            .add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        assertEquals(
            Collections.singletonList("fixtures.PrintsStackTrace -> java.lang.Throwable#printStackTrace()V"),
            violations(Collections.singletonList(fixture)));
    }

    @Test
    public void fixtureUsingOtherSystemMembersPasses() {
        ClassNode fixture = fixture("UsesSystemNanoTime");
        MethodNode method = method(fixture);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
        method.instructions.add(new InsnNode(Opcodes.POP2));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        assertEquals(Collections.emptyList(), violations(Collections.singletonList(fixture)));
    }

    /** Returns one line per class and standard-stream member it uses. */
    static List<String> violations(List<ClassNode> classes) {
        List<String> out = new ArrayList<>();
        for (ClassNode node : classes) {
            String where = node.name.replace('/', '.');
            for (MethodNode method : node.methods) {
                for (TypeRefs.MemberRef ref : TypeRefs.members(method)) {
                    if (isStandardStream(ref)) {
                        String line = where + " -> " + ref;
                        if (!out.contains(line)) {
                            out.add(line);
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean isStandardStream(TypeRefs.MemberRef ref) {
        if (ref.field) {
            return "java/lang/System".equals(ref.owner) && ("out".equals(ref.name) || "err".equals(ref.name));
        }
        return "printStackTrace".equals(ref.name) && "()V".equals(ref.desc);
    }

    /** Returns an empty synthetic class {@code fixtures/<name>}, built in memory. */
    private static ClassNode fixture(String name) {
        ClassNode node = new ClassNode();
        node.version = Java8ApiTest.JAVA_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "fixtures/" + name;
        node.superName = "java/lang/Object";
        return node;
    }

    /** Adds an empty method to the fixture and returns it. */
    private static MethodNode method(ClassNode node) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "m" + node.methods.size(), "()V", null, null);
        node.methods.add(method);
        return method;
    }
}
