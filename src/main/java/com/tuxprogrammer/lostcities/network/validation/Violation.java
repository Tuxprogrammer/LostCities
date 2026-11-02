package com.tuxprogrammer.lostcities.network.validation;

/**
 * A failed validation rule: the reason and a one-line detail for the log.
 *
 * <p>
 * Side: both.
 */
public final class Violation {

    private final Rejection reason;
    private final String detail;

    /**
     * Creates a violation.
     *
     * @param reason why the packet is dropped
     * @param detail what was observed, for the log
     */
    public Violation(Rejection reason, String detail) {
        this.reason = reason;
        this.detail = detail;
    }

    /** Returns why the packet is dropped. */
    public Rejection reason() {
        return reason;
    }

    /** Returns what was observed. */
    public String detail() {
        return detail;
    }

    @Override
    public String toString() {
        return reason + ": " + detail;
    }
}
