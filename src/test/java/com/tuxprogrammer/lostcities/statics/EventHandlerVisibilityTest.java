package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Every {@code @SubscribeEvent} method, and the class that declares it, is public. FML generates each handler's
 * caller in another package, so a package-private handler compiles, boots, and throws {@code IllegalAccessError} the
 * first time its event fires (testing-gtnh-mods, layer 2).
 */
public class EventHandlerVisibilityTest {

    static final String SUBSCRIBE = "Lcpw/mods/fml/common/eventhandler/SubscribeEvent;";

    @Test
    public void everyEventHandlerIsPublic() throws Exception {
        List<String> violations = violations(MainClasses.all());
        assertTrue(violations.isEmpty(), "event handlers FML cannot call:\n" + String.join("\n", violations));
    }

    @Test
    public void fixtureWithAPackagePrivateHandlerFails() throws Exception {
        assertEquals(
            Collections.singletonList("com.tuxprogrammer.lostcities.statics.fixtures.HiddenHandler.onTick"),
            violations(Collections.singletonList(MainClasses.fixture("HiddenHandler"))));
    }

    static List<String> violations(List<ClassNode> classes) {
        List<String> out = new ArrayList<>();
        for (ClassNode node : classes) {
            boolean classPublic = (node.access & Opcodes.ACC_PUBLIC) != 0;
            for (MethodNode method : node.methods) {
                if (!subscribes(method)) {
                    continue;
                }
                boolean methodPublic = (method.access & Opcodes.ACC_PUBLIC) != 0;
                if (!classPublic || !methodPublic) {
                    out.add(node.name.replace('/', '.') + "." + method.name);
                }
            }
        }
        return out;
    }

    private static boolean subscribes(MethodNode method) {
        if (method.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (SUBSCRIBE.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }
}
