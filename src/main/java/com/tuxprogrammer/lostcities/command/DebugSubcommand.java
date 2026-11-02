package com.tuxprogrammer.lostcities.command;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.damage.Explosion;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * {@code /lostcities debug}: the layout state of the sender's chunk, summarized to the sender in six lang-key lines and
 * dumped in full to the log as {@code [LCNH-DEBUG]} lines ({@link ChunkReport}). Operator level 2, unlike upstream's
 * level 0 (avoids CORE-26); upstream printed to standard output only. The console's chunk is the overworld's chunk
 * 0, 0.
 *
 * <p>
 * Spec: docs/new-spec/01-core/commands.md ({@code debug}). Side: server.
 */
public final class DebugSubcommand implements Subcommand {

    /** Prefix of the log lines. */
    static final String TAG = "[LCNH-DEBUG] ";
    /** What a missing value shows as. */
    static final String NONE = "-";

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 0) {
            throw new WrongUsageException(LangKeys.COMMAND_USAGE);
        }
        OverlayWorld overlay = CommandSupport.cities(sender);
        ChunkCoordinates at = sender.getPlayerCoordinates();
        int chunkX = at.posX >> 4;
        int chunkZ = at.posZ >> 4;
        LayoutPlanner planner = overlay.cities()
            .planner();
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        DamageArea damage = overlay.damageArea(chunkX, chunkZ);
        int largest = 0;
        for (Explosion explosion : damage.explosions()) {
            largest = Math.max(largest, explosion.radius());
        }
        String kind = overlay.plan()
            .chunk(chunkX, chunkZ)
            .kind()
            .name();
        String profile = overlay.snapshot()
            .profile();
        List<String> lines = ChunkReport.lines(
            planner,
            profile,
            chunkX,
            chunkZ,
            kind,
            damage.explosions()
                .size(),
            overlay.settings()
                .ruinChance(),
            overlay.heights()
                .height(chunkX, chunkZ));
        for (String line : lines) {
            LostCities.LOG.info("{}dim {} chunk {},{}: {}", TAG, overlay.dimension(), chunkX, chunkZ, line);
        }
        RailInfo rail = planner.infrastructure()
            .rail(chunkX, chunkZ);
        sender.addChatMessage(
            new ChatComponentTranslation(LangKeys.DEBUG_CHUNK, chunkX, chunkZ, overlay.dimension(), profile));
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.DEBUG_CITY,
                Boolean.toString(plan.isCity()),
                orNone(plan.cityStyle()),
                plan.cityLevel(),
                plan.cityGroundLevel(),
                kind));
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.DEBUG_BUILDING,
                plan.hasBuilding() ? plan.building() : NONE,
                orNone(plan.multiBuilding()),
                plan.multiPos()
                    .toString(),
                plan.floors(),
                plan.cellars(),
                plan.ruinHeight()));
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.DEBUG_STREET,
                plan.streetType()
                    .name(),
                plan.plannedRoad()
                    .name(),
                plan.rawRoad()
                    .name(),
                plan.highwayXLevel(),
                plan.highwayZLevel(),
                rail.type()
                    .name(),
                rail.level()));
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.DEBUG_ZONE,
                plan.zone()
                    .zone()
                    .name(),
                orNone(
                    plan.zone()
                        .landmarkId()),
                plan.lane()
                    .role()
                    .name()));
        sender.addChatMessage(
            new ChatComponentTranslation(
                LangKeys.DEBUG_DAMAGE,
                damage.explosions()
                    .size(),
                largest));
        sender.addChatMessage(new ChatComponentTranslation(LangKeys.DEBUG_LOG));
    }

    static String orNone(Object value) {
        return value == null ? NONE : value.toString();
    }
}
