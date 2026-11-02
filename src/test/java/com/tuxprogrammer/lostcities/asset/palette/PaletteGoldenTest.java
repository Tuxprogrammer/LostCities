package com.tuxprogrammer.lostcities.asset.palette;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetFile;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.statics.ProjectFiles;

/**
 * Golden tests for the compiled palettes of representative buildings: the style's layers drawn with a fixed seed, the
 * building's palette on top, and a part's palette where it has one, in the full pack and in vanilla alone. A golden is
 * the SHA-256 of {@link CompiledPalette#describe()}, kept in {@code src/test/resources/golden/palette_<case>.sha256};
 * on a mismatch the actual text goes to {@code build/golden-actual/}, and {@code ./gradlew test -PupdateGoldens}
 * rewrites the goldens (docs/testing/README.md, "Golden chunks", the same convention). A golden changes only in a
 * commit that says why.
 */
public class PaletteGoldenTest {

    private static final String UPDATE_PROPERTY = "lostcitiesnh.updateGoldens";

    private static final AssetSet PACK = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
    private static final AssetSet VANILLA = TestAssets.bundled(TestAssets.vanilla());

    private static List<Palette> style(AssetSet set, String style, long seed) {
        return set.style(AssetId.parse(style))
            .randomPalettes(new Random(seed));
    }

    private static List<Palette> layers(AssetSet set, String... palettes) {
        List<Palette> out = new ArrayList<>();
        for (String palette : palettes) {
            out.add(set.palette(AssetId.parse(palette)));
        }
        return out;
    }

    private static Palette building(AssetSet set, String building) {
        return set.building(AssetId.parse(building))
            .palette();
    }

    @Test
    public void building1InTheStandardStyleOnVanilla() throws IOException {
        List<Palette> layers = style(VANILLA, "standard", 1L);
        layers.add(building(VANILLA, "building1"));
        check("palette_building1_standard_vanilla", CompiledPalette.compile(MetalFlags.ALL_ON, layers));
    }

    @Test
    public void building6TakesItsDeepslateInTheWindowColumns() throws IOException {
        // glass_side_variant_bricks' '@' copies '#'; building6 redefines '#' as deepslate (ASSET-11).
        List<Palette> layers = layers(
            PACK,
            "common",
            "default",
            "bricks_standard",
            "glass_full",
            "glass_side_variant_bricks");
        layers.add(building(PACK, "building6"));
        CompiledPalette compiled = CompiledPalette.compile(MetalFlags.ALL_ON, layers);
        assertTrue(
            compiled.getAll('@')
                .equals(compiled.getAll('#')));
        check("palette_building6_pack", compiled);
    }

    @Test
    public void theRedDesertPaletteHasItsRubble() throws IOException {
        List<Palette> layers = layers(
            PACK,
            "common",
            "default_desert",
            "bricks_desert_red",
            "glass_pane_gray",
            "glass_side_variant_quartz");
        CompiledPalette compiled = CompiledPalette.compile(MetalFlags.ALL_ON, layers);
        assertTrue(compiled.isDefined('}'), "ASSET-20: desert palettes define rubble");
        check("palette_desert_red_pack", compiled);
    }

    @Test
    public void theCabinWithTheCopperFlagOff() throws IOException {
        List<Palette> layers = style(PACK, "outside", 3L);
        CompiledPalette compiled = CompiledPalette.compile(MetalFlags.ALL_OFF, layers)
            .withLayer(
                PACK.part(AssetId.parse("cabin"))
                    .palette());
        check("palette_cabin_copper_off_pack", compiled);
    }

    @Test
    public void theOilRigInThePack() throws IOException {
        check("palette_oilrig_pack", CompiledPalette.compile(MetalFlags.ALL_ON, layers(PACK, "common", "oilrig")));
    }

    private static void check(String caseName, CompiledPalette palette) throws IOException {
        String text = palette.describe();
        String actual = AssetFile.sha256(text);
        Path golden = ProjectFiles.resolve("src/test/resources/golden/" + caseName + ".sha256");
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            writeActual(caseName, text);
            Files.write(
                golden,
                ("# Golden palette " + caseName
                    + ": SHA-256 of CompiledPalette.describe().\n"
                    + "# Rewritten by -PupdateGoldens; say why in the commit.\n"
                    + actual
                    + "\n").getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (!Files.isRegularFile(golden)) {
            fail("no golden " + caseName + " (actual " + actual + ", text in " + writeActual(caseName, text) + ")");
        }
        String expected = null;
        for (String line : Files.readAllLines(golden, StandardCharsets.UTF_8)) {
            if (!line.trim()
                .isEmpty() && !line.startsWith("#")) {
                expected = line.trim();
                break;
            }
        }
        if (!actual.equals(expected)) {
            fail(
                "golden palette " + caseName
                    + ": expected "
                    + expected
                    + ", got "
                    + actual
                    + "; the actual text is in "
                    + writeActual(caseName, text));
        }
    }

    private static Path writeActual(String caseName, String text) throws IOException {
        Path dir = ProjectFiles.resolve("build/golden-actual");
        Files.createDirectories(dir);
        Path file = dir.resolve(caseName + ".txt");
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
