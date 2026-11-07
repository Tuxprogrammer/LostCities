package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetSource;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.BlockData;

/**
 * Every bundled asset loads, and every block it names resolves under the block-name map (CONTRIBUTING § Testing;
 * docs/new-spec/05-assets/asset-format.md, "Checks at load"). The scan runs the asset model's own loader, the same code
 * the game runs at {@code postInit}, over two registries: the full pack, where every name must resolve to its mapped
 * primary, and vanilla 1.7.10 alone, where every name must still resolve through its documented fallback, never to
 * air. The fixtures prove the check fails on a malformed file and on an unknown block.
 */
public class AssetValidationTest {

    /** The fewest bundled assets the real tree must hold: upstream's 322 plus this mod's two rubble variants. */
    static final int MIN_BUNDLED_ASSETS = 324;

    /** Suffix of a bundled asset. */
    static final String ASSET_SUFFIX = ".json";

    /** Suffix of an asset fixture: JSON content, kept out of any JSON formatting of {@code src/**}. */
    static final String FIXTURE_SUFFIX = ".json.txt";

    @Test
    public void everyBundledAssetLoadsAndResolvesInThePack() {
        BlockData data = TestAssets.blockData();
        AssetSet set = TestAssets.bundled(TestAssets.pack(data));
        assertEquals(
            new ArrayList<String>(),
            TestAssets.lines(set, AssetProblem.Severity.ERROR),
            "bundled assets must load without errors");
        assertTrue(
            set.count() >= MIN_BUNDLED_ASSETS,
            "expected at least " + MIN_BUNDLED_ASSETS + ", found " + set.count());
        assertEquals(new ArrayList<>(), set.fallbacks(), "every bundled block resolves to its primary in the pack");
    }

    @Test
    public void everyBundledAssetLoadsOnVanillaThroughItsFallbacks() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        assertEquals(
            new ArrayList<String>(),
            TestAssets.lines(set, AssetProblem.Severity.ERROR),
            "with no pack mod present, every block still resolves through its vanilla fallback");
        assertTrue(set.count() >= MIN_BUNDLED_ASSETS);
        assertTrue(
            !set.fallbacks()
                .isEmpty(),
            "a vanilla-only registry takes fallbacks, so the check looked at the mapping");
    }

    @Test
    public void bundledFileCountsMatchTheCatalog() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        // docs/original-spec/05-assets/loading-and-lookup.md, "Registries and folders", plus the two rubble variants
        // and the three muted desert palettes (AR-16).
        assertEquals(42, set.count(AssetType.BUILDINGS));
        assertEquals(14, set.count(AssetType.MULTIBUILDINGS));
        assertEquals(225, set.count(AssetType.PARTS));
        assertEquals(34, set.count(AssetType.PALETTES));
        assertEquals(14, set.count(AssetType.VARIANTS));
        assertEquals(4, set.count(AssetType.STYLES));
        assertEquals(5, set.count(AssetType.CITYSTYLES));
        assertEquals(3, set.count(AssetType.CONDITIONS));
        assertEquals(2, set.count(AssetType.WORLDSTYLES));
        assertEquals(5, set.count(AssetType.SCATTERED));
        assertEquals(3, set.count(AssetType.STUFF));
        assertEquals(0, set.count(AssetType.PREDEFINEDCITIES));
        assertEquals(0, set.count(AssetType.PREDEFINEDSPHERES));
    }

    @Test
    public void theIndexListsExactlyTheBundledTree() throws IOException {
        Path data = ProjectFiles.resolve("src/main/resources/data");
        List<String> tree = new ArrayList<>();
        for (Path file : assetFiles(data, ASSET_SUFFIX)) {
            tree.add(
                data.relativize(file)
                    .toString()
                    .replace('\\', '/'));
        }
        List<String> index = new ArrayList<>();
        Path indexFile = ProjectFiles.resolve("src/main/resources" + AssetSource.INDEX);
        for (String line : new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8).split("\n")) {
            if (!line.trim()
                .isEmpty() && !line.startsWith("#")) {
                index.add(line.trim());
            }
        }
        assertEquals(tree, index, "rerun tools/asset-convert/convert.py, which writes the index");
    }

    @Test
    public void wellFormedFixturePasses() throws Exception {
        AssetSet set = TestAssets.load(TestAssets.vanilla(), TestAssets.fixtures(fixtureRoot("good"), FIXTURE_SUFFIX));
        assertEquals(new ArrayList<String>(), TestAssets.lines(set, AssetProblem.Severity.ERROR));
        assertEquals(1, set.count(AssetType.PALETTES));
    }

    @Test
    public void malformedJsonFails() throws Exception {
        AssetSet set = TestAssets
            .load(TestAssets.vanilla(), TestAssets.fixtures(fixtureRoot("malformed"), FIXTURE_SUFFIX));
        List<String> errors = TestAssets.lines(set, AssetProblem.Severity.ERROR);
        assertTrue(
            errors.get(0)
                .contains("broken.json.txt")
                && errors.get(0)
                    .contains("not valid JSON"),
            errors.toString());
        assertEquals(0, set.count(AssetType.PALETTES));
    }

    @Test
    public void unknownBlockFails() throws Exception {
        AssetSet set = TestAssets
            .load(TestAssets.vanilla(), TestAssets.fixtures(fixtureRoot("unknownblock"), FIXTURE_SUFFIX));
        List<String> errors = TestAssets.lines(set, AssetProblem.Severity.ERROR);
        assertTrue(
            errors.get(0)
                .startsWith(
                    "ERROR data/fixture/lostcities/palettes/unknown.json.txt: palette[1].block: 'nosuchmod:nosuchblock' is not in the block-name map"),
            errors.toString());
        assertEquals(0, set.count(AssetType.PALETTES), "an asset naming an unknown block is rejected, never aired");
    }

    /**
     * Returns the files with a suffix under a {@code data} root, below {@code <namespace>/lostcities/}.
     *
     * @param dataRoot the root
     * @param suffix   the suffix
     * @return the files, sorted
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
                        .getNameCount() > 2 && "lostcities".equals(
                            dataRoot.relativize(p)
                                .getName(1)
                                .toString()))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    private static Path fixtureRoot(String name) throws URISyntaxException {
        return Paths.get(
            AssetValidationTest.class.getResource("/assetvalidation/" + name + "/data")
                .toURI());
    }
}
