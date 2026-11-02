package com.tuxprogrammer.lostcities.network;

import org.apache.logging.log4j.Logger;

import com.tuxprogrammer.lostcities.network.validation.RejectionSink;
import com.tuxprogrammer.lostcities.network.validation.Violation;

/**
 * Writes dropped packets to the mod log: rule rejections at debug level, since a modified or lagging client produces
 * them in normal play, and handler failures at warn level with the stack trace, since those are our bugs.
 *
 * <p>
 * Side: both.
 */
final class LogRejectionSink implements RejectionSink {

    private final Logger log;

    LogRejectionSink(Logger log) {
        this.log = log;
    }

    @Override
    public void rejected(String packet, String origin, Violation violation) {
        log.debug("Dropped {} from {}: {}", packet, origin, violation);
    }

    @Override
    public void handlerFailed(String packet, String origin, RuntimeException thrown) {
        log.warn("Handling {} from {} threw; the packet was dropped", packet, origin, thrown);
    }
}
