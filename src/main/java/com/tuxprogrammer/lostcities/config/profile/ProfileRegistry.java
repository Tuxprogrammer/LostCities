package com.tuxprogrammer.lostcities.config.profile;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/ProfileSetup.java
// Avoids: PROF-03, PROF-09, PROF-13

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Every profile a server can pin: the built-ins from the jar ({@link BuiltinProfiles}) and the JSON files in
 * {@code config/lostcitiesnh/profiles/} (docs/new-spec/02-profiles-dimensions/index.md, "Profile system").
 *
 * <ul>
 * <li>Built-ins are never written to disk; {@code saveprofile} copies one there to edit.</li>
 * <li>A file is read in {@code preInit}, its name being the file name minus {@code .json} (PROF-22). A file that
 * reuses a built-in's name is rejected, so a name in a pinned world means one thing on every server (P-PROF-2,
 * D31).</li>
 * <li>A file that cannot be read, or fails a check, is rejected with one line naming it and the reason; the rest are
 * still read and mod loading carries on (PROF-02, PROF-03).</li>
 * <li>Once the assets are loaded, {@link #checkAssets} rejects any profile whose {@code worldStyle} or
 * {@code cityStyleAlternative} names no loaded asset, so neither can crash a dimension later (PROF-12, PROF-13).</li>
 * <li>Profiles matter only when a world is pinned: a world reads its snapshot, never this registry (PROF-09). So
 * {@link #load} may run again while a server is up ({@code /lostcities profile reload}).</li>
 * </ul>
 *
 * <p>
 * A registry the mod's lifecycle owns, so a mutable static holder is allowed (CONTRIBUTING § Architecture). Server
 * thread or mod loading only. Side: both (plain Java).
 */
public final class ProfileRegistry {

    /** The registry of this game instance. */
    public static final ProfileRegistry INSTANCE = new ProfileRegistry();

    /** The folder under the config folder that holds profile files. */
    public static final String FOLDER = "lostcitiesnh/profiles";

    /** One line of a load report. */
    public static final class Message {

        private final boolean error;
        private final String text;

        Message(boolean error, String text) {
            this.error = error;
            this.text = text;
        }

        /** Returns whether the line reports a rejected profile. */
        public boolean error() {
            return error;
        }

        /** Returns the line. */
        public String text() {
            return text;
        }

        @Override
        public String toString() {
            return (error ? "ERROR " : "WARN ") + text;
        }
    }

    private final Map<String, Profile> files = new TreeMap<>();
    private final Map<String, String> rejected = new TreeMap<>();
    private final Map<String, Profile> api = new TreeMap<>();

    /** Creates an empty registry: only the built-ins. Tests make their own; the game uses {@link #INSTANCE}. */
    public ProfileRegistry() {}

    /**
     * Returns a profile that can be pinned.
     *
     * @param name the name
     * @return the profile, or {@code null} when no accepted profile has that name
     */
    public synchronized Profile get(String name) {
        if (rejected.containsKey(name)) {
            return null;
        }
        Profile builtIn = BuiltinProfiles.get(name);
        if (builtIn != null) {
            return builtIn;
        }
        Profile file = files.get(name);
        return file != null ? file : api.get(name);
    }

    /** Returns the names of every accepted profile, sorted. */
    public synchronized Set<String> names() {
        Set<String> names = new TreeSet<>(BuiltinProfiles.names());
        names.addAll(files.keySet());
        names.addAll(api.keySet());
        names.removeAll(rejected.keySet());
        return Collections.unmodifiableSet(names);
    }

    /**
     * Returns why a profile was rejected.
     *
     * @param name the name
     * @return the reason, or {@code null} when it was not
     */
    public synchronized String rejection(String name) {
        return rejected.get(name);
    }

    /**
     * Returns a profile or explains why there is none.
     *
     * @param name the name
     * @return the profile
     * @throws IllegalArgumentException naming the reason or the known profiles
     */
    public synchronized Profile require(String name) {
        Profile profile = get(name);
        if (profile != null) {
            return profile;
        }
        String reason = rejected.get(name);
        if (reason != null) {
            throw new IllegalArgumentException("profile '" + name + "' was rejected: " + reason);
        }
        throw new IllegalArgumentException("unknown profile '" + name + "'; known: " + String.join(", ", names()));
    }

    /**
     * Reads every {@code *.json} file of a folder, in name order, replacing the files read before. A missing folder
     * is created, so owners find where profiles go.
     *
     * @param folder the profiles folder
     * @return the report: one line per rejected file, and the warnings of accepted ones
     */
    public synchronized List<Message> load(Path folder) {
        files.clear();
        rejected.clear();
        List<Message> report = new ArrayList<>();
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            report.add(new Message(true, "cannot create " + folder + ": " + e.getMessage()));
            return report;
        }
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*" + ProfileJson.EXTENSION)) {
            for (Path path : stream) {
                paths.add(path);
            }
        } catch (IOException e) {
            report.add(new Message(true, "cannot list " + folder + ": " + e.getMessage()));
            return report;
        }
        Collections.sort(paths);
        for (Path path : paths) {
            String fileName = path.getFileName()
                .toString();
            String name = ProfileJson.nameOf(fileName);
            if (name == null) {
                continue;
            }
            if (BuiltinProfiles.get(name) != null) {
                report.add(
                    new Message(
                        true,
                        fileName + ": '"
                            + name
                            + "' is a built-in profile's name; copy it under a new name with"
                            + " '/lostcities saveprofile "
                            + name
                            + " <newName>'"));
                continue;
            }
            String text;
            try {
                text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // Avoids PROF-03: one unreadable file no longer stops the rest from being read.
                reject(name, fileName + ": cannot be read: " + e.getMessage(), report);
                continue;
            }
            add(name, fileName, ProfileJson.read(name, path.toString(), text), report);
        }
        return report;
    }

    /**
     * Adds a profile read from text, as {@link #load} does for each file; for tests and the reload command.
     *
     * @param name     the profile's name
     * @param fileName the file it came from, for messages
     * @param load     the read profile
     * @param report   where messages go
     */
    synchronized void add(String name, String fileName, ProfileJson.Load load, List<Message> report) {
        for (String warning : load.warnings()) {
            report.add(new Message(false, fileName + ": " + warning));
        }
        if (load.profile() == null) {
            reject(name, String.join("; ", load.errors()), report);
            report.set(report.size() - 1, new Message(true, fileName + ": rejected: " + rejected.get(name)));
            return;
        }
        files.put(name, load.profile());
    }

    /**
     * Replaces the profiles other mods add through the API with a new read of their callbacks (docs/api.md). A name a
     * built-in or a file already has is left to them; the API profile is reported and dropped.
     *
     * @param profiles the API's profiles
     * @return one line per API profile that could not be added, and per error of the build
     */
    public synchronized List<Message> applyApi(ApiProfiles profiles) {
        for (String name : api.keySet()) {
            rejected.remove(name);
        }
        api.clear();
        List<Message> report = new ArrayList<>();
        ApiProfiles.Build build = profiles.build(
            name -> rejected.containsKey(name) ? null
                : BuiltinProfiles.get(name) != null ? BuiltinProfiles.get(name) : files.get(name),
            name -> BuiltinProfiles.get(name) != null || files.containsKey(name));
        for (String error : build.errors()) {
            report.add(new Message(true, error));
        }
        api.putAll(build.profiles());
        return report;
    }

    private void reject(String name, String reason, List<Message> report) {
        rejected.put(name, reason);
        report.add(new Message(true, reason));
    }

    /**
     * Rejects every profile whose world style or alternative city style is not loaded (PROF-12, PROF-13). Built-ins
     * are checked too: a broken asset override can remove a style they name.
     *
     * @param worldStyleExists whether a world style of a name is loaded
     * @param cityStyleExists  whether a city style of a name is loaded
     * @param blockResolves    whether a block name ({@code liquidBlock}, {@code baseBlock}) resolves (CORE-20)
     * @return one line per rejected profile
     */
    public synchronized List<Message> checkAssets(Predicate<String> worldStyleExists, Predicate<String> cityStyleExists,
        Predicate<String> blockResolves) {
        List<Message> report = new ArrayList<>();
        for (String name : names()) {
            Profile profile = get(name);
            ProfileValues values = profile.values();
            String worldStyle = values.getString("worldStyle");
            if (!worldStyleExists.test(worldStyle)) {
                reject(name, "profile '" + name + "': world style '" + worldStyle + "' is not loaded", report);
                continue;
            }
            String badBlock = null;
            for (String key : new String[] { "liquidBlock", "baseBlock" }) {
                if (badBlock == null && !blockResolves.test(values.getString(key))) {
                    badBlock = key + " '" + values.getString(key) + "'";
                }
            }
            if (badBlock != null) {
                reject(name, "profile '" + name + "': " + badBlock + " names no block", report);
                continue;
            }
            String alternative = values.getString("cityStyleAlternative");
            if (values.getFloat("cityStyleThreshold") >= 0 && !cityStyleExists.test(alternative)) {
                reject(
                    name,
                    "profile '" + name + "': cityStyleAlternative '" + alternative + "' is not a loaded city style",
                    report);
            }
        }
        return report;
    }
}
