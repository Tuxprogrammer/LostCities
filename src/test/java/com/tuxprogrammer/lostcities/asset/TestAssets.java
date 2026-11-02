package com.tuxprogrammer.lostcities.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.block.BlockData;
import com.tuxprogrammer.lostcities.asset.block.MappingRow;

/**
 * Shared fixtures for asset tests: registry views (vanilla 1.7.10 only, or the full pack as the block-name map sees
 * it), in-memory and fixture-file sources, and loading helpers. Test code only.
 */
public final class TestAssets {

    /** The class-path list of every vanilla 1.7.10 block registry name. */
    static final String VANILLA_BLOCKS = "/vanilla-1.7.10-blocks.txt";

    private TestAssets() {}

    /** Returns the bundled block data, failing on any problem in it. */
    public static BlockData blockData() {
        Problems problems = new Problems();
        BlockData data = BlockData.bundled(TestAssets.class, problems);
        if (!problems.of(AssetProblem.Severity.ERROR)
            .isEmpty()) {
            throw new AssertionError("bundled block data: " + problems.all());
        }
        return data;
    }

    /** Returns every vanilla 1.7.10 block registry name, {@code minecraft:<name>}. */
    public static Set<String> vanillaNames() {
        try (InputStream in = TestAssets.class.getResourceAsStream(VANILLA_BLOCKS)) {
            Set<String> out = new HashSet<>();
            for (String line : AssetSource.read(in)
                .split("\n")) {
                String name = line.trim();
                if (!name.isEmpty() && !name.startsWith("#")) {
                    out.add("minecraft:" + name);
                }
            }
            return out;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns a registry with only the vanilla 1.7.10 blocks, as in a dev run without the pack. */
    public static BlockRegistryView vanilla() {
        return BlockRegistryView.of(vanillaNames());
    }

    /**
     * Returns a registry with every vanilla block, every block the block-name map names and every block the catalog's
     * variety pools name, as the full pack registers them (the real-pack resolution scenario checks that it does), so
     * every mapping resolves to its primary and every palette-variety option is available.
     */
    public static BlockRegistryView pack(BlockData data) {
        Set<String> names = new HashSet<>(vanillaNames());
        names.addAll(poolBlockNames(data));
        for (MappingRow row : data.mapping()
            .rows()) {
            for (List<MappingRow.Candidate> chain : Arrays.asList(row.chain(), row.doubleChain(), row.metalOff())) {
                for (MappingRow.Candidate candidate : chain) {
                    names.add(
                        candidate.base()
                            .name());
                }
            }
        }
        return BlockRegistryView.of(names);
    }

    /** Returns the registry name of every block the raw variety pools name. */
    public static Set<String> poolBlockNames(BlockData data) {
        Set<String> out = new HashSet<>();
        JsonElement pools = data.pools();
        if (!pools.isJsonObject() || !pools.getAsJsonObject()
            .has("pools")) {
            return out;
        }
        for (JsonElement pool : pools.getAsJsonObject()
            .getAsJsonArray("pools")) {
            for (String key : new String[] { "candidates", "sets", "characters" }) {
                JsonElement rows = pool.getAsJsonObject()
                    .get(key);
                if (rows == null || !rows.isJsonArray()) {
                    continue;
                }
                for (JsonElement row : rows.getAsJsonArray()) {
                    JsonElement blocks = row.getAsJsonObject()
                        .get("blocks");
                    if (blocks == null || !blocks.isJsonArray()) {
                        continue;
                    }
                    for (JsonElement block : blocks.getAsJsonArray()) {
                        out.add(
                            BlockRef.parse(block.getAsString())
                                .name());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Returns a source of in-memory files.
     *
     * @param files path ({@code <namespace>/<folder>/<path>.json}) to content
     * @return the source
     */
    public static AssetSource source(Map<String, String> files) {
        return problems -> {
            List<AssetFile> out = new ArrayList<>();
            for (Map.Entry<String, String> file : files.entrySet()) {
                Object[] located = AssetFile.locate(file.getKey());
                if (located == null) {
                    throw new IllegalArgumentException("not an asset path: " + file.getKey());
                }
                out.add(
                    new AssetFile(
                        (AssetType) located[0],
                        (AssetId) located[1],
                        "test:" + file.getKey(),
                        file.getValue()));
            }
            return out;
        };
    }

    /** Returns a map from alternating path and content strings, in order. */
    public static Map<String, String> files(String... pathsAndContents) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < pathsAndContents.length; i += 2) {
            out.put(pathsAndContents[i], pathsAndContents[i + 1]);
        }
        return out;
    }

    /**
     * Returns a source over fixture files under a {@code data} root, {@code <ns>/lostcities/<folder>/<path><suffix>},
     * so deliberately broken fixtures need not end in {@code .json}.
     */
    public static AssetSource fixtures(Path dataRoot, String suffix) {
        return problems -> {
            List<AssetFile> out = new ArrayList<>();
            List<Path> paths;
            try (Stream<Path> walk = Files.walk(dataRoot)) {
                paths = walk.filter(Files::isRegularFile)
                    .filter(
                        p -> p.toString()
                            .endsWith(suffix))
                    .sorted()
                    .collect(Collectors.toList());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            for (Path path : paths) {
                String relative = dataRoot.relativize(path)
                    .toString()
                    .replace('\\', '/');
                String asJson = relative.substring(0, relative.length() - suffix.length()) + ".json";
                Object[] located = AssetSource.locateBundled(asJson);
                try {
                    out.add(
                        new AssetFile(
                            (AssetType) located[0],
                            (AssetId) located[1],
                            "data/" + relative,
                            new String(Files.readAllBytes(path), StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            return out;
        };
    }

    /** Loads sources with the bundled block data. */
    public static AssetSet load(BlockRegistryView registry, AssetSource... sources) {
        return AssetLoader.load(Arrays.asList(sources), blockData(), registry, new Problems());
    }

    /** Loads in-memory files over a vanilla registry. */
    public static AssetSet loadFiles(String... pathsAndContents) {
        return load(vanilla(), source(files(pathsAndContents)));
    }

    /** Loads the bundled set over a registry. */
    public static AssetSet bundled(BlockRegistryView registry) {
        return load(registry, AssetSource.bundled(TestAssets.class));
    }

    /** Returns the problem lines of a severity, for assertion messages. */
    public static List<String> lines(AssetSet set, AssetProblem.Severity severity) {
        List<String> out = new ArrayList<>();
        for (AssetProblem problem : set.problems(severity)) {
            out.add(problem.toString());
        }
        return Collections.unmodifiableList(out);
    }
}
