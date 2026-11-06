# Configuration

Lost Cities: New Horizons reads one file, `config/lostcitiesnh.cfg`, through GTNHLib's annotation config. Single player's integrated server reads the same file. In game, the **Config** button in the Mods list opens GTNHLib's automatic config screen for it; the mod has no config screen of its own ([09-client-ui](new-spec/09-client-ui/index.md), D31).

## `worldgen`: module flags

One switch per feature area. A switch turns that area's generation off; it never adds or removes blocks or items, because the mod registers none, so a client and a server with different settings can always join each other.

These keys apply to **new worlds only**. A world copies them into its pinned profile when it is created ([0005](design/0005-rwg-overlay.md) §7), and editing them later does not change a world that already exists. The config screen marks the category as needing a world restart.

| Key | Default | What it does |
|---|---|---|
| `worldgen.cities` | `true` | Generate cities: buildings, streets, bridges and their parks |
| `worldgen.highways` | `true` | Generate highways between and through cities |
| `worldgen.railways` | `true` | Generate railways, stations and subways |
| `worldgen.scatteredStructures` | `true` | Generate scattered structures outside cities |
| `worldgen.paletteVariety` | `true` | Draw building walls, glass, interior floors, ceilings, lights, furniture and park flora from the pack's decorative blocks ([palette variety](new-spec/05-assets/asset-format.md#palette-variety)). Off, cities use upstream Lost Cities' blocks only |

In the file, the keys sit in the `worldgen` category:

```
worldgen {
    B:cities=true
    B:highways=true
    B:railways=true
    B:scatteredStructures=true
    B:paletteVariety=true
}
```

## `profiles`: where cities generate

These keys too apply to **new worlds only**: the profile is resolved and pinned into the world when the world is created.

| Key | Default | What it does |
|---|---|---|
| `profiles.dimensionProfiles` | `0=default` | Which dimensions get cities and with which profile, one `<dimension id>=<profile>` per entry; an entry with no profile means `default`. The profile is pinned into the world's `data/lostcitiesnh.dat` when the world is created ([0005](design/0005-rwg-overlay.md) §7). Only world types with a height oracle get cities: `RWG2_CONTINENTS`, `RWG2`, `RWG` and `flat` |
| `profiles.enableOnExistingWorlds` | `false` | Give cities to worlds created before the mod was added, at their next load. Chunks generated before will not match the new ones at the border. `/lostcities profile pin <profile> confirm` opts in one world instead |

```
profiles {
    S:dimensionProfiles <
        0=default
     >
    B:enableOnExistingWorlds=false
}
```

### Choosing a profile

There is no world-preset screen. A world gets the profile `dimensionProfiles` names when it is created, and keeps it:

- **Dedicated server:** set `dimensionProfiles` in `config/lostcitiesnh.cfg`, for example `0=rarecities`, before the world is created (before the first start, or after deleting the world).
- **Single player:** in the main menu, open **Mods**, select Lost Cities: New Horizons, press **Config**, open **Profiles** and edit **Dimension profiles**; the next world you create pins that profile. Worlds that already exist keep theirs.
- **An existing world:** `/lostcities profile pin <profile> confirm` pins a profile into the dimension you stand in. Chunks generated before do not change, so new chunks will not match them at the border.

At start the server logs one line per entry: the profile each dimension's new worlds will pin, or why an entry gives no cities (`[profiles] dimension 0 names profile 'nosuch', which does not exist: new worlds get no cities there`). `/lostcities profile` shows what the dimension you stand in was pinned with.

Every profile value, the module flags, the world type, the loaded asset set (its hash and every file's), the `decorativeMetals` flags and VillageNames' structure settings are pinned into the world when it is created; a later start with a different asset set logs one warning naming the changed files, and the world carries on with the loaded assets (P05-3); a world pinned by an older version reads any option it lacks at its default. `/lostcities profile` shows what a dimension was pinned with, and `/lostcities timers` prints the overlay's generation timers and budgets ([0005](design/0005-rwg-overlay.md) §8).

`ConfigDocsTest` keeps this table in step with the code: every option has a row here with its default.

## `decorativeMetals`: metal accents

One switch per decorative metal at or below EV (decision log D29). With a switch on, that metal's blocks, from every mod, may appear in buildings as small accents under a per-building cap: stripe bands, trims, crates and the odd panel, never whole walls, floors or roofs. With it off, each of those accents uses another block instead. Treasure and chest loot are loot and ignore the switches. Two places keep upstream's look while their metal is on: the cabin's copper walls and stairs, and the highway gas station's and restaurant parking's iron canopy columns, pumps and lamp posts.

These keys apply to **new worlds only**, like the `worldgen` keys: a world pins them when it is created. The config screen marks the category as needing a world restart.

| Key | Default | What it does |
|---|---|---|
| `decorativeMetals.iron` | `true` | Iron accents, and the gas station's iron canopy and pumps |
| `decorativeMetals.gold` | `true` | Gold accents |
| `decorativeMetals.castIron` | `true` | Cast iron accents |
| `decorativeMetals.copper` | `true` | Copper accents, and the cabin's copper walls and stairs |
| `decorativeMetals.tin` | `true` | Tin accents |
| `decorativeMetals.bronze` | `true` | Bronze accents |
| `decorativeMetals.lead` | `true` | Lead accents |
| `decorativeMetals.silver` | `true` | Silver accents |
| `decorativeMetals.nickel` | `true` | Nickel accents |
| `decorativeMetals.invar` | `true` | Invar accents |
| `decorativeMetals.electrum` | `true` | Electrum accents |
| `decorativeMetals.steel` | `true` | Steel accents |
| `decorativeMetals.blackSteel` | `true` | Black steel accents |
| `decorativeMetals.damascusSteel` | `true` | Damascus steel accents |
| `decorativeMetals.meteoricIron` | `true` | Meteoric iron accents |
| `decorativeMetals.electricalSteel` | `true` | Electrical steel accents |
| `decorativeMetals.darkSteel` | `true` | Dark steel accents |
| `decorativeMetals.aluminum` | `true` | Aluminum accents |
| `decorativeMetals.aluminumBrass` | `true` | Aluminum brass accents |
| `decorativeMetals.cobalt` | `true` | Cobalt accents |
| `decorativeMetals.stainlessSteel` | `true` | Stainless steel accents |
| `decorativeMetals.titanium` | `true` | Titanium accents |
| `decorativeMetals.desh` | `true` | Desh accents |
| `decorativeMetals.thaumium` | `true` | Thaumium accents |

## Profiles

A profile is the set of options a world generates with: how common and how large cities are, how tall their buildings, how much damage, where players spawn and how cities meet villages and other landmarks. Profiles use upstream Lost Cities' JSON format ([02-profiles-dimensions](new-spec/02-profiles-dimensions/index.md)).

### Built-in profiles

Ten profiles ship in the jar. Each is the defaults (`default`) with a few options changed. Upstream's `cavern`, `floating`, `space`, `biosphere`, `biosphere_caves`, `bio_wasteland` and `void_outside` need landscapes RWG does not make and are not included (D12).

| Profile | What it is | What it changes from `default` |
|---|---|---|
| `default` | Common cities, explosions | Nothing |
| `nodamage` | No explosion damage, no ruins, no rubble | `explosionChance` 0, `miniExplosionChance` 0, `ruinChance` 0, `rubbleLayer` false |
| `rarecities` | Cities are rare and large | `cityPlacement` grid (64-chunk cells, jitter 16), `cityChance` 0.00025 (about twice the distance between cities), `cityMinRadius` 100, `cityMaxRadius` 256 (about four times the area), `ruinChance` 0, `railwaysCanEnd` true |
| `onlycities` | The world is nearly all city | `cityChance` 0.2, `cityMaxRadius` 256 |
| `tallbuildings` | Very tall buildings, heavier damage | `buildingMinFloors` 4, `buildingMaxFloors` 19, `buildingMinFloorsChance` 8, `buildingMaxFloorsChance` 15, `explosionChance` 0.006, `explosionMinHeight` 130, `explosionMaxHeight` 255, `explosionMaxRadius` 60, `miniExplosionChance` 0.09, `miniExplosionMinRadius` 3, `miniExplosionMaxRadius` 14, `miniExplosionMaxHeight` 255, `debrisToNearbyChunkFactor` 175, `ruinChance` 0.01 |
| `safe` | No spawners, lighting, no loot | `generateSpawners` false, `generateLighting` true, `generateLoot` false |
| `ancient` | Overgrown, mostly ruined jungle city | `ruinChance` 0.9, `ruinMinlevelPercent` 0, `ruinMaxlevelPercent` 0.9, `vineChance` 0.1, `randomLeafBlockChance` 0.05, `randomLeafBlockThickness` 6, `rubbleDirtScale` 2, `rubbleLeaveScale` 2, `explosionChance` 0, `miniExplosionChance` 0 |
| `wasteland` | No water, bare land, half-ruined | `avoidWater` true, `avoidFoliage` true, `ruinChance` 0.5, `ruinMinlevelPercent` 0.5, `ruinMaxlevelPercent` 0.9, `vineChance` 0.003, `randomLeafBlockChance` 0.01, `rubbleDirtScale` 2, `rubbleLeaveScale` 0 |
| `atlantis` | Drowned cities | `seaLevel` 89 (city parts fill with water to Y 89; the RWG sea around stays at 63), `ruinChance` 0.1 |
| `largecities` | Large Perlin-shaped cities with a border style | `cityChance` -1 (Perlin rarity), `cityPerlinScale` 7, `cityPerlinOffset` 0.2, `cityThreshold` 0.1, `cityStyleThreshold` 0.4, `cityStyleAlternative` `citystyle_border`, `buildingChance` 0.4, `buildingMaxFloors` 9, `buildingMaxFloorsChance` 7, `generateLighting` true |

### Your own profiles

Profile files are the `*.json` files in `config/lostcitiesnh/profiles/`, read at start; the folder is created on the first start. A file's name without `.json` is the profile's name (`my.city.json` is `my.city`). To make one, copy a profile and edit the copy:

```
/lostcities saveprofile rarecities sparse
```

writes `config/lostcitiesnh/profiles/sparse.json`, every option with its value. Edit it, run `/lostcities profile reload` (or restart), and name it in `dimensionProfiles` (`0=sparse`) or pin it with `/lostcities profile pin sparse confirm`. A file may leave options out; they take the defaults.

```json
{
  "public": true,
  "lostcity": { "description": "Sparse cities, no ruins", "ruinChance": 0.0 },
  "cities": { "cityChance": 0.003, "cityMaxRadius": 90 }
}
```

How a file is checked when it is read:

- A number outside its range (below) is clamped to it, with a warning in the log.
- A value of the wrong type, an unknown choice, a minimum above its maximum, a `landscapeType` other than `default`, an odd railway spacing, `groundLevel` above 244, `cityStyleThreshold` on with no `cityStyleAlternative`, or a `worldStyle`, `cityStyleAlternative`, `liquidBlock` or `baseBlock` that names nothing loaded rejects the whole file with one log line naming the file, the key and the reason. The other files are still read, and the game starts.
- A file named like a built-in profile is rejected, so a profile name means the same thing on every server.
- Keys this port does not use (upstream's fog, horizon, legacy highway and sphere-surface keys) are read and ignored, so files written for upstream Lost Cities load; `saveprofile` does not write them. Unknown keys are ignored with a warning.

`/lostcities profile list` shows every profile that can be pinned, `/lostcities profile info <profile>` its texts and the options it changes, and `/lostcities profile` the one pinned in your dimension ([commands](new-spec/01-core/commands.md)).

### Spawn options

The pack's spawn is kept unless a profile asks for another. When `spawnBiome`, `spawnCity`, `spawnNotInBuilding`, `forceSpawnInBuilding`, `forceSpawnBuildings` or `forceSpawnParts` is set, a new world's spawn is searched in the city layout, outward from block (0, 0), without generating any chunk; if nothing matches within `spawnCheckAttempts` chunks, the world keeps its own spawn and the log says so. With the building options, each player's first join places them exactly on the building's ground floor; later respawns use the world spawn as usual. If the world asked for a bonus chest, it is placed beside the chosen spawn.

### Every profile option

Status: **live** options change generation (or, for the texts, what commands show) now. **Pinned; read from M07** (or M10) options are checked and pinned into new worlds now, and the scattered structures (M07) or part editor (M10) read them when they arrive. **Deferred** options belong to landscapes not ported yet: they are checked and pinned and change nothing. **Dropped** options are read and ignored. Ranges marked are enforced when a profile is read. `ProfileOptionsDocsTest` keeps this table in step with the code: every option has a row here with its default.

#### `lostcity`

| Key | Default | Range | Status | What it does |
|---|---|---|---|---|
| `description` | `Default generation, common cities, explosions` |  | live | First line of text `/lostcities profile` shows. A built-in's is a lang key |
| `extraDescription` | `""` |  | live | Second line of text |
| `warning` | `""` |  | live | A warning line `/lostcities profile info` shows |
| `worldStyle` | `standard` |  | live | The world style asset this profile uses. A profile naming one that is not loaded is rejected at start |
| `icon` | `""` |  | dropped | Upstream's GUI icon; no GUI here |
| `liquidBlock` | `minecraft:water` |  | live | The block used as liquid in parts and when filling to the water level; a 1.20 name, mapped through the block-name map. A name that resolves to no block rejects the profile |
| `baseBlock` | `minecraft:stone` |  | live | The block filled under streets and corridors; a 1.20 name, mapped through the block-name map. A name that resolves to no block rejects the profile |
| `spawnBiome` | `""` |  | live | Spawn in this biome (its 1.7.10 name, any case). Off when empty |
| `spawnCity` | `""` |  | live | Spawn inside this predefined city. Off when empty |
| `spawnSphere` | `""` |  | deferred | Spawn in a city sphere; no effect until spheres return |
| `spawnNotInBuilding` | `false` |  | live | Never spawn in a building chunk |
| `forceSpawnInBuilding` | `false` |  | live | Spawn on a building's ground floor; a player's first join places them exactly there |
| `forceSpawnBuildings` | `[]` |  | live | Spawn only in these buildings. A non-empty list applies even with `forceSpawnInBuilding` off, as upstream |
| `forceSpawnParts` | `[]` |  | live | Spawn only in buildings whose lowest floor is one of these parts. Applies even with `forceSpawnInBuilding` off |
| `spawnCheckRadius` | `200` | 1 to 100000 | live | Blocks the spawn search covers on its first pass, from block (0, 0) |
| `spawnRadiusIncrease` | `100` | 1 to 100000 | live | Blocks added to the search on each further pass |
| `spawnCheckAttempts` | `20000` | 1 to 1000000 | live | Chunks the search looks at before it keeps the world's own spawn |
| `scatteredChanceMultiplier` | `1.0` | 0 to 100 | live | Multiplies the world style's scattered-structure chance; 0 disables scattered structures (07-scattered) |
| `terrainFixLowerMinOffset` | `-4` | -40 to 40 | live | Lower mesh for raising terrain next to cities, minimum offset from city base level |
| `terrainFixLowerMaxOffset` | `-3` | -40 to 40 | live | Same, maximum offset |
| `terrainFixUpperMinOffset` | `-1` | -40 to 40 | live | Upper mesh for lowering terrain next to cities, minimum offset |
| `terrainFixUpperMaxOffset` | `1` | -40 to 40 | live | Same, maximum offset |
| `vineChance` | `0.009` | 0 to 1 | live | Chance that an outside building block gets a vine; below 0.000001 skips vines |
| `randomLeafBlockChance` | `0.1` | 0 to 1 | live | Chance of leaf blocks at the border between a building and a street |
| `randomLeafBlockThickness` | `2` | 1 to 8 | live | How thick those leaf patches look from the building side |
| `avoidFoliage` | `false` |  | live | Parks get no trees or flowers. City styles may override it |
| `landscapeType` | `default` | default | live | Landscape kind; only `default` is ported, anything else rejects the profile |
| `rubbleLayer` | `true` |  | live | Use the noise-driven dirt/stone/sand and leaf rubble layer that makes cities look overgrown |
| `rubbleDirtScale` | `3.0` | 0 to 100 | live | Noise scale of the dirt rubble; smaller is larger patches; 0 disables |
| `rubbleLeaveScale` | `6.0` | 0 to 100 | live | Noise scale of the leaf rubble; 0 disables |
| `ruinChance` | `0.015` | 0 to 1 | live | Chance a building is ruined (04-city-layout, `BuildingInfo`). A predefined building can prevent ruins. Upstream's default is 0.05; this mod's is lower, so ruins are an accent |
| `ruinMinlevelPercent` | `0.8` | 0 to 1 | live | Lowest start of the ruin cut, as a fraction of building height. The cut is `min + (max - min) * r` |
| `ruinMaxlevelPercent` | `1.0` | 0 to 1 | live | Highest start of the ruin cut |
| `bedrockLayer` | `0` | 0 to 10 | live | Read and writes nothing: RWG already places bedrock at Y 0 to 4. A value above 0 logs that it has no effect |
| `avoidWater` | `false` |  | live | The profile's liquid in parts becomes air |
| `groundLevel` | `71` | 2 to 255 | live | City level 0's ground Y; level n is `groundLevel + 6 × n`. Above 244 rejects the profile (highways would pass Y 255) |
| `seaLevel` | `-1` | -1 to 255 | live | Water level for parts and explosion damage; -1 means RWG's 63 |
| `chestWithoutLootChance` | `0.2` | 0 to 1 | live | Chance a loot chest stays empty |
| `buildingWithoutLootChance` | `0.2` | 0 to 1 | live | Chance a single (non-multi) building has no loot and no spawners |
| `generateSpawners` | `true` |  | live | Place spawners; otherwise spawner positions become air |
| `generateLoot` | `true` |  | live | Fill chests at generation from this mod's tier-band loot categories (Stone to EV); otherwise chests stay empty |
| `generateLighting` | `false` |  | live | Keep palette torches and add bridge lighting; otherwise torch entries become air (04-city-layout) |
| `buildingChance` | `0.3` | 0 to 1 | live | Chance a city chunk holds a building rather than a street. City styles may override |
| `buildingMinFloors` | `0` | 0 to 22 | live | Minimum floors above ground; 0 means the first floor only. The minimum used is `buildingMinFloors + 1` |
| `buildingMaxFloors` | `8` | 0 to 22 | live | Cap on floors above ground; at most 22 so nothing passes Y 255 |
| `buildingMinFloorsChance` | `4` | 1 to 60 | live | Floors are `minFloors + random(minChance + (cityFactor + 0.1) × (maxChance - minChance)) + 1`, then clamped; the bound is never below 1 |
| `buildingMaxFloorsChance` | `6` | 1 to 60 | live | See above |
| `buildingMinCellars` | `0` | 0 to 20 | live | Minimum cellars below ground (raised to the building asset's own minimum) |
| `buildingMaxCellars` | `3` | 0 to 20 | live | Maximum cellars; the code adds the chunk's city level to it before applying city-style constraints |
| `buildingDoorwayChance` | `0.6` | 0 to 1 | live | Chance of a doorway on each side and floor toward an adjacent city chunk |
| `buildingFrontChance` | `0.2` | 0 to 1 | live | Chance a building gets a front part toward an adjacent street. City styles may override |
| `multiUseCorner` | `false` |  | live | Multibuildings take the top-left chunk's city level (true) or the average of their chunks (false) |
| `useAvgHeightmap` | `false` |  | live | City level from the average of the chunk's height and up to four sampled city neighbors. Only active when the profile option `heightSampleSize` is above 2 |
| `parkChance` | `0.2` | 0 to 1 | live | Chance a non-building chunk is a park. City styles may override |
| `openLotParkChance` | `0.8` | 0 to 1 | live | Chance a hierarchical open lot gets a park part. City styles may override via `profile_overrides` |
| `corridorChance` | `0.7` | 0 to 1 | live | Chance a chunk may hold an underground corridor (also needs neighbors). City styles may override |
| `bridgeChance` | `0.7` | 0 to 1 | live | Chance a chunk may hold a bridge on each axis (also needs neighbors) |
| `plannedPrimaryBridgeChance` | `1.0` | 0 to 1 | live | Chance an eligible hierarchical primary-road water crossing becomes one bridge |
| `plannedPrimaryBridgeMaxLength` | `12` | 1 to 64 | live | Longest water gap a primary-road bridge spans |
| `fountainChance` | `0.05` | 0 to 1 | live | Chance a street chunk holds a fountain. City styles may override |
| `bridgeSupports` | `true` |  | live | Bridges get supports where needed |
| `parkElevation` | `true` |  | live | Parks get one extra layer of elevation. City styles may override |
| `parkBorder` | `true` |  | live | Park borders use the street block as base. City styles may override |
| `parkStreetThreshold` | `3` | 0 to 8 | live | How many surrounding streets a chunk needs to become a park. City styles may override |
| `streetGenerationMode` | `HIERARCHICAL_GRID_V1` |  | dropped | Only the hierarchical streets are ported |
| `primaryRoadSpacingX` | `8` | 8 to 128 | live | Spacing of candidate north/south primary roads |
| `primaryRoadSpacingZ` | `8` | 8 to 128 | live | Spacing of candidate east/west primary roads |
| `primaryRoadOptionalChance` | `0.45` | 0 to 1 | live | Chance a non-forced candidate primary road is enabled |
| `primaryRoadForceEvery` | `4` | 1 to 16 | live | Every Nth candidate primary road is forced, capping the gap |
| `secondaryRoadMinCountX` | `0` | 0 to 128 | live | Minimum north/south secondary roads inside a primary block |
| `secondaryRoadMaxCountX` | `2` | 0 to 128 | live | Maximum north/south secondary roads |
| `secondaryRoadMinCountZ` | `0` | 0 to 128 | live | Minimum east/west secondary roads |
| `secondaryRoadMaxCountZ` | `2` | 0 to 128 | live | Maximum east/west secondary roads |
| `minimumRoadSeparation` | `4` | 2 to 32 | live | Minimum distance between parallel secondary roads |
| `minimumRoadEdgeDistance` | `3` | 2 to 32 | live | Minimum distance between a secondary road and its bounding primary road |
| `tertiaryRoadChance` | `0.4` | 0 to 1 | live | Chance a subdivided block gets one short tertiary access road |
| `tertiaryRoadMinLength` | `2` | 1 to 16 | live | Minimum tertiary road length |
| `tertiaryRoadMaxLength` | `5` | 1 to 32 | live | Maximum tertiary road length |
| `multiBuildingStreetConflict` | `OVERRIDE_MINOR` | BLOCK_ALL, OVERRIDE_MINOR, OVERRIDE_ALL | live | How random multibuildings meet planned roads: `BLOCK_ALL` lets any planned road block them, `OVERRIDE_MINOR` only primary roads, `OVERRIDE_ALL` none. Ignored by legacy streets |
| `railwayDungeonChance` | `0.01` | 0 to 1 | live | Chance a chunk next to a railway holds a railway dungeon |
| `railwaysCanEnd` | `false` |  | live | A station spot without a city above gets an end part when one side has no connection |
| `railwaysEnabled` | `true` |  | live | Railways on or off (stations still generate when off) |
| `railwayStationsEnabled` | `true` |  | live | Railway stations on or off |
| `railwaySurfaceStationsEnabled` | `true` |  | live | Surface stations allowed; otherwise underground only |
| `railwayLevelOffset` | `0` | -8 to 2 | live | Vertical offset of the underground railway in levels of 6 blocks; raised, with a warning, so the rails stay above Y 5 |
| `railwaySpacingNorthSouth` | `10` | 4 to 128 | live | North-south distance between east-west tracks, in chunks; even |
| `railwaySpacingEastWest` | `10` | 4 to 128 | live | East-west distance between north-south tracks, in chunks; even |
| `highwayRequiresTwoCities` | `true` |  | dropped | Read only by legacy highways, which are not ported |
| `highwayLevelFromCities` | `3` | 0 to 4 | live | Highway level: 0 first endpoint city, 1 minimum of both, 2 maximum, 3 average, 4 `highwayNetworkLevel` |
| `highwayDistanceMask` | `7` |  | dropped | Read only by legacy highways; `highwaysEnabled` switches highways off |
| `highwayMainPerlinScale` | `50.0` |  | dropped | Legacy highways only |
| `highwaySecondaryPerlinScale` | `10.0` |  | dropped | Legacy highways only |
| `highwayPerlinFactor` | `2.0` |  | dropped | Legacy highways only |
| `highwaySupports` | `true` |  | live | Highways get supports where needed |
| `highwayGenerationMode` | `INTERCITY_NETWORK_V1` |  | dropped | Only the intercity highway planner is ported |
| `highwayPlanningCellSize` | `128` | 32 to 512 | live | Size of one hub-planning cell; at least `highwayHubSampleSpacing`, or the profile is rejected |
| `highwayHubSampleSpacing` | `16` | 1 to 512 | live | Spacing of city-potential samples inside a cell |
| `highwayHubMinimumPotential` | `0.2` | 0 to 1 | live | Minimum height- and biome-adjusted city potential for a hub |
| `highwayHubSearchRadiusCells` | `2` | 0 to 8 | live | Cell radius in which a hub looks for others |
| `highwayMinimumHubDistance` | `64` | 0 to 4096 | live | Minimum Euclidean distance between connected hubs; at most the maximum, or the profile is rejected |
| `highwayMaximumHubDistance` | `320` | 0 to 4096 | live | Maximum Euclidean distance between connected hubs |
| `highwayMaximumConnectionsPerHub` | `2` | 1 to 8 | live | Maximum accepted connections per hub |
| `highwayMinimumRouteLength` | `40` | 0 to 4096 | live | Minimum Manhattan length of an inter-city route |
| `highwayRouteCityPenalty` | `1.0` | 0 to 1000 | live | Weight of city potential when choosing an L-route bend |
| `highwayNetworkLevel` | `0` | 0 to 32 | live | Fixed highway level when `highwayLevelFromCities` is 4; lowered, with a warning, so highways stay below Y 255 (28 at ground 71) |
| `editMode` | `false` |  | live | The world is a part-editing world: generation records every part it writes in `data/lostcitiesnh_edit.dat`, and the part editor's commands work ([commands.md](new-spec/01-core/commands.md)) |
| `generateNether` | `false` |  | dropped | Dead upstream; overworld only |

#### `explosions`

| Key | Default | Range | Status | What it does |
|---|---|---|---|---|
| `debrisToNearbyChunkFactor` | `200` | 1 to 10000 | live | Divisor for debris spilling from damaged neighbor chunks; larger is less debris |
| `explosionChance` | `0.0004` | 0 to 1 | live | Chance a chunk holds an explosion center. Upstream's default is 0.002; this mod's is lower, so craters are an accent |
| `explosionMinRadius` | `12` | 1 to 1000 | live | Minimum explosion radius; at most the maximum, or the profile is rejected |
| `explosionMaxRadius` | `24` | 1 to 3000 | live | Maximum explosion radius (exclusive); upstream's default is 35 |
| `explosionMinHeight` | `75` | 1 to 255 | live | Minimum center height; the center Y is `cityLevel × 6 + min + random(max - min)` |
| `explosionMaxHeight` | `90` | 1 to 255 | live | Maximum explosion center height (exclusive); at most 255 |
| `miniExplosionChance` | `0.005` | 0 to 1 | live | Chance a chunk holds a mini explosion; upstream's default is 0.03 |
| `miniExplosionMinRadius` | `4` | 1 to 1000 | live | Minimum mini-explosion radius; at most the maximum, or the profile is rejected |
| `miniExplosionMaxRadius` | `9` | 1 to 3000 | live | Maximum mini-explosion radius (exclusive); upstream's default is 12 |
| `miniExplosionMinHeight` | `60` | 1 to 255 | live | Minimum mini-explosion center height (plus `cityLevel × 6`) |
| `miniExplosionMaxHeight` | `100` | 1 to 255 | live | Maximum mini-explosion center height (exclusive); at most 255 |
| `explosionsInCitiesOnly` | `true` |  | live | Explosion centers only in city chunks (the blast still reaches non-city chunks) |

#### `cities`

| Key | Default | Range | Status | What it does |
|---|---|---|---|---|
| `cityChance` | `0.01` | -1 to 1 | live | Chance a chunk is a city center; negative switches to the Perlin rarity map |
| `cityPlacement` | `chance` | chance, grid | live | How city centers are placed. `chance`: each chunk is a center with probability `cityChance` (upstream). `grid`: one center per `cityGridCell` x `cityGridCell` chunk cell, jittered, with radius drawn in `cityMinRadius` to `cityMaxRadius`; `cityChance` then only has to be 0 or above. A world pinned without this key reads `chance` |
| `cityGridCell` | `64` | 8 to 1024 | live | Grid placement: cell edge in chunks |
| `cityGridJitter` | `16` | 0 to 511 | live | Grid placement: chunks kept clear inside every cell edge, so centers are at least `cityGridCell` minus twice this apart; below half of `cityGridCell`, or the profile is rejected |
| `citySpawnDistance1` | `0` | 0 to 10000000 | live | Distance from (0, 0) at which the city factor is multiplied by multiplier 1 |
| `citySpawnDistance2` | `0` | 0 to 10000000 | live | Distance at which multiplier 2 applies; 0 disables the distance scaling. Linear in between |
| `citySpawnMultiplier1` | `1.0` | 0 to 1 | live | City factor multiplier at and inside distance 1 |
| `citySpawnMultiplier2` | `1.0` | 0 to 1 | live | City factor multiplier at and beyond distance 2 |
| `cityMinRadius` | `50` | 1 to 2000 | live | Minimum city radius (chance mode) |
| `cityMaxRadius` | `128` | 1 to 2000 | live | Maximum city radius (exclusive); at least `cityMinRadius`, or the profile is rejected |
| `cityPerlinScale` | `3.0` | -1000000 to 1000000 | live | Scale of the city rarity perlin map (perlin mode) |
| `cityPerlinOffset` | `0.1` | -1000000 to 1000000 | live | Offset of the rarity map |
| `cityPerlinInnerScale` | `0.1` | -1000000 to 1000000 | live | Internal scale of the rarity map |
| `cityThreshold` | `0.2` | 0 to 1 | live | A chunk is a city when its city factor is above this (both modes) |
| `cityStyleThreshold` | `-1.0` | -1 to 1 | live | Below this city factor the alternative style is used; -1 (any negative) is off |
| `cityStyleAlternative` | `""` |  | live | The alternative city style; must be a loaded city style while the threshold is on |
| `cityAvoidVoid` | `true` |  | deferred | Floating landscape only |
| `cityLevel0Height` | `75` | 1 to 255 | live | Terrain height below this gives city level 0 |
| `cityLevel1Height` | `83` | 1 to 255 | live | Below this, level 1 |
| `cityLevel2Height` | `91` | 1 to 255 | live | Below this, level 2 |
| `cityLevel3Height` | `99` | 1 to 255 | live | Below this, level 3 |
| `cityLevel4Height` | `107` | 1 to 255 | live | Below this, level 4 |
| `cityLevel5Height` | `115` | 1 to 255 | live | Below this, level 5 |
| `cityLevel6Height` | `123` | 1 to 255 | live | Below this, level 6 |
| `cityLevel7Height` | `131` | 1 to 255 | live | Below this, level 7; at or above, level 8 |
| `cityMinHeight` | `50` | 0 to 255 | live | Chunks whose terrain is below this get no city |
| `cityMaxHeight` | `150` | 0 to 255 | live | Chunks whose terrain is above this get no city; 150 keeps cities off RWG2's mountain chains |
| `oceanCorrectionBorder` | `4` | -255 to 255 | live | Terrain correction offset for chunks next to cities that lie in ocean biomes (03-terrain) |

#### `cityspheres`

| Key | Default | Range | Status | What it does |
|---|---|---|---|---|
| `citySphereFactor` | `1.2` | 0.1 to 10 | deferred | Outer sphere radius = city radius × factor (space) |
| `citySphereChance` | `0.7` | 0 to 1 | deferred | Chance a sphere generates |
| `citySphereClearAbove` | `0` | 0 to 1024 | deferred | Blocks cleared above the top sphere glass; 0 disables |
| `citySphereClearAboveUntilAir` | `false` |  | deferred | Keep clearing upward until air |
| `citySphereClearBelow` | `0` | 0 to 1024 | deferred | Blocks cleared below the sphere; 0 disables |
| `citySphereClearBelowUntilAir` | `false` |  | deferred | Keep clearing downward until air |
| `sphereSurfaceVariation` | `1.0` |  | dropped | Dead upstream |
| `outsideSurfaceVariation` | `1.0` |  | dropped | Dead upstream |
| `monorailChance` | `0.8` | 0 to 1 | deferred | Chance a sphere city wants a monorail in a direction |
| `onlyPredefined` | `false` |  | deferred | Only predefined spheres generate |
| `outsideGroundLevel` | `-1` |  | dropped | Dead upstream |
| `outsideProfile` | `""` |  | deferred | The profile used for chunks outside spheres |
| `monorailOffset` | `-2` | -100 to 100 | deferred | Monorail height relative to the main height (06-highways-railways) |
| `grid32` | `false` |  | deferred | Align spheres to a 32 × 32-chunk grid instead of 16 × 16 |

#### `client`

| Key | Default | Range | Status | What it does |
|---|---|---|---|---|
| `horizon` | `-1.0` |  | dropped | Dead upstream; no horizon or fog here |
| `fogRed` | `-1.0` |  | dropped | Dead upstream; no horizon or fog here |
| `fogGreen` | `-1.0` |  | dropped | Dead upstream; no horizon or fog here |
| `fogBlue` | `-1.0` |  | dropped | Dead upstream; no horizon or fog here |
| `fogDensity` | `-1.0` |  | dropped | Dead upstream; no horizon or fog here |

#### `lostcity`: this port's own options

They go in the `lostcity` category of a profile file, with upstream's options.

| Key | Default | Range | Status | What it does |
|---|---|---|---|---|
| `preventLakesInCities` | `true` |  | live | Vetoes RWG's lakes, lava lakes and dungeons, NHCore's oil and its Railcraft quarried-stone populator where a population area touches a city chunk or a scattered structure; not ravines. 1.12's `preventLakesRavinesInCities` still loads |
| `highwaysEnabled` | `true` |  | live | Generate highways in this world |
| `villageTreatment` | `BUILD_AROUND` | BUILD_AROUND, AVOID, IGNORE | live | How cities meet villages: `BUILD_AROUND` (sew them in), `AVOID` (upstream's avoidance), `IGNORE` |
| `landmarkTreatment` | `BUILD_AROUND` |  | live | The same for other landmarks: one treatment for all, and `<kind>=<treatment>` per kind, comma-separated (`ocean_monument`, `roguelike`, `witchery`, `lootgames`, `meteorite`, `terrain`) |
| `landmarkMargin` | `2` | 0 to 16 | live | Blocks a landmark's footprint grows by before it becomes core chunks |
| `softEdgeWidth` | `6` | 2 to 12 | live | Columns of the strip in a landmark core's margin that meets the blended ground to the core's own edge |
| `cityMaxRise` | `2` | 0 to 8 | live | City levels a city chunk may rise above its city's base level; one more is cut down to it, higher ground is left out of the city; 8 is no cap |
| `cityGrade` | `1` | 0 to 8 | live | City levels a city chunk may sit above or below its city's base level: hills are cut and hollows filled to within this many levels (8 blocks each), so neighboring streets and buildings stay close in height; 8 is no limit |
| `floorPitch` | `8` | 6 to 10 | live | Blocks per building floor and cellar: floor parts are stretched by repeating a mid-room row, and stair runs are re-laid to match; city levels, highways and rails stay 6. A world pinned before this option existed reads 6 (design record 0014) |
| `cityGradeRadius` | `1` | 0 to 4 | live | Chunks around a city chunk whose mean terrain height sets its level, so neighbors change level gradually; 0 uses the chunk's own height |
| `cityAvoidOcean` | `true` | | live | Chunks in an ocean biome are never city, and neither are chunks whose RWG terrain height is under sea level (63), which catches shallow water in biomes not tagged ocean. A profile with its own `seaLevel` (a drowned city) skips the height test |
| `cityConnected` | `true` | | live | A city chunk must join its city's center through other city chunks, edge to edge; a chunk that terrain, water or a landmark cut off leaves the city instead of standing alone (center mode only) |
| `cityTaper` | `1.0` | 0 to 1 | live | How strongly building heights follow the urban intensity: low at a city's edge, a high-rise core at its center, varied by noise ([0011](design/0011-city-edges-and-character.md)); 0 is upstream's floor count |
| `edgeLotChance` | `0.45` | 0 to 1 | live | Chance that a building chunk at a city's very edge is an empty lot, an excavation or a foundation instead; it falls to 0 at intensity 0.4, and open lots take half of it |
| `packHouseChance` | `0.25` | 0 to 1 | live | Chance that an empty edge lot holds a house built by a pack mod's own village generator (vanilla, Railcraft, Tinkers' Construct, Forestry, Witchery), clipped to the lot chunk with its door toward a street; 0 for none. Mods that are absent or have disabled their houses are skipped. Worlds pinned before this option existed read 0 |
| `damageRepairChance` | `0.5` | 0 to 1 | live | Chance that an explosion's damage in a city was partly filled back in with a similar but not identical material, and its windows boarded; 0 leaves all damage |
| `facadeContrast` | `45.0` | 10 to 200 | live | Most contrast (lightness and saturation) two neighboring buildings' facades may have; palettes are chosen against neighbors to keep it ([0012](design/0012-facade-color-feathering.md)); 200 turns it off |
| `villageTransitionRings` | `2` | 0 to 8 | live | Rings of lower building density around a village |
| `villageLinkDistance` | `4` | 0 to 16 | live | Chunks outside a city a village still gets a lane to it |
| `vanillaLootCategories` | `false` |  | live | City chests may also draw the pack's vanilla loot categories (`dungeonChest` instead of the supplies band, and the temple, stronghold, bonus-chest and blacksmith categories upstream's `chestloot` names). Off: chests draw only this mod's tier bands, Stone to EV |
| `enderZooHardMobs` | `false` |  | live | `hardmobs` spawners take EnderZoo's overworld mobs (Fallen Knight, Concussion Creeper, Enderminy, Wither Witch, Dire Wolf) instead of blazes and witches; a skeleton where EnderZoo is absent |
| `heightSampleSize` | `3` | 1 to 100 | live | Upstream's mod-config key, pinned here: chunks sampled for a city level when `useAvgHeightmap` is on (averaging only above 2) |

## Development and testing switches

JVM properties, not config keys. They exist for development and real-pack testing and are off in a normal game.

| Property | Effect |
|---|---|
| `-Dlostcitiesnh.testHooks=true` | Turns on the scenario command and harness, and the `view` and `find` commands, outside the development environment ([docs/testing/](testing/README.md)) |
| `-Dlostcitiesnh.selfCheck=true` or `false` | Runs the dev self-check at `postInit` outside the development environment, or skips it inside |
| `-Dlostcitiesnh.scenarios=<selector>` | Runs the selected scenarios once the server has started, where the test hooks are on |
| `-Dlostcitiesnh.debugWool=true` | Paints each terrain stage's blocks in a wool color, for finding which stage wrote what ([docs/testing/](testing/README.md#debug-wool)) |
| `-Dlostcitiesnh.stopAfterScenarios=true` | Stops the server after that run's summary line |
| `-Dlostcitiesnh.assetReport=true` | Logs the full asset report at `postInit`: every problem, every fallback block taken and every variety-pool block the registry lacks ([asset-format.md](new-spec/05-assets/asset-format.md#the-block-name-map)) |
| `-Dlostcitiesnh.prefetch=false` | Turns off the plan prefetcher, which plans chunks a few chunks ahead of generation a little each server tick so a city meeting new ground does not stall one chunk's generation ([0005](design/0005-rwg-overlay.md) §8). What is generated is the same either way; for comparing budget runs |
| `-Dlostcitiesnh.dimensionProfiles=<dim>=<profile>[,...]` | With the test hooks on, replaces `dimensionProfiles` for this run, so a real-pack run can create a world with another profile without editing the config file |
| `-Dlostcitiesnh.profileOverrides=<key>=<value>[;...]` | With the test hooks on, these option values replace the pinned profile's when a world is pinned, checked like a profile file; for trying an option in a real-pack run |
| `-Dlostcitiesnh.planSource=synthetic` | With the test hooks on, pins the synthetic test city (M04) into new worlds instead of the real cities of the layout, for the overlay scenarios and screenshots ([docs/testing/](testing/README.md)) |
| `-Dlostcitiesnh.capture=true` | Client, with the test hooks on: clean screenshot capture. Hides the GUI, hand, chat, achievement toast and HUD overlays, and logs `[LCNH-CAPTURE] settled` when a viewpoint has loaded ([tools/remote/README.md](../tools/remote/README.md#clean-capture)) |
| `-Dlostcitiesnh.captureFov=<30-110>`, `captureRenderDistance=<2-16>`, `captureChunkRadius=<n>`, `captureClouds=true` | The capture's field of view (default 70), render distance (default 10), the radius of chunks that must arrive before a viewpoint settles (default render distance − 1), and clouds (default off) |
| `-Dlostcitiesnh.configScreenProbe=true` | Client, with the test hooks on: at the title screen, opens the Mods list, presses Config for this mod and checks that every label and tooltip of GTNHLib's config screen translates, logging `[LCNH-CLIENT] config` lines ([docs/testing/](testing/README.md#scenario-list)) |
| `-Dlostcitiesnh.captureServer=<host>:<port>` | With the capture on, the client joins that server from the main menu by itself, and retries once after a disconnect |
