package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.concurrent.Semaphore;

/**
 * The lock over one world's planning objects: the plan cache, the layout planner and every cache and oracle under it
 * (D-plan-worker-1). Those caches are plain access-ordered maps, so only one thread may use them at a time.
 *
 * <p>
 * The server thread holds the lock by default, from the moment the lock is made, so no server-thread path into the
 * planner needs to remember to take it. It opens a <em>window</em> only where it is known to run no code of this mod
 * that plans: while the wrapped generator builds a chunk's base terrain, and between the end of one server tick and the
 * start of the next. The plan worker takes the lock for one short unit of work at a time (one probe attempt and at most
 * one fetch), so closing a window waits for at most one unit, never for a whole plan.
 *
 * <p>
 * Determinism: every plan is computed by exactly one thread at a time from the same pure inputs, so the order in which
 * the two threads plan never changes a plan. The semaphore is fair: a server thread closing its window is served before
 * the worker's next unit. Side: server, and plain Java in tests.
 */
public final class PlanLock {

    private final Semaphore permit = new Semaphore(1, true);
    /** Whether the server thread has a window open; only the server thread reads or writes it. */
    private boolean open;

    /** Creates the lock, held by the calling thread, which must be the server thread. */
    public PlanLock() {
        permit.acquireUninterruptibly();
    }

    /**
     * Opens a window: lets the worker plan until {@link #closeWindow()}. A window already open stays open.
     *
     * @return {@code true} when this call opened it, so the caller must close it
     */
    public boolean openWindow() {
        if (open) {
            return false;
        }
        open = true;
        permit.release();
        return true;
    }

    /** Closes the window, waiting for the worker's current unit; does nothing when no window is open. */
    public void closeWindow() {
        if (!open) {
            return;
        }
        permit.acquireUninterruptibly();
        open = false;
    }

    /** Returns whether a window is open. Server thread only. */
    public boolean isOpen() {
        return open;
    }

    /**
     * Takes the lock for one unit of the worker's work.
     *
     * @throws InterruptedException when the worker is stopped while it waits
     */
    void acquireUnit() throws InterruptedException {
        permit.acquire();
    }

    /** Gives the lock back after one unit of the worker's work. */
    void releaseUnit() {
        permit.release();
    }
}
