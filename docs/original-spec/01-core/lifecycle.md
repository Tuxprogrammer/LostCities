# Mod lifecycle and registration

How upstream Lost Cities boots: the `@Mod` entry point, what it registers and in which lifecycle phase, the inter-mod (IMC) entry points, the Forge-bus handlers that are not about spawn or beds, cache cleanup, and the jar metadata (`mods.toml`, access transformer, `pack.mcmeta`). Spawn selection and the bed teleporter, which share the `ForgeEventHandlers` class, have their own sheets.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `LostCities` (the `@Mod("lostcities")` class; builds registries, configs and listeners in its constructor). `ModSetup` (pre-init and common setup; holds the list of profile-setup callbacks). `Registration` (the feature `DeferredRegister` and the Lost City dimension keys). `LostCitiesImp`, `LostCitiesPreImp` (API implementations, see [api.md](api.md)). `setup/ClientSetup` (client-only setup, see `09-client-ui`). `setup/ForgeEventHandlers` (Forge-bus handlers; this sheet covers commands, capability attach, level tick, server start/stop and cleanup). `datagen/DataGenerators` (data-generation hook). `META-INF/mods.toml`, `META-INF/accesstransformer.cfg`. Config keys: none read here; the three config specs are registered here and documented in [config.md](config.md). |
| 2 | **Inputs** | FML lifecycle events (mod construction, construct-mod event, common setup, IMC processing, client setup, data generation). IMC messages `getLostCitiesPre` and `getLostCities`. Forge server events: command registration, entity capability attach, level tick, server about-to-start, server stopping. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | No random sources. Order of IMC callbacks is the order FML delivers the messages. Global mutable state lives in statics (`LostCities.instance`, `LostCities.setup`, `LostCities.lostCitiesImp`, `ModSetup.logger`, `ModSetup.profileSetups`, every cache cleared by `ForgeEventHandlers.cleanUp`). Cleanup is driven by server start and stop and by the feature's dirty counter, not by world identity. |
| 6 | **Data formats** | `mods.toml`, access-transformer entries and `pack.mcmeta` (below). No NBT or saved data. |
| 7 | **Output** | Two worldgen features registered (`lostcities:lostcity`, `lostcities:spheres`); three config specs registered; the folder `config/lostcities/` created; the network channel and Forge handlers registered at common setup; profiles written to `config/lostcities/profiles/` by `ProfileSetup` (`02-profiles-dimensions`). |
| 8 | **Relationships** | Calls `CustomRegistries.init` and `CustomRegistries::onDataPackRegistry` (`05-assets`), `ProfileSetup.setupProfiles` (`02-profiles-dimensions`), `PacketHandler.registerMessages` ([network.md](network.md)), `ModCommands.register` ([commands.md](commands.md)), `AssetRegistries.reset/load` (`05-assets`), `GlobalTodo.executeAndClearTodo` (`03-terrain`), the cache owners `BuildingInfo`, `MultiChunk`, `Highway`, `Railway`, `BiomeInfo`, `City`, `CitySphere`, `Scattered` (`03`, `04`, `06`, `07`), `LCBlockTags` (`08-loot-mobs-blocks`). `ClientSetup` registers `ClientEventHandlers` (`09-client-ui`). |
| 9 | **1.20 dependencies** | Deferred registers for `Feature`; `ModConfig` CLIENT/COMMON/SERVER specs, with SERVER files stored per world; IMC messages carrying `Supplier<Function<…>>`; `FMLConstructModEvent.enqueueWork`; `LevelTickEvent`; `GatherDataEvent` data generation; `mods.toml`; access transformers keyed by SRG names. Each is a row in the area's 1.20 dependency table. |
| 10 | **Bugs** | `CORE-03` (profile-setup callbacks never run), `CORE-12` (cleanup misses the spawn-position map), `CORE-33` (vanilla `/resetchunks` registered, see [commands.md](commands.md)). |

## Numbers

| Item | Value |
|---|---|
| Mod ID | `lostcities` (`LostCities.MODID`, also `ILostCities.LOSTCITIES`) |
| Logger | `LostCities.LOGGER` named `lostcities`; `ModSetup.logger` is a second logger obtained with no name (log4j names it after the calling class, `mcjty.lostcities.setup.ModSetup`) |
| Features | `lostcities:lostcity` (`LostCityFeature`), `lostcities:spheres` (`LostCitySphereFeature`) |
| Dimension keys | `Registration.LOSTCITY` = `lostcities:lostcity`; `DIMENSION_TYPE` and `DIMENSION` keys both use that location |
| Config folder | `config/lostcities/` (created with `mkdirs` in the constructor) |
| Config files | CLIENT `config/lostcities/client.toml`; COMMON `config/lostcities/common.toml`; SERVER default name `lostcities-server.toml`, stored in each world's `serverconfig/` |
| IMC method names | `getLostCitiesPre` (`ILostCities.GET_LOST_CITIES_PRE`), `getLostCities` (`ILostCities.GET_LOST_CITIES`) |
| Network channel | `lostcities:lostcities`, protocol `1.0` ([network.md](network.md)) |
| Capability | `lostcities:spawnset` on every `Player` ([spawn-selection.md](spawn-selection.md)) |
| Forge dependency (mods.toml) | `forge`, mandatory, `[43.1.30,)`, ordering `NONE`, side `BOTH`; loader `javafml`, `loaderVersion="[47,)"` |

## Behavior detail

1. (both) **Construction.** The `LostCities` constructor runs once per game launch:
   1. store the instance;
   2. register the feature deferred register (`Registration.init`) and the custom data-pack registries (`CustomRegistries.init`, `05-assets`) on the mod bus;
   3. create `config/lostcities/`;
   4. register the CLIENT, COMMON and SERVER config specs with the file names above;
   5. call `ModSetup.preInit`: create `ModSetup.logger`, then `ProfileSetup.setupProfiles()` (`02-profiles-dimensions`), which builds the standard profiles, runs every callback in `ModSetup.profileSetups`, and writes each profile except `customized` to `config/lostcities/profiles/<name>.json`. At this point `profileSetups` is always empty, because the IMC that fills it is drained later (step 2) (`CORE-03`);
   6. add mod-bus listeners for common setup, IMC processing, the construct-mod event and the data-pack registry event; on the client distribution also for client setup.
2. (both) **Construct-mod event.** Work enqueued on `FMLConstructModEvent` reads every IMC message whose method is `getLostCitiesPre`. Each message supplies a function that is applied to a fresh `LostCitiesPreImp`; a mod calls `registerProfileSetupCallback` on it, which appends its consumer to `ModSetup.profileSetups`. Nothing later reads the list again.
3. (both) **Common setup** (`ModSetup.init`): register the network channel; register one `ForgeEventHandlers` instance on the Forge bus; `AssetRegistries.reset()`. Commented-out leftovers: terrain-event handler registration and loot-table registration (not active).
4. (both) **IMC processing.** Every IMC message with method `getLostCities` supplies a function applied to the singleton `LostCities.lostCitiesImp`. This is how other mods obtain `ILostCities`.
5. (client) **Client setup**: `ClientSetup.init` registers `ClientEventHandlers` on the Forge bus (`09-client-ui`).
6. (server) **Command registration**: `ModCommands.register` on `RegisterCommandsEvent` ([commands.md](commands.md)).
7. (both) **Capability attach**: every `Player` entity without the capability gets the `lostcities:spawnset` provider ([spawn-selection.md](spawn-selection.md)).
8. (server) **Level tick**, END phase, server levels only: `AssetRegistries.load(level)` (`05-assets`; called every tick of every level, expected to return at once when already loaded), then `GlobalTodo.get(level).executeAndClearTodo(level)` (`03-terrain`: deferred block work that needs a real level, such as POI blocks and sapling growth).
9. (server) **Server about to start**: `ForgeEventHandlers.cleanUp()`. **Server stopping**: `cleanUp()` then `Config.reset()` ([config.md](config.md)).
10. (server) **`cleanUp()`** resets the dimension-profile cache (`Config.resetProfileCache`) and clears the caches of `BuildingInfo`, `MultiChunk`, `Highway`, `Railway`, `BiomeInfo`, `City`, `CitySphere` and `Scattered`. It does not clear the spawn-position map ([spawn-selection.md](spawn-selection.md), `CORE-12`).
11. (both) **Dirty counter.** `LostCityFeature.globalDimensionInfoDirtyCounter` (a `volatile int` in `03-terrain`'s feature) is incremented by the new-world spawn handler, the world-creation GUI and the client logout handler. The next `getDimensionInfo` call that sees a stale counter takes the feature's write lock and runs: `LostCitiesImp.cleanUp()` (drop per-dimension API info), `ForgeEventHandlers.cleanUp()`, `AssetRegistries.reset()`, shut down and drop every dimension info. The increment itself is not atomic.
12. (dev only) **Data generation**: on `GatherDataEvent`, when server data is included, `DataGenerators` adds the block-tag provider `LCBlockTags`. The generated tags live in `src/generated/resources/data/lostcities/tags/blocks/` (`easybreakable`, `foliage`, `lights`, `needspoi`, `notbreakable`, `rotatable`) and are documented in `08-loot-mobs-blocks`.

## Data formats

### `META-INF/mods.toml`

| Field | Value |
|---|---|
| `modLoader` | `javafml` |
| `loaderVersion` | `[47,)` |
| `license` | `MIT License https://github.com/McJtyMods/LostCities/blob/1.16/LICENCE` |
| `issueTrackerURL` | `http://github.com/McJtyMods/LostCities/issues` |
| `displayURL` | `http://github.com/McJtyMods/LostCities/` |
| `authors` | `McJty` |
| `[[mods]] modId` | `lostcities` |
| `version` | `${file.jarVersion}` |
| `displayName` | `LostCities` |
| `description` | `Generate cities all over the world` |
| Dependency | `forge`, mandatory, `[43.1.30,)`, `NONE`, `BOTH`. No `minecraft` dependency is declared. |
| Commented out | `logoFile`, `credits`, `updateJSONURL` |

### `META-INF/accesstransformer.cfg`

| Target | Purpose at the pin |
|---|---|
| `CreateWorldScreen.f_100833_` (inMoreWorldOptionsDisplay) | Unused leftover |
| `CreateWorldScreen.field_146331_K` (selectedIndex) | Pre-1.13 field name; matches nothing in 1.20 |
| `CreateWorldScreen.f_170146_` (worldGenSettingsVisible) | Unused leftover |
| `CreateWorldScreen.f_267424_` (tabManager) | Read by the Cities button to find the active tab (`09-client-ui`) |
| `CreateWorldScreen$MoreTab` | Made public for the same check |
| `NoiseChunk.m_209247_` (getInterpolatedState), `NoiseChunk$NoiseChunkDensityFunction`, `DensityFunctions$Marker`, `$Marker$Type`, `$Marker$Type.codec`, `$Marker` constructor | Terrain sampling (`03-terrain`) |
| `ClientLevel$ClientLevelData.f_104832_` (isFlat, `public-f`), `NoiseBasedChunkGenerator.m_224284_` (doFill), `StructureManager.f_220460_` (level) | Commented "For Lost Worlds" (a companion mod) |
| Commented-out block for `NoiseBasedChunkGenerator` noise sizes | Not active |

### `pack.mcmeta`

`src/main/resources/pack.mcmeta`: the metadata that makes the jar's resources a resource and data pack, `pack_format` 15 (the 1.20.1 format) and description `Resources used for The Lost Cities`.

## On 1.12

`upstream/1.12` (`e8900023`) boots through `@Mod` with `ClientProxy`/`ServerProxy` (`setup/IProxy.java`), reads `LostCityConfiguration` through Forge's `Configuration` class (present in 1.7.10), and registers world types (`LostWorldType`, `LostWorldTypeBOP`) plus a dimension (`dimensions/ModDimensions.java`) instead of features. Those mechanisms are much closer to 1.7.10 than the 1.20 deferred registers.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core) and [0003](../../design/0003-world-presence.md), which drops the Lost City dimension.
