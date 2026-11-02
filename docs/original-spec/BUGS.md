# Suspected bugs in upstream Lost Cities

<!-- Bugs in UPSTREAM Lost Cities 1.20 at f18a094d, listed so this backport does not reproduce them.
     Suspected, not confirmed at runtime; no fixes designed here. Merged from every area index. -->

234 raw findings merge into **203 unique bugs**: 45 crash, 73 worldgen, 24 gameplay, 15 performance, 16 visual, 30 cosmetic. One further finding was checked and is not a bug; it is listed under [Not bugs](#not-bugs) with the evidence, and its ID stays reserved.

The ID is the first area's ID (area order `01-core` to `09-client-ui`); **Also** lists the same bug as reported by other areas. Every area ID stays a valid reference: each area's own table in its `index.md` holds the full description. Areas that report the same bug give it the same severity. Where one row merges several narrower area findings (`ASSET-21` covers `SCAT-17` to `SCAT-19`), the row takes the most severe and each area ID keeps its own. Severity is the worst effect when the code path runs; how reachable that path is (dead code, API only, config only) is stated in the area's row, not folded into the severity. Severity is one of crash, worldgen (wrong or broken world output), gameplay, performance, visual or cosmetic. Locations are on `f18a094d`, as the areas give them: Java paths relative to `src/main/java/mcjty/lostcities/`, resource paths relative to `src/main/resources/` unless they start with `src/`, and bare `palettes/`, `citystyles/` and `stuff/` paths under `data/lostcities/lostcities/`.

## Crash

| ID | Also | Area(s) | Title | Location |
|---|---|---|---|---|
| CORE-01 | PROF-24 | 01-core, 02-profiles-dimensions | `setOverworldProfile` loads a client-only class from common code | `LostCitiesImp.java:44` |
| CORE-02 | | 01-core | `getLostInfo` casts any `Level` to `WorldGenLevel` | `LostCitiesImp.java:30` |
| CORE-04 | PROF-05 | 01-core, 02-profiles-dimensions | Unknown `selectedProfile` crashes profile resolution | `setup/Config.java:122` |
| CORE-05 | PROF-21 | 01-core, 02-profiles-dimensions | Profile cache built without synchronization | `setup/Config.java:76` |
| CORE-13 | | 01-core | Duplicate forced-spawn entries crash world creation | `setup/ForgeEventHandlers.java:210` |
| CORE-14 | | 01-core | Failed spawn search aborts world creation | `setup/ForgeEventHandlers.java:317` |
| CORE-18 | | 01-core | Bed-teleport landing search can loop forever | `setup/ForgeEventHandlers.java:561` |
| CORE-21 | CITY-09, LOOT-04 | 01-core, 04-city-layout, 08-loot-mobs-blocks | Weighted choice can return null | `varia/Tools.java:117` (callers `worldgen/lost/City.java:294`, `worldgen/lost/MultiChunk.java:106`) |
| CORE-22 | | 01-core | `TimedCache` computes values while holding the map's bin lock | `varia/TimedCache.java:67` |
| CORE-34 | | 01-core | Edit-mode records written from generation threads into a plain map | `editor/EditModeData.java:53` |
| PROF-02 | | 02-profiles-dimensions | Malformed profile file aborts mod loading | `config/ProfileSetup.java:483` |
| PROF-06 | TERR-03, CITY-04, CITY-14 | 02-profiles-dimensions, 03-terrain, 04-city-layout | Unchecked min/max option pairs crash generation | `worldgen/lost/DamageArea.java:105` (also `:107`, `:116`, `:118`; `worldgen/lost/City.java:225`; `worldgen/lost/BuildingInfo.java:928`; `worldgen/lost/CitySphere.java:263`) |
| PROF-07 | | 02-profiles-dimensions | Missing outside profile crashes sphere worlds | `worldgen/LostCityFeature.java:199` |
| PROF-12 | | 02-profiles-dimensions | Unresolvable city style crashes generation | `worldgen/lost/City.java:301` |
| PROF-13 | | 02-profiles-dimensions | Unknown `worldStyle` crashes dimension setup | `worldgen/DefaultDimensionInfo.java:59` |
| TERR-16 | | 03-terrain | Sphere clearing writes past the build limit | `worldgen/gen/Spheres.java:118` (also `:126`) |
| TERR-29 | | 03-terrain | Ruin column cap compares a count with a Y | `worldgen/LostCityTerrainFeature.java:1160` |
| TERR-30 | | 03-terrain | Ruin leaves can fall forever | `worldgen/LostCityTerrainFeature.java:1181` |
| TERR-31 | | 03-terrain | Degenerate building bottom is not handled | `worldgen/LostCityTerrainFeature.java:2192` |
| TERR-35 | | 03-terrain | Sphere feature has no exception guard | `worldgen/LostCitySphereFeature.java:35` |
| TERR-38 | | 03-terrain | World-generation saved data is saved without the writers' lock | `worldgen/LostCityWorldGenData.java:146` (iterations at `:150`, `:153`) |
| CITY-15 | | 04-city-layout | Height-cap loop has no lower bound | `worldgen/lost/BuildingInfo.java:948` |
| CITY-17 | | 04-city-layout | Every chunk selects floor parts and can throw | `worldgen/lost/BuildingInfo.java:1043` |
| CITY-25 | | 04-city-layout | `MultiChunk` crashes on bad multibuilding settings | `worldgen/lost/MultiChunk.java:138` (also `:79`) |
| CITY-27 | ASSET-16 | 04-city-layout, 05-assets | Rail transform throws for `MIRROR_Z` | `worldgen/lost/Transform.java:79` (throw at `:204`) |
| CITY-32 | | 04-city-layout | Locale-dependent enum parsing | `worldgen/lost/regassets/data/CitySphereSettings.java:26-27` |
| CITY-37 | ASSET-27 | 04-city-layout, 05-assets | API street width unboxes `null` | `worldgen/lost/cityassets/CityStyle.java:185-186` |
| ASSET-01 | | 05-assets | `getOrWarn` throws instead of warning | `worldgen/lost/cityassets/RegistryAssetRegistry.java:76-82` |
| ASSET-02 | | 05-assets | `CompiledPalette.getAll` crashes on random characters | `worldgen/lost/cityassets/CompiledPalette.java:161` |
| ASSET-03 | | 05-assets | Palette with 1.12 `@meta` block syntax | `src/main/resources/data/lostcities/lostcities/palettes/bricks_desert_redsand.json` |
| ASSET-08 | | 05-assets | Style palette pick can merge null | `worldgen/lost/cityassets/Style.java:52-60` |
| ASSET-14 | | 05-assets | Palette compilation needs a running server | `worldgen/lost/cityassets/Palette.java:84-86`; `varia/Tools.java:65` |
| ASSET-21 | SCAT-17, SCAT-18, SCAT-19 | 05-assets, 07-scattered | Stuff ranges unchecked; unset `inbuilding` disables | `worldgen/gen/Stuff.java:32,74,78`; `worldgen/lost/regassets/StuffSettingsRE.java` |
| ASSET-23 | | 05-assets | Single-character fields accept bad strings | `worldgen/lost/cityassets/Palette.java:67`; `worldgen/lost/regassets/BuildingRE.java:58,116`; `worldgen/lost/regassets/data/DataTools.java:19`; `worldgen/lost/cityassets/BuildingPart.java:58` (meta `char`) |
| ASSET-24 | | 05-assets | Part slices not validated | `worldgen/lost/regassets/BuildingPartRE.java:42-58` |
| ASSET-25 | | 05-assets | Sphere enums parsed with `valueOf` | `worldgen/lost/regassets/data/CitySphereSettings.java:26-27` |
| ASSET-29 | LOOT-03 | 05-assets, 08-loot-mobs-blocks | Conditions that match nothing crash | `worldgen/LostCityTerrainFeature.java:2030-2034,2071-2077` |
| ASSET-31 | | 05-assets | Multibuilding size not checked against its grid | `worldgen/lost/cityassets/MultiBuilding.java:20-24,37` |
| ASSET-32 | | 05-assets | Empty part lists crash | `worldgen/LostCityTerrainFeature.java:522-528` |
| HWY-04 | | 06-highways-railways | Legacy extent walk can loop forever | `worldgen/lost/Highway.java:121`, `:128` |
| HWY-12 | | 06-highways-railways | Missing palette entries crash generation | `worldgen/gen/Highways.java:110`, `worldgen/gen/Railways.java:150` |
| SCAT-06 | | 07-scattered | Zero total weight crashes | `worldgen/gen/Scattered.java:356` |
| SCAT-08 | | 07-scattered | `repeatslice` fill has no lower bound | `worldgen/gen/Scattered.java:451` |
| UI-01 | | 09-client-ui | Customize crashes when the profile list was never built | `gui/LostCitySetup.java:95` |
| UI-06 | | 09-client-ui | Preview biome lookup dereferences a null registry | `gui/NullDimensionInfo.java:292` |

## Worldgen

| ID | Also | Area(s) | Title | Location |
|---|---|---|---|---|
| CORE-07 | SCAT-04 | 01-core, 07-scattered | Default `avoidStructures` names `minecraft:swamp_huts` | `setup/Config.java:45` |
| CORE-10 | | 01-core | Layout-changing keys live in the per-installation COMMON config | `setup/Config.java:162` |
| CORE-20 | ASSET-13 | 01-core, 05-assets | Unknown block names silently become air | `varia/Tools.java:73` |
| CORE-23 | | 01-core | `checkStructures` position math is wrong | `varia/WorldTools.java:65` |
| CORE-25 | HWY-03 | 01-core, 06-highways-railways | Octaves of `PerlinNoiseGenerator14` are identical | `varia/PerlinNoiseGenerator14.java:16` |
| CORE-32 | | 01-core | `createbuilding` rewrites the shared layout cache | `commands/CommandCreateBuilding.java:71` |
| PROF-04 | | 02-profiles-dimensions | Declared option ranges are not enforced on load | `config/Configuration.java:158` |
| PROF-09 | | 02-profiles-dimensions | Existing worlds follow live profile definitions | `setup/Config.java:109` |
| PROF-10 | CITY-03 | 02-profiles-dimensions, 04-city-layout | City style rolled per chunk instead of per city | `worldgen/lost/City.java:282` |
| PROF-11 | CITY-01 | 02-profiles-dimensions, 04-city-layout | City centers and center styles ignore the world seed | `worldgen/lost/City.java:187` (also `:215`, `:243`) |
| PROF-14 | | 02-profiles-dimensions | `seaLevel` override ignored by explosion damage | `worldgen/lost/DamageArea.java:83` |
| TERR-01 | | 03-terrain | Sphere radius factor applies to the random part only | `worldgen/lost/CitySphere.java:263` |
| TERR-02 | | 03-terrain | City sphere cache depends on query order | `worldgen/lost/CitySphere.java:483` |
| TERR-04 | | 03-terrain | Debris from tall buildings starts at the bottom of the world | `worldgen/LostCityTerrainFeature.java:2113` |
| TERR-05 | | 03-terrain | Debris landing height is not reset per block | `worldgen/LostCityTerrainFeature.java:2127` |
| TERR-06 | | 03-terrain | Water-mode clearing skips the water-level row | `worldgen/LostCityTerrainFeature.java:533` (also `:543`) |
| TERR-07 | CITY-12, CITY-13 | 03-terrain, 04-city-layout | Street type always rolls `NORMAL` and is re-rolled into the cached plan at generation time | `worldgen/LostCityTerrainFeature.java:1234` (same expression at `worldgen/lost/BuildingInfo.java:897`) |
| TERR-09 | | 03-terrain | Front parts are written with the neighbor's `BuildingInfo` | `worldgen/LostCityTerrainFeature.java:1449` (effects at `:1903`, `:1928`, `:400`) |
| TERR-10 | | 03-terrain | Terrain correction mutates cached heightmaps | `worldgen/LostCityTerrainFeature.java:675` (and `:878`–`882`) |
| TERR-11 | | 03-terrain | Building vines depend on neighbor status | `worldgen/ChunkFixer.java:41` (also `:56`, `:72`, `:87`) |
| TERR-14 | | 03-terrain | Driver cache aliases positions outside the chunk | `worldgen/ChunkDriver.java:420` (also `:458`) |
| TERR-17 | | 03-terrain | Clear-below-until-air never stops below sea level | `worldgen/gen/Spheres.java:146` |
| TERR-18 | | 03-terrain | Explosions flood enclosed air below the water level | `worldgen/lost/DamageArea.java:88` (and `worldgen/LostCityTerrainFeature.java:484`) |
| TERR-19 | | 03-terrain | Damage pass covers Y 0–255 only | `worldgen/LostCityTerrainFeature.java:455` (also `worldgen/lost/DamageArea.java:39`) |
| TERR-20 | | 03-terrain | Deferred tree growth is dropped or cascades | `worldgen/LostCityTerrainFeature.java:1974` (queue at `worldgen/GlobalTodo.java:57`) |
| TERR-22 | | 03-terrain | Cavern heightmap rule no longer fits one sample | `worldgen/ChunkHeightmap.java:51` |
| TERR-24 | CITY-22 | 03-terrain, 04-city-layout | Hard-coded Y bounds on a −64 to 320 world | `worldgen/LostCityTerrainFeature.java:728` (also `:781`, `:1466`, `:1484`, `:1502`, `:1520`, `:2127`, `:1341`, `:1436`, `:271`; `worldgen/lost/BuildingInfo.java:2009`, `:2069`) |
| TERR-25 | | 03-terrain | Generation errors are swallowed after partial writes | `worldgen/LostCityFeature.java:63` |
| TERR-34 | | 03-terrain | `optimizedHeightmap` changes the world | `worldgen/HeightGenOpt.java:13` (switch at `worldgen/LostCityTerrainFeature.java:930`) |
| TERR-36 | | 03-terrain | Predicate range writes skip unstaged blocks | `worldgen/ChunkDriver.java:397` |
| CITY-02 | | 04-city-layout | Multibuilding layout ignores world seed and dimension | `worldgen/lost/MultiChunk.java:73` |
| CITY-05 | | 04-city-layout | Rarity map cached with the first caller's parameters | `worldgen/lost/City.java:50` |
| CITY-06 | | 04-city-layout | City style cache ignores the profile | `worldgen/lost/City.java:249` |
| CITY-07 | | 04-city-layout | Predefined multibuilding city forcing assumes 2 × 2 | `worldgen/lost/City.java:317` |
| CITY-08 | | 04-city-layout | Overlapping predefined entries resolve by map order | `worldgen/lost/City.java:112` (also `:145`, `:165`) |
| CITY-10 | | 04-city-layout | Building-chance roll reused as the first prefers-lonely roll | `worldgen/lost/BuildingInfo.java:805` |
| CITY-11 | | 04-city-layout | Prefers-lonely reads neighbors' candidate buildings | `worldgen/lost/BuildingInfo.java:805-811` |
| CITY-16 | | 04-city-layout | Cellar count piles up at the maximum | `worldgen/lost/BuildingInfo.java:955` |
| CITY-18 | | 04-city-layout | `getBuildingBottomHeight` mutates floors and cellars | `worldgen/lost/BuildingInfo.java:1919` (also `:1926`) |
| CITY-19 | HWY-07 | 04-city-layout, 06-highways-railways | `BuildingInfo` deletes railway chunks from a shared cache | `worldgen/lost/BuildingInfo.java:1012` |
| CITY-20 | | 04-city-layout | Provisional plans are visible to neighbors | `worldgen/plan/ChunkPlan.java:47` (also `:67`) |
| CITY-21 | | 04-city-layout | Street-majority tie broken by `HashMap` order | `worldgen/lost/BuildingInfo.java:431` |
| CITY-26 | | 04-city-layout | `MultiChunk` checks positions against the wrong level and profile | `worldgen/lost/MultiChunk.java:86` (also `:92`, `:140`) |
| CITY-34 | | 04-city-layout | Averaged-heightmap sample grid uses truncating division | `worldgen/lost/BuildingInfo.java:1268` |
| CITY-35 | | 04-city-layout | Multibuilding doorway guard covers only the last column and row | `worldgen/lost/BuildingInfo.java:1837` (also `:1874`) |
| ASSET-04 | LOOT-02 | 05-assets, 08-loot-mobs-blocks | `belowpart` tests the current part | `worldgen/lost/cityassets/ConditionContext.java:85`; `worldgen/lost/BuildingInfo.java:758,1050` |
| ASSET-06 | | 05-assets | City-style inheritance appends selector lists | `worldgen/lost/cityassets/CityStyle.java:362-371`; `citystyles/citystyle_border.json` |
| ASSET-07 | | 05-assets | Inheritance skips `generalblocks` and park `grass` | `worldgen/lost/cityassets/CityStyle.java:357-471` |
| ASSET-09 | | 05-assets | Selector distances overflow `int` | `worldgen/lost/cityassets/CityStyle.java:482-485` |
| ASSET-10 | | 05-assets | Zero-weight entries can still be picked | `varia/Tools.java:100-117` |
| ASSET-12 | | 05-assets | Information survives a plain redefinition | `worldgen/lost/cityassets/CompiledPalette.java:98-110` |
| ASSET-15 | | 05-assets | Stuff index filled only after the first tick | `worldgen/lost/cityassets/AssetRegistries.java:63-70`; `setup/ForgeEventHandlers.java:103`; `worldgen/gen/Stuff.java:27` |
| ASSET-28 | | 05-assets | Failed inheritance leaves a half-built style | `worldgen/lost/cityassets/CityStyle.java:353-357,472` |
| ASSET-33 | | 05-assets | Name forms are inconsistent | `worldgen/lost/cityassets/ConditionContext.java:85-97`; `worldgen/lost/regassets/data/ResourceLocationMatcher.java:19-24` |
| HWY-01 | | 06-highways-railways | Station "city to the west" test checks the east spot | `worldgen/lost/Railway.java:130` |
| HWY-02 | | 06-highways-railways | Z highways test the east neighbor twice | `worldgen/gen/Highways.java:37`, `:39`, `:46` |
| HWY-06 | | 06-highways-railways | Intercity mode ignores `highwayDistanceMask = 0` | `worldgen/lost/Highway.java:65-74` |
| HWY-08 | | 06-highways-railways | Rail cache ignores the asking profile | `worldgen/lost/Railway.java:334-353` |
| HWY-11 | | 06-highways-railways | Hub persistence signature misses inputs | `worldgen/LostCityHighwayData.java:135-162` |
| SCAT-01 | | 07-scattered | Single-chunk avoidance window is shifted by (+1, +1) | `worldgen/lost/StructureAvoidance.java:84-87` |
| SCAT-02 | | 07-scattered | Multibuilding footprint decision fails open and is cached for good | `worldgen/lost/StructureAvoidance.java:92` (resolver `:140-157`) |
| SCAT-05 | | 07-scattered | Scattered weighted pick is off by one | `worldgen/gen/Scattered.java:360` |
| SCAT-07 | | 07-scattered | Selection filters by the area origin's biome | `worldgen/gen/Scattered.java:132` (also `:346`) |
| SCAT-09 | | 07-scattered | Single-chunk terrain-height modes are identical | `worldgen/gen/Scattered.java:471-474` |
| SCAT-10 | | 07-scattered | Scattered floors never match top-of-building conditions | `worldgen/gen/Scattered.java:398-401` |
| SCAT-11 | | 07-scattered | Part2 condition context lacks the chosen part | `worldgen/gen/Scattered.java:424` |
| SCAT-12 | | 07-scattered | Terrain stats mutate a shared heightmap | `worldgen/gen/Scattered.java:294` |
| SCAT-13 | | 07-scattered | Part2 is placed one part too high | `worldgen/gen/Scattered.java:463-465` |
| SCAT-20 | | 07-scattered | Stuff building filter uses the candidate building in street chunks | `worldgen/gen/Stuff.java:34` |
| SCAT-22 | | 07-scattered | Stuff order follows hash-map iteration | `worldgen/lost/cityassets/AssetRegistries.java:65-69` |
| SCAT-23 | | 07-scattered | Stuff `seesky` reads live world state | `worldgen/gen/Stuff.java:83` |
| LOOT-05 | | 08-loot-mobs-blocks | Loot draws depend on vines and `HashMap` order | `worldgen/ChunkFixer.java:189` |
| LOOT-07 | | 08-loot-mobs-blocks | Shipped `easybreakable` tag drifts from its generator | `datagen/LCBlockTags.java:246` |

## Gameplay

| ID | Also | Area(s) | Title | Location |
|---|---|---|---|---|
| CORE-03 | PROF-01 | 01-core, 02-profiles-dimensions | Profile-setup callbacks never run | `LostCities.java:52` |
| CORE-06 | PROF-23 | 01-core, 02-profiles-dimensions | API dimension registrations discarded on every cache reset | `setup/Config.java:63` |
| CORE-09 | SCAT-16 | 01-core, 07-scattered | Avoided-structure set never refreshed; a bad entry throws in generation | `setup/Config.java:141` |
| CORE-11 | | 01-core | Login handler reverts later spawn changes | `setup/ForgeEventHandlers.java:84` |
| CORE-12 | | 01-core | Stored spawn positions survive into the next world | `setup/ForgeEventHandlers.java:61` |
| CORE-16 | | 01-core | Spawn column scan is bottom-up with a fixed top | `setup/ForgeEventHandlers.java:407` |
| CORE-17 | | 01-core | Custom spawn skips the bonus chest | `setup/ForgeEventHandlers.java:248` |
| CORE-19 | | 01-core | Bed-teleport cobblestone and landing spot disagree | `setup/ForgeEventHandlers.java:565` |
| CORE-27 | | 01-core | `saveprofile` lets any player write files | `commands/CommandSaveProfile.java:29` |
| CORE-28 | | 01-core | `exportpart` writes any file name in the server directory | `commands/CommandExportPart.java:159` |
| CORE-30 | | 01-core | `locatepart` never finds bundled parts | `commands/CommandLocatePart.java:62` |
| CORE-33 | | 01-core | Vanilla `/resetchunks` registered in production | `commands/ModCommands.java:40` |
| CORE-35 | | 01-core | Edit sessions never expire | `editor/EditorInfo.java:19` |
| CORE-39 | | 01-core | Bed check parses `specialBedBlock` on every sleep | `setup/ForgeEventHandlers.java:440` |
| PROF-03 | | 02-profiles-dimensions | One unreadable profile stops reading the rest | `config/ProfileSetup.java:490` |
| PROF-08 | | 02-profiles-dimensions | `generateNether` has no effect | `setup/Config.java:122` |
| PROF-19 | | 02-profiles-dimensions | Four-argument `setCityLevelHeights` gives uneven levels | `config/LostCityProfile.java:713` |
| PROF-22 | | 02-profiles-dimensions | Profile name taken up to the first dot | `config/ProfileSetup.java:485` |
| TERR-21 | | 03-terrain | Global job queues outlive the world | `worldgen/GlobalTodo.java:33` |
| ASSET-05 | LOOT-01 | 05-assets, 08-loot-mobs-blocks | Loot and spawner floors use truncating division | `worldgen/LostCityTerrainFeature.java:2015-2016,2057-2058` |
| LOOT-06 | | 08-loot-mobs-blocks | Dispenser loot table used for chests | `data/lostcities/lostcities/conditions/chestloot.json:45` |
| LOOT-10 | | 08-loot-mobs-blocks | Multibuildings ignore `buildingWithoutLootChance` | `worldgen/lost/BuildingInfo.java:995` |
| UI-02 | | 09-client-ui | Customize duplicates list entries and replaces the global profile at once | `gui/LostCitySetup.java:94` |
| UI-07 | | 09-client-ui | Client cleanup hooked to a server-side event | `setup/ClientEventHandlers.java:75` |

## Performance

| ID | Also | Area(s) | Title | Location |
|---|---|---|---|---|
| CORE-15 | | 01-core | Spawn search generates up to 20000 chunks synchronously | `setup/ForgeEventHandlers.java:306` |
| CORE-26 | | 01-core | `debug` and `map` are open to every player | `commands/CommandDebug.java:33` |
| TERR-15 | | 03-terrain | Every read is written back at commit | `worldgen/ChunkDriver.java:78` |
| TERR-26 | | 03-terrain | Border vine scan is heavy and repeated | `worldgen/ChunkFixer.java:154` (second run from `worldgen/gen/Spheres.java:57`) |
| CITY-23 | | 04-city-layout | Legacy bridge walks recurse across rows | `worldgen/lost/BuildingInfo.java:1541` |
| ASSET-26 | | 05-assets | Part palettes recompiled on every placement | `worldgen/LostCityTerrainFeature.java:1881-1889` |
| HWY-05 | | 06-highways-railways | Rejected legacy highways are never cached | `worldgen/lost/Highway.java:133-159` |
| HWY-10 | | 06-highways-railways | "LRU" caches evict arbitrary entries | `worldgen/highway/IntercityHighwayPlanner.java:17`, `:621-642`; `worldgen/highway/BoundedFutureCache.java:64-85` |
| HWY-14 | | 06-highways-railways | One thread pool per dimension info | `worldgen/highway/HighwayPlanningService.java:31` |
| HWY-15 | | 06-highways-railways | Unbounded static caches | `worldgen/lost/Highway.java:19-21`, `worldgen/lost/Railway.java:94` |
| SCAT-03 | | 07-scattered | Footprint decision cache is unbounded | `worldgen/lost/StructureAvoidance.java:36` |
| SCAT-14 | | 07-scattered | Plan cache is unbounded | `worldgen/gen/Scattered.java:37` |
| SCAT-15 | | 07-scattered | Near-highway placement search is costly | `worldgen/gen/Scattered.java:221-237` |
| UI-03 | | 09-client-ui | World-style cycling leaks resource managers and misses world packs | `gui/LostCitySetup.java:140` |
| UI-04 | | 09-client-ui | Preview rebuilds a whole dimension every frame | `gui/GuiLCConfig.java:469` |

## Visual

| ID | Also | Area(s) | Title | Location |
|---|---|---|---|---|
| PROF-20 | | 02-profiles-dimensions | Lost City dimension cuts the overworld noise | `data/lostcities/dimension_type/lostcity.json` |
| TERR-12 | | 03-terrain | Cross-chunk shape updates are dropped | `worldgen/ChunkDriver.java:180` |
| TERR-13 | | 03-terrain | Heightmaps can only rise after the commit | `worldgen/ChunkDriver.java:52` |
| TERR-23 | | 03-terrain | Lowering under water leaves air under the water | `worldgen/LostCityTerrainFeature.java:751` (clear at `:774`) |
| TERR-28 | | 03-terrain | No cleanup of floating fragments after explosions | `worldgen/LostCityTerrainFeature.java:356` |
| CITY-28 | ASSET-17 | 04-city-layout, 05-assets | Mirror transforms are inconsistent | `worldgen/lost/Transform.java:10-12` |
| CITY-30 | | 04-city-layout | Z bridges are transposed but not block-rotated | `worldgen/gen/Bridges.java:49` |
| CITY-36 | | 04-city-layout | West/north passage ignores the neighbor's `dontconnect` | `worldgen/gen/Doors.java:33` |
| CITY-38 | | 04-city-layout | Bridges skip only level-0 highways | `worldgen/gen/Bridges.java:20` |
| ASSET-11 | | 05-assets | `frompalette` resolved once, against the base layer | `worldgen/lost/cityassets/CompiledPalette.java:81-96` |
| ASSET-18 | LOOT-09 | 05-assets, 08-loot-mobs-blocks | Directional blocks outside the rotatable tag do not turn | `worldgen/LostCityTerrainFeature.java:1993-2009` |
| ASSET-20 | | 05-assets | Desert brick palettes lack rubble | `palettes/bricks_desert.json`, `bricks_desert_red.json`, `bricks_desert_orange.json`, `default_desert.json` |
| ASSET-30 | | 05-assets | Damaged blocks keyed by block state | `worldgen/lost/cityassets/Palette.java:80,90,106`; `worldgen/lost/cityassets/CompiledPalette.java:100-103` |
| HWY-09 | | 06-highways-railways | Overlapping routes collapse to the lowest level | `worldgen/highway/IntercityHighwayPlanner.java:455`, `:458` |
| LOOT-08 | | 08-loot-mobs-blocks | `lights` tag misses conditional and modded light sources | `datagen/LCBlockTags.java:247` |
| UI-05 | | 09-client-ui | Elements accumulate on every resize | `gui/GuiLCConfig.java:116` |

## Cosmetic

| ID | Also | Area(s) | Title | Location |
|---|---|---|---|---|
| CORE-08 | | 01-core | `heightSampleSize` comment states the wrong default | `setup/Config.java:166` |
| CORE-24 | | 01-core | `/lostcities stats` average is wrong | `varia/Statistics.java:34` |
| CORE-29 | | 01-core | `exportpart` output can be corrupt or unstable | `commands/CommandExportPart.java:115` |
| CORE-31 | | 01-core | `locatepart` result limit not enforced | `commands/CommandLocatePart.java:66` |
| CORE-36 | | 01-core | Inert network channel accepts any version | `network/PacketHandler.java:27` |
| CORE-37 | | 01-core | Unused player capability | `setup/ForgeEventHandlers.java:72` |
| CORE-38 | | 01-core | API leaks internals and has dead or misspelled members | `api/LostChunkCharacteristics.java:4` |
| PROF-15 | | 02-profiles-dimensions | `landscapeType` allowed list is incomplete | `config/LostCityProfile.java:366` |
| PROF-16 | | 02-profiles-dimensions | `cityStyleThreshold` default lies outside its range | `config/LostCityProfile.java:603` |
| PROF-17 | | 02-profiles-dimensions | Dead options are exposed | `config/LostCityProfile.java:304` |
| PROF-18 | | 02-profiles-dimensions | Profile texts are wrong or stale | `config/ProfileSetup.java:429` |
| TERR-08 | | 03-terrain | Random vegetation is asymmetric | `worldgen/LostCityTerrainFeature.java:1523` (also `:1469`, `:1479`, `:1487`) |
| TERR-27 | | 03-terrain | Some seeds ignore the world seed | `worldgen/LostCityTerrainFeature.java:352` (also `:552`–`568`, `:1233`) |
| TERR-32 | | 03-terrain | Vines never use the last column of a wall | `worldgen/ChunkFixer.java:45` (also `:60`, `:76`, `:91`) |
| TERR-33 | | 03-terrain | Error chat throttle and logging | `worldgen/ErrorLogger.java:21` (and `worldgen/LostCityFeature.java:64`) |
| TERR-37 | | 03-terrain | Rubble height interpolation is reversed | `worldgen/LostCityTerrainFeature.java:1113` |
| CITY-24 | | 04-city-layout | Dead avoidance clause | `worldgen/lost/BuildingInfo.java:389` |
| CITY-29 | | 04-city-layout | `randomRotation` uses `Math.random` | `worldgen/lost/Transform.java:25` |
| CITY-31 | ASSET-19 | 04-city-layout, 05-assets | Codec encoders write wrong values | `worldgen/lost/regassets/data/CitySphereSettings.java:14`; `worldgen/lost/regassets/data/ObjectSelector.java:16-18` |
| CITY-33 | | 04-city-layout | GUI preview characteristics differ from the world | `worldgen/lost/BuildingInfo.java:349-365` |
| ASSET-22 | | 05-assets | Dead fields and assets | `regassets/data/StreetSettings.java` (`width`, `streetvariant`); `CompiledPalette.isSimple`; `ConditionContext.parseTest(JsonElement)`; `Transform.randomRotation`; `stuff/example.json`; `palettes/glass_side_variant_street.json` |
| SCAT-21 | | 07-scattered | Stuff random ignores the world seed | `worldgen/gen/Stuff.java:23` |
| UI-08 | | 09-client-ui | Damage preview uses a fixed ground and can draw outside the box | `gui/GuiLCConfig.java:425` |
| UI-09 | | 09-client-ui | Config-button image relies on texture wrapping | `setup/ClientEventHandlers.java:56` |
| UI-10 | | 09-client-ui | Profile icons are dead assets with a wrong documented size | `src/main/resources/assets/lostcities/textures/gui/` |
| UI-11 | | 09-client-ui | No localization; lang files are obsolete | `src/main/resources/assets/lostcities/lang/en_us.json:2` |
| UI-12 | | 09-client-ui | Stale custom JSON survives a switch to a standard profile | `gui/GuiLCConfig.java:552` |
| UI-13 | | 09-client-ui | Boolean fields skip validation | `gui/elements/BooleanElement.java:28` |
| UI-14 | | 09-client-ui | Rejected numbers stay on screen | `gui/elements/IntElement.java:31` |
| UI-15 | | 09-client-ui | Refreshing fields writes values back into shared profiles | `gui/elements/IntElement.java:74` |

## Not bugs

Findings filed as suspected and then checked against the code or assets. They need no regression test; the area row says why.

| ID | Area | Title | Location | Why it is not a bug |
|---|---|---|---|---|
| HWY-13 | 06-highways-railways | Rail dungeon tunnels differ north vs south | `worldgen/gen/Railways.java:156-179` | `rails_horizontal`'s open interior spans rows z 4 to 10 of 16 (`parts/rails_horizontal.json`), so the 4-deep north tunnel (rows 0 to 3) and the 5-deep south tunnel (rows 15 to 11) each end where the interior starts |

<!-- In this mod: a regression test per reproducible bug, named doesNotReproduce<ID>_<what>,
     and the change's "Inherited bugs avoided" list names the IDs. Ported files list them on their Avoids: line. -->
