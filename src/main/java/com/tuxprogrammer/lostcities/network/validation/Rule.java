package com.tuxprogrammer.lostcities.network.validation;

/**
 * One validation check a packet declares. Rules run in declaration order and the first violation wins, so declare
 * cheap range checks before target lookups.
 *
 * <p>
 * Side: both.
 */
@FunctionalInterface
public interface Rule {

    /** Returns {@code null} when the rule holds, or the violation when it does not. */
    Violation check();
}
