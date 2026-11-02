package com.tuxprogrammer.lostcities.network.validation;

import java.util.function.LongSupplier;

/**
 * Passes at most a fixed number of reports per time window on to another sink and drops the rest, counting them, so a
 * client spamming bad packets cannot flood the server log. The first report after a window with drops says how many
 * were dropped.
 *
 * <p>
 * The clock is injected so tests are deterministic. Wall-clock time here only limits logging; it never affects what
 * the game does with a packet. Side: both.
 */
public final class RateLimitedSink implements RejectionSink {

    private final RejectionSink delegate;
    private final int perWindow;
    private final long windowNanos;
    private final LongSupplier clock;

    private long windowStart;
    private int inWindow;
    private long dropped;
    private boolean started;

    /**
     * Creates the sink.
     *
     * @param delegate    where reports that get through go
     * @param perWindow   the most reports per window, at least 1
     * @param windowNanos the window length, in nanoseconds, greater than zero
     * @param clock       a monotonic clock in nanoseconds, such as {@code System::nanoTime}
     */
    public RateLimitedSink(RejectionSink delegate, int perWindow, long windowNanos, LongSupplier clock) {
        if (perWindow < 1 || windowNanos <= 0) {
            throw new IllegalArgumentException("perWindow " + perWindow + " and windowNanos " + windowNanos);
        }
        this.delegate = delegate;
        this.perWindow = perWindow;
        this.windowNanos = windowNanos;
        this.clock = clock;
    }

    @Override
    public synchronized void rejected(String packet, String origin, Violation violation) {
        if (admit()) {
            delegate.rejected(packet, origin, withDropCount(violation));
        }
    }

    @Override
    public synchronized void handlerFailed(String packet, String origin, RuntimeException thrown) {
        if (admit()) {
            delegate.handlerFailed(packet, origin, thrown);
        }
    }

    /** Returns how many reports were dropped since the last one that got through. */
    public synchronized long droppedSinceLastReport() {
        return dropped;
    }

    private boolean admit() {
        long now = clock.getAsLong();
        if (!started || now - windowStart >= windowNanos) {
            started = true;
            windowStart = now;
            inWindow = 0;
        }
        if (inWindow >= perWindow) {
            dropped++;
            return false;
        }
        inWindow++;
        return true;
    }

    private Violation withDropCount(Violation violation) {
        if (dropped == 0) {
            return violation;
        }
        Violation annotated = new Violation(
            violation.reason(),
            violation.detail() + " (" + dropped + " earlier reports dropped)");
        dropped = 0;
        return annotated;
    }
}
