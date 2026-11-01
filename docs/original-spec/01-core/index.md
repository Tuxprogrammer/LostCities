# 01 · Core

## Overview

The parts of upstream Lost Cities that are not world generation proper: how the mod boots and what it registers, the mod-level config and how a dimension gets its profile, commands, the public API and its events, the in-game part editor, spawn selection for new worlds, the bed teleporter to the Lost City dimension, the network channel, and the shared utilities and random/noise generators in `varia`. It documents Lost Cities 1.20-7.5.7 for Minecraft 1.20.1 (upstream branch `1.20` at `f18a094d`). Source paths are relative to `src/main/java/mcjty/lostcities/` unless they start with `src/`.

## Contents

- [lifecycle.md](lifecycle.md): mod construction, registration, IMC, Forge handlers, cache cleanup, `mods.toml`, access transformer, `pack.mcmeta`
- [config.md](config.md): every key of the three config files, and the dimension-to-profile resolution
- [spawn-selection.md](spawn-selection.md): new-world spawn search, forced-building prefilter, first-login correction, the player capability
- [bed-teleporter.md](bed-teleporter.md): sleeping in a skull-ringed bed to move to and from the Lost City dimension
- [commands.md](commands.md): every `/lostcities` (`/lost`) subcommand with arguments, permission, behavior and output
- [part-editor.md](part-editor.md): edit-mode recording, edit sessions and part export
- [network.md](network.md): the channel and its two (inert) packets
- [api.md](api.md): IMC access, query interfaces, profile registration, events, value types
- [utilities.md](utilities.md): `ChunkCoord`, `TimedCache`, `TodoQueue`, `Tools`, `WorldTools`, `GeometryTools`, `Counter`, `Statistics`, `ComponentFactory`
- [noise-and-random.md](noise-and-random.md): `QualityRandom` and the three noise generators, specified bit for bit

## Key facts

- **The profile handoff is a sentinel in a per-world file.** The world-creation screen puts the chosen profile in a static (`Config.profileFromClient`); the first profile lookup on the server copies it into the world's `serverconfig/lostcities-server.toml` only if `selectedProfile` still holds `<CHECK>`. A dedicated server therefore has no overworld cities until the owner sets `selectedProfile` ([config.md](config.md)).
- **Two config keys change layout but are not per world:** `heightSampleSize` (default 3) and `optimizedHeightmap` (default false) sit in the COMMON file (`CORE-10`).
- **Spawn search reads real blocks.** It spirals out from chunk 0,0 and forces up to `spawnCheckAttempts` (20000) chunks to generate synchronously, then throws if nothing fits ([spawn-selection.md](spawn-selection.md)).
- **Random generators are layout-defining.** `QualityRandom` and the noise classes must be reproduced bit for bit for seed-compatible layouts; `PerlinNoiseGenerator14`'s octaves are all seeded alike (`CORE-25`), and that quirk is part of where cities appear ([noise-and-random.md](noise-and-random.md)). Whether 1.7.10's `java.util.Random` and `NoiseGeneratorSimplex` reproduce 1.20's `LegacyRandomSource` and `SimplexNoise` is *unverified* and is a test for roadmap milestone M03 ([Equivalence with 1.7.10](noise-and-random.md#equivalence-with-1710)).
- **`TimedCache` evicts by wall-clock time** (`cacheCleanupSeconds`, default 300 s). That is safe only because cached values are pure functions of key, seed, profile and assets.
- **Every player-visible string is hard-coded English.** No lang key is used by core code.
- **Dead or inert at the pin:** both network packets, the `lostcities:spawnset` capability, `IChunkPrimerFactory`, `WorldTools.checkStructures`/`chunkLoaded`/`loadWorld`/`getWorld`, the `CustomTeleporter` constructor, `findValidTeleportLocation`, `QualityRandom()` without a seed, `PerlinNoiseGenerator14.getSurfaceNoiseValue`, `Tools.DONE`, `Config.DEBUG`, `CATEGORY_GENERAL`.
- **`src/api/java/ivorius/reccomplex/dimensions/DimensionDictionary.java`** is third-party code from Recurrent Complex (author Ivorius; MIT at its source repository, not McJty's code), a stale copy written against 1.12-era types (`WorldProvider`, Trove maps). The file has no license header and upstream carries it without attribution. The upstream 1.20 build does not compile `src/api`, and nothing references it. The port leaves it out because it is unused ([decision log](../../design/decision-log.md) D12). Contents for the record: dimension type tags `UNCATEGORIZED`, `MC_DEFAULT`, `REAL`, `UNREAL` (subtypes `IMAGINARY`, `SIMULATED`, `ABSTRACT`), `TOP_LIMIT`, `NO_TOP_LIMIT`, `BOTTOM_LIMIT`, `NO_BOTTOM_LIMIT`, `FINITE`, `INFINITE`, `PLANET_SURFACE`, `EARTH`, `HELL`, `ENDER`, `BOSS_ARENA`; dimension 0 is `MC_DEFAULT, REAL, INFINITE, NO_TOP_LIMIT, BOTTOM_LIMIT, PLANET_SURFACE, EARTH`, −1 is `MC_DEFAULT, REAL, INFINITE, TOP_LIMIT, BOTTOM_LIMIT, HELL`, 1 is `MC_DEFAULT, REAL, FINITE, NO_TOP_LIMIT, NO_BOTTOM_LIMIT, BOSS_ARENA, ENDER`; type matching walks subtypes breadth-first.

## Related areas

- `02-profiles-dimensions`: profile options read here (spawn options, `editMode`, `generateNether`, `groundLevel`, `landscapeType`); `ProfileSetup` runs from the mod constructor; the Lost City dimension data
- `03-terrain`: `LostCityFeature` (dimension infos, dirty counter, lifecycle lock), `LostCityTerrainFeature` (statistics, edit-mode recording, events, noise users), `GlobalTodo` (per-tick queue), city spheres
- `04-city-layout`: `BuildingInfo`, chunk characteristics, `City`, `StructureAvoidance` (the avoid keys), `Counter` and `QualityRandom` users
- `05-assets`: asset registries, palettes and parts used by commands and the editor; `Tools.stringToState`
- `06-highways-railways`: highway noise from `PerlinNoiseGenerator14`; the highway planner dumped by `/lostcities debug`; `heightSampleSize` in the highway hash
- `07-scattered`: `QualityRandom` in `Scattered`
- `08-loot-mobs-blocks`: block tags produced by data generation
- `09-client-ui`: the world-creation screen that sets `profileFromClient`, `ClientSetup`/`ClientEventHandlers`, and the lang files

## Coverage

- Source files read: **70 of 70** in scope, each in full: `LostCities.java`, `LostCitiesImp.java`, `LostCitiesPreImp.java`; `api/*` (20); `commands/*` (14); `datagen/DataGenerators.java`; `editor/*` (3); `network/*` (3); `playerdata/*` (3); `setup/Config.java`, `ForgeEventHandlers.java`, `ModSetup.java`, `Registration.java`, `SpawnBuildingPrefilter.java` (5); `varia/*` (14); `src/main/resources/META-INF/accesstransformer.cfg`, `mods.toml`, `src/main/resources/pack.mcmeta` (3); `src/api/java/ivorius/reccomplex/dimensions/DimensionDictionary.java` (1).
- The in-scope files `setup/ClientSetup.java`, `setup/ClientEventHandlers.java`, `gui/**`, the lang files and the PNG textures are covered in `09-client-ui` (38 files), for 108 of 108 across both areas.
- Lang keys: none in this area's code (see `09-client-ui`). Hard-coded strings are listed in each sheet.
- Out-of-scope files read, in part, to describe interfaces: `worldgen/LostCityFeature.java` (dimension-info lookup and cleanup, lines 160–235); `config/ProfileSetup.java` (profile list, `setupProfiles`); `config/LostCityProfileSetupImp.java` (full); `config/LostCityProfile.java` (spawn options, `editMode`, `generateNether`, `iconFile`); `worldgen/LostCityTerrainFeature.java` (constructor noise, timing and events, edit-mode recording); `worldgen/lost/BuildingInfo.java` (`getBuildingInfo`, `setBuildingType`, `getCityInfo`); `worldgen/lost/regassets/data/DataTools.java` (`toName`, `fromName`); `worldgen/lost/cityassets/RegistryAssetRegistry.java` (name lookup); `worldgen/lost/regassets/BuildingPartRE.java`, `PaletteRE.java`, `data/PaletteEntry.java` (codec field names); `worldgen/lost/Highway.java`, `CityRarityMap.java`, `worldgen/GenerationContext.java` (call sites, by search); `build.gradle` (source sets). On `upstream/1.12` (`e8900023`): `ForgeEventHandlers.java`, `ClientEventHandlers.java`, `config/LostCityConfiguration.java`, `api/ILostCities.java`, `api/LostCityEvent.java`, `varia/ChunkCoord.java`, `varia/QualityRandom.java`, plus file listings.
- Corrections to earlier statements: none (first version).

## Known limits

- Nothing was run. Every behavior is read from code; concurrency bugs (`CORE-05`, `CORE-22`, `CORE-34`) are suspected from the code paths, not observed.
- Vanilla 1.20.1 behavior is inferred, not read from this repository: that canceling `CreateSpawnPosition` also skips the bonus chest (`CORE-17`), that the bed event position is the head half, that vanilla registers `/resetchunks` only in development, and that the Forge block registry returns air for unknown names (`CORE-20`).
- McJty's online documentation was not consulted for this area; the code at the pin is the only source.
- How the profile options used here are defined and validated is `02-profiles-dimensions`' to state; ranges quoted here come from `LostCityProfile`'s config definitions.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Deferred registers and worldgen `Feature`s | `setup/Registration` | 1.7.10 has no feature registry; city generation must hook chunk providers or populate events instead (spike S2) |
| `ForgeConfigSpec` with CLIENT/COMMON/SERVER files, per-world `serverconfig`, auto-save on `set` | `setup/Config`, `LostCities` | 1.7.10 Forge has one-file `Configuration` and no per-world server config; the `<CHECK>` profile handoff depends on a per-world file |
| IMC carrying `Supplier<Function<…>>`; `FMLConstructModEvent.enqueueWork` | `LostCities` | 1.7.10 `FMLInterModComms` carries strings, NBT and item stacks only; API acquisition needs another route |
| `ResourceKey<Level>` dimension IDs | `Config`, `ChunkCoord`, `EditModeData`, packets, API | 1.7.10 uses integer dimension IDs |
| Structure IDs and `StructureManager` (mansion, igloo, pillager outpost, pyramids, swamp hut) | `Config.avoidStructures`, `WorldTools` | 1.7.10 has villages, strongholds, mineshafts and temples only; avoid lists need 1.7.10 equivalents |
| `LevelEvent.CreateSpawnPosition` (cancelable), `setDefaultSpawnPos` with angle | `ForgeEventHandlers` | `upstream/1.12` uses `WorldEvent.CreateSpawnPosition`; the 1.7.10 counterpart must be confirmed at the pinned Forge |
| Forge capabilities on players | `playerdata/*` | 1.7.10 uses `IExtendedEntityProperties` (the capability is dead anyway) |
| `PlayerSleepInBedEvent` with `BedSleepingProblem`, `changeDimension` with `ITeleporter` | bed teleporter | Dimension not ported (0003) |
| Brigadier commands (`requires`, argument types, suggestion providers) | `commands/*` | 1.7.10 uses `ICommand`/`CommandBase` with string arguments, `getRequiredPermissionLevel` and tab completion |
| Vanilla `ResetChunksCommand` | `ModCommands` | No 1.7.10 counterpart |
| `SavedData` in `DimensionDataStorage` | `EditModeData` | 1.7.10 `WorldSavedData` in `MapStorage` |
| `SimpleChannel`, `FriendlyByteBuf`, `NetworkEvent.Context` | `network/*` | 1.7.10 `SimpleNetworkWrapper` (packets are inert anyway) |
| Block-state strings, `BlockStateParser`, `BlockStateData.upgradeBlock` (1.13 flattening fixer) | `Tools`, `exportpart`, editor | 1.7.10 identifies blocks by name plus metadata; no property strings, no flattening |
| Block tags (`TagKey`, registry holders) | `Tools.getBlocksForTag`, `hasTag` | 1.7.10 has no block tags; tag membership becomes lists |
| Sea level from the chunk generator | `Tools.getSeaLevel` | 1.7.10 has no generator sea-level query; RWG has its own sea level (0003) |
| `RandomSource`, `LegacyRandomSource`, vanilla `SimplexNoise` | `varia` noise classes | `java.util.Random` and 1.7.10's `NoiseGeneratorSimplex`; whether they reproduce the 1.20 values is *unverified* ([Equivalence with 1.7.10](noise-and-random.md#equivalence-with-1710)) |
| World height −64 to 320, `getMaxBuildHeight` | `createbuilding`, spawn scan | 1.7.10 is 0 to 255 |
| Java 17: records (`ChunkCoord`, `MultiPos`, `PartData`, `LostCityInformation`, `MultiBuildingInfo`), switch expressions, pattern `instanceof`, `Set.of`, `List.of`, `Map.copyOf` | throughout | Java 8 API: no records, no `Set.of`/`List.of` (jabel allows syntax only) |
| `mods.toml`, access transformers keyed by SRG names, `CreateWorldScreen` tabs | `META-INF/*` | 1.7.10 uses `mcmod.info` and SRG `func_`/`field_` names; the creation screen has no tabs |
| `ServerAboutToStartEvent`, `ServerStoppingEvent`, `LevelTickEvent` on the Forge bus | `ForgeEventHandlers` | 1.7.10 has FML server lifecycle events on the mod and `TickEvent.WorldTickEvent` |
| `GatherDataEvent` data generation | `datagen/DataGenerators` | No 1.7.10 counterpart; tags are hand-written or replaced |
| `ServerLifecycleHooks.getCurrentServer` | `WorldTools`, `Tools.stringToState` | 1.7.10 `MinecraftServer.getServer()` |

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| CORE-01 | `setOverworldProfile` loads a client-only class from common code | `LostCitiesImp.java:44` | The API setter calls the world-creation screen class, which extends a client-only `Screen`; on a dedicated server the call fails with a class-loading error | crash |
| CORE-02 | `getLostInfo` casts any `Level` to `WorldGenLevel` | `LostCitiesImp.java:30` | A client level is not a `WorldGenLevel`, so the call throws `ClassCastException` instead of returning null | crash |
| CORE-03 | Profile-setup callbacks never run | `LostCities.java:52` | Profiles are set up (and callbacks run) in the mod constructor; the `getLostCitiesPre` IMC is drained later (`LostCities.java:69`) into a list nothing reads again, so API-registered profiles never exist | gameplay |
| CORE-04 | Unknown `selectedProfile` crashes profile resolution | `setup/Config.java:122` | The nether check dereferences the overworld's standard profile without a null check; a typo in `selectedProfile`, or `customized` without JSON, throws on the first lookup | crash |
| CORE-05 | Profile cache built without synchronization | `setup/Config.java:76` | A static `HashMap` lazily filled, and the config written back, by whichever thread asks first; `LostCityFeature` calls it under a shared read lock from world-generation threads, so entries can be lost and chunks generate without cities; a reset from another thread (server stop, logout, the GUI) between the null check and the final lookup throws `NullPointerException` | crash |
| CORE-06 | API dimension registrations discarded on every cache reset | `setup/Config.java:63` | `registerLostCityDimension` writes into the cache that server start/stop, lifecycle cleanup, the GUI and the logout handler all drop | gameplay |
| CORE-07 | Default `avoidStructures` names `minecraft:swamp_huts` | `setup/Config.java:45` | The vanilla ID is `minecraft:swamp_hut`; witch huts are never avoided by default | worldgen |
| CORE-08 | `heightSampleSize` comment states the wrong default | `setup/Config.java:166` | Says the default is 1 (every chunk sampled); it is 3 | cosmetic |
| CORE-09 | Avoided-structure set never refreshed; a bad entry throws in generation | `setup/Config.java:141` | Parsed once per JVM, so reloads and later worlds in the same session keep the first set; a malformed ID throws from the first chunk that checks avoidance | gameplay |
| CORE-10 | Layout-changing keys live in the per-installation COMMON config | `setup/Config.java:162` | `heightSampleSize` and `optimizedHeightmap` change terrain and highway layout but are not stored with the world; changing them breaks the border with existing chunks | worldgen |
| CORE-11 | Login handler reverts later spawn changes | `setup/ForgeEventHandlers.java:84` | Runs on every login and keeps the stored spawn while it still matches, so a later `/setworldspawn` is undone and the next player to log in is teleported | gameplay |
| CORE-12 | Stored spawn positions survive into the next world | `setup/ForgeEventHandlers.java:61` | The map is never cleared on server stop; in single player a second world whose profile needs no search inherits the first world's spawn and teleport | gameplay |
| CORE-13 | Duplicate forced-spawn entries crash world creation | `setup/ForgeEventHandlers.java:210` | `Set.of` throws on duplicates in `forceSpawnBuildings` or `forceSpawnParts` | crash |
| CORE-14 | Failed spawn search aborts world creation | `setup/ForgeEventHandlers.java:317` | Throws `Can't find a valid spawn position!` instead of falling back to vanilla | crash |
| CORE-15 | Spawn search generates up to 20000 chunks synchronously | `setup/ForgeEventHandlers.java:306` | Reading block states forces full generation of every probed chunk on the server thread during world creation | performance |
| CORE-16 | Spawn column scan is bottom-up with a fixed top | `setup/ForgeEventHandlers.java:407` | Scans `groundLevel − 5` to Y 124 and takes the lowest open spot: spawns in caves or cellars, and nothing at Y 125 or above is considered | gameplay |
| CORE-17 | Custom spawn skips the bonus chest | `setup/ForgeEventHandlers.java:248` | Canceling the spawn event skips vanilla's spawn routine, which also places the bonus chest; the `cavern` profile recommends enabling it (inferred from vanilla 1.20.1) | gameplay |
| CORE-18 | Bed-teleport landing search can loop forever | `setup/ForgeEventHandlers.java:561` | The loop tests the start position's Y, not the moving one; a destination column of air down past the bottom (void and sphere worlds) hangs the server | crash |
| CORE-19 | Bed-teleport cobblestone and landing spot disagree | `setup/ForgeEventHandlers.java:565` | Cobblestone goes at the bed's coordinates in the destination, not under the landing spot; if bed + 5 is solid the player lands inside blocks | gameplay |
| CORE-20 | Unknown block names silently become air | `varia/Tools.java:73` | The Forge block registry returns air for unknown names, so the "Cannot find block" check is dead and misspelled palette blocks vanish | worldgen |
| CORE-21 | Weighted choice can return null | `varia/Tools.java:117` | The `java.util.Random` overload returns null when float rounding leaves a remainder; the `RandomSource` overload returns the last element | crash |
| CORE-22 | `TimedCache` computes values while holding the map's bin lock | `varia/TimedCache.java:67` | Suppliers run inside `ConcurrentHashMap.compute` (also line 81); a supplier that touches the same cache throws `Recursive update` or deadlocks, and slow suppliers block other threads | crash |
| CORE-23 | `checkStructures` position math is wrong | `varia/WorldTools.java:65` | `chunkX << 4 + 8` shifts by 12 bits; *dead code* | worldgen |
| CORE-24 | `/lostcities stats` average is wrong | `varia/Statistics.java:34` | Integer division and empty slots counted; minimum and maximum are all-time, not the last 100 | cosmetic |
| CORE-25 | Octaves of `PerlinNoiseGenerator14` are identical | `varia/PerlinNoiseGenerator14.java:16` | Every octave gets a fresh `LegacyRandomSource(seed)`, so all share permutation and offsets; `Highway` also seeds its X and Z noise alike. Layout-defining: changing it moves cities and highways | worldgen |
| CORE-26 | `debug` and `map` are open to every player | `commands/CommandDebug.java:33` | Permission 0 (also `CommandMap.java:25`); both run large synchronous layout computations on the server thread and print only to the console | performance |
| CORE-27 | `saveprofile` lets any player write files | `commands/CommandSaveProfile.java:29` | Permission 0; writes `<profile>.json` into the server's working directory; failures sent as success messages | gameplay |
| CORE-28 | `exportpart` writes any file name in the server directory | `commands/CommandExportPart.java:159` | The name is a word with `.` allowed, so a level-1 user can overwrite `server.properties`, `ops.json` or `whitelist.json`; the writer is not closed on error | gameplay |
| CORE-29 | `exportpart` output can be corrupt or unstable | `commands/CommandExportPart.java:115` | When the 492-character pool runs out, `null` is written into rows; `missingpalette` follows `HashMap` order | cosmetic |
| CORE-30 | `locatepart` never finds bundled parts | `commands/CommandLocatePart.java:62` | Records store short names (`worldgen/LostCityTerrainFeature.java:1787`), but the argument is compared as a full `namespace:path` | gameplay |
| CORE-31 | `locatepart` result limit not enforced | `commands/CommandLocatePart.java:66` | `break` leaves only the inner loop | cosmetic |
| CORE-32 | `createbuilding` rewrites the shared layout cache | `commands/CommandCreateBuilding.java:71` | `setBuildingType` mutates the cached `BuildingInfo`, so later generation and queries for that chunk see the forced building until eviction | worldgen |
| CORE-33 | Vanilla `/resetchunks` registered in production | `commands/ModCommands.java:40` | Vanilla registers this chunk-regeneration command only in development; Lost Cities adds it everywhere | gameplay |
| CORE-34 | Edit-mode records written from generation threads into a plain map | `editor/EditModeData.java:53` | `addPartData` mutates a `HashMap` and reaches the overworld's data storage from world-generation threads | crash |
| CORE-35 | Edit sessions never expire | `editor/EditorInfo.java:19` | Static map keyed by UUID survives logout and world changes, so `exportpart` can use another world's session | gameplay |
| CORE-36 | Inert network channel accepts any version | `network/PacketHandler.java:27` | Both version predicates accept anything and both handlers are empty | cosmetic |
| CORE-37 | Unused player capability | `setup/ForgeEventHandlers.java:72` | `lostcities:spawnset` is attached to and saved for every player but never read or set, and not copied on respawn | cosmetic |
| CORE-38 | API leaks internals and has dead or misspelled members | `api/LostChunkCharacteristics.java:4` | API types reference internal classes (`PlannedRoadType`, `varia.ChunkCoord`, `cityassets.Palette`); `setCityChancle`; `IChunkPrimerFactory` unused; Javadoc names a nonexistent `ModSetup.GetLostCities` | cosmetic |
| CORE-39 | Bed check parses `specialBedBlock` on every sleep | `setup/ForgeEventHandlers.java:440` | A malformed value throws on every sleep in every dimension; an unknown one resolves to air, so a bed over air ringed by skulls qualifies | gameplay |

<!-- Merged and deduplicated into BUGS.md. Suspected, not confirmed; no fixes designed here. -->
