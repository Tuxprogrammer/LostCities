# Public API

The surface other mods use: how they obtain the API through IMC, the query interfaces for per-dimension and per-chunk city information, profile registration, the four generation events, and the shared value types. The implementations live in `LostCitiesImp`, `LostCitiesPreImp`, `LostCityProfileSetupImp` (`02`), `LostCityProfile` (`02`) and `BuildingInfo` (`04`).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | Package `mcjty.lostcities.api` (20 files): `ILostCities`, `ILostCitiesPre`, `ILostCityProfileSetup`, `ILostCityProfile`, `ILostCityInformation`, `ILostChunkInfo` (with record `MultiBuildingInfo`), `ILostCityInfo`, `ILostSphere`, `ILostExplosion`, `ILostCityAsset`, `ILostCityAssetRegistry`, `ILostCityBuilding`, `ILostCityMultiBuilding`, `ILostCityCityStyle`, `LostChunkCharacteristics`, `LostCityEvent` (and four nested events), `MultiPos`, `RailChunkType`, `IChunkPrimerFactory`, `ILostWorldsChunkGenerator`. Implementations: `LostCitiesImp` (with record `LostCityInformation`), `LostCitiesPreImp`. |
| 2 | **Inputs** | IMC messages from other mods; a `Level` or chunk coordinates; dimension infos (`03-terrain`). |
| 3 | **Numbers** | See [Constants](#constants). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Queries are served from the same cached, seed-derived layout data generation uses (`04-city-layout`), so they agree with generation. `LostCitiesImp` keeps one `LostCityInformation` per dimension in a `ConcurrentHashMap`, cleared by lifecycle cleanup. Event listeners can change characteristics and chunk content; a listener that is not deterministic makes generation non-deterministic. |
| 6 | **Data formats** | None. |
| 7 | **Output** | Information objects, profile registration, events posted on the Forge bus during generation. |
| 8 | **Relationships** | `ILostChunkInfo` is implemented by `BuildingInfo`, `ILostSphere` by `CitySphere`, `ILostExplosion` by `Explosion`, `ILostCityBuilding`/`ILostCityMultiBuilding`/`ILostCityCityStyle` by the asset classes (`05-assets`), `ILostCityProfile` by `LostCityProfile` (`02`). Events are posted by `BuildingInfo` (`CharacteristicsEvent`) and `LostCityTerrainFeature` (the other four) (`03`, `04`). `ILostWorldsChunkGenerator` is checked by `Spheres` (`03`). `SpawnBuildingPrefilter` inspects the `CharacteristicsEvent` listener list ([spawn-selection.md](spawn-selection.md)). |
| 9 | **1.20 dependencies** | IMC with `Supplier<Function<…>>`; `Level`, `WorldGenLevel`, `CommonLevelAccessor`, `ChunkAccess`, `ProtoChunk`, `ResourceKey<Level>`, `ResourceLocation`, `BlockPos` in signatures; Forge `Event` with `@Cancelable`; Java records (`MultiPos`, `ILostChunkInfo.MultiBuildingInfo`, `LostCityInformation`). |
| 10 | **Bugs** | `CORE-01`, `CORE-02`, `CORE-03`, `CORE-06`, `CORE-38` |

## Constants

`ILostCities` constants:

| Name | Value | Meaning |
|---|---|---|
| `LOSTCITIES` | `lostcities` | Mod ID to address IMC to |
| `GET_LOST_CITIES` | `getLostCities` | IMC method that yields `ILostCities` (processed at IMC-process time) |
| `GET_LOST_CITIES_PRE` | `getLostCitiesPre` | IMC method that yields `ILostCitiesPre` (processed on the construct-mod event) |
| `META_DONTCONNECT` | `dontconnect` | Part metadata key usable in assets (`05-assets`) |
| `META_SUPPORT` | `support` | Part metadata key |
| `META_Z_1` | `z1` | Part metadata key |
| `META_Z_2` | `z2` | Part metadata key |
| `META_NOWATER` | `nowater` | Part metadata key |
| `META_FORCEDAIR` | `forcedair` | Part metadata key |

`MultiPos.SINGLE` = `(x -1, z -1, w 1, h 1)`. `ILostCityInformation.getRealHeight(level)` = `groundLevel + 6 × level`.

## Interfaces

| Interface | Members (in declaration order) | Behavior at the pin |
|---|---|---|
| `ILostCities` | `getLostInfo(Level)` (nullable); `registerDimension(ResourceKey<Level>, String profile)`; `setOverworldProfile(String)` | See below |
| `ILostCitiesPre` | `registerProfileSetupCallback(Consumer<ILostCityProfileSetup>)` | Appends to `ModSetup.profileSetups`, which is never read after it is filled (`CORE-03`) |
| `ILostCityProfileSetup` | `createProfile(String name, String baseProfile)` → `ILostCityProfile` | Copies the base standard profile into a new public profile and adds it to the standard profiles; unknown base throws `Unknown base profile '<base>'!` |
| `ILostCityProfile` | `setDescription`, `setWorldStyle`, `setCityChancle(double)` (sic), `setRuinChance(float chance, float minPercent, float maxPercent)`, `setGroundLevel(int)`, `setCityLevelHeights(l0..l3)`, `setCityLevelHeights(l0..l7)`, `setOceanCorrectionBorder(int)` | Setters on profile options (`02`) |
| `ILostCityInformation` | `getChunkInfo(chunkX, chunkZ)`; `getSphere(x, y, z)`; `getSphere(x, z)`; `getRealHeight(level)`; `getBuildings()`; `getMultiBuildings()`; `getCityStyles()` | See below |
| `ILostChunkInfo` | `isCity()`; `getCityStyle()`; `getCityInfo()` (nullable); `getBuildingType()` (name or null); `getBuildingId()`; `getMultiBuildingInfo()` (nullable); `getRailType()` (non-null); `getRailLevel()`; `getCityLevel()`; `getNumFloors()` (above ground, including the top part); `getNumCellars()`; `getDamage(chunkY)` (0 = none, above 1 destroys blocks; sample at the center of the 16-block section); `getRuinLevel()` (−1 = not ruined); `getExplosions()`; `getMaxHighwayLevel()` (−1 = none); `getSphere()` (nullable) | Implemented by `BuildingInfo` (`04`) |
| `ILostChunkInfo.MultiBuildingInfo` | record `(buildingType ResourceLocation, offsetX, offsetZ, w, h)` | Value type |
| `ILostCityInfo` | `getCityRadius()` (blocks); `getCityStyle()` (style asset name) | Anonymous object returned by `BuildingInfo.getCityInfo` for city-center chunks only, backed by `City.getCityRadius` and `City.getCityStyleForCityCenter` (`04`); marked to be rewritten upstream |
| `ILostSphere` | `getCenter()` (`varia.ChunkCoord`); `getCenterPos()`; `getRadius()`; `isEnabled()` | Implemented by `CitySphere` (`03`) |
| `ILostExplosion` | `getRadius()`; `getCenter()` | Implemented by `Explosion` (`03`) |
| `ILostCityAsset` | `init(CommonLevelAccessor)` (default no-op, called after fetch); `getName()`; `getId()` | Asset base (`05`) |
| `ILostCityAssetRegistry<T>` | `get(level, String)`; `get(level, ResourceLocation)`; `getIterable()` | Asset registry (`05`) |
| `ILostCityBuilding` | `getLocalPalette(level)` (returns internal `cityassets.Palette`); `getPrefersLonely()` (0 default, 1 always alone); `getMaxFloors()`; `getMaxCellars()`; `getMinFloors()`; `getMinCellars()`; `getAllowDoors()`; `getAllowFillers()`; `getOverrideFloors()`; `getFillerBlock()`; `getRubbleBlock()` (nullable; fall back to filler) | Building asset (`05`) |
| `ILostCityMultiBuilding` | `getBuilding(x, z)` (name relative to the top-left chunk); `getDimX()`; `getDimZ()` | Multibuilding asset (`05`) |
| `ILostCityCityStyle` | `getStyle`, `getExplosionChance`, `getStreetWidth`, `getMinFloorCount`, `getMinCellarCount`, `getMaxFloorCount`, `getMaxCellarCount`, `getParkChance`, `getFountainChance`, `getBuildingChance`, `getCorridorChance`, `getFrontChance`, `getAvoidFoliage`, `getParkBorder`, `getParkElevation`, `getParkStreetThreshold`, `getStreetBlock`, `getStreetBaseBlock`, `getStreetVariantBlock`, `getRailMainBlock`, `getParkElevationBlock`, `getCorridorRoofBlock`, `getCorridorGlassBlock`, `getBorderBlock`, `getWallBlock`, `getGrassBlock` (null = `minecraft:grass_block`), `getIronbarsBlock` (null = `minecraft:iron_bars`), `getGlowstoneBlock` (null = `minecraft:glowstone`), `getLeavesBlock` (null = random leaves) | City-style asset (`05`) |
| `IChunkPrimerFactory` | `fillChunk(chunkX, chunkZ, ProtoChunk)`; `getHeight(chunkX, chunkZ, x, z)` (estimate, 0–255) | *Unused*: nothing implements or calls it at the pin |
| `ILostWorldsChunkGenerator` | `getOuterSeaLevel()` (nullable) | Copied from Lost Worlds to avoid a circular dependency; `Spheres` uses it for the sea level outside spheres (`03`) |

## Value types

| Type | Fields or members |
|---|---|
| `LostChunkCharacteristics` | public mutable fields: `isCity`, `couldHaveBuilding`, `multiPos` (`SINGLE` for single buildings), `cityLevel` (0 lowest), `cityStyleId`, `cityStyle`, `multiBuildingId`, `multiBuilding`, `buildingTypeId`, `buildingType`, `rawPlannedRoadType` and `plannedRoadType` (internal `worldgen.street.PlannedRoadType`, default `NONE`) |
| `MultiPos` | record `(x, z, w, h)`: `isSingle()` (x = −1), `isMulti()`, `isTopLeft()` (0, 0), `isRightSide()` (x = w − 1), `isBottomSide()` (z = h − 1) |
| `RailChunkType` | 13 values with `isStation`, `isSurface`: `NONE` (no, no), `STATION_SURFACE` (yes, yes), `STATION_UNDERGROUND` (yes, no), `STATION_EXTENSION_SURFACE` (yes, yes), `STATION_EXTENSION_UNDERGROUND` (yes, no), `GOING_DOWN_TWO_FROM_SURFACE` (no, yes), `GOING_DOWN_ONE_FROM_SURFACE` (no, yes), `GOING_DOWN_FURTHER`, `HORIZONTAL`, `THREE_SPLIT`, `VERTICAL`, `DOUBLE_BEND`, `RAILS_END_HERE` (all no, no) |

## Events

All fire on the Forge event bus during generation (server, possibly on world-generation threads) and carry `getWorld()` (`WorldGenLevel`), `getLostCities()`, `getChunkX()`, `getChunkZ()`.

| Event | Cancelable | Payload | When (per the Javadoc and call sites) |
|---|---|---|---|
| `CharacteristicsEvent` | no | `getCharacteristics()` (mutable) | When the characteristics of any chunk, city or not, are decided; listeners may change any field. Calling `getChunkInfo` from inside may recurse forever |
| `PreGenCityChunkEvent` | yes | `getChunkAccess()` | Before a city chunk's street or building is written; the chunk already holds stone up to city level. Cancel to skip the street or building only |
| `PostGenCityChunkEvent` | no | `getChunkAccess()` | After a city chunk's street or building, before highways and subways |
| `PostGenOutsideChunkEvent` | no | `getChunkAccess()` | After a non-city chunk, before highways and subways |
| `PreExplosionEvent` | yes | `getChunkAccess()` | After generation, before explosion damage and debris; cancel to skip both |

## Behavior detail

1. (both) **Obtaining the API.** A mod sends IMC to `lostcities` with method `getLostCities` and a supplier of a function taking `ILostCities`. Lost Cities applies it to its singleton during IMC processing. `getLostCitiesPre` works the same way with `ILostCitiesPre`, but is drained on the construct-mod event ([lifecycle.md](lifecycle.md)). The `ILostCities` Javadoc points at `ModSetup.GetLostCities::new`, which does not exist.
2. (server) **`getLostInfo(level)`**: cast the level to `WorldGenLevel` (a client level fails the cast, `CORE-02`), get the dimension info, return `null` if the dimension has no profile, else the per-dimension `LostCityInformation` (created on first use).
3. (server) **`registerDimension(key, profile)`**: delegates to `Config.registerLostCityDimension`; lost on the next profile-cache reset ([config.md](config.md), `CORE-06`). The Javadoc says to repeat it after loading a world, for example from a chunk generator's surface building.
4. (client) **`setOverworldProfile(profile)`**: delegates to the world-creation GUI's `selectProfile(profile, null)`, which sets `Config.profileFromClient`, bumps the dirty counter and resets the profile cache. It is documented as client-side, but `LostCitiesImp` is common code and the GUI class extends a client-only screen class, so a call on a dedicated server fails to load the class (`CORE-01`).
5. (server) **`LostCityInformation`**:
   - `getChunkInfo(x, z)`: `BuildingInfo.getBuildingInfo` for that chunk (cached; the Javadoc says not to call it during world generation);
   - `getSphere(x, y, z)` and `getSphere(x, z)`: only for profiles with spheres or space, else `null`; looked up by block coordinates (`CitySphere`, `03`);
   - `getRealHeight(level)`: `groundLevel + 6 × level`; the Javadoc notes that usable air starts one block higher;
   - `getBuildings()`, `getMultiBuildings()`, `getCityStyles()`: the asset registries cast to the API types.

## On 1.12

`upstream/1.12`'s API has `ILostCities.getLostGenerator(int dimension)` returning an `ILostChunkGenerator` instead of `getLostInfo`, and lacks `ILostCitiesPre`, `ILostCityProfileSetup`, `ILostCityProfile`, `ILostCityInformation`, `MultiPos` and `ILostWorldsChunkGenerator`. The other interfaces and the event set exist there with 1.12 types (integer dimension IDs, `World`, `ChunkPrimer`).

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
