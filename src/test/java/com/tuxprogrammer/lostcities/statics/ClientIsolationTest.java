package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A dedicated server never loads a client class (CONTRIBUTING § Architecture, networking-side-safety). No compiled
 * main class outside {@code com.tuxprogrammer.lostcities.client} may refer to:
 * <ul>
 * <li>{@code net.minecraft.client.*} or {@code org.lwjgl.*};</li>
 * <li>the mod's own {@code client} package: common code reaches it only through the proxy, which FML instantiates by
 * name;</li>
 * <li>any class on the classpath annotated {@code @SideOnly(Side.CLIENT)}, such as vanilla's {@code MouseHelper} or a
 * library's client-only class.</li>
 * </ul>
 * References inside a field or method annotated {@code @SideOnly(Side.CLIENT)}, which FML strips on the server, are
 * allowed, and so are lambdas declared in such a method. A class outside the client package must not itself be
 * {@code @SideOnly(Side.CLIENT)}: it belongs in the client package.
 *
 * <p>
 * Calls to client-only members of common classes are not checked here; the {@code runServer} boot catches those.
 */
public class ClientIsolationTest {

    private static final String CLIENT_PACKAGE = MainClasses.ROOT + "client/";

    private static final Map<String, Boolean> CLIENT_ONLY_CACHE = new HashMap<>();

    @Test
    public void commonCodeReferencesNoClientClass() throws Exception {
        List<String> violations = violations(MainClasses.all());
        assertTrue(
            violations.isEmpty(),
            "client classes referenced outside com.tuxprogrammer.lostcities.client:\n" + String.join("\n", violations));
    }

    @Test
    public void fixtureThatUsesAClientClassFailsTheCheck() throws Exception {
        ClassNode bad = MainClasses.fixture("UsesClientClass");
        assertEquals(
            Collections.singletonList(
                "com.tuxprogrammer.lostcities.statics.fixtures.UsesClientClass.tick()V -> net.minecraft.client.Minecraft"),
            violations(Collections.singletonList(bad)));
    }

    @Test
    public void fixtureWithAClientOnlyMemberPasses() throws Exception {
        ClassNode ok = MainClasses.fixture("SideOnlyClientMember");
        assertEquals(Collections.emptyList(), violations(Collections.singletonList(ok)));
    }

    @Test
    public void vanillaSideOnlyClassIsRecognized() throws Exception {
        assertTrue(isClientOnlyClass("net/minecraft/util/MouseHelper"), "MouseHelper is @SideOnly(CLIENT)");
        assertFalse(isClientOnlyClass("net/minecraft/block/Block"), "Block is common");
    }

    static List<String> violations(List<ClassNode> classes) throws IOException {
        List<String> out = new ArrayList<>();
        for (ClassNode node : classes) {
            if (node.name.startsWith(CLIENT_PACKAGE)) {
                continue;
            }
            String owner = node.name.replace('/', '.');
            if (TypeRefs.isClientOnly(node.visibleAnnotations) || TypeRefs.isClientOnly(node.invisibleAnnotations)) {
                out.add(owner + " is @SideOnly(CLIENT) but outside the client package");
                continue;
            }
            report(owner, TypeRefs.ofHeader(node), out);
            for (FieldNode field : node.fields) {
                if (!TypeRefs.isClientOnly(field.visibleAnnotations)) {
                    report(owner + "." + field.name, TypeRefs.of(field), out);
                }
            }
            List<String> exempt = new ArrayList<>();
            for (MethodNode method : node.methods) {
                if (TypeRefs.isClientOnly(method.visibleAnnotations)) {
                    exempt.add(method.name);
                }
            }
            for (MethodNode method : node.methods) {
                if (TypeRefs.isClientOnly(method.visibleAnnotations) || isLambdaOf(method.name, exempt)) {
                    continue;
                }
                report(owner + "." + method.name + method.desc, TypeRefs.of(method), out);
            }
        }
        return out;
    }

    private static void report(String where, Set<String> refs, List<String> out) throws IOException {
        for (String ref : refs) {
            if (isForbidden(ref)) {
                out.add(where + " -> " + ref.replace('/', '.'));
            }
        }
    }

    private static boolean isLambdaOf(String name, List<String> exempt) {
        if (!name.startsWith("lambda$")) {
            return false;
        }
        for (String method : exempt) {
            if (name.startsWith("lambda$" + method + "$")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isForbidden(String ref) throws IOException {
        if (ref.startsWith("net/minecraft/client/") || ref.startsWith("org/lwjgl/") || ref.startsWith(CLIENT_PACKAGE)) {
            return true;
        }
        if (ref.startsWith("java/") || ref.startsWith("javax/")) {
            return false;
        }
        return isClientOnlyClass(ref);
    }

    static synchronized boolean isClientOnlyClass(String internalName) throws IOException {
        Boolean cached = CLIENT_ONLY_CACHE.get(internalName);
        if (cached != null) {
            return cached;
        }
        ClassNode node = MainClasses.fromClasspath(internalName);
        boolean clientOnly = node != null
            && (TypeRefs.isClientOnly(node.visibleAnnotations) || TypeRefs.isClientOnly(node.invisibleAnnotations));
        CLIENT_ONLY_CACHE.put(internalName, clientOnly);
        return clientOnly;
    }
}
