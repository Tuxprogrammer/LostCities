package com.tuxprogrammer.lostcities.asset;

/**
 * One thing found wrong, or worth reporting, while loading assets: which file, which field, which value, and what is
 * wrong with it (docs/new-spec/05-assets/asset-format.md, "Checks at load": every check reports the file, the field
 * and the value).
 *
 * <p>
 * Severity decides the consequence. An {@link Severity#ERROR} rejects the asset that holds it (and fails the dev
 * self-check); a {@link Severity#WARNING} is logged once and the asset is used; a {@link Severity#NOTE} is a fact the
 * self-check lists, such as a fallback block taken because a mod is absent. Side: both.
 */
public final class AssetProblem {

    /** How serious a problem is. */
    public enum Severity {
        /** The asset is rejected; the dev self-check fails. */
        ERROR,
        /** Logged once; the asset is used as far as it is valid. */
        WARNING,
        /** Reported by the self-check, not a fault (a documented fallback taken, an unreachable asset). */
        NOTE
    }

    private final Severity severity;
    private final String file;
    private final String field;
    private final String value;
    private final String message;

    /**
     * Creates a problem.
     *
     * @param severity how serious it is
     * @param file     where the asset came from, as {@link AssetFile#origin()} gives it, or a data file's name
     * @param field    the JSON path of the field, such as {@code palette[3].block}; empty for the whole file
     * @param value    the offending value, or null when there is none
     * @param message  what is wrong, in plain words
     */
    public AssetProblem(Severity severity, String file, String field, String value, String message) {
        this.severity = severity;
        this.file = file == null ? "" : file;
        this.field = field == null ? "" : field;
        this.value = value;
        this.message = message;
    }

    /** Returns the severity. */
    public Severity severity() {
        return severity;
    }

    /** Returns the file. */
    public String file() {
        return file;
    }

    /** Returns the field path, empty for the whole file. */
    public String field() {
        return field;
    }

    /** Returns the offending value, or null. */
    public String value() {
        return value;
    }

    /** Returns the message. */
    public String message() {
        return message;
    }

    /** Returns whether this problem rejects its asset. */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /** Returns one log line: {@code SEVERITY file: field: message ('value')}. */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(severity)
            .append(' ')
            .append(file);
        if (!field.isEmpty()) {
            out.append(": ")
                .append(field);
        }
        out.append(": ")
            .append(message);
        if (value != null) {
            out.append(" ('")
                .append(value)
                .append("')");
        }
        return out.toString();
    }
}
