package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.ProfileSnapshot;

/**
 * The save's pinned profile snapshots (docs/new-spec/03-terrain/index.md, "Saved data"; 0005 §7): what is pinned is
 * written, read back unchanged, and a change made after a save is never left out of the file.
 */
public class ProfileSavedDataTest {

    private static ProfileSnapshot snapshot(String profile, String groundLevel) {
        Map<String, String> values = new TreeMap<>();
        values.put("groundLevel", groundLevel);
        values.put("heightSampleSize", "3");
        Map<String, Boolean> modules = new TreeMap<>();
        modules.put("cities", true);
        modules.put("highways", false);
        return new ProfileSnapshot(
            profile,
            ProfileSnapshot.PLAN_LAYOUT,
            values,
            modules,
            "assets-hash",
            ProfileSnapshot.LAYOUT_VERSION,
            "RWG2_CONTINENTS",
            "test");
    }

    private static ProfileSavedData reload(ProfileSavedData data) {
        NBTTagCompound tag = new NBTTagCompound();
        data.writeToNBT(tag);
        ProfileSavedData loaded = new ProfileSavedData(ProfileSavedData.NAME);
        loaded.readFromNBT(tag);
        return loaded;
    }

    @Test
    public void pinnedSnapshotsAndSpawnsRoundTrip() {
        ProfileSavedData data = new ProfileSavedData(ProfileSavedData.NAME);
        ProfileSnapshot overworld = snapshot("default", "71");
        data.pin(0, overworld);
        data.setSpawn(0, 120, 72, -40);
        ProfileSavedData loaded = reload(data);
        assertEquals(overworld, loaded.snapshot(0));
        assertArrayEquals(new int[] { 120, 72, -40 }, loaded.spawn(0));
        assertNull(loaded.snapshot(7), "a dimension never pinned has no snapshot, so no cities");
        assertNull(loaded.spawn(7));
    }

    /**
     * Upstream wrote its per-dimension modes into its saved data whenever a dimension info was built, from world
     * generation threads, while {@code save} iterated the maps unlocked: a put during a save threw, and a put after the
     * iteration but before the dirty flag was cleared stayed out of the file. Here only the server thread pins, and
     * every pin and spawn marks the data dirty again, so a change made after a save is written by the next one.
     */
    @Test
    public void doesNotReproduceTERR38_aChangeAfterASaveIsWrittenByTheNextSave() {
        ProfileSavedData data = new ProfileSavedData(ProfileSavedData.NAME);
        assertFalse(data.isDirty(), "nothing to save yet");
        data.pin(0, snapshot("default", "71"));
        assertTrue(data.isDirty(), "a pin is saved");
        data.writeToNBT(new NBTTagCompound());
        // MapStorage clears the flag once the file is written.
        data.setDirty(false);
        ProfileSnapshot other = snapshot("atlantis", "71");
        data.pin(-1, other);
        assertTrue(data.isDirty(), "a pin after a save is not lost");
        data.setDirty(false);
        data.setSpawn(-1, 8, 80, 8);
        assertTrue(data.isDirty(), "a spawn after a save is not lost");
        ProfileSavedData loaded = reload(data);
        assertEquals(snapshot("default", "71"), loaded.snapshot(0));
        assertEquals(other, loaded.snapshot(-1));
        assertArrayEquals(new int[] { 8, 80, 8 }, loaded.spawn(-1));
    }

    @Test
    public void missingKeysReadAsEmpty() {
        ProfileSavedData loaded = new ProfileSavedData(ProfileSavedData.NAME);
        loaded.readFromNBT(new NBTTagCompound());
        assertNull(loaded.snapshot(0));
        assertNull(loaded.spawn(0));
    }
}
