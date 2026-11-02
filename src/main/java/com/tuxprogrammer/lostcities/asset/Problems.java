package com.tuxprogrammer.lostcities.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.AssetProblem.Severity;

/**
 * Collects the problems found while loading assets, in the order they were found. A parser reports through a
 * {@link Scope} bound to one file, so every problem names its file. Not thread-safe; loading runs on one thread.
 *
 * <p>
 * Side: both.
 */
public final class Problems {

    private final List<AssetProblem> all = new ArrayList<>();

    /**
     * Records a problem.
     *
     * @param problem the problem
     */
    public void add(AssetProblem problem) {
        all.add(problem);
    }

    /**
     * Returns a scope that reports problems against one file.
     *
     * @param file the file name, as the problem lines show it
     * @return the scope
     */
    public Scope in(String file) {
        return new Scope(this, file);
    }

    /** Returns every problem, in the order found. */
    public List<AssetProblem> all() {
        return Collections.unmodifiableList(all);
    }

    /** Returns the problems of one severity, in the order found. */
    public List<AssetProblem> of(Severity severity) {
        List<AssetProblem> out = new ArrayList<>();
        for (AssetProblem problem : all) {
            if (problem.severity() == severity) {
                out.add(problem);
            }
        }
        return out;
    }

    /** Returns how many problems were recorded so far, for checking whether a step added an error. */
    public int size() {
        return all.size();
    }

    /** Returns whether any problem from position {@code since} on is an error. */
    public boolean errorsSince(int since) {
        for (int i = since; i < all.size(); i++) {
            if (all.get(i)
                .isError()) {
                return true;
            }
        }
        return false;
    }

    /** Reports problems against one file; the parsers' only way to report. */
    public static final class Scope {

        private final Problems problems;
        private final String file;

        private Scope(Problems problems, String file) {
            this.problems = problems;
            this.file = file;
        }

        /** Returns the file this scope reports against. */
        public String file() {
            return file;
        }

        /** Returns the collector this scope reports to. */
        public Problems problems() {
            return problems;
        }

        /**
         * Reports an error: the asset is rejected.
         *
         * @param field   the JSON path of the field
         * @param value   the offending value, or null
         * @param message what is wrong
         */
        public void error(String field, String value, String message) {
            problems.add(new AssetProblem(Severity.ERROR, file, field, value, message));
        }

        /**
         * Reports a warning: logged, the asset is still used.
         *
         * @param field   the JSON path of the field
         * @param value   the offending value, or null
         * @param message what is wrong
         */
        public void warning(String field, String value, String message) {
            problems.add(new AssetProblem(Severity.WARNING, file, field, value, message));
        }

        /**
         * Reports a note: a fact for the self-check, not a fault.
         *
         * @param field   the JSON path of the field
         * @param value   the value concerned, or null
         * @param message what happened
         */
        public void note(String field, String value, String message) {
            problems.add(new AssetProblem(Severity.NOTE, file, field, value, message));
        }
    }
}
