# Profile options on 1.7.10

What happens to each of upstream's 163 profile options ([original catalog](../../original-spec/02-profiles-dimensions/profile-options.md)), and which options are new. The [02 index](index.md#profile-options) summarizes it. Each option's meaning, default and consumer are upstream's unless a row says otherwise; the area named under **Owner** builds the consumer and may refine the row in its own new-spec area.

Status: rows that cite a decision are *Accepted*. The former proposals on this page (P-PROF-1, P-PROF-5, P-PROF-6) were accepted as recommended by decision log D31; nothing here is open.

## Rules for every option

- **Format.** Profiles stay in upstream's JSON format: the same five categories, the same keys ([0005](../../design/0005-rwg-overlay.md) §7). A file written for upstream 1.20 loads.
- **Dropped keys** are read and ignored without a warning, so upstream-format files still load. They are not pinned and not written back by `saveprofile`.
- **Deferred keys** (the sphere and non-default landscape options) are kept in the format, validated and stored, but have no effect until the landscape types return after beta (D12).
- **Ranges are enforced when a profile loads**, not only in a GUI (PROF-04). A single numeric value out of range is clamped with a warning; anything a clamp cannot fix (a wrong type, an unknown enum value, an inverted min/max pair, a failed cross-option check) rejects the profile with a message, and mod loading never fails (P-PROF-1, D31).
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
| `vineChance`, `randomLeafBlockChance`, `randomLeafBlockThickness`, `avoidFoliage`, `rubbleLayer`, `rubbleDirtScale`, `rubbleLeaveScale`, `ruinMinlevelPercent`, `ruinMaxlevelPercent`, `avoidWater` | same | | 03, 04 | | |
| `ruinChance` | changed | Default 0.015 instead of 0.05, so a ruined top is an occasional accent ([03-terrain](../03-terrain/index.md#explosions-damage-debris-ruins-rubble-damage-ruins-rubblemd)). A profile can still ask for upstream's 0.05. | 03, 04 | AR-8 ([alpha review](../../reviews/2026-11-02-alpha.md)) | |
| `landscapeType` | changed | Only `default` is valid. Any other value rejects the profile at load with a message, so the allowed list and the check agree. | 02 | [0005](../../design/0005-rwg-overlay.md) §7, D12 | PROF-15 |
| `bedrockLayer` | changed | Read and writes nothing, with a warning when above 0: RWG already places bedrock at Y 0 to 4, and generation never places unbreakable blocks. The key stays so upstream profiles load. | 03 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `groundLevel` | same | 71, range clamped to 2 to 255. | 03, 04 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `seaLevel` | changed | −1 resolves to 63 (RWG fills water below 63), not to a generator query. Explosion damage reads this resolved value too. | 03 | [0005](../../design/0005-rwg-overlay.md) §6 | PROF-14 |
| `chestWithoutLootChance`, `buildingWithoutLootChance`, `generateSpawners`, `generateLoot`, `generateLighting` | same | Loot is pack loot under the EV ceiling, spawners stay vanilla. | 08 | D20 | |
| `buildingChance`, `buildingMinFloors`, `buildingMinFloorsChance`, `buildingMaxFloorsChance`, `buildingMinCellars`, `buildingMaxCellars`, `buildingDoorwayChance`, `buildingFrontChance`, `multiUseCorner`, `useAvgHeightmap` | same | The floor-chance pair is checked at load so the random bound is never below 1. | 04 | Bug register | PROF-06 |
| `buildingMaxFloors` | changed | Default 8, allowed range 0 to 22 instead of 0 to 60. Upstream's per-chunk cap (below Y 248) keeps everything below Y 255: at city level 8 a building gets at most 21 floors ([height-mapping.md](../03-terrain/height-mapping.md#building-envelope)). | 04 | [0005](../../design/0005-rwg-overlay.md) §6 | |
| `parkChance`, `openLotParkChance`, `corridorChance`, `bridgeChance`, `plannedPrimaryBridgeChance`, `plannedPrimaryBridgeMaxLength`, `fountainChance`, `bridgeSupports`, `parkElevation`, `parkBorder`, `parkStreetThreshold`, `primaryRoadSpacingX`, `primaryRoadSpacingZ`, `primaryRoadOptionalChance`, `primaryRoadForceEvery`, `secondaryRoadMinCountX`, `secondaryRoadMaxCountX`, `secondaryRoadMinCountZ`, `secondaryRoadMaxCountZ`, `minimumRoadSeparation`, `minimumRoadEdgeDistance`, `tertiaryRoadChance`, `tertiaryRoadMinLength`, `tertiaryRoadMaxLength`, `multiBuildingStreetConflict` | same | Upstream's explicit checks stay, with upstream's messages as lang keys. | 04 | | |
| `streetGenerationMode` | dropped | Only `HIERARCHICAL_GRID_V1` is ported, and it is not persisted. | 04 | D11 | |
| `railwayDungeonChance`, `railwaysCanEnd`, `railwaysEnabled`, `railwayStationsEnabled`, `railwaySurfaceStationsEnabled`, `railwayLevelOffset`, `railwaySpacingNorthSouth`, `railwaySpacingEastWest` | same | Rail heights within 0 to 255 are `06-highways-railways`' to map: the rail Y is ground + 6 × (−3 + `railwayLevelOffset`), and an offset that would put it below Y 5 (RWG's bedrock is Y 0 to 4) is clamped up with a warning (P06-7, D31). | 06 | D31 | |
| `highwayLevelFromCities`, `highwaySupports`, `highwayPlanningCellSize`, `highwayHubSampleSpacing`, `highwayHubMinimumPotential`, `highwayHubSearchRadiusCells`, `highwayMinimumHubDistance`, `highwayMaximumHubDistance`, `highwayMaximumConnectionsPerHub`, `highwayMinimumRouteLength`, `highwayRouteCityPenalty`, `highwayNetworkLevel` | same | The intercity planner's options. It runs synchronously with no saved hub data. `highwayNetworkLevel` is clamped, with a warning, to the highest level whose tallest highway part still ends at or below Y 255 (28 at ground 71); a `groundLevel` above 244 rejects the profile (P06-6, D31, [06-highways-railways](../06-highways-railways/index.md)). | 06 | D11, D31 | |
| `highwayGenerationMode` | dropped | Only `INTERCITY_NETWORK_V1` is ported, and it is not persisted. | 06 | D11 | |
| `highwayRequiresTwoCities`, `highwayMainPerlinScale`, `highwaySecondaryPerlinScale`, `highwayPerlinFactor` | dropped | Read only by legacy noise highways ([Highway.java#L136](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Highway.java#L136), [#L168-L174](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Highway.java#L168-L174)). | 06 | D11 | |
| `highwayDistanceMask` | dropped | Read only by legacy highways too ([Highway.java#L101-L105](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Highway.java#L101-L105)); its "0 disables highways" use goes to the new profile option `highwaysEnabled` (P-PROF-5, P06-5). | 06 | D11, D31 | (`06`'s HWY-06) |
| `editMode` | same | Pinned at creation, so a world is an edit world from its start. | 01 | D13 | |
| `generateNether` | dropped | Dead upstream; overworld only. | 02 | D12, [0003](../../design/0003-world-presence.md) | PROF-08 |

## Category `explosions` (12 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `debrisToNearbyChunkFactor`, `explosionsInCitiesOnly` | same | | 03 | | |
| `explosionChance`, `miniExplosionChance` | changed | Defaults 0.0004 and 0.005 instead of 0.002 and 0.03: craters are an occasional accent, not the look of a city ([03-terrain](../03-terrain/index.md#explosions-damage-debris-ruins-rubble-damage-ruins-rubblemd)). A profile can still ask for upstream's numbers. | 03 | AR-8 ([alpha review](../../reviews/2026-11-02-alpha.md)) | |
| `explosionMinRadius`, `explosionMaxRadius`, `explosionMinHeight`, `explosionMaxHeight`, `miniExplosionMinRadius`, `miniExplosionMaxRadius`, `miniExplosionMinHeight`, `miniExplosionMaxHeight` | changed | Heights clamped to 0 to 255. Each minimum must be below its maximum, checked at load. Default radii 12 to 24 and 4 to 9 instead of 15 to 35 and 5 to 12 (AR-8). | 03 | [0005](../../design/0005-rwg-overlay.md) §6; bug register; AR-8 | PROF-06 |

## Category `cities` (28 options)

| Options | Here | What changes | Owner | Decision | Avoids |
|---|---|---|---|---|---|
| `cityChance`, `cityMinRadius`, `cityMaxRadius`, `cityPerlinScale`, `cityPerlinOffset`, `cityPerlinInnerScale`, `cityThreshold`, `oceanCorrectionBorder` | same | City placement and city-center styles mix in the world seed; each chunk takes its city center's style. The radius pair is checked at load. | 04 | D10 | PROF-06, PROF-10, PROF-11 |
| `cityPlacement`, `cityGridCell`, `cityGridJitter` | new | Deviation: `rarecities` places one city center per 64 x 64 chunk cell, jittered 16 chunks in from the cell edge, radius 100 to 255 blocks, instead of a per-chunk 0.00025 chance (which clumps and leaves large gaps). Cell centers depend on the world seed and the cell alone. Every other profile keeps `chance`, and so does any world pinned before these options existed (a snapshot without the key reads the default). `cityConnected`, `cityAvoidOcean` and the city mask still apply, so a grid center on water yields no city. | 04 | D270 | none |
| `networkLayout` (`legacy`, `links`) | new | Deviation: `links` routes highways and railways along the city link graph (needs `cityPlacement` grid). Default `legacy`; `rarecities` uses `links` with `railwaysCanEnd` false ([0015](../../design/0015-city-link-network.md), D324) | 06 | D324 | none |
| `citySpawnDistance1`, `citySpawnDistance2`, `citySpawnMultiplier1`, `citySpawnMultiplier2` | same | Upstream's semantics: they only scale cities that already exist, so they cannot place one near spawn. M03's measurement showed no near-spawn option is needed (D30, D53). | 04 | D21, D30 | |
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
| `preventLakesInCities` (upstream 1.12's `preventLakesRavinesInCities`, renamed because the cave carver is not wrapped, so ravines are not covered: P-PROF-6, P03-2) | `lostcity` | on | Vetoes RWG's `LAKE`, `LAVA` and `DUNGEON` populate steps and NHCore's oil deposits wherever a population area touches a city chunk or a scattered structure's footprint (P07-4); landmark cores get no veto | 03 | [0005](../../design/0005-rwg-overlay.md) §3, D31 |
| `highwaysEnabled` | `lostcity` | true | A per-world switch for highways; false generates none (P06-5, HWY-06) | 06 | D31 |
| `villageTreatment` | `lostcity` | `BUILD_AROUND` | `BUILD_AROUND`, `AVOID` (upstream's avoidance), `IGNORE`; replaces `avoidVillages` | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §3 |
| `landmarkTreatment` | `lostcity` | `BUILD_AROUND` | The same three values for each landmark kind other than villages: a comma-separated list of a bare treatment, which sets every kind, and `<kind>=<treatment>` entries (`ocean_monument=AVOID`); replaces `avoidStructures` | 04, 07 | [0006](../../design/0006-build-around-other-worldgen.md) §3, D138 |
| `landmarkMargin` | `lostcity` | 2 blocks | Growth of a landmark footprint before it is turned into core chunks | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §1 |
| `softEdgeWidth` | `lostcity` | 6 columns | Width of the strip in a core's margin that meets the blended ground to the core's own edge columns | 03 | [0006](../../design/0006-build-around-other-worldgen.md) §4 |
| `cityMaxRise` | `lostcity` | 2 city levels | How far a city chunk's level may rise above its city's base level; one level more is cut down to it, higher ground is not city; 8 is no cap (alpha review AR-4) | 03, 04 | [height-mapping.md](../03-terrain/height-mapping.md#rise-cap), D148 |
| `villageTransitionRings` | `lostcity` | 2 | Rings of lower building density around a village | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §1 |
| `villageLinkDistance` | `lostcity` | 4 chunks | How far outside a city a village still gets a lane | 04 | [0006](../../design/0006-build-around-other-worldgen.md) §3 |
| `cityTaper` | `lostcity` | 1.0 | How strongly building heights follow the urban intensity, low at a city's edge and high at its core; 0 is upstream's floor count | 04 | [0011](../../design/0011-city-edges-and-character.md), D219, D226 |
| `edgeLotChance` | `lostcity` | 0.45 | Chance that a building chunk at a city's very edge is an empty lot, an excavation or a foundation | 04 | [0011](../../design/0011-city-edges-and-character.md), D220, D226 |
| `packHouseChance` | `lostcity` | 0.25 | Chance that an empty edge lot holds a pack mod's village house; a snapshot without the key (a world pinned before it) reads 0 | 04 | [0011](../../design/0011-city-edges-and-character.md), D297, D282 |
| `densityRings` | `lostcity` | 1.0 | Strength of the density rings, building classes and ring construction lots; 0 is off; `rarecities` 1.5; a snapshot without the key (a world pinned before it) reads 0 | 04 | D306, D282 |
| `lotFootprints` | `lostcity` | true | Ring construction lots take their ring class's real size (1x1, 2x1 or 2x2 chunks, drawn per 2x2 cell), dig its cellars at the floor pitch and raise a frame a random part of its floors; false keeps single-chunk lots; a snapshot without the key (a world pinned before it) reads false | 04 | [0011](../../design/0011-city-edges-and-character.md), D325, D282 |
| `furnish` | `lostcity` | true | Furnish building floors from the room palettes in `data/lostcitiesnh/furnish/`; a snapshot without the key (a world pinned before it) reads false | 03 | [0016](../../design/0016-furnish-pass.md), D335, D282 |
| `damageRepairChance` | `lostcity` | 0.5 | Chance that an explosion in a city was partly repaired with similar blocks and its windows boarded | 03 | [0011](../../design/0011-city-edges-and-character.md), D222, D226 |
| `facadeContrast` | `lostcity` | 45 | Most lightness and saturation contrast two neighboring facades may have; 200 turns the check off | 05 | [0012](../../design/0012-facade-color-feathering.md), D226 |
| `vanillaLootCategories` | `lostcity` | off | Lets city chests also draw from the pack's vanilla `ChestGenHooks` categories | 08 | D20, D196 |
| `enderZooHardMobs` | `lostcity` | off | `hardmobs` spawners take EnderZoo's overworld mobs instead of blazes and witches | 08 | D199 |
| `floorPitch` | `lostcity` | 8 blocks | Blocks per building floor and cellar, 6 to 10; floor parts are stretched to it, city levels stay 6. A snapshot without the key, from a world pinned before the option, reads its legacy default 6 | 04, 05 | [0014](../../design/0014-floor-pitch.md), D295 |

Every new option is in the `lostcity` category (P-PROF-6, D31), and so is `heightSampleSize`, upstream's mod-config key, which is a pinned profile option here (D73). The module flags and the decorative metal flags are mod config, pinned with the profile ([01-core § Mod config](../01-core/index.md#mod-config), [pinned-profile.md](pinned-profile.md)). The catalog of every option, its default, range and status is [docs/config.md § Every profile option](../../config.md#every-profile-option).

## Count

107 + 12 + 25 + 14 + 5 = 163 upstream options: 133 kept (same or changed), 17 dropped, 13 deferred (`spawnSphere` waits with the spheres); 17 options of this port's own: the 16 in the table above and `heightSampleSize`.
