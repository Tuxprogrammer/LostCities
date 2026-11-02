package com.tuxprogrammer.lostcities.scenario;

/**
 * An expected scenario failure: the outcome was checked and is wrong. The message says what was observed, so the
 * report diagnoses the failure on its own.
 *
 * <p>
 * Side: server.
 */
public final class ScenarioFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the failure.
     *
     * @param message what was expected and what was observed, on one line
     */
    public ScenarioFailure(String message) {
        super(message);
    }
}
