package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The mod runs on the Java 8 API (CLAUDE.md non-negotiable 8; CONTRIBUTING § Toolchain and build): Jabel allows modern
 * syntax, but the game runs on Java 8, where records, {@code List.of}, {@code String.isBlank} and every other newer
 * API do not exist. The buildscript already compiles with {@code --release 8}; this check reads the compiled classes
 * and fails if anything got past that, for instance after a toolchain change.
 *
 * <p>
 * For every main class it checks that the class-file version is at most 52 (Java 8), that every {@code java.*} class
 * (and JDK {@code javax.*} class) it names exists in Java 8, and that every field and method it uses on such a class
 * exists in Java 8 on that class or a supertype. Members reached through the mod's own classes (an inherited
 * {@code getMessage()} called on our exception) are not resolved; javac's {@code --release 8} covers those.
 */
public class Java8ApiTest {

    /** Class-file major version of Java 8. */
    static final int JAVA_8 = 52;

    private static Java8Api api;

    @BeforeAll
    public static void loadApi() throws IOException {
        api = Java8Api.load();
    }

    @Test
    public void mainClassesUseOnlyTheJava8Api() throws Exception {
        List<String> violations = violations(MainClasses.all());
        assertTrue(violations.isEmpty(), "API newer than Java 8 in main classes:\n" + String.join("\n", violations));
    }

    @Test
    public void fixtureCallingListOfFails() throws Exception {
        ClassNode fixture = fixture("UsesListOf", JAVA_8);
        call(fixture, Opcodes.INVOKESTATIC, "java/util/List", "of", "()Ljava/util/List;", true);
        assertEquals(
            Collections.singletonList("fixtures.UsesListOf -> java.util.List#of()Ljava/util/List;"),
            violations(Collections.singletonList(fixture)));
    }

    @Test
    public void fixtureCallingStringIsBlankFails() throws Exception {
        ClassNode fixture = fixture("UsesIsBlank", JAVA_8);
        call(fixture, Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", false);
        assertEquals(
            Collections.singletonList("fixtures.UsesIsBlank -> java.lang.String#isBlank()Z"),
            violations(Collections.singletonList(fixture)));
    }

    @Test
    public void fixtureExtendingRecordFails() throws Exception {
        ClassNode fixture = fixture("IsARecord", JAVA_8);
        fixture.superName = "java/lang/Record";
        assertEquals(
            Collections.singletonList("fixtures.IsARecord -> class java.lang.Record is not in Java 8"),
            violations(Collections.singletonList(fixture)));
    }

    @Test
    public void fixtureWithJava17BytecodeFails() throws Exception {
        ClassNode fixture = fixture("NewBytecode", 61);
        assertEquals(
            Collections.singletonList("fixtures.NewBytecode -> class-file version 61, above Java 8's 52"),
            violations(Collections.singletonList(fixture)));
    }

    @Test
    public void fixtureUsingJava8ApiPasses() throws Exception {
        ClassNode fixture = fixture("UsesJava8", JAVA_8);
        // An inherited default method, a method an interface gets from Object, and plain Java 8 members.
        call(fixture, Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "stream", "()Ljava/util/stream/Stream;", false);
        call(fixture, Opcodes.INVOKEINTERFACE, "java/util/List", "hashCode", "()I", true);
        call(fixture, Opcodes.INVOKEVIRTUAL, "java/lang/String", "isEmpty", "()Z", false);
        call(
            fixture,
            Opcodes.INVOKESTATIC,
            "java/util/Objects",
            "requireNonNull",
            "(Ljava/lang/Object;)Ljava/lang/Object;",
            false);
        fixture.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "out", "Ljava/util/Optional;", null, null));
        assertEquals(Collections.emptyList(), violations(Collections.singletonList(fixture)));
    }

    /** Returns one line per class and newer-than-Java-8 thing it uses. */
    static List<String> violations(List<ClassNode> classes) throws IOException {
        List<String> out = new ArrayList<>();
        for (ClassNode node : classes) {
            String where = node.name.replace('/', '.');
            int major = node.version & 0xFFFF;
            if (major > JAVA_8) {
                out.add(where + " -> class-file version " + major + ", above Java 8's " + JAVA_8);
            }
            Set<String> types = new TreeSet<>(TypeRefs.ofHeader(node));
            for (FieldNode field : node.fields) {
                types.addAll(TypeRefs.of(field));
            }
            Set<String> members = new TreeSet<>();
            for (MethodNode method : node.methods) {
                types.addAll(TypeRefs.of(method));
                for (TypeRefs.MemberRef ref : TypeRefs.members(method)) {
                    if (api.isPlatformClass(ref.owner) && api.hasClass(ref.owner) && !hasMember(ref)) {
                        members.add(where + " -> " + ref);
                    }
                }
            }
            for (String type : types) {
                if (api.isPlatformClass(type) && !api.hasClass(type)) {
                    out.add(where + " -> class " + type.replace('/', '.') + " is not in Java 8");
                }
            }
            out.addAll(members);
        }
        return out;
    }

    private static boolean hasMember(TypeRefs.MemberRef ref) throws IOException {
        return ref.field ? api.hasField(ref.owner, ref.name, ref.desc) : api.hasMethod(ref.owner, ref.name, ref.desc);
    }

    /** Returns an empty synthetic class {@code fixtures/<name>}; built in memory, since javac would refuse it. */
    private static ClassNode fixture(String name, int version) {
        ClassNode node = new ClassNode();
        node.version = version;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "fixtures/" + name;
        node.superName = "java/lang/Object";
        return node;
    }

    /** Adds a method to the fixture whose only work is one call. */
    private static void call(ClassNode node, int opcode, String owner, String name, String desc, boolean itf) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "m" + node.methods.size(), "()V", null, null);
        method.instructions.add(new MethodInsnNode(opcode, owner, name, desc, itf));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
    }
}
