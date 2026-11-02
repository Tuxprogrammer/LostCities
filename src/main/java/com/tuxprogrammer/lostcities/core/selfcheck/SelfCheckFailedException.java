package com.tuxprogrammer.lostcities.core.selfcheck;

import java.util.Collections;
import java.util.List;

/**
 * Thrown by the dev self-check when it finds a problem. FML wraps an exception from {@code postInit} in a crash report,
 * which stops the dev server or client; that is the intended outcome.
 *
 * <p>
 * Side: both.
 */
public final class SelfCheckFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> problems;

    /**
     * Creates the exception.
     *
     * @param problems every problem the checks reported, one line each
     */
    public SelfCheckFailedException(List<String> problems) {
        super("Lost Cities: New Horizons dev self-check failed with " + problems.size() + " problem(s):\n"
            + String.join("\n", problems));
        this.problems = Collections.unmodifiableList(problems);
    }

    /** Returns the problems, one line each. */
    public List<String> problems() {
        return problems;
    }
}
