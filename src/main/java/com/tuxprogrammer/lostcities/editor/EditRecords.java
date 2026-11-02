package com.tuxprogrammer.lostcities.editor;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/editor/EditModeData.java
// Avoids: CORE-34

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.worldgen.city.PartRecorder;

/**
 * The part editor's records: which parts an {@code editMode} world's generation wrote into each chunk, per dimension
 * and chunk, each chunk's list in the order the parts were written (docs/original-spec/01-core/part-editor.md,
 * Recording; docs/new-spec/01-core/index.md, Part editor). The records never feed generation.
 *
 * <p>
 * Changes from upstream's {@code EditModeData}:
 * <ul>
 * <li>Avoids CORE-34: records are written only by the terrain phase on the server thread, into a sorted map, never
 * from world-generation threads into a plain {@code HashMap}; the dimension is an integer ID.</li>
 * <li>A chunk written again replaces its list instead of appending a second copy.</li>
 * <li>Each record keeps the part's transform and offset, so a rotated street part is re-placed as it was
 * written.</li>
 * </ul>
 *
 * <p>
 * Not thread-safe: the server thread only. Side: both (plain Java); {@link EditModeData} saves it.
 */
public final class EditRecords {

    /** A chunk of a dimension, ordered by dimension, then X, then Z, so saves are stable. */
    static final class ChunkKey implements Comparable<ChunkKey> {

        final int dimension;
        final int chunkX;
        final int chunkZ;

        ChunkKey(int dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public int compareTo(ChunkKey other) {
            if (dimension != other.dimension) {
                return Integer.compare(dimension, other.dimension);
            }
            if (chunkX != other.chunkX) {
                return Integer.compare(chunkX, other.chunkX);
            }
            return Integer.compare(chunkZ, other.chunkZ);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ChunkKey that && compareTo(that) == 0;
        }

        @Override
        public int hashCode() {
            return (dimension * 31 + chunkX) * 31 + chunkZ;
        }
    }

    /** One chunk's records, as the saved data lists them. */
    public static final class Entry {

        private final int dimension;
        private final int chunkX;
        private final int chunkZ;
        private final PartRecord record;

        Entry(int dimension, int chunkX, int chunkZ, PartRecord record) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.record = record;
        }

        /** Returns the dimension ID. */
        public int dimension() {
            return dimension;
        }

        /** Returns the chunk X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the chunk Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the record. */
        public PartRecord record() {
            return record;
        }
    }

    private final Map<ChunkKey, List<PartRecord>> chunks = new TreeMap<>();
    private final Runnable changed;

    /**
     * Creates empty records.
     *
     * @param changed called after every change, so the saved data is marked for saving
     */
    public EditRecords(Runnable changed) {
        this.changed = changed;
    }

    /**
     * Returns a chunk's records in the order they were written.
     *
     * @param dimension the dimension ID
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return the records; empty when there are none
     */
    public List<PartRecord> at(int dimension, int chunkX, int chunkZ) {
        List<PartRecord> list = chunks.get(new ChunkKey(dimension, chunkX, chunkZ));
        return list == null ? Collections.<PartRecord>emptyList() : Collections.unmodifiableList(list);
    }

    /**
     * Replaces a chunk's records.
     *
     * @param dimension the dimension ID
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param records   the records, in writing order; empty to forget the chunk
     */
    public void put(int dimension, int chunkX, int chunkZ, List<PartRecord> records) {
        ChunkKey key = new ChunkKey(dimension, chunkX, chunkZ);
        if (records.isEmpty()) {
            chunks.remove(key);
        } else {
            chunks.put(key, new ArrayList<>(records));
        }
        changed.run();
    }

    /**
     * Appends a record to a chunk.
     *
     * @param dimension the dimension ID
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param record    the record
     */
    public void add(int dimension, int chunkX, int chunkZ, PartRecord record) {
        chunks.computeIfAbsent(new ChunkKey(dimension, chunkX, chunkZ), k -> new ArrayList<>())
            .add(record);
        changed.run();
    }

    /** Returns every record in a stable order: dimension, chunk X, chunk Z, then writing order. */
    public List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<ChunkKey, List<PartRecord>> chunk : chunks.entrySet()) {
            ChunkKey key = chunk.getKey();
            for (PartRecord record : chunk.getValue()) {
                out.add(new Entry(key.dimension, key.chunkX, key.chunkZ, record));
            }
        }
        return out;
    }

    /** Returns how many chunks have records. */
    public int chunkCount() {
        return chunks.size();
    }

    /** Forgets every record, before a load. */
    public void clear() {
        chunks.clear();
    }

    /**
     * Returns the recorder an {@code editMode} world's terrain phase writes through.
     *
     * @param dimension the world's dimension ID
     * @return the recorder
     */
    public PartRecorder recorder(int dimension) {
        return new PartRecorder() {

            @Override
            public void beginChunk(int chunkX, int chunkZ) {
                if (chunks.remove(new ChunkKey(dimension, chunkX, chunkZ)) != null) {
                    changed.run();
                }
            }

            @Override
            public void record(int chunkX, int chunkZ, AssetId part, int y, Transform transform, int offsetX,
                int offsetZ) {
                add(dimension, chunkX, chunkZ, new PartRecord(part, y, transform, offsetX, offsetZ));
            }
        };
    }
}
