package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;

/**
 * The pinned profile snapshots of a save, in the overworld's {@code data/lostcitiesnh.dat}, one per dimension
 * (docs/design/0005-rwg-overlay.md §7). Keys are constants; a reader tolerates missing keys with the defaults of
 * formats 1 and 2, and a dimension without a snapshot has no cities (CONTRIBUTING § Code standards, "NBT and saved
 * data").
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class ProfileSavedData extends WorldSavedData {

    /** The saved-data name, which is also the file name: {@code data/lostcitiesnh.dat}. */
    public static final String NAME = "lostcitiesnh";

    static final String KEY_FORMAT = "format";
    static final String KEY_DIMENSIONS = "dimensions";
    static final String KEY_PROFILE = "profile";
    static final String KEY_PLAN = "planSource";
    static final String KEY_VALUES = "values";
    static final String KEY_MODULES = "modules";
    static final String KEY_ASSETS = "assetSetHash";
    static final String KEY_ASSET_FILES = "assetFiles";
    static final String KEY_METALS = "decorativeMetals";
    static final String KEY_STRUCTURES = "structureSettings";
    static final String KEY_LAYOUT = "layoutVersion";
    static final String KEY_WORLD_TYPE = "worldType";
    static final String KEY_CREATED_BY = "createdBy";

    /** The spawns chosen from the profile's spawn options, per dimension, as {@code [x, y, z]} (D21). */
    static final String KEY_SPAWNS = "spawns";

    private final Map<Integer, ProfileSnapshot> snapshots = new TreeMap<>();
    private final Map<Integer, int[]> spawns = new TreeMap<>();

    /**
     * Creates empty data; the name is what {@code MapStorage} passes back when loading.
     *
     * @param name the saved-data name
     */
    public ProfileSavedData(String name) {
        super(name);
    }

    /**
     * Returns the save's data, loading it from {@code data/lostcitiesnh.dat} or creating it.
     *
     * @param world any world of the save
     * @return the data
     */
    public static ProfileSavedData get(World world) {
        MapStorage storage = world.mapStorage;
        ProfileSavedData data = (ProfileSavedData) storage.loadData(ProfileSavedData.class, NAME);
        if (data == null) {
            data = new ProfileSavedData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    /**
     * Returns a dimension's snapshot.
     *
     * @param dimension the dimension ID
     * @return the snapshot, or {@code null} when the dimension was never pinned
     */
    public ProfileSnapshot snapshot(int dimension) {
        return snapshots.get(dimension);
    }

    /**
     * Pins a dimension's snapshot and marks the data for saving.
     *
     * @param dimension the dimension ID
     * @param snapshot  the snapshot
     */
    public void pin(int dimension, ProfileSnapshot snapshot) {
        snapshots.put(dimension, snapshot);
        markDirty();
    }

    /**
     * Returns the spawn the profile's spawn options chose for a dimension when it was created.
     *
     * @param dimension the dimension ID
     * @return {@code [x, y, z]}, the block the feet stand in, or {@code null} when none was chosen
     */
    public int[] spawn(int dimension) {
        int[] spawn = spawns.get(dimension);
        return spawn == null ? null : spawn.clone();
    }

    /**
     * Records the spawn chosen for a dimension and marks the data for saving.
     *
     * @param dimension the dimension ID
     * @param x         block X
     * @param y         the feet's block Y
     * @param z         block Z
     */
    public void setSpawn(int dimension, int x, int y, int z) {
        spawns.put(dimension, new int[] { x, y, z });
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        spawns.clear();
        NBTTagCompound spawnTag = tag.getCompoundTag(KEY_SPAWNS);
        for (Object key : spawnTag.func_150296_c()) {
            int[] position = spawnTag.getIntArray((String) key);
            if (position.length == 3) {
                try {
                    spawns.put(Integer.parseInt((String) key), position);
                } catch (NumberFormatException e) {
                    // Not a dimension written by this class; skipped like any foreign key.
                    continue;
                }
            }
        }
        snapshots.clear();
        NBTTagCompound dimensions = tag.getCompoundTag(KEY_DIMENSIONS);
        for (Object key : dimensions.func_150296_c()) {
            String name = (String) key;
            int dimension;
            try {
                dimension = Integer.parseInt(name);
            } catch (NumberFormatException e) {
                continue;
            }
            snapshots.put(dimension, read(dimensions.getCompoundTag(name)));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger(KEY_FORMAT, ProfileSnapshot.FORMAT);
        NBTTagCompound dimensions = new NBTTagCompound();
        for (Map.Entry<Integer, ProfileSnapshot> entry : snapshots.entrySet()) {
            dimensions.setTag(Integer.toString(entry.getKey()), write(entry.getValue()));
        }
        tag.setTag(KEY_DIMENSIONS, dimensions);
        NBTTagCompound spawnTag = new NBTTagCompound();
        for (Map.Entry<Integer, int[]> entry : spawns.entrySet()) {
            spawnTag.setIntArray(Integer.toString(entry.getKey()), entry.getValue());
        }
        tag.setTag(KEY_SPAWNS, spawnTag);
    }

    static ProfileSnapshot read(NBTTagCompound tag) {
        Map<String, String> values = new TreeMap<>();
        NBTTagCompound valueTag = tag.getCompoundTag(KEY_VALUES);
        for (Object key : valueTag.func_150296_c()) {
            values.put((String) key, valueTag.getString((String) key));
        }
        Map<String, Boolean> modules = new TreeMap<>();
        NBTTagCompound moduleTag = tag.getCompoundTag(KEY_MODULES);
        for (Object key : moduleTag.func_150296_c()) {
            modules.put((String) key, moduleTag.getBoolean((String) key));
        }
        Map<String, String> assetFiles = new TreeMap<>();
        NBTTagCompound fileTag = tag.getCompoundTag(KEY_ASSET_FILES);
        for (Object key : fileTag.func_150296_c()) {
            assetFiles.put((String) key, fileTag.getString((String) key));
        }
        Map<String, Boolean> metals = new TreeMap<>();
        NBTTagCompound metalTag = tag.getCompoundTag(KEY_METALS);
        for (Object key : metalTag.func_150296_c()) {
            metals.put((String) key, metalTag.getBoolean((String) key));
        }
        String structures = tag.hasKey(KEY_STRUCTURES) ? tag.getString(KEY_STRUCTURES) : "";
        String plan = tag.hasKey(KEY_PLAN) ? tag.getString(KEY_PLAN) : ProfileSnapshot.PLAN_LAYOUT;
        int layout = tag.hasKey(KEY_LAYOUT) ? tag.getInteger(KEY_LAYOUT) : ProfileSnapshot.LAYOUT_VERSION;
        return new ProfileSnapshot(
            tag.getString(KEY_PROFILE),
            plan,
            values,
            modules,
            tag.hasKey(KEY_ASSETS) ? tag.getString(KEY_ASSETS) : ProfileSnapshot.NO_ASSETS,
            assetFiles,
            metals,
            layout,
            tag.getString(KEY_WORLD_TYPE),
            tag.getString(KEY_CREATED_BY)).withStructureSettings(structures);
    }

    static NBTTagCompound write(ProfileSnapshot snapshot) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY_PROFILE, snapshot.profile());
        tag.setString(KEY_PLAN, snapshot.planSource());
        NBTTagCompound values = new NBTTagCompound();
        for (Map.Entry<String, String> entry : snapshot.values()
            .entrySet()) {
            values.setString(entry.getKey(), entry.getValue());
        }
        tag.setTag(KEY_VALUES, values);
        NBTTagCompound modules = new NBTTagCompound();
        for (Map.Entry<String, Boolean> entry : snapshot.modules()
            .entrySet()) {
            modules.setBoolean(entry.getKey(), entry.getValue());
        }
        tag.setTag(KEY_MODULES, modules);
        tag.setString(KEY_ASSETS, snapshot.assetSetHash());
        NBTTagCompound files = new NBTTagCompound();
        for (Map.Entry<String, String> entry : snapshot.assetFiles()
            .entrySet()) {
            files.setString(entry.getKey(), entry.getValue());
        }
        tag.setTag(KEY_ASSET_FILES, files);
        NBTTagCompound metals = new NBTTagCompound();
        for (Map.Entry<String, Boolean> entry : snapshot.metals()
            .entrySet()) {
            metals.setBoolean(entry.getKey(), entry.getValue());
        }
        tag.setTag(KEY_METALS, metals);
        tag.setString(KEY_STRUCTURES, snapshot.structureSettings());
        tag.setInteger(KEY_LAYOUT, snapshot.layoutVersion());
        tag.setString(KEY_WORLD_TYPE, snapshot.worldType());
        tag.setString(KEY_CREATED_BY, snapshot.createdBy());
        return tag;
    }
}
