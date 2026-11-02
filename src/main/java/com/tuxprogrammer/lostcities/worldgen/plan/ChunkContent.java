package com.tuxprogrammer.lostcities.worldgen.plan;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;

/**
 * Writes a city chunk's own blocks: its building's floors, cellars and roof, or its street, park or plaza, with the
 * street decorations and stuff upstream writes in {@code doCityChunk}. The terrain phase calls it once per city chunk
 * after leveling and, for a building, after {@code makeRoomForBuilding}; street bases and borders, ruins, damage,
 * debris and vines are the terrain phase's own steps and run after it (docs/new-spec/03-terrain/pipeline-on-rwg.md,
 * stage 5).
 *
 * <p>
 * Implementations write only through the buffer, only into this chunk, and place no tile entities: chests, spawners
 * and torch fix-ups are {@link PopulationJob}s of the plan. They must be pure functions of the plan and the context's
 * random, so the chunk is the same whatever order chunks generate in. M05 implements it with M03's plan and M02's
 * parts; M04 has the synthetic test city's writer.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
@FunctionalInterface
public interface ChunkContent {

    /** Writes nothing: for chunks whose content is the leveled ground alone. */
    ChunkContent NONE = (buffer, context) -> {};

    /**
     * Writes the chunk's content.
     *
     * @param buffer  the chunk, already leveled to the plan's city ground
     * @param context the chunk's plan, its neighbors and the seeded random
     */
    void write(ChunkBuffer buffer, ContentContext context);
}
