package com.tuxprogrammer.lostcities.network.validation;

import java.util.function.Consumer;

/**
 * The "validate, then handle, never throw" pipeline both packet base classes share. Plain Java, so unit tests drive it
 * with fakes.
 *
 * <p>
 * Side: both.
 */
public final class PacketPipeline {

    private PacketPipeline() {}

    /**
     * Declares and evaluates the rules, then runs the handler only if every rule held. Nothing propagates to the
     * caller: an exception from declaring, evaluating or handling is reported to the sink and the packet is dropped.
     *
     * @param packet  the packet's class name, for the log
     * @param origin  who sent it, for the log
     * @param rules   an empty rule set to fill
     * @param declare fills {@code rules}
     * @param handler the packet's work, run only when validation passed
     * @param sink    where drops are reported
     * @return what happened
     */
    public static PacketOutcome run(String packet, String origin, Rules rules, Consumer<Rules> declare,
        Runnable handler, RejectionSink sink) {
        try {
            declare.accept(rules);
            Violation violation = rules.firstViolation();
            if (violation != null) {
                sink.rejected(packet, origin, violation);
                return PacketOutcome.rejected(violation);
            }
            handler.run();
            return PacketOutcome.accepted();
        } catch (RuntimeException e) {
            sink.handlerFailed(packet, origin, e);
            return PacketOutcome.failed();
        }
    }
}
