package com.tuxprogrammer.lostcities.statics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The Java 8 platform API: which classes exist and which fields and methods they declare, for {@code Java8ApiTest}.
 *
 * <p>
 * On a JDK 9 or newer it comes from the JDK's own {@code lib/ct.sym}, the signature data behind
 * {@code javac --release 8}: each entry's first path segment lists the releases it belongs to, one character per
 * release, and the entries whose segment contains {@code 8} are the Java 8 API. On a Java 8 runtime the running
 * platform is itself the Java 8 API, so classes are read from it.
 */
final class Java8Api {

    private static final String RELEASE = "8";
    private static final String SIG = ".sig";

    private final Map<String, ClassFileSummary> classes;
    private final Set<String> jdkClassNames;
    private final boolean fromRuntime;

    private Java8Api(Map<String, ClassFileSummary> classes, Set<String> jdkClassNames, boolean fromRuntime) {
        this.classes = classes;
        this.jdkClassNames = jdkClassNames;
        this.fromRuntime = fromRuntime;
    }

    /** Loads the model for the JVM the tests run on. */
    static Java8Api load() throws IOException {
        if ("1.8".equals(System.getProperty("java.specification.version"))) {
            return new Java8Api(new HashMap<>(), Collections.<String>emptySet(), true);
        }
        File ctSym = new File(System.getProperty("java.home"), "lib/ct.sym");
        if (!ctSym.isFile()) {
            throw new IllegalStateException(
                "no " + ctSym + ": run the tests on a JDK (with ct.sym) or on Java 8, not on a bare runtime");
        }
        Map<String, ClassFileSummary> classes = new HashMap<>();
        Set<String> all = new HashSet<>();
        try (ZipFile zip = new ZipFile(ctSym)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String path = entry.getName();
                if (!path.endsWith(SIG) || path.endsWith("module-info" + SIG)) {
                    continue;
                }
                int slash = path.indexOf('/');
                if (slash < 0) {
                    continue;
                }
                boolean java8 = path.substring(0, slash)
                    .contains(RELEASE);
                ClassFileSummary summary;
                try (InputStream in = zip.getInputStream(entry)) {
                    summary = ClassFileSummary.parse(MainClasses.readAll(in));
                }
                all.add(summary.name);
                if (java8) {
                    classes.put(summary.name, summary);
                }
            }
        }
        if (!classes.containsKey("java/lang/Object")) {
            throw new IllegalStateException(ctSym + " holds no Java 8 data; this JDK cannot compile with --release 8");
        }
        return new Java8Api(classes, all, false);
    }

    /**
     * Returns whether the name is a class of the Java platform in some release, so the check applies to it. Every
     * {@code java.*} class does; a {@code javax.*} class only when the JDK ships it, since libraries use that prefix
     * too.
     */
    boolean isPlatformClass(String internalName) {
        if (internalName.startsWith("java/")) {
            return true;
        }
        if (!internalName.startsWith("javax/")) {
            return false;
        }
        if (fromRuntime) {
            URL url = ClassLoader.getSystemResource(internalName + ".class");
            return url != null && url.toString()
                .contains("/rt.jar!");
        }
        return jdkClassNames.contains(internalName);
    }

    /** Returns whether Java 8 has the class. */
    boolean hasClass(String internalName) throws IOException {
        return summary(internalName) != null;
    }

    /** Returns whether Java 8's class, or a supertype of it, declares the field. */
    boolean hasField(String owner, String name, String desc) throws IOException {
        for (ClassFileSummary type : hierarchy(owner)) {
            if (type.fields.contains(name + ":" + desc)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether Java 8's class, or a supertype of it, declares the method. */
    boolean hasMethod(String owner, String name, String desc) throws IOException {
        boolean polymorphic = "java/lang/invoke/MethodHandle".equals(owner)
            || "java/lang/invoke/VarHandle".equals(owner);
        for (ClassFileSummary type : hierarchy(owner)) {
            if (type.methods.contains(name + desc) || polymorphic && type.methodNames.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the class and every supertype Java 8 knows, java.lang.Object included for interfaces. */
    private Iterable<ClassFileSummary> hierarchy(String owner) throws IOException {
        Deque<String> pending = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        List<ClassFileSummary> out = new ArrayList<>();
        pending.add(owner);
        while (!pending.isEmpty()) {
            String name = pending.poll();
            if (!seen.add(name)) {
                continue;
            }
            ClassFileSummary type = summary(name);
            if (type == null) {
                continue;
            }
            out.add(type);
            if (type.superName != null) {
                pending.add(type.superName);
            }
            pending.addAll(type.interfaces);
        }
        if (seen.add("java/lang/Object")) {
            ClassFileSummary object = summary("java/lang/Object");
            if (object != null) {
                out.add(object);
            }
        }
        return out;
    }

    private ClassFileSummary summary(String internalName) throws IOException {
        if (!fromRuntime) {
            return classes.get(internalName);
        }
        if (classes.containsKey(internalName)) {
            return classes.get(internalName);
        }
        ClassFileSummary summary = null;
        try (InputStream in = ClassLoader.getSystemResourceAsStream(internalName + ".class")) {
            if (in != null) {
                summary = ClassFileSummary.parse(MainClasses.readAll(in));
            }
        }
        classes.put(internalName, summary);
        return summary;
    }
}
