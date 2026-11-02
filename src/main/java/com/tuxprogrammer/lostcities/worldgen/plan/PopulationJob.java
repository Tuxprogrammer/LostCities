package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.Objects;

/**
 * One thing the population phase does with world access inside the chunk being populated: fix a torch's attachment,
 * create a chest or a spawner with its tile entity, or tick a command block (docs/design/0005-rwg-overlay.md §3,
 * docs/new-spec/03-terrain/pipeline-on-rwg.md "Stage map", 13b). The plan lists them as pure data, so the jobs of a
 * chunk generated in one server session and populated in the next are the same. Saplings are not a job: they are
 * written as blocks in the terrain phase and never grown (P03-10, D31).
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class PopulationJob {

    /** What a job does. */
    public enum Kind {
        /** Set a torch's metadata: standing on a block below, else attached to the first solid side (fixTorches). */
        TORCH,
        /** Place a chest and fill it from the loot category in {@link #argument()}; empty category, empty chest. */
        CHEST,
        /** Place a vanilla spawner for the entity name in {@link #argument()}. */
        SPAWNER,
        /** Schedule a 1-tick update on the command block already written there. */
        COMMAND_BLOCK
    }

    private final Kind kind;
    private final int x;
    private final int y;
    private final int z;
    private final String argument;

    private PopulationJob(Kind kind, int x, int y, int z, String argument) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y > 255) {
            throw new IllegalArgumentException("job position (" + x + ", " + y + ", " + z + ") is outside the chunk");
        }
        this.kind = Objects.requireNonNull(kind);
        this.x = x;
        this.y = y;
        this.z = z;
        this.argument = Objects.requireNonNull(argument);
    }

    /**
     * Returns a job.
     *
     * @param kind     what to do
     * @param x        local X, 0 to 15
     * @param y        block Y, 0 to 255
     * @param z        local Z, 0 to 15
     * @param argument the loot category or entity name; empty where the kind takes none
     * @return the job
     */
    public static PopulationJob of(Kind kind, int x, int y, int z, String argument) {
        return new PopulationJob(kind, x, y, z, argument);
    }

    /** Returns what the job does. */
    public Kind kind() {
        return kind;
    }

    /** Returns local X, 0 to 15. */
    public int x() {
        return x;
    }

    /** Returns block Y. */
    public int y() {
        return y;
    }

    /** Returns local Z, 0 to 15. */
    public int z() {
        return z;
    }

    /** Returns the loot category or entity name, or the empty string. */
    public String argument() {
        return argument;
    }

    @Override
    public String toString() {
        return kind + "@" + x + "," + y + "," + z + (argument.isEmpty() ? "" : ":" + argument);
    }
}
