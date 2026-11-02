package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.tuxprogrammer.lostcities.config.profile.ProfileJson;
import com.tuxprogrammer.lostcities.config.profile.ProfileOption;
import com.tuxprogrammer.lostcities.config.profile.ProfileOptions;
import com.tuxprogrammer.lostcities.core.LangFile;

/**
 * Upstream parts the port leaves out or replaces, checked over the compiled classes and the resources so they cannot
 * come back with their bugs: the unused player capability, the profile icons and the obsolete lang files
 * (docs/new-spec/01-core/index.md, "Lifecycle and registration"; docs/new-spec/09-client-ui/index.md, "Lang and
 * textures").
 */
public class UpstreamLeftoversTest {

    static final String EXTENDED_PROPERTIES = "net/minecraftforge/common/IExtendedEntityProperties";

    /** Returns the classes that define or attach per-entity extended properties, one line each. */
    static List<String> extendedProperties(List<ClassNode> classes) {
        List<String> offenders = new ArrayList<>();
        for (ClassNode node : classes) {
            if (node.interfaces.contains(EXTENDED_PROPERTIES)) {
                offenders.add(node.name + " implements IExtendedEntityProperties");
            }
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode
                        && "registerExtendedProperties".equals(((MethodInsnNode) insn).name)) {
                        offenders.add(node.name + "." + method.name + " registers extended properties");
                    }
                }
            }
        }
        return offenders;
    }

    @Test
    public void doesNotReproduceCORE37_noPlayerDataIsAttachedToEveryPlayer() throws Exception {
        // The first-join mark is a flag in the player's persisted NBT, which Forge copies across death (D31).
        List<String> offenders = extendedProperties(MainClasses.all());
        assertTrue(offenders.isEmpty(), "upstream's unused capability is back:\n" + String.join("\n", offenders));
    }

    @Test
    public void fixtureAttachingPlayerDataFails() throws Exception {
        assertEquals(
            2,
            extendedProperties(Collections.singletonList(MainClasses.fixture("PlayerCapability"))).size(),
            "the fixture both implements and registers extended properties");
    }

    @Test
    public void doesNotReproduceUI10_noProfileIconShipsAndTheIconOptionIsIgnored() throws IOException {
        List<String> images;
        try (Stream<Path> files = Files.walk(ProjectFiles.resolve("src/main/resources"))) {
            images = files.map(ProjectFiles::relative)
                .filter(
                    name -> name.toLowerCase(Locale.ROOT)
                        .endsWith(".png"))
                .collect(Collectors.toList());
        }
        assertTrue(images.isEmpty(), "no image ships in the jar (0001 §5): " + images);
        assertEquals(
            ProfileOption.Status.DROPPED,
            ProfileOptions.get("icon")
                .status());
        ProfileJson.Load load = ProfileJson
            .read("iconic", "iconic.json", "{ \"lostcity\": { \"icon\": \"textures/gui/icon_iconic.png\" } }");
        assertNotNull(load.profile(), load.errors()::toString);
        assertTrue(
            load.ignored()
                .contains("icon"),
            "an upstream file with an icon still loads, and the icon is ignored");
    }

    @Test
    public void doesNotReproduceUI11_everyStringIsAKeyInTheLangFileThatThisGameVersionLoads() throws Exception {
        // 1.7.10 reads assets/<domain>/lang/<locale>.lang; upstream shipped JSON lang files and five .lang files
        // that 1.20 never loaded, and hard-coded its UI text.
        assertTrue(
            Files.isRegularFile(ProjectFiles.resolve("src/main/resources" + LangFile.EN_US)),
            LangFile.EN_US);
        Set<String> defined = LangKeysTest.definedKeys();
        for (String key : defined) {
            assertFalse(key.startsWith("generator."), key + " names a world type this mod does not add");
        }
        List<String> missing = LangKeysTest.missing(MainClasses.all(), defined);
        assertTrue(missing.isEmpty(), "keys used in code but missing from en_US.lang:\n" + String.join("\n", missing));
        List<String> plain = LangKeysTest.plainTextChat(MainClasses.all());
        assertTrue(plain.isEmpty(), "hard-coded chat text in:\n" + String.join("\n", plain));
    }
}
