package com.tuxprogrammer.lostcities.statics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Reads compiled classes with ASM, without loading them, for the static checks.
 *
 * <p>
 * The main classes are found in the test classpath's directory entries that hold {@code com/tuxprogrammer/lostcities}
 * and are not this source set's own output, so the checks see exactly what {@code compileJava} produced.
 */
final class MainClasses {

    /** Internal name of the mod's root package, with a trailing slash. */
    static final String ROOT = "com/tuxprogrammer/lostcities/";

    private static List<ClassNode> cache;

    private MainClasses() {}

    /** Returns every compiled main class of the mod, sorted by name. */
    static synchronized List<ClassNode> all() throws IOException {
        if (cache != null) {
            return cache;
        }
        Path testOutput = testOutput();
        List<ClassNode> nodes = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path")
            .split(File.pathSeparator)) {
            Path dir = Paths.get(entry)
                .toAbsolutePath();
            if (!Files.isDirectory(dir) || dir.equals(testOutput)) {
                continue;
            }
            Path pkg = dir.resolve(ROOT);
            if (!Files.isDirectory(pkg)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(pkg)) {
                for (Path file : files.filter(
                    p -> p.toString()
                        .endsWith(".class"))
                    .sorted()
                    .collect(Collectors.toList())) {
                    nodes.add(read(Files.readAllBytes(file)));
                }
            }
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException("no compiled main classes on the test classpath");
        }
        nodes.sort((a, b) -> a.name.compareTo(b.name));
        cache = Collections.unmodifiableList(nodes);
        return cache;
    }

    /**
     * Returns a class from the classpath by internal name, such as a test fixture or a game class.
     *
     * @param internalName for example {@code net/minecraft/block/Block}
     * @return the class, or {@code null} when it is not on the classpath
     */
    static ClassNode fromClasspath(String internalName) throws IOException {
        try (InputStream in = MainClasses.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            if (in == null) {
                return null;
            }
            return read(readAll(in));
        }
    }

    /** Returns a fixture class, failing when it is missing so a fixture test can never pass vacuously. */
    static ClassNode fixture(String simpleName) throws IOException {
        String name = ROOT + "statics/fixtures/" + simpleName;
        ClassNode node = fromClasspath(name);
        if (node == null) {
            throw new IllegalStateException("fixture " + name + " is not on the test classpath");
        }
        return node;
    }

    /** Parses class bytes. */
    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    /** Reads a stream to its end. */
    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static Path testOutput() {
        try {
            return Paths.get(
                MainClasses.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                .toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
