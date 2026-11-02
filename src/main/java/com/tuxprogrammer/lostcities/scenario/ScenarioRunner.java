package com.tuxprogrammer.lostcities.scenario;

import java.util.ArrayDeque;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.core.LangKeys;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Runs scenarios one after another, driven by server ticks: one run at a time, every step at the end of a server
 * tick on the server thread.
 *
 * <p>
 * Results go to the server log as {@link ScenarioReport} lines, and as translated chat to the player who started the
 * run. A run that a JVM property started ({@link ScenarioAutorun}) may stop the server when it ends.
 *
 * <p>
 * Side: server.
 */
public final class ScenarioRunner {

    /** The most ticks a scenario may ask for: ten minutes of server time. */
    public static final int MAX_TICKS = 20 * 60 * 10;

    /** The one runner of the game. */
    public static final ScenarioRunner INSTANCE = new ScenarioRunner();

    private static boolean subscribed;

    private ArrayDeque<Scenario> queue;
    private ICommandSender sender;
    private Runnable onFinished;
    private int passed;
    private int failed;
    private int arenaIndex;
    private Scenario current;
    private ScenarioContext context;
    private long startNanos;

    private ScenarioRunner() {}

    /** Subscribes the runner to server ticks, once per JVM. Called in {@code init} where the test hooks are on. */
    public static synchronized void subscribe() {
        if (subscribed) {
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        subscribed = true;
    }

    /** Returns {@code true} while a run is in progress. */
    public boolean isRunning() {
        return queue != null;
    }

    /** Drops any run in progress without reporting it; called when a server stops. */
    public void reset() {
        queue = null;
        sender = null;
        onFinished = null;
        current = null;
        context = null;
    }

    /**
     * Starts a run; the first scenario sets up at the end of the next server tick.
     *
     * @param scenarios  the scenarios, in run order
     * @param sender     who asked, for chat results; the console gets log lines only
     * @param onFinished what to do after the summary line, or {@code null}
     * @return {@code false}, doing nothing, when a run is already in progress or the runner is not subscribed
     */
    public boolean start(List<Scenario> scenarios, ICommandSender sender, Runnable onFinished) {
        if (isRunning() || !subscribed) {
            return false;
        }
        this.queue = new ArrayDeque<>(scenarios);
        this.sender = sender;
        this.onFinished = onFinished;
        this.passed = 0;
        this.failed = 0;
        this.arenaIndex = 0;
        this.current = null;
        this.context = null;
        return true;
    }

    /**
     * Advances the run by one step at the end of each server tick.
     *
     * @param event the server tick
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || queue == null) {
            return;
        }
        if (current == null) {
            Scenario next = queue.poll();
            if (next == null) {
                finishRun();
            } else {
                begin(next);
            }
            return;
        }
        try {
            context.advanceTick();
            current.tick(context);
            if (context.ticksRun() >= current.ticks() || current.finished(context)) {
                current.verify(context);
                finish(true, "");
            }
        } catch (ScenarioFailure failure) {
            finish(false, failure.getMessage());
        } catch (Exception | LinkageError | AssertionError e) {
            crashed(e);
        }
    }

    private void begin(Scenario scenario) {
        current = scenario;
        startNanos = System.nanoTime();
        WorldServer overworld = MinecraftServer.getServer()
            .worldServerForDimension(0);
        context = new ScenarioContext(overworld, arenaIndex);
        arenaIndex++;
        try {
            int ticks = scenario.ticks();
            if (ticks < 0 || ticks > MAX_TICKS) {
                finish(false, "ticks() is " + ticks + ", outside 0.." + MAX_TICKS);
                return;
            }
            scenario.setup(context);
            if (ticks == 0) {
                scenario.verify(context);
                finish(true, "");
            }
        } catch (ScenarioFailure failure) {
            finish(false, failure.getMessage());
        } catch (Exception | LinkageError | AssertionError e) {
            crashed(e);
        }
    }

    private void finish(boolean ok, String message) {
        long millis = (System.nanoTime() - startNanos) / 1_000_000L;
        String name = current.name();
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        emit(
            ScenarioReport.resultLine(ok, name, millis, message),
            LangKeys.SCENARIO_RESULT,
            ScenarioReport.verdict(ok),
            name,
            millis,
            ScenarioReport.messageOrDefault(ok, message));
        current = null;
        context = null;
    }

    private void finishRun() {
        emit(ScenarioReport.summaryLine(passed, failed), LangKeys.SCENARIO_SUMMARY, passed, failed);
        Runnable then = onFinished;
        queue = null;
        sender = null;
        onFinished = null;
        if (then != null) {
            then.run();
        }
    }

    private void emit(String logLine, String chatKey, Object... chatArgs) {
        LostCities.LOG.info(logLine);
        if (sender == null || sender instanceof MinecraftServer) {
            return;
        }
        try {
            sender.addChatMessage(new ChatComponentTranslation(chatKey, chatArgs));
        } catch (RuntimeException e) {
            // The player may have logged out mid-run; the log line above is the record.
            LostCities.LOG.debug("Could not send a scenario result to {}", sender.getCommandSenderName(), e);
        }
    }

    /** Fails the current scenario for an unexpected throwable, with its stack trace in the log. */
    private void crashed(Throwable thrown) {
        LostCities.LOG.error("Scenario {} threw", current.name(), thrown);
        String message = thrown.getMessage();
        finish(
            false,
            thrown.getClass()
                .getSimpleName() + (message == null ? "" : ": " + message));
    }
}
