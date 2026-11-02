package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Every bundled asset parses, and every block it names is in the block-name map (CONTRIBUTING § Testing).
 *
 * <p>
 * Bundled assets live in the jar as upstream's tree, {@code data/<namespace>/lostcities/<type>/<path>.json}
 * (docs/new-spec/05-assets/asset-format.md, "Where"). Each must be strict JSON: no comments, no trailing data.
 * Block names are read from every {@code "block"} field, as palettes write them, and each must be in the block-name
 * map.
 *
 * <p>
 * M01 ships no assets and no block-name map yet, so the scan over the real tree finds nothing; the fixtures prove both
 * rules fail when broken. M02 converts the bundled assets, supplies the block-name map built from
 * docs/new-spec/00-pack-integration/block-mapping.md, replaces the {@code "block"} field reader with the asset model's
 * own, and raises {@link #MIN_BUNDLED_ASSETS} so the scan can no longer pass on an empty tree.
 */
public class AssetValidationTest {

    /** The fewest bundled assets the real tree must hold. M02 raises it to the converted catalog's size. */
    static final int MIN_BUNDLED_ASSETS = 0;

    /** Suffix of a bundled asset. */
    static final String ASSET_SUFFIX = ".json";

    /** Suffix of an asset fixture: JSON content, kept out of Spotless's JSON formatting. */
    static final String FIXTURE_SUFFIX = ".json.txt";

    private static final Gson GSON = new Gson();

    @Test
    public void everyBundledAssetParsesAndNamesKnownBlocks() throws Exception {
        List<Path> assets = assetFiles(ProjectFiles.resolve("src/main/resources/data"));
        assertTrue(
            assets.size() >= MIN_BUNDLED_ASSETS,
            "expected at least " + MIN_BUNDLED_ASSETS + " bundled assets, found " + assets.size());
        // No block-name map before M02, and no asset that could name a block.
        List<String> problems = validate(assets, Collections.<String>emptySet());
        assertTrue(problems.isEmpty(), "bundled assets:\n" + String.join("\n", problems));
    }

    @Test
    public void wellFormedFixturePasses() throws Exception {
        assertEquals(Collections.emptyList(), validate(fixtures("good"), knownBlocks()));
    }

    @Test
    public void malformedJsonFails() throws Exception {
        List<String> problems = validate(fixtures("malformed"), knownBlocks());
        assertEquals(1, problems.size(), "problems: " + problems);
        assertTrue(problems.get(0).contains("broken.json.txt") && problems.get(0).contains("not valid JSON"), problems.get(0));
    }

    @Test
    public void unknownBlockFails() throws Exception {
        List<String> problems = validate(fixtures("unknownblock"), knownBlocks());
        assertEquals(
            Collections.singletonList(
                "data/fixture/lostcities/palettes/unknown.json.txt: block 'nosuchmod:nosuchblock' is not in the block-name map"),
            problems);
    }

    /** Returns one line per file that does not parse and per block name the map lacks. */
    static List<String> validate(List<Path> assets, Set<String> knownBlocks) throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path asset : assets) {
            String id = displayName(asset);
            JsonElement json;
            try (Reader reader = Files.newBufferedReader(asset, StandardCharsets.UTF_8)) {
                json = parseStrict(reader);
            } catch (JsonParseException | IOException | IllegalStateException e) {
                problems.add(id + ": not valid JSON: " + e.getMessage());
                continue;
            }
            for (String block : blockNames(json)) {
                if (!knownBlocks.contains(block)) {
                    problems.add(id + ": block '" + block + "' is not in the block-name map");
                }
            }
        }
        return problems;
    }

    /** Parses one strict JSON document; trailing content fails. */
    static JsonElement parseStrict(Reader reader) throws IOException {
        JsonReader json = new JsonReader(reader);
        json.setLenient(false);
        JsonElement element = GSON.getAdapter(JsonElement.class)
            .read(json);
        if (json.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonParseException("content after the end of the document");
        }
        return element;
    }

    /** Returns every string value of a {@code "block"} field anywhere in the document. */
    static List<String> blockNames(JsonElement json) {
        List<String> out = new ArrayList<>();
        collect(json, out);
        return out;
    }

    private static void collect(JsonElement json, List<String> out) {
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                collect(element, out);
            }
        } else if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject()
                .entrySet()) {
                JsonElement value = entry.getValue();
                if ("block".equals(entry.getKey()) && value.isJsonPrimitive()) {
                    out.add(value.getAsString());
                } else {
                    collect(value, out);
                }
            }
        }
    }

    /** Returns the asset files under a {@code data} root: {@code <namespace>/lostcities/**}{@code /*.json}. */
    static List<Path> assetFiles(Path dataRoot) throws IOException {
        return assetFiles(dataRoot, ASSET_SUFFIX);
    }

    /**
     * Returns the files with a suffix under a {@code data} root, below {@code <namespace>/lostcities/}. Fixtures end in
     * {@link #FIXTURE_SUFFIX}, because Spotless formats every {@code src/**}{@code /*.json} and a deliberately broken
     * file would fail its check.
     */
    static List<Path> assetFiles(Path dataRoot, String suffix) throws IOException {
        if (!Files.isDirectory(dataRoot)) {
            return new ArrayList<>();
        }
        try (Stream<Path> walk = Files.walk(dataRoot)) {
            return walk.filter(Files::isRegularFile)
                .filter(
                    p -> p.toString()
                        .endsWith(suffix))
                .filter(
                    p -> dataRoot.relativize(p)
                        .getNameCount() > 2
                        && "lostcities".equals(
                            dataRoot.relativize(p)
                                .getName(1)
                                .toString()))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    private static List<Path> fixtures(String name) throws IOException, URISyntaxException {
        Path data = Paths.get(
            AssetValidationTest.class.getResource("/assetvalidation/" + name + "/data")
                .toURI());
        List<Path> files = assetFiles(data, FIXTURE_SUFFIX);
        assertTrue(!files.isEmpty(), "fixture " + name + " holds assets");
        return files;
    }

    private static String displayName(Path asset) {
        String path = asset.toString()
            .replace('\\', '/');
        int data = path.lastIndexOf("/data/");
        return data >= 0 ? path.substring(data + 1) : path;
    }

    private static Set<String> knownBlocks() {
        return new HashSet<>(Arrays.asList("minecraft:stone", "minecraft:stonebrick"));
    }
}
