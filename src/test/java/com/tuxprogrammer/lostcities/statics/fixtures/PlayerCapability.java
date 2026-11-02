package com.tuxprogrammer.lostcities.statics.fixtures;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * Fixture for {@code UpstreamLeftoversTest}: per-player data attached to every player, as upstream's unused
 * {@code lostcities:spawnset} capability was (CORE-37). Never run.
 */
public class PlayerCapability implements IExtendedEntityProperties {

    /** Attaches the data to a player. */
    public static void attach(EntityPlayer player) {
        player.registerExtendedProperties("spawnset", new PlayerCapability());
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {}

    @Override
    public void loadNBTData(NBTTagCompound compound) {}

    @Override
    public void init(Entity entity, World world) {}
}
