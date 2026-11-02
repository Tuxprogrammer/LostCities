package com.tuxprogrammer.lostcities.command;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.core.LangFile;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.editor.EditModeData;
import com.tuxprogrammer.lostcities.editor.EditSession;
import com.tuxprogrammer.lostcities.editor.EditorEvents;
import com.tuxprogrammer.lostcities.editor.PartEditor;
import com.tuxprogrammer.lostcities.editor.PartRecord;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ChunkSpiral;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * In-world checks of the {@code /lostcities} commands (docs/testing/README.md, "commands" scenarios): each runs the
 * subcommands through the server's command manager as fake players, an operator and an ordinary player, and reads the
 * chat they get back.
 * <ul>
 * <li>{@code commands.permissions}: an ordinary player is refused every subcommand; an operator gets answers; every
 * message is a lang key ({@code lostcitiesnh.*} defined in en_US.lang, or vanilla's {@code commands.*}), and none is
 * vanilla's "an unexpected error occurred". Read-only: nothing is written into the world.</li>
 * <li>{@code commands.editor}: the part editor end to end in the scenario's arena: in an {@code editMode} world the
 * records of a freshly generated building, {@code listparts}, {@code editpart}, an edit, {@code exportpart} with the
 * new block in its missing palette, {@code resumeedit}, {@code locatepart}, then {@code createpart},
 * {@code createbuilding} and a logout that ends the session; in another world, the edit-mode gate.</li>
 * </ul>
 * Each line {@code [LCNH-COMMANDS] <who> <command> -> <keys>} goes to the log.
 *
 * <p>
 * Side: server.
 */
public final class CommandScenarios {

    /** Prefix of the log lines. */
    static final String TAG = "[LCNH-COMMANDS] ";
    /** Vanilla's lang key for a command the sender may not use. */
    static final String VANILLA_PERMISSION = "commands.generic.permission";
    /** Vanilla's lang key for an exception a command threw that was not a command error. */
    static final String VANILLA_CRASH = "commands.generic.exception";
    /** Vanilla's lang key wrapping a usage line. */
    static final String VANILLA_USAGE = "commands.generic.usage";
    /** How far the editor scenario looks for a building from its arena, in chunks. */
    static final int SEARCH_RADIUS = 48;
    /** The export file the editor scenario writes and removes. */
    static final String EXPORT_NAME = "m10_scenario_export";

    private CommandScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry the registry
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Permissions());
        registry.register(new Editor());
    }

    /** Runs a command line and returns the lang keys of the chat it sent, a usage line's inner key included. */
    static List<String> run(TestPlayer player, String line, Set<String> lang, ScenarioContext context) {
        player.clearChat();
        MinecraftServer.getServer()
            .getCommandManager()
            .executeCommand(player, line);
        List<String> keys = new ArrayList<>();
        for (IChatComponent message : player.chat()) {
            context.check(
                message instanceof ChatComponentTranslation,
                "'" + line + "' sent text that is not a lang key: " + message.getUnformattedText());
            ChatComponentTranslation translation = (ChatComponentTranslation) message;
            String key = translation.getKey();
            keys.add(key);
            context.check(!VANILLA_CRASH.equals(key), "'" + line + "' threw instead of answering");
            if (VANILLA_USAGE.equals(key) && translation.getFormatArgs().length > 0
                && translation.getFormatArgs()[0] instanceof ChatComponentTranslation inner) {
                // Vanilla wraps a usage line in its own message; the line itself is ours.
                String usage = inner.getKey();
                keys.add(usage);
                context.check(lang.contains(usage), "'" + line + "' usage key " + usage + " is not in en_US.lang");
            } else if (key.startsWith(LostCities.MODID + ".")) {
                context.check(lang.contains(key), "'" + line + "' sent " + key + ", which en_US.lang lacks");
            } else {
                context.check(key.startsWith("commands."), "'" + line + "' sent a key of no known kind: " + key);
            }
        }
        LostCities.LOG.info("{}{} {} -> {}", TAG, player.getCommandSenderName(), line, keys);
        return keys;
    }

    /** Returns the keys en_US.lang defines. */
    static Set<String> langKeys() throws IOException {
        try (InputStream in = CommandScenarios.class.getResourceAsStream(LangFile.EN_US)) {
            if (in == null) {
                throw new IOException("en_US.lang is not on the classpath");
            }
            return LangFile.keys(in);
        }
    }

    /** An ordinary player is refused; an operator is answered in lang keys. */
    static final class Permissions implements Scenario {

        /** Every subcommand, with arguments that make it answer without writing into the world. */
        static final String[] OPERATOR_LINES = { "lostcities", "lostcities nosuchsubcommand", "lostcities debug",
            "lostcities map", "lostcities locate building1", "lostcities locate no_such_building", "lostcities profile",
            "lostcities profile list", "lostcities profile info default", "lostcities timers", "lostcities timings",
            "lostcities listparts", "lostcities locatepart building1_1", "lostcities editpart", "lostcities resumeedit",
            "lostcities exportpart Bad.Name", "lostcities exportpart m10_no_session",
            "lostcities createpart no_such_part ~ ~ ~", "lostcities createbuilding no_such_building 1 0 ~ ~ ~",
            "lostcities createbuilding building1 99 0 ~ ~ ~", "lostcities saveprofile default default",
            "lostcities test list" };

        /** Every subcommand's name, which an ordinary player may not use. */
        static final String[] SUBCOMMANDS = { "profile", "saveprofile", "timers", "timings", "test", "view", "find",
            "debug", "map", "locate", "createbuilding", "createpart", "editpart", "resumeedit", "listparts",
            "locatepart", "exportpart" };

        @Override
        public String name() {
            return "commands.permissions";
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public void setup(ScenarioContext context) {}

        @Override
        public void verify(ScenarioContext context) throws IOException {
            Set<String> lang = langKeys();
            WorldServer world = context.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            TestPlayer player = new TestPlayer(world, "LCNHPlayer", 0);
            player.moveTo(spawn.posX + 0.5, spawn.posY, spawn.posZ + 0.5);
            for (String subcommand : SUBCOMMANDS) {
                for (String root : new String[] { "lostcities ", "lost ", "lostcitiesnh " }) {
                    List<String> keys = run(player, root + subcommand, lang, context);
                    context.check(
                        keys.size() == 1 && VANILLA_PERMISSION.equals(keys.get(0)),
                        "an ordinary player ran '" + root + subcommand + "': " + keys);
                }
            }
            TestPlayer operator = new TestPlayer(world, "LCNHOperator", 4);
            operator.moveTo(spawn.posX + 0.5, spawn.posY, spawn.posZ + 0.5);
            boolean cities = hasCities(world);
            for (String line : OPERATOR_LINES) {
                List<String> keys = run(operator, line, lang, context);
                context.check(!keys.isEmpty(), "an operator got no answer to '" + line + "'");
                context.check(!keys.contains(VANILLA_PERMISSION), "an operator was refused '" + line + "'");
            }
            expect(operator, "lostcities nosuchsubcommand", LangKeys.COMMAND_UNKNOWN, lang, context);
            expect(operator, "lostcities exportpart Bad.Name", LangKeys.EXPORTPART_NAME, lang, context);
            if (cities) {
                expect(operator, "lostcities debug", LangKeys.DEBUG_CHUNK, lang, context);
                expect(operator, "lostcities map", LangKeys.MAP_DONE, lang, context);
                expect(operator, "lostcities locate no_such_building", LangKeys.LOCATE_UNKNOWN, lang, context);
                expect(operator, "lostcities exportpart m10_no_session", LangKeys.EDITOR_NOT_EDITING, lang, context);
                expect(
                    operator,
                    "lostcities createpart no_such_part ~ ~ ~",
                    LangKeys.EDITOR_UNKNOWN_PART,
                    lang,
                    context);
            } else {
                expect(operator, "lostcities debug", LangKeys.COMMAND_NO_CITIES, lang, context);
            }
        }

        private static void expect(TestPlayer player, String line, String key, Set<String> lang,
            ScenarioContext context) {
            List<String> keys = run(player, line, lang, context);
            context.check(keys.contains(key), "'" + line + "' should answer " + key + ", answered " + keys);
        }
    }

    /** The part editor end to end, or its gate in a world without {@code editMode}. */
    static final class Editor implements Scenario {

        @Override
        public String name() {
            return "commands.editor";
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public void setup(ScenarioContext context) {}

        @Override
        public void verify(ScenarioContext context) throws IOException {
            Set<String> lang = langKeys();
            WorldServer world = context.world();
            OverlayWorld overlay = Overlays.INSTANCE.get(world);
            TestPlayer editor = new TestPlayer(world, "LCNHEditor", 4);
            int centerX = context.arenaChunkX() + ScenarioContext.ARENA_CHUNKS / 2;
            int centerZ = context.arenaChunkZ() + ScenarioContext.ARENA_CHUNKS / 2;
            editor.moveTo((centerX << 4) + 8.5, 100, (centerZ << 4) + 8.5);
            if (overlay == null || overlay.cities() == null) {
                List<String> keys = run(editor, "lostcities editpart", lang, context);
                context.check(
                    keys.contains(LangKeys.COMMAND_NO_CITIES),
                    "a world without cities must refuse the editor, answered " + keys);
                LostCities.LOG.info("{}world has no real cities: only the dimension check was checked", TAG);
                return;
            }
            if (!overlay.editMode()) {
                List<String> keys = run(editor, "lostcities listparts", lang, context);
                context.check(
                    keys.contains(LangKeys.EDITOR_NOT_EDIT_MODE),
                    "a world without editMode must refuse listparts, answered " + keys);
                LostCities.LOG.info("{}world has no editMode: only the gate was checked", TAG);
                return;
            }
            int[] chunk = nearestBuilding(
                overlay.cities()
                    .planner(),
                centerX,
                centerZ);
            context.check(chunk != null, "no building within " + SEARCH_RADIUS + " chunks of the arena");
            world.theChunkProviderServer.loadChunk(chunk[0], chunk[1]);
            List<PartRecord> records = EditModeData.get(world)
                .records()
                .at(0, chunk[0], chunk[1]);
            context.check(!records.isEmpty(), "the generated building chunk has no records");
            PartRecord first = records.get(0);
            double x = (chunk[0] << 4) + 8.5;
            double z = (chunk[1] << 4) + 8.5;
            editor.moveTo(x, first.y() + 1, z);

            List<String> keys = run(editor, "lostcities listparts", lang, context);
            context.check(
                keys.size() == records.size() && keys.get(0)
                    .equals(LangKeys.LISTPARTS_ENTRY),
                "listparts should list " + records.size() + " parts, answered " + keys);
            keys = run(editor, "lostcities editpart", lang, context);
            context.check(keys.contains(LangKeys.EDITPART_STARTED), "editpart answered " + keys);
            EditSession session = overlay.editSessions()
                .get(editor.getUniqueID());
            context.check(session != null, "editpart started no session");

            // An edit with a block no palette has: it goes to the export's missing palette.
            int editX = (chunk[0] << 4) + 3;
            int editY = session.bottom() + 1;
            int editZ = (chunk[1] << 4) + 3;
            world.setBlock(editX, editY, editZ, Blocks.sponge, 0, 3);
            Path file = Loader.instance()
                .getConfigDir()
                .toPath()
                .resolve(ExportPartSubcommand.FOLDER)
                .resolve(EXPORT_NAME + ExportPartSubcommand.EXTENSION);
            Files.deleteIfExists(file);
            keys = run(editor, "lostcities exportpart " + EXPORT_NAME, lang, context);
            context.check(keys.contains(LangKeys.EXPORTPART_DONE), "exportpart answered " + keys);
            context.check(Files.exists(file), "exportpart wrote no " + file);
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(json)
                .getAsJsonObject();
            context.check(
                root.has("exportedpart") && root.has("missingpalette") && json.contains("minecraft:sponge:0"),
                "the export lacks the edited block in its missing palette: " + json.length() + " characters");
            keys = run(editor, "lostcities exportpart " + EXPORT_NAME, lang, context);
            context.check(keys.contains(LangKeys.EXPORTPART_EXISTS), "an existing file needs confirm: " + keys);

            keys = run(editor, "lostcities resumeedit", lang, context);
            context.check(keys.contains(LangKeys.RESUMEEDIT_STARTED), "resumeedit answered " + keys);
            context
                .check(world.getBlock(editX, editY, editZ) == Blocks.sponge, "resumeedit must keep the player's edit");

            keys = run(editor, "lostcities locatepart " + first.part(), lang, context);
            context.check(keys.contains(LangKeys.LOCATEPART_FOUND), "locatepart answered " + keys);

            Part part = new PartEditor(overlay).part(
                first.part()
                    .toString());
            int partX = (centerX << 4) + 1;
            int partZ = (centerZ << 4) + 1;
            editor.moveTo(partX + 0.5, 120, partZ + 0.5);
            keys = run(editor, "lostcities createpart " + first.part() + " ~ 120 ~", lang, context);
            context.check(keys.contains(LangKeys.CREATEPART_DONE), "createpart answered " + keys);
            context.check(
                overlay.editSessions()
                    .get(editor.getUniqueID())
                    .bottom() == 120,
                "createpart's session starts at Y 120");
            context.check(part != null, "the recorded part is not loaded");

            keys = run(
                editor,
                "lostcities createbuilding building1 2 1 " + (partX + 16) + " 90 " + partZ,
                lang,
                context);
            context.check(keys.contains(LangKeys.CREATEBUILDING_DONE), "createbuilding answered " + keys);
            int solid = 0;
            for (int y = 90; y < 90 + 4 * 6; y++) {
                solid += world.isAirBlock(partX + 16 + 1, y, partZ + 1) ? 0 : 1;
            }
            context.check(solid > 0, "createbuilding placed nothing in its chunk");

            new EditorEvents().onLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(editor));
            context.check(
                overlay.editSessions()
                    .get(editor.getUniqueID()) == null,
                "logging out must end the session (CORE-35)");
            keys = run(editor, "lostcities exportpart " + EXPORT_NAME + " confirm", lang, context);
            context.check(keys.contains(LangKeys.EDITOR_NOT_EDITING), "after logout exportpart answered " + keys);
            Files.deleteIfExists(file);
        }

        /** Returns the nearest building chunk to a chunk, from the layout alone. */
        private static int[] nearestBuilding(LayoutPlanner planner, int centerX, int centerZ) {
            int[][] found = new int[1][];
            ChunkSpiral.walk(centerX, centerZ, SEARCH_RADIUS, (x, z) -> {
                ChunkPlan plan = planner.plan(x, z);
                if (plan.hasBuilding() && plan.multiPos()
                    .isSingle()) {
                    found[0] = new int[] { x, z };
                    return false;
                }
                return true;
            });
            return found[0];
        }
    }

    /** Whether a world has real cities, as the subcommands decide it. */
    static boolean hasCities(WorldServer world) {
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        return overlay != null && overlay.cities() != null;
    }
}
