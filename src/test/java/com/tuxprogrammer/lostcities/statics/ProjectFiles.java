package com.tuxprogrammer.lostcities.statics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Locates the project tree, for the static checks that read sources, resources and docs. */
public final class ProjectFiles {

    private ProjectFiles() {}

    /**
     * Returns the project root: the nearest folder at or above the working directory that holds both
     * {@code CONTRIBUTING.md} and {@code gradle.properties}.
     */
    public static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir"))
            .toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("CONTRIBUTING.md")) && Files.isRegularFile(p.resolve("gradle.properties"))) {
                return p;
            }
        }
        throw new IllegalStateException("no project root at or above " + dir);
    }

    /** Returns a path under the project root. */
    public static Path resolve(String relative) {
        return root().resolve(relative);
    }

    /** Returns the path of a file relative to the root, with forward slashes. */
    public static String relative(Path file) {
        return root().relativize(file.toAbsolutePath())
            .toString()
            .replace(File.separatorChar, '/');
    }

    /** Reads a UTF-8 text file. */
    public static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
