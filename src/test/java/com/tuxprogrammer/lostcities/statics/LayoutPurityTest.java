package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The layout and the asset model are pure Java (CONTRIBUTING § Architecture, decision log D6): no class in
 * {@code com.tuxprogrammer.lostcities.asset} or {@code com.tuxprogrammer.lostcities.layout} may refer to a Minecraft,
 * Forge, FML, LWJGL or other mod's type, so they take seeds, coordinates, profiles and assets and return plain data.
 *
 * <p>
 * Allowed references: the Java platform ({@code java.*}, {@code javax.annotation.*}), Gson for asset JSON,
 * nullness annotations, and the pure packages themselves. Anything else, including the mod's own glue packages, is a
 * violation, since glue may hold Minecraft types.
 */
public class LayoutPurityTest {

    /** The pure packages, as internal-name prefixes. */
    static final List<String> PURE_PACKAGES = Collections
        .unmodifiableList(Arrays.asList(MainClasses.ROOT + "asset/", MainClasses.ROOT + "layout/"));

    /** Prefixes that pure classes may refer to, besides the pure packages. */
    static final List<String> ALLOWED = Collections.unmodifiableList(
        Arrays.asList("java/", "javax/annotation/", "com/google/gson/", "org/jetbrains/annotations/"));

    @Test
    public void pureClassesReferOnlyToPureTypes() throws Exception {
        List<ClassNode> pure = new ArrayList<>();
        for (ClassNode node : MainClasses.all()) {
            if (isPure(node.name)) {
                pure.add(node);
            }
        }
        assertTrue(!pure.isEmpty(), "no class found in the pure packages, so the check would pass vacuously");
        List<String> violations = violations(pure);
        assertTrue(
            violations.isEmpty(),
            "impure references in asset/layout (decision log D6):\n" + String.join("\n", violations));
    }

    @Test
    public void fixtureThatUsesAWorldFailsTheCheck() throws Exception {
        ClassNode impure = MainClasses.fixture("ImpureLayout");
        assertEquals(
            Collections.singletonList(
                "com.tuxprogrammer.lostcities.statics.fixtures.ImpureLayout -> net.minecraft.world.World"),
            violations(Collections.singletonList(impure)));
    }

    @Test
    public void fixtureThatUsesOnlyJavaPasses() throws Exception {
        ClassNode pure = MainClasses.fixture("PureLayout");
        assertEquals(Collections.emptyList(), violations(Collections.singletonList(pure)));
    }

    /** Returns one line per class and forbidden type it refers to. */
    static List<String> violations(List<ClassNode> classes) {
        List<String> out = new ArrayList<>();
        for (ClassNode node : classes) {
            Set<String> refs = new TreeSet<>(TypeRefs.ofHeader(node));
            for (FieldNode field : node.fields) {
                refs.addAll(TypeRefs.of(field));
            }
            for (MethodNode method : node.methods) {
                refs.addAll(TypeRefs.of(method));
            }
            String outer = node.name.contains("$") ? node.name.substring(0, node.name.indexOf('$')) : node.name;
            for (String ref : refs) {
                // A class may always name itself and its own nested classes.
                boolean self = ref.equals(outer) || ref.startsWith(outer + "$");
                if (!self && !isAllowed(ref)) {
                    out.add(node.name.replace('/', '.') + " -> " + ref.replace('/', '.'));
                }
            }
        }
        return out;
    }

    private static boolean isPure(String internalName) {
        for (String prefix : PURE_PACKAGES) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowed(String ref) {
        if (isPure(ref)) {
            return true;
        }
        for (String prefix : ALLOWED) {
            if (ref.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
