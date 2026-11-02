package com.tuxprogrammer.lostcities.asset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Where asset files come from. The loader reads the jar's bundled files first, then
 * {@code config/lostcitiesnh/assets/},
 * whose files replace jar files with the same type and id and add new ones (docs/new-spec/05-assets/asset-format.md,
 * "Loading and override"; D14, P05-1).
 *
 * <p>
 * Side: both.
 */
public interface AssetSource {

    /** The class-path folder of the bundled tree, upstream's {@code data/} layout. */
    String DATA_ROOT = "/data/";

    /**
     * The bundled index: one path per line, relative to {@link #DATA_ROOT}. A jar cannot be listed portably, so the
     * index names every bundled file; {@code AssetValidationTest} checks that it matches the tree exactly.
     */
    String INDEX = "/data/lostcitiesnh/assets.index";

    /**
     * Returns the files, sorted by path, so the load order never depends on the file system.
     *
     * @param problems where a file that cannot be read is reported
     * @return the files
     */
    List<AssetFile> files(Problems problems);

    /**
     * Returns the bundled source, read from the class path through {@link #INDEX}. Bundled paths are
     * {@code <namespace>/lostcities/<folder>/<path>.json}, upstream's data-pack layout verbatim.
     *
     * @param anchor a class whose loader holds the resources
     * @return the source
     */
    static AssetSource bundled(Class<?> anchor) {
        return problems -> {
            List<AssetFile> out = new ArrayList<>();
            String index;
            try (InputStream in = anchor.getResourceAsStream(INDEX)) {
                if (in == null) {
                    problems.in("jar:" + INDEX)
                        .error("", null, "the bundled asset index is missing from the jar");
                    return out;
                }
                index = read(in);
            } catch (IOException e) {
                problems.in("jar:" + INDEX)
                    .error("", null, "cannot read the bundled asset index: " + e.getMessage());
                return out;
            }
            for (String line : index.split("\n")) {
                String path = line.trim();
                if (path.isEmpty() || path.startsWith("#")) {
                    continue;
                }
                String origin = "jar:data/" + path;
                Object[] located = locateBundled(path);
                if (located == null) {
                    problems.in(origin)
                        .error("", path, "not a data/<namespace>/lostcities/<type>/<path>.json asset path");
                    continue;
                }
                try (InputStream in = anchor.getResourceAsStream(DATA_ROOT + path)) {
                    if (in == null) {
                        problems.in(origin)
                            .error("", null, "listed in the asset index but missing from the jar");
                        continue;
                    }
                    out.add(new AssetFile((AssetType) located[0], (AssetId) located[1], origin, read(in)));
                } catch (IOException e) {
                    problems.in(origin)
                        .error("", null, "cannot be read: " + e.getMessage());
                }
            }
            return out;
        };
    }

    /**
     * Returns the override source over a folder laid out as {@code <namespace>/<folder>/<path>.json}. A missing folder
     * holds no files.
     *
     * @param root the folder, normally {@code config/lostcitiesnh/assets}
     * @return the source
     */
    static AssetSource directory(Path root) {
        return problems -> {
            List<AssetFile> out = new ArrayList<>();
            if (!Files.isDirectory(root)) {
                return out;
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(Files::isRegularFile)
                    .sorted()
                    .collect(Collectors.toList());
            } catch (IOException e) {
                problems.in("config:" + root)
                    .error("", null, "cannot list the override folder: " + e.getMessage());
                return out;
            }
            for (Path file : files) {
                String relative = root.relativize(file)
                    .toString()
                    .replace('\\', '/');
                String origin = "config:" + relative;
                if (!relative.endsWith(".json")) {
                    continue;
                }
                Object[] located = AssetFile.locate(relative);
                if (located == null) {
                    problems.in(origin)
                        .warning("", relative, "not a <namespace>/<type>/<path>.json asset path; ignored");
                    continue;
                }
                try {
                    String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    out.add(new AssetFile((AssetType) located[0], (AssetId) located[1], origin, text));
                } catch (IOException e) {
                    problems.in(origin)
                        .error("", null, "cannot be read: " + e.getMessage());
                }
            }
            return out;
        };
    }

    /** Locates a bundled path, {@code <namespace>/lostcities/<folder>/<path>.json}. */
    static Object[] locateBundled(String path) {
        String[] parts = path.split("/", 3);
        if (parts.length != 3 || !"lostcities".equals(parts[1])) {
            return null;
        }
        return AssetFile.locate(parts[0] + "/" + parts[2]);
    }

    /** Reads a stream as UTF-8. */
    static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
