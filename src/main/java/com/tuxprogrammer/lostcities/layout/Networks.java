package com.tuxprogrammer.lostcities.layout;

import java.util.List;

import com.tuxprogrammer.lostcities.layout.highway.CityPotential;
import com.tuxprogrammer.lostcities.layout.highway.HighwayInfo;
import com.tuxprogrammer.lostcities.layout.highway.HighwayObstacles;
import com.tuxprogrammer.lostcities.layout.highway.HighwayPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.layout.rail.RailwayInputs;
import com.tuxprogrammer.lostcities.layout.rail.RailwayLayout;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;

/**
 * A world's highway and railway networks (M06), built by the {@link LayoutPlanner} from its own city map, city levels
 * and landmark zones, and read by it through {@link InfrastructureView}: the intercity highway planner
 * ({@link HighwayPlanner}) and the railway grid ({@link RailwayLayout}). Neither reads a chunk's building plan, which
 * reads them (InfrastructureView's contract); the railway reads the highway, never the other way round.
 *
 * <p>
 * The highway's city potential is the layout's own city factor, so hubs sit on the cities that generate (new-spec 06,
 * the {@code ApproximateCityPotential} row); its obstacles are the landmarks' cores and margins (D31 P06-1, P06-2,
 * alpha review AR-10). Spec: docs/new-spec/06-highways-railways/index.md. Not thread-safe; one per world. Side:
 * both.
 */
public final class Networks implements InfrastructureView {

    /** City factors this close to zero skip the pruning bound, as upstream. */
    private static final float FACTOR_EPSILON = 0.0001f;
    /** Cells the link graph's node and link caches hold. */
    private static final int LINK_CACHE_CELLS = 1024;

    private final HighwayPlanner highways;
    private final RailwayLayout railways;
    private final boolean subways;
    /** The city link graph ({@code networkLayout=links} with grid-placed cities), or {@code null}. */
    private final CityLinkGraph links;

    /**
     * Builds the networks of a planner's world.
     *
     * @param planner  the planner, with its city map, levels and zones built
     * @param settings the network settings
     */
    Networks(LayoutPlanner planner, NetworkSettings settings) {
        CityMap cities = planner.cities;
        CityLevels levels = planner.levels;
        LandmarkZones zones = planner.zones;
        float maxMultiplier = planner.worldStyle.maxCityChanceMultiplier();
        CityPotential potential = new CityPotential() {

            @Override
            public float potential(int chunkX, int chunkZ) {
                return cities.uncachedCityFactor(chunkX, chunkZ);
            }

            @Override
            public int scoreWithUpperBound(int chunkX, int chunkZ, int required) {
                float raw = cities.rawCityFactor(chunkX, chunkZ);
                if (raw > FACTOR_EPSILON && Float.isFinite(raw)
                    && Float.isFinite(maxMultiplier)
                    && maxMultiplier >= 1.0f) {
                    float upper = Math.nextUp(raw * maxMultiplier);
                    if (CityPotential.score(upper) < required) {
                        return -1;
                    }
                }
                return CityPotential.score(cities.uncachedCityFactor(chunkX, chunkZ));
            }
        };
        HighwayObstacles obstacles = new HighwayObstacles() {

            @Override
            public boolean blocksHub(int chunkX, int chunkZ) {
                return zones.zone(chunkX, chunkZ)
                    .zone()
                    .forcesNonCity();
            }

            @Override
            public Area area(int minX, int minZ, int maxX, int maxZ) {
                List<Landmark> landmarks = zones.obstaclesIn(minX, minZ, maxX, maxZ);
                if (landmarks.isEmpty()) {
                    return (x, z) -> false;
                }
                return (x, z) -> {
                    for (Landmark landmark : landmarks) {
                        if (zones.inCoreOrRing(landmark, x, z)) {
                            return true;
                        }
                    }
                    return false;
                };
            }
        };
        this.highways = new HighwayPlanner(
            planner.seed,
            planner.dimension.stableHash(),
            settings.highways(),
            potential,
            levels::cityLevel,
            obstacles,
            settings.gate());
        RailwayInputs inputs = new RailwayInputs() {

            @Override
            public boolean isCityRaw(int chunkX, int chunkZ) {
                return cities.isCityRaw(chunkX, chunkZ);
            }

            @Override
            public int cityLevel(int chunkX, int chunkZ) {
                return levels.cityLevel(chunkX, chunkZ);
            }

            @Override
            public int highwayXLevel(int chunkX, int chunkZ) {
                return highways.info(chunkX, chunkZ)
                    .xLevel();
            }

            @Override
            public int highwayZLevel(int chunkX, int chunkZ) {
                return highways.info(chunkX, chunkZ)
                    .zLevel();
            }

            @Override
            public boolean isCore(int chunkX, int chunkZ) {
                return zones.zone(chunkX, chunkZ)
                    .zone() == Zone.CORE;
            }

            @Override
            public boolean isMargin(int chunkX, int chunkZ) {
                Zone zone = zones.zone(chunkX, chunkZ)
                    .zone();
                return zone == Zone.EDGE_RING || zone == Zone.AVOID_RING;
            }

            @Override
            public int lowestGround(int chunkX, int chunkZ) {
                int lowest = planner.terrain.height(chunkX, chunkZ);
                lowest = Math.min(lowest, planner.terrain.height(chunkX - 1, chunkZ));
                lowest = Math.min(lowest, planner.terrain.height(chunkX + 1, chunkZ));
                lowest = Math.min(lowest, planner.terrain.height(chunkX, chunkZ - 1));
                return Math.min(lowest, planner.terrain.height(chunkX, chunkZ + 1));
            }
        };
        this.subways = settings.subways();
        this.railways = new RailwayLayout(
            planner.seed,
            settings.railways(),
            inputs,
            planner.profile.groundLevel,
            planner.worldStyle.railPartHeight6());
        final CityGrid grid = cities.grid();
        if (settings.links() && grid != null) {
            // centerYieldsCity reads characteristics(...).isCity, which never reads the infrastructure view, so the
            // graph does not depend on the networks it drives (0015 §1).
            this.links = new CityLinkGraph(new CityLinkGraph.Nodes() {

                @Override
                public int cell() {
                    return grid.cell();
                }

                @Override
                public int[] cityCenter(int cellX, int cellZ) {
                    CityGrid.Center c = grid.center(cellX, cellZ);
                    if (c == null || !planner.centerYieldsCity(c.chunkX(), c.chunkZ())) {
                        return null;
                    }
                    return new int[] { c.chunkX(), c.chunkZ() };
                }
            }, LINK_CACHE_CELLS);
        } else {
            this.links = null;
        }
    }

    /**
     * Returns the city link graph, or {@code null} unless the profile reads {@code networkLayout=links} and places
     * cities on the grid.
     *
     * @return the graph, or {@code null}
     */
    public CityLinkGraph links() {
        return links;
    }

    /**
     * Returns the most memory the networks' caches can hold, estimated, in bytes, for budget B5.
     *
     * @return bytes
     */
    public long estimatedCacheBytes() {
        return highways.estimatedCacheBytes() + railways.estimatedCacheBytes();
    }

    /** Returns whether the cities' subways generate (the {@code railways} module). */
    public boolean subways() {
        return subways;
    }

    /** Returns the highway planner. */
    public HighwayPlanner highways() {
        return highways;
    }

    /** Returns the railway layout. */
    public RailwayLayout railways() {
        return railways;
    }

    /**
     * Returns the highway in a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the highway info
     */
    public HighwayInfo highway(int chunkX, int chunkZ) {
        return highways.info(chunkX, chunkZ);
    }

    /**
     * Returns the railway in a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the rail chunk
     */
    public RailChunk railChunk(int chunkX, int chunkZ) {
        return railways.rail(chunkX, chunkZ);
    }

    @Override
    public int highwayXLevel(int chunkX, int chunkZ) {
        return highways.info(chunkX, chunkZ)
            .xLevel();
    }

    @Override
    public int highwayZLevel(int chunkX, int chunkZ) {
        return highways.info(chunkX, chunkZ)
            .zLevel();
    }

    @Override
    public RailInfo rail(int chunkX, int chunkZ) {
        return railways.rail(chunkX, chunkZ)
            .info();
    }

    @Override
    public int railwayLevel() {
        return railways.railwayLevel();
    }
}
