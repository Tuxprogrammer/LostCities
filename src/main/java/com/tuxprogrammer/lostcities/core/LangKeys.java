package com.tuxprogrammer.lostcities.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every lang key the code sends to players, in one place. Player-visible text is never hard-coded English: the server
 * sends these keys as {@code ChatComponentTranslation}, and the client resolves them from
 * {@code assets/lostcitiesnh/lang/en_US.lang} (CONTRIBUTING § Assets and localization).
 *
 * <p>
 * {@code LangKeysTest} checks that every key here, and every other key-shaped constant in the code, is in the lang
 * file; the dev self-check checks the same against the lang file inside the jar. The config screen's keys are not
 * listed here: GTNHLib derives them from the config fields, and {@code ConfigDocsTest} checks those.
 *
 * <p>
 * Side: both.
 */
public final class LangKeys {

    /** {@code /lostcities} usage line. */
    public static final String COMMAND_USAGE = "lostcitiesnh.command.usage";
    /** An unknown subcommand; argument: what was typed. */
    public static final String COMMAND_UNKNOWN = "lostcitiesnh.command.unknown";
    /** A subcommand the sender's permission level does not allow. */
    public static final String COMMAND_NO_PERMISSION = "lostcitiesnh.command.no_permission";

    /** {@code /lostcities test} usage line. */
    public static final String TEST_USAGE = "lostcitiesnh.command.test.usage";
    /** The scenario hooks are off (release jar without {@code -Dlostcitiesnh.testHooks=true}). */
    public static final String TEST_DISABLED = "lostcitiesnh.command.test.disabled";
    /** No scenario matches; argument: the selector. */
    public static final String TEST_NO_MATCH = "lostcitiesnh.command.test.no_match";
    /** A run is already in progress. */
    public static final String TEST_BUSY = "lostcitiesnh.command.test.busy";
    /** The scenario list; argument: the names. */
    public static final String TEST_LIST = "lostcitiesnh.command.test.list";
    /** A run started; argument: the number of scenarios. */
    public static final String TEST_STARTED = "lostcitiesnh.command.test.started";

    /** {@code /lostcities profile} usage line. */
    public static final String PROFILE_USAGE = "lostcitiesnh.command.profile.usage";
    /** The dimension has no pinned profile; argument: the dimension. */
    public static final String PROFILE_NONE = "lostcitiesnh.command.profile.none";
    /** The pinned profile; arguments: the dimension, the summary. */
    public static final String PROFILE_SHOW = "lostcitiesnh.command.profile.show";
    /** An unknown profile; arguments: the name, the known names. */
    public static final String PROFILE_UNKNOWN = "lostcitiesnh.command.profile.unknown";
    /** Pinning needs {@code confirm}; arguments: the profile, the dimension. */
    public static final String PROFILE_CONFIRM = "lostcitiesnh.command.profile.confirm";
    /** A profile was pinned; arguments: the profile, the dimension. */
    public static final String PROFILE_PINNED = "lostcitiesnh.command.profile.pinned";
    /** The world type has no height oracle; arguments: the dimension, the world type. */
    public static final String PROFILE_UNSUPPORTED = "lostcitiesnh.command.profile.unsupported";
    /** One line of a profile's text; argument: the text, as a component. */
    public static final String PROFILE_TEXT = "lostcitiesnh.command.profile.text";
    /** The profile list's header; argument: the count. */
    public static final String PROFILE_LIST = "lostcitiesnh.command.profile.list";
    /** One profile of the list; arguments: the name, where it comes from, the description. */
    public static final String PROFILE_LIST_ENTRY = "lostcitiesnh.command.profile.list_entry";
    /** Where a built-in comes from. */
    public static final String PROFILE_BUILT_IN = "lostcitiesnh.command.profile.built_in";
    /** Where a profile file comes from. */
    public static final String PROFILE_FILE = "lostcitiesnh.command.profile.file";
    /** A profile's first info line; arguments: the name, the source, the description. */
    public static final String PROFILE_INFO = "lostcitiesnh.command.profile.info";
    /** A profile's warning; argument: the warning. */
    public static final String PROFILE_INFO_WARNING = "lostcitiesnh.command.profile.info_warning";
    /** A profile changes nothing from the defaults. */
    public static final String PROFILE_INFO_DEFAULTS = "lostcitiesnh.command.profile.info_defaults";
    /** The options a profile changes; arguments: the count, the list. */
    public static final String PROFILE_INFO_CHANGES = "lostcitiesnh.command.profile.info_changes";
    /** A rejected profile; arguments: the name, the reason. */
    public static final String PROFILE_REJECTED = "lostcitiesnh.command.profile.rejected";
    /** The profiles were read again; arguments: accepted, rejected, report lines. */
    public static final String PROFILE_RELOADED = "lostcitiesnh.command.profile.reloaded";

    /** {@code /lostcities saveprofile} usage line. */
    public static final String SAVEPROFILE_USAGE = "lostcitiesnh.command.saveprofile.usage";
    /** A new name with characters outside {@code [a-z0-9_]}; argument: the name. */
    public static final String SAVEPROFILE_NAME = "lostcitiesnh.command.saveprofile.name";
    /** A new name that is a built-in's; argument: the name. */
    public static final String SAVEPROFILE_BUILT_IN = "lostcitiesnh.command.saveprofile.built_in";
    /** The file exists; argument: the file. */
    public static final String SAVEPROFILE_EXISTS = "lostcitiesnh.command.saveprofile.exists";
    /** The file was written; arguments: the profile, the file, the new name. */
    public static final String SAVEPROFILE_SAVED = "lostcitiesnh.command.saveprofile.saved";
    /** The file could not be written; arguments: the file, the reason. */
    public static final String SAVEPROFILE_FAILED = "lostcitiesnh.command.saveprofile.failed";

    /** {@code /lostcities timers} usage line. */
    public static final String TIMERS_USAGE = "lostcitiesnh.command.timers.usage";
    /** The overlay is not installed; argument: the dimension. */
    public static final String TIMERS_NONE = "lostcitiesnh.command.timers.none";
    /** One report line; argument: the line. */
    public static final String TIMERS_LINE = "lostcitiesnh.command.timers.line";
    /** The timers were reset; argument: the dimension. */
    public static final String TIMERS_RESET = "lostcitiesnh.command.timers.reset";

    /** {@code /lostcities view} usage line. */
    public static final String VIEW_USAGE = "lostcitiesnh.command.view.usage";
    /** A player was moved; arguments: the player, X, Y, Z. */
    public static final String VIEW_DONE = "lostcitiesnh.command.view.done";

    /** {@code /lostcities find} usage line. */
    public static final String FIND_USAGE = "lostcitiesnh.command.find.usage";
    /** The world has no real cities to search; argument: the dimension. */
    public static final String FIND_NO_CITIES = "lostcitiesnh.command.find.no_cities";
    /** Nothing of a kind within a radius; arguments: the kind, the radius in chunks. */
    public static final String FIND_NONE = "lostcitiesnh.command.find.none";
    /** A find; arguments: kind, X, Y, Z, label, view X, view Y, view Z, yaw, pitch. */
    public static final String FIND_FOUND = "lostcitiesnh.command.find.found";

    /** A subcommand that needs a player was sent from the console; argument: the subcommand. */
    public static final String COMMAND_PLAYER_ONLY = "lostcitiesnh.command.player_only";
    /** The sender's dimension has no real cities; argument: the dimension. */
    public static final String COMMAND_NO_CITIES = "lostcitiesnh.command.no_cities";

    /** {@code /lostcities debug}: the chunk; arguments: chunk X, chunk Z, dimension, profile. */
    public static final String DEBUG_CHUNK = "lostcitiesnh.command.debug.chunk";
    /** The city line; arguments: city, style, city level, city ground Y, kind. */
    public static final String DEBUG_CITY = "lostcitiesnh.command.debug.city";
    /** The building line; arguments: building, multibuilding, its position, floors, cellars, ruin height. */
    public static final String DEBUG_BUILDING = "lostcitiesnh.command.debug.building";
    /** The street line; arguments: street type, planned road, raw road, highway X, highway Z, rail, rail level. */
    public static final String DEBUG_STREET = "lostcitiesnh.command.debug.street";
    /** The landmark line; arguments: zone, landmark, lane role. */
    public static final String DEBUG_ZONE = "lostcitiesnh.command.debug.zone";
    /** The damage line; arguments: explosions reaching the chunk, the largest radius. */
    public static final String DEBUG_DAMAGE = "lostcitiesnh.command.debug.damage";
    /** Where the full dump went. */
    public static final String DEBUG_LOG = "lostcitiesnh.command.debug.log";

    /**
     * {@code /lostcities map}: the summary; arguments: size, size, chunk X, chunk Z, buildings, city, highway, cores.
     */
    public static final String MAP_DONE = "lostcitiesnh.command.map.done";

    /** {@code /lostcities locate} usage line. */
    public static final String LOCATE_USAGE = "lostcitiesnh.command.locate.usage";
    /** An unknown building; argument: the name. */
    public static final String LOCATE_UNKNOWN = "lostcitiesnh.command.locate.unknown";
    /** A hit; arguments: building, X, ground Y, Z. */
    public static final String LOCATE_FOUND = "lostcitiesnh.command.locate.found";
    /** No hit; arguments: building, radius in chunks. */
    public static final String LOCATE_NONE = "lostcitiesnh.command.locate.none";

    /** {@code /lostcities createbuilding} usage line. */
    public static final String CREATEBUILDING_USAGE = "lostcitiesnh.command.createbuilding.usage";
    /** An unknown building; argument: the name. */
    public static final String CREATEBUILDING_UNKNOWN = "lostcitiesnh.command.createbuilding.unknown";
    /** A level matches no part; argument: the building. */
    public static final String CREATEBUILDING_NO_PARTS = "lostcitiesnh.command.createbuilding.no_parts";
    /** Too tall; arguments: floors, cellars, Y, the top Y allowed, the floors that fit. */
    public static final String CREATEBUILDING_TOO_HIGH = "lostcitiesnh.command.createbuilding.too_high";
    /** Built; arguments: building, chunk X, chunk Z, floors, cellars, Y. */
    public static final String CREATEBUILDING_DONE = "lostcitiesnh.command.createbuilding.done";

    /** The world is not a part-editing world. */
    public static final String EDITOR_NOT_EDIT_MODE = "lostcitiesnh.command.editor.not_edit_mode";
    /** An unknown part; argument: the name. */
    public static final String EDITOR_UNKNOWN_PART = "lostcitiesnh.command.editor.unknown_part";
    /** A record names a part that is not loaded; argument: the part. */
    public static final String EDITOR_UNKNOWN_IN_CHUNK = "lostcitiesnh.command.editor.unknown_in_chunk";
    /** No recorded part spans the player; no arguments. */
    public static final String EDITOR_NOTHING_HERE = "lostcitiesnh.command.editor.nothing_here";
    /** The player has no session. */
    public static final String EDITOR_NOT_EDITING = "lostcitiesnh.command.editor.not_editing";
    /** {@code /lostcities createpart} usage line. */
    public static final String CREATEPART_USAGE = "lostcitiesnh.command.createpart.usage";
    /** A part was placed and a session started; arguments: part, X, Y, Z. */
    public static final String CREATEPART_DONE = "lostcitiesnh.command.createpart.done";
    /** {@code editpart} started; arguments: part, Y. */
    public static final String EDITPART_STARTED = "lostcitiesnh.command.editpart.started";
    /** {@code resumeedit} started; arguments: part, Y. */
    public static final String RESUMEEDIT_STARTED = "lostcitiesnh.command.resumeedit.started";
    /** {@code listparts} entry; arguments: part, Y. */
    public static final String LISTPARTS_ENTRY = "lostcitiesnh.command.listparts.entry";
    /** {@code listparts} found none; arguments: chunk X, chunk Z. */
    public static final String LISTPARTS_NONE = "lostcitiesnh.command.listparts.none";
    /** {@code /lostcities locatepart} usage line. */
    public static final String LOCATEPART_USAGE = "lostcitiesnh.command.locatepart.usage";
    /** A hit; arguments: part, X, Y, Z. */
    public static final String LOCATEPART_FOUND = "lostcitiesnh.command.locatepart.found";
    /** No hit; arguments: part, radius in chunks. */
    public static final String LOCATEPART_NONE = "lostcitiesnh.command.locatepart.none";
    /** {@code /lostcities exportpart} usage line. */
    public static final String EXPORTPART_USAGE = "lostcitiesnh.command.exportpart.usage";
    /** A bad file name; argument: the name. */
    public static final String EXPORTPART_NAME = "lostcitiesnh.command.exportpart.name";
    /** The file exists; argument: the file. */
    public static final String EXPORTPART_EXISTS = "lostcitiesnh.command.exportpart.exists";
    /** The character pool ran out; argument: its size. */
    public static final String EXPORTPART_POOL = "lostcitiesnh.command.exportpart.pool";
    /** Exported; arguments: part, file, new blocks. */
    public static final String EXPORTPART_DONE = "lostcitiesnh.command.exportpart.done";
    /** The file could not be written; arguments: file, reason. */
    public static final String EXPORTPART_FAILED = "lostcitiesnh.command.exportpart.failed";

    /** One scenario's result; arguments: PASS or FAIL, name, milliseconds, message. */
    public static final String SCENARIO_RESULT = "lostcitiesnh.scenario.result";
    /** A run's summary; arguments: passed, failed. */
    public static final String SCENARIO_SUMMARY = "lostcitiesnh.scenario.summary";

    private LangKeys() {}

    /** Returns every key declared in this class (reflection gives no guaranteed order; callers sort if it matters). */
    public static List<String> all() {
        List<String> keys = new ArrayList<>();
        for (Field field : LangKeys.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType() == String.class) {
                try {
                    keys.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    // The fields are public, so reflection on them cannot be refused.
                    throw new IllegalStateException("cannot read " + field.getName(), e);
                }
            }
        }
        return Collections.unmodifiableList(keys);
    }
}
