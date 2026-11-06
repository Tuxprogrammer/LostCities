package com.tuxprogrammer.lostcities.core.api;

import com.tuxprogrammer.lostcities.api.ILostChunkInfo;
import com.tuxprogrammer.lostcities.api.ILostCityAssetRegistry;
import com.tuxprogrammer.lostcities.api.ILostCityBuilding;
import com.tuxprogrammer.lostcities.api.ILostCityCityStyle;
import com.tuxprogrammer.lostcities.api.ILostCityInformation;
import com.tuxprogrammer.lostcities.api.ILostCityMultiBuilding;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;

/**
 * One world's {@link ILostCityInformation} over an {@link ApiSource}: the layout generation uses, read only (D9), so
 * the answers agree with generation and asking writes nothing into any cache a later chunk depends on.
 *
 * <p>
 * Server thread only. Side: server (plain Java).
 */
public final class LostCityInformationImpl implements ILostCityInformation {

    private final ApiSource source;

    /**
     * Creates the information of a world.
     *
     * @param source the world's planner, assets and settings
     */
    public LostCityInformationImpl(ApiSource source) {
        this.source = source;
    }

    @Override
    public int getDimension() {
        return source.dimension();
    }

    @Override
    public String getProfile() {
        return source.profile();
    }

    @Override
    public ILostChunkInfo getChunkInfo(int chunkX, int chunkZ) {
        return new LostChunkInfoImpl(source, chunkX, chunkZ);
    }

    @Override
    public int getGroundLevel() {
        return source.groundLevel();
    }

    @Override
    public int getRealHeight(int level) {
        return source.groundLevel() + ChunkPlan.LEVEL_HEIGHT * level;
    }

    @Override
    public ILostCityAssetRegistry<ILostCityBuilding> getBuildings() {
        return AssetRegistryImpl.buildings(source.assets());
    }

    @Override
    public ILostCityAssetRegistry<ILostCityMultiBuilding> getMultiBuildings() {
        return AssetRegistryImpl.multiBuildings(source.assets());
    }

    @Override
    public ILostCityAssetRegistry<ILostCityCityStyle> getCityStyles() {
        return AssetRegistryImpl.cityStyles(source.assets());
    }
}
