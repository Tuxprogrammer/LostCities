package com.tuxprogrammer.lostcities.core.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.api.ILostChunkInfo;
import com.tuxprogrammer.lostcities.api.ILostCityInfo;
import com.tuxprogrammer.lostcities.api.ILostExplosion;
import com.tuxprogrammer.lostcities.api.LostChunkType;
import com.tuxprogrammer.lostcities.api.LostLandmarkZone;
import com.tuxprogrammer.lostcities.api.LostMultiBuildingInfo;
import com.tuxprogrammer.lostcities.api.LostRoadType;
import com.tuxprogrammer.lostcities.api.RailChunkType;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.MultiPos;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.damage.Explosion;

/**
 * One chunk's {@link ILostChunkInfo}, read from the immutable layout plan (D9) and the chunk's damage area, the same
 * values generation writes from. Internal types are translated into the API's own (avoids CORE-38).
 *
 * <p>
 * Immutable once built. Side: server (plain Java).
 */
final class LostChunkInfoImpl implements ILostChunkInfo {

    /** Block Y of the middle of a 16-block section, from its bottom. */
    static final int SECTION_MIDDLE = 8;

    private final ChunkPlan plan;
    private final LayoutPlanner planner;
    private final DamageArea damage;
    private final float ruinChance;
    private final String scattered;
    private final RailInfo rail;

    LostChunkInfoImpl(ApiSource source, int chunkX, int chunkZ) {
        this.planner = source.planner();
        this.plan = planner.plan(chunkX, chunkZ);
        this.damage = source.damage(chunkX, chunkZ);
        this.ruinChance = source.ruinChance();
        this.scattered = source.scattered(chunkX, chunkZ);
        this.rail = planner.infrastructure()
            .rail(chunkX, chunkZ);
    }

    @Override
    public int getChunkX() {
        return plan.chunkX();
    }

    @Override
    public int getChunkZ() {
        return plan.chunkZ();
    }

    @Override
    public boolean isCity() {
        return plan.isCity();
    }

    @Override
    public LostChunkType getType() {
        return type(plan);
    }

    /** Translates a plan into upstream's "final city content". */
    static LostChunkType type(ChunkPlan plan) {
        if (plan.zone()
            .zone() == Zone.CORE) {
            return LostChunkType.LANDMARK;
        }
        if (!plan.isCity()) {
            return LostChunkType.NONE;
        }
        if (plan.hasBuilding()) {
            return plan.multiPos()
                .isMulti() ? LostChunkType.MULTI_BUILDING : LostChunkType.BUILDING;
        }
        if (plan.isPlannedRoad()) {
            return LostChunkType.PLANNED_ROAD;
        }
        if (plan.isPredefinedStreet()) {
            return LostChunkType.PREDEFINED_STREET;
        }
        if (plan.streetType() == StreetType.PARK) {
            return plan.parkPart() == null ? LostChunkType.OPEN_LOT : LostChunkType.PARK;
        }
        return LostChunkType.STREET;
    }

    @Override
    public String getCityStyle() {
        return plan.isCity() ? plan.cityStyle() : null;
    }

    @Override
    public ILostCityInfo getCityInfo() {
        int x = plan.chunkX();
        int z = plan.chunkZ();
        if (!planner.isCityCenter(x, z)) {
            return null;
        }
        float radius = planner.cityRadius(x, z);
        String style = plan.cityStyle();
        return new ILostCityInfo() {

            @Override
            public float getCityRadius() {
                return radius;
            }

            @Override
            public String getCityStyle() {
                return style;
            }
        };
    }

    @Override
    public String getBuildingType() {
        return plan.hasBuilding() ? plan.building() : null;
    }

    @Override
    public LostMultiBuildingInfo getMultiBuildingInfo() {
        MultiPos pos = plan.multiPos();
        if (!plan.hasBuilding() || !pos.isMulti() || plan.multiBuilding() == null) {
            return null;
        }
        return new LostMultiBuildingInfo(plan.multiBuilding(), pos.x(), pos.z(), pos.width(), pos.height());
    }

    @Override
    public int getCityLevel() {
        return plan.isCity() ? plan.cityLevel() : 0;
    }

    @Override
    public int getCityGroundLevel() {
        return plan.cityGroundLevel();
    }

    @Override
    public int getNumFloors() {
        return plan.hasBuilding() ? plan.floors() : 0;
    }

    @Override
    public int getNumCellars() {
        return plan.hasBuilding() ? plan.cellars() : 0;
    }

    @Override
    public LostRoadType getRoadType() {
        return LostRoadType.valueOf(
            plan.plannedRoad()
                .name());
    }

    @Override
    public int getHighwayXLevel() {
        return plan.highwayXLevel();
    }

    @Override
    public int getHighwayZLevel() {
        return plan.highwayZLevel();
    }

    @Override
    public int getMaxHighwayLevel() {
        return plan.maxHighwayLevel();
    }

    @Override
    public RailChunkType getRailType() {
        return RailChunkType.valueOf(
            rail.type()
                .name());
    }

    @Override
    public int getRailLevel() {
        return rail.isPresent() ? rail.level() : -1;
    }

    @Override
    public LostLandmarkZone getLandmarkZone() {
        return LostLandmarkZone.valueOf(
            plan.zone()
                .zone()
                .name());
    }

    @Override
    public String getScatteredStructure() {
        return scattered;
    }

    @Override
    public int getRuinLevel() {
        return plan.hasBuilding() ? plan.ruinLevel(ruinChance) : -1;
    }

    @Override
    public float getDamage(int chunkY) {
        if (chunkY < 0 || chunkY > 15) {
            return 0f;
        }
        int x = (plan.chunkX() << 4) + SECTION_MIDDLE;
        int z = (plan.chunkZ() << 4) + SECTION_MIDDLE;
        return damage.damageAt(x, (chunkY << 4) + SECTION_MIDDLE, z);
    }

    @Override
    public List<ILostExplosion> getExplosions() {
        List<ILostExplosion> out = new ArrayList<>();
        for (Explosion explosion : damage.explosions()) {
            out.add(new ExplosionView(explosion));
        }
        return Collections.unmodifiableList(out);
    }

    /** An explosion, as the API shows it. */
    private static final class ExplosionView implements ILostExplosion {

        private final Explosion explosion;

        ExplosionView(Explosion explosion) {
            this.explosion = explosion;
        }

        @Override
        public int getRadius() {
            return explosion.radius();
        }

        @Override
        public int getCenterX() {
            return explosion.x();
        }

        @Override
        public int getCenterY() {
            return explosion.y();
        }

        @Override
        public int getCenterZ() {
            return explosion.z();
        }
    }
}
