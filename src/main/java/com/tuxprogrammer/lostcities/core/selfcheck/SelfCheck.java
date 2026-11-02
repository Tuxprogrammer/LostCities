package com.tuxprogrammer.lostcities.core.selfcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One check of the dev self-check: a named test that returns the problems it finds, as plain text with no Minecraft
 * types, so unit tests can run it too. A check reports; it never throws for a problem it finds.
 *
 * <p>
 * Side: both.
 */
public interface SelfCheck {

    /** Returns a short name for the log, for example {@code lang}. */
    String name();

    /**
     * Runs the check.
     *
     * @return one line per problem; empty when everything holds
     */
    List<String> problems();

    /**
     * Runs checks in order and collects their problems, each prefixed with its check's name. A check that throws is
     * reported as a problem rather than ending the run, so one broken check cannot hide the others.
     *
     * @param checks the checks to run
     * @return every problem found, in check order
     */
    static List<String> runAll(List<? extends SelfCheck> checks) {
        List<String> all = new ArrayList<>();
        for (SelfCheck check : checks) {
            try {
                for (String problem : check.problems()) {
                    all.add(check.name() + ": " + problem);
                }
            } catch (RuntimeException e) {
                all.add(check.name() + ": the check itself failed: " + e);
            }
        }
        return Collections.unmodifiableList(all);
    }
}
