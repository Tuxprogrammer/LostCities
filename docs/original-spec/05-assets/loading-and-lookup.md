# Asset loading, registries and lookup

How the JSON asset system gets from files in a data pack to compiled Java objects that world generation asks for by name. Covers the thirteen asset types, data-pack overrides, name resolution, caching, reset and the public registry interface. The JSON fields of each type are in their own sheets ([Contents](index.md#contents)).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `setup/CustomRegistries` declares thirteen data-pack registries. `worldgen/lost/regassets/*RE` are the raw records, one per JSON file, decoded by a codec. `worldgen/lost/cityassets/AssetRegistries` holds one `RegistryAssetRegistry<T, R>` per type, which turns a raw record `R` into a compiled asset `T` on first use and caches it. `regassets/IAsset` lets the registry stamp the file's id into the raw record (`setRegistryName`). `regassets/data/DataTools` converts between short names and ids. `api/ILostCityAsset` and `api/ILostCityAssetRegistry` are the public view. Config keys and profile options: none. |
| 2 | **Inputs** | JSON files in every enabled data pack (the mod's own jar is one), the level's `RegistryAccess`, and asset names given by other assets, the profile (`worldStyle`, `CITY_STYLE_ALTERNATIVE`) and code defaults. |
| 3 | **Numbers** | 13 registries. Bundled files: 322 (see the table below). Default namespace for unqualified names: `lostcities`. No size limits, no versioning. |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Loading and lookup use no randomness. The caches are `ConcurrentHashMap`s, so `getIterable()` order is hash order of the ids (stable for a given set of ids on one JVM, not insertion order). `STUFF_BY_TAG` lists are built from that order, and stuff generation consumes a shared random in list order ([ASSET-15](index.md#suspected-bugs)). |
| 6 | **Data formats** | One JSON object per file; folder and file name give the id (table below). Unknown keys are ignored by the codecs. No NBT or saved data is written by this system. |
| 7 | **Output** | Compiled assets (`Building`, `BuildingPart`, `Palette`, `Variant`, `Style`, `CityStyle`, `Condition`, `MultiBuilding`, `WorldStyle`, `ScatteredBuilding`, `PredefinedCity`, `PredefinedSphere`, `StuffObject`) and the `STUFF_BY_TAG` index, handed to `03-terrain`, `04-city-layout`, `06-highways-railways` and `07-scattered`. |
| 8 | **Relationships** | Called by `ForgeEventHandlers.onWorldTick` (preload), `LostCityFeature.cleanUp` and `ModSetup.init` (reset), `City` and `CitySphere` (predefined assets), `SpawnBuildingPrefilter` (multibuildings), `ModCommands` (part and building listings), and every generator through `AssetRegistries.<TYPE>.get/getOrThrow/getOrWarn`. Mod lifecycle is in `01-core`; the profile's world-style choice in `02-profiles-dimensions`. |
| 9 | **1.20 dependencies** | Forge `DataPackRegistryEvent.NewRegistry` and `DeferredRegister` for custom data-pack registries; Mojang DFU codecs; `RegistryAccess` lookups through a `CommonLevelAccessor`; `ResourceLocation` ids; data-pack ordering for overrides. See the [index table](index.md#120-dependencies). |
| 10 | **Bugs** | [ASSET-01](index.md#suspected-bugs) (`getOrWarn` throws), [ASSET-15](index.md#suspected-bugs) (stuff index filled late), [ASSET-23](index.md#suspected-bugs) and [ASSET-24](index.md#suspected-bugs) (decoding accepts malformed values that crash later). |

## Registries and folders

Forge places a custom data-pack registry whose key is `lostcities:<type>` in the folder `data/<namespace>/lostcities/<type>/`. A file `data/<ns>/lostcities/<type>/<path>.json` registers id `<ns>:<path>`.

| Registry key | Folder | Raw record | Compiled asset | Bundled files | Preloaded by `load` | Spec sheet |
|---|---|---|---|---|---|---|
| `lostcities:buildings` | `buildings/` | `BuildingRE` | `Building` | 37 | yes | [buildings-and-multibuildings.md](buildings-and-multibuildings.md) |
| `lostcities:multibuildings` | `multibuildings/` | `MultiBuildingRE` | `MultiBuilding` | 14 | no (`SpawnBuildingPrefilter` calls `loadAll`) | [buildings-and-multibuildings.md](buildings-and-multibuildings.md) |
| `lostcities:parts` | `parts/` | `BuildingPartRE` | `BuildingPart` | 207 | yes | [parts-and-placement.md](parts-and-placement.md) |
| `lostcities:palettes` | `palettes/` | `PaletteRE` | `Palette` | 30 | no | [palettes-and-variants.md](palettes-and-variants.md) |
| `lostcities:variants` | `variants/` | `VariantRE` | `Variant` | 12 | no | [palettes-and-variants.md](palettes-and-variants.md) |
| `lostcities:styles` | `styles/` | `StyleRE` | `Style` | 4 | no | [styles-and-city-styles.md](styles-and-city-styles.md) |
| `lostcities:citystyles` | `citystyles/` | `CityStyleRE` | `CityStyle` | 5 | no | [styles-and-city-styles.md](styles-and-city-styles.md) |
| `lostcities:conditions` | `conditions/` | `ConditionRE` | `Condition` | 3 | no | [conditions.md](conditions.md) |
| `lostcities:worldstyles` | `worldstyles/` | `WorldStyleRE` | `WorldStyle` | 2 | no | [world-styles.md](world-styles.md) |
| `lostcities:scattered` | `scattered/` | `ScatteredRE` | `ScatteredBuilding` | 5 | no | [scattered-stuff-predefined.md](scattered-stuff-predefined.md) |
| `lostcities:stuff` | `stuff/` | `StuffSettingsRE` | `StuffObject` | 3 | yes, plus the tag index | [scattered-stuff-predefined.md](scattered-stuff-predefined.md) |
| `lostcities:predefinedcities` | `predefinedcities/` | `PredefinedCityRE` | `PredefinedCity` | 0 | by `loadPredefinedStuff` | [scattered-stuff-predefined.md](scattered-stuff-predefined.md) |
| `lostcities:predefinedspheres` | `predefinedspheres/` | `PredefinedSphereRE` | `PredefinedSphere` | 0 | by `loadPredefinedStuff` | [scattered-stuff-predefined.md](scattered-stuff-predefined.md) |

The mod's own data (`src/main/resources/data/lostcities/...`) also carries files outside these registries (dimension, dimension type, biome modifiers, two chest loot tables, configured and placed features); those belong to `02-profiles-dimensions`, `03-terrain` and `08-loot-mobs-blocks`. The six block tags that the asset system relies on (`lostcities:rotatable`, `lights`, `needspoi`, `foliage`, `easybreakable`, `notbreakable`) are generated by datagen into `src/generated/resources` and are owned by `08-loot-mobs-blocks`.

## Behavior detail

1. (both) **Registration.** The mod constructor calls `CustomRegistries.init(bus)` (registers the thirteen `DeferredRegister`s, which hold no entries) and listens for `DataPackRegistryEvent.NewRegistry`, where each key is registered with its codec. No network codec is given, so the registries exist on the server only and are not synced to clients.
2. (server) **Decoding.** When a world loads, Minecraft reads every data pack in priority order and decodes each JSON file with the type's codec. A file that fails to decode, or whose constructor throws (for example an empty `char` string, or an unknown sphere `centertype`), aborts world loading with the data-pack error screen or a crash, depending on where it throws. The registries are frozen for the world's lifetime: `/reload` does not re-read them.
3. (server) **Override.** A higher-priority pack that ships a file at the same `data/<ns>/lostcities/<type>/<path>.json` replaces the whole lower-priority file. There is no field-level merge and no way to delete an asset. Adding a file with a new id adds an asset; it is used only if something refers to it by name (city styles, world styles, buildings, profiles). Data packs in their own namespace must refer to their assets with a full `ns:name`, because unqualified names resolve to `lostcities:`.
4. (server) **Name resolution.** `DataTools.fromName(name)`: a name containing `:` is parsed as a full id; otherwise it becomes `lostcities:<name>`. `DataTools.toName(id)` returns the bare path for the `lostcities` namespace and the full id otherwise. `getName()` on every compiled asset uses `toName`, and condition tests compare against those short names ([conditions.md](conditions.md)).
5. (server) **Lookup**, `RegistryAssetRegistry.get(level, id)`:
   1. Return the cached compiled asset if present.
   2. Otherwise fetch the registry from `level.registryAccess()`, read the raw record for the id, stamp the id into it (`setRegistryName`), and run the type's constructor, which compiles it (palettes resolve their blocks at this point).
   3. Store it with `putIfAbsent`; if another thread won the race, use the stored one.
   4. Call `init(level)` on the result, every time, not only on first creation. Only `CityStyle` does work there (inheritance, once).
   5. Any exception while fetching or compiling is rethrown as `RuntimeException("Error getting resource <id>!")`. A missing id makes the constructor dereference a null record, so a missing name always throws ([ASSET-01](index.md#suspected-bugs)).
6. (server) **Lookup variants.**

   | Method | Null name | Blank name | Unknown name |
   |---|---|---|---|
   | `get(level, String)` | returns null | throws (`fromName("")` gives `lostcities:`, which is missing) | throws |
   | `getOrThrow` | throws "Invalid name given to …" | throws | throws "Can't find …" in principle, but `get` throws first |
   | `getOrWarn` | returns null | returns null | throws (the warning branch is unreachable) |

7. (server) **Preload**, `AssetRegistries.load(level)`: guarded by a volatile `loaded` flag and double-checked locking. Runs `loadAll` for parts, buildings and stuff (compile every entry in the registry that is not cached yet; `init` is not called), then builds `STUFF_BY_TAG`: tag → immutable list of stuff objects having that tag, in cache iteration order. It is called at the end of every server-level tick (`ForgeEventHandlers.onWorldTick`), so it first runs after the first tick; chunks generated before that (spawn preparation) see an empty stuff index ([ASSET-15](index.md#suspected-bugs)).
8. (server) **Predefined preload**, `loadPredefinedStuff(level)`: same pattern with its own flag; compiles all predefined cities and spheres. Called by `City` and `CitySphere` before they iterate them.
9. (server) **Iteration.** `getIterable()` returns only what has been compiled so far. Callers that need every asset call `loadAll` first (multibuildings, predefined assets) or rely on `load` (parts, buildings, stuff).
10. (server) **Reset**, `AssetRegistries.reset()`: clears every cache and the stuff index and resets both flags. Called from `ModSetup.init` (common setup), and from `LostCityFeature.cleanUp` on `ServerAboutToStartEvent` and `ServerStoppingEvent`, so a new world in the same session recompiles from its own data packs.
11. (server) **Counts.** `getNumAssets(level)` returns the size of the underlying registry, not of the cache.

## Thread safety

World generation runs on worker threads in 1.20. The caches are concurrent maps, `load` and `loadPredefinedStuff` are synchronized on the class, and lazily-built fields (`BuildingPart.vslices`, `BuildingPart.localPalette`, `Building.localPalette`, `CityStyle` inheritance) use volatile fields with double-checked locking. Two threads may both compile the same asset; the loser's copy is discarded.

## On 1.12

Branch `1.12` (`upstream/1.12`, `e8900023`) has no registries. `ModSetup.postInit` resets `AssetRegistries` and reads a list of files from the config key `assets` (`LostCityConfiguration.ASSETS`): jar resources such as `/assets/lostcities/citydata/palette.json`, `buildingparts.json`, `library.json`, and `$lostcities/userassets.json` from the config folder. Each file is a JSON array of objects with a `"type"` field (`style`, `condition`, `palette`, `citystyle`, `part`, `building`, `multibuilding`, `worldstyle`, `city`, `sphere`) and a `"name"`. `AbstractAssetRegistry` stores them in a `HashMap` by name, so a later file overrides an earlier one with the same name. Lookups return null for unknown names. This is close to what 1.7.10 can do (load at `postInit` from the jar and the config folder), and the field names are mostly the same.
