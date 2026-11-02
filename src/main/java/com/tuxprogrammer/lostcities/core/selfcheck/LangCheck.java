package com.tuxprogrammer.lostcities.core.selfcheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks that every lang key the mod uses is defined in the lang file shipped in the jar (CONTRIBUTING § Testing,
 * "Dev self-check").
 *
 * <p>
 * Side: both (plain Java).
 */
public final class LangCheck implements SelfCheck {

    private final Collection<String> required;
    private final Set<String> defined;

    /**
     * Creates the check.
     *
     * @param required the keys the code and the config screen use
     * @param defined  the keys the lang file defines
     */
    public LangCheck(Collection<String> required, Set<String> defined) {
        this.required = required;
        this.defined = defined;
    }

    @Override
    public String name() {
        return "lang";
    }

    @Override
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        for (String key : new TreeSet<>(required)) {
            if (!defined.contains(key)) {
                problems.add("missing from en_US.lang: " + key);
            }
        }
        return problems;
    }
}
