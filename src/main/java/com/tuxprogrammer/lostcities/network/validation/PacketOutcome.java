package com.tuxprogrammer.lostcities.network.validation;

/**
 * What happened to one packet: handled, rejected by a rule, or failed inside its handler.
 *
 * <p>
 * Side: both.
 */
public final class PacketOutcome {

    /** The three possible results. */
    public enum Kind {
        /** Every rule held and the handler ran to completion. */
        ACCEPTED,
        /** A rule failed; the handler did not run. */
        REJECTED,
        /** The rules or the handler threw; the exception was contained and logged. */
        FAILED
    }

    private static final PacketOutcome ACCEPTED = new PacketOutcome(Kind.ACCEPTED, null);
    private static final PacketOutcome FAILED = new PacketOutcome(Kind.FAILED, null);

    private final Kind kind;
    private final Violation violation;

    private PacketOutcome(Kind kind, Violation violation) {
        this.kind = kind;
        this.violation = violation;
    }

    /** Returns the outcome of a handled packet. */
    public static PacketOutcome accepted() {
        return ACCEPTED;
    }

    /** Returns the outcome of a packet whose handling threw. */
    public static PacketOutcome failed() {
        return FAILED;
    }

    /**
     * Returns the outcome of a rejected packet.
     *
     * @param violation the rule that failed
     * @return the outcome
     */
    public static PacketOutcome rejected(Violation violation) {
        return new PacketOutcome(Kind.REJECTED, violation);
    }

    /** Returns which result this is. */
    public Kind kind() {
        return kind;
    }

    /** Returns the failed rule of a rejected packet, otherwise {@code null}. */
    public Violation violation() {
        return violation;
    }

    /** Returns the rejection reason of a rejected packet, otherwise {@code null}. */
    public Rejection reason() {
        return violation == null ? null : violation.reason();
    }

    @Override
    public String toString() {
        return violation == null ? kind.toString() : kind + " (" + violation + ")";
    }
}
