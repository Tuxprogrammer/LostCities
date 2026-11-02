# Profile selection and dimension info

How a dimension gets a profile, how the per-dimension generation context (`IDimensionInfo`) is built from it, and how outside profiles work.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `setup/Config.getProfileForDimension` and `registerLostCityDimension` (the mapping; out of scope, read for behavior); `worldgen/LostCityFeature.getOrCreateDimensionInfo` (out of scope, read for behavior); `worldgen/IDimensionInfo` (the per-dimension context interface); `worldgen/DefaultDimensionInfo` (its only server implementation; `gui/NullDimensionInfo` is the GUI preview one, 09-client-ui). Config keys: common `profiles.dimensionsWithProfiles`; server `profiles.selectedProfile`, `profiles.selectedCustomJson`. Profile options: `outsideProfile`, `worldStyle`, `generateNether`, `streetGenerationMode`, `highwayGenerationMode`, `cityMinHeight`, `cityMaxHeight`. |
| 2 | **Inputs** | `ProfileSetup.STANDARD_PROFILES`; the common and server configs; `Config.profileFromClient` and `Config.jsonFromClient` (set by the world-creation GUI on the integrated server, 09-client-ui); API calls `ILostCities.registerDimension` and `setOverworldProfile`; the world seed and dimension key; saved data `LostCityWorldGenData` and `LostCityHighwayData`; the world style asset. |
| 3 | **Numbers** | Default dimension list: `lostcities:lostcity=biosphere`, `lostworlds:abyss=biosphere_caves`. Default `selectedProfile`: `<CHECK>`. Default `selectedCustomJson`: `""`. Biome sample point for highway constraints: block (chunkX × 16 + 8, terrain height, chunkZ × 16 + 8). Highway constraint cutoff: potential ≤ 0.0001 is passed through unchanged. |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | The mapping itself is deterministic. `DefaultDimensionInfo.getRandom()` is a thread-local `java.util.Random` seeded once with the world seed; it is stateful, so any caller's result depends on how much the same thread drew before. At the pin its only users reseed it per chunk first (`gen/Stuff`, 07-scattered). The street planner gets the world seed and the dimension id string. The terrain feature gets a `LegacyRandomSource` seeded with the world seed. The persisted street and highway modes make layout depend on when the world was created, by design. `PROF-11` (city centers and city-style choices ignore the world seed) is a determinism defect in the consumers. |
| 6 | **Data formats** | Config TOML keys below. Saved data `LostCityWorldGenData` (overworld data storage, name `LostCityWorldGenData`): booleans `newWorldStreetModes`, `newWorldHighwayModes`; compounds `streetModes` and `highwayModes` mapping dimension id strings to enum names. |
| 7 | **Output** | One `IDimensionInfo` per dimension key that has a profile, cached in `LostCityFeature` until the lifecycle counter changes. `null` for dimensions without a profile, which makes the Lost Cities features do nothing there. |
| 8 | **Relationships** | Called by `LostCityFeature` and `LostCitySphereFeature` on every chunk ([lost-city-dimension.md](lost-city-dimension.md) for how they are attached), by the API (`LostCitiesImp.getLostInfo`, 01-core), by commands (01-core) and by spawn selection (01-core). `DefaultDimensionInfo` builds the street planner (04-city-layout), the highway planner and its saved data (06-highways-railways) and the terrain feature (03-terrain). |
| 9 | **1.20 dependencies** | `ResourceKey<Level>` namespaced dimension ids; Forge `ForgeConfigSpec` common and per-world server configs; `SavedData`; `WorldGenLevel`, `ServerChunkCache`, `BiomeSource.getNoiseBiome` with a `Climate.Sampler` (3D biomes at quart resolution); `Holder<Biome>` and the biome registry; multithreaded chunk generation (`volatile` world field, `ThreadLocal` random). |
| 10 | **Bugs** | `PROF-05`, `PROF-07`, `PROF-08`, `PROF-09`, `PROF-13`, `PROF-21`, `PROF-23`, `PROF-24` |

## Behavior detail

### The dimension-to-profile mapping (server)

`Config.getProfileForDimension(dimension)` returns a profile name or `null`. It builds a static `HashMap` on first use after a reset:

1. For each entry of `dimensionsWithProfiles` (common config, comment `A list of dimensions with associated city generation profiles (format <dimensionid>=<profilename>`):
   - split on `=`; if there are not exactly two parts, log `Bad format for config value: '<entry>'!` and skip;
   - if the profile name is in `STANDARD_PROFILES`, map the dimension id to it; otherwise log `Cannot find profile: <profile> for dimension <dimension>!` and skip.
2. Read `selectedProfile` (server config, default `<CHECK>`). If it is `<CHECK>`, this is a world's first start:
   - if the client handed over a profile name (integrated server, chosen in the world-creation GUI), write it into `selectedProfile`, and write the handed-over JSON (or `""`) into `selectedCustomJson`;
   - otherwise (a dedicated server, or a world created without choosing) write `""` into `selectedProfile`.
   Forge's server config lives in the world's `serverconfig` folder, so this choice is stored per world (*inferred* from Forge 47's autosaving config files).
3. If the selected name is not empty, map `minecraft:overworld` to it. This replaces any overworld entry from step 1. The name is not checked against `STANDARD_PROFILES` (`PROF-05`). If `selectedCustomJson` is not empty, parse it as a profile named `customized` and copy it into `STANDARD_PROFILES["customized"]` (creating a private `customized` profile first if none exists).
4. If the overworld now has a profile and that profile's `generateNether` is true, map `minecraft:the_nether` to `cavern` (`PROF-08`).
5. Return the mapping for the requested dimension.

An empty `selectedProfile` means the overworld is vanilla: no Lost Cities generation. A dedicated server therefore generates no overworld cities unless the operator sets `selectedProfile` by hand.

The cache is cleared by `Config.resetProfileCache()` on `ServerAboutToStartEvent` and `ServerStoppingEvent` (`ForgeEventHandlers.cleanUp`), and `Config.reset()` on server stop also clears the client hand-over values. The cache is built and read without synchronization from worker threads (`PROF-21`).

API entry points (01-core):

- `ILostCities.registerDimension(key, profile)` calls `Config.registerLostCityDimension`, which builds the cache and adds the mapping if the dimension has none. The mapping lives only in the cache and is lost when the cache is reset at server start (`PROF-23`).
- `ILostCities.setOverworldProfile(profile)` calls the client GUI class `GuiLCConfig.selectProfile`, which sets the client hand-over values (`PROF-24`).

### Building the dimension info (server)

`LostCityFeature.getOrCreateDimensionInfo(world)`, called under the feature's lifecycle read lock:

1. Look up the dimension's profile name; `null` → return `null`.
2. Look up the profile in `STANDARD_PROFILES`; missing → return `null` silently.
3. If the dimension has no cached info, create one:
   - the outside profile is `STANDARD_PROFILES[profile.outsideProfile]`, which is `null` for the default `""` and for unknown names, with no warning (`PROF-07`);
   - construct `DefaultDimensionInfo(world, profile, outsideProfile)`.
4. Update the cached info's world reference to the current `WorldGenLevel` and return it.

`DefaultDimensionInfo` construction, in order:

1. Store the world, its seed, its dimension key, the profile and the outside profile.
2. Resolve the world style asset named by the profile's `worldStyle` from the asset registry (05-assets). An unknown name fails inside the asset constructor (`PROF-13`).
3. Ask `LostCityWorldGenData` for the dimension's street mode, passing the profile's `streetGenerationMode` as the request ([Persisted generation modes](#persisted-generation-modes)).
4. Build the `HierarchicalStreetPlanner` from the world seed, the dimension id string and the profile's street settings (04-city-layout). It is built even when the persisted mode is `LEGACY`.
5. Create the thread-local random (seed = world seed) and the `LostCityTerrainFeature` with a `LegacyRandomSource` seeded with the world seed (03-terrain).
6. Ask `LostCityWorldGenData` for the highway mode. If it is `INTERCITY_NETWORK_V1`, build the highway planner settings from the profile, compute a cache signature from the seed, the profile, the outside profile, the settings and the world style id, and build the `IntercityHighwayPlanner` with an `ApproximateCityPotential` (constrained by [the highway city constraint](#highway-city-constraint)), a city-level source and the dimension's slice of `LostCityHighwayData`, plus a `HighwayPlanningService` (06-highways-railways). In `LEGACY` mode both are `null`, and asking for them throws `The inter-city highway planner is unavailable in LEGACY mode` or `The inter-city highway planner service is unavailable in LEGACY mode`.
7. Look up the biome registry.

`shutdown()` closes the highway planning service if there is one.

### What `IDimensionInfo` provides

| Method | Behavior |
|---|---|
| `getSeed`, `getType`, `dimension` | World seed and dimension key, fixed at construction. |
| `getWorld` | The `WorldGenLevel` of the generation context currently open on this thread, if any, else the last world given to `setWorld`. |
| `getProfile`, `getOutsideProfile` | The two profiles; the outside one may be `null`. |
| `getWorldStyle` | The resolved world style ([world-styles.md](world-styles.md)). |
| `getStreetGenerationMode`, `getStreetPlanner` | Persisted street mode and the planner. |
| `getHighwayGenerationMode`, `getHighwayPlanner`, `getHighwayPlanningService` | Persisted highway mode and its planner (throws in `LEGACY`). |
| `getRandom` | Thread-local `Random(worldSeed)`. |
| `getFeature` | The terrain feature. |
| `getHeightmap(chunkX, chunkZ)`, `getHeightmap(coord)` | The terrain feature's heightmap for that chunk (03-terrain). |
| `getBiome(pos)` | On a server chunk cache: the generator's biome source sampled at quart coordinates (x >> 2, y >> 2, z >> 2) with the chunk cache's climate sampler, so the result is the noise biome at that 3D position. Otherwise `minecraft:plains`. |
| `shutdown` | See above. |

### Highway city constraint

`applyHighwayCityConstraints(chunkX, chunkZ, potential)` adjusts a city potential sample for the highway planner:

1. A potential ≤ 0.0001 is returned unchanged.
2. Terrain height outside `cityMinHeight`..`cityMaxHeight` gives 0.
3. Otherwise the potential is multiplied by the world style's city chance multiplier for the biome at (chunkX × 16 + 8, height, chunkZ × 16 + 8) ([world-styles.md](world-styles.md#city-chance-multiplier)).

The planner is also given the world style's maximum multiplier as an upper bound.

### Outside profile

For `space`, `spheres` and `cavernspheres` landscapes (`isSpace() || isSpheres()`), chunks that do not intersect a city sphere are "outside" (03-terrain owns the sphere test). For them:

- `BuildingInfo.getProfile(coord)` returns the outside profile, so building, street, ruin, loot and every other per-chunk option comes from it (04-city-layout);
- `City.isCityCenter` uses the outside profile's `cityChance`, `City.getCityRadius` its radius range, and `BuildingInfo` its `groundLevel` and `seaLevel`;
- the city level is computed by the normal height rule with the outside profile's level thresholds.

A `space` or `spheres` profile without a resolvable outside profile throws `NullPointerException` the first time an outside chunk is generated (`PROF-07`). City chance multipliers always come from the world style named by whichever profile is in use for that chunk.

### Persisted generation modes

`LostCityWorldGenData` (overworld saved data) fixes each dimension's street and highway mode the first time it is asked:

1. A world is "new" when `LevelEvent.CreateSpawnPosition` fires for it, which calls `initializeNewWorld` and sets both new-world flags.
2. The first request for a dimension records a mode: the profile's requested mode if the world is new, otherwise `LEGACY`. Later requests return the recorded mode, whatever the profile now says.
3. An unknown persisted name logs `Unknown persisted street mode '<value>' for <dimension>; using LEGACY` (or the highway equivalent) and uses `LEGACY`.
4. Accessing the data without an overworld throws `Cannot access Lost Cities world generation data without an overworld`.

So worlds created before these modes existed keep legacy streets and highways, and changing the profile's mode never affects an existing world.

### Profiles are referenced by name

A world stores only the selected profile name, except for `customized`, whose full JSON is stored in `selectedCustomJson`. The profile values used are whatever `STANDARD_PROFILES` holds at server start: the current built-in definitions or the current contents of a user profile file (`PROF-09`).

## Config keys (01-core owns the file; listed here because they drive selection)

| File | Key | Type | Default | Comment (exact English) |
|---|---|---|---|---|
| `config/lostcities/common.toml` | `profiles.dimensionsWithProfiles` | list of strings | `["lostcities:lostcity=biosphere", "lostworlds:abyss=biosphere_caves"]` | `A list of dimensions with associated city generation profiles (format <dimensionid>=<profilename>` |
| per-world server config | `profiles.selectedProfile` | string | `<CHECK>` | none |
| per-world server config | `profiles.selectedCustomJson` | string | `""` | none |

## On 1.12

In `upstream/1.12` a profile is bound to a dedicated world type or dimension through `dimensions/world/LostWorldType` and `LostWorldProvider`, which also apply `BIOME_SELECTION_STRATEGY` and an outside profile's strategy (`LostWorldProvider.java:81-86`). There is no overlay on an existing overworld generator.

## Deviations in the new mod

See [new-spec 02-profiles-dimensions § Profile selection and dimension info](../../new-spec/02-profiles-dimensions/index.md#profile-selection-and-dimension-info) and the [pinned profile](../../new-spec/02-profiles-dimensions/pinned-profile.md). [0003](../../design/0003-world-presence.md) drops the Lost City dimension.
