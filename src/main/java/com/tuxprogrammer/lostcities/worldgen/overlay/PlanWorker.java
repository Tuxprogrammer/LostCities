package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;

/**
 * Plans chunks ahead of generation on one background thread per world (D-plan-worker-1, docs/design/0005 §8, B4), so
 * the burst of samples a layout cell needs when it meets new ground is paid off the server thread.
 *
 * <ul>
 * <li>Targets come from the {@link PlanPrefetcher}, on the server thread; the worker takes them in order.</li>
 * <li>Each unit of work holds the world's {@link PlanLock}: the target's plan asked for with the probe active, and on a
 * miss one fetch. The plan lands in the shared {@link CachedCityPlan} under the lock, so the server thread reads it
 * there; it is immutable once built.</li>
 * <li>A fetch the worker may not run (VillageNames' start, which touches the world and the generator's random, and any
 * source not listed as pure) is handed to the server thread with its target ({@link #pollDeferred()}); the worker
 * drops the target meanwhile.</li>
 * <li>Nothing depends on it: the server thread plans any chunk the worker has not, inline, with the same code, and a
 * plan is pure, so the blocks are the same with the worker on or off ({@code -Dlostcitiesnh.planWorker=false}).</li>
 * </ul>
 *
 * <p>
 * The worker never plans with the probe off: that would run a deferred fetch on its own thread. Side: server, and plain
 * Java in tests.
 */
public final class PlanWorker {

    /** The JVM switch that turns the worker off; on by default. */
    public static final String PROPERTY = "lostcitiesnh.planWorker";
    /**
     * Fetch sources the worker runs itself: our height caches over a worker-owned oracle, the highway planner's hub
     * steps, and RWG2's landmark regions over a worker-owned chunk manager. Every other source is deferred.
     */
    public static final Set<String> SAFE_SOURCES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList("ColumnHeightCache", "ChunkHeights", "ChunkColumnCache", "NetworkGate", "Rwg2LandmarkOracle")));
    /** Fetches after which a target is dropped; the server thread plans it inline if it is ever needed. */
    static final int MAX_FETCHES_PER_TARGET = PlanPrefetcher.MAX_FETCHES_PER_TARGET;

    /** A fetch the worker set aside for the server thread, with the target that needed it. */
    public static final class Deferred {

        /** The target chunk's key. */
        public final long target;
        /** The fetch; run it with the probe off. */
        public final Runnable fetch;
        /** Whether it is a heavy fetch ({@link HeightProbe#missHeavy}). */
        public final boolean heavy;

        Deferred(long target, Runnable fetch, boolean heavy) {
            this.target = target;
            this.fetch = fetch;
            this.heavy = heavy;
        }
    }

    private final CachedCityPlan plan;
    private final HeightProbe probe;
    private final PlanLock lock;
    private final Runnable bindThread;
    private final LinkedBlockingDeque<Long> targets = new LinkedBlockingDeque<>();
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Deferred> deferred = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;
    private final AtomicLong planned = new AtomicLong();
    private final AtomicLong fetched = new AtomicLong();
    private final AtomicLong deferrals = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private volatile boolean warned;

    /**
     * Starts the worker of one world.
     *
     * @param name       the thread's name
     * @param plan       the world's plan cache
     * @param probe      the world's probe
     * @param lock       the world's plan lock
     * @param bindThread run first on the worker thread: binds the thread's own world inputs (D-plan-worker-2)
     */
    public PlanWorker(String name, CachedCityPlan plan, HeightProbe probe, PlanLock lock, Runnable bindThread) {
        this.plan = plan;
        this.probe = probe;
        this.lock = lock;
        this.bindThread = bindThread;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        executor.execute(this::run);
    }

    /** Returns whether the JVM switch leaves the worker on. */
    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "true"));
    }

    /**
     * Queues a target unless it is queued already.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param first  {@code true} to plan it before the others (a target whose deferred fetch just ran)
     */
    public void submit(int chunkX, int chunkZ, boolean first) {
        long key = key(chunkX, chunkZ);
        if (queued.add(key)) {
            if (first) {
                targets.addFirst(key);
            } else {
                targets.addLast(key);
            }
        }
    }

    /** Returns how many targets wait. */
    public int queued() {
        return queued.size();
    }

    /** Returns the next fetch set aside for the server thread, or {@code null}. */
    public Deferred pollDeferred() {
        return deferred.poll();
    }

    /** Returns the next deferred fetch without taking it, or {@code null}. */
    public Deferred peekDeferred() {
        return deferred.peek();
    }

    /** Stops the worker and waits a little for its current unit. Server thread, with no window open. */
    public void stop() {
        executor.shutdownNow();
        lock.openWindow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        } finally {
            lock.closeWindow();
        }
        probe.bindWorker(null, source -> false);
    }

    /** Returns a line for the budget report. */
    public String summary() {
        return "plan worker: " + planned.get()
            + " plans, "
            + fetched.get()
            + " fetches, "
            + deferrals.get()
            + " deferred to the server thread, "
            + failures.get()
            + " failed, "
            + queued()
            + " queued";
    }

    private void run() {
        probe.bindWorker(Thread.currentThread(), SAFE_SOURCES::contains);
        bindThread.run();
        try {
            while (!Thread.currentThread()
                .isInterrupted()) {
                long key = targets.take();
                try {
                    plan(key);
                } finally {
                    queued.remove(key);
                }
            }
        } catch (InterruptedException stopped) {
            // The world unloads.
        }
    }

    /** Plans one target, one unit per lock hold, until it is cached, deferred or dropped. */
    private void plan(long key) throws InterruptedException {
        int x = (int) (key >> 32);
        int z = (int) key;
        for (int fetches = 0; fetches <= MAX_FETCHES_PER_TARGET; fetches++) {
            lock.acquireUnit();
            try {
                if (unit(key, x, z)) {
                    return;
                }
            } finally {
                lock.releaseUnit();
            }
        }
        if (!warned) {
            warned = true;
            LostCities.LOG.warn("Plan worker dropped chunk ({}, {}) after {} fetches", x, z, MAX_FETCHES_PER_TARGET);
        }
    }

    /** One attempt and at most one fetch, under the lock; {@code true} when the target is done with. */
    private boolean unit(long key, int x, int z) {
        if (plan.isCached(x, z)) {
            return true;
        }
        probe.setActive(true);
        try {
            plan.chunk(x, z);
            planned.incrementAndGet();
            return true;
        } catch (HeightProbe.NotCached missing) {
            probe.setActive(false);
            boolean heavy = probe.missingIsHeavy();
            try {
                if (!probe.fetchMissing()) {
                    return true;
                }
                fetched.incrementAndGet();
                return false;
            } catch (HeightProbe.NotCached refused) {
                boolean deferredHeavy = probe.deferredIsHeavy() || heavy;
                Runnable fetch = probe.takeDeferred();
                if (fetch != null) {
                    deferrals.incrementAndGet();
                    deferred.add(new Deferred(key, fetch, deferredHeavy));
                }
                return true;
            }
        } catch (RuntimeException e) {
            // The server thread plans this chunk inline when it needs it, and logs a failure there.
            failures.incrementAndGet();
            return true;
        } finally {
            probe.setActive(false);
        }
    }

    static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
