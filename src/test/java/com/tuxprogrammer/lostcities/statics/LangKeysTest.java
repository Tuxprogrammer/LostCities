package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.tuxprogrammer.lostcities.config.LostCitiesConfig;
import com.tuxprogrammer.lostcities.core.LangFile;

/**
 * Every player-visible string is a lang key (CONTRIBUTING § Assets and localization). Two rules over the compiled main
 * classes:
 * <ol>
 * <li>every lang key in code is defined in {@code en_US.lang}. A key is a string constant (an {@code ldc}, or a
 * {@code static final} field's value) of the form {@code lostcitiesnh.<area>.<name>[...]}: two or more lowercase
 * segments after the mod ID. Identifiers that share the prefix, such as JVM properties, use camelCase or a single
 * segment ({@code lostcitiesnh.testHooks}), so they are not mistaken for keys;</li>
 * <li>no code builds a {@code ChatComponentText}, vanilla's untranslated chat text: chat goes out as
 * {@code ChatComponentTranslation} with a key.</li>
 * </ol>
 * The config screen's keys, which GTNHLib derives from the config fields, are checked against the lang file here too.
 */
public class LangKeysTest {

    static final Pattern KEY = Pattern.compile("lostcitiesnh(\\.[a-z0-9_]+){2,}");

    static final String PLAIN_TEXT_CHAT = "net/minecraft/util/ChatComponentText";

    @Test
    public void everyKeyInCodeIsInTheLangFile() throws Exception {
        List<String> missing = missing(MainClasses.all(), definedKeys());
        assertTrue(missing.isEmpty(), "lang keys used in code but missing from en_US.lang:\n" + String.join("\n", missing));
    }

    @Test
    public void everyConfigScreenKeyIsInTheLangFile() throws Exception {
        Set<String> defined = definedKeys();
        List<String> missing = new ArrayList<>();
        for (String key : LostCitiesConfig.langKeys()) {
            if (!defined.contains(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "config screen keys missing from en_US.lang:\n" + String.join("\n", missing));
    }

    @Test
    public void noCodeSendsUntranslatedChat() throws Exception {
        List<String> offenders = plainTextChat(MainClasses.all());
        assertTrue(offenders.isEmpty(), "ChatComponentText (hard-coded text) built in:\n" + String.join("\n", offenders));
    }

    @Test
    public void fixtureWithAnUndefinedKeyFails() throws Exception {
        ClassNode fixture = MainClasses.fixture("LangKeyUser");
        assertEquals(
            Collections.singletonList("lostcitiesnh.fixture.missing_key (com.tuxprogrammer.lostcities.statics.fixtures.LangKeyUser)"),
            missing(Collections.singletonList(fixture), new HashSet<>()));
    }

    @Test
    public void fixtureSendingPlainTextFails() throws Exception {
        ClassNode fixture = MainClasses.fixture("PlainTextChat");
        assertEquals(
            Collections.singletonList("com.tuxprogrammer.lostcities.statics.fixtures.PlainTextChat"),
            plainTextChat(Collections.singletonList(fixture)));
    }

    @Test
    public void identifiersThatShareThePrefixAreNotKeys() {
        assertTrue(!KEY.matcher("lostcitiesnh.testHooks")
            .matches(), "camelCase JVM property");
        assertTrue(!KEY.matcher("lostcitiesnh")
            .matches(), "the mod ID");
        assertTrue(KEY.matcher("lostcitiesnh.command.usage")
            .matches(), "a real key");
    }

    @Test
    public void jarLangFileIsTheSourceFile() throws Exception {
        try (InputStream in = LangKeysTest.class.getResourceAsStream(LangFile.EN_US)) {
            assertTrue(in != null, LangFile.EN_US + " is on the classpath");
            assertEquals(definedKeys(), LangFile.keys(in));
        }
    }

    static List<String> missing(List<ClassNode> classes, Set<String> defined) {
        Set<String> out = new TreeSet<>();
        for (ClassNode node : classes) {
            for (String constant : constants(node)) {
                if (KEY.matcher(constant)
                    .matches() && !defined.contains(constant)) {
                    out.add(constant + " (" + node.name.replace('/', '.') + ")");
                }
            }
        }
        return new ArrayList<>(out);
    }

    static List<String> plainTextChat(List<ClassNode> classes) {
        List<String> out = new ArrayList<>();
        for (ClassNode node : classes) {
            boolean builds = false;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof TypeInsnNode && PLAIN_TEXT_CHAT.equals(((TypeInsnNode) insn).desc)) {
                        builds = true;
                    }
                }
            }
            if (builds) {
                out.add(node.name.replace('/', '.'));
            }
        }
        return out;
    }

    static Set<String> constants(ClassNode node) {
        Set<String> out = new HashSet<>();
        for (FieldNode field : node.fields) {
            if (field.value instanceof String) {
                out.add((String) field.value);
            }
        }
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                    out.add((String) ((LdcInsnNode) insn).cst);
                }
            }
        }
        return out;
    }

    /** Returns the keys of {@code src/main/resources/assets/lostcitiesnh/lang/en_US.lang}. */
    static Set<String> definedKeys() throws IOException {
        try (InputStream in = Files.newInputStream(ProjectFiles.resolve("src/main/resources" + LangFile.EN_US))) {
            return LangFile.keys(in);
        }
    }
}
