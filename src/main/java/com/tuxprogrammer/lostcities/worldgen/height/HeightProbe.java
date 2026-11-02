package com.tuxprogrammer.lostcities.worldgen.height;

/**
 * Lets the plan prefetcher find out which height samples a plan needs without paying for them inside a chunk's
 * phase (docs/design/0005-rwg-overlay.md §8, B4). While a probe is active, a height cache asked
 * for a value it does not hold records how to fetch it and throws {@link NotCached} instead of calling the oracle.
 * The prefetcher catches it, fetches that one value with the probe off, and asks again.
 *
 * <p>
 * Determinism: the throw only ever abandons a computation. Every cache on the way between a plan and a height
 * stores a value only once it is complete, so an abandoned computation leaves nothing behind, and the values that
 * end up cached are the ones the oracle gives anyway. Nothing that catches {@link RuntimeException} on that way may
 * swallow {@link NotCached}; see {@link #isProbe(Throwable)}.
 *
 * <p>
 * One per world; not thread-safe. Server thread only. Side: server, and plain Java in tests.
 */
public final class HeightProbe {

    /** Thrown while probing by a height cache that does not hold the value asked for. Carries no stack trace. */
    public static final class NotCached extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private NotCached() {
            super("height not cached while probing", null, false, false);
        }
    }

    /** One thrown instance: the throw is control flow, and its stack trace would cost more than the probe. */
    private static final NotCached NOT_CACHED = new NotCached();

    private boolean active;
    private Runnable missing;
    private boolean missingHeavy;

    /** Returns whether a probe is active. */
    public boolean active() {
        return active;
    }

    /**
     * Starts or ends a probe.
     *
     * @param active {@code true} to start
     */
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            missing = null;
        }
    }

    /**
     * Records the fetch of a missing value and returns the exception to throw.
     *
     * @param fetch computes and caches the value; run with the probe off
     * @return the exception
     */
    public NotCached miss(Runnable fetch) {
        missing = fetch;
        missingHeavy = false;
        return NOT_CACHED;
    }

    /**
     * Records the fetch of a missing value that costs several milliseconds and cannot be split (a structure
     * generator's own start constructor), and returns the exception to throw. The prefetcher runs such a fetch alone
     * at the start of a tick.
     *
     * @param fetch computes and caches the value; run with the probe off
     * @return the exception
     */
    public NotCached missHeavy(Runnable fetch) {
        missing = fetch;
        missingHeavy = true;
        return NOT_CACHED;
    }

    /** Returns whether the value the last probe missed is a heavy one ({@link #missHeavy}). */
    public boolean missingIsHeavy() {
        return missing != null && missingHeavy;
    }

    /** Forgets the value the last probe missed; the next probe of the same plan misses it again. */
    public void dropMissing() {
        missing = null;
    }

    /**
     * Fetches the value the last probe missed, with the probe off.
     *
     * @return {@code false} when nothing was missing
     */
    public boolean fetchMissing() {
        Runnable fetch = missing;
        missing = null;
        if (fetch == null) {
            return false;
        }
        boolean was = active;
        active = false;
        try {
            fetch.run();
        } finally {
            active = was;
        }
        return true;
    }

    /**
     * Returns whether a throwable is a probe's miss, which code that catches {@link RuntimeException} on the way from a
     * plan to a height must rethrow.
     *
     * @param error the throwable
     * @return {@code true} for {@link NotCached}
     */
    public static boolean isProbe(Throwable error) {
        return error instanceof NotCached;
    }
}
