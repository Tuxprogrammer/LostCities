package com.tuxprogrammer.lostcities.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.ProfileOption;
import com.tuxprogrammer.lostcities.config.profile.ProfileOptions;
import com.tuxprogrammer.lostcities.statics.ProjectFiles;

/**
 * Every profile option is documented in docs/config.md with its current default (CONTRIBUTING § GTNH integration,
 * "Config"): a row that starts {@code | `<key>` | `<default>` |}. The test also writes every row's generated part to
 * {@code build/docs/profile-options.md}, so a changed option's row can be copied from there.
 */
public class ProfileOptionsDocsTest {

    /** Returns the generated start of an option's row: key, default, range, status. */
    static String row(ProfileOption option) {
        String value = option.defaultValue()
            .isEmpty() ? "\"\"" : option.defaultValue();
        String range = option.range();
        if (!option.allowed()
            .isEmpty()) {
            range = String.join(", ", option.allowed());
        }
        String status;
        switch (option.status()) {
            case LIVE:
                status = "live";
                break;
            case PENDING:
                status = "pinned; read from " + option.owner();
                break;
            case DEFERRED:
                status = "deferred";
                break;
            default:
                status = "dropped";
        }
        return "| `" + option.key() + "` | `" + value + "` | " + (range.isEmpty() ? "" : range) + " | " + status + " |";
    }

    /** Returns the part of a row the test requires: key and default. */
    static String prefix(ProfileOption option) {
        String value = option.defaultValue()
            .isEmpty() ? "\"\"" : option.defaultValue();
        return "| `" + option.key() + "` | `" + value + "` |";
    }

    @Test
    public void everyProfileOptionHasADocsRowWithItsDefault() throws IOException {
        String docs = ProjectFiles.read(ProjectFiles.resolve("docs/config.md"));
        List<String> generated = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        ProfileOption.Category category = null;
        for (ProfileOption option : ProfileOptions.all()) {
            if (option.category() != category) {
                category = option.category();
                generated.add("");
                generated.add("### `" + category.key() + "`");
            }
            generated.add(row(option));
            if (!docs.contains(prefix(option))) {
                missing.add(prefix(option));
            }
        }
        Path out = ProjectFiles.root()
            .resolve("build/docs/profile-options.md");
        Files.createDirectories(out.getParent());
        Files.write(
            out,
            String.join("\n", generated)
                .getBytes(StandardCharsets.UTF_8));
        assertTrue(
            missing.isEmpty(),
            "docs/config.md has no row starting (see build/docs/profile-options.md):\n" + String.join("\n", missing));
    }
}
