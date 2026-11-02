package com.tuxprogrammer.lostcities.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/** {@link EditModeData}: the records' NBT round trip, and a reader that tolerates missing and bad keys. */
public class EditModeDataTest {

    @Test
    public void recordsSurviveASaveAndALoad() {
        EditModeData data = new EditModeData(EditModeData.NAME);
        data.records()
            .add(0, 5, -7, new PartRecord(AssetId.parse("building1_1"), 70, Transform.ROTATE_NONE, 0, 0));
        data.records()
            .add(-1, 5, -7, new PartRecord(AssetId.parse("mymod:part"), 12, Transform.MIRROR_X, 3, 4));
        assertTrue(data.isDirty());
        NBTTagCompound tag = new NBTTagCompound();
        data.writeToNBT(tag);

        EditModeData loaded = new EditModeData(EditModeData.NAME);
        loaded.readFromNBT(tag);
        assertFalse(loaded.isDirty(), "loading is not a change");
        assertEquals(
            data.records()
                .at(0, 5, -7),
            loaded.records()
                .at(0, 5, -7));
        assertEquals(
            data.records()
                .at(-1, 5, -7),
            loaded.records()
                .at(-1, 5, -7));
    }

    @Test
    public void missingKeysReadAsDefaultsAndBadEntriesAreSkipped() {
        NBTTagList list = new NBTTagList();
        NBTTagCompound minimal = new NBTTagCompound();
        minimal.setString(EditModeData.KEY_PART, "street_basic_1");
        list.appendTag(minimal);
        NBTTagCompound badPart = new NBTTagCompound();
        badPart.setString(EditModeData.KEY_PART, "a:b:c");
        list.appendTag(badPart);
        NBTTagCompound badTransform = new NBTTagCompound();
        badTransform.setString(EditModeData.KEY_PART, "street_basic_1");
        badTransform.setString(EditModeData.KEY_TRANSFORM, "SIDEWAYS");
        list.appendTag(badTransform);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(EditModeData.KEY_DATA, list);

        EditModeData loaded = new EditModeData(EditModeData.NAME);
        loaded.readFromNBT(tag);
        assertEquals(
            1,
            loaded.records()
                .entries()
                .size());
        PartRecord record = loaded.records()
            .at(0, 0, 0)
            .get(0);
        assertEquals(0, record.y());
        assertEquals(Transform.ROTATE_NONE, record.transform());
    }
}
