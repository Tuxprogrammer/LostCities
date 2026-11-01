# Mod config and dimension-profile resolution

The mod-level config (`setup/Config.java`): every key in the three Forge config files, the runtime statics that carry the profile chosen on the world-creation screen to the server, and the algorithm that decides which profile, if any, a dimension uses. Profile *contents* (the hundreds of per-profile options) belong to `02-profiles-dimensions`.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `setup/Config` (three `ForgeConfigSpec` builders, the key handles, the profile cache and the avoided-structure set). Config keys: all keys in [Keys](#keys). Profile options read here: `generateNether` (`GENERATE_NETHER`). |
| 2 | **Inputs** | The three TOML files; `Config.profileFromClient` and `Config.jsonFromClient` (set by the world-creation GUI, `09-client-ui`); `ProfileSetup.STANDARD_PROFILES` (`02-profiles-dimensions`); calls through the API (`registerDimension`). |
| 3 | **Numbers** | See [Keys](#keys) and [Constants](#constants). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | No random sources. Output depends on config, not only on seed and profile: the COMMON keys `heightSampleSize` and `optimizedHeightmap` change terrain and highway layout and are per installation, not per world (`CORE-10`). `heightSampleSize` is folded into the highway-data hash (`06-highways-railways`). The avoid keys make layout depend on vanilla structure starts (`04-city-layout`). The profile cache is a plain `HashMap` built lazily from whichever thread asks first (`CORE-05`). |
| 6 | **Data formats** | Three TOML files; every key below. The profile choice is persisted by writing `selectedProfile` and `selectedCustomJson` back into the world's server config. No NBT. |
| 7 | **Output** | A profile name (or none) per dimension for `LostCityFeature.getOrCreateDimensionInfo` (`03-terrain`); key values for the consumers listed in [Keys](#keys). |
| 8 | **Relationships** | Read by `LostCityFeature` (`03`), `GlobalTodo`, `ChunkHeightmap`, `LostCityTerrainFeature` (`03`), `BuildingInfo`, `StructureAvoidance`, `City`, `MultiChunk`, `BiomeInfo`, `ChunkPlanner` (`04`), `LostCityHighwayData` (`06`), the bed teleporter ([bed-teleporter.md](bed-teleporter.md)). Written by the GUI (`09-client-ui`), `LostCitiesImp.registerDimension` ([api.md](api.md)) and the lifecycle cleanup ([lifecycle.md](lifecycle.md)). |
| 9 | **1.20 dependencies** | `ForgeConfigSpec` TOML with per-world SERVER files and auto-save on `set`; dimensions keyed by namespaced `ResourceKey<Level>` (1.7.10 uses integer dimension IDs); structure IDs such as `minecraft:mansion` that have no 1.7.10 counterpart. |
| 10 | **Bugs** | `CORE-04`, `CORE-05`, `CORE-06`, `CORE-07`, `CORE-08`, `CORE-09`, `CORE-10` |

## Keys

Every section is named `profiles` and carries the comment `General settings`. Keys are listed in definition order.

### COMMON: `config/lostcities/common.toml`

| Key | Type | Default | Range | Comment (exact) | Read by |
|---|---|---|---|---|---|
| `dimensionsWithProfiles` | list of strings | `["lostcities:lostcity=biosphere", "lostworlds:abyss=biosphere_caves"]` | each element must be a string | `A list of dimensions with associated city generation profiles (format <dimensionid>=<profilename>` (the closing parenthesis is missing in upstream) | Profile resolution (below) |
| `optimizedHeightmap` | boolean | `false` | none | `If true then a different heightmap generation algorithm is used which should be slightly more efficient. Be careful with this as it might not be 100% compatible with some other terrain generation mods!` | `ChunkHeightmap`, `LostCityTerrainFeature` (`03-terrain`) |
| `heightSampleSize` | int | `3` | 1 to 100 | `The size of the chunk grid used for heightmap sampling. Default is 1 which means every chunk is sampled. Higher values will sample less chunks and thus be faster but also less accurate` (says default 1; it is 3, `CORE-08`) | `LostCityTerrainFeature` (`03`); `BuildingInfo` when the profile's `USE_AVG_HEIGHTMAP` is on and the value is above 2 (`04`); highway-data hash (`06`) |

### SERVER: `<world>/serverconfig/lostcities-server.toml`

| Key | Type | Default | Range | Comment (exact) | Read by |
|---|---|---|---|---|---|
| `specialBedBlock` | string | `minecraft:diamond_block` | none | `Block to put underneath a bed so that it qualifies as a teleporter bed` | [bed-teleporter.md](bed-teleporter.md) |
| `selectedProfile` | string | `<CHECK>` | none | none | Profile resolution |
| `selectedCustomJson` | string | empty | none | none | Profile resolution |
| `todoQueueSize` | int | `20` | 1 to 100000 | `The size of the todo queues for the lost city generator` | `GlobalTodo` (`03-terrain`) |
| `forceSaplingGrowth` | boolean | `true` | none | `If this is true then saplings will grow into trees during generation. This is more expensive` | `LostCityTerrainFeature` (`03`) |
| `cacheCleanupSeconds` | int | `300` (seconds) | 1 to 86400 | `Time in seconds after which cached chunk data is evicted` | Time-to-live of every `TimedCache` ([utilities.md](utilities.md)): biome info, multichunks, city styles, chunk plans, city levels, heightmaps |
| `avoidStructures` | list of strings | `minecraft:mansion`, `minecraft:jungle_pyramid`, `minecraft:desert_pyramid`, `minecraft:igloo`, `minecraft:swamp_huts`, `minecraft:pillager_outpost` | each element must be a string | `List of structures to avoid when generating cities (for example to avoid generating a city in a woodland mansion)` | `StructureAvoidance` (`04-city-layout`). `minecraft:swamp_huts` names no structure (`CORE-07`) |
| `avoidStructuresAdjacent` | boolean | `false` | none | `If true then also avoid generating the structures mentioned in 'avoidStructures' in chunks adjacent to the chunk with the structure` | `StructureAvoidance` |
| `avoidVillagesAdjacent` | boolean | `false` | none | `If true then also avoid generating cities in chunks adjacent to the chunks with villages` | `StructureAvoidance` |
| `avoidVillages` | boolean | `true` | none | `If true then avoid generating cities in chunks with villages` | `StructureAvoidance` |
| `avoidFlattening` | boolean | `true` | none | `If true then avoid flattening the terrain around the city in case there was a structure that was avoided` | `StructureAvoidance`, `LostCityTerrainFeature` |

### CLIENT: `config/lostcities/client.toml`

Only the empty `profiles` section. No keys.

## Constants

| Name | Value | Use |
|---|---|---|
| `CATEGORY_PROFILES` | `profiles` | Section name in all three files |
| `CATEGORY_GENERAL` | `general` | Unused |
| `DEBUG` | `false` | Compile-time constant, unused |
| `<CHECK>` | sentinel default of `selectedProfile` | "Take the profile from the client" (below) |
| `customized` | profile name | Name of the GUI-customized profile |
| `cavern` | profile name | Profile given to the nether when the overworld profile has `generateNether` |

## Behavior detail

### Runtime statics

| Static | Meaning | Set by | Cleared by |
|---|---|---|---|
| `profileFromClient` | Profile name picked on the world-creation screen | GUI `selectProfile` (`09-client-ui`) | `Config.reset` (server stopping, client logout) |
| `jsonFromClient` | JSON of the customized profile, if any | GUI `selectProfile` with a profile | `Config.reset` |
| `dimensionProfileCache` | `Map<ResourceKey<Level>, String>` from dimension to profile name; `null` means "rebuild on next use" | Profile resolution, `registerLostCityDimension` | `reset`, `resetProfileCache` |
| `AVOID_STRUCTURES_SET` | Parsed `avoidStructures` | First `isAvoidedStructure` or `hasAvoidedStructures` call | Never (`CORE-09`) |

### Profile resolution: `getProfileForDimension(dimension)` (server)

1. If the cache is non-null, return its entry for the dimension, or `null`.
2. Otherwise create an empty `HashMap` and fill it:
   1. For each `dimensionsWithProfiles` entry, split on `=`. With other than two parts, log `Bad format for config value: '<entry>'!` and skip. Otherwise, if the profile name is a standard profile, map the dimension to it; else log `Cannot find profile: <name> for dimension <dimension>!` and skip. A malformed dimension ID throws from the `ResourceLocation` constructor.
   2. Read `selectedProfile`. If it equals `<CHECK>`:
      - if `profileFromClient` is non-empty: write it to `selectedProfile`, and write `jsonFromClient` (or an empty string) to `selectedCustomJson`. Forge saves the server config file on `set`, so the choice is persisted in the world;
      - else write an empty string to `selectedProfile`.
   3. If the selected profile is non-empty, map the overworld to it. If `selectedCustomJson` is non-empty, parse it into a profile named `customized`, create a non-public `customized` standard profile if none exists, and copy the parsed values into it.
   4. Look up the overworld's profile (recursive call, served from the half-built cache). If it is non-empty and that standard profile has `generateNether`, map the nether to `cavern`. An overworld profile name that is not a standard profile throws a `NullPointerException` here (`CORE-04`).
3. Return the entry for the requested dimension.

Consequences:
- **Single player:** the GUI sets `profileFromClient` before the integrated server starts. A new world's server config holds `<CHECK>`, so the first resolution copies the choice into the world. An existing world already holds a name (or empty), so a later GUI choice is ignored.
- **Dedicated server:** no client choice exists, so a new world stores an empty `selectedProfile` and the overworld gets no cities unless the owner sets `selectedProfile` (or ships it through Forge's `defaultconfigs`). `dimensionsWithProfiles` still applies.
- With the defaults, `lostcities:lostcity` uses `biosphere` and `lostworlds:abyss` (a dimension of the companion mod Lost Worlds) uses `biosphere_caves`.

### Dimension registration through the API

`registerLostCityDimension(dimension, profile)` resolves the dimension first (which builds the cache) and, if it has no profile, puts the given one into the cache. The cache is discarded by every `reset` or `resetProfileCache`: server start and stop, every lifecycle cleanup, the GUI, the client logout handler. Registrations made before such a reset are lost (`CORE-06`). The API Javadoc tells callers to re-register after loading a world.

### Avoided structures

`isAvoidedStructure(id)` and `hasAvoidedStructures()` parse `avoidStructures` into a set of `ResourceLocation` once and keep it for the life of the JVM. A malformed entry throws when first parsed, which happens during chunk generation (`CORE-09`).

## On 1.12

`config/LostCityConfiguration.java` on `upstream/1.12` uses Forge's `Configuration` class (present in 1.7.10) with a `general` category, a config `version`, lists of profiles and private profiles, `DIMENSION_ID = 111`, `DIMENSION_PROFILE`, `DEFAULT_PROFILE`, `SPECIAL_BED_BLOCK`, an asset list (`ASSETS`), `ADAPTING_WORLDTYPES` and `BLOCKS_REQUIRING_LIGHTING_UPDATES`. There the profile is chosen through the world type, not a sentinel in a per-world file.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
