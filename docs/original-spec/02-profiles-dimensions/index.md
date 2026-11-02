# 02 · Profiles and dimensions

## Overview

This area documents how Lost Cities 1.20-7.5.7 (upstream `1.20` at `f18a094d`) is configured per world: the `LostCityProfile` and every one of its 163 options, the 17 built-in profiles, how profiles are stored, loaded, copied and validated, which profile each dimension gets, the per-dimension generation context (`IDimensionInfo`), landscape types, how the world style picks city styles and scales city chance by biome, how city styles override profile options, and the Lost City dimension with the biome modifiers that attach the generator to biomes.

## Contents

- [profile-system.md](profile-system.md): profile class, JSON format, startup write and read, copying, validation, API profile creation
- [profile-options.md](profile-options.md): catalog of all 163 options with type, default, range, meaning, consumer and built-in overrides; validation messages; level heights
- [builtin-profiles.md](builtin-profiles.md): catalog of the 17 built-in profiles, their exact texts, icons and overrides
- [profile-selection.md](profile-selection.md): dimension-to-profile mapping, world-creation hand-over, `DefaultDimensionInfo` and `IDimensionInfo`, outside profiles, persisted street and highway modes
- [landscape-types.md](landscape-types.md): the six landscape types and every branch they drive
- [world-styles.md](world-styles.md): world style behavior, biome matching and sampling, city chance multipliers, city style choice, the two bundled world styles
- [effective-city-settings.md](effective-city-settings.md): city style overrides and constraints on profile options
- [lost-city-dimension.md](lost-city-dimension.md): the dimension and dimension type JSON, and the biome modifiers that attach the features

## Key facts

- **The selection chain is profile → world style → city style.** A dimension has one profile; the profile names one world style; the world style picks a city style per chunk by biome; the city style may override about a dozen profile values ([effective-city-settings.md](effective-city-settings.md)).
- **A profile is a flat set of 163 options in five categories.** There is no profile inheritance at load time. Built-ins are defined in code, rewritten as read-only JSON to `config/lostcities/profiles/` on every start and read back; extra JSON files there add user profiles. The API's `createProfile` and the GUI's `customized` profile take a one-time copy of a base profile.
- **Declared ranges are not enforced when a profile file is loaded** (`PROF-04`). Only the road, railway, highway and enum options are checked, and a failed check, a malformed file or a wrong value type aborts mod loading (`PROF-02`).
- **Nine options are config-only or dead**: `horizon`, the four `fog*` options, `sphereSurfaceVariation`, `outsideSurfaceVariation`, `outsideGroundLevel`, and `generateNether` in effect (`PROF-08`, `PROF-17`). `BiomeSelectionStrategy` is dead code.
- **Overworld cities only happen when a profile is selected.** The world-creation GUI hands the choice to the integrated server, which stores it in the world's server config. A dedicated server stores an empty selection and generates a vanilla overworld unless the operator edits `selectedProfile` ([profile-selection.md](profile-selection.md)).
- **Worlds reference profiles by name** and pick up later changes to the profile's definition (`PROF-09`), except street and highway modes, which are fixed per dimension at first use.
- **Landscape types do not generate terrain at the pin.** They adapt cities to terrain from another generator (vanilla noise or Lost Worlds) ([landscape-types.md](landscape-types.md)).
- **Every height is a 1.20 overworld number.** Ground level 71, level thresholds 75–131, sea level 63 via the generator, ranges up to 384 ([profile-options.md](profile-options.md#120-height-assumptions)).
- **The Lost City dimension exists in every world** (data-pack dimension, Y 0..255) and is mapped to `biosphere` by default. The generator runs in any dimension whose biomes are tagged `#minecraft:is_overworld`.
- **City placement randomness partly ignores the world seed** (`PROF-11`), which matters for any determinism test that varies the seed.

## Related areas

- `01-core`: mod config file, lifecycle (`ModSetup.preInit`), IMC and the public API, spawn selection that reads the spawn options, edit-mode commands, profile network sync.
- `03-terrain`: consumers of terrain, explosion, sphere, rubble, vine and bedrock options; heightmaps used for city levels and biome sampling; the placed and configured features.
- `04-city-layout`: consumers of city, building, park, street and corridor options; `City` and `BuildingInfo`, which call the world style; `MultiSettings`.
- `05-assets`: the JSON schema of world styles, city styles (including `profile_overrides` and `inherit`) and conditions; asset registries.
- `06-highways-railways`: consumers of highway, railway and monorail options; the highway planner built by `DefaultDimensionInfo`.
- `07-scattered`: `scatteredChanceMultiplier` and the world style's `scattered` block.
- `08-loot-mobs-blocks`: loot, spawner and lighting options; `groundLevel` as the base of the condition `level`.
- `09-client-ui`: profile list, icons, descriptions, option editor (the only place ranges are enforced), `NullDimensionInfo` preview, the client hand-over of the selected profile.

## Coverage

- Source files read: 25 of 25 in scope, all in full.
  - Java (19): `config/BiomeSelectionStrategy`, `Configuration`, `HighwayGenerationMode`, `LandscapeType`, `LostCityProfile`, `LostCityProfileSetupImp`, `MultiBuildingStreetConflict`, `ProfileSetup`, `StreetGenerationMode`; `worldgen/DefaultDimensionInfo`, `IDimensionInfo`; `worldgen/lost/BiomeInfo`; `worldgen/lost/cityassets/WorldStyle`, `EffectiveCitySettings`; `worldgen/lost/regassets/data/WorldSettings`, `CityProfileOverrides`, `CityBiomeMultiplier`, `CityStyleSelector`, `BiomeMatcher`.
  - Resources (6): `data/lostcities/dimension/lostcity.json`, `dimension_type/lostcity.json`, `forge/biome_modifier/lostcities.json`, `forge/biome_modifier/lostcity_spheres.json`, `lostcities/worldstyles/standard.json`, `standard_everywhere.json`.
- In-scope files not covered: none.
- Out-of-scope files read, for behavior only: `setup/Config.java` (full), `setup/ModSetup.java`, `setup/Registration.java`, `setup/ClientEventHandlers.java`, `LostCities.java`, `LostCitiesImp.java`, `LostCitiesPreImp.java`, `api/ILostCityProfile.java`, `api/ILostCityProfileSetup.java`, and parts of `setup/ForgeEventHandlers.java`, `worldgen/LostCityFeature.java`, `worldgen/LostCityTerrainFeature.java`, `worldgen/LostCityWorldGenData.java`, `worldgen/ChunkHeightmap.java`, `worldgen/gen/{Spheres,Highways,Scattered}.java`, `worldgen/lost/{City,BuildingInfo,DamageArea}.java`, `worldgen/lost/cityassets/{CityStyle,RegistryAssetRegistry}.java`, `worldgen/lost/regassets/{WorldStyleRE,CityStyleRE}.java`, `regassets/data/MultiSettings.java`, `gui/GuiLCConfig.java`, `data/lostcities/worldgen/{configured_feature,placed_feature}/lostcities.json`, upstream `profile_plan.md` (intent only), and on `upstream/1.12` `config/LostCityProfile.java`, `config/LandscapeType.java`.
- Profile options covered: 163 of 163. Built-in profiles: 17 of 17. Bundled world styles: 2 of 2.
- Lang keys in scope: 0 of 0. Hard-coded English in scope: profile descriptions, extra descriptions and warnings (all in [builtin-profiles.md](builtin-profiles.md)), validation and log messages (in the sheets), the 163 option comments (shown as GUI tooltips, owned by 09-client-ui; meanings paraphrased in [profile-options.md](profile-options.md)), the read-only marker text, and the dimension type `name`.
- Textures: profile icons are listed by file name only; sizes and content belong to 09-client-ui.
- Corrections to earlier statements: none (first version, 2026-11-01).

## Known limits

- Nothing was run. Claims about generated terrain (the clamped Lost City dimension, `PROF-20`) and about Forge config persistence are *inferred* from code and from knowledge of Forge 47 and Minecraft 1.20.1.
- The behavior of landscape types is summarized from call sites; the algorithms are verified only as far as needed for the map in [landscape-types.md](landscape-types.md) and belong to the owning areas.
- `PROF-23` and `PROF-24` depend on mod-loading order and on `GuiLCConfig` failing to load on a dedicated server; both are inferred, not observed.
- McJty's online profile documentation was not consulted; the code at the pin is the source.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| World height -64..320 and Y values calibrated to 1.20 vanilla terrain | `LostCityProfile` defaults and ranges: `groundLevel` 71 (2..256), `cityLevel0Height`…`cityLevel7Height` 75..131 (1..384), `cityMinHeight`/`cityMaxHeight` 50/150 (-1024..2048), explosion heights 60..100 (1..256), `seaLevel` (-1..256), `horizon` (-1..256); built-in level tables; `bedrockLayer` counted from the minimum build height | 1.7.10 is Y 0..255 and RWG has its own height profile and sea level; every Y number and range needs remapping in the new spec. |
| Generator sea level lookup | `seaLevel` = -1 resolves through `ChunkGenerator.getSeaLevel` (`BuildingInfo.java:859`, `DamageArea.java:83`) | 1.7.10 has no generator sea-level getter; sea level is 63 or RWG's. |
| Namespaced dimension keys (`ResourceKey<Level>`) | `Config.getProfileForDimension`, `IDimensionInfo.getType`, the dimension list `lostcities:lostcity=biosphere`, `lostworlds:abyss=biosphere_caves` | 1.7.10 dimensions are integer IDs. |
| Forge common and per-world server config (`ForgeConfigSpec`, autosaved TOML in `serverconfig`) | `setup/Config.java` keys `dimensionsWithProfiles`, `selectedProfile`, `selectedCustomJson` | 1.7.10 Forge `Configuration` files are global; storing the selected profile per world needs another home. |
| Data-pack dimension and dimension type | `dimension/lostcity.json`, `dimension_type/lostcity.json` | 1.7.10 registers dimensions in code; the dimension is not ported ([0003](../../design/0003-world-presence.md)). |
| Forge biome modifiers and placed-feature steps | `forge/biome_modifier/lostcities.json` (`raw_generation`), `lostcity_spheres.json` (`top_layer_modification`), biome tag `#minecraft:is_overworld` | 1.7.10 has no biome modifiers or feature steps; the hook into RWG generation is spike S2. |
| Noise generator with overworld noise settings and multi-noise biomes | `dimension/lostcity.json` | No counterpart in 1.7.10; irrelevant once the dimension is dropped. |
| 3D noise biomes sampled at a Y with a `Climate.Sampler` | `DefaultDimensionInfo.getBiome`, `BiomeInfo.getBiomeInfo` (samples at terrain height) | 1.7.10 biomes are 2D per column (`WorldChunkManager`), and RWG provides its own biome layout. |
| Biome holders, holder sets and biome tags | `BiomeMatcher`; `standard.json` uses `#minecraft:is_ocean`, `#minecraft:is_river`, `#minecraft:is_beach`, `#minecraft:is_deep_ocean`, `minecraft:desert`, `minecraft:badlands` | 1.7.10 has numeric biome IDs and `BiomeDictionary` types; badlands is "Mesa"; RWG adds its own biomes. |
| DFU codecs and data-pack asset registries | `WorldStyleRE`, `CityStyleSelector`, `CityBiomeMultiplier`, `WorldSettings` (`StringRepresentable` enum), `CityProfileOverrides` (`Codec.floatRange`), `BiomeMatcher` | No codecs or data packs in 1.7.10; assets need plain Java parsing (CONTRIBUTING § Ported code). |
| `BlockState` with properties and the Forge block registry | `WorldSettings` vine states (`minecraft:vine` with one side true); `liquidBlock`, `baseBlock` resolved through `ForgeRegistries.BLOCKS` | 1.7.10 uses block plus metadata (vines use a side bitmask) and `GameRegistry`/`Block.blockRegistry` names. |
| `SavedData` | `LostCityWorldGenData` (street and highway modes), `LostCityHighwayData` | 1.7.10 `WorldSavedData` through `MapStorage` is a direct counterpart. |
| Multithreaded chunk generation | `DefaultDimensionInfo` (`volatile` world, `ThreadLocal` random, context world lookup), the unsynchronized profile cache (`PROF-21`) | 1.7.10 generates on the server thread; the thread machinery has no purpose there. |
| `LevelEvent.CreateSpawnPosition` as the new-world signal | `ForgeEventHandlers.onCreateSpawnPoint` → `LostCityWorldGenData.initializeNewWorld` | 1.7.10 Forge has `WorldEvent.CreateSpawnPosition`; to be confirmed against the pack's Forge build. |
| Mod lifecycle: constructor-time setup, `FMLConstructModEvent` IMC, `FMLPaths.CONFIGDIR` | `LostCities` constructor, `ProfileSetup.setupProfiles` | 1.7.10 FML has preInit/init/postInit and `FMLInterModComms` processed in `IMCEvent`; the config directory comes from the preInit event. |
| Java 16+ language and API | records (`EffectiveCitySettings`, `WorldSettings`, `CityStyleSelector`, `CityBiomeMultiplier`, `CityProfileOverrides`), `List.of`/`List.copyOf` in `Configuration`, switch expressions | The runtime is Java 8: no records, no `List.of` (CONTRIBUTING § Toolchain). |
| `FriendlyByteBuf` profile sync | `LostCityProfile.toBytes` | 1.7.10 uses Netty `ByteBuf`/`PacketBuffer`; owned by 01-core. |
| Lost Worlds world types as companion terrain | profile `warning` texts; landscape types rely on another generator's terrain | Lost Worlds does not exist for 1.7.10 GTNH; RWG supplies the terrain ([0003](../../design/0003-world-presence.md)). |

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| PROF-01 | Profile API callbacks never run | `LostCities.java:52` | `setup.preInit()` runs `ProfileSetup.setupProfiles` (which applies `profileSetups` at `config/ProfileSetup.java:453`) in the mod constructor, before the `FMLConstructModEvent` handler (`LostCities.java:66-71`) registers `ILostCitiesPre` callbacks, so third-party profiles from `registerProfileSetupCallback` are never created. | gameplay |
| PROF-02 | Malformed profile file aborts mod loading | `config/ProfileSetup.java:483` | Only `IOException` is caught. Malformed JSON, a wrong value type (`ClassCastException` in `Configuration.getFloat`/`getInt`/`getBoolean`/`getString`, `config/Configuration.java:202`), an unknown landscape type (`LostCityProfile.java:373`) or a failed road/railway/highway check throw out of the mod constructor. | crash |
| PROF-03 | One unreadable profile stops reading the rest | `config/ProfileSetup.java:490` | `return` inside the file loop skips every remaining profile file after the first `IOException`. | gameplay |
| PROF-04 | Declared option ranges are not enforced on load | `config/Configuration.java:158` | `fromJson` stores numbers with min = max = 0 and `constrain()` is only called by the GUI for one edited value, so out-of-range file values (negative chances, `groundLevel` 500, a `highwayDistanceMask` that is not a power of two minus one) reach generation unchecked. | worldgen |
| PROF-05 | Unknown selected overworld profile crashes | `setup/Config.java:122` | `selectedProfile` is mapped without checking that the profile exists; `STANDARD_PROFILES.get(profile).GENERATE_NETHER` then throws `NullPointerException` on every dimension lookup. | crash |
| PROF-06 | Unchecked min/max option pairs crash generation | `worldgen/lost/DamageArea.java:105` | `Random.nextInt(max - min)` with no check that max > min for explosion radius and height (also `:107`, `:116`, `:118`), the outside profile's city radius (`worldgen/lost/City.java:225`, no guard unlike the main profile), and the floors bound (`worldgen/lost/BuildingInfo.java:928`) when `buildingMaxFloorsChance` is below `buildingMinFloorsChance`. | crash |
| PROF-07 | Missing outside profile crashes sphere worlds | `worldgen/LostCityFeature.java:199` | An empty (default) or unknown `outsideProfile` resolves to `null` with no warning; a `space`, `spheres` or `cavernspheres` profile then throws `NullPointerException` on the first outside chunk (`worldgen/lost/City.java:193`, `worldgen/lost/BuildingInfo.java:853`). | crash |
| PROF-08 | `generateNether` has no effect | `setup/Config.java:122` | It maps the Nether to `cavern`, but the features are only attached to `#minecraft:is_overworld` biomes (`data/lostcities/forge/biome_modifier/lostcities.json`), so nothing generates there. | gameplay |
| PROF-09 | Existing worlds follow live profile definitions | `setup/Config.java:109` | Only the profile name is stored per world (except `customized`); built-in files are rewritten from code every start and user files can be edited, so a mod update or edit changes new chunks of existing worlds and breaks borders with old chunks. | worldgen |
| PROF-10 | City style rolled per chunk instead of per city | `worldgen/lost/City.java:282` | `getCityStyleForCityCenter(coord, …)` is passed the current chunk instead of the city center `c`, so each chunk rolls its own "center" style from its own coordinates and biome, and one city mixes styles (for example at desert edges). | worldgen |
| PROF-11 | City centers and center styles ignore the world seed | `worldgen/lost/City.java:187` | `isCityCenter` (and `getCityStyleForCityCenter`, `:243`) seed `Random` from chunk coordinates only, so in chance mode every world has city centers and center-style rolls at the same coordinates regardless of seed. | worldgen |
| PROF-12 | Unresolvable city style crashes generation | `worldgen/lost/City.java:301` | `WorldStyle.getRandomCityStyle` returns `null` when no selector matches (`worldgen/lost/cityassets/WorldStyle.java:160`), and an active `cityStyleThreshold` with an empty or unknown `cityStyleAlternative` names no style; the registry lookup or a later dereference fails. | crash |
| PROF-13 | Unknown `worldStyle` crashes dimension setup | `worldgen/DefaultDimensionInfo.java:59` | A profile naming a missing world style makes the asset constructor fail on a `null` record (wrapped as `Error getting resource …`); profile loading does not check the name and there is no fallback. | crash |
| PROF-14 | `seaLevel` override ignored by explosion damage | `worldgen/lost/DamageArea.java:83` | Damage uses the generator's sea level, not the profile's `seaLevel`, so in `atlantis` (89) blocks destroyed between Y 63 and 89 become air instead of water. | worldgen |
| PROF-15 | `landscapeType` allowed list is incomplete | `config/LostCityProfile.java:366` | The declared values omit `spheres` and `cavernspheres`, which built-ins use; the GUI's range check rejects them and its tooltip omits them. | cosmetic |
| PROF-16 | `cityStyleThreshold` default lies outside its range | `config/LostCityProfile.java:603` | Default -1 (disabled) but declared range 0..1, so the GUI cannot set the disabled value back after an edit. | cosmetic |
| PROF-17 | Dead options are exposed | `config/LostCityProfile.java:304` | `horizon`, `fogRed`, `fogGreen`, `fogBlue`, `fogDensity` (reader commented out in `setup/ClientEventHandlers.java`), `sphereSurfaceVariation`, `outsideSurfaceVariation` (`:318-319`) and `outsideGroundLevel` (`:323`) have no reader, yet appear in files and the GUI and are set by built-ins. | cosmetic |
| PROF-18 | Profile texts are wrong or stale | `config/ProfileSetup.java:429` | `largecities` sets no description and shows the default's text; `nodamage`, `floating` and `space` extra descriptions promise disabled ravines, mineshafts, strongholds and villages that no option controls. | cosmetic |
| PROF-19 | Four-argument `setCityLevelHeights` gives uneven levels | `config/LostCityProfile.java:713` | Levels 4–7 become `l3 + 30`, `+40`, `+50`, `+60`, a 30-block jump after 8-block steps everywhere else. | gameplay |
| PROF-20 | Lost City dimension cuts the overworld noise | `data/lostcities/dimension_type/lostcity.json` | Y 0..256 type with `minecraft:overworld` noise settings built for -64..320; generation is clamped, so peaks above 255 and everything below 0 are lost (*inferred*). | visual |
| PROF-21 | Unsynchronized static profile cache | `setup/Config.java:76` | `dimensionProfileCache` is a lazily built `HashMap`, read from worldgen worker threads, reset on server events and mutated by `registerLostCityDimension`, with no synchronization. | crash |
| PROF-22 | Profile name taken up to the first dot | `config/ProfileSetup.java:485` | `my.city.json` registers as `my`; two files that map to the same name resolve in `listFiles` order. | gameplay |
| PROF-23 | API dimension registrations are lost | `setup/Config.java:68` | `registerLostCityDimension` writes only into the cache, which `resetProfileCache` clears at every server start; building the cache this early also reads the server config before it is loaded (*inferred*). | gameplay |
| PROF-24 | API `setOverworldProfile` touches a client class | `LostCitiesImp.java:44` | Common API code calls `GuiLCConfig.selectProfile`, a `Screen` subclass; on a dedicated server the call fails with `NoClassDefFoundError` (*inferred*). | crash |

<!-- Merged and deduplicated into BUGS.md. Suspected, not confirmed; no fixes designed here. -->
