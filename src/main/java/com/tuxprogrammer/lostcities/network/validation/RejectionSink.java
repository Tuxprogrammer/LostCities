package com.tuxprogrammer.lostcities.network.validation;

/**
 * Where dropped packets are reported. The game's sink logs at debug level through a rate limiter, so a hostile client
 * cannot flood the log; tests use a recording sink.
 *
 * <p>
 * Side: both.
 */
public interface RejectionSink {

    /**
     * Reports a packet a rule rejected.
     *
     * @param packet    the packet's class name
     * @param origin    who sent it
     * @param violation the failed rule
     */
    void rejected(String packet, String origin, Violation violation);

    /**
     * Reports a packet whose rules or handler threw.
     *
     * @param packet the packet's class name
     * @param origin who sent it
     * @param thrown what was thrown
     */
    void handlerFailed(String packet, String origin, RuntimeException thrown);
}
