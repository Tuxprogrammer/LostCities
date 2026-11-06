package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The bundled room palettes, one per {@link RoomKind}, read once from {@code data/lostcitiesnh/furnish/<room>.json}
 * (design record 0016, decision 5; D-p6b-7 for the folder). A palette that is missing or unreadable leaves its rooms
 * empty and is reported in {@link #problems()}; registration never depends on these files.
 *
 * <p>
 * Immutable once built. Side: both (plain Java).
 */
public final class FurnishPalettes {

    /** The class-path folder of the palettes. */
    public static final String FOLDER = "/data/lostcitiesnh/furnish/";

    private static volatile FurnishPalettes bundled;

    private final Map<RoomKind, FurnishPalette> palettes;
    private final List<String> problems;

    FurnishPalettes(Map<RoomKind, FurnishPalette> palettes, List<String> problems) {
        this.palettes = Collections.unmodifiableMap(new EnumMap<>(palettes));
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    /** Returns the bundled palettes, read on first use. */
    public static FurnishPalettes bundled() {
        FurnishPalettes out = bundled;
        if (out == null) {
            synchronized (FurnishPalettes.class) {
                out = bundled;
                if (out == null) {
                    out = read();
                    bundled = out;
                }
            }
        }
        return out;
    }

    private static FurnishPalettes read() {
        Map<RoomKind, FurnishPalette> map = new EnumMap<>(RoomKind.class);
        List<String> problems = new ArrayList<>();
        for (RoomKind kind : RoomKind.values()) {
            String path = FOLDER + kind.id() + ".json";
            try (InputStream in = FurnishPalettes.class.getResourceAsStream(path)) {
                if (in == null) {
                    problems.add(path + " is not in the jar");
                    continue;
                }
                map.put(kind, FurnishPalette.read(kind, new InputStreamReader(in, StandardCharsets.UTF_8), problems));
            } catch (IOException | IllegalStateException e) {
                problems.add(path + ": " + e.getMessage());
            }
        }
        return new FurnishPalettes(map, problems);
    }

    /**
     * Returns a room kind's palette.
     *
     * @param kind the kind
     * @return the palette, or {@code null} when its file was missing
     */
    public FurnishPalette palette(RoomKind kind) {
        return palettes.get(kind);
    }

    /** Returns one line per problem in the files; empty when all are well formed. */
    public List<String> problems() {
        return problems;
    }
}
