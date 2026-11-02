# Profile options on 1.7.10

What happens to each of upstream's 163 profile options ([original catalog](../../original-spec/02-profiles-dimensions/profile-options.md)), and which options are new. The [02 index](index.md#profile-options) summarizes it. Each option's meaning, default and consumer are upstream's unless a row says otherwise; the area named under **Owner** builds the consumer and may refine the row in its own new-spec area.

Status: rows that cite a decision are *Accepted*; rows marked *Proposed* are listed in [Proposed, not decided](index.md#proposed-not-decided).

## Rules for every option

- **Format.** Profiles stay in upstream's JSON format: the same five categories, the same keys ([0005](../../design/0005-rwg-overlay.md) §7). A file written for upstream 1.20 loads.
- **Dropped keys** are read and ignored, with one debug log line per file, so upstream-format files still load. They are not written back by `saveprofile`.
- **Deferred keys** (the sphere and non-default landscape options) are kept in the format, validated and stored, but have no effect until the landscape types return after beta (D12).
- **Ranges are enforced when a profile loads**, not only in a GUI (PROF-04). How a violation is handled is *Proposed* ([P-PROF-1](index.md#proposed-not-decided)).
- **Heights.** RWG's sea level is 63, as the 1.20 overworld's, so upstream's Y numbers carry over; any range above 255 or below 0 is clamped to 0 to 255 at load with a warning ([0005](../../design/0005-rwg-overlay.md) §6).
- **Block names** (`liquidBlock`, `baseBlock`) are upstream's 1.20 names, translated at load by the block-mapping table (D14, D19).
- **Every value is pinned** into the world's snapshot when the world is created ([pinned-profile.md](pinned-profile.md)).

## Category `lostcity` (107 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `description`, `extraDescription`, `warning` | changed | Shown by `/lostcities profile` and in the log, not in a GUI ([09-client-ui](../09-client-ui/index.md)). Built-ins use lang keys with texts that promise only what the options do; user profiles keep their literal text. Warnings that name Lost Worlds go. | 02 | [CONTRIBUTING § Assets and localization](../../../CONTRIBUTING.md#assets-and-localization) | PROF-18 |
| `icon` | dropped | Upstream's icons are screenshots of Minecraft textures ([0001](../../design/0001-port-policy-and-licensing.md) §5) and were never displayed. | 09 | [0001](../../design/0001-port-policy-and-licensing.md) §5 | UI-10 |
| `worldStyle` | same | Validated at load: a profile that names a missing world style is rejected with a message, never a crash at dimension setup. | 02, 05 | Bug register | PROF-13 |
| `liquidBlock`, `baseBlock` | changed | 1.20 names through the block-mapping table; an unknown name is an error at load. The water and stone fallbacks stay. | 05 | D14, D19 | CORE-20 |
| `spawnBiome`, `spawnCity`, `spawnSphere`, `spawnNotInBuilding`, `forceSpawnInBuilding`, `forceSpawnBuildings`, `forceSpawnParts`, `spawnCheckRadius`, `spawnRadiusIncrease`, `spawnCheckAttempts` | same | Off by default, searched in the layout. `spawnBiome` names a 1.7.10 biome. `spawnSphere` is deferred with spheres. `forceSpawnInBuilding` places a player exactly only on first join. | 01 | D21, [01 · Spawn selection](../01-core/index.md#spawn-selection) | CORE-13, CORE-14, CORE-15, CORE-16 |
| `scatteredChanceMultiplier` | same | | 07 | | |
| `terrainFixLowerMinOffset`, `terrainFixLowerMaxOffset`, `terrainFixUpperMinOffset`, `terrainFixUpperMaxOffset` | same | Upstream's −4/−3 and −1/+1. | 03 | [0005](../../design/0005-rwg-overlay.md) §5, §6 | |
| `vineChance`, `randomLeafBlockChance`, `randomLeafBlockThickness`, `avoidFoliage`, `rubbleLayer`, `rubbleDirtScale`, `rubbleLeaveScale`, `ruinChance`, `ruinMinlevelPercent`, `ruinMaxlevelPercent`, `avoidWater` | same | | 03, 04 | | |
| `landscapeType` | changed | Only `default` is valid. Any other value rejects the profile at load with a message, so the allowed list and the check agree. | 02 | [0005](../../design/0005-rwg-overlay.md) §7, D12 | PROF-15 |
| `bedrockLayer` | changed | Read and writes nothing, with a warning when above 0: RWG already places bedrock at Y 0 to 4, and generation never places unbreakable blocks. The key stays so upstream profiles load. | 03 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `groundLevel` | same | 71, range clamped to 2 to 255. | 03, 04 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `seaLevel` | changed | −1 resolves to 63 (RWG fills water below 63), not to a generator query. Explosion damage reads this resolved value too. | 03 | [0005](../../design/0005-rwg-overlay.md) §6 | PROF-14 |
| `chestWithoutLootChance`, `buildingWithoutLootChance`, `generateSpawners`, `generateLoot`, `generateLighting` | same | Loot is pack loot under the EV ceiling, spawners stay vanilla. | 08 | D20 | |
| `buildingChance`, `buildingMinFloors`, `buildingMinFloorsChance`, `buildingMaxFloorsChance`, `buildingMinCellars`, `buildingMaxCellars`, `buildingDoorwayChance`, `buildingFrontChance`, `multiUseCorner`, `useAvgHeightmap` | same | The floor-chance pair is checked at load so the random bound is never below 1. | 04 | Bug register | PROF-06 |
| `buildingMaxFloors` | changed | Default 8, allowed range 0 to 22 instead of 0 to 60. Upstream's per-chunk cap (below Y 248) keeps everything below Y 255: at city level 8 a building gets at most 21 floors ([height-mapping.md](../03-terrain/height-mapping.md#building-envelope)). | 04 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `parkChance`, `openLotParkChance`, `corridorChance`, `bridgeChance`, `plannedPrimaryBridgeChance`, `plannedPrimaryBridgeMaxLength`, `fountainChance`, `bridgeSupports`, `parkElevation`, `parkBorder`, `parkStreetThreshold`, `primaryRoadSpacingX`, `primaryRoadSpacingZ`, `primaryRoadOptionalChance`, `primaryRoadForceEvery`, `secondaryRoadMinCountX`, `secondaryRoadMaxCountX`, `secondaryRoadMinCountZ`, `secondaryRoadMaxCountZ`, `minimumRoadSeparation`, `minimumRoadEdgeDistance`, `tertiaryRoadChance`, `tertiaryRoadMinLength`, `tertiaryRoadMaxLength`, `multiBuildingStreetConflict` | same | Upstream's explicit checks stay, with upstream's messages as lang keys. | 04 | | |
| `streetGenerationMode` | dropped | Only `HIERARCHICAL_GRID_V1` is ported, and it is not persisted. | 04 | D11 | |
| `railwayDungeonChance`, `railwaysCanEnd`, `railwaysEnabled`, `railwayStationsEnabled`, `railwaySurfaceStationsEnabled`, `railwayLevelOffset`, `railwaySpacingNorthSouth`, `railwaySpacingEastWest` | same | Rail heights within 0 to 255 are `06-highways-railways`' to map. | 06 | | |
| `highwayLevelFromCities`, `highwaySupports`, `highwayPlanningCellSize`, `highwayHubSampleSpacing`, `highwayHubMinimumPotential`, `highwayHubSearchRadiusCells`, `highwayMinimumHubDistance`, `highwayMaximumHubDistance`, `highwayMaximumConnectionsPerHub`, `highwayMinimumRouteLength`, `highwayRouteCityPenalty`, `highwayNetworkLevel` | same | The intercity planner's options. It runs synchronously with no saved hub data. | 06 | D11 | |
| `highwayGenerationMode` | dropped | Only `INTERCITY_NETWORK_V1` is ported, and it is not persisted. | 06 | D11 | |
| `highwayRequiresTwoCities`, `highwayMainPerlinScale`, `highwaySecondaryPerlinScale`, `highwayPerlinFactor` | dropped | Read only by legacy noise highways ([Highway.java#L136](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Highway.java#L136), [#L168-L174](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Highway.java#L168-L174)). | 06 | D11 | |
| `highwayDistanceMask` | *Proposed*: dropped | Read only by legacy highways too ([Highway.java#L101-L105](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Highway.java#L101-L105)); its "0 disables highways" use goes to the highways module flag ([P-PROF-5](index.md#proposed-not-decided)). | 06 | D11; *Proposed* | (`06`'s HWY-06) |
| `editMode` | same | Pinned at creation, so a world is an edit world from its start. | 01 | D13 | |
| `generateNether` | dropped | Dead upstream; overworld only. | 02 | D12, [0003](../../design/0003-world-presence.md) | PROF-08 |

## Category `explosions` (12 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `debrisToNearbyChunkFactor`, `explosionChance`, `explosionsInCitiesOnly`, `miniExplosionChance` | same | | 03 | | |
| `explosionMinRadius`, `explosionMaxRadius`, `explosionMinHeight`, `explosionMaxHeight`, `miniExplosionMinRadius`, `miniExplosionMaxRadius`, `miniExplosionMinHeight`, `miniExplosionMaxHeight` | changed | Heights clamped to 0 to 255. Each minimum must be below its maximum, checked at load. | 03 | [0005](../../design/0005-rwg-overlay.md) §6; bug register | PROF-06 |

## Category `cities` (25 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `cityChance`, `cityMinRadius`, `cityMaxRadius`, `cityPerlinScale`, `cityPerlinOffset`, `cityPerlinInnerScale`, `cityThreshold`, `oceanCorrectionBorder` | same | City placement and city-center styles mix in the world seed; each chunk takes its city center's style. The radius pair is checked at load. | 04 | D10 | PROF-06, PROF-10, PROF-11 |
| `citySpawnDistance1`, `citySpawnDistance2`, `citySpawnMultiplier1`, `citySpawnMultiplier2` | same | Upstream's semantics: they only scale cities that already exist, so they cannot place one near spawn. A city near spawn comes from a new seeded option, on by default, if M03's measurement shows it is needed (D30). | 04 | D21, D30 | |
| `cityStyleThreshold` | changed | Accepted range is −1 (off) or 0 to 1, so the default is inside it. | 04 | Bug register | PROF-16 |
| `cityStyleAlternative` | changed | Must name an existing city style whenever `cityStyleThreshold` is on; checked at load. | 04 | Bug register | PROF-12 |
| `cityAvoidVoid` | deferred | Acts only in the `floating` landscape. | 03 | D12 | |
| `cityLevel0Height` to `cityLevel7Height` | same | 75 to 131, step 8; city ground 71 to 119. Ranges clamped to 0 to 255. | 04 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `cityMinHeight`, `cityMaxHeight` | same | 50 and 150; 150 keeps cities off RWG2's mountain chains. Ranges clamped to 0 to 255. | 04 | [0005](../../design/0005-rwg-overlay.md) §6 | |

## Category `cityspheres` (14 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `citySphereFactor`, `citySphereChance`, `citySphereClearAbove`, `citySphereClearAboveUntilAir`, `citySphereClearBelow`, `citySphereClearBelowUntilAir`, `onlyPredefined`, `outsideProfile`, `grid32` | deferred | With the sphere landscapes. When they return, `outsideProfile` is checked at load. | 03 | D12 | PROF-07 |
| `monorailChance`, `monorailOffset` | deferred | With the monorails only `space` generates. | 06 | D12 | |
| `sphereSurfaceVariation`, `outsideSurfaceVariation`, `outsideGroundLevel` | dropped | No reader upstream. | 02 | D12 | PROF-17 |

## Category `client` (5 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `horizon`, `fogRed`, `fogGreen`, `fogBlue`, `fogDensity` | dropped | Dead upstream; the horizon needs a world type of its own. | 09 | D12, [0005](../../design/0005-rwg-overlay.md) §7 | PROF-17 |

## Root key

| Key | Here | What changes | Decision |
|---|---|---|---|
| `public` | changed | Read and kept; it has no effect until a client profile picker exists ([09-client-ui](../09-client-ui/index.md)). | [0005](../../design/0005-rwg-overlay.md) §7 |

## Options that are new

| Option | Category | Default | Meaning | Owner | Decision |
|---|---|---|---|---|---|
| Lake and dungeon veto in city chunks (upstream 1.12's `preventLakesRavinesInCities`; name *Proposed*, [P-PROF-6](index.md#proposed-not-decided)) | `lostcity` | on | Vetoes RWG's `LAKE`, `LAVA` and `DUNGEON` populate steps and NHCore's oil deposits wherever a population area touches a city chunk; landmark cores get no veto | 03 | [0005](../../design/0005-rwg-overlay.md) §3 |
| `villageTreatment` | `lostcity` | `BUILD_AROUND` | `BUILD_AROUND`, `AVOID` (upstream's avoidance), `IGNORE`; replaces `avoidVillages` | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §3 |
| `landmarkTreatment` | `lostcity` | `BUILD_AROUND` per kind | The same three values for each landmark kind; replaces `avoidStructures` | 04, 07 | [0006](../../design/0006-build-around-other-worldgen.md) §3 |
| `landmarkMargin` | `lostcity` | 2 blocks | Growth of a landmark footprint before it is turned into core chunks | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §1 |
| `softEdgeWidth` | `lostcity` | 6 columns | Width of the strip that steps city ground down to natural ground facing a core | 03 | [0006](../../design/0006-build-around-other-worldgen.md) §4 |
| `villageTransitionRings` | `lostcity` | 2 | Rings of lower building density around a village | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §1 |
| `villageLinkDistance` | `lostcity` | 4 chunks | How far outside a city a village still gets a lane | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §3 |
| Vanilla loot categories opt-in (name `08-loot-mobs-blocks`') | `lostcity` | off | Lets city chests also draw from vanilla `ChestGenHooks` categories | 08 | D20 |
| A city near spawn (name and distance set at M03; added only if M03's measurement shows the nearest city center is too far from spawn) | `lostcity` | on | Guarantees a city center within a set distance of (0, 0), seeded from the world seed | 04 | D30 |

Placing the new options in the `lostcity` category is *Proposed* ([P-PROF-6](index.md#proposed-not-decided)). The mod-config keys that are pinned with the profile (module flags, the decorative metal flags, `heightSampleSize`, `forceSaplingGrowth`) are in [01-core § Mod config](../01-core/index.md#mod-config) and [pinned-profile.md](pinned-profile.md).

## Count

107 + 12 + 25 + 14 + 5 = 163 upstream options: 134 kept (same or changed), 17 dropped (one of them, `highwayDistanceMask`, *Proposed*), 12 deferred; 8 new options, and a ninth, the near-spawn city, if M03 needs it (D30).
