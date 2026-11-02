package com.tuxprogrammer.lostcities.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.tuxprogrammer.lostcities.statics.ProjectFiles;

/**
 * Every config option is documented (CONTRIBUTING § GTNH integration, "Config"): it has a comment and an explicit
 * default in code, and a row in docs/config.md of the form {@code | `<category>.<option>` | <default> | ...}, so the
 * reference can never fall behind the code.
 */
public class ConfigDocsTest {

    @Test
    public void everyOptionHasACommentADefaultAndADocsRow() throws Exception {
        String docs = ProjectFiles.read(ProjectFiles.resolve("docs/config.md"));
        List<String> problems = new ArrayList<>();
        for (Class<?> configClass : LostCitiesConfig.CLASSES) {
            String category = configClass.getAnnotation(Config.class)
                .category();
            for (Field option : LostCitiesConfig.options(configClass)) {
                String key = category + "." + option.getName();
                if (!option.isAnnotationPresent(Config.Comment.class)) {
                    problems.add(key + " has no @Config.Comment");
                }
                Config.DefaultBoolean defaultBoolean = option.getAnnotation(Config.DefaultBoolean.class);
                if (option.getType() == boolean.class && defaultBoolean == null) {
                    problems.add(key + " has no @Config.DefaultBoolean");
                }
                String row = "| `" + key + "` | `" + (defaultBoolean == null ? "" : defaultBoolean.value()) + "` |";
                if (defaultBoolean != null && !docs.contains(row)) {
                    problems.add("docs/config.md has no row starting " + row);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void docsSayWorldgenKeysApplyToNewWorldsOnly() throws Exception {
        String docs = ProjectFiles.read(ProjectFiles.resolve("docs/config.md"));
        assertTrue(
            docs.contains("new worlds only"),
            "docs/config.md explains that worldgen keys apply to new worlds only");
    }
}
