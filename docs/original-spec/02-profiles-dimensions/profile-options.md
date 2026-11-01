# Profile options

Every option of `LostCityProfile` at `f18a094d`: 163 options in 5 categories, plus the root `public` flag ([profile-system.md](profile-system.md#data-format)). This is a catalog that sits alongside the [profile system sheet](profile-system.md); the per-profile values are cross-tabulated in [builtin-profiles.md](builtin-profiles.md).

How to read the table:

- **Key** is the JSON key inside its category object. **Field** is the Java field (short names for the few private ones).
- **Default** is the class default, which is also the `default` profile's value.
- **Range** is the min..max or allowed list the code declares. It is enforced only when a user edits the value in the GUI, never when a profile file is loaded (`PROF-04`). "checked" means `init` also throws when the value is outside the range (see [Validation messages](#validation-messages)).
- **Read by** names the area that owns the consumer. *config-only* means nothing reads the field outside the profile and GUI code; *dead* means a reader exists only in commented-out code.
- **Built-in overrides** lists profiles whose code sets a different value. A profile setting the same value as the default is listed with "(same)". Profiles not listed keep the default.
- Units: chances are probabilities 0–1 as the code compares them (`random.nextFloat() < chance`); heights are absolute block Y; spacings and distances are in chunks unless marked blocks.

## Category `lostcity` (107 options)

### Identity and blocks

| Key | Field | Type | Default | Range | Meaning | Read by | Built-in overrides |
|---|---|---|---|---|---|---|---|
| `description` | `description` | string | `Default generation, common cities, explosions` | – | First GUI line for the profile. | 09-client-ui | every built-in except `default` and `largecities`, see [builtin-profiles.md](builtin-profiles.md) |
| `extraDescription` | `extraDescription` | string | `""` | – | Second GUI line. | 09-client-ui | `cavern`, `nodamage`, `floating`, `space`, `wasteland` |
| `warning` | `warning` | string | `""` | – | Warning line in the GUI. | 09-client-ui | `cavern`, `floating`, `space`, `biosphere_caves`, `biosphere`, `atlantis` |
| `worldStyle` | `worldStyle` | string | `standard` | asset name | The world style asset this profile uses ([world-styles.md](world-styles.md)). An unknown name crashes dimension setup (`PROF-13`). | this area | none (every built-in uses `standard`) |
| `icon` | `iconFile` | string | `""` | path in namespace `lostcities` | GUI icon, 64 × 64. Empty means no icon. | 09-client-ui | all public built-ins, see [builtin-profiles.md](builtin-profiles.md) |
| `liquidBlock` | `LIQUID_BLOCK` | block id | `minecraft:water` | registry name | The block used as "liquid" in parts and water-level filling. Unknown names fall back to water with an error log. | 03-terrain, 04-city-layout | none |
| `baseBlock` | `BASE_BLOCK` | block id | `minecraft:stone` | registry name | The block used as terrain base by the terrain driver and corridors. Unknown names fall back to stone. | 03-terrain, 04-city-layout | none |

### Spawn selection (read by 01-core, `ForgeEventHandlers`)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `spawnBiome` | `SPAWN_BIOME` | string | `""` | biome id | Player always spawns in this biome when set. Unknown id logs `Cannot find biome '<id>' for the player to spawn in !`. | none |
| `spawnCity` | `SPAWN_CITY` | string | `""` | predefined city name | Player always spawns in this predefined city when set. | none |
| `spawnSphere` | `SPAWN_SPHERE` | string | `""` | sphere name, `<in>`, `<out>` | Spawn in this predefined sphere; `<in>` means any sphere, `<out>` means outside all spheres. | `space`, `biosphere_caves`, `biosphere` = `<in>` |
| `spawnNotInBuilding` | `SPAWN_NOT_IN_BUILDING` | boolean | `false` | – | Never spawn inside a building. | none |
| `forceSpawnInBuilding` | `FORCE_SPAWN_IN_BUILDING` | boolean | `false` | – | Always spawn inside a building. | none |
| `forceSpawnBuildings` | `FORCE_SPAWN_BUILDINGS` | string[] | `[]` | building names | With `forceSpawnInBuilding`: allowed buildings; empty means any. | none |
| `forceSpawnParts` | `FORCE_SPAWN_PARTS` | string[] | `[]` | part names | With `forceSpawnInBuilding`: allowed parts; empty means any. | none |
| `spawnCheckRadius` | `SPAWN_CHECK_RADIUS` | int (blocks) | 200 | 1..100000 | Starting radius of the outward spiral search from world center. | none |
| `spawnRadiusIncrease` | `SPAWN_RADIUS_INCREASE` | int (blocks) | 100 | 1..100000 | Radius added each time the search fails. | none |
| `spawnCheckAttempts` | `SPAWN_CHECK_ATTEMPTS` | int (chunks) | 20000 | 1..1000000 | Chunks examined before the search gives up with an error. | none |

### Terrain look and damage layers (read by 03-terrain unless noted)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `scatteredChanceMultiplier` | `SCATTERED_CHANCE_MULTIPLIER` | float | 1.0 | 0..100 | Multiplies the world style's scattered-structure chance; 0 disables scattered structures (07-scattered). | none |
| `terrainFixLowerMinOffset` | `TERRAIN_FIX_LOWER_MIN_OFFSET` | int (blocks) | -4 | -40..40 | Lower mesh for raising terrain next to cities, minimum offset from city base level. | none |
| `terrainFixLowerMaxOffset` | `TERRAIN_FIX_LOWER_MAX_OFFSET` | int (blocks) | -3 | -40..40 | Same, maximum offset. | none |
| `terrainFixUpperMinOffset` | `TERRAIN_FIX_UPPER_MIN_OFFSET` | int (blocks) | -1 | -40..40 | Upper mesh for lowering terrain next to cities, minimum offset. | none |
| `terrainFixUpperMaxOffset` | `TERRAIN_FIX_UPPER_MAX_OFFSET` | int (blocks) | 1 | -40..40 | Same, maximum offset. | none |
| `vineChance` | `VINE_CHANCE` | float | 0.009 | 0..1 | Chance that an outside building block gets a vine (`ChunkFixer`). Below 0.000001 skips vines entirely. Shares a random stream with loot (`LOOT-05`). | `ancient` 0.1, `wasteland` 0.003, `bio_wasteland` 0.0, `void_outside` 0.0 |
| `randomLeafBlockChance` | `CHANCE_OF_RANDOM_LEAFBLOCKS` | float | 0.1 | 0..1 | Chance of leaf blocks at the border between a building and a street. | `ancient` 0.05, `wasteland` 0.01, `bio_wasteland` 0.0, `void_outside` 0.0 |
| `randomLeafBlockThickness` | `THICKNESS_OF_RANDOM_LEAFBLOCKS` | int | 2 | 1..8 | How thick those leaf patches look from the building side. | `ancient` 6 |
| `avoidFoliage` | `AVOID_FOLIAGE` | boolean | `false` | – | Parks get no trees or flowers (saplings and flowers become air); also blocks water filling at the water level. City styles may override it ([effective-city-settings.md](effective-city-settings.md)). 04-city-layout. | `wasteland`, `bio_wasteland`, `void_outside` = `true` |
| `landscapeType` | `LANDSCAPE_TYPE` | string → `LandscapeType` | `default` | `default`, `floating`, `space`, `cavern` (checked against all 6 names) | Landscape kind ([landscape-types.md](landscape-types.md)). The declared list omits `spheres` and `cavernspheres` (`PROF-15`). | `cavern` = cavern, `floating` = floating, `space` = space, `biosphere_caves` = cavernspheres, `biosphere` = spheres |
| `rubbleLayer` | `RUBBLELAYER` | boolean | `true` | – | Use the noise-driven dirt/stone/sand and leaf rubble layer that makes cities look overgrown. | `nodamage` false, `space` false, `biosphere_caves` false, `biosphere` false, `ancient` (same), `wasteland` (same), `bio_wasteland` (same), `void_outside` (same) |
| `rubbleDirtScale` | `RUBBLE_DIRT_SCALE` | float | 3.0 | 0..100 | Noise scale of the dirt rubble; smaller is larger patches; 0 disables. | `ancient` 2.0, `wasteland` 2.0, `bio_wasteland` 2.0, `void_outside` 2.0 |
| `rubbleLeaveScale` | `RUBBLE_LEAVE_SCALE` | float | 6.0 | 0..100 | Noise scale of the leaf rubble; 0 disables. | `ancient` 2.0, `wasteland` 0.0, `bio_wasteland` 0.0, `void_outside` 0.0 |
| `ruinChance` | `RUIN_CHANCE` | float | 0.05 | 0..1 | Chance a building is ruined (04-city-layout, `BuildingInfo`). A predefined building can prevent ruins. | `nodamage` 0, `biosphere_caves` 0.7, `biosphere` 0.7, `rarecities` 0, `tallbuildings` 0.01, `ancient` 0.9, `wasteland` 0.5, `atlantis` 0.1, `bio_wasteland` 1.0, `void_outside` 1.0 |
| `ruinMinlevelPercent` | `RUIN_MINLEVEL_PERCENT` | float | 0.8 | 0..1 | Lowest start of the ruin cut, as a fraction of building height. The cut is `min + (max - min) * r`. | `biosphere_caves` 0.3, `biosphere` 0.3, `ancient` 0.0, `wasteland` 0.5, `bio_wasteland` 0.1, `void_outside` 0.1 |
| `ruinMaxlevelPercent` | `RUIN_MAXLEVEL_PERCENT` | float | 1.0 | 0..1 | Highest start of the ruin cut. | `biosphere_caves` 0.8, `biosphere` 0.8, `ancient` 0.9, `wasteland` 0.9, `bio_wasteland` 0.4, `void_outside` 0.4 |
| `bedrockLayer` | `BEDROCK_LAYER` | int (blocks) | 1 | 0..10 | Bedrock layer thickness, counted up from the world's minimum build height, written into city chunks of `default` and `spheres` landscapes; 0 writes none. | none |
| `avoidWater` | `AVOID_WATER` | boolean | `false` | – | The profile's liquid in parts becomes air. | `wasteland`, `bio_wasteland`, `void_outside` = `true` |

### Levels (read by 03-terrain and 04-city-layout)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `groundLevel` | `GROUNDLEVEL` | int (Y) | 71 | 2..256 | City level 0 ground floor Y. City ground for level n is `groundLevel + 6 × n`. Also the base of the condition `level` value (08-loot-mobs-blocks). | `cavern` 40, `floating` 50, `space` 71 (same), `bio_wasteland` 71 (same), `void_outside` 71 (same) |
| `seaLevel` | `SEALEVEL` | int (Y) | -1 | -1..256 | Water level for part filling; -1 uses the chunk generator's sea level (63 in the 1.20 overworld). Not used by explosion damage (`PROF-14`). | `cavern` 32, `atlantis` 89 |

### Loot, spawners, lighting (read by 08-loot-mobs-blocks)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `chestWithoutLootChance` | `CHEST_WITHOUT_LOOT_CHANCE` | float | 0.2 | 0..1 | Chance a loot chest gets no loot table. | none |
| `buildingWithoutLootChance` | `BUILDING_WITHOUT_LOOT_CHANCE` | float | 0.2 | 0..1 | Chance a single (non-multi) building has no loot and no spawners. | none |
| `generateSpawners` | `GENERATE_SPAWNERS` | boolean | `true` | – | Place spawners; otherwise spawner positions become air. | `safe` false |
| `generateLoot` | `GENERATE_LOOT` | boolean | `true` | – | Give chests a loot table; otherwise chests stay empty. | `safe` false |
| `generateLighting` | `GENERATE_LIGHTING` | boolean | `false` | – | Keep palette torches and add bridge lighting; otherwise torch entries become air (04-city-layout). | `cavern`, `space`, `biosphere_caves`, `biosphere`, `safe`, `largecities` = `true` |

### Buildings (read by 04-city-layout)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `buildingChance` | `BUILDING_CHANCE` | float | 0.3 | 0..1 | Chance a city chunk holds a building rather than a street. City styles may override. | `space` 0.3 (same), `biosphere_caves` 0.3 (same), `biosphere` 0.3 (same), `largecities` 0.4 |
| `buildingMinFloors` | `BUILDING_MINFLOORS` | int | 0 | 0..60 | Minimum floors above ground; 0 means the first floor only. The minimum used is `buildingMinFloors + 1`. | `tallbuildings` 4 |
| `buildingMaxFloors` | `BUILDING_MAXFLOORS` | int | 8 | 0..60 | Cap on floors above ground. | `tallbuildings` 19, `largecities` 9 |
| `buildingMinFloorsChance` | `BUILDING_MINFLOORS_CHANCE` | int | 4 | 1..60 | Floors are `minFloors + random(minChance + (cityFactor + 0.1) × (maxChance - minChance)) + 1`, then clamped. A bound below 1 crashes (`PROF-06`). | `tallbuildings` 8 |
| `buildingMaxFloorsChance` | `BUILDING_MAXFLOORS_CHANCE` | int | 6 | 1..60 | See above. | `tallbuildings` 15, `largecities` 7 |
| `buildingMinCellars` | `BUILDING_MINCELLARS` | int | 0 | 0..20 | Minimum cellars below ground (raised to the building asset's own minimum). | none |
| `buildingMaxCellars` | `BUILDING_MAXCELLARS` | int | 3 | 0..20 | Maximum cellars; the code adds the chunk's city level to it before applying city-style constraints. | `floating` 1, `bio_wasteland` 1, `void_outside` 1 |
| `buildingDoorwayChance` | `BUILDING_DOORWAYCHANCE` | float | 0.6 | 0..1 | Chance of a doorway on each side and floor toward an adjacent city chunk. | none |
| `buildingFrontChance` | `BUILDING_FRONTCHANCE` | float | 0.2 | 0..1 | Chance a building gets a front part toward an adjacent street. City styles may override. | none |
| `multiUseCorner` | `MULTI_USE_CORNER` | boolean | `false` | – | Multibuildings take the top-left chunk's city level (true) or the average of their chunks (false). | none |
| `useAvgHeightmap` | `USE_AVG_HEIGHTMAP` | boolean | `false` | – | City level from the average of the chunk's height and up to four sampled city neighbors. Only active when the mod config `heightSampleSize` is above 2. | none |

### Streets, parks, bridges, corridors (read by 04-city-layout)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `parkChance` | `PARK_CHANCE` | float | 0.2 | 0..1 | Chance a non-building chunk is a park (legacy streets). City styles may override. | none |
| `openLotParkChance` | `OPEN_LOT_PARK_CHANCE` | float | 0.8 | 0..1 | Chance a hierarchical open lot gets a park part. City styles may override via `profile_overrides`. | none |
| `corridorChance` | `CORRIDOR_CHANCE` | float | 0.7 | 0..1 | Chance a chunk may hold an underground corridor (also needs neighbors). City styles may override. | none |
| `bridgeChance` | `BRIDGE_CHANCE` | float | 0.7 | 0..1 | Chance a chunk may hold a bridge on each axis (also needs neighbors). | none |
| `plannedPrimaryBridgeChance` | `PLANNED_PRIMARY_BRIDGE_CHANCE` | float | 1.0 | 0..1, checked | Chance an eligible hierarchical primary-road water crossing becomes one bridge. | none |
| `plannedPrimaryBridgeMaxLength` | `PLANNED_PRIMARY_BRIDGE_MAX_LENGTH` | int (chunks) | 12 | 1..64, checked | Longest water gap a primary-road bridge spans. | none |
| `fountainChance` | `FOUNTAIN_CHANCE` | float | 0.05 | 0..1 | Chance a street chunk holds a fountain. City styles may override. | none |
| `bridgeSupports` | `BRIDGE_SUPPORTS` | boolean | `true` | – | Bridges get supports where needed. | `space` false, `void_outside` false |
| `parkElevation` | `PARK_ELEVATION` | boolean | `true` | – | Parks get one extra layer of elevation. City styles may override. | none |
| `parkBorder` | `PARK_BORDER` | boolean | `true` | – | Park borders use the street block as base. City styles may override. | none |
| `parkStreetThreshold` | `PARK_STREET_THRESHOLD` | int | 3 | 0..8 | How many surrounding streets a chunk needs to become a park. City styles may override. | none |
| `streetGenerationMode` | `STREET_GENERATION_MODE` | string → enum | `HIERARCHICAL_GRID_V1` | `LEGACY`, `HIERARCHICAL_GRID_V1`, checked | Street layout requested for a new world; existing worlds keep their persisted mode ([profile-selection.md](profile-selection.md#persisted-generation-modes)). | none |
| `primaryRoadSpacingX` | `PRIMARY_ROAD_SPACING_X` | int (chunks) | 8 | 8..128, checked | Spacing of candidate north/south primary roads. | none |
| `primaryRoadSpacingZ` | `PRIMARY_ROAD_SPACING_Z` | int (chunks) | 8 | 8..128, checked | Spacing of candidate east/west primary roads. | none |
| `primaryRoadOptionalChance` | `PRIMARY_ROAD_OPTIONAL_CHANCE` | float | 0.45 | 0..1, checked | Chance a non-forced candidate primary road is enabled. | none |
| `primaryRoadForceEvery` | `PRIMARY_ROAD_FORCE_EVERY` | int | 4 | 1..16, checked | Every Nth candidate primary road is forced, capping the gap. | none |
| `secondaryRoadMinCountX` | `SECONDARY_ROAD_MIN_COUNT_X` | int | 0 | 0..128, checked | Minimum north/south secondary roads inside a primary block. | none |
| `secondaryRoadMaxCountX` | `SECONDARY_ROAD_MAX_COUNT_X` | int | 2 | 0..128, checked | Maximum north/south secondary roads. | none |
| `secondaryRoadMinCountZ` | `SECONDARY_ROAD_MIN_COUNT_Z` | int | 0 | 0..128, checked | Minimum east/west secondary roads. | none |
| `secondaryRoadMaxCountZ` | `SECONDARY_ROAD_MAX_COUNT_Z` | int | 2 | 0..128, checked | Maximum east/west secondary roads. | none |
| `minimumRoadSeparation` | `MINIMUM_ROAD_SEPARATION` | int (chunks) | 4 | 2..32, checked | Minimum distance between parallel secondary roads. | none |
| `minimumRoadEdgeDistance` | `MINIMUM_ROAD_EDGE_DISTANCE` | int (chunks) | 3 | 2..32, checked | Minimum distance between a secondary road and its bounding primary road. | none |
| `tertiaryRoadChance` | `TERTIARY_ROAD_CHANCE` | float | 0.40 | 0..1, checked | Chance a subdivided block gets one short tertiary access road. | none |
| `tertiaryRoadMinLength` | `TERTIARY_ROAD_MIN_LENGTH` | int (chunks) | 2 | 1..16, checked (min ≥ 1, min ≤ max) | Minimum tertiary road length. | none |
| `tertiaryRoadMaxLength` | `TERTIARY_ROAD_MAX_LENGTH` | int (chunks) | 5 | 1..32, checked (max ≤ 32) | Maximum tertiary road length. | none |
| `multiBuildingStreetConflict` | `MULTI_BUILDING_STREET_CONFLICT` | string → enum | `OVERRIDE_MINOR` | `BLOCK_ALL`, `OVERRIDE_MINOR`, `OVERRIDE_ALL`, checked | How random multibuildings meet planned roads: `BLOCK_ALL` lets any planned road block them, `OVERRIDE_MINOR` only primary roads, `OVERRIDE_ALL` none. Ignored by legacy streets. | none |

### Railways (read by 06-highways-railways)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `railwayDungeonChance` | `RAILWAY_DUNGEON_CHANCE` | float | 0.01 | 0..1 | Chance a chunk next to a railway holds a railway dungeon. | none |
| `railwaysCanEnd` | `RAILWAYS_CAN_END` | boolean | `false` | – | A station spot without a city above gets an end part when one side has no connection. | `floating`, `space`, `biosphere_caves`, `biosphere`, `rarecities` = `true` |
| `railwaysEnabled` | `RAILWAYS_ENABLED` | boolean | `true` | – | Railways on or off (stations still generate when off). | `cavern`, `floating`, `space`, `biosphere_caves`, `biosphere`, `void_outside` = `false` |
| `railwayStationsEnabled` | `RAILWAY_STATIONS_ENABLED` | boolean | `true` | – | Railway stations on or off. | `floating`, `space`, `biosphere_caves`, `biosphere`, `void_outside` = `false` |
| `railwaySurfaceStationsEnabled` | `RAILWAY_SURFACE_STATIONS_ENABLED` | boolean | `true` | – | Surface stations allowed; otherwise underground only. | none |
| `railwayLevelOffset` | `RAILWAY_LEVEL_OFFSET` | int (× 6 blocks) | 0 | -8..2 | Vertical offset of the underground railway. | none |
| `railwaySpacingNorthSouth` | `RAILWAY_SPACING_NORTH_SOUTH` | int (chunks) | 10 | 4..128, even, checked | North-south distance between east-west tracks. | none |
| `railwaySpacingEastWest` | `RAILWAY_SPACING_EAST_WEST` | int (chunks) | 10 | 4..128, even, checked | East-west distance between north-south tracks. | none |

### Highways (read by 06-highways-railways)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `highwayRequiresTwoCities` | `HIGHWAY_REQUIRES_TWO_CITIES` | boolean | `true` | – | A legacy highway needs a city at both ends; false needs one. | `rarecities` false |
| `highwayLevelFromCities` | `HIGHWAY_LEVEL_FROM_CITIES_MODE` | int | 3 | 0..4 | Highway level: 0 first endpoint city, 1 minimum of both, 2 maximum, 3 average, 4 `highwayNetworkLevel`. | none |
| `highwayDistanceMask` | `HIGHWAY_DISTANCE_MASK` | int | 7 | 0..2147483647 | Legacy highway reach mask, meant to be a power of two minus one (not checked); 0 disables highways. | `floating` 15, `space` 0, `biosphere_caves` 0, `biosphere` 0, `void_outside` 0 |
| `highwayMainPerlinScale` | `HIGHWAY_MAINPERLIN_SCALE` | float | 50.0 | 1..1000 | Legacy highway noise scale along the main axis. | none |
| `highwaySecondaryPerlinScale` | `HIGHWAY_SECONDARYPERLIN_SCALE` | float | 10.0 | 1..1000 | Legacy highway noise scale on the secondary axis. | none |
| `highwayPerlinFactor` | `HIGHWAY_PERLIN_FACTOR` | float | 2.0 | -100..100 | Legacy highway noise threshold; 0 gives about 50 %. Highways only lie on chunk multiples of 8. | none |
| `highwaySupports` | `HIGHWAY_SUPPORTS` | boolean | `true` | – | Highways get supports where needed. | `floating`, `space`, `void_outside` = `false` |
| `highwayGenerationMode` | `HIGHWAY_GENERATION_MODE` | string → enum | `INTERCITY_NETWORK_V1` | `LEGACY`, `INTERCITY_NETWORK_V1`, checked | Highway layout requested for a new world; existing worlds keep their persisted mode. | none |
| `highwayPlanningCellSize` | `HIGHWAY_PLANNING_CELL_SIZE` | int (chunks) | 128 | 32..512 | Size of one hub-planning cell. Must be ≥ `highwayHubSampleSpacing` (checked). | none |
| `highwayHubSampleSpacing` | `HIGHWAY_HUB_SAMPLE_SPACING` | int (chunks) | 16 | 1..512 | Spacing of city-potential samples inside a cell. | none |
| `highwayHubMinimumPotential` | `HIGHWAY_HUB_MINIMUM_POTENTIAL` | float | 0.20 | 0..1 | Minimum height- and biome-adjusted city potential for a hub. | none |
| `highwayHubSearchRadiusCells` | `HIGHWAY_HUB_SEARCH_RADIUS_CELLS` | int (cells) | 2 | 0..8 | Cell radius in which a hub looks for others. | none |
| `highwayMinimumHubDistance` | `HIGHWAY_MINIMUM_HUB_DISTANCE` | int (chunks) | 64 | 0..4096 | Minimum Euclidean distance between connected hubs. Must be ≤ the maximum (checked). | none |
| `highwayMaximumHubDistance` | `HIGHWAY_MAXIMUM_HUB_DISTANCE` | int (chunks) | 320 | 0..4096 | Maximum Euclidean distance between connected hubs. | none |
| `highwayMaximumConnectionsPerHub` | `HIGHWAY_MAXIMUM_CONNECTIONS_PER_HUB` | int | 2 | 1..8 | Maximum accepted connections per hub. | none |
| `highwayMinimumRouteLength` | `HIGHWAY_MINIMUM_ROUTE_LENGTH` | int (chunks) | 40 | 0..4096 | Minimum Manhattan length of an inter-city route. | none |
| `highwayRouteCityPenalty` | `HIGHWAY_ROUTE_CITY_PENALTY` | float | 1.0 | 0..1000 | Weight of city potential when choosing an L-route bend. | none |
| `highwayNetworkLevel` | `HIGHWAY_NETWORK_LEVEL` | int (city level) | 0 | 0..32 | Fixed highway level when `highwayLevelFromCities` is 4. | none |

### Modes and switches

| Key | Field | Type | Default | Range | Meaning | Read by | Built-in overrides |
|---|---|---|---|---|---|---|---|
| `editMode` | `EDITMODE` | boolean | `false` | – | World is in part-edit mode; gates the edit commands. | 01-core, 03-terrain | none |
| `generateNether` | `GENERATE_NETHER` | boolean | `false` | – | When the overworld profile has it, maps `minecraft:the_nether` to the `cavern` profile. Has no effect in practice (`PROF-08`). | this area | none |

## Category `explosions` (12 options, read by 03-terrain)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `debrisToNearbyChunkFactor` | `DEBRIS_TO_NEARBYCHUNK_FACTOR` | int | 200 | 1..10000 | Divisor for debris spilling from damaged neighbor chunks; larger is less debris. | `tallbuildings` 175 |
| `explosionChance` | `EXPLOSION_CHANCE` | float | 0.002 | 0..1 | Chance a chunk holds an explosion center. | `cavern` 0, `nodamage` 0, `space` 0.0001, `biosphere_caves` 0.0, `biosphere` 0.0, `tallbuildings` 0.006, `ancient` 0 |
| `explosionMinRadius` | `EXPLOSION_MINRADIUS` | int (blocks) | 15 | 1..1000 | Minimum explosion radius. Must be below the maximum or generation crashes (`PROF-06`). | none |
| `explosionMaxRadius` | `EXPLOSION_MAXRADIUS` | int (blocks) | 35 | 1..3000 | Maximum explosion radius (exclusive). | `tallbuildings` 60 |
| `explosionMinHeight` | `EXPLOSION_MINHEIGHT` | int (Y) | 75 | 1..256 | Minimum center height; the center Y is `cityLevel × 6 + min + random(max - min)`. | `tallbuildings` 130 |
| `explosionMaxHeight` | `EXPLOSION_MAXHEIGHT` | int (Y) | 90 | 1..256 | Maximum center height (exclusive). | `tallbuildings` 256 |
| `miniExplosionChance` | `MINI_EXPLOSION_CHANCE` | float | 0.03 | 0..1 | Chance a chunk holds a mini explosion. | `cavern` 0, `nodamage` 0, `space` 0.001, `biosphere_caves` 0.01, `biosphere` 0.01, `tallbuildings` 0.09, `ancient` 0 |
| `miniExplosionMinRadius` | `MINI_EXPLOSION_MINRADIUS` | int (blocks) | 5 | 1..1000 | Minimum mini-explosion radius. | `biosphere_caves` 5 (same), `biosphere` 5 (same), `tallbuildings` 3 |
| `miniExplosionMaxRadius` | `MINI_EXPLOSION_MAXRADIUS` | int (blocks) | 12 | 1..3000 | Maximum mini-explosion radius (exclusive). | `biosphere_caves` 10, `biosphere` 10, `tallbuildings` 14 |
| `miniExplosionMinHeight` | `MINI_EXPLOSION_MINHEIGHT` | int (Y) | 60 | 1..256 | Minimum mini-explosion center height (plus `cityLevel × 6`). | `biosphere_caves` 60 (same), `biosphere` 60 (same) |
| `miniExplosionMaxHeight` | `MINI_EXPLOSION_MAXHEIGHT` | int (Y) | 100 | 1..256 | Maximum mini-explosion center height (exclusive). | `biosphere_caves` 75, `biosphere` 75, `tallbuildings` 256 |
| `explosionsInCitiesOnly` | `EXPLOSIONS_IN_CITIES_ONLY` | boolean | `true` | – | Explosion centers only in city chunks (the blast still reaches non-city chunks). | none |

## Category `cities` (25 options, read by 04-city-layout unless noted)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `cityChance` | `CITY_CHANCE` | double | 0.01 | -1..1 | Chance a chunk is a city center. -1 (any negative) switches to the perlin rarity map. | `floating` 0.03f, `space` 0.7f, `biosphere_caves` 0.9f, `biosphere` 0.8f, `rarecities` 0.001, `onlycities` 0.2, `bio_wasteland` 0.008f, `void_outside` 0, `largecities` -1 |
| `citySpawnDistance1` | `CITY_SPAWN_DISTANCE1` | int (blocks) | 0 | 0..10000000 | Distance from (0, 0) at which the city factor is multiplied by multiplier 1. | none |
| `citySpawnDistance2` | `CITY_SPAWN_DISTANCE2` | int (blocks) | 0 | 0..10000000 | Distance at which multiplier 2 applies; 0 disables the distance scaling. Linear in between. | none |
| `citySpawnMultiplier1` | `CITY_SPAWN_MULTIPLIER1` | double | 1.0 | 0..1 | City factor multiplier at and inside distance 1. | none |
| `citySpawnMultiplier2` | `CITY_SPAWN_MULTIPLIER2` | double | 1.0 | 0..1 | City factor multiplier at and beyond distance 2. | none |
| `cityMinRadius` | `CITY_MINRADIUS` | int (blocks) | 50 | 1..2000 | Minimum city radius (chance mode). | `biosphere_caves` 50 (same), `biosphere` 50 (same), `bio_wasteland` 30, `void_outside` 30 |
| `cityMaxRadius` | `CITY_MAXRADIUS` | int (blocks) | 128 | 1..2000 | Maximum city radius (exclusive; a range below 1 is treated as 1 for the main profile, not for the outside profile, `PROF-06`). | `space` 90, `biosphere_caves` 65, `biosphere` 65, `onlycities` 256, `bio_wasteland` 80, `void_outside` 80 |
| `cityPerlinScale` | `CITY_PERLIN_SCALE` | double | 3 | -1000000..1000000 | Scale of the city rarity perlin map (perlin mode). | `largecities` 7.0 |
| `cityPerlinOffset` | `CITY_PERLIN_OFFSET` | double | 0.1 | -1000000..1000000 | Offset of the rarity map. | `largecities` 0.2 |
| `cityPerlinInnerScale` | `CITY_PERLIN_INNERSCALE` | double | 0.1 | -1000000..1000000 | Internal scale of the rarity map. | `largecities` 0.1 (same) |
| `cityThreshold` | `CITY_THRESHOLD` | float | 0.2 | 0..1 | A chunk is a city when its city factor is above this (both modes). | `space` 0.05, `biosphere_caves` 0.05, `biosphere` 0.05, `largecities` 0.1 |
| `cityStyleThreshold` | `CITY_STYLE_THRESHOLD` | float | -1 | 0..1 (default outside range, `PROF-16`) | Below this city factor the alternative city style is used. -1 disables. | `largecities` 0.4 |
| `cityStyleAlternative` | `CITY_STYLE_ALTERNATIVE` | string | `""` | city style name | The alternative city style (`PROF-12` if empty while the threshold is active). | `largecities` `citystyle_border` |
| `cityAvoidVoid` | `CITY_AVOID_VOID` | boolean | `true` | – | Floating landscape only: skip city generation in a chunk when any of five probe columns (local x, z = 2,2; 2,14; 14,2; 14,14; 8,8) is void from Y 255 down to the minimum build height (03-terrain). | none |
| `cityLevel0Height` | `CITY_LEVEL0_HEIGHT` | int (Y) | 75 | 1..384 | Terrain height below this gives city level 0. | see [Level heights](#level-heights) |
| `cityLevel1Height` | `CITY_LEVEL1_HEIGHT` | int (Y) | 83 | 1..384 | Below this, level 1. | same |
| `cityLevel2Height` | `CITY_LEVEL2_HEIGHT` | int (Y) | 91 | 1..384 | Below this, level 2. | same |
| `cityLevel3Height` | `CITY_LEVEL3_HEIGHT` | int (Y) | 99 | 1..384 | Below this, level 3. | same |
| `cityLevel4Height` | `CITY_LEVEL4_HEIGHT` | int (Y) | 107 | 1..384 | Below this, level 4. | same |
| `cityLevel5Height` | `CITY_LEVEL5_HEIGHT` | int (Y) | 115 | 1..384 | Below this, level 5. | same |
| `cityLevel6Height` | `CITY_LEVEL6_HEIGHT` | int (Y) | 123 | 1..384 | Below this, level 6. | same |
| `cityLevel7Height` | `CITY_LEVEL7_HEIGHT` | int (Y) | 131 | 1..384 | Below this, level 7; at or above, level 8. | same |
| `cityMinHeight` | `CITY_MINHEIGHT` | int (Y) | 50 | -1024..2048 | Chunks whose terrain height is below this get city factor 0. | none |
| `cityMaxHeight` | `CITY_MAXHEIGHT` | int (Y) | 150 | -1024..2048 | Chunks whose terrain height is above this get city factor 0. | none |
| `oceanCorrectionBorder` | `OCEAN_CORRECTION_BORDER` | int (blocks) | 4 | -255..255 | Terrain correction offset for chunks next to cities that lie in ocean biomes (03-terrain). | none |

### Level heights

City level n means the city ground is at `groundLevel + 6 × n`. A chunk's level is the first n whose threshold its terrain height is below (thresholds `cityLevel0Height` … `cityLevel7Height`), else 8. With defaults: height < 75 → level 0 (Y 71); < 83 → 1 (Y 77); < 91 → 2 (Y 83); < 99 → 3 (Y 89); < 107 → 4 (Y 95); < 115 → 5 (Y 101); < 123 → 6 (Y 107); < 131 → 7 (Y 113); else 8 (Y 119).

| Profile | L0 | L1 | L2 | L3 | L4 | L5 | L6 | L7 | groundLevel |
|---|---|---|---|---|---|---|---|---|---|
| default (and all not listed) | 75 | 83 | 91 | 99 | 107 | 115 | 123 | 131 | 71 |
| `cavern` | 44 | 52 | 60 | 68 | 76 | 82 | 90 | 98 | 40 |
| `floating` | 50 | 56 | 62 | 68 | 76 | 84 | 92 | 100 | 50 |
| `space`, `biosphere_caves`, `biosphere` | 60 | 66 | 72 | 78 | 86 | 94 | 100 | 108 | 71 |

## Category `cityspheres` (14 options, read by 03-terrain unless noted)

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `citySphereFactor` | `CITYSPHERE_FACTOR` | float | 1.2 | 0.1..10 | Outer sphere radius = city radius × factor (space). | none |
| `citySphereChance` | `CITYSPHERE_CHANCE` | float | 0.7 | 0..1 | Chance a sphere generates. | `space` 0.9, `biosphere_caves` 0.5, `biosphere` 0.5 |
| `citySphereClearAbove` | `CITYSPHERE_CLEARABOVE` | int (blocks) | 0 | 0..1024 | Blocks cleared above the top sphere glass; 0 disables. | `space` 8, `biosphere_caves` 30, `biosphere` 30 |
| `citySphereClearAboveUntilAir` | `CITYSPHERE_CLEARABOVE_UNTIL_AIR` | boolean | `false` | – | Keep clearing upward until air. | `space` true |
| `citySphereClearBelow` | `CITYSPHERE_CLEARBELOW` | int (blocks) | 0 | 0..1024 | Blocks cleared below the sphere; 0 disables. | `space` 8 |
| `citySphereClearBelowUntilAir` | `CITYSPHERE_CLEARBELOW_UNTIL_AIR` | boolean | `false` | – | Keep clearing downward until air. | `space` true |
| `sphereSurfaceVariation` | `CITYSPHERE_SURFACE_VARIATION` | float | 1.0 | 0..1 | Inside-sphere surface variation. *config-only* (`PROF-17`). | none |
| `outsideSurfaceVariation` | `CITYSPHERE_OUTSIDE_SURFACE_VARIATION` | float | 1.0 | 0..1 | Outside-sphere surface variation. *config-only*. | `biosphere_caves` 0.5, `biosphere` 0.5 |
| `monorailChance` | `CITYSPHERE_MONORAIL_CHANCE` | float | 0.8 | 0..1 | Chance a sphere city wants a monorail in a direction (06-highways-railways). | `biosphere_caves` 0.0, `biosphere` 0.0 |
| `onlyPredefined` | `CITYSPHERE_ONLY_PREDEFINED` | boolean | `false` | – | Only predefined spheres generate. | none |
| `outsideGroundLevel` | `CITYSPHERE_OUTSIDE_GROUNDLEVEL` | int (Y) | -1 | -1..256 | Deprecated; *config-only*. | none |
| `outsideProfile` | `CITYSPHERE_OUTSIDE_PROFILE` | string | `""` | profile name | The profile used for chunks outside spheres ([profile-selection.md](profile-selection.md#outside-profile)). | `space` `void_outside`, `biosphere_caves` `cavern`, `biosphere` `bio_wasteland` |
| `monorailOffset` | `CITYSPHERE_MONORAIL_HEIGHT_OFFSET` | int (blocks) | -2 | -100..100 | Monorail height relative to the main height (06-highways-railways). | none |
| `grid32` | `CITYSPHERE_32GRID` | boolean | `false` | – | Align spheres to a 32 × 32-chunk grid instead of 16 × 16. | none |

## Category `client` (5 options, all *dead*)

The only reader, a fog handler in `setup/ClientEventHandlers`, is commented out (`PROF-17`).

| Key | Field | Type | Default | Range | Meaning | Built-in overrides |
|---|---|---|---|---|---|---|
| `horizon` | `HORIZON` | float (Y) | -1 | -1..256 | Horizon height, -1 for default. | `cavern` 128, `floating` 0, `space` 0, `biosphere_caves` 30, `biosphere` 30 |
| `fogRed` | `FOG_RED` | float | -1 | -1..1 | Fog red, -1 for default. | `cavern` 0.0 |
| `fogGreen` | `FOG_GREEN` | float | -1 | -1..1 | Fog green. | `cavern` 0.0 |
| `fogBlue` | `FOG_BLUE` | float | -1 | -1..1 | Fog blue. | `cavern` 0.0 |
| `fogDensity` | `FOG_DENSITY` | float | -1 | -1..1 | Fog density. | `cavern` 0.02 |

## Validation messages

Hard-coded English thrown by `LostCityProfile.init` (`IllegalArgumentException` unless noted):

| Condition | Message |
|---|---|
| Unknown landscape type (`RuntimeException`) | `Bad landscape type: <value>!` |
| Unknown street mode | `Unknown street generation mode '<value>'` |
| Unknown highway mode | `Unknown highway generation mode '<value>'` |
| Unknown multibuilding policy | `Unknown multi-building street conflict policy '<value>'` |
| Secondary min > max | `Secondary road minimum counts cannot exceed maximum counts` |
| Tertiary min > max | `tertiaryRoadMinLength cannot exceed tertiaryRoadMaxLength` |
| Primary spacing outside 8..128 | `Primary road candidate spacing must be between 8 and 128 chunks` |
| Primary chance or force interval | `Invalid primary road activation chance or forced interval` |
| Planned bridge chance or length | `Invalid planned primary bridge chance or maximum length` |
| Secondary counts outside 0..128 | `Secondary road counts must be between 0 and 128` |
| Separation or edge distance outside 2..32 | `Road separation and edge distance must be between 2 and 32 chunks` |
| Tertiary chance or length | `Invalid tertiary road chance or length` |
| Railway spacing odd or outside 4..128 | `Railway spacing must be an even number from 4 through 128 chunks` |
| Sample spacing > cell size | `highwayHubSampleSpacing cannot exceed highwayPlanningCellSize` |
| Min hub distance > max | `highwayMinimumHubDistance cannot exceed highwayMaximumHubDistance` |

## Options with an unchecked ordering constraint

These pairs are not validated, and an inverted or equal pair crashes generation with `IllegalArgumentException` from `Random.nextInt` (`PROF-06`): `explosionMinRadius`/`explosionMaxRadius`, `explosionMinHeight`/`explosionMaxHeight`, `miniExplosionMinRadius`/`miniExplosionMaxRadius`, `miniExplosionMinHeight`/`miniExplosionMaxHeight`, the outside profile's `cityMinRadius`/`cityMaxRadius`, and `buildingMinFloorsChance`/`buildingMaxFloorsChance` when the computed bound falls below 1. The level thresholds `cityLevel0Height`…`cityLevel7Height` are expected to rise but are not checked; a lower later threshold makes that level unreachable.

## 1.20 height assumptions

Every Y value above is an absolute block Y calibrated to vanilla 1.20 overworld terrain (sea level 63, world -64..320): `groundLevel` 71, level thresholds 75–131, `cityMinHeight` 50, `cityMaxHeight` 150, explosion heights 60–100, `seaLevel` -1 meaning the generator's sea level. Declared ranges reflect the 1.20 world: level thresholds up to 384 (the 1.20 overworld height span), city min/max height -1024..2048, and several older ranges capped at 256. `bedrockLayer` counts up from the world's minimum build height (-64 in the 1.20 overworld). These rows are in the index's 1.20 dependencies table.
