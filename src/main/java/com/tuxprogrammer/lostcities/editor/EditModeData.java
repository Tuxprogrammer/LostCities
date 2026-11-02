package com.tuxprogrammer.lostcities.editor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * The part editor's records of a save, in the overworld's {@code data/lostcitiesnh_edit.dat} (P-CORE-4, D31): one list
 * entry per recorded part with its integer dimension, chunk, part id, Y, transform and offset
 * (docs/new-spec/01-core/index.md, Part editor). Keys are constants; a reader tolerates missing keys with defaults and
 * skips an entry whose part id or transform cannot be read, with one log line (CONTRIBUTING § Code standards, "NBT and
 * saved data").
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class EditModeData extends WorldSavedData {

    /** The saved-data name, which is also the file name: {@code data/lostcitiesnh_edit.dat}. */
    public static final String NAME = "lostcitiesnh_edit";

    static final String KEY_DATA = "data";
    static final String KEY_DIMENSION = "level";
    static final String KEY_X = "x";
    static final String KEY_Z = "z";
    static final String KEY_PART = "part";
    static final String KEY_Y = "y";
    static final String KEY_TRANSFORM = "transform";
    static final String KEY_OFFSET_X = "ox";
    static final String KEY_OFFSET_Z = "oz";

    private final EditRecords records = new EditRecords(this::markDirty);

    /**
     * Creates empty data; the name is what {@code MapStorage} passes back when loading.
     *
     * @param name the saved-data name
     */
    public EditModeData(String name) {
        super(name);
    }

    /**
     * Returns the save's records, loading them from {@code data/lostcitiesnh_edit.dat} or creating them. The storage
     * is the save's shared one, so every dimension's records live in one file, as upstream keeps them in the
     * overworld's.
     *
     * @param world any world of the save
     * @return the data
     */
    public static EditModeData get(World world) {
        MapStorage storage = world.mapStorage;
        EditModeData data = (EditModeData) storage.loadData(EditModeData.class, NAME);
        if (data == null) {
            data = new EditModeData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    /** Returns the records. */
    public EditRecords records() {
        return records;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        records.clear();
        NBTTagList list = tag.getTagList(KEY_DATA, Constants.NBT.TAG_COMPOUND);
        int skipped = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            PartRecord record = read(entry);
            if (record == null) {
                skipped++;
                continue;
            }
            records.add(entry.getInteger(KEY_DIMENSION), entry.getInteger(KEY_X), entry.getInteger(KEY_Z), record);
        }
        if (skipped > 0) {
            LostCities.LOG.warn("{}: skipped {} part record(s) that could not be read", NAME, skipped);
        }
        // Loading is not a change.
        setDirty(false);
    }

    /** Reads one record; {@code null} when its part id or transform is unreadable. */
    static PartRecord read(NBTTagCompound entry) {
        AssetId part;
        try {
            part = AssetId.parse(entry.getString(KEY_PART));
        } catch (IllegalArgumentException e) {
            return null;
        }
        Transform transform = Transform.ROTATE_NONE;
        if (entry.hasKey(KEY_TRANSFORM)) {
            try {
                transform = Transform.valueOf(entry.getString(KEY_TRANSFORM));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return new PartRecord(
            part,
            entry.getInteger(KEY_Y),
            transform,
            entry.getInteger(KEY_OFFSET_X),
            entry.getInteger(KEY_OFFSET_Z));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (EditRecords.Entry entry : records.entries()) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setInteger(KEY_DIMENSION, entry.dimension());
            compound.setInteger(KEY_X, entry.chunkX());
            compound.setInteger(KEY_Z, entry.chunkZ());
            PartRecord record = entry.record();
            compound.setString(
                KEY_PART,
                record.part()
                    .toString());
            compound.setInteger(KEY_Y, record.y());
            if (record.transform() != Transform.ROTATE_NONE) {
                compound.setString(
                    KEY_TRANSFORM,
                    record.transform()
                        .name());
            }
            if (record.offsetX() != 0) {
                compound.setInteger(KEY_OFFSET_X, record.offsetX());
            }
            if (record.offsetZ() != 0) {
                compound.setInteger(KEY_OFFSET_Z, record.offsetZ());
            }
            list.appendTag(compound);
        }
        tag.setTag(KEY_DATA, list);
    }
}
