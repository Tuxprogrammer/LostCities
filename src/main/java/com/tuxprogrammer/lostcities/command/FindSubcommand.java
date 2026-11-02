package com.tuxprogrammer.lostcities.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.worldgen.city.CityFinder;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.damage.Explosion;
import com.tuxprogrammer.lostcities.worldgen.damage.Repairs;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

/**
 * {@code /lostcities find <kind> [radius]}: prints where the nearest thing of a kind is in the sender's world, with a
 * suggested viewpoint, for screenshots and checks (test hooks only, operator level 2). Kinds: {@code center} (city
 * center), {@code street} (primary street), {@code interior} (an enterable building), {@code edge} (where a city meets
 * RWG terrain), {@code reserved} (a landmark core or its edge ring) and {@code village} (a village's gate, lane or link
 * chunk); the last two find something only where a structure oracle knows a landmark. The radius is in chunks,
 * {@value #DEFAULT_RADIUS} by default, at most {@value #MAX_RADIUS}. With {@code best} after the radius, a soft edge,
 * reserved lot or village is the one within the radius that shows the city and the landmark together most clearly,
 * with a view framing both ({@link CityFinder#findBest}), not the nearest.
 *
 * <p>
 * Output: one chat line (a lang key), and one log line for automation:
 * {@code [LCNH-FIND] kind=interior block=X,Y,Z chunk=CX,CZ view=X,Y,Z,YAW,PITCH label=...}, whose view feeds
 * {@code /lostcities view <player> X Y Z YAW PITCH}. For a street or an interior the viewpoint is moved to the nearest
 * two-block air space with a floor on the walking level, reading the generated chunk (a command may load chunks).
 *
 * <p>
 * Side: server.
 */
public final class FindSubcommand implements Subcommand {

    /** Default search radius, in chunks. */
    static final int DEFAULT_RADIUS = 48;
    /** Largest search radius, in chunks. */
    static final int MAX_RADIUS = 256;
    /** Last argument that asks for the clearest landmark view instead of the nearest. */
    static final String BEST = "best";
    /** Prefix of the log line automation reads. */
    public static final String TAG = "[LCNH-FIND] ";

    @Override
    public String name() {
        return "find";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (!DevEnvironment.testHooksEnabled()) {
            throw new CommandException(LangKeys.TEST_DISABLED);
        }
        if (args.length < 1 || args.length > 3) {
            throw new WrongUsageException(LangKeys.FIND_USAGE);
        }
        CityFinder.Kind kind = CityFinder.Kind.of(args[0]);
        boolean best = args.length == 3 && BEST.equalsIgnoreCase(args[2]);
        if (kind == null || args.length == 3 && !best) {
            throw new WrongUsageException(LangKeys.FIND_USAGE);
        }
        int radius = args.length >= 2 ? CommandBase.parseIntBounded(sender, args[1], 0, MAX_RADIUS) : DEFAULT_RADIUS;
        World world = sender.getEntityWorld();
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null || overlay.cities() == null) {
            throw new CommandException(LangKeys.FIND_NO_CITIES, world.provider.dimensionId);
        }
        ChunkCoordinates at = sender.getPlayerCoordinates();
        CityFinder finder = new CityFinder(
            overlay.cities()
                .planner(),
            overlay.plan(),
            overlay.cities()
                .structures()).withRepairs((cx, cz) -> repaired(overlay, cx, cz));
        CityFinder.Result result = best ? finder.findBest(kind, at.posX, at.posZ, radius)
            : finder.find(kind, at.posX, at.posZ, radius);
        if (result == null) {
            LostCities.LOG.info("{}kind={} none within {} chunks", TAG, kind.key(), radius);
            sender.addChatMessage(new ChatComponentTranslation(LangKeys.FIND_NONE, kind.key(), radius));
            return;
        }
        if (kind == CityFinder.Kind.STREET || kind == CityFinder.Kind.INTERIOR) {
            result = standingRoom(world, result, kind == CityFinder.Kind.INTERIOR);
        }
        LostCities.LOG.info(TAG + result.format());
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.FIND_FOUND,
                kind.key(),
                result.x(),
                result.y(),
                result.z(),
                result.label(),
                format(result.viewX()),
                format(result.viewY()),
                format(result.viewZ()),
                format(result.yaw()),
                format(result.pitch())));
    }

    /** Whether an explosion that reaches a chunk was repaired (0011 §4), for {@code find repair}. */
    private static boolean repaired(OverlayWorld overlay, int chunkX, int chunkZ) {
        long seed = overlay.world()
            .getSeed();
        Repairs repairs = overlay.plan()
            .repairs();
        if (!repairs.active()) {
            return false;
        }
        DamageArea area = DamageArea.of(seed, chunkX, chunkZ, overlay.settings(), overlay.plan());
        for (Explosion explosion : area.explosions()) {
            if (repairs.isRepaired(seed, explosion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves a walking-level viewpoint to the two-high air space (with a floor under it when asked) in the target's
     * chunk nearest to where the finder put the view.
     */
    static CityFinder.Result standingRoom(World world, CityFinder.Result result, boolean floor) {
        int feet = (int) Math.floor(result.viewY());
        int baseX = result.chunkX() << 4;
        int baseZ = result.chunkZ() << 4;
        int viewX = (int) Math.floor(result.viewX()) - baseX;
        int viewZ = (int) Math.floor(result.viewZ()) - baseZ;
        world.getChunkFromChunkCoords(result.chunkX(), result.chunkZ());
        int bestX = -1;
        int bestZ = -1;
        int best = Integer.MAX_VALUE;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                if (!world.isAirBlock(x, feet, z) || !world.isAirBlock(x, feet + 1, z)
                    || floor && world.isAirBlock(x, feet - 1, z)) {
                    continue;
                }
                int distance = (dx - viewX) * (dx - viewX) + (dz - viewZ) * (dz - viewZ);
                if (distance < best) {
                    best = distance;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        return best == Integer.MAX_VALUE ? result : result.withView(bestX + 0.5, feet, bestZ + 0.5);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        if (args.length == 3) {
            return CommandBase.getListOfStringsMatchingLastWord(args, BEST);
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> keys = new ArrayList<>();
        for (CityFinder.Kind kind : CityFinder.Kind.values()) {
            keys.add(kind.key());
        }
        return CommandBase.getListOfStringsMatchingLastWord(args, keys.toArray(new String[0]));
    }
}
