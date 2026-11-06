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
        /**
         * Fill the container already written there from the {@code ChestGenHooks} category in {@link #argument()}
         * (empty: leave it empty), after applying the {@link #template()}. Nothing happens when the block is gone (a
         * ruin or an explosion took it), as upstream checks the block before filling.
         */
        CHEST,
        /** Set the entity of the vanilla spawner already written there to the name in {@link #argument()}. */
        SPAWNER,
        /** Schedule a 1-tick update on the command block already written there. */
        COMMAND_BLOCK,
        /**
         * Create the tile entity of the block already written there and apply the {@link #template()}, its 1.7.10 NBT
         * as JSON with items named by registry name; an empty template only makes sure the tile entity exists (doors in
         * this pack keep their state in one).
         */
        TILE_ENTITY,
        /**
         * Complete the furnish pass's floor at {@link #y()} (its standing level): create the tile entities of the
         * palette's tile blocks found there and give each GT machine block its MetaTileEntity and facing. The
         * {@link #argument()} holds the room kind and the floor's salt (design record 0016).
         */
        FURNISH
    }

    private final Kind kind;
    private final int x;
    private final int y;
    private final int z;
    private final String argument;
    private final String template;

    private PopulationJob(Kind kind, int x, int y, int z, String argument, String template) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y > 255) {
            throw new IllegalArgumentException("job position (" + x + ", " + y + ", " + z + ") is outside the chunk");
        }
        this.kind = Objects.requireNonNull(kind);
        this.x = x;
        this.y = y;
        this.z = z;
        this.argument = Objects.requireNonNull(argument);
        this.template = Objects.requireNonNull(template);
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
        return new PopulationJob(kind, x, y, z, argument, "");
    }

    /**
     * Returns this job with a tile-entity template.
     *
     * @param json the template as JSON text, its facings already turned with the part; empty for none
     * @return the job
     */
    public PopulationJob withTemplate(String json) {
        return new PopulationJob(kind, x, y, z, argument, json);
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

    /** Returns the tile-entity template as JSON text, or the empty string. */
    public String template() {
        return template;
    }

    @Override
    public String toString() {
        return kind + "@"
            + x
            + ","
            + y
            + ","
            + z
            + (argument.isEmpty() ? "" : ":" + argument)
            + (template.isEmpty() ? "" : " " + template);
    }
}
