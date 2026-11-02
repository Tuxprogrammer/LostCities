package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.editor.PartExport;

/** {@link ExportPartSubcommand}: exports stay in their folder, and the file is stable and never corrupt. */
public class ExportPartSubcommandTest {

    @Test
    public void doesNotReproduceCORE28_onlyPlainNamesInTheExportFolder() {
        assertTrue(ExportPartSubcommand.validName("my_part_2"));
        for (String bad : new String[] { "", "../server", "a/b", "a.json", "Part", "a b", "..", "c:\\x" }) {
            assertFalse(ExportPartSubcommand.validName(bad), bad);
        }
        File config = new File("config");
        Path file = ExportPartSubcommand.file(config, "my_part_2");
        assertEquals(
            new File(new File(config, "lostcitiesnh/export/parts"), "my_part_2.json").toPath(),
            file,
            "the export goes outside the live assets, under config/lostcitiesnh/");
        assertTrue(
            file.normalize()
                .startsWith(new File(config, "lostcitiesnh").toPath()));
    }

    @Test
    public void doesNotReproduceCORE29_theSamePartExportsTheSameText() {
        BlockRef[] blocks = { BlockRef.of("minecraft:wool", 3), BlockRef.of("minecraft:stone", 0),
            BlockRef.of("minecraft:wool", 1) };
        PartExport.Result a = PartExport.export(
            3,
            1,
            1,
            Collections.<BlockRef, Character>emptyMap(),
            Collections.<Character>emptySet(),
            null,
            (x, y, z) -> blocks[x]);
        PartExport.Result b = PartExport.export(
            3,
            1,
            1,
            Collections.<BlockRef, Character>emptyMap(),
            Collections.<Character>emptySet(),
            null,
            (x, y, z) -> blocks[x]);
        assertEquals(a.json(), b.json());
        assertFalse(
            a.json()
                .contains("null"));
    }
}
