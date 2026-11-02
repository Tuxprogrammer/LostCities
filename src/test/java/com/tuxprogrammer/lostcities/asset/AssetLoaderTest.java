package com.tuxprogrammer.lostcities.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Stuff;

/** Loading, override, name resolution, inheritance and the indexes (docs/new-spec/05-assets/asset-format.md). */
public class AssetLoaderTest {

    private static final String PART = "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"#\"]]}";

    @Test
    public void namesAreNormalizedToFullIds() {
        assertEquals(
            "lostcities:building1",
            AssetId.parse("building1")
                .toString());
        assertEquals(
            "mypack:tower",
            AssetId.parse("mypack:tower")
                .toString());
        assertEquals(AssetId.parse("building1"), AssetId.parse("lostcities:building1"));
    }

    @Test
    public void configFilesReplaceJarFilesWithTheSameIdAndAddNewOnes(@TempDir Path config) throws IOException {
        write(
            config,
            "lostcities/palettes/common.json",
            "{\"palette\": [{\"char\": \"#\", \"block\": \"minecraft:glass\"}]}");
        write(config, "mypack/parts/tower.json", PART);
        AssetSet set = TestAssets
            .load(TestAssets.vanilla(), AssetSource.bundled(TestAssets.class), AssetSource.directory(config));
        assertEquals(
            1,
            set.palette(AssetId.parse("common"))
                .entries()
                .size(),
            "the whole file is replaced, as upstream's data packs do");
        assertNotNull(set.part(AssetId.parse("mypack:tower")));
        assertTrue(
            set.problems()
                .stream()
                .anyMatch(
                    p -> p.toString()
                        .contains("replaces jar:data/lostcities/lostcities/palettes/common.json")));
        assertNotEquals(
            TestAssets.bundled(TestAssets.vanilla())
                .hash(),
            set.hash());
        List<String> changed = set.changedFiles(
            TestAssets.bundled(TestAssets.vanilla())
                .fileHashes());
        assertEquals(java.util.Arrays.asList("palettes/lostcities:common", "parts/mypack:tower"), changed);
    }

    @Test
    public void theHashIsStableForTheSameFiles() {
        assertEquals(
            TestAssets.bundled(TestAssets.vanilla())
                .hash(),
            TestAssets.bundled(TestAssets.pack(TestAssets.blockData()))
                .hash());
    }

    @Test
    public void doesNotReproduceASSET01_missingOptionalReferencesWarnAndRequiredOnesRejectAtLoad() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/parts/p.json",
            PART,
            "lostcities/buildings/b.json",
            "{\"filler\": \"#\", \"parts\": [{\"part\": \"typo\"}]}",
            "lostcities/citystyles/c.json",
            "{\"selectors\": {\"largebridges\": [{\"factor\": 1, \"value\": \"typo_bridge\"}]}}");
        assertNull(set.building(AssetId.parse("b")), "a required reference rejects the referring asset");
        CityStyle style = set.cityStyle(AssetId.parse("c"));
        assertNotNull(style, "an optional reference warns and acts as absent");
        assertTrue(
            style.selectors(CityStyle.SelectorKind.LARGE_BRIDGES)
                .isEmpty());
        // Lookups of unknown names never throw.
        assertNull(set.part(AssetId.parse("nosuch")));
        assertNull(set.worldStyle(AssetId.parse("nosuch")));
    }

    @Test
    public void doesNotReproduceASSET15_theStuffIndexExistsAtLoad() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        List<Stuff> rubble = set.stuffByTag("rubble");
        assertEquals(2, rubble.size(), "chains and cobwebs, available before any chunk generates");
        assertTrue(
            set.stuffByTag("nosuch")
                .isEmpty());
    }

    @Test
    public void doesNotReproduceSCAT22_eachTagsListIsSortedById() {
        String stuff = "{\"tags\": [\"t\"], \"column\": \"x\", \"mincount\": 1, \"maxcount\": 1, \"attempts\": 1}";
        AssetSet set = TestAssets.loadFiles(
            "lostcities/stuff/zebra.json",
            stuff,
            "lostcities/stuff/alpha.json",
            stuff,
            "lostcities/stuff/mango.json",
            stuff);
        List<String> ids = new ArrayList<>();
        for (Stuff s : set.stuffByTag("t")) {
            ids.add(
                s.id()
                    .path());
        }
        assertEquals(java.util.Arrays.asList("alpha", "mango", "zebra"), ids);
    }

    @Test
    public void doesNotReproduceASSET28_inheritanceIsResolvedRootFirstAtLoad() {
        AssetSet set = TestAssets.loadFiles(
            "lostcities/citystyles/grandchild.json",
            "{\"inherit\": \"child\"}",
            "lostcities/citystyles/child.json",
            "{\"inherit\": \"root\", \"railblocks\": {\"railmain\": \"y\"}}",
            "lostcities/citystyles/root.json",
            "{\"streetblocks\": {\"street\": \"S\"}}");
        CityStyle grandchild = set.cityStyle(AssetId.parse("grandchild"));
        assertEquals(Character.valueOf('S'), grandchild.character(CityStyle.Setting.STREET));
        assertEquals(Character.valueOf('y'), grandchild.character(CityStyle.Setting.RAIL_MAIN));
        AssetSet broken = TestAssets.loadFiles(
            "lostcities/citystyles/child.json",
            "{\"inherit\": \"root\"}",
            "lostcities/citystyles/root.json",
            "{\"style\": 5}");
        assertNull(broken.cityStyle(AssetId.parse("root")));
        assertNull(broken.cityStyle(AssetId.parse("child")), "never a half-built style without its parent");
    }

    @Test
    public void unreachableAssetsAreListed() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        // docs/new-spec/05-assets/index.md, "Dead assets": stuff/example and two palettes nothing names.
        assertTrue(
            set.unreachable()
                .contains("stuff/lostcities:example"),
            set.unreachable()
                .toString());
        assertTrue(
            set.unreachable()
                .contains("palettes/lostcities:glass_side_variant_street"));
        assertTrue(
            set.unreachable()
                .contains("palettes/lostcities:bricks_desert_redsand"));
        assertTrue(
            !set.unreachable()
                .contains("buildings/lostcities:building1"));
    }

    @Test
    public void codeNamedBlocksAndDefaultTablesResolve() {
        AssetSet pack = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
        assertEquals(
            BlockRef.parse("malisisdoors:door_birch:3"),
            pack.codeBlock("door.birch")
                .block());
        assertEquals(
            BlockRef.parse("chisel:moss:0"),
            pack.codeBlock("rubble.moss")
                .block());
        assertEquals(
            BlockRef.parse("minecraft:leaves:7"),
            pack.codeBlock("leaves.jungle")
                .block(),
            "jungle leaves with the no-decay bit");
        assertEquals(68, count(pack, BlockRef.parse("chisel:moss:0")));
        AssetSet vanilla = TestAssets.bundled(TestAssets.vanilla());
        assertEquals(
            BlockRef.parse("minecraft:wooden_door:3"),
            vanilla.codeBlock("door.birch")
                .block());
        assertEquals(
            108,
            count(vanilla, BlockRef.parse("minecraft:mossy_cobblestone:0")),
            "D31: 40 + 68 without Chisel");
    }

    @Test
    public void malformedFilesAreRejectedAndLoadingCarriesOn() {
        AssetSet set = TestAssets
            .loadFiles("lostcities/parts/broken.json", "{\"xsize\": 1,", "lostcities/parts/fine.json", PART);
        assertNull(set.part(AssetId.parse("broken")));
        assertNotNull(set.part(AssetId.parse("fine")));
        assertEquals(Collections.singletonList("parts/lostcities:fine"), set.unreachable());
    }

    private static int count(AssetSet set, BlockRef block) {
        int n = 0;
        for (int i = 0; i < 128; i++) {
            if (set.defaultRubble()
                .slot(i)
                .block()
                .equals(block)) {
                n++;
            }
        }
        return n;
    }

    private static void write(Path root, String relative, String text) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }
}
